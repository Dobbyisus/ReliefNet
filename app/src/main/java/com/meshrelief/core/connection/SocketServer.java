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
            System.out.println("Server already running");
            return;
        }

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                System.out.println("ServerSocket started on port " + PORT);
                System.out.println("Waiting for client connection...");

                // Accept one client connection (2-device only)
                clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress());

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
                    System.out.println("MESSAGE RECEIVED: " + messageFromClient);
                    if (messageListener != null) {
                        messageListener.onMessageReceived(messageFromClient);
                    }
                }

                System.out.println("Client disconnected");
                if (messageListener != null) {
                    messageListener.onConnectionClosed();
                }

            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
                if (messageListener != null) {
                    messageListener.onError(e.getMessage());
                }
                e.printStackTrace();
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

                System.err.println(
                        "Client not connected"
                );

                return;
            }

            try {

                writer.println(message);

                System.out.println(
                        "MESSAGE SENT: "
                                + message
                );

            } catch (Exception e) {

                e.printStackTrace();
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
        } catch (Exception e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isClientConnected() {
        return clientSocket != null && !clientSocket.isClosed();
    }
}
