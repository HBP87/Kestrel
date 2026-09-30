package org.rashid.resp;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public sealed interface RespValue permits RespValue.SimpleString, RespValue.BulkString, RespValue.Array {

    /**
     * Returns this value as a flat list of string tokens, e.g. {@code ["ECHO", "Hey"]} for a command array.
     */
    List<String> toTokens() throws RespParseException;

    record SimpleString(String value) implements RespValue {
        public SimpleString {
            if (value == null) {
                throw new IllegalArgumentException("Simple string cannot be null");
            }
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Simple string cannot contain CR or LF");
            }
        }

        @Override
        public List<String> toTokens() {
            return List.of(value);
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
        public List<String> toTokens() throws RespParseException {
            if (isNull()) {
                throw new RespParseException("Null bulk string cannot be a token");
            }
            return List.of(asString());
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

    record Array(List<RespValue> elements) implements RespValue {
        public Array {
            if (elements == null) {
                throw new IllegalArgumentException("Array elements cannot be null");
            }
            elements = List.copyOf(elements);
        }

        @Override
        public List<String> toTokens() throws RespParseException {
            List<String> tokens = new ArrayList<>(elements.size());
            for (RespValue element : elements) {
                if (element instanceof Array) {
                    throw new RespParseException("Nested arrays cannot be tokens");
                }
                tokens.addAll(element.toTokens());
            }
            return tokens;
        }
    }
}
