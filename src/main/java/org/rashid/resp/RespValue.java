package org.rashid.resp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public sealed interface RespValue permits RespValue.SimpleString, RespValue.BulkString {

    record SimpleString(String value) implements RespValue {
        public SimpleString {
            if (value == null) {
                throw new IllegalArgumentException("Simple string cannot be null");
            }
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Simple string cannot contain CR or LF");
            }
        }
    }

    /**
     * A binary-safe string. A {@code null} value represents the RESP null bulk string.
     */
    record BulkString(byte[] value) implements RespValue {
        public static final BulkString NULL = new BulkString(null);

        public static BulkString of(String s) {
            return new BulkString(s.getBytes(StandardCharsets.UTF_8));
        }

        public boolean isNull() {
            return value == null;
        }

        public String asString() {
            return value == null ? null : new String(value, StandardCharsets.UTF_8);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BulkString other && Arrays.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return "BulkString[" + asString() + "]";
        }
    }
}
