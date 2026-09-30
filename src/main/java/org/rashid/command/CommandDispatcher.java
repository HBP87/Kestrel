package org.rashid.command;

import org.rashid.resp.RespValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Routes a parsed command to its {@link Command} after validating the name and arity.
 * <p>
 * {@code args} is the full argv, including the command name at index 0 (e.g. {@code ["SET", "foo", "bar"]}).
 * Arity follows the Redis convention and counts the command name: a positive value means exactly that many
 * arguments, a negative value means at least {@code |arity|}.
 */
public class CommandDispatcher {
    private final Map<String, Command> commands;

    public CommandDispatcher(List<Command> commands) {
        Map<String, Command> byName = new HashMap<>();
        for (Command command : commands) {
            String name = command.name().toUpperCase(Locale.ROOT);
            if (byName.putIfAbsent(name, command) != null) {
                throw new IllegalArgumentException("Duplicate command: " + name);
            }
        }
        this.commands = Map.copyOf(byName);
    }

    public RespValue dispatch(List<byte[]> args) {
        if (args.isEmpty()) {
            return new RespValue.Error("ERR empty command");
        }
        String name = new String(args.getFirst(), StandardCharsets.UTF_8);
        Command command = commands.get(name.toUpperCase(Locale.ROOT));
        if (command == null) {
            return new RespValue.Error("ERR unknown command '" + name + "'");
        }
        if (!arityMatches(command.arity(), args.size())) {
            return new RespValue.Error(
                    "ERR wrong number of arguments for '" + command.name().toLowerCase(Locale.ROOT) + "' command");
        }
        return command.execute(args);
    }

    private static boolean arityMatches(int arity, int argc) {
        return arity >= 0 ? argc == arity : argc >= -arity;
    }
}
