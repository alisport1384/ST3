#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ساخت دیتاست بک‌تست جفت‌ارز (M1) از دادهٔ عمومی OANDA.

منبع: مجموعهٔ `bunqi/OANDA_GBPUSD_GBPUSD` روی Hugging Face — کندل یک‌دقیقه‌ای
      OANDA برای GBPUSD از ۲۰۰۲-۰۵ تا امروز، ماه‌به‌ماه در فایل‌های Parquet:
          1/OANDA_GBPUSD_GBPUSD_1_YYYY-MM-DD_to_YYYY-MM-DD.parquet
      ستون‌ها: `time` (ثانیهٔ یونیکس UTC) · open · high · low · close ·
      `volume` (عدد صحیح = تعداد تیک در آن دقیقه).

چرا این منبع:
  • رایگان و بدون کلید است و **حجم واقعی** دارد. HistData (ASCII/MT) ستون حجم را
    برای همهٔ کندل‌ها صفر می‌گذارد و فید روزانهٔ Dukascopy هم با حجم واقعی است
    اما هر کندل روز در یک فایل جداست (۷۳۰۵ فایل برای ۲۰ سال) و عملاً قابل
    دانلود انبوه نیست.
  • همان «M1 با حجم» است که طلا (Kaggle xauusd-ohlcv-1m) و نفت با آن ساخته شده‌اند،
    و واحد حجمش (تیک در دقیقه) دقیقاً همان واحد دیتاست‌های منتشرشدهٔ طلاست.

خروجی دقیقاً همان قالب دیتاست‌های طلا/نفت است:
    timestamp(ms,UTC),open,high,low,close,volume
و همان قاعده‌های بهداشت داده:
  ۱) حذف شنبه و یکشنبهٔ بسته (شنبه کامل، یکشنبه پیش از ۲۲:۰۰ UTC) — قواعد MarketHours موتور
  ۲) مرتب‌سازی + حذف timestamp تکراری
  ۳) اعتبارسنجی OHLC و نبود کندل بی‌حجم
  ۴) gzip قطعی (mtime=0) تا SHA-256 هر بار یکی باشد

وابستگی: `pip install pyarrow`

نمونه:
  python3 tools/build_fx_dataset.py --symbol GBPUSD \
      --from 2006-08-21 --to 2026-08-21 \
      --cache /tmp/hfcache --out-dir datasets --ranges 1,2,3,10,20
