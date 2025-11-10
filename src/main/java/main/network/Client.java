package main.network;

import main.model.Message;
import main.model.FileTransfer;
import main.util.SSLUtil;

import javax.net.ssl.SSLSocketFactory;
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
    private boolean connected;
    private boolean useSSL = true; // Enable SSL by default
    
    public Client(String host, int port, MessageHandler messageHandler) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
        this.connected = false;
    }
    
    /**
     * Connect to a peer
     */
    public boolean connect() {
        try {
            messageHandler.onServerStatus("Attempting to connect to " + host + ":" + port + "...");
            
            // Try SSL connection first
            if (useSSL && SSLUtil.isSSLAvailable()) {
                try {
                    SSLSocketFactory sslFactory = SSLUtil.getSocketFactory();
                    socket = sslFactory.createSocket();
                    socket.connect(new InetSocketAddress(host, port), 10000); // 10 second timeout
                    messageHandler.onServerStatus("🔒 Establishing secure connection (SSL/TLS)...");
                } catch (Exception sslError) {
                    // Fallback to non-SSL if SSL fails
                    messageHandler.onServerStatus("SSL connection failed, trying standard connection...");
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 10000);
                }
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 10000);
            }
            
            connection = new PeerConnection(socket, messageHandler);
            connected = true;
            
            // Start listening for messages
            new Thread(connection).start();
            
            String securityStatus = (socket instanceof javax.net.ssl.SSLSocket) ? 
                "✅ Connected securely (Encrypted)" : "✅ Connected (Not encrypted)";
            messageHandler.onServerStatus(securityStatus + " to " + host + ":" + port);
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
        return connected && socket != null && !socket.isClosed();
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
}
