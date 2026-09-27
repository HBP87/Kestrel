package org.rashid;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import org.rashid.resp.*;

@Slf4j
public class Kestrel {

    public static void main(String[] args) {
        Kestrel kestrel = new Kestrel();
        kestrel.run();
    }

    public void run() {
        log.info("Starting kestrel server");
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6380;

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            log.info("Listening on port: {}", port);
            while (true) {
                clientSocket = serverSocket.accept();
                Socket cs = clientSocket;
                CompletableFuture.runAsync(() -> {
                    try {
                        handleClient(cs);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            log.error("IOException", e);
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                log.error("IOException", e);
            }
        }
    }

    private void handleClient(Socket clientSocket) throws Exception {
        RespParser parser = new RespParser(clientSocket.getInputStream());
        OutputStream out = clientSocket.getOutputStream();
        RespValue value;
        while ((value = parser.read()) != null) {
            RespEncoder.write(out, new RespValue.SimpleString("PONG"));
        }
    }
}
