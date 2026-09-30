package org.rashid;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.rashid.resp.*;

@Slf4j
public class Kestrel {

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
            List<String> tokens = value.toTokens();
            log.info("Client sent: {}", tokens);
            if (tokens.isEmpty()) {
                RespEncoder.write(out, new RespValue.SimpleString("ERR empty command"));
                continue;
            }
            String command = tokens.getFirst().toUpperCase();
            switch (command) {
                case "PING" -> RespEncoder.write(out, new RespValue.SimpleString("PONG"));
                case "ECHO" -> handleEchoCommand(tokens, out);
                default ->
                        RespEncoder.write(out, new RespValue.SimpleString("ERR unknown command '" + tokens.getFirst() + "'"));
            }
        }
    }

    private static void handleEchoCommand(List<String> tokens, OutputStream out) throws IOException {
        if (tokens.size() != 2) {
            RespEncoder.write(out, new RespValue.SimpleString("ERR wrong number of arguments for 'echo' command"));
        } else {
            RespEncoder.write(out, RespValue.BulkString.of(tokens.get(1)));
        }
    }
}
