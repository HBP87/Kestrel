#!/usr/bin/env bash
# Benchmarks Kestrel with redis-benchmark, optionally side by side with a real redis-server.
#
# Usage: bench/benchmark.sh [--compare-redis] [extra redis-benchmark args...]
#
# Tunables (env vars): REQUESTS (default 100000), CLIENTS (50), PIPELINE (1), DATA_SIZE (3),
#                      TESTS (ping_mbulk,set,get), KESTREL_PORT (6380), REDIS_PORT (6399)
set -euo pipefail

cd "$(dirname "$0")/.."

REQUESTS=${REQUESTS:-100000}
CLIENTS=${CLIENTS:-50}
PIPELINE=${PIPELINE:-1}
DATA_SIZE=${DATA_SIZE:-3}
# PING_INLINE is excluded: Kestrel only parses RESP arrays, not inline commands.
TESTS=${TESTS:-ping_mbulk,set,get}
KESTREL_PORT=${KESTREL_PORT:-6380}
REDIS_PORT=${REDIS_PORT:-6399}

compare_redis=false
if [[ "${1:-}" == "--compare-redis" ]]; then
    compare_redis=true
    shift
fi

command -v redis-benchmark >/dev/null || { echo "redis-benchmark not found (install redis-tools)" >&2; exit 1; }

pids=()
cleanup() {
    for pid in "${pids[@]}"; do kill "$pid" 2>/dev/null || true; done
    wait 2>/dev/null || true
}
trap cleanup EXIT

wait_for_port() {
    for _ in $(seq 1 100); do
        if (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; then return 0; fi
        sleep 0.1
    done
    echo "Server on port $1 did not start" >&2
    return 1
}

run_benchmark() {
    local name=$1 port=$2
    shift 2
    echo
    echo "=== $name (port $port): n=$REQUESTS c=$CLIENTS P=$PIPELINE d=$DATA_SIZE ==="
    # Warm up the JIT before measuring.
    redis-benchmark -p "$port" -t "$TESTS" -n "$((REQUESTS / 2))" -c "$CLIENTS" -P "$PIPELINE" -d "$DATA_SIZE" -q "$@" >/dev/null 2>&1
    redis-benchmark -p "$port" -t "$TESTS" -n "$REQUESTS" -c "$CLIENTS" -P "$PIPELINE" -d "$DATA_SIZE" -q "$@" 2>/dev/null \
        | tr '\r' '\n' | grep 'requests per second'
}

./gradlew --quiet installDist
build/install/Kestrel/bin/Kestrel "$KESTREL_PORT" >build/kestrel-bench.log 2>&1 &
pids+=($!)
wait_for_port "$KESTREL_PORT"
run_benchmark Kestrel "$KESTREL_PORT" "$@"

if $compare_redis; then
    command -v redis-server >/dev/null || { echo "redis-server not found" >&2; exit 1; }
    redis-server --port "$REDIS_PORT" --save "" --appendonly no >/dev/null &
    pids+=($!)
    wait_for_port "$REDIS_PORT"
    run_benchmark Redis "$REDIS_PORT" "$@"
fi
