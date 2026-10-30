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
#   ./tools/fetch_datasets.sh oil             # هر پنج برش نفت
#   ./tools/fetch_datasets.sh all             # هر سه نماد، هر پانزده فایل
#
# ⚠ SHAها را هرگز دستی ننویسید: از `datasets/fx_gbpusd_summary.json` و
#   `datasets/*.meta.json` کپی می‌شوند و آزمون‌های `Repository*DataTest` همان‌ها را قفل
#   کرده‌اند. SHA اشتباه یعنی اپ فایل سالم را «خراب» می‌داند و هرگز تأیید نمی‌کند.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

OWNER_REPO="${GOLDPIN_DATA_REPO:-alisport1384/ST3}"
MIRROR_REPO="${GOLDPIN_DATA_MIRROR:-alisport1384/ST2}"
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

# نفت — تگ data-oil-v1 (هش‌های فایل‌های موجود در Release اصلی)
TAG_OIL="data-oil-v1"
declare -A SHA_OIL=(
  [1y]=616cb8f68ccce3793c529282a86e9500f1e16ea6e54b55376f07e44150bc8590
  [2y]=78e4a8fdbce2fcd4a39a37f01f3e3a22ffb73cb70aa38cb25594c9447f7d6d0a
  [3y]=9394e460c705bdcddc2b3a843beb8ff3769fb72e436565bb8cc84521afc6b21b
  [10y]=7c1f7cb1be6bbb3033be3d6c96555a6cebc21b151e4bda1b2cb8fb0a352465ec
  [20y]=cee6b304a900e6f0bc2d3a33ee201523c2905714d9ece805aeabfc9028ab6313
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
    oil|usoil)           syms+=(usoil) ;;
    all)                 syms+=(xauusd usoil gbpusd) ;;
    1y|2y|3y|10y|20y)    ranges+=("$a") ;;
    *) echo "!! آرگومان ناشناخته: $a  (xau | oil | fx | all | 1y | 2y | 3y | 10y | 20y)" >&2; exit 2 ;;
  esac
done
[ ${#syms[@]}   -eq 0 ] && syms=(xauusd)
[ ${#ranges[@]} -eq 0 ] && ranges=(1y 2y 3y 10y 20y)

for sym in "${syms[@]}"; do
  for t in "${ranges[@]}"; do
    case "$sym" in
      gbpusd) fetch_one gbpusd "$TAG_FX" "${SHA_FX[$t]}" "$t" ;;
      usoil)  fetch_one usoil "$TAG_OIL" "${SHA_OIL[$t]}" "$t" ;;
      xauusd) fetch_one xauusd "$TAG_XAU" "${SHA_XAU[$t]}" "$t" ;;
    esac
  done
done

echo "تمام. فایل‌ها در $DEST"
