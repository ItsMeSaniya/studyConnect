package main.network;

import main.model.ScreenFrame;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * UDP broadcaster for screen sharing
 * Sends screen frames to connected clients
 */
public class UDPBroadcaster {
    private DatagramSocket socket;
    private boolean broadcasting;
    private Thread captureThread;
    private int port;
    private String username;
    private int frameNumber = 0;
    private List<InetSocketAddress> clients = new ArrayList<>();
    
    // Broadcast settings
    private static final int FRAME_RATE = 2; // 2 frames per second
    private static final int DELAY_MS = 1000 / FRAME_RATE;
    private static final int SCALE_WIDTH = 640; // Scale to reasonable size
    private static final int SCALE_HEIGHT = 480;
    private static final int MAX_PACKET_SIZE = 65000; // UDP max practical size
    
    public UDPBroadcaster(int port, String username) {
        this.port = port;
        this.username = username;
    }
    
    /**
     * Add client to receive broadcasts
     */
    public synchronized void addClient(String host, int port) {
        try {
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(host), port);
            if (!clients.contains(addr)) {
                clients.add(addr);
                System.out.println("[UDP Broadcaster] Added client: " + host + ":" + port);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Remove client from broadcasts
     */
    public synchronized void removeClient(String host, int port) {
        try {
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(host), port);
            clients.remove(addr);
            System.out.println("[UDP Broadcaster] Removed client: " + host + ":" + port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Start broadcasting screen
     */
    public boolean start() {
        try {
            socket = new DatagramSocket();
            broadcasting = true;
            
            captureThread = new Thread(this::captureAndBroadcast);
            captureThread.setDaemon(true);
            captureThread.setName("ScreenCapture-" + port);
            captureThread.start();
            
            System.out.println("[UDP Broadcaster] Started on port " + port);
            return true;
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Stop broadcasting
     */
    public void stop() {
        broadcasting = false;
        
        if (captureThread != null) {
            captureThread.interrupt();
        }
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        System.out.println("[UDP Broadcaster] Stopped");
    }
    
    /**
     * Capture and broadcast screen frames
     */
    private void captureAndBroadcast() {
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            return;
        }
        
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        
        while (broadcasting && !Thread.currentThread().isInterrupted()) {
            try {
                // Capture screen
                BufferedImage screenshot = robot.createScreenCapture(screenRect);
                
                // Scale down to reduce size
                BufferedImage scaledImage = scaleImage(screenshot, SCALE_WIDTH, SCALE_HEIGHT);
                
                // Convert to byte array (JPEG for compression)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(scaledImage, "jpg", baos);
                byte[] imageData = baos.toByteArray();
                
                // Check size
                if (imageData.length > MAX_PACKET_SIZE) {
                    System.err.println("[UDP Broadcaster] Frame too large: " + imageData.length + " bytes");
                    Thread.sleep(DELAY_MS);
                    continue;
                }
                
                // Create screen frame
                ScreenFrame frame = new ScreenFrame(imageData, SCALE_WIDTH, SCALE_HEIGHT, username);
                frame.setFrameNumber(frameNumber++);
                
                // Broadcast to all registered clients
                synchronized (this) {
                    for (InetSocketAddress client : clients) {
                        broadcastToClient(client.getAddress().getHostAddress(), client.getPort(), frame);
                    }
                }
                
                if (clients.size() > 0) {
                    System.out.println("[UDP Broadcaster] Frame " + frame.getFrameNumber() + 
                        " broadcasted to " + clients.size() + " client(s) (" + imageData.length + " bytes)");
                }
                
                Thread.sleep(DELAY_MS);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Broadcast frame to specific client
     */
    private void broadcastToClient(String clientIP, int clientPort, ScreenFrame frame) {
        if (!broadcasting || socket == null) {
            return;
        }
        
        try {
            // Serialize frame
            ByteArrayOutputStream objStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(objStream);
            oos.writeObject(frame);
            oos.flush();
            byte[] packetData = objStream.toByteArray();
            
            // Send packet
            InetAddress address = InetAddress.getByName(clientIP);
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, clientPort);
            socket.send(packet);
            
        } catch (Exception e) {
            System.err.println("[UDP Broadcaster] Error sending to " + clientIP + ":" + clientPort + " - " + e.getMessage());
        }
    }
    
    /**
     * Scale image to target size
     */
    private BufferedImage scaleImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return scaled;
    }
    
    public boolean isBroadcasting() {
        return broadcasting;
    }
    
    public int getPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }
}
