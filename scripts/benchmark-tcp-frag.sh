#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

FRAMEWORKS_DIR="$ROOT_DIR/frameworks"
BENCHMARK="$SCRIPT_DIR/benchmark.sh"
LOG_DIR="$ROOT_DIR/results/logs"
mkdir -p "$LOG_DIR"

TEST="tcp-frag"
REQUESTS_DIR="$ROOT_DIR/requests"
export CUSTOM_PROFILE="1|2|0-31,64-95|512,4096,16384|tcp-frag"
export CUSTOM_RAW="$REQUESTS_DIR/get-frag.raw,$REQUESTS_DIR/get-frag.raw,$REQUESTS_DIR/post-frag.raw,$REQUESTS_DIR/post-chunked-frag.raw,$REQUESTS_DIR/noise-frag.raw,$REQUESTS_DIR/cookie-frag.raw,$REQUESTS_DIR/manyheaders-frag.raw,$REQUESTS_DIR/body29-frag.raw"

# Template parser: extracts per-endpoint ok/err counts from gcannon output
# Templates: get-frag×2, post-frag×1, post-chunked-frag×1, noise-frag×1, cookie-frag×1, manyheaders-frag×1, body29-frag×1
read -r -d '' CUSTOM_TPL_PARSER << 'PARSER' || true
INPUT=$(cat)
tpl_line=$(echo "$INPUT" | grep -oP 'Per-template-ok: \K.*' || echo "")
tpl_all_line=$(echo "$INPUT" | grep -oP 'Per-template: \K.*' || echo "")
[ -z "$tpl_line" ] && exit 0
[ -z "$tpl_all_line" ] && tpl_all_line="$tpl_line"
IFS=',' read -ra ok <<< "$tpl_line"
IFS=',' read -ra all <<< "$tpl_all_line"
tf_get=$(( ${ok[0]:-0} + ${ok[1]:-0} ))
tf_post=${ok[2]:-0}
tf_chunked=${ok[3]:-0}
tf_noise=${ok[4]:-0}
tf_cookie=${ok[5]:-0}
tf_headers=${ok[6]:-0}
tf_body29=${ok[7]:-0}
tf_get_err=$(( ${all[0]:-0} + ${all[1]:-0} - tf_get ))
tf_post_err=$(( ${all[2]:-0} - tf_post ))
tf_chunked_err=$(( ${all[3]:-0} - tf_chunked ))
tf_noise_err=$(( ${all[4]:-0} - tf_noise ))
tf_cookie_err=$(( ${all[5]:-0} - tf_cookie ))
tf_headers_err=$(( ${all[6]:-0} - tf_headers ))
tf_body29_err=$(( ${all[7]:-0} - tf_body29 ))
printf ',\n  "tf_get": %d, "tf_get_err": %d,\n  "tf_post": %d, "tf_post_err": %d,\n  "tf_chunked": %d, "tf_chunked_err": %d,\n  "tf_noise": %d, "tf_noise_err": %d,\n  "tf_cookie": %d, "tf_cookie_err": %d,\n  "tf_headers": %d, "tf_headers_err": %d,\n  "tf_body29": %d, "tf_body29_err": %d' \
    "$tf_get" "$tf_get_err" "$tf_post" "$tf_post_err" "$tf_chunked" "$tf_chunked_err" \
    "$tf_noise" "$tf_noise_err" "$tf_cookie" "$tf_cookie_err" "$tf_headers" "$tf_headers_err" \
    "$tf_body29" "$tf_body29_err"
PARSER
export CUSTOM_TPL_PARSER

restore_mtu() {
    echo "[tune] Restoring loopback MTU to 65536..."
    sudo ip link set lo mtu 65536
    sudo ip route flush cache 2>/dev/null || true
}
trap restore_mtu EXIT

# Collect enabled frameworks that support this test
frameworks=()
for meta in "$FRAMEWORKS_DIR"/*/meta.json; do
    dir="$(dirname "$meta")"
    name="$(basename "$dir")"
    enabled=$(python3 -c "import json; m=json.load(open('$meta')); print(m.get('enabled',True) and '$TEST' in m.get('tests',[]))" 2>/dev/null || echo "False")
    if [ "$enabled" = "True" ]; then
        frameworks+=("$name")
    fi
done

total=${#frameworks[@]}
echo "=== $TEST: $total frameworks ==="
echo ""

echo "[tune] Setting loopback MTU to 69 for TCP fragmentation test..."
sudo ip link set lo mtu 69

passed=0
failed=0
failed_list=()

for i in "${!frameworks[@]}"; do
    fw="${frameworks[$i]}"
    n=$((i + 1))
    log="$LOG_DIR/${TEST}_${fw}.log"

    echo "[$n/$total] $fw"
    if "$BENCHMARK" "$fw" "$TEST" --save > "$log" 2>&1; then
        echo "         PASS"
        ((++passed))
    else
        echo "         FAIL (see $log)"
        ((++failed))
        failed_list+=("$fw")
    fi

    if [ "$n" -lt "$total" ]; then
        echo "         Waiting 15s..."
        sleep 15
    fi
done

echo ""
echo "=== $TEST done ==="
echo "Passed: $passed / $total"
if [ "$failed" -gt 0 ]; then
    echo "Failed: $failed — ${failed_list[*]}"
    exit 1
fi
