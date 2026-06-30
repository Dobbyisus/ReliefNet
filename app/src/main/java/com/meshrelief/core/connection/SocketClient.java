package com.meshrelief.core.connection;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.PacketSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Bidirectional client socket for connecting to server.
 * Connects to Group Owner IP on port 8888 and can send/receive packets.
 */
public class SocketClient {

    private static final int PORT = 8888;

    private Socket socket;
    private DataOutputStream writer;
    private DataInputStream reader;
    private boolean isConnected = false;
    private Thread readThread;
    private final MessageListener messageListener;

    public SocketClient(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    /**
     * Connects to the server.
     *
     * @param serverAddress The Group Owner IP address
     */
    public void connect(InetAddress serverAddress) {
        new Thread(() -> {
            try {
                socket = new Socket(serverAddress, PORT);
                writer = new DataOutputStream(socket.getOutputStream());
                reader = new DataInputStream(socket.getInputStream());
                isConnected = true;

                if (messageListener != null) {
                    messageListener.onConnectionEstablished();
                }

                startReadingPackets();
            } catch (Exception e) {
                if (messageListener != null) {
                    messageListener.onError(e.getMessage());
                }
                isConnected = false;
            }
        }).start();
    }

    private void startReadingPackets() {
        readThread = new Thread(() -> {
            try {
                while (isConnected) {
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
                    messageListener.onError(e.getClass().getName());
                }
            } finally {
                if (messageListener != null) {
                    messageListener.onConnectionClosed();
                }
            }
        });
        readThread.start();
    }

    /**
     * Sends a packet to the server.
     */
    public void sendPacket(Packet packet) {
        new Thread(() -> {
            if (!isConnected || writer == null) {
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
     * Closes the connection.
     */
    public void disconnect() {
        isConnected = false;
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {}
    }

    public boolean isConnected() {
        return isConnected;
    }
}
