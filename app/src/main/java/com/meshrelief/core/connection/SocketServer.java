package com.meshrelief.core.connection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Bidirectional server socket for 2-device P2P.
 * Accepts one client connection and can send/receive messages.
 */
public class SocketServer {

    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter writer;
    private BufferedReader reader;
    private boolean isRunning = false;
    private Thread serverThread;
    private MessageListener messageListener;

    public SocketServer(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    /**
     * Starts the server socket in a background thread.
     */
    public void start() {
        if (isRunning) {
            return;
        }

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;

                // Accept one client connection (2-device only)
                clientSocket = serverSocket.accept();

                // Setup reader and writer for bidirectional communication
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                reader = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream())
                );

                if (messageListener != null) {
                    messageListener.onConnectionEstablished();
                }

                // Read messages from client in a loop
                String messageFromClient;
                while ((messageFromClient = reader.readLine()) != null && isRunning) {
                    if (messageListener != null) {
                        messageListener.onMessageReceived(messageFromClient);
                    }
                }

                if (messageListener != null) {
                    messageListener.onConnectionClosed();
                }

            } catch (Exception e) {
                if (messageListener != null) {
                    messageListener.onError(e.getMessage());
                }
            } finally {
                stop();
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Sends a message to the connected client.
     */
    public void sendMessage(String message) {

        new Thread(() -> {

            if (!isRunning || writer == null) {
                return;
            }

            try {

                writer.println(message);
            } catch (Exception e) {
                // Ignore send failures; connection lifecycle handles user-facing errors.
            }

        }).start();
    }

    /**
     * Stops the server socket.
     */
    public void stop() {
        isRunning = false;
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (Exception e) {}
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isClientConnected() {
        return clientSocket != null && !clientSocket.isClosed();
    }
}
