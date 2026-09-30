# Kestrel

A small Redis-compatible server written in Java 21. It speaks the RESP protocol, so standard Redis clients
(`redis-cli`, Jedis, etc.) can talk to it, and it handles each connection on a virtual thread.

## Supported commands

| Command | Notes |
|---|---|
| `PING` | Replies `PONG` |
| `ECHO message` | |
| `SET key value` | Options (`EX`, `PX`, `NX`, `XX`, ...) are not supported yet |
| `GET key` | |

Keys and values are binary-safe. Data lives in memory only.

## Running

```bash
./gradlew run                          # listens on port 6380
./gradlew installDist && build/install/Kestrel/bin/Kestrel 7000   # custom port

redis-cli -p 6380 SET foo bar
redis-cli -p 6380 GET foo
```

## Testing

```bash
./gradlew test               # unit tests
./gradlew integrationTest    # end-to-end tests against a live server using Jedis
./gradlew check              # both
```

## Benchmarking

`bench/benchmark.sh` starts Kestrel and runs `redis-benchmark` against it (requires `redis-tools`).
Pass `--compare-redis` to run the same load against a local `redis-server` as well.

```bash
bench/benchmark.sh --compare-redis
PIPELINE=16 REQUESTS=500000 bench/benchmark.sh --compare-redis
```

Tunables: `REQUESTS`, `CLIENTS`, `PIPELINE`, `DATA_SIZE`, `TESTS`, `KESTREL_PORT`, `REDIS_PORT`. Any extra
arguments are passed through to `redis-benchmark`.

## Layout

```
src/main/java/org/rashid/
├── Kestrel.java     server: accepts connections, one virtual thread per client
├── resp/            RESP parser and encoder
├── command/         command implementations and the dispatcher (name + arity checks)
└── store/           in-memory keyspace
src/integrationTest/ Jedis-based end-to-end tests
bench/               benchmark script
```