"""
import argparse
import concurrent.futures as cf
import datetime as dt
import gzip
import hashlib
import json
import os
import random
import re
import sys
import time
import urllib.error
import urllib.request

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
HF_REPO = "bunqi/OANDA_GBPUSD_GBPUSD"
SATURDAY, SUNDAY = 5, 6
SUNDAY_OPEN_HOUR_UTC = 22
MONTH_FILE = re.compile(r"^1/OANDA_(?P<sym>[A-Z0-9]+)_(?P=sym)_1_"
                        r"(?P<a>\d{4}-\d{2}-\d{2})_to_(?P<b>\d{4}-\d{2}-\d{2})\.parquet$")


def parse_args(argv):
    p = argparse.ArgumentParser()
    p.add_argument("--symbol", required=True, help="جفت‌ارز، مثل GBPUSD")
    p.add_argument("--from", dest="dfrom", required=True, help="YYYY-MM-DD (UTC، شامل)")
    p.add_argument("--to", dest="dto", required=True, help="YYYY-MM-DD (UTC، غیرشامل)")
    p.add_argument("--cache", required=True, help="پوشهٔ کش فایل‌های Parquet خام")
    p.add_argument("--out-dir", required=True, help="پوشهٔ خروجی دیتاست‌ها")
    p.add_argument("--ranges", default="1,2,3,10,20", help="بازه‌های سالانه، مثل 1,2,3,10,20")
    p.add_argument("--anchor-to", default=None,
                   help="لنگر پایان برش‌ها (پیش‌فرض = --to)؛ هر برش N سال پیش از این تاریخ است")
    p.add_argument("--workers", type=int, default=6)
    p.add_argument("--retries", type=int, default=5)
    p.add_argument("--max-gap-hours", type=float, default=20.0)
    p.add_argument("--sample-out", default=None,
                   help="مسیر دارایی نمونهٔ داخلی اپ (sample_<sym>_m1.csv) — N کندل آخر")
    p.add_argument("--sample-bars", type=int, default=40_000,
                   help="تعداد کندل دارایی نمونه (پیش‌فرض ۴۰٬۰۰۰ — هم‌اندازهٔ طلا و نفت)")
    return p.parse_args(argv)


def http_get(url, retries=4, timeout=60):
    last = ""
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=timeout) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            last = "HTTP %d" % e.code
            if e.code == 404:
                raise
        except Exception as e:                                    # noqa: BLE001
            last = type(e).__name__
        time.sleep(min(20.0, 1.5 * (2 ** attempt)) + random.random())
    raise RuntimeError("download failed: %s (%s)" % (url, last))


def month_files(sym):
    """فهرست فایل‌های ماهانهٔ M1 یک نماد → [(date_from, date_to, filename)]"""
    info = json.loads(http_get("https://huggingface.co/api/datasets/" + HF_REPO))
    out = []
    for s in info.get("siblings", []):
        m = MONTH_FILE.match(s.get("rfilename", ""))
        if not m or m.group("sym") != sym.upper():
            continue
        a = dt.datetime.strptime(m.group("a"), "%Y-%m-%d").replace(tzinfo=dt.timezone.utc)
        b = dt.datetime.strptime(m.group("b"), "%Y-%m-%d").replace(tzinfo=dt.timezone.utc)
        out.append((a, b, s["rfilename"]))
    out.sort()
    if not out:
        raise RuntimeError("no M1 parquet file found for %s in %s" % (sym, HF_REPO))
    return out


def ensure_cached(cache, sym, files, workers, retries):
    os.makedirs(os.path.join(cache, sym), exist_ok=True)
    todo = []
    for a, b, name in files:
        dest = os.path.join(cache, sym, os.path.basename(name))
        if os.path.isfile(dest) and os.path.getsize(dest) > 0:
            continue
        todo.append((name, dest))

    def one(job):
        name, dest = job
        data = http_get("https://huggingface.co/datasets/%s/resolve/main/%s" % (HF_REPO, name),
                        retries=retries, timeout=120)
        tmp = dest + ".part"
        with open(tmp, "wb") as f:
            f.write(data)
        os.replace(tmp, dest)
        return name, len(data)

    if todo:
        with cf.ThreadPoolExecutor(max_workers=workers) as ex:
            for i, (name, size) in enumerate(ex.map(one, todo), 1):
                if i % 25 == 0 or i == len(todo):
                    print("  دانلود %d/%d ماه" % (i, len(todo)), flush=True)
    print("کش آماده: %d ماه (%d تازه)" % (len(files), len(todo)), flush=True)


def iter_bars(cache, sym, files, lo_ms, hi_ms):
    """کندل‌های [lo,hi) به ترتیب زمانی: (ms, o, h, l, c, v)"""
    import pyarrow.parquet as pq
    for a, b, name in files:
        # +۱ روز حاشیهٔ امن: نام فایل بازهٔ اسمی ماه است، نه لزوماً بازهٔ داده
        if (b + dt.timedelta(days=1)).timestamp() * 1000 < lo_ms:
            continue
        if a.timestamp() * 1000 >= hi_ms:
            continue
        path = os.path.join(cache, sym, os.path.basename(name))
        tbl = pq.read_table(path, columns=["time", "open", "high", "low", "close", "volume"])
        t = tbl.column("time").to_pylist()
        o = tbl.column("open").to_pylist()
        h = tbl.column("high").to_pylist()
        l = tbl.column("low").to_pylist()
        c = tbl.column("close").to_pylist()
        v = tbl.column("volume").to_pylist()
        for i in range(len(t)):
            yield t[i] * 1000, o[i], h[i], l[i], c[i], v[i]


def tradable(ms):
    d = dt.datetime.fromtimestamp(ms / 1000, tz=dt.timezone.utc)
    if d.weekday() == SATURDAY:
        return False
    if d.weekday() == SUNDAY and d.hour < SUNDAY_OPEN_HOUR_UTC:
        return False
    return True


def write_range(cache, sym, files, lo_ms, hi_ms, out_path, max_gap_hours):
    bars = zero_vol = dup = dropped_weekend = bad_ohlc = gaps = 0
    prev_ms = None
    first_t = last_t = None
    tmp = out_path + ".part"
    # gzip قطعی: mtime=0 ⇒ SHA-256 در هر اجرا یکی می‌ماند
    with open(tmp, "wb") as fo:
        gz = gzip.GzipFile(filename="", mode="wb", fileobj=fo, mtime=0, compresslevel=9)
        gz.write(b"timestamp,open,high,low,close,volume\n")
        for (ms, o, h, l, c, vol) in iter_bars(cache, sym, files, lo_ms, hi_ms):
            if ms < lo_ms or ms >= hi_ms:
                continue
            if not (o and h and l and c) or h < l or h < o or h < c or l > o or l > c:
                bad_ohlc += 1
                continue
            if prev_ms is not None and ms <= prev_ms:
                dup += 1
                continue
            if not tradable(ms):
                dropped_weekend += 1
                continue
            v = int(round(float(vol)))
            if v <= 0:
                zero_vol += 1
                continue
            if prev_ms is not None and (ms - prev_ms) / 3_600_000.0 > max_gap_hours \
                    and tradable(prev_ms):
                gaps += 1
            gz.write(("%d,%.5f,%.5f,%.5f,%.5f,%d\n" % (ms, o, h, l, c, v)).encode("ascii"))
            prev_ms = ms
            bars += 1
            if first_t is None:
                first_t = ms
            last_t = ms
        gz.close()
    os.replace(tmp, out_path)
    sha = hashlib.sha256()
    with open(out_path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            sha.update(chunk)
    return {
        "bars": bars,
        "bytesGz": os.path.getsize(out_path),
        "sha256": sha.hexdigest(),
        "fromUtc": (dt.datetime.fromtimestamp(first_t / 1000, tz=dt.timezone.utc)
                    .strftime("%Y-%m-%dT%H:%M:%S+00:00") if first_t else None),
        "toUtc": (dt.datetime.fromtimestamp(last_t / 1000, tz=dt.timezone.utc)
                  .strftime("%Y-%m-%dT%H:%M:%S+00:00") if last_t else None),
        "droppedWeekend": dropped_weekend,
        "droppedDup": dup,
        "badOhlc": bad_ohlc,
        "zeroVolumeBars": zero_vol,
        "gapsOverHours": gaps,
    }


def write_sample(cache, sym, files, hi_ms, bars, out_path, max_gap_hours):
    """N کندل آخرِ بازه (با همان قاعده‌های بهداشت داده) → CSV خام برای assets اپ."""
    from collections import deque
    keep = deque(maxlen=bars)
    prev_ms = None
    for (ms, o, h, l, c, vol) in iter_bars(cache, sym, files, 0, hi_ms):
        if ms >= hi_ms:
            continue
        if not (o and h and l and c) or h < l or h < o or h < c or l > o or l > c:
            continue
        if prev_ms is not None and ms <= prev_ms:
            continue
        if not tradable(ms):
            continue
        v = int(round(float(vol)))
        if v <= 0:
            continue
        prev_ms = ms
        keep.append((ms, o, h, l, c, v))
    tmp = out_path + ".part"
    with open(tmp, "w", encoding="ascii", newline="\n") as f:
        f.write("timestamp,open,high,low,close,volume\n")
        for (ms, o, h, l, c, v) in keep:
            f.write("%d,%.5f,%.5f,%.5f,%.5f,%d\n" % (ms, o, h, l, c, v))
    os.replace(tmp, out_path)
    return len(keep)


def main(argv):
    a = parse_args(argv)
    sym = a.symbol.upper()
    dfrom = dt.datetime.strptime(a.dfrom, "%Y-%m-%d").replace(tzinfo=dt.timezone.utc)
    dto = dt.datetime.strptime(a.dto, "%Y-%m-%d").replace(tzinfo=dt.timezone.utc)
    anchor = (dt.datetime.strptime(a.anchor_to, "%Y-%m-%d").replace(tzinfo=dt.timezone.utc)
              if a.anchor_to else dto)
    ranges = sorted({int(x) for x in a.ranges.split(",") if x.strip()})

    print("نماد=%s بازهٔ کل=%s تا %s" % (sym, dfrom.date(), dto.date()), flush=True)
    all_months = month_files(sym)
    months = [(x, y, n) for (x, y, n) in all_months
              if (y + dt.timedelta(days=1)) >= dfrom and x < dto]
    print("ماه‌های لازم=%d (از %d ماه موجود: %s … %s)"
          % (len(months), len(all_months), all_months[0][2].split('_1_')[1][:10],
             all_months[-1][2].split('_1_')[1][:10]), flush=True)
    os.makedirs(a.cache, exist_ok=True)
    os.makedirs(a.out_dir, exist_ok=True)
    ensure_cached(a.cache, sym, months, a.workers, a.retries)

    # ── دارایی نمونهٔ داخلی اپ ─────────────────────────────────────────────────
    #  همان قرارداد طلا و نفت: ۴۰٬۰۰۰ کندل M1 **آخر** با حجم، CSV خام (بدون gzip)
    #  داخل `app/src/main/assets`. این داده برای نخستین اجرای بی‌اینترنت اپ است، پس
    #  تازه‌ترین بازهٔ موجود گرفته می‌شود، نه پنجرهٔ بک‌تست.
    if a.sample_out:
        n = write_sample(a.cache, sym, months, int(dto.timestamp() * 1000),
                         a.sample_bars, a.sample_out, a.max_gap_hours)
        print("✓ نمونهٔ داخلی %s کندل=%d" % (os.path.basename(a.sample_out), n), flush=True)

    summary = {}
    for years in ranges:
        # برش **تقویمی**، نه ۳۶۵×N روز: دیتاست‌های طلا هم دقیقاً از همان تاریخِ
        # N سال پیش شروع می‌شوند (۲۰ ساله از ۲۰۰۶-۰۸-۲۱)، وگرنه با احتساب سال‌های
        # کبیسه پنجرهٔ GBPUSD چند روز از طلا عقب‌تر می‌افتاد و مقایسهٔ دو نماد
        # دیگر هم‌بازه نبود.
        lo = anchor.replace(year=anchor.year - years)
        name = "goldpin_%s_m1_%dy.csv.gz" % (sym.lower(), years)
        out = os.path.join(a.out_dir, name)
        st = write_range(a.cache, sym, months, int(lo.timestamp() * 1000),
                         int(anchor.timestamp() * 1000), out, a.max_gap_hours)
        st.update({
            "file": name,
            "symbol": sym,
            "years": years,
            "source": "OANDA 1-minute candles via Hugging Face dataset %s" % HF_REPO,
            "volumeUnit": "ticks per minute (OANDA tick volume)",
            "timezone": "UTC",
            "columns": "timestamp(ms,UTC),open,high,low,close,volume",
            "builtAtUtc": dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        })
        with open(os.path.join(a.out_dir, name + ".meta.json"), "w", encoding="utf-8") as f:
            json.dump(st, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print("✓ %-34s کندل=%-9d بایت=%-10s %s … %s  sha=%s"
              % (name, st["bars"], st["bytesGz"], st["fromUtc"], st["toUtc"], st["sha256"][:16]),
              flush=True)
        summary[str(years)] = st
    with open(os.path.join(a.out_dir, "fx_%s_summary.json" % sym.lower()), "w",
              encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
        f.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
