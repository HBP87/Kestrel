package org.rashid.resp;

import java.io.IOException;

public class RespParseException extends IOException {
    public RespParseException(String message) {
        super(message);
    }
}
