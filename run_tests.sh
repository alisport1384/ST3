#!/usr/bin/env bash
# اجرای آزمون‌های :engine بدون Gradle (Gradle در این سندباکس OOM می‌شود).
#   JAVA_HOME=... KOTLINC=... bash run_tests.sh [نام کلاس ...]
set -u
KC="${KOTLINC:-/home/user/.cache/tools/kotlinc}"
export JAVA_HOME="${JAVA_HOME:-/home/user/.cache/tools/jdk17}"
CONSOLE="${JUNIT_CONSOLE:-/home/user/.cache/tools/junit-console.jar}"
HEAP="${HEAP:-900m}"
CP="/tmp/engclasses:/tmp/testclasses:$KC/lib/kotlin-test.jar:$KC/lib/kotlin-test-junit5.jar:$KC/lib/kotlin-stdlib.jar"
SEL=()
for c in "$@"; do SEL+=(--select-class=com.alisport.goldpin.core."$c"); done
[ ${#SEL[@]} -eq 0 ] && SEL=(--select-package=com.alisport.goldpin.core)
cd "$(dirname "$0")"
"$JAVA_HOME/bin/java" -Xmx"$HEAP" -jar "$CONSOLE" execute -cp "$CP" \
  "${SEL[@]}" --details=summary --disable-ansi-colors --reports-dir=/tmp/junit-reports
