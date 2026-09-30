package org.rashid.store;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A value held in the keyspace.
 */
public sealed interface StoredValue permits StoredValue.StringValue {

    /**
     * A binary-safe string value. The array is treated as immutable and is not copied.
     */
    record StringValue(byte[] data) implements StoredValue {
        public StringValue {
            if (data == null) {
                throw new IllegalArgumentException("String value cannot be null");
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof StringValue other && Arrays.equals(data, other.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }

        @Override
        public String toString() {
            return "StringValue[" + new String(data, StandardCharsets.UTF_8) + "]";
        }
    }
}
