package main.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Message model for chat messages
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        TEXT,
        FILE,
        BROADCAST,
        PEER_TO_PEER,
        USER_JOIN,
        USER_LEAVE,
        QUIZ_START,
        QUIZ_ANSWER,
        QUIZ_RESULT,
        PEER_LIST,  // Server sends list of connected peers to clients
        SERVER_SHUTDOWN,  // Server is shutting down - clients should disconnect
        HEARTBEAT,  // Ping/pong to keep connection alive and detect dead connections
        CLASS_JOIN,  // Student joins the class for screen sharing
        CLASS_LEAVE,  // Student leaves the class
        CLASS_INFO  // Server sends UDP port info to student
    }
    
    private String sender;
    private String recipient;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    
    // Screen sharing field
    private int udpPort;  // UDP port for screen sharing
    
    // Quiz-related fields
    private Quiz quizData;
    private QuizAnswer quizAnswer;
    private QuizResult quizResult;
    
    // File transfer field
    private FileTransfer fileTransfer;
    
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Message(String sender, String recipient, String content, MessageType type) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getRecipient() {
        return recipient;
    }
    
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    
    // Alias for P2P messages
    public String getReceiver() {
        return recipient;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return timestamp.format(formatter);
    }
    
    // Quiz-related getters and setters
    public Quiz getQuizData() {
        return quizData;
    }
    
    public void setQuizData(Quiz quizData) {
        this.quizData = quizData;
    }
    
    public QuizAnswer getQuizAnswer() {
        return quizAnswer;
    }
    
    public void setQuizAnswer(QuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
    
    public QuizResult getQuizResult() {
        return quizResult;
    }
    
    public void setQuizResult(QuizResult quizResult) {
        this.quizResult = quizResult;
    }
    
    public FileTransfer getFileTransfer() {
        return fileTransfer;
    }
    
    public void setFileTransfer(FileTransfer fileTransfer) {
        this.fileTransfer = fileTransfer;
    }
    
    public int getUdpPort() {
        return udpPort;
    }
    
    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", getFormattedTimestamp(), sender, content);
    }
}
