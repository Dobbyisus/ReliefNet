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
                    System.out.println("Attempting connection to: " + serverAddress);
                    socket = new Socket(serverAddress, PORT);
                    writer = new PrintWriter(socket.getOutputStream(), true);
                    reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream())
                    );
                    isConnected = true;
                    System.out.println("Connected to server at " + serverAddress);

                    if (messageListener != null) {
                        messageListener.onConnectionEstablished();
                    }

                    // Send test message
                    sendMessage("Hello Mesh Relief");

                    // Start reading messages from server in background
                    startReadingMessages();

                } catch (Exception e) {
                    System.err.println("Connection error: " + e.getMessage());
                    if (messageListener != null) {
                        messageListener.onError(e.getMessage());
                    }
                    e.printStackTrace();
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
                        System.out.println("MESSAGE RECEIVED: " + messageFromServer);
                        if (messageListener != null) {
                            messageListener.onMessageReceived(messageFromServer);
                        }
                    }
                    System.out.println("Server disconnected");
                    if (messageListener != null) {
                        messageListener.onConnectionClosed();
                    }
                } catch (Exception e) {

                    System.err.println(
                            "READ EXCEPTION TYPE = "
                                    + e.getClass().getName()
                    );

                    e.printStackTrace();

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

                    System.err.println(
                            "Not connected to server"
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
         * Closes the connection.
         */
        public void disconnect() {
            isConnected = false;
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }

        public boolean isConnected() {
            return isConnected;
        }
    }
