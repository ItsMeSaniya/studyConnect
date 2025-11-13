package main.network;

import main.model.Message;
import main.model.FileTransfer;

import java.io.*;
import java.net.*;

/**
 * Client class to connect to peer servers
 */
public class Client {
    private String host;
    private int port;
    private Socket socket;
    private PeerConnection connection;
    private MessageHandler messageHandler;
    private String currentUsername;
    private boolean connected;
    private Thread heartbeatThread;
    private volatile long lastHeartbeatTime;
    private static final long HEARTBEAT_INTERVAL = 30000; // 30 seconds
    private static final long HEARTBEAT_TIMEOUT = 60000; // 60 seconds
    
    public Client(String host, int port, MessageHandler messageHandler, String currentUsername) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
        this.currentUsername = currentUsername;
        this.connected = false;
        this.lastHeartbeatTime = System.currentTimeMillis();
    }
    
    /**
     * Connect to a peer
     */
    public boolean connect() {
        try {
            messageHandler.onServerStatus("Attempting to connect to " + host + ":" + port + "...");
            
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 10000); // 10 second timeout for connection
            
            // Enable TCP keep-alive to prevent auto-disconnect
            socket.setKeepAlive(true);
            // Disable socket timeout - keep connection open indefinitely
            socket.setSoTimeout(0);
            // Enable TCP_NODELAY for better responsiveness
            socket.setTcpNoDelay(true);
            
            // Wrap the message handler to detect connection loss
            MessageHandler wrappedHandler = new MessageHandler() {
                @Override
                public void onMessageReceived(Message message, PeerConnection conn) {
                    // Update heartbeat time on any message received
                    if (message.getType() == Message.MessageType.HEARTBEAT) {
                        lastHeartbeatTime = System.currentTimeMillis();
                        // Don't forward heartbeat to main handler
                        return;
                    }
                    lastHeartbeatTime = System.currentTimeMillis();
                    messageHandler.onMessageReceived(message, conn);
                }
                
                @Override
                public void onFileReceived(FileTransfer fileTransfer, PeerConnection conn) {
                    lastHeartbeatTime = System.currentTimeMillis();
                    messageHandler.onFileReceived(fileTransfer, conn);
                }
                
                @Override
                public void onServerStatus(String status) {
                    messageHandler.onServerStatus(status);
                }
                
                @Override
                public void onConnectionLost(PeerConnection conn) {
                    // Mark as disconnected and stop heartbeat
                    connected = false;
                    stopHeartbeat();
                    messageHandler.onConnectionLost(conn);
                }
            };
            
            connection = new PeerConnection(socket, wrappedHandler, currentUsername);
            connected = true;
            
            // Start listening for messages
            new Thread(connection).start();
            
            // Start heartbeat sender
            startHeartbeat();
            
            messageHandler.onServerStatus("✅ Connected to " + host + ":" + port);
            return true;
        } catch (SocketTimeoutException e) {
            messageHandler.onServerStatus("❌ Connection timeout! Possible causes:\n" +
                "1. Server is not running\n" +
                "2. Firewall is blocking connection\n" +
                "3. Wrong IP/Port\n" +
                "4. Server is behind router (needs port forwarding)");
            return false;
        } catch (ConnectException e) {
            messageHandler.onServerStatus("❌ Connection refused! Server is not accepting connections on " + host + ":" + port);
            return false;
        } catch (UnknownHostException e) {
            messageHandler.onServerStatus("❌ Unknown host: " + host + " - Check if IP address is correct");
            return false;
        } catch (IOException e) {
            messageHandler.onServerStatus("❌ Failed to connect: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disconnect from peer
     */
    public void disconnect() {
        stopHeartbeat();
        
        if (connection != null) {
            connection.close();
        }
        
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        connected = false;
        messageHandler.onServerStatus("Disconnected from " + host + ":" + port);
    }
    
    /**
     * Send message to peer
     */
    public void sendMessage(Message message) {
        if (connection != null && connected) {
            connection.sendMessage(message);
        }
    }
    
    /**
     * Send file to peer
     */
    public void sendFile(FileTransfer fileTransfer) {
        if (connection != null && connected) {
            connection.sendFile(fileTransfer);
        }
    }
    
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && connection != null && connection.isRunning();
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    /**
     * Start heartbeat sender thread
     */
    private void startHeartbeat() {
        lastHeartbeatTime = System.currentTimeMillis();
        heartbeatThread = new Thread(() -> {
            while (connected && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                    
                    if (!connected) break;
                    
                    // Check if we've received any message recently
                    long timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeatTime;
                    if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT) {
                        // Connection appears dead - force disconnect
                        System.err.println("[Client] Heartbeat timeout - connection appears dead");
                        messageHandler.onServerStatus("⚠️ Connection timeout - server not responding");
                        
                        // Trigger connection lost
                        connected = false;
                        if (connection != null) {
                            messageHandler.onConnectionLost(connection);
                        }
                        break;
                    }
                    
                    // Send heartbeat
                    if (connection != null && connection.isRunning()) {
                        Message heartbeat = new Message(currentUsername, "server", "ping", Message.MessageType.HEARTBEAT);
                        connection.sendMessage(heartbeat);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.setName("Heartbeat-" + host + ":" + port);
        heartbeatThread.start();
    }
    
    /**
     * Stop heartbeat thread
     */
    private void stopHeartbeat() {
        if (heartbeatThread != null && heartbeatThread.isAlive()) {
            heartbeatThread.interrupt();
            try {
                heartbeatThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
