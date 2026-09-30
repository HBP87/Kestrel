package org.rashid.command;

import org.rashid.resp.RespValue;

import java.util.List;

public class EchoCommand implements Command {
    @Override
    public String name() {
        return "ECHO";
    }

    @Override
    public int arity() {
        return 2;
    }

    @Override
    public RespValue execute(List<byte[]> args) {
        return new RespValue.BulkString(args.get(1));
    }
}
