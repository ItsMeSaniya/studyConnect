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
    private Map<PeerConnection, Long> lastHeartbeatTime; // Track last heartbeat from each client
    private Thread connectionMonitor;
    private static final long CLIENT_TIMEOUT = 90000; // 90 seconds - longer than client heartbeat timeout
    
    // File sharing
    private List<main.model.FileMetadata> sharedFiles; // List of shared files
    private Map<String, byte[]> fileStorage; // Store actual file data by fileId
    private static final String UPLOAD_DIR = "shared_files/";

    public Server(int port, MessageHandler messageHandler, String currentUsername) {
        this.port = port;
        this.messageHandler = messageHandler;
        this.currentUsername = currentUsername;
        this.connections = new CopyOnWriteArrayList<>();
        this.connectionUsernames = new ConcurrentHashMap<>();
        this.lastHeartbeatTime = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.sharedFiles = new CopyOnWriteArrayList<>();
        this.fileStorage = new ConcurrentHashMap<>();
        
        // Create upload directory
        new java.io.File(UPLOAD_DIR).mkdirs();
    }

    /**
     * Start the server
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        
        // Start connection monitor thread
        startConnectionMonitor();

        threadPool.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                messageHandler.onServerStatus("Server started on port " + port);

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
                            
                            @Override
                            public void onConnectionLost(PeerConnection conn) {
                                // Remove the connection and notify clients
                                Server.this.removePeerConnection(conn);
                                messageHandler.onConnectionLost(conn);
                            }
                        };
                        
                        PeerConnection connection = new PeerConnection(clientSocket, serverMessageHandler, currentUsername);
                        connections.add(connection);
                        lastHeartbeatTime.put(connection, System.currentTimeMillis());
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
     * Stop the server..
     */
    public void stop() {
        running = false;
        
        // Stop connection monitor
        if (connectionMonitor != null && connectionMonitor.isAlive()) {
            connectionMonitor.interrupt();
        }

        // Notify all clients that server is shutting down
        Message serverStopMsg = new Message("Server", "all", 
            "Server is shutting down", Message.MessageType.SERVER_SHUTDOWN);
        broadcast(serverStopMsg);
        
        // Give clients a moment to receive the shutdown message
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Close all peer connections
        for (PeerConnection conn : connections) {
            conn.close();
        }
        connections.clear();
        connectionUsernames.clear();
        lastHeartbeatTime.clear();

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
     * Get list of shared files (for admin access)
     */
    public List<main.model.FileMetadata> getSharedFiles() {
        return new ArrayList<>(sharedFiles);
    }
    
    /**
     * Get file data by ID (for admin download)
     */
    public byte[] getFileData(String fileId, String filePath) {
        // Try memory first
        byte[] fileData = fileStorage.get(fileId);
        
        if (fileData == null && filePath != null) {
            // Try disk
            try {
                fileData = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
            } catch (Exception e) {
                System.err.println("[SERVER] Failed to read file from disk: " + e.getMessage());
            }
        }
        
        return fileData;
    }

    /**
     * Broadcast message to all connected peers
     */
    public void broadcast(Message message) {
        // Create a copy to avoid ConcurrentModificationException
        List<PeerConnection> activeConnections = new ArrayList<>(connections);
        List<PeerConnection> deadConnections = new ArrayList<>();
        
        for (PeerConnection conn : activeConnections) {
            if (conn.isRunning()) {
                conn.sendMessage(message);
            } else {
                // Mark dead connection for removal
                deadConnections.add(conn);
            }
        }
        
        // Clean up dead connections
        if (!deadConnections.isEmpty()) {
            for (PeerConnection conn : deadConnections) {
                connections.remove(conn);
                String username = connectionUsernames.remove(conn);
                if (username != null) {
                    System.out.println("[SERVER] Removed dead connection: " + username);
                    // Broadcast updated peer list
                    broadcastPeerList();
                }
            }
        }
    }

    /**
     * Handle messages received from clients
     */
    private void handleClientMessage(Message message, PeerConnection connection) {
        switch (message.getType()) {
            case USER_JOIN: {
                // Store username for this connection
                String username = message.getSender();
                connectionUsernames.put(connection, username);

                // Notify server UI (dashboard) about the join so it can update its view
                messageHandler.onMessageReceived(message, connection);

                // Broadcast the join message and updated peer list
                broadcast(message);
                broadcastPeerList();
                break;
            }

            case PEER_TO_PEER:
            case FILE: {
                // Forward P2P message or file to target peer
                String targetUser = message.getReceiver();
                
                // Check if message is for the server (admin)
                if (targetUser.equalsIgnoreCase("Server") || targetUser.equalsIgnoreCase(currentUsername)) {
                    // Message is for the server/admin, pass to main handler (already done above)
                    System.out.println("[SERVER] Received P2P message from " + message.getSender() + " to Server");
                } else {
                    // Forward to another client
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
                }
                break;
            }

            case TEXT:
            case BROADCAST:
            case QUIZ_START:
            case QUIZ_ANSWER: {
                // Let the server UI (dashboard) see the message, then broadcast
                messageHandler.onMessageReceived(message, connection);
                broadcast(message);
                break;
                
            case HEARTBEAT:
                // Respond to heartbeat - send it back to keep connection alive
                lastHeartbeatTime.put(connection, System.currentTimeMillis());
                Message heartbeatResponse = new Message("server", message.getSender(), "pong", Message.MessageType.HEARTBEAT);
                connection.sendMessage(heartbeatResponse);
                break;
                
            case CLASS_JOIN:
                // Student wants to join the class - notify the message handler (MainDashboard)
                if (messageHandler != null) {
                    messageHandler.onMessageReceived(message, connection);
                }
                break;
                
            case CLASS_LEAVE:
                // Student leaves the class - notify the message handler
                if (messageHandler != null) {
                    messageHandler.onMessageReceived(message, connection);
                }
                break;
                
            case USER_LEAVE:
                connectionUsernames.remove(connection);
                broadcast(message);
                broadcastPeerList(); // Update peer list after someone leaves
                break;
                
            case FILE_LIST_REQUEST:
                // Client requests list of shared files
                sendFileListToClient(connection);
                break;
                
            case FILE_UPLOAD:
                // Client uploads a file
                handleFileUpload(message, connection);
                break;
                
            case FILE_DOWNLOAD_REQUEST:
                // Client requests to download a file
                handleFileDownload(message, connection);
                break;
                
            case FILE_DELETE_REQUEST:
                // Client requests to delete a file
                handleFileDelete(message, connection);
                break;
                
            default:
                // For other types, just broadcast
                broadcast(message);
                break;
            }
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
     * Remove a peer connection and clean up
     */
    public void removePeerConnection(PeerConnection connection) {
        connections.remove(connection);
        String username = connectionUsernames.remove(connection);
        lastHeartbeatTime.remove(connection);
        
        if (username != null) {
            System.out.println("[SERVER] Removed connection: " + username);
            
            // Broadcast updated peer list to remaining clients
            broadcastPeerList();
            
            // Notify all clients that user left
            Message leaveMsg = new Message(username, "all", 
                username + " has left the chat", Message.MessageType.USER_LEAVE);
            broadcast(leaveMsg);
        }
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
    
    public Map<PeerConnection, String> getConnectionUsernames() {
        return new HashMap<>(connectionUsernames);
    }
    
    /**
     * Start monitoring client connections for timeouts
     */
    private void startConnectionMonitor() {
        connectionMonitor = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30000); // Check every 30 seconds
                    
                    if (!running) break;
                    
                    long currentTime = System.currentTimeMillis();
                    List<PeerConnection> deadConnections = new ArrayList<>();
                    
                    // Check all connections for timeout
                    for (Map.Entry<PeerConnection, Long> entry : lastHeartbeatTime.entrySet()) {
                        PeerConnection conn = entry.getKey();
                        Long lastTime = entry.getValue();
                        
                        if (lastTime != null) {
                            long timeSinceLastHeartbeat = currentTime - lastTime;
                            if (timeSinceLastHeartbeat > CLIENT_TIMEOUT) {
                                // Connection has timed out
                                String username = connectionUsernames.get(conn);
                                System.err.println("[SERVER] Client timeout: " + username + " (no heartbeat for " + timeSinceLastHeartbeat + "ms)");
                                deadConnections.add(conn);
                            }
                        }
                    }
                    
                    // Remove dead connections
                    for (PeerConnection conn : deadConnections) {
                        removePeerConnection(conn);
                        conn.close();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        connectionMonitor.setDaemon(true);
        connectionMonitor.setName("ConnectionMonitor");
        connectionMonitor.start();
    }
    
    /**
     * Send list of shared files to a client
     */
    private void sendFileListToClient(PeerConnection connection) {
        Message response = new Message("server", connectionUsernames.get(connection),
            "FILE_LIST", Message.MessageType.FILE_LIST_RESPONSE);
        response.setFileList(new ArrayList<>(sharedFiles));
        connection.sendMessage(response);
        System.out.println("[SERVER] Sent file list to " + connectionUsernames.get(connection) + 
            " (" + sharedFiles.size() + " files)");
    }
    
    /**
     * Handle file upload from client
     */
    private void handleFileUpload(Message message, PeerConnection connection) {
        main.model.FileMetadata metadata = message.getFileMetadata();
        FileTransfer fileTransfer = message.getFileTransfer();
        
        if (metadata != null && fileTransfer != null) {
            // Store file data
            fileStorage.put(metadata.getFileId(), fileTransfer.getFileData());
            
            // Save file to disk (optional, for persistence)
            try {
                String fileName = UPLOAD_DIR + metadata.getFileId() + "_" + metadata.getFileName();
                java.nio.file.Files.write(
                    java.nio.file.Paths.get(fileName),
                    fileTransfer.getFileData()
                );
                metadata.setFilePath(fileName);
            } catch (Exception e) {
                System.err.println("[SERVER] Failed to save file to disk: " + e.getMessage());
            }
            
            // Add to shared files list
            sharedFiles.add(metadata);
            
            System.out.println("[SERVER] File uploaded: " + metadata.getFileName() + 
                " by " + metadata.getUploader() + " (" + metadata.getFormattedSize() + ")");
            
            // Notify all clients about new file
            broadcastFileListUpdate();
        }
    }
    
    /**
     * Handle file upload directly (for admin, without connection)
     */
    public void handleFileUploadDirect(Message message) {
        handleFileUpload(message, null);
    }
    
    /**
     * Handle file download request
     */
    private void handleFileDownload(Message message, PeerConnection connection) {
        main.model.FileMetadata metadata = message.getFileMetadata();
        
        if (metadata != null) {
            byte[] fileData = fileStorage.get(metadata.getFileId());
            
            if (fileData == null) {
                // Try to read from disk
                try {
                    if (metadata.getFilePath() != null) {
                        fileData = java.nio.file.Files.readAllBytes(
                            java.nio.file.Paths.get(metadata.getFilePath())
                        );
                    }
                } catch (Exception e) {
                    System.err.println("[SERVER] Failed to read file from disk: " + e.getMessage());
                }
            }
            
            if (fileData != null) {
                // Send file to client
                FileTransfer fileTransfer = new FileTransfer(
                    metadata.getFileName(),
                    metadata.getFileSize(),
                    fileData,
                    "server",
                    connectionUsernames.get(connection)
                );
                
                Message fileMessage = new Message("server", connectionUsernames.get(connection),
                    metadata.getFileName(), Message.MessageType.FILE);
                fileMessage.setFileTransfer(fileTransfer);
                
                connection.sendMessage(fileMessage);
                System.out.println("[SERVER] File downloaded: " + metadata.getFileName() + 
                    " by " + connectionUsernames.get(connection));
            } else {
                System.err.println("[SERVER] File not found: " + metadata.getFileId());
            }
        }
    }
    
    /**
     * Handle file delete request
     */
    private void handleFileDelete(Message message, PeerConnection connection) {
        main.model.FileMetadata metadata = message.getFileMetadata();
        String requestUser = connectionUsernames.get(connection);
        
        if (metadata != null && requestUser != null) {
            // Check permissions (admin or file owner)
            boolean isAdmin = requestUser.equalsIgnoreCase("admin");
            boolean isOwner = metadata.getUploader().equals(requestUser);
            
            if (isAdmin || isOwner) {
                // Remove from list
                sharedFiles.removeIf(f -> f.getFileId().equals(metadata.getFileId()));
                
                // Remove from storage
                fileStorage.remove(metadata.getFileId());
                
                // Delete file from disk
                try {
                    if (metadata.getFilePath() != null) {
                        java.nio.file.Files.deleteIfExists(
                            java.nio.file.Paths.get(metadata.getFilePath())
                        );
                    }
                } catch (Exception e) {
                    System.err.println("[SERVER] Failed to delete file from disk: " + e.getMessage());
                }
                
                System.out.println("[SERVER] File deleted: " + metadata.getFileName() + 
                    " by " + requestUser);
                
                // Notify all clients about file list update
                broadcastFileListUpdate();
            } else {
                System.err.println("[SERVER] Permission denied: " + requestUser + 
                    " tried to delete " + metadata.getFileName());
            }
        }
    }
    
    /**
     * Handle file delete directly (for admin, without connection)
     */
    public void handleFileDeleteDirect(Message message, String requestUser) {
        main.model.FileMetadata metadata = message.getFileMetadata();
        
        if (metadata != null && requestUser != null) {
            // Check permissions (admin or file owner)
            boolean isAdmin = requestUser.equalsIgnoreCase("admin");
            boolean isOwner = metadata.getUploader().equals(requestUser);
            
            if (isAdmin || isOwner) {
                // Remove from list
                sharedFiles.removeIf(f -> f.getFileId().equals(metadata.getFileId()));
                
                // Remove from storage
                fileStorage.remove(metadata.getFileId());
                
                // Delete file from disk
                try {
                    if (metadata.getFilePath() != null) {
                        java.nio.file.Files.deleteIfExists(
                            java.nio.file.Paths.get(metadata.getFilePath())
                        );
                    }
                } catch (Exception e) {
                    System.err.println("[SERVER] Failed to delete file from disk: " + e.getMessage());
                }
                
                System.out.println("[SERVER] File deleted: " + metadata.getFileName() + 
                    " by " + requestUser);
                
                // Notify all clients about file list update
                broadcastFileListUpdate();
            }
        }
    }
    
    /**
     * Broadcast updated file list to all clients
     */
    private void broadcastFileListUpdate() {
        Message update = new Message("server", "all",
            "FILE_LIST_UPDATE", Message.MessageType.FILE_LIST_RESPONSE);
        update.setFileList(new ArrayList<>(sharedFiles));
        broadcast(update);
    }
}
