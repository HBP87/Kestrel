package org.rashid.command;

import org.rashid.resp.RespValue;

import java.util.List;

public interface Command {
    String name();

    int arity();

    RespValue execute(List<byte[]> args);
}
