package main.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.function.Consumer;

/**
 * UDP client for receiving notification messages
 */
public class NotificationClient {
    private static final int NOTIFICATION_PORT = 9877;
    private DatagramSocket socket;
    private Thread listenerThread;
    private boolean running = false;
    private Consumer<String> messageHandler;
    private String username;
    
    public NotificationClient(Consumer<String> messageHandler, String username) {
        this.messageHandler = messageHandler;
        this.username = username;
    }
    
    public void start() {
        if (running) {
            return;
        }
        
        try {
            socket = new DatagramSocket(NOTIFICATION_PORT);
            running = true;
            
            listenerThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                
                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        
                        String message = new String(packet.getData(), 0, packet.getLength());
                        
                        if (messageHandler != null) {
                            messageHandler.accept(message);
                        }
                    } catch (Exception e) {
                        if (running) {
                            System.err.println("[NotificationClient] Error receiving packet: " + e.getMessage());
                        }
                    }
                }
            });
            
            listenerThread.setDaemon(true);
            listenerThread.start();
            
            System.out.println("[NotificationClient] Started on port " + NOTIFICATION_PORT);
        } catch (Exception e) {
            System.err.println("[NotificationClient] Failed to start: " + e.getMessage());
        }
    }
    
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}
