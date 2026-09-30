package org.rashid.command;

import org.rashid.resp.RespValue;
import org.rashid.store.ByteKey;
import org.rashid.store.Keyspace;
import org.rashid.store.StoredValue;

import java.util.List;

public class GetCommand implements Command {
    private final Keyspace keyspace;

    public GetCommand(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    @Override
    public String name() {
        return "GET";
    }

    @Override
    public int arity() {
        return 2; //fixed two tokens
    }

    @Override
    public RespValue execute(List<byte[]> args) {
        return keyspace.get(new ByteKey(args.get(1)))
                .<RespValue>map(value -> switch (value) {
                    case StoredValue.StringValue s -> new RespValue.BulkString(s.data());
                })
                .orElse(RespValue.BulkString.NULL);
    }
}
