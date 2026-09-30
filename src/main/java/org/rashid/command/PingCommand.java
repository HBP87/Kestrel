package org.rashid.command;

import org.rashid.resp.RespValue;

import java.util.List;

public class PingCommand implements Command {
    private static final RespValue PONG = new RespValue.SimpleString("PONG");

    @Override
    public String name() {
        return "PING";
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public RespValue execute(List<byte[]> args) {
        return PONG;
    }
}
