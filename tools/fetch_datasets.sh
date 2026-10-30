#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# دریافت دیتاست‌های بک‌تست GoldPin از بخش Releases
#
# فایل‌های M1 (۱، ۲، ۳، ۱۰ و ۲۰ ساله) داخل مخزن نیستند؛ به‌صورت asset در Release
# منتشر شده‌اند. این اسکریپت آن‌ها را در datasets/ می‌گذارد تا تست‌های engine/app و
# هارنس cli بتوانند اجرا شوند. SHA-256 هر فایل پیش از تأیید بررسی می‌شود.
#
#   ./tools/fetch_datasets.sh                 # هر پنج برش طلا (رفتار قدیمی، بدون تغییر)
#   ./tools/fetch_datasets.sh 1y 10y          # فقط برش‌های خواسته‌شدهٔ طلا
#   ./tools/fetch_datasets.sh fx              # هر پنج برش پوند/دلار
#   ./tools/fetch_datasets.sh fx 1y 20y       # فقط دو برش پوند
#   ./tools/fetch_datasets.sh all             # هر دو نماد، هر ده فایل
#
# ⚠ SHAها را هرگز دستی ننویسید: از `datasets/fx_gbpusd_summary.json` و
#   `datasets/*.meta.json` کپی می‌شوند و آزمون‌های `Repository*DataTest` همان‌ها را قفل
#   کرده‌اند. SHA اشتباه یعنی اپ فایل سالم را «خراب» می‌داند و هرگز تأیید نمی‌کند.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

OWNER_REPO="${GOLDPIN_DATA_REPO:-alisport1384/ST2}"
MIRROR_REPO="${GOLDPIN_DATA_MIRROR:-alisport1384/ST1}"
FX_REPO="${GOLDPIN_FX_DATA_REPO:-alisport1384/ST3}"
DEST="$(cd "$(dirname "$0")/.." && pwd)/datasets"

# طلا — تگ data-v1
TAG_XAU="data-v1"
declare -A SHA_XAU=(
  [1y]=15fffd94b9c1970080d6012ff2aa3ae307d18a872381c677e44d16b5fd08db44
  [2y]=873d4ffc765723abfada8ef55fbf8b9bb9c7432125187395972f51af94496e9f
  [3y]=56751049149f3df1ed134fdbc5e667ec57655f445edde5820ef9d9fed6697fcc
  [10y]=c0e5c8d2151605db8d0cf1c75f3f1d6cca0ecc5304bad233f536ffe45d7f7881
  [20y]=ffbc7c1bd8723728714dfc2f476102f7887be3797d0caf187b622aa533c830c9
)

# پوند/دلار — تگ data-fx-v1 (منبع: OANDA M1 روی Hugging Face)
TAG_FX="data-fx-v1"
declare -A SHA_FX=(
  [1y]=c66b3d01614f42508978c6061a9a29a6a452ff25f5aa868489075d94f28a3abd
  [2y]=547d84e1faf31bc5930460ac8c07f52f4b5c065b7e6107fc1ffe12dd7189b56e
  [3y]=d227be7d372e44fd1133b22fd9818cc8dcfae25f6f33e976aa615db2a9b89e31
  [10y]=081bb7af6e8dfe966ef4c17d03f9ddba4788d02914aa3e6daf698193b24394fe
  [20y]=fc1ccef3b080e00ea049624684d20cc5cbfb35d3f53942ce42a2fa52487aec2e
)

fetch_one() {   # <prefix> <tag> <sha>
  local prefix="$1" tag="$2" want="$3"
  local file="goldpin_${prefix}_m1_${4}.csv.gz"
  local out="$DEST/$file"

  if [ -f "$out" ]; then
    local got; got=$(sha256sum "$out" | cut -d' ' -f1)
    if [ "$got" = "$want" ]; then echo "== $file از قبل سالم است"; return 0; fi
    echo "== $file خراب/ناقص است، دوباره دانلود می‌شود"
    rm -f "$out"
  fi

  local repo url
  local repos=("$OWNER_REPO" "$MIRROR_REPO")
  if [ "$prefix" = "gbpusd" ]; then repos=("$FX_REPO"); fi
  for repo in "${repos[@]}"; do
    url="https://github.com/$repo/releases/download/$tag/$file"
    echo "== دانلود $file  ←  $repo"
    if curl -fL --retry 3 --retry-delay 2 -C - -o "$out" "$url"; then
      local got2; got2=$(sha256sum "$out" | cut -d' ' -f1)
      if [ "$got2" = "$want" ]; then echo "   ✓ SHA-256 تأیید شد"; return 0; fi
      echo "!! SHA-256 نامطابق از $repo (${got2:0:12}…)" >&2
      rm -f "$out"
    fi
  done
  echo "!! دانلود $file ناموفق بود" >&2
  return 1
}

mkdir -p "$DEST"
args=("$@")
[ ${#args[@]} -eq 0 ] && args=(xau)

# ── تجزیهٔ آرگومان‌ها: اول نمادها، بعد برش‌ها ──────────────────────────────────
#  هیچ آرگومانی ⇒ طلا (سازگاری با اسکریپت قدیمی). برشِ تنها بدون نماد هم یعنی طلا.
syms=(); ranges=()
for a in "${args[@]}"; do
  case "$a" in
    xau|gold)            syms+=(xauusd) ;;
    fx|gbpusd)           syms+=(gbpusd) ;;
    all)                 syms+=(xauusd gbpusd) ;;
    1y|2y|3y|10y|20y)    ranges+=("$a") ;;
    *) echo "!! آرگومان ناشناخته: $a  (xau | fx | all | 1y | 2y | 3y | 10y | 20y)" >&2; exit 2 ;;
  esac
done
[ ${#syms[@]}   -eq 0 ] && syms=(xauusd)
[ ${#ranges[@]} -eq 0 ] && ranges=(1y 2y 3y 10y 20y)

for sym in "${syms[@]}"; do
  for t in "${ranges[@]}"; do
    if [ "$sym" = "gbpusd" ]; then
      fetch_one gbpusd "$TAG_FX" "${SHA_FX[$t]}" "$t"
    else
      fetch_one xauusd "$TAG_XAU" "${SHA_XAU[$t]}" "$t"
    fi
  done
done

echo "تمام. فایل‌ها در $DEST"
