#!/bin/bash

PASS=0
FAIL=0
RESULTS=()

run_test() {
  local name="$1"
  local dir="$2"
  local cmd="$3"

  echo ">>> Testing: $name"
  (cd "$dir" && eval "$cmd")
  if [ $? -eq 0 ]; then
    RESULTS+=("PASS  $name")
    ((PASS++))
  else
    RESULTS+=("FAIL  $name")
    ((FAIL++))
  fi
  echo
}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

run_test "bare-minimum Maven"       "$SCRIPT_DIR/spring-boot/bare-minimum/maven"   "mvn test"
run_test "bare-minimum Gradle"      "$SCRIPT_DIR/spring-boot/bare-minimum/gradle"  "./gradlew test --stacktrace"
run_test "full-stack Maven"         "$SCRIPT_DIR/spring-boot/full-stack/maven"     "mvn test"
run_test "full-stack Gradle"        "$SCRIPT_DIR/spring-boot/full-stack/gradle"    "./gradlew test --stacktrace"
run_test "full-stack-kotlin Gradle" "$SCRIPT_DIR/spring-boot/full-stack-kotlin"    "./gradlew test --stacktrace"

echo "================================"
echo "Results:"
for r in "${RESULTS[@]}"; do
  echo "  $r"
done
echo "--------------------------------"
echo "  Passed: $PASS  Failed: $FAIL"
echo "================================"

[ $FAIL -eq 0 ]
