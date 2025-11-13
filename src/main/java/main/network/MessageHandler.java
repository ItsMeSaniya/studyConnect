package main.network;

import main.model.Message;
import main.model.FileTransfer;

/**
 * Interface for handling network events
 */
public interface MessageHandler {
    /**
     * Called when a message is received
     */
    void onMessageReceived(Message message, PeerConnection connection);
    
    /**
     * Called when a file is received
     */
    void onFileReceived(FileTransfer fileTransfer, PeerConnection connection);
    
    /**
     * Called when server status changes
     */
    void onServerStatus(String status);
    
    /**
     * Called when connection is lost
     */
    void onConnectionLost(PeerConnection connection);
}
