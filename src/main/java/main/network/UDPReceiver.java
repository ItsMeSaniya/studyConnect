package main.network;

import main.model.ScreenFrame;

import java.io.*;
import java.net.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * UDP receiver for screen sharing
 * Receives screen frames from broadcaster
 */
public class UDPReceiver {
    private DatagramSocket socket;
    private boolean receiving;
    private Thread receiveThread;
    private int port;
    private ScreenFrameListener listener;
    
    private static final int BUFFER_SIZE = 70000; // Larger than max packet
    
    public interface ScreenFrameListener {
        void onFrameReceived(BufferedImage image, ScreenFrame frame);
        void onError(String error);
    }
    
    public UDPReceiver(int port, ScreenFrameListener listener) {
        this.port = port;
        this.listener = listener;
    }
    
    /**
     * Start receiving frames
     */
    public boolean start() {
        try {
            socket = new DatagramSocket(port);
            receiving = true;
            
            receiveThread = new Thread(this::receiveFrames);
            receiveThread.setDaemon(true);
            receiveThread.setName("ScreenReceiver-" + port);
            receiveThread.start();
            
            System.out.println("[UDP Receiver] Started on port " + port);
            return true;
        } catch (SocketException e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onError("Failed to start receiver: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Stop receiving
     */
    public void stop() {
        receiving = false;
        
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        System.out.println("[UDP Receiver] Stopped");
    }
    
    /**
     * Receive and process frames
     */
    private void receiveFrames() {
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (receiving && !Thread.currentThread().isInterrupted()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // Deserialize
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);
                ScreenFrame frame = (ScreenFrame) ois.readObject();
                
                // Convert byte array to image
                ByteArrayInputStream imageStream = new ByteArrayInputStream(frame.getImageData());
                BufferedImage image = ImageIO.read(imageStream);
                
                if (image != null && listener != null) {
                    listener.onFrameReceived(image, frame);
                }
                
            } catch (SocketException e) {
                if (receiving) {
                    System.err.println("[UDP Receiver] Socket error: " + e.getMessage());
                }
                break;
            } catch (IOException | ClassNotFoundException e) {
                if (receiving) {
                    System.err.println("[UDP Receiver] Error receiving frame: " + e.getMessage());
                }
            }
        }
    }
    
    public boolean isReceiving() {
        return receiving;
    }
    
    public int getPort() {
        return port;
    }
    
    /**
     * Get the actual local port the socket is bound to
     * Useful when port was set to 0 (auto-assign)
     */
    public int getLocalPort() {
        if (socket != null && !socket.isClosed()) {
            return socket.getLocalPort();
        }
        return port;
    }
}
