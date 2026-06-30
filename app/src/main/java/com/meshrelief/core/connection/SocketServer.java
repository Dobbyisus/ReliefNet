package com.meshrelief.core.connection;

import com.meshrelief.core.model.Packet;
import com.meshrelief.core.model.PacketSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GO-side multi-client server for same-group routed chat.
 */
public class SocketServer {

    private static final int PORT = 8888;

    private final MessageListener messageListener;
    private final Map<String, ClientSession> sessionsByConnectionId = new ConcurrentHashMap<>();
    private final Map<String, ClientSession> sessionsByNodeId = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private Thread acceptThread;

    public SocketServer(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void start() {
        if (isRunning) {
            return;
        }

        acceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;

                if (messageListener != null) {
                    messageListener.onConnectionEstablished();
                }

                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    ClientSession session = new ClientSession(
                            UUID.randomUUID().toString(),
                            socket
                    );
                    sessionsByConnectionId.put(session.connectionId, session);
                    session.start();
                }
            } catch (Exception e) {
                if (isRunning && messageListener != null) {
                    messageListener.onError(e.getMessage());
                }
            } finally {
                stop();
            }
        });

        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void sendPacket(Packet packet) {
        if (packet.getDestinationNodeId() != null) {
            sendToNode(packet, packet.getDestinationNodeId());
        } else {
            broadcastToGroup(packet, null);
        }
    }

    public void sendToNode(Packet packet, String nodeId) {
        ClientSession session = sessionsByNodeId.get(nodeId);
        if (session != null) {
            session.send(packet);
        }
    }

    public void broadcastToGroup(Packet packet, String excludeNodeId) {
        for (ClientSession session : sessionsByConnectionId.values()) {
            if (excludeNodeId != null && excludeNodeId.equals(session.boundNodeId)) {
                continue;
            }
            session.send(packet);
        }
    }

    public void stop() {
        isRunning = false;

        for (ClientSession session : sessionsByConnectionId.values()) {
            session.close();
        }

        sessionsByConnectionId.clear();
        sessionsByNodeId.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {}

        if (messageListener != null) {
            messageListener.onConnectionClosed();
        }
    }

    public boolean hasConnectedClients() {
        return !sessionsByConnectionId.isEmpty();
    }

    public boolean isRunning() {
        return isRunning;
    }

    private final class ClientSession {
        private final String connectionId;
        private final Socket socket;
        private DataOutputStream writer;
        private DataInputStream reader;
        private Thread readThread;
        private String boundNodeId;

        private ClientSession(String connectionId, Socket socket) {
            this.connectionId = connectionId;
            this.socket = socket;
        }

        private void start() {
            readThread = new Thread(() -> {
                try {
                    writer = new DataOutputStream(socket.getOutputStream());
                    reader = new DataInputStream(socket.getInputStream());

                    while (isRunning && !socket.isClosed()) {
                        int packetLength = reader.readInt();
                        byte[] packetBytes = new byte[packetLength];
                        reader.readFully(packetBytes);
                        Packet packet = PacketSerializer.deserialize(packetBytes);

                        if (packet.getSourceNodeId() != null) {
                            boundNodeId = packet.getSourceNodeId();
                            sessionsByNodeId.put(boundNodeId, this);
                        }

                        if (messageListener != null) {
                            messageListener.onPacketReceived(packet, packet.getSourceNodeId());
                        }
                    }
                } catch (EOFException e) {
                    // Client disconnected.
                } catch (Exception e) {
                    if (isRunning && messageListener != null) {
                        messageListener.onError(e.getMessage());
                    }
                } finally {
                    if (boundNodeId != null) {
                        sessionsByNodeId.remove(boundNodeId);
                        if (messageListener != null) {
                            messageListener.onPeerDisconnected(boundNodeId);
                        }
                    }

                    sessionsByConnectionId.remove(connectionId);
                    close();
                }
            });

            readThread.setDaemon(true);
            readThread.start();
        }

        private void send(Packet packet) {
            if (writer == null) {
                return;
            }

            new Thread(() -> {
                try {
                    byte[] packetBytes = PacketSerializer.serialize(packet);
                    synchronized (writer) {
                        writer.writeInt(packetBytes.length);
                        writer.write(packetBytes);
                        writer.flush();
                    }
                } catch (Exception e) {
                    // Ignore send failures; disconnect handling will clean up state.
                }
            }).start();
        }

        private void close() {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception e) {}
        }
    }
}
