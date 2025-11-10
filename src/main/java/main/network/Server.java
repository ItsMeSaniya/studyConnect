package main.network;

import main.model.Message;
import main.model.FileTransfer;
import main.util.SSLUtil;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
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
    private boolean useSSL = true; // Enable SSL by default
    
    public Server(int port, MessageHandler messageHandler) {
        this.port = port;
        this.messageHandler = messageHandler;
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
                // Create SSL or regular server socket
                if (useSSL && SSLUtil.isSSLAvailable()) {
                    SSLServerSocketFactory sslFactory = SSLUtil.getServerSocketFactory();
                    serverSocket = sslFactory.createServerSocket(port);
                    messageHandler.onServerStatus("🔒 Secure server started on port " + port + " (SSL/TLS enabled)");
                } else {
                    serverSocket = new ServerSocket(port);
                    messageHandler.onServerStatus("Server started on port " + port + " (No encryption)");
                }
                
                running = true;
                
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        PeerConnection connection = new PeerConnection(clientSocket, messageHandler);
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
            } catch (Exception e) {
                messageHandler.onServerStatus("SSL error: " + e.getMessage());
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
