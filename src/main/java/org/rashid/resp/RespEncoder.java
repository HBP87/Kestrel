package org.rashid.resp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class RespEncoder {
    private static final byte[] CRLF = {'\r', '\n'};

    private RespEncoder() {
    }

    public static byte[] encode(RespValue value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        switch (value) {
            case RespValue.SimpleString s -> {
                out.write('+');
                out.writeBytes(s.value().getBytes(StandardCharsets.UTF_8));
                out.writeBytes(CRLF);
            }
            case RespValue.Error e -> {
                out.write('-');
                out.writeBytes(e.message().getBytes(StandardCharsets.UTF_8));
                out.writeBytes(CRLF);
            }
            case RespValue.BulkString b when b.isNull() -> out.writeBytes("$-1\r\n".getBytes(StandardCharsets.US_ASCII));
            case RespValue.BulkString b -> {
                out.write('$');
                out.writeBytes(Integer.toString(b.value().length).getBytes(StandardCharsets.US_ASCII));
                out.writeBytes(CRLF);
                out.writeBytes(b.value());
                out.writeBytes(CRLF);
            }
            case RespValue.Array a -> {
                out.write('*');
                out.writeBytes(Integer.toString(a.elements().size()).getBytes(StandardCharsets.US_ASCII));
                out.writeBytes(CRLF);
                for (RespValue element : a.elements()) {
                    out.writeBytes(encode(element));
                }
            }
        }
        return out.toByteArray();
    }

    public static void write(OutputStream out, RespValue value) throws IOException {
        out.write(encode(value));
    }
}
