package main.model;

import java.io.Serializable;

/**
 * Represents a screen frame/screenshot for UDP broadcasting
 */
public class ScreenFrame implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private byte[] imageData;
    private int width;
    private int height;
    private long timestamp;
    private int frameNumber;
    private String sender;
    
    public ScreenFrame(byte[] imageData, int width, int height, String sender) {
        this.imageData = imageData;
        this.width = width;
        this.height = height;
        this.sender = sender;
        this.timestamp = System.currentTimeMillis();
    }
    
    public byte[] getImageData() {
        return imageData;
    }
    
    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getFrameNumber() {
        return frameNumber;
    }
    
    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public int getDataSize() {
        return imageData != null ? imageData.length : 0;
    }
}
