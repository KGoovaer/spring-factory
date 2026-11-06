#!/bin/sh
# Fast pre-commit checks, run in parallel. Full tests stay in `npm run check`
# (mvn verify). Compile is enough to catch a broken commit quickly.

logs=$(mktemp -d)
trap 'rm -rf "$logs"' EXIT

run() {
  name=$1
  shift
  ("$@" >"$logs/$name.log" 2>&1 || touch "$logs/$name.failed") &
}

run markdown npm run --silent format:check
run compile mvn -q -B -o test-compile
wait

status=0
for failed in "$logs"/*.failed; do
  [ -e "$failed" ] || continue
  name=$(basename "$failed" .failed)
  echo "✖ $name failed:"
  cat "$logs/$name.log"
  echo
  status=1
done
exit $status
