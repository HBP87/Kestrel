package org.rashid.resp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads RESP values from a stream. Currently supports simple strings and bulk strings.
 */
public class RespParser {
    private final InputStream in;

    public RespParser(InputStream in) {
        this.in = in instanceof BufferedInputStream ? in : new BufferedInputStream(in);
    }

    /**
     * Reads the next value, or returns {@code null} if the stream ends cleanly before a value starts.
     */
    public RespValue read() throws IOException {
        int type = in.read();
        if (type == -1) {
            return null;
        }
        return switch (type) {
            case '+' -> new RespValue.SimpleString(new String(readLine(), StandardCharsets.UTF_8));
            case '$' -> readBulkString();
            default -> throw new RespParseException("Unknown RESP type: '" + (char) type + "'");
        };
    }

    private RespValue.BulkString readBulkString() throws IOException {
        String lengthLine = new String(readLine(), StandardCharsets.US_ASCII);
        int length;
        try {
            length = Integer.parseInt(lengthLine);
        } catch (NumberFormatException e) {
            throw new RespParseException("Invalid bulk string length: " + lengthLine);
        }
        if (length == -1) {
            return RespValue.BulkString.NULL;
        }
        if (length < 0) {
            throw new RespParseException("Invalid bulk string length: " + length);
        }
        byte[] data = in.readNBytes(length);
        if (data.length != length) {
            throw new RespParseException("Unexpected end of stream in bulk string");
        }
        expectCrlf();
        return new RespValue.BulkString(data);
    }

    private byte[] readLine() throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            switch (b) {
                case -1 -> throw new RespParseException("Unexpected end of stream, expected CRLF");
                case '\n' -> throw new RespParseException("Unexpected LF without preceding CR");
                case '\r' -> {
                    if (in.read() != '\n') {
                        throw new RespParseException("Expected LF after CR");
                    }
                    return line.toByteArray();
                }
                default -> line.write(b);
            }
        }
    }

    private void expectCrlf() throws IOException {
        if (in.read() != '\r' || in.read() != '\n') {
            throw new RespParseException("Expected CRLF");
        }
    }
}
