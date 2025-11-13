package main.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive whiteboard panel for drawing and annotations
 */
public class WhiteboardPanel extends JPanel {
    private BufferedImage canvas;
    private Graphics2D g2d;
    private Point lastPoint;
    private Color currentColor = Color.BLACK;
    private int brushSize = 3;
    private List<DrawingListener> listeners = new ArrayList<>();
    
    public interface DrawingListener {
        void onDrawingUpdate(BufferedImage image);
    }
    
    public WhiteboardPanel(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.WHITE);
        
        // Initialize canvas
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        g2d = canvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(currentColor);
        g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Mouse listeners for drawing
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getPoint();
                if (lastPoint != null) {
                    g2d.setColor(currentColor);
                    g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y);
                    lastPoint = currentPoint;
                    repaint();
                    notifyListeners();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, null);
    }
    
    public void setDrawingColor(Color color) {
        this.currentColor = color;
    }
    
    public void setBrushSize(int size) {
        this.brushSize = size;
    }
    
    public void clear() {
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g2d.setColor(currentColor);
        repaint();
        notifyListeners();
    }
    
    public void setCanvasImage(BufferedImage image) {
        if (image != null) {
            g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            repaint();
        }
    }
    
    public BufferedImage getCanvasImage() {
        return canvas;
    }
    
    public void addDrawingListener(DrawingListener listener) {
        listeners.add(listener);
    }
    
    public void removeDrawingListener(DrawingListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners() {
        for (DrawingListener listener : listeners) {
            listener.onDrawingUpdate(canvas);
        }
    }
}
