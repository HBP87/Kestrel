package org.rashid;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.SetParams;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Talks to a real {@link Kestrel} server over TCP using the Jedis client.
 */
class KestrelJedisIT {
    private static Kestrel server;

    private Jedis jedis;

    @BeforeAll
    static void startServer() throws Exception {
        server = new Kestrel(0);
        server.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.close();
    }

    @BeforeEach
    void connect() {
        jedis = newClient();
    }

    @AfterEach
    void disconnect() {
        jedis.close();
    }

    private static Jedis newClient() {
        return new Jedis("localhost", server.port());
    }

    /** Keys are unique per test because all tests share one server. */
    private static String key(String name) {
        return name + ":" + UUID.randomUUID();
    }

    @Test
    void ping() {
        assertEquals("PONG", jedis.ping());
    }

    @Test
    void echo() {
        assertEquals("hello world", jedis.echo("hello world"));
    }

    @Test
    void setThenGet() {
        String key = key("foo");
        assertEquals("OK", jedis.set(key, "bar"));
        assertEquals("bar", jedis.get(key));
    }

    @Test
    void getMissingKeyReturnsNull() {
        assertNull(jedis.get(key("missing")));
    }

    @Test
    void setOverwritesExistingValue() {
        String key = key("foo");
        jedis.set(key, "bar");
        jedis.set(key, "baz");
        assertEquals("baz", jedis.get(key));
    }

    @Test
    void valuesAreBinarySafe() {
        byte[] key = key("bin").getBytes(StandardCharsets.UTF_8);
        byte[] value = {0, (byte) 0xff, '\r', '\n', '$', '*'};
        jedis.set(key, value);
        assertArrayEquals(value, jedis.get(key));
    }

    @Test
    void largeValueRoundTrips() {
        String key = key("large");
        String value = "x".repeat(1024 * 1024);
        jedis.set(key, value);
        assertEquals(value, jedis.get(key));
    }

    @Test
    void unicodeRoundTrips() {
        String key = key("ключ-🔑");
        jedis.set(key, "значение-✓");
        assertEquals("значение-✓", jedis.get(key));
    }

    @Test
    void writesAreVisibleToOtherConnections() {
        String key = key("shared");
        jedis.set(key, "v");
        try (Jedis other = newClient()) {
            assertEquals("v", other.get(key));
        }
    }

    @Test
    void errorsAreReportedAndConnectionStaysUsable() {
        JedisDataException unsupported = assertThrows(JedisDataException.class,
                () -> jedis.set(key("foo"), "bar", SetParams.setParams().ex(10)));
        assertEquals("ERR syntax error", unsupported.getMessage());

        JedisDataException unknown = assertThrows(JedisDataException.class, () -> jedis.dbSize());
        assertTrue(unknown.getMessage().startsWith("ERR unknown command"), unknown.getMessage());

        assertEquals("PONG", jedis.ping());
    }

    @Test
    void pipelinedCommandsAreAnsweredInOrder() {
        String prefix = key("pipe");
        int n = 1_000;
        List<Response<String>> gets = new ArrayList<>(n);
        try (Pipeline pipeline = jedis.pipelined()) {
            for (int i = 0; i < n; i++) {
                pipeline.set(prefix + i, "v" + i);
            }
            for (int i = 0; i < n; i++) {
                gets.add(pipeline.get(prefix + i));
            }
            pipeline.sync();
        }
        for (int i = 0; i < n; i++) {
            assertEquals("v" + i, gets.get(i).get());
        }
    }

    @Test
    void concurrentClients() throws Exception {
        int clients = 50;
        int opsPerClient = 200;
        String prefix = key("concurrent");
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int c = 0; c < clients; c++) {
            int client = c;
            tasks.add(() -> {
                try (Jedis j = newClient()) {
                    for (int i = 0; i < opsPerClient; i++) {
                        String key = prefix + ":" + client + ":" + i;
                        String value = client + "-" + i;
                        assertEquals("OK", j.set(key, value));
                        assertEquals(value, j.get(key));
                    }
                }
                return null;
            });
        }
        try (ExecutorService executor = Executors.newFixedThreadPool(clients)) {
            for (Future<Void> result : executor.invokeAll(tasks)) {
                result.get();
            }
        }
    }
}
