package main.network;

import main.model.Message;
import main.model.FileTransfer;

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
    private Map<PeerConnection, String> connectionUsernames; // Maps connections to usernames

    public Server(int port, MessageHandler messageHandler, String currentUsername) {
        this.port = port;
        this.messageHandler = messageHandler;
        this.currentUsername = currentUsername;
        this.connections = new CopyOnWriteArrayList<>();
        this.connectionUsernames = new ConcurrentHashMap<>();
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
                        // Create connection with a custom message handler to intercept messages
                        MessageHandler serverMessageHandler = new MessageHandler() {
                            @Override
                            public void onMessageReceived(Message message, PeerConnection conn) {
                                Server.this.handleClientMessage(message, conn);
                            }
                            
                            @Override
                            public void onFileReceived(FileTransfer fileTransfer, PeerConnection conn) {
                                messageHandler.onFileReceived(fileTransfer, conn);
                            }
                            
                            @Override
                            public void onServerStatus(String status) {
                                messageHandler.onServerStatus(status);
                            }
                        };
                        
                        PeerConnection connection = new PeerConnection(clientSocket, serverMessageHandler, currentUsername);
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
     * Handle messages received from clients
     */
    private void handleClientMessage(Message message, PeerConnection connection) {
        // First, pass to the main message handler so dashboard can process it
        messageHandler.onMessageReceived(message, connection);
        
        switch (message.getType()) {
            case USER_JOIN:
                // Store username for this connection
                String username = message.getSender();
                connectionUsernames.put(connection, username);
                
                // Broadcast user join to all clients
                Message joinMsg = new Message("System", "all",
                    username + " has joined", Message.MessageType.USER_JOIN);
                broadcast(joinMsg);
                
                // Send updated peer list to all clients
                broadcastPeerList();
                break;
                
            case PEER_TO_PEER:
            case FILE:
                // Forward P2P message or file to target peer
                String targetUser = message.getReceiver();
                PeerConnection targetConnection = null;
                
                // Find the target connection by username
                for (Map.Entry<PeerConnection, String> entry : connectionUsernames.entrySet()) {
                    if (entry.getValue().equals(targetUser)) {
                        targetConnection = entry.getKey();
                        break;
                    }
                }
                
                if (targetConnection != null) {
                    targetConnection.sendMessage(message);
                    String messageType = message.getType() == Message.MessageType.FILE ? "file" : "P2P message";
                    System.out.println("[SERVER] Forwarded " + messageType + " from " + 
                        message.getSender() + " to " + targetUser);
                } else {
                    System.err.println("[SERVER] Target user not found: " + targetUser);
                }
                break;
                
            case TEXT:
            case BROADCAST:
            case QUIZ_START:
            case QUIZ_ANSWER:
                // Broadcast these to all clients
                broadcast(message);
                break;
                
            case USER_LEAVE:
                connectionUsernames.remove(connection);
                broadcast(message);
                broadcastPeerList(); // Update peer list after someone leaves
                break;
                
            default:
                // For other types, just broadcast
                broadcast(message);
                break;
        }
    }
    
    /**
     * Broadcast the list of connected peers to all clients
     */
    private void broadcastPeerList() {
        // Create a list of usernames including admin
        StringBuilder peerList = new StringBuilder();
        
        // Add admin first so clients can message the admin
        peerList.append("admin").append(",");
        
        // Add all connected clients
        for (String username : connectionUsernames.values()) {
            peerList.append(username).append(",");
        }
        
        // Send peer list to all clients
        Message peerListMsg = new Message("Server", "all", 
            peerList.toString(), Message.MessageType.PEER_LIST);
        broadcast(peerListMsg);
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
