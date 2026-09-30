package org.rashid.store;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A binary-safe keyspace key. Wraps {@code byte[]} so that keys compare by content rather than by identity.
 */
public record ByteKey(byte[] bytes) {
    public ByteKey {
        if (bytes == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        // Copy so that a caller mutating its array cannot corrupt the key's hash inside the map.
        bytes = bytes.clone();
    }

    public static ByteKey of(String s) {
        return new ByteKey(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ByteKey other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "ByteKey[" + new String(bytes, StandardCharsets.UTF_8) + "]";
    }
}
