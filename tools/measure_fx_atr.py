#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
اندازه‌گیری ATR٪ ساعتی دو نماد برای بازمقیاس‌کردن پیش‌فرض‌های GBPUSD از طلا.

همان تعریف موتور (`Engine.structureAtr`): میانگین سادهٔ True Range روی
`slAtrPeriod = 14` کندل **بستهٔ** تایم‌فریم ساختار (H1) و
`ATR٪ = ATR ÷ کلوز × ۱۰۰`. کندل H1 با کف‌کردن timestamp روی ساعت UTC ساخته می‌شود.

خروجی: میانه/صدک‌های ATR٪ برای هر دو نماد در هر پنج بازهٔ ۱/۲/۳/۱۰/۲۰ ساله،
به‌علاوهٔ نسبت GBPUSD÷XAUUSD که ضریب بازمقیاس‌کردن است.
"""
import gzip
import json
import os
import sys

PERIOD = 14
DATASETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "local-data")


def h1_atr(path):
    """میانه و صدک‌های ATR٪ ساعتی + میانگین کلوز. هیچ نگاه به آینده‌ای در کار نیست."""
    buckets = {}
    with gzip.open(path, "rt") as f:
        next(f)
        for line in f:
            p = line.split(",")
            ms = int(p[0]); o = float(p[1]); h = float(p[2]); l = float(p[3]); c = float(p[4])
            b = (ms // 3_600_000) * 3_600_000
            e = buckets.get(b)
            if e is None:
                buckets[b] = [o, h, l, c]
            else:
                if h > e[1]: e[1] = h
                if l < e[2]: e[2] = l
                e[3] = c
    keys = sorted(buckets)
    bars = [buckets[k] for k in keys]
    pcts = []
    for i in range(PERIOD, len(bars)):
        tr = 0.0
        for j in range(i - PERIOD, i):
            h, l = bars[j][1], bars[j][2]
            pc = bars[j - 1][3]
            a = h - l
            b = abs(h - pc)
            if b > a: a = b
            b = abs(l - pc)
            if b > a: a = b
            tr += a
        px = bars[i][3]
        if px > 0:
            pcts.append(tr / PERIOD / px * 100.0)
    pcts.sort()
    n = len(pcts)
    if n == 0:
        return None
    closes = sorted(b[3] for b in bars)
    return {
        "h1Bars": len(bars),
        "n": n,
        "median": pcts[n // 2],
        "p10": pcts[n // 10],
        "p25": pcts[n // 4],
        "p75": pcts[3 * n // 4],
        "p90": pcts[9 * n // 10],
        "medianClose": closes[len(closes) // 2],
        "lastClose": bars[-1][3],
    }


def main():
    out = {}
    for years in (1, 2, 3, 10, 20):
        row = {}
        for sym in ("xauusd", "gbpusd"):
            path = os.path.join(DATASETS, "goldpin_%s_m1_%dy.csv.gz" % (sym, years))
            if not os.path.isfile(path):
                print("skip", path, file=sys.stderr)
                continue
            st = h1_atr(path)
            row[sym] = st
            print("%-8s %2dy  H1=%-7d n=%-7d medianATR%%=%.4f  p10=%.4f p25=%.4f "
                  "p75=%.4f p90=%.4f  medClose=%.5f"
                  % (sym, years, st["h1Bars"], st["n"], st["median"], st["p10"], st["p25"],
                     st["p75"], st["p90"], st["medianClose"]), flush=True)
        if len(row) == 2:
            row["ratio"] = row["gbpusd"]["median"] / row["xauusd"]["median"]
            print("   → نسبت میانهٔ ATR٪ (GBPUSD ÷ XAUUSD) = %.4f\n" % row["ratio"], flush=True)
        out[str(years)] = row
    with open("/home/user/ST1/docs/fx_atr_calibration.json", "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=2)
        f.write("\n")


if __name__ == "__main__":
    main()
