package org.rashid.command;

import org.rashid.resp.RespValue;
import org.rashid.store.ByteKey;
import org.rashid.store.Keyspace;
import org.rashid.store.StoredValue;

import java.util.List;

public class SetCommand implements Command {
    private static final RespValue OK = new RespValue.SimpleString("OK");

    private final Keyspace keyspace;

    public SetCommand(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    @Override
    public String name() {
        return "SET";
    }

    @Override
    public int arity() {
        return -3; //means at least 3 tokens
    }

    @Override
    public RespValue execute(List<byte[]> args) {
        // Options (EX, PX, NX, XX, ...) are not supported yet.
        if (args.size() != 3) {
            return new RespValue.Error("ERR syntax error");
        }
        keyspace.set(new ByteKey(args.get(1)), new StoredValue.StringValue(args.get(2)));
        return OK;
    }
}
