#!/usr/bin/env bash
# اجرای دسته‌ای spec ها روی دادهٔ ۲۰ ساله — هر spec یک CSV جدا (چون --trades بازنویسی می‌شود)
set -u
ROOT="$(cd "$(dirname "$0")" && pwd)"
J="${JAVA_HOME:-/home/user/.tools/jdk17}/bin/java"
CP="$ROOT/cli/build/install/cli/lib/*"
DATA=$ROOT/local-data/goldpin_xauusd_m1_20y.csv.gz
OUT=$ROOT/out/cx20y
mkdir -p "$OUT"
LIST="$1"
while IFS='|' read -r tag spec; do
  [ -z "${tag:-}" ] && continue
  if [ -s "$OUT/$tag.csv" ]; then echo "SKIP $tag"; continue; fi
  echo "=== RUN $tag  $(date +%H:%M:%S)"
  $J -Xmx1050m -XX:+UseSerialGC -cp "$CP" com.alisport.goldpin.cli.OptimizeKt \
      --data "$DATA" --spec "$spec,tag=$tag" \
      --trades "$OUT/${tag}_trades.csv" --out "$OUT/$tag.csv" 2>&1 | tail -3 | tee -a "$OUT/run.log"
done < "$LIST"
