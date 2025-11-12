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
    
    public Client(String host, int port, MessageHandler messageHandler, String currentUsername) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
        this.currentUsername = currentUsername;
        this.connected = false;
    }
    
    /**
     * Connect to a peer
     */
    public boolean connect() {
        try {
            messageHandler.onServerStatus("Attempting to connect to " + host + ":" + port + "...");
            
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 10000); // 10 second timeout
            
            connection = new PeerConnection(socket, messageHandler, currentUsername);
            connected = true;
            
            // Start listening for messages
            new Thread(connection).start();
            
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
