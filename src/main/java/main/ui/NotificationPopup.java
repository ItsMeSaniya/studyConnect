package main.ui;

import javax.swing.*;
import java.awt.*;

public class NotificationPopup extends JDialog {

    public NotificationPopup(Frame owner, String message) {
        super(owner, false); // Non-modal dialog
        setUndecorated(true); // Remove title bar
        setBackground(new Color(0, 0, 0, 0)); // Transparent background for rounded corners
        
        System.out.println("[DEBUG] Creating NotificationPopup with message: " + message);
        
        // Create a rounded panel
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
            }
        };
        
        contentPanel.setLayout(new BorderLayout(10, 10));
        contentPanel.setBackground(new Color(60, 63, 65));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        // Add icon and message
        JPanel messagePanel = new JPanel(new BorderLayout(10, 0));
        messagePanel.setOpaque(false);
        
        // Notification icon
        JLabel iconLabel = new JLabel("[!]");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Message label with HTML for better text wrapping
        JLabel messageLabel = new JLabel("<html><div style='width: 250px; color: white; font-family: Segoe UI; font-size: 13px;'>" + 
                                        message + "</div></html>");
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        messagePanel.add(iconLabel, BorderLayout.WEST);
        messagePanel.add(messageLabel, BorderLayout.CENTER);
        
        // Close button
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        closeButton.setForeground(new Color(200, 200, 200));
        closeButton.setBackground(new Color(80, 80, 80));
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        
        // Hover effects for close button
        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeButton.setBackground(new Color(100, 100, 100));
                closeButton.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeButton.setBackground(new Color(80, 80, 80));
                closeButton.setForeground(new Color(200, 200, 200));
            }
        });
        
        contentPanel.add(messagePanel, BorderLayout.CENTER);
        contentPanel.add(closeButton, BorderLayout.EAST);
        
        setContentPane(contentPanel);
        pack();
        
        // Position relative to main window's top-right
        positionRelativeToOwner();
        
        setAlwaysOnTop(true);
        
        // Add shadow effect
        getRootPane().setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
        
        // Add fade-in animation
        setOpacity(0f);
        
        // Add listeners for debugging
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                System.out.println("[DEBUG] NotificationPopup shown");
            }
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                System.out.println("[DEBUG] NotificationPopup hidden");
            }
        });
    }
    
    private void positionRelativeToOwner() {
        Frame owner = (Frame) getOwner();
        if (owner != null && owner.isVisible()) {
            // Get owner window location and size
            Point ownerLocation = owner.getLocation();
            Dimension ownerSize = owner.getSize();
            Dimension popupSize = getSize();
            
            // Calculate position relative to owner's top-right
            int x = ownerLocation.x + ownerSize.width - popupSize.width - 20; // 20px from right edge
            int y = ownerLocation.y + 20; // 20px from top edge
            
            // Make sure it doesn't go off-screen
            Rectangle screenBounds = getGraphicsConfiguration().getBounds();
            if (x + popupSize.width > screenBounds.x + screenBounds.width) {
                x = screenBounds.x + screenBounds.width - popupSize.width - 10;
            }
            if (y + popupSize.height > screenBounds.y + screenBounds.height) {
                y = screenBounds.y + screenBounds.height - popupSize.height - 10;
            }
            if (x < screenBounds.x) {
                x = screenBounds.x + 10;
            }
            if (y < screenBounds.y) {
                y = screenBounds.y + 10;
            }
            
            setLocation(x, y);
        } else {
            // Fallback to screen top-right if owner not available
            positionScreenTopRight();
        }
    }
    
    private void positionScreenTopRight() {
        GraphicsConfiguration config = getGraphicsConfiguration();
        Rectangle bounds = config.getBounds();
        
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(config);
        int topMargin = screenInsets.top + 20;
        int rightMargin = 20;
        
        int x = bounds.x + bounds.width - getWidth() - rightMargin;
        int y = bounds.y + topMargin;
        
        setLocation(x, y);
    }

    public void showPopup(int durationMillis) {
        System.out.println("[DEBUG] showPopup called");
        
        // Fade-in animation
        Timer fadeInTimer = new Timer(10, null);
        fadeInTimer.addActionListener(new java.awt.event.ActionListener() {
            float opacity = 0f;
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                opacity += 0.1f;
                if (opacity >= 1f) {
                    opacity = 1f;
                    setOpacity(opacity);
                    fadeInTimer.stop();
                } else {
                    setOpacity(opacity);
                }
            }
        });
        
        setVisible(true);
        fadeInTimer.start();

        // Auto-close timer
        Timer closeTimer = new Timer(durationMillis, e -> {
            System.out.println("[DEBUG] Timer expired, disposing popup");
            
            // Fade-out animation before disposing
            Timer fadeOutTimer = new Timer(10, null);
            fadeOutTimer.addActionListener(new java.awt.event.ActionListener() {
                float opacity = 1f;
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    opacity -= 0.1f;
                    if (opacity <= 0f) {
                        opacity = 0f;
                        setOpacity(opacity);
                        fadeOutTimer.stop();
                        dispose();
                    } else {
                        setOpacity(opacity);
                    }
                }
            });
            fadeOutTimer.start();
        });
        closeTimer.setRepeats(false);
        closeTimer.start();
        
        // Add mouse listener to pause timer when hovered
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeTimer.stop();
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeTimer.restart();
            }
        });
    }
    
    // Optional: Override paint to create rounded corners
    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Clear with transparent background
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw rounded rectangle background
        g2d.setColor(getContentPane().getBackground());
        g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        
        // Draw border
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        
        super.paint(g2d);
        g2d.dispose();
    }
}