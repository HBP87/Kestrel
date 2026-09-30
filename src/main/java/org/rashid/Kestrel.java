package org.rashid;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.rashid.command.*;
import org.rashid.resp.*;
import org.rashid.store.InMemoryKeyspace;
import org.rashid.store.Keyspace;

@Slf4j
public class Kestrel {
    private final CommandDispatcher dispatcher;

    public Kestrel() {
        Keyspace keyspace = new InMemoryKeyspace();
        this.dispatcher = new CommandDispatcher(List.of(
                new PingCommand(),
                new EchoCommand(),
                new GetCommand(keyspace),
                new SetCommand(keyspace)
        ));
    }

    public static void main(String[] args) {
        Kestrel kestrel = new Kestrel();
        kestrel.run();
    }

    public void run() {
        log.info("Starting kestrel server");
        int port = 6380;

        try (ServerSocket serverSocket = new ServerSocket();
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            log.info("Listening on port: {}", port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> {
                    try (clientSocket) {
                        handleClient(clientSocket);
                    } catch (Exception e) {
                        log.error("Error handling client {}", clientSocket.getRemoteSocketAddress(), e);
                    }
                });
            }
        } catch (IOException e) {
            log.error("IOException", e);
        }
    }

    private void handleClient(Socket clientSocket) throws Exception {
        RespParser parser = new RespParser(clientSocket.getInputStream());
        OutputStream out = clientSocket.getOutputStream();
        RespValue value;
        while ((value = parser.read()) != null) {
            List<byte[]> args = value.toArgs();
            if (log.isDebugEnabled()) {
                log.debug("Client sent: {}", args.stream().map(a -> new String(a, StandardCharsets.UTF_8)).toList());
            }
            RespEncoder.write(out, dispatcher.dispatch(args));
        }
    }
}
