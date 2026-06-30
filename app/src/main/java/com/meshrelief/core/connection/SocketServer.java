package com.meshrelief.core.connection;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.PacketSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Bidirectional server socket for 2-device P2P.
 * Accepts one client connection and can send/receive packets.
 */
public class SocketServer {

    private static final int PORT = 8888;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private DataOutputStream writer;
    private DataInputStream reader;
    private boolean isRunning = false;
    private Thread serverThread;
    private final MessageListener messageListener;

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

                clientSocket = serverSocket.accept();
                writer = new DataOutputStream(clientSocket.getOutputStream());
                reader = new DataInputStream(clientSocket.getInputStream());

                if (messageListener != null) {
                    messageListener.onConnectionEstablished();
                }

                while (isRunning) {
                    int packetLength = reader.readInt();
                    byte[] packetBytes = new byte[packetLength];
                    reader.readFully(packetBytes);
                    Packet packet = PacketSerializer.deserialize(packetBytes);

                    if (messageListener != null) {
                        messageListener.onPacketReceived(packet, packet.getSourceId());
                    }
                }
            } catch (EOFException e) {
                // Peer closed connection.
            } catch (Exception e) {
                if (messageListener != null) {
                    messageListener.onError(e.getMessage());
                }
            } finally {
                if (messageListener != null) {
                    messageListener.onConnectionClosed();
                }
                stop();
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Sends a packet to the connected client.
     */
    public void sendPacket(Packet packet) {
        new Thread(() -> {
            if (!isRunning || writer == null) {
                return;
            }

            try {
                byte[] packetBytes = PacketSerializer.serialize(packet);
                synchronized (writer) {
                    writer.writeInt(packetBytes.length);
                    writer.write(packetBytes);
                    writer.flush();
                }
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
