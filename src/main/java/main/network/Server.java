package main.network;

import main.model.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Server class to handle incoming peer connections
 */
public class Server {
    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    private List<PeerConnection> connections;
    private ExecutorService threadPool;
    private MessageHandler messageHandler;
    private String currentUsername;

    public Server(int port, MessageHandler messageHandler, String currentUsername) {
        this.port = port;
        this.messageHandler = messageHandler;
        this.currentUsername = currentUsername;
        this.connections = new CopyOnWriteArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Start the server
     */
    public void start() {
        if (running) {
            return;
        }

        threadPool.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                messageHandler.onServerStatus("Server started on port " + port);

                running = true;

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        PeerConnection connection = new PeerConnection(clientSocket, messageHandler, currentUsername);
                        connections.add(connection);
                        threadPool.execute(connection);

                        messageHandler.onServerStatus("New peer connected: " +
                                clientSocket.getInetAddress().getHostAddress());
                    } catch (SocketException e) {
                        if (running) {
                            messageHandler.onServerStatus("Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                messageHandler.onServerStatus("Server error: " + e.getMessage());
            }
        });
    }

    /**
     * Stop the server
     */
    public void stop() {
        running = false;

        // Close all peer connections
        for (PeerConnection conn : connections) {
            conn.close();
        }
        connections.clear();

        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        messageHandler.onServerStatus("Server stopped");
    }

    /**
     * Broadcast message to all connected peers
     */
    public void broadcast(Message message) {
        for (PeerConnection conn : connections) {
            conn.sendMessage(message);
        }
        if (!message.getSender().equals(currentUsername)) {
            NotificationServer.notify(
                    "New message from " + message.getSender() + ": " + message.getContent());

        }
    }

    /**
     * Remove a peer connection
     */
    public void removePeerConnection(PeerConnection connection) {
        connections.remove(connection);
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public List<PeerConnection> getConnections() {
        return new ArrayList<>(connections);
    }
}
