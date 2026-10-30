# دیتاست‌های بک‌تست — طلا (XAUUSD) و پوند/دلار (GBPUSD)، کندل M1

داده‌ها **نه داخل APK و نه داخل مخزن** نگهداری می‌شوند؛ هر پنج برش به‌صورت asset در بخش
[**Releases**](https://github.com/alisport1384/ST2/releases/tag/data-v1) با تگ `data-v1` منتشر شده‌اند و اپ هر بازه را پس از انتخاب کاربر جداگانه از همان‌جا دانلود می‌کند. مزیت: کلون مخزن چند مگابایت
است، نه چند صد مگابایت، و دانلود داخل اپ از CDN ریلیز سریع‌تر و پایدارتر است. دیتاست و نتیجهٔ اجرای بک‌تست هر بازه در فایل مستقل ذخیره می‌شود.

| بازه | فایل | کندل | اندازهٔ فشرده | شروع UTC | پایان UTC | SHA-256 |
|---|---|---:|---:|---|---|---|
| ۱ سال | [`goldpin_xauusd_m1_1y.csv.gz`](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_1y.csv.gz) | 351,894 | 5,128,001 بایت | 2025-08-21 00:00 | 2026-08-20 23:59 | `15fffd94b9c1970080d6012ff2aa3ae307d18a872381c677e44d16b5fd08db44` |
| ۲ سال | [`goldpin_xauusd_m1_2y.csv.gz`](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_2y.csv.gz) | 701,067 | 9,844,331 بایت | 2024-08-21 00:00 | 2026-08-20 23:59 | `873d4ffc765723abfada8ef55fbf8b9bb9c7432125187395972f51af94496e9f` |
| ۳ سال | [`goldpin_xauusd_m1_3y.csv.gz`](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_3y.csv.gz) | 1,040,730 | 14,310,975 بایت | 2023-08-21 00:00 | 2026-08-20 23:59 | `56751049149f3df1ed134fdbc5e667ec57655f445edde5820ef9d9fed6697fcc` |
| ۱۰ سال | [`goldpin_xauusd_m1_10y.csv.gz`](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_10y.csv.gz) | 3,402,126 | 43,314,146 بایت (حدود 41.3 MiB) | 2016-08-21 23:00 | 2026-08-20 23:59 | `c0e5c8d2151605db8d0cf1c75f3f1d6cca0ecc5304bad233f536ffe45d7f7881` |
| ۲۰ سال | [`goldpin_xauusd_m1_20y.csv.gz`](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_20y.csv.gz) | 6,563,723 | 77,779,089 بایت (حدود 74.2 MiB) | 2006-08-21 00:02 | 2026-08-20 23:59 | `ffbc7c1bd8723728714dfc2f476102f7887be3797d0caf187b622aa533c830c9` |

همهٔ فایل‌ها شامل سرستون و ردیف‌های `timestamp(ms,UTC),open,high,low,close,volume` هستند. حجم، tick volume منبع است. در برش ۲۰ساله کندل بی‌حجم، نامعتبر یا timestamp تکراری وجود ندارد؛ ۱٬۰۷۱ فاصلهٔ بیش از ۲۰ ساعت گزارش شده است. فایل `.meta.json` کنار دیتاست، آمار ساخت را ثبت می‌کند.

## رفتار اپ

- از تنظیمات، بازهٔ ۱، ۲، ۳، ۱۰ یا ۲۰ سال را انتخاب و همان فایل را دانلود کنید؛ اپ اول از
  `ST2/releases/download/data-v1` و در صورت خطا از آینهٔ `ST1` می‌گیرد؛ SHA-256 قبل از تأیید فایل بررسی می‌شود.
- برای اجرای بک‌تست، نشست داخلی را روشن کنید. با تغییر بازه، «اجرای مجدد» دیتاست انتخابی را بارگذاری می‌کند.
- داده و نتیجهٔ هر بازه جدا هستند؛ حذف، دانلود یا اجرای یک بازه به بقیه دست نمی‌زند. هیچ دیتاستی داخل APK نیست.
- برای تاریخچه‌های بلند، کندل‌ها با ستون‌های primitive نگه‌داری می‌شوند؛ زمان‌های دقیقه‌ای هم فشرده می‌شوند تا حافظهٔ کمتری مصرف شود.
- دیتاست ۲۰ساله حدود ۷۴٫۲ MiB فشرده است و ۶٫۵۶ میلیون کندل دارد. پردازش آن ممکن است ده‌ها دقیقه طول بکشد و روی گوشی‌های کم‌حافظه محدودیت داشته باشد.

## منبع و بازتولید

هر پنج برش از [Kaggle: majidmehdipour/xauusd-ohlcv-1m](https://www.kaggle.com/datasets/majidmehdipour/xauusd-ohlcv-1m) ساخته شده‌اند (مجوز MIT؛ فایل منبع 6,886,934 ردیف دارد). بازهٔ ۲۰ساله از `2006-08-21` تا `2026-08-21` (حد بالا غیرشامل) انتخاب شده؛ دادهٔ منبع در ۲۰۲۶-۰۸-۲۰ پایان می‌یابد. انتهای دیتاست ۲۰ساله با تمام ردیف‌های فایل ۱۰ساله، بایت‌به‌بایت یکسان است.

ساخت برش‌های کوتاه‌تر با `tools/build_sample.py` انجام می‌شود. برای برش‌های بلند از نسخهٔ دسته‌ای سریع `tools/build_long_sample.py` استفاده کنید (نیازمند `pandas` و `numpy`). هر دو ابزار زمان `Europe/Berlin` را با DST به UTC تبدیل، آخرهفتهٔ بستهٔ بازار را حذف، OHLC را بررسی و خروجی gzip قطعی (`mtime=0`) تولید می‌کنند. برای بازسازی ۲۰ سال:

```bash
python3 -m pip install pandas numpy
python3 tools/build_long_sample.py --src /path/XAUUSD_OHLCV_1M.csv \
  --from 2006-08-21 --to 2026-08-21 \
  --out datasets/goldpin_xauusd_m1_20y.csv.gz
```

## دریافت مستقیم (بدون اپ)

هر فایل مستقیماً از Release قابل دانلود است — همین URLهایی که اپ استفاده می‌کند:

```bash
BASE=https://github.com/alisport1384/ST2/releases/download/data-v1
curl -fLO $BASE/goldpin_xauusd_m1_1y.csv.gz
curl -fLO $BASE/goldpin_xauusd_m1_2y.csv.gz
curl -fLO $BASE/goldpin_xauusd_m1_3y.csv.gz
curl -fLO $BASE/goldpin_xauusd_m1_10y.csv.gz
curl -fLO $BASE/goldpin_xauusd_m1_20y.csv.gz
```

| برش | لینک دانلود | آینهٔ دوم |
|---|---|---|
| ۱ سال | [data-v1/1y](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_1y.csv.gz) | [ST1](https://github.com/alisport1384/ST1/releases/download/data-v1/goldpin_xauusd_m1_1y.csv.gz) |
| ۲ سال | [data-v1/2y](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_2y.csv.gz) | [ST1](https://github.com/alisport1384/ST1/releases/download/data-v1/goldpin_xauusd_m1_2y.csv.gz) |
| ۳ سال | [data-v1/3y](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_3y.csv.gz) | [ST1](https://github.com/alisport1384/ST1/releases/download/data-v1/goldpin_xauusd_m1_3y.csv.gz) |
| ۱۰ سال | [data-v1/10y](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_10y.csv.gz) | [ST1](https://github.com/alisport1384/ST1/releases/download/data-v1/goldpin_xauusd_m1_10y.csv.gz) |
| ۲۰ سال | [data-v1/20y](https://github.com/alisport1384/ST2/releases/download/data-v1/goldpin_xauusd_m1_20y.csv.gz) | [ST1](https://github.com/alisport1384/ST1/releases/download/data-v1/goldpin_xauusd_m1_20y.csv.gz) |

## دیتاست‌های پوند/دلار — GBPUSD، کندل M1 (تگ `data-fx-v1`)

هر پنج برش در Release با تگ [`data-fx-v1`](https://github.com/alisport1384/ST3/releases/tag/data-fx-v1) منتشر می‌شوند، نه در مخزن یا APK. اپ هر بازه را جداگانه دریافت می‌کند و
SHA-256 پیش از تأیید بررسی می‌شود. آزمون `RepositoryFxDatasetTest` همین SHAها را قفل می‌کند.

| بازه | فایل | کندل | اندازهٔ فشرده | شروع UTC | پایان UTC | SHA-256 |
|---|---|---:|---:|---|---|---|
| ۱ سال | [`goldpin_gbpusd_m1_1y.csv.gz`](https://github.com/alisport1384/ST3/releases/download/data-fx-v1/goldpin_gbpusd_m1_1y.csv.gz) | 369,390 | 4,044,650 بایت | 2025-08-21 00:00 | 2026-08-20 23:59 | `c66b3d01614f42508978c6061a9a29a6a452ff25f5aa868489075d94f28a3abd` |
| ۲ سال | [`goldpin_gbpusd_m1_2y.csv.gz`](https://github.com/alisport1384/ST3/releases/download/data-fx-v1/goldpin_gbpusd_m1_2y.csv.gz) | 738,328 | 8,148,903 بایت | 2024-08-21 00:00 | 2026-08-20 23:59 | `547d84e1faf31bc5930460ac8c07f52f4b5c065b7e6107fc1ffe12dd7189b56e` |
| ۳ سال | [`goldpin_gbpusd_m1_3y.csv.gz`](https://github.com/alisport1384/ST3/releases/download/data-fx-v1/goldpin_gbpusd_m1_3y.csv.gz) | 1,107,619 | 12,014,871 بایت | 2023-08-21 00:00 | 2026-08-20 23:59 | `d227be7d372e44fd1133b22fd9818cc8dcfae25f6f33e976aa615db2a9b89e31` |
| ۱۰ سال | [`goldpin_gbpusd_m1_10y.csv.gz`](https://github.com/alisport1384/ST3/releases/download/data-fx-v1/goldpin_gbpusd_m1_10y.csv.gz) | 3,580,052 | 38,247,889 بایت (حدود 36.5 MiB) | 2016-08-21 22:00 | 2026-08-20 23:59 | `081bb7af6e8dfe966ef4c17d03f9ddba4788d02914aa3e6daf698193b24394fe` |
| ۲۰ سال | [`goldpin_gbpusd_m1_20y.csv.gz`](https://github.com/alisport1384/ST3/releases/download/data-fx-v1/goldpin_gbpusd_m1_20y.csv.gz) | 7,101,926 | 74,396,016 بایت (حدود 70.9 MiB) | 2006-08-21 00:00 | 2026-08-20 23:59 | `fc1ccef3b080e00ea049624684d20cc5cbfb35d3f53942ce42a2fa52487aec2e` |

**منبع:** مجموعهٔ عمومی [`bunqi/OANDA_GBPUSD_GBPUSD`](https://huggingface.co/datasets/bunqi/OANDA_GBPUSD_GBPUSD)
روی Hugging Face — کندل یک‌دقیقه‌ای OANDA از ۲۰۰۲-۰۵ تا امروز، ۲۹۴ فایل ماهانهٔ Parquet بدون شکاف.

**حجم:** ستون `volume` = **تعداد تیک در دقیقه** (tick volume)، عدد صحیح. در هیچ‌کدام از پنج برش
کندل بی‌حجم وجود ندارد (`zeroVolumeBars = 0` در همهٔ `.meta.json`). این همان خانوادهٔ واحدی است
که دیتاست‌های منتشرشدهٔ طلا دارند (میانهٔ حجم طلای یک‌ساله ۱۴۹، پوند ۱۲۲)، پس **بدون هیچ ضریبی**
مستقیم مصرف می‌شود — استراتژی‌های باکس ولوم کم/زیاد بدون حجم کار نمی‌کنند و این پیش‌شرط اصلی بود.

**لنگر زمانی:** بازه‌ها با **تفریق سال تقویمی** از یک لنگر مشترک ساخته شده‌اند، دقیقاً مثل طلا:
آخرین کندل هر پنج فایل `2026-08-20T23:59Z` است. کندل‌های آخرهفتهٔ بسته حذف شده‌اند
(`droppedWeekend` در `.meta.json`) و OHLC نامعتبر و timestamp تکراری صفر است.

**منابع ردشده (مستند، تا دوباره امتحان نشوند):**
- `HISTDATA.COM` — فایل‌های M1 در هر دو قالب ASCII و MT برای **همهٔ** ردیف‌ها `volume = 0`
  دارند (روی ۳۷۳٬۰۵۷ ردیف سال ۲۰۲۰ بررسی شد). بی‌فایده برای این استراتژی.
- `Dukascopy` — قالب bi5 درست است ولی سرور با هم‌زمانی ۵۰۳ می‌دهد و دانلود ۷٬۳۰۵ فایل
  از این محیط در ۱۵ دقیقه تمام نشد.
- `getdata-finance/gbpusd-1m-ohlcv-forex-historical-data` — نسخهٔ رایگان فقط حدود ۶ ماه است.

**بازتولید:**

```bash
python3 -m pip install pyarrow
python3 tools/build_fx_dataset.py --symbol GBPUSD \
  --from 2006-08-21 --to 2026-08-21 --anchor-to 2026-08-21 \
  --cache /tmp/fxcache --out-dir datasets --ranges 1,2,3,10,20 \
  --sample-out app/src/main/assets/sample_gbpusd_m1.csv --sample-bars 40000
```

خروجی gzip **قطعی** است (`mtime=0`): بازسازی برش ۲۰ساله در ۲۰۲۶-۱۰-۳۰ بایت‌به‌بایت همان
SHA-256 جدول بالا را داد. آمار هر برش در `.meta.json` کنارش و خلاصهٔ همه در
`datasets/fx_gbpusd_summary.json` نوشته می‌شود.

### دریافت مستقیم (بدون اپ)

```bash
FX=https://github.com/alisport1384/ST3/releases/download/data-fx-v1
curl -fLO $FX/goldpin_gbpusd_m1_1y.csv.gz
curl -fLO $FX/goldpin_gbpusd_m1_2y.csv.gz
curl -fLO $FX/goldpin_gbpusd_m1_3y.csv.gz
curl -fLO $FX/goldpin_gbpusd_m1_10y.csv.gz
curl -fLO $FX/goldpin_gbpusd_m1_20y.csv.gz
```

## برای کار روی کد (تست‌ها و هارنس CLI)

تست‌های `engine` و `app` و همچنین `cli/OptimizeKt` دیتاست را از پوشهٔ محلی `datasets/` می‌خوانند.
یک بار اسکریپت زیر را بزنید تا هر پنج فایل دانلود و SHA-256 بررسی شود:

```bash
./tools/fetch_datasets.sh            # همه
./tools/fetch_datasets.sh 1y 10y     # فقط دو برش
```

این پوشه در `.gitignore` است، پس چیزی به مخزن برنمی‌گردد. اگر فایل نباشد، تست‌های وابسته به
داده با `assumeTrue` **skip** می‌شوند (build نمی‌شکند).

### ساخت بازه‌های کوتاه‌تر از فایل ۲۰ساله

می‌توان همان برش‌ها را از فایل ۲۰ساله بازساخت؛ خروجی با SHA-256 جدول بررسی می‌شود:

```bash
python3 tools/slice_fx_dataset.py /path/GBPUSD-20y-M1-volume.csv.gz 10 goldpin_gbpusd_m1_10y.csv.gz
```

بازهٔ خروجی می‌تواند ۱، ۲، ۳ یا ۱۰ سال باشد. سپس فایل فشرده را در بخش «وارد کردن فایل فشردهٔ داده از دستگاه» وارد کنید.
