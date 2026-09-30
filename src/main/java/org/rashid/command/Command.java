package org.rashid.command;

import org.rashid.resp.RespValue;

import java.util.List;

public interface Command {
    /**
     * The command name, matched case-insensitively.
     */
    String name();

    /**
     * Number of arguments, counting the command name. A negative value means at least {@code |arity|}.
     */
    int arity();

    /**
     * Runs the command. {@code args} includes the command name at index 0 and has already been checked
     * against {@link #arity()} by the {@link CommandDispatcher}.
     */
    RespValue execute(List<byte[]> args);
}
