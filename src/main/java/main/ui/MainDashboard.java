package main.ui;

import main.model.*;
import main.network.Client;
import main.network.MessageHandler;
import main.network.NotificationClient;
import main.network.PeerConnection;
import main.network.Server;
import main.util.NetworkUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Main Dashboard - Central hub for StudyConnect with Quiz, Broadcast & P2P Chat
 */
public class MainDashboard extends JFrame implements MessageHandler {
    private User currentUser;
    private Server server;
    private List<Client> connectedPeers;
    private Map<String, PeerConnection> peerConnections; // Track peer connections
    private Map<String, String> peerUsernames; // Map IP:Port -> Username
    private Map<PeerConnection, String> connectionUsernames; // Map Connection -> Username
    private NotificationClient notificationClient;
    
    // Group Chat components
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    
    // Broadcast components
    private JTextArea broadcastArea;
    private JTextField broadcastField;
    private JButton broadcastButton;
    
    // P2P Chat components
    private JComboBox<String> peerSelector;
    private JTextArea p2pChatArea;
    private JTextField p2pMessageField;
    private JButton p2pSendButton;
    
    // Quiz components
    private QuizCreatorPanel quizCreatorPanel;
    private QuizParticipationPanel quizParticipationPanel;
    private JTextArea leaderboardArea;
    private Quiz activeQuiz;
    private Map<String, QuizResult> quizResults;
    
    // UI Components
    private JButton startServerButton;
    private JButton stopServerButton;
    private JButton connectPeerButton;
    private JButton sendFileButton;
    private JLabel statusLabel;
    private JLabel ipLabel;
    private JTextField portField;
    private JList<String> peerList;
    private DefaultListModel<String> peerListModel;
    private JTabbedPane tabbedPane;
    
    public MainDashboard(User user) {
        this.currentUser = user;
        this.connectedPeers = new ArrayList<>();
        this.peerConnections = new HashMap<>();
        this.peerUsernames = new HashMap<>();
        this.connectionUsernames = new HashMap<>();
        this.peerListModel = new DefaultListModel<>();
        this.quizResults = new HashMap<>();
        
        // Start UDP listener for notifications
        notificationClient = new NotificationClient(msg -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    System.out.println("[DEBUG] " + currentUser.getUsername() + " showing notification: " + msg);
                    NotificationPopup popup = new NotificationPopup(this, msg);
                    popup.showPopup(3000); // Show for 3 seconds
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to show notification: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }, currentUser.getUsername());

