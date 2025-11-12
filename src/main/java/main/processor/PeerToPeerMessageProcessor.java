package main.processor;

import main.model.Message;
import main.network.PeerConnection;

import java.util.function.Consumer;

/**
 * Processor for PEER_TO_PEER messages
 * Follows Open/Closed Principle
 */
public class PeerToPeerMessageProcessor implements MessageProcessor {
    private final Consumer<String> p2pLogger;
    private final String currentUsername;
    
    /**
     * Constructor
     * @param p2pLogger Callback to log P2P messages
     * @param currentUsername Current user's username to determine if message is for them
     */
    public PeerToPeerMessageProcessor(Consumer<String> p2pLogger, String currentUsername) {
        this.p2pLogger = p2pLogger;
        this.currentUsername = currentUsername;
    }
    
    @Override
    public void process(Message message, PeerConnection connection) {
        // Check if the message is for the current user
        String recipient = message.getRecipient();
        boolean isForCurrentUser = recipient != null && 
            recipient.equalsIgnoreCase(currentUsername);
        
        String displayMessage;
        if (isForCurrentUser) {
            // Message is for current user
            displayMessage = message.getSender() + " → You: " + message.getContent();
        } else {
            // Message is between other users (admin observing)
            displayMessage = message.getSender() + " → " + recipient + ": " + message.getContent();
        }
        
        p2pLogger.accept(displayMessage);
    }
    
    @Override
    public Message.MessageType getHandledType() {
        return Message.MessageType.PEER_TO_PEER;
    }
}
