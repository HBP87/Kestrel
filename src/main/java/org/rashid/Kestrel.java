package org.rashid;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.rashid.command.*;
import org.rashid.resp.*;
import org.rashid.store.InMemoryKeyspace;
import org.rashid.store.Keyspace;

@Slf4j
public class Kestrel implements AutoCloseable {
    public static final int DEFAULT_PORT = 6380;

    private final int requestedPort;
    private final CommandDispatcher dispatcher;
    private final ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * @param port the port to listen on, or {@code 0} to pick a free one (see {@link #port()})
     */
    public Kestrel(int port) throws IOException {
        this.requestedPort = port;
        this.serverSocket = new ServerSocket();
        Keyspace keyspace = new InMemoryKeyspace();
        this.dispatcher = new CommandDispatcher(List.of(
                new PingCommand(),
                new EchoCommand(),
                new GetCommand(keyspace),
                new SetCommand(keyspace)
        ));
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Kestrel(port).start();
    }

    /**
     * Binds the server socket and starts accepting clients on a background thread. Returns once the socket is bound.
     */
    public void start() throws IOException {
        log.info("Starting kestrel server");
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(requestedPort));
        log.info("Listening on port: {}", port());
        Thread.ofPlatform().name("kestrel-acceptor").start(this::acceptLoop);
    }

    /**
     * The port the server is bound to. Only meaningful after {@link #start()}.
     */
    public int port() {
        return serverSocket.getLocalPort();
    }

    /**
     * Stops accepting new clients and interrupts the handlers of connected ones.
     */
    @Override
    public void close() throws IOException {
        serverSocket.close();
        executor.shutdownNow();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    log.error("IOException", e);
                }
                return;
            }
            executor.submit(() -> {
                try (clientSocket) {
                    handleClient(clientSocket);
                } catch (SocketException e) {
                    // Clients that close with SO_LINGER=0 (e.g. Jedis) send an RST instead of a FIN, which
                    // surfaces here as "Connection reset". That is an ordinary disconnect, not a server error.
                    log.debug("Client {} disconnected: {}", clientSocket.getRemoteSocketAddress(), e.getMessage());
                } catch (Exception e) {
                    if (!serverSocket.isClosed()) {
                        log.error("Error handling client {}", clientSocket.getRemoteSocketAddress(), e);
                    }
                }
            });
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
