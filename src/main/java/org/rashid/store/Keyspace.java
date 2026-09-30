package org.rashid.store;

import java.util.Optional;

public interface Keyspace {
    Optional<StoredValue> get(ByteKey key);

    /**
     * Stores {@code value} under {@code key}, replacing any existing value regardless of its type.
     */
    void set(ByteKey key, StoredValue value);
}
