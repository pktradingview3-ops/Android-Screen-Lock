#!/usr/bin/env bash
# Runs Gradle. On failure, copies the key error lines (compile errors, failed tests with
# messages) into GitHub annotations, so they are visible even when raw logs are not.
set -o pipefail

LOG=/tmp/gradle-run.log
gradle --no-daemon "$@" 2>&1 | tee "$LOG"
rc=${PIPESTATUS[0]}

if [ "$rc" -ne 0 ]; then
  echo "::error::Gradle failed with exit code $rc"
  grep -E "^e: |What went wrong|Could not |Caused by|FAILED" "$LOG" | head -40 \
    | while IFS= read -r line; do echo "::error::${line}"; done

  python3 - << 'PY'
import glob
import xml.etree.ElementTree as ET

files = glob.glob("app/build/outputs/androidTest-results/**/*.xml", recursive=True)
files += glob.glob("app/build/test-results/**/*.xml", recursive=True)
total = failed = 0
for f in files:
    try:
        root = ET.parse(f).getroot()
    except Exception as exc:  # keep going, report what we can
        print(f"::error::could not parse {f}: {exc}")
        continue
    for tc in root.iter("testcase"):
        total += 1
        for node in list(tc.findall("failure")) + list(tc.findall("error")):
            failed += 1
            lines = [l.strip() for l in (node.text or "").splitlines() if l.strip()][:4]
            msg = (node.get("message") or "")[:250]
            print(f"::error::TEST FAILED {tc.get('classname')}.{tc.get('name')}: {msg} || " + " | ".join(lines))
print(f"::notice::Test results: {total} run, {failed} failed")
PY
fi
exit "$rc"
