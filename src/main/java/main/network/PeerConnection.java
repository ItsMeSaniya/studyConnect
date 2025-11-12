package main.network;

import main.model.Message;
import main.model.FileTransfer;

import java.io.*;
import java.net.*;

/**
 * PeerConnection handles communication with a single peer
 */
public class PeerConnection implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private MessageHandler messageHandler;
    private boolean running;
    private String peerAddress;
    private String currentUsername;

    public PeerConnection(Socket socket, MessageHandler messageHandler, String currentUsername) {
        this.socket = socket;
        this.messageHandler = messageHandler;
        this.currentUsername = currentUsername;
        this.peerAddress = socket.getInetAddress().getHostAddress();

        try {
            // Create output stream first
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
            this.running = true;
        } catch (IOException e) {
            messageHandler.onServerStatus("Error initializing peer connection: " + e.getMessage());
            close();
        }
    }

    @Override
    public void run() {
        while (running && !socket.isClosed()) {
            try {
                Object obj = in.readObject();

                if (obj instanceof Message) {
                    Message message = (Message) obj;
                    messageHandler.onMessageReceived(message, this);
                    // Popup notifications removed

                } else if (obj instanceof FileTransfer) {
                    FileTransfer fileTransfer = (FileTransfer) obj;

                    // Check if this is an incoming file (not our own)
                    boolean isIncomingFile = !fileTransfer.getSender().equals(currentUsername);
                    boolean isForCurrentUser = fileTransfer.getRecipient().equals(currentUsername);

                    if (isIncomingFile && isForCurrentUser) {
                        messageHandler.onFileReceived(fileTransfer, this);
                        // Popup notification removed
                    } else {
                        // This is our own file transfer echo, just log it
                        System.out.println("[DEBUG] Ignoring file transfer: " + fileTransfer.getFileName() +
                                " | Sender: " + fileTransfer.getSender() +
                                " | Recipient: " + fileTransfer.getRecipient());
                    }
                }
            } catch (EOFException | SocketException e) {
                break;
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    messageHandler.onServerStatus("Error reading from peer: " + e.getMessage());
                }
                break;
            }
        }

        close();
        messageHandler.onServerStatus("Peer disconnected: " + peerAddress);
        // Popup notification removed
    }

    /**
     * Send a message to the peer
     */
    public synchronized void sendMessage(Message message) {
        try {
            if (out != null && !socket.isClosed()) {
                out.writeObject(message);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            messageHandler.onServerStatus("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Send a file to the peer
     */
    public synchronized void sendFile(FileTransfer fileTransfer) {
        try {
            if (out != null && !socket.isClosed()) {
                out.writeObject(fileTransfer);
                out.flush();
                out.reset();
                messageHandler.onServerStatus("File sent: " + fileTransfer.getFileName());
                // Popup notification removed
            }
        } catch (IOException e) {
            messageHandler.onServerStatus("Error sending file: " + e.getMessage());
        }
    }

    /**
     * Close the connection
     */
    public void close() {
        running = false;

        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public boolean isRunning() {
        return running && !socket.isClosed();
    }
}