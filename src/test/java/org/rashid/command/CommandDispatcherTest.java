package org.rashid.command;

import org.junit.jupiter.api.Test;
import org.rashid.resp.RespValue;
import org.rashid.store.InMemoryKeyspace;
import org.rashid.store.Keyspace;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandDispatcherTest {
    private final Keyspace keyspace = new InMemoryKeyspace();
    private final CommandDispatcher dispatcher = new CommandDispatcher(List.of(
            new PingCommand(),
            new EchoCommand(),
            new GetCommand(keyspace),
            new SetCommand(keyspace)
    ));

    private RespValue run(String... args) {
        return dispatcher.dispatch(Arrays.stream(args).map(a -> a.getBytes(StandardCharsets.UTF_8)).toList());
    }

    @Test
    void setThenGet() {
        assertEquals(new RespValue.SimpleString("OK"), run("SET", "foo", "bar"));
        assertEquals(RespValue.BulkString.of("bar"), run("GET", "foo"));
    }

    @Test
    void getMissingKeyReturnsNull() {
        assertEquals(RespValue.BulkString.NULL, run("GET", "missing"));
    }

    @Test
    void setOverwritesExistingValue() {
        run("SET", "foo", "bar");
        run("SET", "foo", "baz");
        assertEquals(RespValue.BulkString.of("baz"), run("GET", "foo"));
    }

    @Test
    void emptyKeyAndValueAreAllowed() {
        assertEquals(new RespValue.SimpleString("OK"), run("SET", "", ""));
        assertEquals(RespValue.BulkString.of(""), run("GET", ""));
    }

    @Test
    void valuesAreBinarySafe() {
        byte[] value = {0, (byte) 0xff, '\r', '\n'};
        dispatcher.dispatch(List.of("SET".getBytes(), "bin".getBytes(), value));
        assertEquals(new RespValue.BulkString(value), run("GET", "bin"));
    }

    @Test
    void commandNamesAreCaseInsensitiveButKeysAreNot() {
        run("set", "Foo", "bar");
        assertEquals(RespValue.BulkString.of("bar"), run("gEt", "Foo"));
        assertEquals(RespValue.BulkString.NULL, run("GET", "foo"));
    }

    @Test
    void wrongArity() {
        assertEquals(new RespValue.Error("ERR wrong number of arguments for 'get' command"), run("GET"));
        assertEquals(new RespValue.Error("ERR wrong number of arguments for 'get' command"), run("GET", "a", "b"));
        assertEquals(new RespValue.Error("ERR wrong number of arguments for 'set' command"), run("SET", "foo"));
    }

    @Test
    void unsupportedSetOptionIsSyntaxError() {
        assertEquals(new RespValue.Error("ERR syntax error"), run("SET", "foo", "bar", "EX", "10"));
        assertEquals(RespValue.BulkString.NULL, run("GET", "foo"));
    }

    @Test
    void unknownAndEmptyCommands() {
        assertEquals(new RespValue.Error("ERR unknown command 'NOPE'"), run("NOPE"));
        assertEquals(new RespValue.Error("ERR empty command"), dispatcher.dispatch(List.of()));
    }

    @Test
    void pingAndEcho() {
        assertEquals(new RespValue.SimpleString("PONG"), run("PING"));
        assertEquals(RespValue.BulkString.of("hey"), run("ECHO", "hey"));
    }

    @Test
    void duplicateCommandNamesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CommandDispatcher(List.of(new PingCommand(), new PingCommand())));
    }
}
