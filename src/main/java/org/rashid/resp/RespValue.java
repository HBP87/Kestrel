package org.rashid.resp;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public sealed interface RespValue permits RespValue.SimpleString, RespValue.Error, RespValue.BulkString, RespValue.Array {

    /**
     * Returns this value as a flat, binary-safe argument list, e.g. {@code ["SET", "foo", "bar"]} for a command array.
     */
    List<byte[]> toArgs() throws RespParseException;

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
        public List<byte[]> toArgs() {
            return List.of(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * A RESP simple error, e.g. {@code -ERR unknown command}. Only ever sent to clients, never parsed as a command.
     */
    record Error(String message) implements RespValue {
        public Error {
            if (message == null) {
                throw new IllegalArgumentException("Error message cannot be null");
            }
            if (message.indexOf('\r') >= 0 || message.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Error message cannot contain CR or LF");
            }
        }

        @Override
        public List<byte[]> toArgs() throws RespParseException {
            throw new RespParseException("Error cannot be a command argument");
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
        public List<byte[]> toArgs() throws RespParseException {
            if (isNull()) {
                throw new RespParseException("Null bulk string cannot be a command argument");
            }
            return List.of(value);
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
        public List<byte[]> toArgs() throws RespParseException {
            List<byte[]> args = new ArrayList<>(elements.size());
            for (RespValue element : elements) {
                if (element instanceof Array) {
                    throw new RespParseException("Nested arrays cannot be command arguments");
                }
                args.addAll(element.toArgs());
            }
            return args;
        }
    }
}
