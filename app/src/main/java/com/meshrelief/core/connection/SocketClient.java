    package com.meshrelief.core.connection;

    import java.io.BufferedReader;
    import java.io.InputStreamReader;
    import java.io.PrintWriter;
    import java.net.InetAddress;
    import java.net.Socket;

    /**
     * Bidirectional client socket for connecting to server.
     * Connects to Group Owner IP on port 8888 and can send/receive messages.
     */
    public class SocketClient {

        private static final int PORT = 8888;
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private boolean isConnected = false;
        private Thread readThread;
        private MessageListener messageListener;

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
                    writer = new PrintWriter(socket.getOutputStream(), true);
                    reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream())
                    );
                    isConnected = true;

                    if (messageListener != null) {
                        messageListener.onConnectionEstablished();
                    }

                    // Start reading messages from server in background
                    startReadingMessages();

                } catch (Exception e) {
                    if (messageListener != null) {
                        messageListener.onError(e.getMessage());
                    }
                    isConnected = false;
                }
            }).start();
        }

        /**
         * Continuously reads messages from server.
         */
        private void startReadingMessages() {
            readThread = new Thread(() -> {
                try {
                    String messageFromServer;
                    while ((messageFromServer = reader.readLine()) != null && isConnected) {
                        if (messageListener != null) {
                            messageListener.onMessageReceived(messageFromServer);
                        }
                    }
                    if (messageListener != null) {
                        messageListener.onConnectionClosed();
                    }
                } catch (Exception e) {
                    if (messageListener != null) {
                        messageListener.onError(
                                e.getClass().getName()
                        );
                    }
                }
            });
            readThread.start();
        }

        /**
         * Sends a message to the server.
         *
         * @param message The message to send
         */
        public void sendMessage(String message) {

            new Thread(() -> {

                if (!isConnected || writer == null) {
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
