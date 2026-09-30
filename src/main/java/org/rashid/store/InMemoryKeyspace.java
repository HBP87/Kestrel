package org.rashid.store;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe keyspace shared by all client connections.
 */
public class InMemoryKeyspace implements Keyspace {
    private final Map<ByteKey, StoredValue> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<StoredValue> get(ByteKey key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public void set(ByteKey key, StoredValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        entries.put(key, value);
    }
}