        notificationClient.start();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        initComponents();
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        setTitle("StudyConnect - Dashboard");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 245, 245));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Top panel - Header
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Center panel - Split view
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setLeftComponent(createSidePanel());
        splitPane.setRightComponent(createTabbedPanel()); // Changed to tabbed panel
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // Bottom panel - Status bar
        mainPanel.add(createStatusPanel(), BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(66, 133, 244));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("Welcome, " + currentUser.getUsername() + "!");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(234, 67, 53));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> handleLogout());
        
        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(logoutButton, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createSidePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Check if user is admin
        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                          currentUser.getPassword().equals("admin");
        
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);
        
        if (isAdmin) {
            // Admin panel - Server Control ONLY
            JPanel serverPanel = new JPanel();
            serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.Y_AXIS));
            serverPanel.setBackground(Color.WHITE);
            serverPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(200, 200, 200)), "Server Control"));
            
            // IP Address display
            ipLabel = new JLabel("IP: " + NetworkUtil.getLocalIPAddress());
            ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            ipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Port field
            JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            portPanel.setOpaque(false);
            portPanel.add(new JLabel("Port:"));
            portField = new JTextField("8888", 8);
            portPanel.add(portField);
            portPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Start/Stop server buttons
            startServerButton = new JButton("Start Server");
            startServerButton.setBackground(new Color(52, 168, 83));
            startServerButton.setForeground(Color.WHITE);
            startServerButton.setFocusPainted(false);
            startServerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            startServerButton.setMaximumSize(new Dimension(200, 35));
            startServerButton.addActionListener(e -> startServer());
            
            stopServerButton = new JButton("Stop Server");
            stopServerButton.setBackground(new Color(234, 67, 53));
            stopServerButton.setForeground(Color.WHITE);
            stopServerButton.setFocusPainted(false);
            stopServerButton.setEnabled(false);
            stopServerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            stopServerButton.setMaximumSize(new Dimension(200, 35));
            stopServerButton.addActionListener(e -> stopServer());
            
            serverPanel.add(ipLabel);
            serverPanel.add(Box.createVerticalStrut(10));
            serverPanel.add(portPanel);
            serverPanel.add(Box.createVerticalStrut(10));
            serverPanel.add(startServerButton);
            serverPanel.add(Box.createVerticalStrut(5));
            serverPanel.add(stopServerButton);
            
            topPanel.add(serverPanel);
        } else {
            // Client panel - Connect to Server
            JPanel connectPanel = new JPanel();
            connectPanel.setLayout(new BoxLayout(connectPanel, BoxLayout.Y_AXIS));
            connectPanel.setBackground(Color.WHITE);
            connectPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(200, 200, 200)), "Connect to Server"));
            
            // Server IP input
            JPanel ipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            ipPanel.setOpaque(false);
            ipPanel.add(new JLabel("Server IP:"));
            JTextField serverIpField = new JTextField("127.0.0.1", 12);
            ipPanel.add(serverIpField);
            ipPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Port input
            JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            portPanel.setOpaque(false);
            portPanel.add(new JLabel("Port:"));
            JTextField serverPortField = new JTextField("8888", 8);
            portPanel.add(serverPortField);
            portPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Connect button
            JButton connectToServerButton = new JButton("Connect to Server");
            connectToServerButton.setBackground(new Color(66, 133, 244));
            connectToServerButton.setForeground(Color.WHITE);
            connectToServerButton.setFocusPainted(false);
            connectToServerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            connectToServerButton.setMaximumSize(new Dimension(200, 35));
            connectToServerButton.addActionListener(e -> {
                String ip = serverIpField.getText().trim();
                String portStr = serverPortField.getText().trim();
                
                if (ip.isEmpty() || portStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "Please enter server IP and port", 
                        "Connection Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    int port = Integer.parseInt(portStr);
                    connectToPeer(ip, port);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid port number", 
                        "Connection Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            connectPanel.add(ipPanel);
            connectPanel.add(Box.createVerticalStrut(5));
            connectPanel.add(portPanel);
            connectPanel.add(Box.createVerticalStrut(10));
            connectPanel.add(connectToServerButton);
            
            topPanel.add(connectPanel);
        }
        
        // Connected peers list (for both admin and client)
        JPanel peerListPanel = new JPanel(new BorderLayout());
        peerListPanel.setBackground(Color.WHITE);
        peerListPanel.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(200, 200, 200)), "Connected Peers"));
        
        peerList = new JList<>(peerListModel);
        peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane peerScrollPane = new JScrollPane(peerList);
        peerScrollPane.setPreferredSize(new Dimension(200, 150));
        
        peerListPanel.add(peerScrollPane, BorderLayout.CENTER);
        
        // Add all to side panel
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(peerListPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    
    private JPanel createTabbedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Check if user is admin
        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                          currentUser.getPassword().equals("admin");
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        // Tab 1: Group Chat & File Sharing
        tabbedPane.addTab("💬 Group Chat", createGroupChatTab());
        
        // Tab 2: Peer-to-Peer Chat
        tabbedPane.addTab("👥 P2P Chat", createP2PChatTab());
        
        // Tab 3: Broadcast
        tabbedPane.addTab("📢 Broadcast", createBroadcastTab());
        
        // Tab 4: Create Quiz (admin only)
        if (isAdmin) {
            tabbedPane.addTab("📊 Create Quiz", createQuizCreatorTab());
            // Tab 5: Leaderboard (admin can see results)
            tabbedPane.addTab("🏆 Leaderboard", createLeaderboardTab());
        } else {
            // Tab 4: Quiz Participation (non-admin only)
            tabbedPane.addTab("✏️ Take Quiz", createQuizParticipationTab());
            // Tab 5: Leaderboard (everyone can see)
            tabbedPane.addTab("🏆 Leaderboard", createLeaderboardTab());
        }
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createGroupChatTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Title
        JLabel titleLabel = new JLabel("Chat & File Sharing");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Message input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.addActionListener(e -> sendMessage());
        
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(66, 133, 244));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(80, 35));
        sendButton.addActionListener(e -> sendMessage());
        
        sendFileButton = new JButton("Send File");
        sendFileButton.setBackground(new Color(251, 188, 5));
        sendFileButton.setForeground(Color.WHITE);
        sendFileButton.setFocusPainted(false);
        sendFileButton.setPreferredSize(new Dimension(100, 35));
        sendFileButton.addActionListener(e -> sendFile());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(sendFileButton);
        buttonPanel.add(sendButton);
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(chatScrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createP2PChatTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Title
        JLabel titleLabel = new JLabel("Peer-to-Peer Chat");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Peer selector
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.setOpaque(false);
        selectorPanel.add(new JLabel("Chat with: "));
        peerSelector = new JComboBox<>();
        peerSelector.setPreferredSize(new Dimension(200, 30));
        peerSelector.addItem("Select a peer...");
        selectorPanel.add(peerSelector);
        
        // Chat area
        p2pChatArea = new JTextArea();
        p2pChatArea.setEditable(false);
        p2pChatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p2pChatArea.setLineWrap(true);
        p2pChatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(p2pChatArea);
        
        // Message input
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        p2pMessageField = new JTextField();
        p2pMessageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p2pMessageField.addActionListener(e -> sendP2PMessage());
        
        p2pSendButton = new JButton("Send");
        p2pSendButton.setBackground(new Color(66, 133, 244));
        p2pSendButton.setForeground(Color.WHITE);
        p2pSendButton.setFocusPainted(false);
        p2pSendButton.setPreferredSize(new Dimension(80, 35));
        p2pSendButton.addActionListener(e -> sendP2PMessage());
        
        inputPanel.add(p2pMessageField, BorderLayout.CENTER);
        inputPanel.add(p2pSendButton, BorderLayout.EAST);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(selectorPanel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(chatScrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createBroadcastTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Title
        JLabel titleLabel = new JLabel("Broadcast Messages");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Info label
        JLabel infoLabel = new JLabel("📢 Send messages to all connected peers simultaneously");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Broadcast history area
        broadcastArea = new JTextArea();
        broadcastArea.setEditable(false);
        broadcastArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        broadcastArea.setLineWrap(true);
        broadcastArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(broadcastArea);
        
        // Broadcast input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        broadcastField = new JTextField();
        broadcastField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        broadcastField.addActionListener(e -> sendBroadcast());
        
        broadcastButton = new JButton("📢 Broadcast to All");
        broadcastButton.setBackground(new Color(251, 188, 5));
        broadcastButton.setForeground(Color.WHITE);
        broadcastButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        broadcastButton.setFocusPainted(false);
        broadcastButton.setPreferredSize(new Dimension(160, 35));
        broadcastButton.addActionListener(e -> sendBroadcast());
        
        inputPanel.add(broadcastField, BorderLayout.CENTER);
        inputPanel.add(broadcastButton, BorderLayout.EAST);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoLabel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createQuizCreatorTab() {
        quizCreatorPanel = new QuizCreatorPanel(quiz -> {
            // When quiz is created
            activeQuiz = quiz;
            activeQuiz.start();
            
            // Broadcast quiz to all connected peers
            Message quizMsg = new Message(currentUser.getUsername(), "all",
                "New quiz started: " + quiz.getTitle(), Message.MessageType.QUIZ_START);
            quizMsg.setQuizData(quiz);
            
            for (Client client : connectedPeers) {
                client.sendMessage(quizMsg);
            }
            
            if (server != null && server.isRunning()) {
                server.broadcast(quizMsg);
            }
            
            appendToBroadcast("[QUIZ] Started: " + quiz.getTitle());
            JOptionPane.showMessageDialog(this, 
                "Quiz '" + quiz.getTitle() + "' has been started!\nAll connected peers will receive it.",
                "Quiz Started", JOptionPane.INFORMATION_MESSAGE);
        });
        
        return quizCreatorPanel;
    }
    
    private JPanel createQuizParticipationTab() {
        quizParticipationPanel = new QuizParticipationPanel(result -> {
            // When quiz is completed
            Message resultMsg = new Message(currentUser.getUsername(), "admin",
                "Quiz completed", Message.MessageType.QUIZ_ANSWER);
            resultMsg.setQuizAnswer(result);
            
            for (Client client : connectedPeers) {
                client.sendMessage(resultMsg);
            }
            
            if (server != null && server.isRunning()) {
                server.broadcast(resultMsg);
            }
        });
        
        // Set the username on the panel
        if (quizParticipationPanel instanceof QuizParticipationPanel) {
            ((QuizParticipationPanel) quizParticipationPanel).username = currentUser.getUsername();
        }
        
        return quizParticipationPanel;
    }
    
    private JPanel createLeaderboardTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Title
        JLabel titleLabel = new JLabel("🏆 Quiz Leaderboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Leaderboard area
        leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        leaderboardArea.setText("No quiz results yet.\n\nWait for a quiz to be completed to see rankings here.");
        JScrollPane scrollPane = new JScrollPane(leaderboardArea);
        
        // Refresh button
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.setBackground(new Color(66, 133, 244));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> updateLeaderboard());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(refreshButton);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        statusLabel = new JLabel("Status: Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(statusLabel);
        
        return panel;
    }
    
    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText());
            
            if (!NetworkUtil.isValidPort(port)) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a valid port (1024-65535)",
                    "Invalid Port", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            server = new Server(port, this, currentUser.getUsername());
            server.start();
            
            startServerButton.setEnabled(false);
            stopServerButton.setEnabled(true);
            portField.setEnabled(false);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid port number",
                "Invalid Port", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
        
        startServerButton.setEnabled(true);
        stopServerButton.setEnabled(false);
        portField.setEnabled(true);
    }
    
    // Overloaded method that accepts IP and port directly
    private void connectToPeer(String ip, int port) {
        if (!NetworkUtil.isValidIPAddress(ip)) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid IP address",
                "Invalid IP", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!NetworkUtil.isValidPort(port)) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid port (1024-65535)",
                "Invalid Port", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            Client client = new Client(ip, port, this, currentUser.getUsername());
            if (client.connect()) {
                connectedPeers.add(client);
                peerListModel.addElement(ip + ":" + port);
                
                // Send greeting message
                Message greeting = new Message(currentUser.getUsername(), "all",
                    currentUser.getUsername() + " has joined the chat",
                    Message.MessageType.USER_JOIN);
                client.sendMessage(greeting);
                
                // Update peer selector to show server/admin
                SwingUtilities.invokeLater(() -> {
                    peerSelector.removeAllItems();
                    peerSelector.addItem("Select a peer...");
                    peerSelector.addItem("Server (Admin)");
                    
                    // Store server connection for P2P messaging
                    String peerAddress = ip + ":" + port;
                    peerUsernames.put(peerAddress, "Server (Admin)");
                });
                
                appendToChat("[SYSTEM] ✅ Connected to " + ip + ":" + port);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Failed to connect: " + e.getMessage(),
                "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void connectToPeer() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.add(new JLabel("IP Address:"));
        JTextField ipField = new JTextField();
        panel.add(ipField);
        panel.add(new JLabel("Port:"));
        JTextField peerPortField = new JTextField("8888");
        panel.add(peerPortField);
        
        int result = JOptionPane.showConfirmDialog(this, panel,
            "Connect to Peer", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String ip = ipField.getText().trim();
            
            try {
                int port = Integer.parseInt(peerPortField.getText());
                connectToPeer(ip, port); // Use the overloaded method
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a valid port number",
                    "Invalid Port", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        
        Message message = new Message(currentUser.getUsername(), "all", text, Message.MessageType.TEXT);
        
        // Send to all connected peers (both clients and server connections)
        for (Client client : connectedPeers) {
            client.sendMessage(message);
        }
        
        if (server != null && server.isRunning()) {
            server.broadcast(message);
        }
        
        // Display in chat area
        appendToChat("You: " + text);
        messageField.setText("");
    }
    
    private void sendP2PMessage() {
        String text = p2pMessageField.getText().trim();
        String selectedPeer = (String) peerSelector.getSelectedItem();
        
        if (text.isEmpty() || selectedPeer == null || selectedPeer.equals("Select a peer...")) {
            JOptionPane.showMessageDialog(this,
                "Please select a peer and type a message",
                "No Peer Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create P2P message with target username
        Message message = new Message(currentUser.getUsername(), selectedPeer, 
            text, Message.MessageType.PEER_TO_PEER);
        
        // Send through server (first connected peer is always the server)
        if (!connectedPeers.isEmpty()) {
            connectedPeers.get(0).sendMessage(message);
            appendToP2PChat("You → " + selectedPeer + ": " + text);
            p2pMessageField.setText("");
        } else {
            JOptionPane.showMessageDialog(this,
                "Not connected to server!",
                "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void sendBroadcast() {
        String text = broadcastField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        
        Message message = new Message(currentUser.getUsername(), "all", text, Message.MessageType.BROADCAST);
        
        // Send to all connected peers
        for (Client client : connectedPeers) {
            client.sendMessage(message);
        }
        
        if (server != null && server.isRunning()) {
            server.broadcast(message);
        }
        
        // Display in broadcast area
        appendToBroadcast("[BROADCAST] You: " + text);
        broadcastField.setText("");
    }
    
    private void updateLeaderboard() {
        if (quizResults.isEmpty()) {
            leaderboardArea.setText("No quiz results yet.\n\nWait for a quiz to be completed to see rankings here.");
            return;
        }
        
        // Sort results by score
        List<Map.Entry<String, QuizResult>> sortedResults = new ArrayList<>(quizResults.entrySet());
        sortedResults.sort((a, b) -> Integer.compare(b.getValue().getEarnedPoints(), a.getValue().getEarnedPoints()));
        
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════\n");
        sb.append("                    🏆 QUIZ LEADERBOARD 🏆\n");
        sb.append("═══════════════════════════════════════════════════════\n\n");
        sb.append(String.format("%-5s %-20s %-10s %-10s %-10s\n", "Rank", "Player", "Score", "Percent", "Grade"));
        sb.append("───────────────────────────────────────────────────────\n");
        
        int rank = 1;
        for (Map.Entry<String, QuizResult> entry : sortedResults) {
            String username = entry.getKey();
            QuizResult result = entry.getValue();
            
            String medal = "";
            if (rank == 1) medal = "🥇";
            else if (rank == 2) medal = "🥈";
            else if (rank == 3) medal = "🥉";
            
            sb.append(String.format("%-5s %-20s %-10d %-10.1f%% %-10s\n",
                medal + rank, username, result.getEarnedPoints(), 
                result.getPercentage(), result.getGrade()));
            rank++;
        }
        
        sb.append("───────────────────────────────────────────────────────\n");
        sb.append(String.format("\nTotal Participants: %d\n", quizResults.size()));
        
        leaderboardArea.setText(sb.toString());
    }
    
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            
            try {
                byte[] fileData = main.util.FileUtil.readFileToBytes(file);
                FileTransfer transfer = new FileTransfer(file.getName(), file.length(),
                    fileData, currentUser.getUsername(), "all");
                
                // Send to all connected peers
                for (Client client : connectedPeers) {
                    client.sendFile(transfer);
                }
                
                appendToChat("System: Sent file - " + file.getName() + 
                    " (" + transfer.getFileSizeFormatted() + ")");
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error sending file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void handleLogout() {
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?",
            "Confirm Logout", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            // Clean up connections
            if (server != null) {
                server.stop();
            }
            
            for (Client client : connectedPeers) {
                client.disconnect();
            }
            
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }
    
    private void appendToChat(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    private void appendToP2PChat(String text) {
        SwingUtilities.invokeLater(() -> {
            p2pChatArea.append(text + "\n");
            p2pChatArea.setCaretPosition(p2pChatArea.getDocument().getLength());
        });
    }
    
    private void appendToBroadcast(String text) {
        SwingUtilities.invokeLater(() -> {
            broadcastArea.append(text + "\n");
            broadcastArea.setCaretPosition(broadcastArea.getDocument().getLength());
        });
    }
    
    private void updatePeerSelector(PeerConnection connection) {
        SwingUtilities.invokeLater(() -> {
            // Rebuild peer selector list
            peerSelector.removeAllItems();
            peerSelector.addItem("Select a peer...");
            
            boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                              currentUser.getPassword().equals("admin");
            
            // If this is NOT admin (client), add "Server (Admin)" option
            if (!isAdmin && !connectedPeers.isEmpty()) {
                peerSelector.addItem("Server (Admin)");
            }
            
            // Add all server connections with usernames (admin side)
            for (Map.Entry<PeerConnection, String> entry : connectionUsernames.entrySet()) {
                String username = entry.getValue();
                if (!username.equals(currentUser.getUsername())) {
                    peerSelector.addItem(username);
                    // Also store reverse mapping for sending
                    peerConnections.put(username, entry.getKey());
                }
            }
            
            // Add all connected clients with usernames (client side)
            for (Client client : connectedPeers) {
                String peerAddress = client.getHost() + ":" + client.getPort();
                String username = peerUsernames.get(peerAddress);
                if (username != null && !username.equals(currentUser.getUsername()) 
                    && !username.equals("Server (Admin)")) {
                    peerSelector.addItem(username);
                }
            }
        });
    }
    
    // MessageHandler implementation
    @Override
    public void onMessageReceived(Message message, PeerConnection connection) {
        switch (message.getType()) {
            case TEXT:
                appendToChat(message.getSender() + ": " + message.getContent());
                break;
                
            case BROADCAST:
                appendToBroadcast("[BROADCAST] " + message.getSender() + ": " + message.getContent());
                break;
                
            case PEER_TO_PEER:
                // Display received P2P message
                appendToP2PChat(message.getSender() + " → You: " + message.getContent());
                break;
                
            case USER_JOIN:
                // Track username for this connection
                String username = message.getSender();
                String peerAddr = connection.getPeerAddress();
                connectionUsernames.put(connection, username);
                peerUsernames.put(peerAddr, username);
                
                // Update peer list with username
                boolean found = false;
                for (int i = 0; i < peerListModel.size(); i++) {
                    if (peerListModel.get(i).contains(peerAddr)) {
                        peerListModel.set(i, username + " (" + peerAddr + ")");
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    peerListModel.addElement(username + " (" + peerAddr + ")");
                }
                
                // Show connection message
                boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                                  currentUser.getPassword().equals("admin");
                if (isAdmin) {
                    appendToChat("[SYSTEM] Client connected: " + username + " (" + peerAddr + ")");
                } else {
                    appendToChat("[SYSTEM] " + message.getContent());
                }
                
                // Update peer selector with username
                updatePeerSelector(connection);
                break;
                
            case USER_LEAVE:
                String leavingUser = connectionUsernames.get(connection);
                if (leavingUser != null) {
                    appendToChat("[SYSTEM] " + leavingUser + " has left");
                    connectionUsernames.remove(connection);
                    
                    // Remove from peer list
                    for (int i = 0; i < peerListModel.size(); i++) {
                        if (peerListModel.get(i).contains(leavingUser)) {
                            peerListModel.remove(i);
                            break;
                        }
                    }
                }
                break;
                
            case PEER_LIST:
                // Received list of connected peers from server
                String[] peers = message.getContent().split(",");
                
                SwingUtilities.invokeLater(() -> {
                    peerSelector.removeAllItems();
                    peerSelector.addItem("Select a peer...");
                    
                    for (String peer : peers) {
                        if (!peer.trim().isEmpty() && 
                            !peer.trim().equals(currentUser.getUsername())) {
                            peerSelector.addItem(peer.trim());
                        }
                    }
                    
                    System.out.println("[DEBUG] Updated peer list: " + Arrays.toString(peers));
                });
                break;
                
            case QUIZ_START:
                // New quiz started
                Quiz quiz = message.getQuizData();
                if (quiz != null && quizParticipationPanel != null) {
                    activeQuiz = quiz;
                    quizParticipationPanel.startQuiz(quiz);
                    appendToChat("[QUIZ] New quiz available: " + quiz.getTitle());
                    JOptionPane.showMessageDialog(this,
                        "New quiz started: " + quiz.getTitle() + 
                        "\nGo to 'Take Quiz' tab to participate!",
                        "Quiz Notification", JOptionPane.INFORMATION_MESSAGE);
                    
                    // Automatically switch to quiz tab (non-admin only)
                    boolean isAdminUser = currentUser.getUsername().equalsIgnoreCase("admin") && 
                                      currentUser.getPassword().equals("admin");
                    if (!isAdminUser) {
                        // For non-admin users, "Take Quiz" tab is at index 3
                        tabbedPane.setSelectedIndex(3);
                    }
                }
                break;
                
            case QUIZ_ANSWER:
                // Quiz answer received (admin side)
                QuizAnswer answer = message.getQuizAnswer();
                if (answer != null && activeQuiz != null) {
                    // Calculate the score based on correct answers
                    int correctCount = 0;
                    int totalPoints = 0;
                    int earnedPoints = 0;
                    
                    List<QuizQuestion> questions = activeQuiz.getQuestions();
                    for (int i = 0; i < questions.size(); i++) {
                        QuizQuestion question = questions.get(i);
                        Integer userAnswer = answer.getAnswers().get(i);
                        
                        totalPoints += question.getPoints();
                        
                        if (userAnswer != null && userAnswer == question.getCorrectAnswer()) {
                            correctCount++;
                            earnedPoints += question.getPoints();
                        }
                    }
                    
                    // Create quiz result
                    QuizResult result = new QuizResult(
                        activeQuiz.getId(),
                        message.getSender(),
                        questions.size(),
                        correctCount,
                        totalPoints,
                        earnedPoints
                    );
                    
                    quizResults.put(message.getSender(), result);
                    updateLeaderboard();
                    appendToChat("[QUIZ] " + message.getSender() + " completed the quiz. Score: " + 
                        result.getEarnedPoints() + "/" + result.getTotalPoints());
                    
                    // Send result back to the client
                    Message resultMsg = new Message("Server", message.getSender(),
                        "Quiz completed", Message.MessageType.QUIZ_RESULT);
                    resultMsg.setQuizResult(result);
                    connection.sendMessage(resultMsg);
                }
                break;
                
            case QUIZ_RESULT:
                // Quiz result received
                QuizResult myResult = message.getQuizResult();
                if (myResult != null) {
                    // Store the result in the local quizResults map
                    quizResults.put(currentUser.getUsername(), myResult);
                    
                    // Update the leaderboard to show the result
                    updateLeaderboard();
                    
                    // Switch to leaderboard tab to show results
                    boolean isAdminUser = currentUser.getUsername().equalsIgnoreCase("admin") && 
                                      currentUser.getPassword().equals("admin");
                    if (!isAdminUser) {
                        // For non-admin users, "Leaderboard" tab is at index 4
                        tabbedPane.setSelectedIndex(4);
                    }
                    
                    // Show result dialog
                    JOptionPane.showMessageDialog(this,
                        String.format("Quiz Results:\n\n" +
                            "Correct Answers: %d / %d\n" +
                            "Points Earned: %d / %d\n" +
                            "Percentage: %.1f%%\n" +
                            "Grade: %s",
                            myResult.getCorrectAnswers(), myResult.getTotalQuestions(),
                            myResult.getEarnedPoints(), myResult.getTotalPoints(),
                            myResult.getPercentage(), myResult.getGrade()),
                        "Your Quiz Results", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
                
            default:
                break;
        }
    }
    
    @Override
    public void onFileReceived(FileTransfer fileTransfer, PeerConnection connection) {
        SwingUtilities.invokeLater(() -> {
            int result = JOptionPane.showConfirmDialog(this,
                "Receive file: " + fileTransfer.getFileName() + 
                " (" + fileTransfer.getFileSizeFormatted() + ") from " + 
                fileTransfer.getSender() + "?",
                "File Received", JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new java.io.File(fileTransfer.getFileName()));
                
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        main.util.FileUtil.writeBytesToFile(fileTransfer.getFileData(),
                            fileChooser.getSelectedFile());
                        appendToChat("System: File saved - " + fileTransfer.getFileName());
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this,
                            "Error saving file: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }
    
    @Override
    public void onServerStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + status);
            appendToChat("System: " + status);
        });
    }
}
