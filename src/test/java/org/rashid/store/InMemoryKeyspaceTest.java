package org.rashid.store;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryKeyspaceTest {
    private final Keyspace keyspace = new InMemoryKeyspace();

    @Test
    void getMissingKeyIsEmpty() {
        assertTrue(keyspace.get(ByteKey.of("missing")).isEmpty());
    }

    @Test
    void setThenGet() {
        keyspace.set(ByteKey.of("foo"), new StoredValue.StringValue("bar".getBytes()));
        assertEquals(Optional.of(new StoredValue.StringValue("bar".getBytes())), keyspace.get(ByteKey.of("foo")));
    }

    @Test
    void setOverwrites() {
        keyspace.set(ByteKey.of("foo"), new StoredValue.StringValue("bar".getBytes()));
        keyspace.set(ByteKey.of("foo"), new StoredValue.StringValue("baz".getBytes()));
        assertEquals(Optional.of(new StoredValue.StringValue("baz".getBytes())), keyspace.get(ByteKey.of("foo")));
    }

    @Test
    void keysCompareByContent() {
        keyspace.set(new ByteKey(new byte[]{0, (byte) 0xff, 1}), new StoredValue.StringValue(new byte[]{42}));
        assertEquals(Optional.of(new StoredValue.StringValue(new byte[]{42})),
                keyspace.get(new ByteKey(new byte[]{0, (byte) 0xff, 1})));
    }

    @Test
    void mutatingSourceArrayDoesNotAffectKey() {
        byte[] raw = "foo".getBytes();
        keyspace.set(new ByteKey(raw), new StoredValue.StringValue("bar".getBytes()));
        raw[0] = 'x';
        assertTrue(keyspace.get(ByteKey.of("foo")).isPresent());
    }
}
