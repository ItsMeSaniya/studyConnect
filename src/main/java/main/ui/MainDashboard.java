package main.ui;

import main.model.*;
import main.network.Client;
import main.network.MessageHandler;
import main.network.PeerConnection;
import main.network.Server;
import main.network.UDPBroadcaster;
import main.network.UDPReceiver;
import main.util.NetworkUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Main Dashboard - Central hub for StudyConnect with Quiz, Broadcast & P2P Chat
 */
public class MainDashboard extends JFrame implements MessageHandler {
    private User currentUser;
    private Server server;
    private Client serverClient;  // Client connection to server
    private List<Client> connectedPeers;
    private Map<String, PeerConnection> peerConnections; // Track peer connections
    private Map<String, String> peerUsernames; // Map IP:Port -> Username
    private Map<PeerConnection, String> connectionUsernames; // Map Connection -> Username

    // Group Chat components
    private JPanel chatMessagesPanel;  // Modern chat UI with bubbles
    private JScrollPane chatScrollPane;
    private JTextArea systemMessagesArea;  // For admin only - system messages
    private JTextField messageField;
    private JButton sendButton;
    
    // P2P Chat components
    private JTabbedPane p2pChatTabs;  // Tabs for different P2P conversations
    private Map<String, JPanel> p2pChatPanels;  // Map of peer -> chat panel
    private Map<String, JTextField> p2pInputFields;  // Map of peer -> input field
    private JList<String> p2pPeerList;  // List of available peers for P2P
    private DefaultListModel<String> p2pPeerListModel;
    
    // Broadcast components
    private JTextArea broadcastArea;
    private JTextField broadcastField;
    private JButton broadcastButton;

    // P2P Chat components
    private JComboBox<String> peerSelector;
    private JTextArea p2pChatArea;
    private JTextField p2pMessageField;
    private JButton p2pSendButton;

    // File sharing components
    private JComboBox<String> fileTargetSelector;
    private JTextArea fileHistoryArea;
    private DefaultListModel<FileMetadata> sharedFilesListModel;
    private JList<FileMetadata> sharedFilesList;
    private JProgressBar uploadProgressBar;
    private JLabel uploadStatusLabel;
    private List<FileMetadata> availableFiles;
    private Map<String, String> pendingDownloads; // Map fileId -> save path
    
    // Quiz components
    private QuizCreatorPanel quizCreatorPanel;
    private QuizParticipationPanel quizParticipationPanel;
    private JTextArea leaderboardArea;
    private JTextArea studentLeaderboardArea; // For students to view shared leaderboard
    private Quiz activeQuiz;
    private Map<String, QuizResult> quizResults;

    // UI Components
    private JButton startServerButton;
    private JButton stopServerButton;
    private JButton connectPeerButton;
    private JButton sendFileButton;
    private JButton connectToServerButton;
    private JButton disconnectFromServerButton;
    private JTextField serverIpField;
    private JTextField serverPortField;
    private JLabel statusLabel;
    private JLabel ipLabel;
    private JTextField portField;
    private int serverPort = 0; // Store the server port for students
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
        this.p2pChatPanels = new HashMap<>();
        this.p2pInputFields = new HashMap<>();
        this.p2pPeerListModel = new DefaultListModel<>();
        this.sharedFilesListModel = new DefaultListModel<>();
        this.availableFiles = new ArrayList<>();
        this.pendingDownloads = new HashMap<>();
        
        // Start UDP listener for notifications - but don't show popups
        notificationClient = new NotificationClient(msg -> {
            // Popup notifications disabled - just log to console
            System.out.println("[NOTIFICATION] " + msg);
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

            // IP Address display (Local and Public)
            String localIP = NetworkUtil.getLocalIPAddress();
            String publicIP = getPublicIP();

            ipLabel = new JLabel("<html><b>Local IP:</b> " + localIP + "<br>" +
                    "<b>Public IP:</b> " + publicIP + "</html>");
            ipLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            ipLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Connection instructions
            JLabel infoLabel = new JLabel("<html><font size='2' color='gray'>" +
                    "Local: Same network users<br>" +
                    "Public: Internet users (port forward 8888)</font></html>");
            infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

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
            serverPanel.add(Box.createVerticalStrut(5));
            serverPanel.add(infoLabel);
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
            serverIpField = new JTextField("", 12);
            serverIpField.setToolTipText("Enter the admin's IP address (e.g., 192.168.1.3)");
            ipPanel.add(serverIpField);
            ipPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Help label
            JLabel helpLabel = new JLabel(
                    "<html><i> Ask admin for their IP address<br/>(NOT 127.0.0.1 unless on same PC)</i></html>");
            helpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            helpLabel.setForeground(new Color(100, 100, 100));
            helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Port input
            JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            portPanel.setOpaque(false);
            portPanel.add(new JLabel("Port:"));
            serverPortField = new JTextField("8888", 8);
            portPanel.add(serverPortField);
            portPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Connect button
            connectToServerButton = new JButton("Connect to Server");
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
                    
                    // Disable connect button and show connecting state
                    connectToServerButton.setEnabled(false);
                    connectToServerButton.setText("Connecting...");
                    
                    // Attempt connection
                    if (!NetworkUtil.isValidIPAddress(ip)) {
                        JOptionPane.showMessageDialog(this,
                            "Please enter a valid IP address",
                            "Invalid IP", JOptionPane.ERROR_MESSAGE);
                        connectToServerButton.setEnabled(true);
                        connectToServerButton.setText("Connect to Server");
                        return;
                    }
                    
                    if (!NetworkUtil.isValidPort(port)) {
                        JOptionPane.showMessageDialog(this,
                            "Please enter a valid port (1024-65535)",
                            "Invalid Port", JOptionPane.ERROR_MESSAGE);
                        connectToServerButton.setEnabled(true);
                        connectToServerButton.setText("Connect to Server");
                        return;
                    }
                    
                    serverClient = new Client(ip, port, this, currentUser.getUsername());
                    if (serverClient.connect()) {
                        // Store the server port for later use (e.g., UDP screen sharing)
                        serverPort = port;
                        
                        connectedPeers.add(serverClient);
                        peerListModel.addElement(ip + ":" + port);
                        
                        // Send greeting message
                        Message greeting = new Message(currentUser.getUsername(), "all",
                            currentUser.getUsername() + " has joined the chat",
                            Message.MessageType.USER_JOIN);
                        serverClient.sendMessage(greeting);
                        
                        // Update UI - keep connect button disabled, enable disconnect
                        connectToServerButton.setText("Connected");
                        disconnectFromServerButton.setEnabled(true);
                        statusLabel.setText("Status: Connected to " + ip + ":" + port);
                        statusLabel.setForeground(new Color(76, 175, 80));
                    } else {
                        // Connection failed
                        JOptionPane.showMessageDialog(this,
                            "Failed to connect to server at " + ip + ":" + port,
                            "Connection Failed", JOptionPane.ERROR_MESSAGE);
                        connectToServerButton.setEnabled(true);
                        connectToServerButton.setText("Connect to Server");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid port number", 
                        "Connection Error", 
                        JOptionPane.ERROR_MESSAGE);
                    connectToServerButton.setEnabled(true);
                    connectToServerButton.setText("Connect to Server");
                }
            });
            
            // Disconnect button
            disconnectFromServerButton = new JButton("Disconnect from Server");
            disconnectFromServerButton.setBackground(new Color(244, 67, 54));
            disconnectFromServerButton.setForeground(Color.WHITE);
            disconnectFromServerButton.setFocusPainted(false);
            disconnectFromServerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            disconnectFromServerButton.setMaximumSize(new Dimension(200, 35));
            disconnectFromServerButton.setEnabled(false);  // Initially disabled
            disconnectFromServerButton.addActionListener(e -> {
                disconnectFromServer();
            });
            
            connectPanel.add(ipPanel);
            connectPanel.add(Box.createVerticalStrut(3));
            connectPanel.add(helpLabel);
            connectPanel.add(Box.createVerticalStrut(5));
            connectPanel.add(portPanel);
            connectPanel.add(Box.createVerticalStrut(10));
            connectPanel.add(connectToServerButton);
            connectPanel.add(Box.createVerticalStrut(5));
            connectPanel.add(disconnectFromServerButton);
            
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

        // Tab 1: Study Group Chat
        tabbedPane.addTab("Study Chat", createGroupChatTab());
        
        // Tab 2: P2P Chat
        tabbedPane.addTab("P2P Chat", createP2PChatTab());

        // Tab 3: Resource Sharing
        tabbedPane.addTab("Resources", createFileSharingTab());
        
        // Tab 4: Announcements
        tabbedPane.addTab("Announcements", createBroadcastTab());
        
        // Tab 5: Create Quiz (admin only)
        if (isAdmin) {
            tabbedPane.addTab("Start Class", createStartClassTab());
            tabbedPane.addTab("Create Quiz", createQuizCreatorTab());
            // Tab 6: Results Dashboard (admin can see all results)
            tabbedPane.addTab("Results", createLeaderboardTab());
        } else {
            // Tab 5: Join Class (students)
            tabbedPane.addTab("Join Class", createJoinClassTab());
            // Tab 6: Quiz Participation (non-admin only)
            tabbedPane.addTab("Take Quiz", createQuizParticipationTab());
            // Tab 5: My Results (students can see when shared)
            tabbedPane.addTab("Results", createStudentLeaderboardTab());
        }

        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGroupChatTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Check if admin
        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                         currentUser.getPassword().equals("admin");
        
        // Title
        JLabel titleLabel = new JLabel("💬 Study Group Chat");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Info label
        String infoText = isAdmin ? 
            "Monitor and participate in group discussions" :
            "Chat with your study group members";
        JLabel infoLabel = new JLabel(infoText);
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoLabel, BorderLayout.SOUTH);
        
        // For admin: Create split pane with system messages and chat
        if (isAdmin) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPane.setResizeWeight(0.3);
            splitPane.setBorder(null);
            
            // System Messages Panel (top)
            JPanel systemPanel = new JPanel(new BorderLayout());
            systemPanel.setBackground(Color.WHITE);
            systemPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(200, 200, 200)), "🔔 System Messages"));
            
            systemMessagesArea = new JTextArea();
            systemMessagesArea.setEditable(false);
            systemMessagesArea.setFont(new Font("Consolas", Font.PLAIN, 12));
            systemMessagesArea.setLineWrap(true);
            systemMessagesArea.setWrapStyleWord(true);
            systemMessagesArea.setBackground(new Color(245, 245, 245));
            systemMessagesArea.setForeground(new Color(60, 60, 60));
            JScrollPane systemScrollPane = new JScrollPane(systemMessagesArea);
            systemScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            systemPanel.add(systemScrollPane, BorderLayout.CENTER);
            
            // Chat Messages Panel (bottom)
            JPanel chatPanel = createModernChatPanel();
            
            splitPane.setTopComponent(systemPanel);
            splitPane.setBottomComponent(chatPanel);
            
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(splitPane, BorderLayout.CENTER);
        } else {
            // For clients: Only show chat messages
            JPanel chatPanel = createModernChatPanel();
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(chatPanel, BorderLayout.CENTER);
        }
        
        return panel;
    }
    
    /**
     * Creates a modern chat panel with bubble-style messages
     */
    private JPanel createModernChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // Chat messages container (vertical BoxLayout for bubbles)
        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBackground(new Color(240, 242, 245));
        chatMessagesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        chatScrollPane = new JScrollPane(chatMessagesPanel);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Message input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        messageField.addActionListener(e -> sendGroupMessage());

        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(66, 133, 244));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        sendButton.addActionListener(e -> sendGroupMessage());

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        panel.add(chatScrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    /**
     * Adds a chat message bubble to the chat panel
     * @param sender The sender's username
     * @param message The message text
     * @param isOwn Whether this is the current user's message
     */
    private void addChatBubble(String sender, String message, boolean isOwn) {
        SwingUtilities.invokeLater(() -> {
            JPanel bubbleContainer = new JPanel();
            bubbleContainer.setLayout(new BoxLayout(bubbleContainer, BoxLayout.X_AXIS));
            bubbleContainer.setOpaque(false);
            bubbleContainer.setBorder(new EmptyBorder(5, 10, 5, 10));
            
            // Create message bubble
            JPanel bubble = new JPanel();
            bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
            bubble.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
            
            if (isOwn) {
                // Own message - right side, blue bubble
                bubble.setBackground(new Color(66, 133, 244));
                bubble.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(66, 133, 244), 0, true),
                    new EmptyBorder(10, 15, 10, 15)
                ));
                
                JLabel messageLabel = new JLabel("<html><div style='width:300px;'>" + 
                    escapeHtml(message) + "</div></html>");
                messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                messageLabel.setForeground(Color.WHITE);
                messageLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
                
                JLabel timeLabel = new JLabel(getCurrentTime());
                timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                timeLabel.setForeground(new Color(230, 230, 255));
                timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
                
                bubble.add(messageLabel);
                bubble.add(Box.createVerticalStrut(3));
                bubble.add(timeLabel);
                
                bubbleContainer.add(Box.createHorizontalGlue());
                bubbleContainer.add(bubble);
                
            } else {
                // Others' message - left side, white bubble
                bubble.setBackground(Color.WHITE);
                bubble.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(220, 220, 220), 1, true),
                    new EmptyBorder(10, 15, 10, 15)
                ));
                
                JLabel senderLabel = new JLabel(sender);
                senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                senderLabel.setForeground(new Color(66, 133, 244));
                senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                JLabel messageLabel = new JLabel("<html><div style='width:300px;'>" + 
                    escapeHtml(message) + "</div></html>");
                messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                messageLabel.setForeground(new Color(50, 50, 50));
                messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                JLabel timeLabel = new JLabel(getCurrentTime());
                timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                timeLabel.setForeground(new Color(150, 150, 150));
                timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                bubble.add(senderLabel);
                bubble.add(Box.createVerticalStrut(3));
                bubble.add(messageLabel);
                bubble.add(Box.createVerticalStrut(3));
                bubble.add(timeLabel);
                
                bubbleContainer.add(bubble);
                bubbleContainer.add(Box.createHorizontalGlue());
            }
            
            chatMessagesPanel.add(bubbleContainer);
            chatMessagesPanel.revalidate();
            
            // Auto-scroll to bottom
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }
    
    /**
     * Escapes HTML special characters
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("\n", "<br/>");
    }
    
    /**
     * Gets current time in HH:mm format
     */
    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
    }
    
    /**
     * Adds system message to admin's system messages area
     */
    /**
     * Appends system message to admin's system messages area
     */
    private void appendToSystemMessages(String text) {
        if (systemMessagesArea != null) {
            SwingUtilities.invokeLater(() -> {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                systemMessagesArea.append("[" + timestamp + "] " + text + "\n");
                systemMessagesArea.setCaretPosition(systemMessagesArea.getDocument().getLength());
            });
        }
    }
    
    /**
     * Updates admin's peer list with all server connections (real-time)
     */
    private void updateAdminPeerList() {
        SwingUtilities.invokeLater(() -> {
            // Only for admin when server is running
            boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                             currentUser.getPassword().equals("admin");
            
            if (!isAdmin || server == null || !server.isRunning()) {
                return;
            }
            
            // Clear and rebuild peer list from server connections
            peerListModel.clear();
            
            // Add all server connections with usernames
            for (Map.Entry<PeerConnection, String> entry : connectionUsernames.entrySet()) {
                PeerConnection conn = entry.getKey();
                String username = entry.getValue();
                String address = conn.getPeerAddress();
                
                // Only add if connection is still active
                if (conn.isRunning()) {
                    peerListModel.addElement(username + " (" + address + ")");
                }
            }
        });
    }
    
    private JPanel createP2PChatTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Left sidebar: Peer list with controls
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setPreferredSize(new Dimension(220, 0));
        
        // Title for peer list
        JLabel peerListTitle = new JLabel("💬 Chats");
        peerListTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        peerListTitle.setBorder(new EmptyBorder(5, 5, 10, 5));
        
        // New chat button
        JButton newChatBtn = new JButton("+ New Chat");
        newChatBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        newChatBtn.setBackground(new Color(0, 120, 215));
        newChatBtn.setForeground(Color.WHITE);
        newChatBtn.setFocusPainted(false);
        newChatBtn.setBorderPainted(false);
        newChatBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newChatBtn.addActionListener(e -> openNewChatDialog());
        
        // Peer list
        p2pPeerList = new JList<>(p2pPeerListModel);
        p2pPeerList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p2pPeerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p2pPeerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedPeer = p2pPeerList.getSelectedValue();
                if (selectedPeer != null) {
                    switchToP2PChat(selectedPeer);
                }
            }
        });
        JScrollPane peerScrollPane = new JScrollPane(p2pPeerList);
        peerScrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        
        // Server chat option
        JButton serverChatBtn = new JButton("💻 Chat with Server");
        serverChatBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        serverChatBtn.setBackground(new Color(240, 240, 240));
        serverChatBtn.setFocusPainted(false);
        serverChatBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        serverChatBtn.addActionListener(e -> switchToP2PChat("Server"));
        
        // Broadcast button
        JButton broadcastBtn = new JButton("📢 Broadcast to Selected");
        broadcastBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        broadcastBtn.setBackground(new Color(255, 165, 0));
        broadcastBtn.setForeground(Color.WHITE);
        broadcastBtn.setFocusPainted(false);
        broadcastBtn.setBorderPainted(false);
        broadcastBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        broadcastBtn.addActionListener(e -> openBroadcastDialog());
        
        JPanel leftButtonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        leftButtonPanel.setBackground(Color.WHITE);
        leftButtonPanel.add(newChatBtn);
        leftButtonPanel.add(serverChatBtn);
        leftButtonPanel.add(broadcastBtn);
        
        leftPanel.add(peerListTitle, BorderLayout.NORTH);
        leftPanel.add(peerScrollPane, BorderLayout.CENTER);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        // Right panel: Chat tabs
        p2pChatTabs = new JTabbedPane();
        p2pChatTabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        p2pChatTabs.setTabPlacement(JTabbedPane.TOP);
        
        // Add a welcome panel
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(Color.WHITE);
        JLabel welcomeLabel = new JLabel("<html><div style='text-align: center;'>" +
                "<h2>Welcome to P2P Chat!</h2>" +
                "<p>Click 'New Chat' to start a conversation with another client,<br>" +
                "or click 'Chat with Server' to message the server directly.</p>" +
                "<p style='color: gray; font-size: 11px;'>You can also broadcast messages to multiple selected clients.</p>" +
                "</div></html>", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
        p2pChatTabs.addTab("Welcome", welcomePanel);
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, p2pChatTabs);
        splitPane.setDividerLocation(220);
        splitPane.setDividerSize(1);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void switchToP2PChat(String peerName) {
        // Check if chat already exists
        if (p2pChatPanels.containsKey(peerName)) {
            // Switch to existing tab
            int tabIndex = p2pChatTabs.indexOfTab(peerName);
            if (tabIndex != -1) {
                p2pChatTabs.setSelectedIndex(tabIndex);
            }
        } else {
            // Create new chat panel
            createP2PChatPanel(peerName);
        }
    }
    
    private void createP2PChatPanel(String peerName) {
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(Color.WHITE);
        
        // Messages panel (bubble style)
        JPanel messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(new Color(240, 242, 245));
        messagesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane messagesScrollPane = new JScrollPane(messagesPanel);
        messagesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        messagesScrollPane.setBorder(null);
        messagesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JTextField inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(8, 10, 8, 10)));
        
        JButton sendBtn = new JButton("Send");
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendBtn.setBackground(new Color(0, 120, 215));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.setPreferredSize(new Dimension(80, 40));
        
        ActionListener sendAction = e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                sendP2PMessage(peerName, message, messagesPanel);
                inputField.setText("");
            }
        };
        
        sendBtn.addActionListener(sendAction);
        inputField.addActionListener(sendAction);
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        
        chatPanel.add(messagesScrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // Store references
        p2pChatPanels.put(peerName, messagesPanel);
        p2pInputFields.put(peerName, inputField);
        
        // Remove welcome tab if this is the first chat
        int welcomeIndex = p2pChatTabs.indexOfTab("Welcome");
        if (welcomeIndex != -1 && p2pChatPanels.size() == 1) {
            p2pChatTabs.removeTabAt(welcomeIndex);
        }
        
        // Add new tab
        p2pChatTabs.addTab(peerName, chatPanel);
        p2pChatTabs.setSelectedIndex(p2pChatTabs.getTabCount() - 1);
    }
    
    private void sendP2PMessage(String peerName, String messageText, JPanel messagesPanel) {
        try {
            // Add bubble to UI (own message - right aligned, blue)
            addP2PBubble(messagesPanel, currentUser.getUsername(), messageText, true);
            
            Message message = new Message(
                currentUser.getUsername(),
                peerName,
                messageText,
                Message.MessageType.PEER_TO_PEER
            );
            
            // If we're running as server (admin), send directly to the peer
            if (server != null && server.isRunning()) {
                if (peerName.equals("Server")) {
                    // Can't message yourself as server
                    JOptionPane.showMessageDialog(this, 
                        "Cannot send P2P message to yourself!", 
                        "Invalid Recipient", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Find the target connection by username
                PeerConnection targetConn = null;
                Map<PeerConnection, String> connUsernames = server.getConnectionUsernames();
                for (Map.Entry<PeerConnection, String> entry : connUsernames.entrySet()) {
                    if (entry.getValue().equals(peerName)) {
                        targetConn = entry.getKey();
                        break;
                    }
                }
                
                if (targetConn != null && targetConn.isRunning()) {
                    targetConn.sendMessage(message);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Peer '" + peerName + "' is not connected!", 
                        "Connection Error", 
                        JOptionPane.WARNING_MESSAGE);
                }
            } 
            // If we're a client, route through the server
            else if (serverClient != null && serverClient.isConnected()) {
                serverClient.sendMessage(message);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Not connected to server!", 
                    "Connection Error", 
                    JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Failed to send message: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addP2PBubble(JPanel messagesPanel, String sender, String message, boolean isOwn) {
        JPanel bubbleContainer = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 5));
        bubbleContainer.setBackground(new Color(240, 242, 245));
        bubbleContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(isOwn ? new Color(0, 120, 215) : Color.WHITE);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isOwn ? new Color(0, 120, 215) : new Color(220, 220, 220), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        
        // Sender label (only for received messages)
        if (!isOwn) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            senderLabel.setForeground(new Color(0, 120, 215));
            senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(senderLabel);
            bubble.add(Box.createVerticalStrut(3));
        }
        
        // Message text
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageArea.setForeground(isOwn ? Color.WHITE : Color.BLACK);
        messageArea.setBackground(isOwn ? new Color(0, 120, 215) : Color.WHITE);
        messageArea.setBorder(null);
        messageArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Calculate preferred size
        int maxWidth = 400;
        messageArea.setSize(maxWidth, Integer.MAX_VALUE);
        Dimension d = messageArea.getPreferredSize();
        messageArea.setPreferredSize(new Dimension(Math.min(maxWidth, d.width), d.height));
        
        bubble.add(messageArea);
        
        // Timestamp
        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(isOwn ? new Color(230, 230, 230) : new Color(120, 120, 120));
        timeLabel.setAlignmentX(isOwn ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        bubble.add(Box.createVerticalStrut(3));
        bubble.add(timeLabel);
        
        bubbleContainer.add(bubble);
        messagesPanel.add(bubbleContainer);
        messagesPanel.revalidate();
        
        // Auto-scroll
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, messagesPanel);
            if (scrollPane != null) {
                scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
            }
        });
    }
    
    private void openNewChatDialog() {
        if (peerListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No peers available. Please connect to the server first!", 
                "No Peers", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String[] peers = new String[peerListModel.size()];
        for (int i = 0; i < peerListModel.size(); i++) {
            peers[i] = peerListModel.getElementAt(i);
        }
        
        String selectedPeer = (String) JOptionPane.showInputDialog(
                this,
                "Select a peer to chat with:",
                "New Chat",
                JOptionPane.QUESTION_MESSAGE,
                null,
                peers,
                peers[0]);
        
        if (selectedPeer != null) {
            switchToP2PChat(selectedPeer);
        }
    }
    
    private void openBroadcastDialog() {
        if (peerListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No peers available. Please connect to the server first!", 
                "No Peers", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Create a dialog with checkboxes
        JDialog dialog = new JDialog(this, "Broadcast to Peers", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        
        JLabel titleLabel = new JLabel("Select peers to broadcast to:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setBorder(new EmptyBorder(10, 10, 5, 10));
        
        // Checkboxes for peers
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(Color.WHITE);
        checkboxPanel.setBorder(new EmptyBorder(5, 15, 5, 15));
        
        java.util.List<JCheckBox> checkboxes = new ArrayList<>();
        for (int i = 0; i < peerListModel.size(); i++) {
            String peer = peerListModel.getElementAt(i);
            JCheckBox checkbox = new JCheckBox(peer);
            checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            checkbox.setBackground(Color.WHITE);
            checkboxes.add(checkbox);
            checkboxPanel.add(checkbox);
            checkboxPanel.add(Box.createVerticalStrut(5));
        }
        
        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Message input
        JTextArea messageArea = new JTextArea(3, 30);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 5, 5, 5)));
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBackground(Color.WHITE);
        messagePanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        JLabel messageLabel = new JLabel("Message:");
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        messagePanel.add(messageLabel, BorderLayout.NORTH);
        messagePanel.add(messageScrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton sendBtn = new JButton("Send");
        sendBtn.setBackground(new Color(0, 120, 215));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.addActionListener(e -> {
            String message = messageArea.getText().trim();
            if (message.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a message!", "Empty Message", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            java.util.List<String> selectedPeers = new ArrayList<>();
            for (JCheckBox cb : checkboxes) {
                if (cb.isSelected()) {
                    selectedPeers.add(cb.getText());
                }
            }
            
            if (selectedPeers.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select at least one peer!", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Send to selected peers
            for (String peer : selectedPeers) {
                try {
                    Message msg = new Message(
                        currentUser.getUsername(),
                        peer,
                        message,
                        Message.MessageType.PEER_TO_PEER
                    );
                    
                    // If we're running as server (admin), send directly
                    if (server != null && server.isRunning()) {
                        PeerConnection targetConn = null;
                        Map<PeerConnection, String> connUsernames = server.getConnectionUsernames();
                        for (Map.Entry<PeerConnection, String> entry : connUsernames.entrySet()) {
                            if (entry.getValue().equals(peer)) {
                                targetConn = entry.getKey();
                                break;
                            }
                        }
                        if (targetConn != null && targetConn.isRunning()) {
                            targetConn.sendMessage(msg);
                        }
                    }
                    // If we're a client, route through server
                    else if (serverClient != null && serverClient.isConnected()) {
                        serverClient.sendMessage(msg);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
            JOptionPane.showMessageDialog(dialog, 
                "Broadcast sent to " + selectedPeers.size() + " peer(s)!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(sendBtn);
        buttonPanel.add(cancelBtn);
        
        dialog.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(messagePanel, BorderLayout.SOUTH);
        
        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private JPanel createFileSharingTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Title Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("📚 Shared Resources");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        
        JLabel infoLabel = new JLabel("Upload and download study materials with the class");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        
        JPanel titleBox = new JPanel(new BorderLayout());
        titleBox.setOpaque(false);
        titleBox.add(titleLabel, BorderLayout.NORTH);
        titleBox.add(infoLabel, BorderLayout.SOUTH);
        
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> requestFileList());
        
        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(refreshButton, BorderLayout.EAST);
        
        // Files List Panel
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.setBackground(Color.WHITE);
        filesPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            "Available Files",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 13)));
        
        // Custom cell renderer for file list
        sharedFilesList = new JList<>(sharedFilesListModel);
        sharedFilesList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sharedFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sharedFilesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                         int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FileMetadata) {
                    FileMetadata file = (FileMetadata) value;
                    String icon = getFileIcon(file.getFileType());
                    setText(String.format("<html><b>%s %s</b><br>" +
                                        "<font size='2' color='gray'>%s • Uploaded by %s • %s</font></html>",
                        icon, file.getFileName(), file.getFormattedSize(), 
                        file.getUploader(), file.getFormattedUploadTime()));
                }
                setBorder(new EmptyBorder(5, 10, 5, 10));
                return this;
            }
        });
        
        JScrollPane filesScrollPane = new JScrollPane(sharedFilesList);
        filesScrollPane.setPreferredSize(new Dimension(0, 300));
        
        // File Actions Panel
        JPanel fileActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        fileActionsPanel.setOpaque(false);
        
        JButton downloadButton = new JButton("⬇ Download");
        downloadButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        downloadButton.setBackground(new Color(33, 150, 243));
        downloadButton.setForeground(Color.WHITE);
        downloadButton.setFocusPainted(false);
        downloadButton.addActionListener(e -> downloadSelectedFile());
        
        JButton deleteButton = new JButton("� Delete");
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteButton.setBackground(new Color(244, 67, 54));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.addActionListener(e -> deleteSelectedFile());
        
        fileActionsPanel.add(downloadButton);
        fileActionsPanel.add(deleteButton);
        
        JPanel filesListPanel = new JPanel(new BorderLayout());
        filesListPanel.setOpaque(false);
        filesListPanel.add(filesScrollPane, BorderLayout.CENTER);
        filesListPanel.add(fileActionsPanel, BorderLayout.SOUTH);
        
        filesPanel.add(filesListPanel, BorderLayout.CENTER);
        
        // Upload Panel
        JPanel uploadPanel = new JPanel();
        uploadPanel.setLayout(new BoxLayout(uploadPanel, BoxLayout.Y_AXIS));
        uploadPanel.setBackground(Color.WHITE);
        uploadPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            "Upload New File",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 13)));
        
        JPanel uploadButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        uploadButtonPanel.setOpaque(false);
        
        JButton uploadButton = new JButton("📤 Choose File to Upload");
        uploadButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        uploadButton.setBackground(new Color(76, 175, 80));
        uploadButton.setForeground(Color.WHITE);
        uploadButton.setFocusPainted(false);
        uploadButton.setPreferredSize(new Dimension(220, 35));
        uploadButton.addActionListener(e -> uploadFileToServer());
        
        uploadButtonPanel.add(uploadButton);
        
        // Progress bar
        uploadProgressBar = new JProgressBar(0, 100);
        uploadProgressBar.setStringPainted(true);
        uploadProgressBar.setVisible(false);
        uploadProgressBar.setPreferredSize(new Dimension(400, 25));
        
        uploadStatusLabel = new JLabel(" ");
        uploadStatusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        uploadStatusLabel.setForeground(new Color(100, 100, 100));
        
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        progressPanel.setOpaque(false);
        progressPanel.add(uploadProgressBar);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setOpaque(false);
        statusPanel.add(uploadStatusLabel);
        
        uploadPanel.add(uploadButtonPanel);
        uploadPanel.add(progressPanel);
        uploadPanel.add(statusPanel);
        
        // Main layout
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(filesPanel, BorderLayout.CENTER);
        panel.add(uploadPanel, BorderLayout.SOUTH);
        
        // Request file list when tab is created
        requestFileList();
        
        return panel;
    }
    
    private String getFileIcon(String fileType) {
        switch (fileType.toLowerCase()) {
            case "pdf": return "📄";
            case "doc":
            case "docx": return "📝";
            case "xls":
            case "xlsx": return "📊";
            case "ppt":
            case "pptx": return "📽";
            case "txt": return "📃";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif": return "🖼";
            case "zip":
            case "rar": return "📦";
            case "mp4":
            case "avi": return "🎬";
            case "mp3": return "🎵";
            default: return "📎";
        }
    }
    
    private JPanel createBroadcastTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Check if current user is admin
        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") &&
                currentUser.getPassword().equals("admin");

        // Title
        JLabel titleLabel = new JLabel("Announcements");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Info label - different for admin vs students
        String infoText = isAdmin ? "Post important announcements to all students"
                : "View announcements from your instructor";
        JLabel infoLabel = new JLabel(infoText);
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

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoLabel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Only add broadcast input panel for admin users
        if (isAdmin) {
            JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
            inputPanel.setBackground(Color.WHITE);
            inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

            broadcastField = new JTextField();
            broadcastField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            broadcastField.addActionListener(e -> sendBroadcast());

            broadcastButton = new JButton("Broadcast to All");
            broadcastButton.setBackground(new Color(251, 188, 5));
            broadcastButton.setForeground(Color.WHITE);
            broadcastButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
            broadcastButton.setFocusPainted(false);
            broadcastButton.setPreferredSize(new Dimension(160, 35));
            broadcastButton.addActionListener(e -> sendBroadcast());

            inputPanel.add(broadcastField, BorderLayout.CENTER);
            inputPanel.add(broadcastButton, BorderLayout.EAST);

            panel.add(inputPanel, BorderLayout.SOUTH);
        }

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
            // Popup removed - QuizCreatorPanel already shows confirmation
        });

        return quizCreatorPanel;
    }

    private JPanel createQuizParticipationTab() {
        quizParticipationPanel = new QuizParticipationPanel(result -> {
            // When quiz is completed (only students should reach here)
            Message resultMsg = new Message(currentUser.getUsername(), "admin",
                    "Quiz completed", Message.MessageType.QUIZ_ANSWER);
            resultMsg.setQuizAnswer(result);

            // Send to server/clients
            for (Client client : connectedPeers) {
                client.sendMessage(resultMsg);
            }

            // Note: Admin should never take quizzes, so server.broadcast should not be
            // called here
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
        JLabel titleLabel = new JLabel("Quiz Results & Rankings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Leaderboard area
        leaderboardArea = new JTextArea();
        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        leaderboardArea.setText("No quiz results yet.\n\nWait for a quiz to be completed to see rankings here.");
        JScrollPane scrollPane = new JScrollPane(leaderboardArea);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(new Color(66, 133, 244));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> updateLeaderboard());
        buttonPanel.add(refreshButton);

        // Share Leaderboard button (admin only)
        JButton shareButton = new JButton("Share with Students");
        shareButton.setBackground(new Color(52, 168, 83));
        shareButton.setForeground(Color.WHITE);
        shareButton.setFocusPainted(false);
        shareButton.addActionListener(e -> shareLeaderboard());
        buttonPanel.add(shareButton);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStudentLeaderboardTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Title
        JLabel titleLabel = new JLabel("Quiz Results");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Info label
        JLabel infoLabel = new JLabel("View quiz performance and rankings");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Leaderboard area for students
        studentLeaderboardArea = new JTextArea();
        studentLeaderboardArea.setEditable(false);
        studentLeaderboardArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        studentLeaderboardArea.setText("No leaderboard shared yet.\n\n" +
                "The admin will share quiz results when available.\n" +
                "Check the Broadcast tab for shared leaderboards.");
        JScrollPane scrollPane = new JScrollPane(studentLeaderboardArea);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoLabel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
    
    // =========================== Start Class Tab (Admin) ===========================
    private UDPBroadcaster udpBroadcaster;
    private JLabel broadcastStatusLabel;
    private JButton startBroadcastButton;
    private JButton stopBroadcastButton;
    private JPanel screenPreviewPanel;
    private WhiteboardPanel whiteboardPanel;
    private JTextArea classStudentsArea;
    
    private JPanel createStartClassTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("📡 Start Class - Screen Sharing");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        broadcastStatusLabel = new JLabel("Status: Not Broadcasting");
        broadcastStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        broadcastStatusLabel.setForeground(new Color(100, 100, 100));
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(broadcastStatusLabel, BorderLayout.SOUTH);
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setOpaque(false);
        
        startBroadcastButton = new JButton("▶ Start Broadcasting");
        startBroadcastButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        startBroadcastButton.setBackground(new Color(0, 150, 136));
        startBroadcastButton.setForeground(Color.WHITE);
        startBroadcastButton.setFocusPainted(false);
        startBroadcastButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        startBroadcastButton.addActionListener(e -> startBroadcasting());
        
        stopBroadcastButton = new JButton("⏹ Stop Broadcasting");
        stopBroadcastButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        stopBroadcastButton.setBackground(new Color(244, 67, 54));
        stopBroadcastButton.setForeground(Color.WHITE);
        stopBroadcastButton.setFocusPainted(false);
        stopBroadcastButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        stopBroadcastButton.setEnabled(false);
        stopBroadcastButton.addActionListener(e -> stopBroadcasting());
        
        controlPanel.add(startBroadcastButton);
        controlPanel.add(stopBroadcastButton);
        
        // Content Panel - Split between preview and students
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        contentPanel.setOpaque(false);
        
        // Left: Whiteboard/Preview Panel
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setOpaque(false);
        
        JLabel previewLabel = new JLabel("🖊 Whiteboard / Annotations");
        previewLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        previewLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        whiteboardPanel = new WhiteboardPanel(640, 480);
        whiteboardPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));
        whiteboardPanel.setPreferredSize(new Dimension(400, 300));
        
        JPanel whiteboardControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        whiteboardControls.setOpaque(false);
        
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> whiteboardPanel.clear());
        
        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> {
            Color color = JColorChooser.showDialog(this, "Choose Drawing Color", Color.BLACK);
            if (color != null) whiteboardPanel.setDrawingColor(color);
        });
        
        JLabel brushLabel = new JLabel("Brush:");
        JSpinner brushSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
        brushSpinner.addChangeListener(e -> 
            whiteboardPanel.setBrushSize((Integer) brushSpinner.getValue()));
        
        whiteboardControls.add(clearButton);
        whiteboardControls.add(colorButton);
        whiteboardControls.add(brushLabel);
        whiteboardControls.add(brushSpinner);
        
        leftPanel.add(previewLabel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(whiteboardPanel), BorderLayout.CENTER);
        leftPanel.add(whiteboardControls, BorderLayout.SOUTH);
        
        // Right: Connected Students Panel
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setOpaque(false);
        
        JLabel studentsLabel = new JLabel("👥 Students in Class");
        studentsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        studentsLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        classStudentsArea = new JTextArea();
        classStudentsArea.setEditable(false);
        classStudentsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        classStudentsArea.setText("No students connected yet.\n\nStart broadcasting to allow students to join.");
        classStudentsArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        rightPanel.add(studentsLabel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(classStudentsArea), BorderLayout.CENTER);
        
        contentPanel.add(leftPanel);
        contentPanel.add(rightPanel);
        
        // Assemble
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titlePanel, BorderLayout.NORTH);
        topPanel.add(controlPanel, BorderLayout.CENTER);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // =========================== Join Class Tab (Students) ===========================
    private UDPReceiver udpReceiver;
    private JLabel receiveStatusLabel;
    private JButton joinClassButton;
    private JButton leaveClassButton;
    private JPanel screenDisplayPanel;
    private JLabel screenImageLabel;
    
    private JPanel createJoinClassTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("📺 Join Class - Screen Viewing");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        receiveStatusLabel = new JLabel("Status: Not Joined");
        receiveStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        receiveStatusLabel.setForeground(new Color(100, 100, 100));
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(receiveStatusLabel, BorderLayout.SOUTH);
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setOpaque(false);
        
        joinClassButton = new JButton("▶ Join Class");
        joinClassButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        joinClassButton.setBackground(new Color(33, 150, 243));
        joinClassButton.setForeground(Color.WHITE);
        joinClassButton.setFocusPainted(false);
        joinClassButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        joinClassButton.addActionListener(e -> joinClass());
        
        leaveClassButton = new JButton("⏹ Leave Class");
        leaveClassButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        leaveClassButton.setBackground(new Color(244, 67, 54));
        leaveClassButton.setForeground(Color.WHITE);
        leaveClassButton.setFocusPainted(false);
        leaveClassButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        leaveClassButton.setEnabled(false);
        leaveClassButton.addActionListener(e -> leaveClass());
        
        controlPanel.add(joinClassButton);
        controlPanel.add(leaveClassButton);
        
        // Screen Display Panel
        screenDisplayPanel = new JPanel(new BorderLayout());
        screenDisplayPanel.setBackground(Color.BLACK);
        screenDisplayPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));
        
        screenImageLabel = new JLabel("Waiting for class to start...", SwingConstants.CENTER);
        screenImageLabel.setForeground(Color.WHITE);
        screenImageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        screenDisplayPanel.add(screenImageLabel, BorderLayout.CENTER);
        
        // Assemble
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titlePanel, BorderLayout.NORTH);
        topPanel.add(controlPanel, BorderLayout.CENTER);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(screenDisplayPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // =========================== Screen Sharing Methods ===========================
    
    private void startBroadcasting() {
        if (server == null) {
            JOptionPane.showMessageDialog(this,
                "Server must be running to start broadcasting!",
                "Server Not Running", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int udpPort = Integer.parseInt(portField.getText()) + 1000; // Offset for UDP
            udpBroadcaster = new UDPBroadcaster(udpPort, currentUser.getUsername());
            udpBroadcaster.start();
            
            startBroadcastButton.setEnabled(false);
            stopBroadcastButton.setEnabled(true);
            broadcastStatusLabel.setText("Status: Broadcasting on UDP port " + udpPort);
            broadcastStatusLabel.setForeground(new Color(0, 150, 136));
            
            updateClassStudentsList();
            
            JOptionPane.showMessageDialog(this,
                "Broadcasting started!\nStudents can now join your class.",
                "Broadcasting Started", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Failed to start broadcasting: " + ex.getMessage(),
                "Broadcast Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void stopBroadcasting() {
        if (udpBroadcaster != null) {
            udpBroadcaster.stop();
            udpBroadcaster = null;
        }
        
        startBroadcastButton.setEnabled(true);
        stopBroadcastButton.setEnabled(false);
        broadcastStatusLabel.setText("Status: Not Broadcasting");
        broadcastStatusLabel.setForeground(new Color(100, 100, 100));
        
        classStudentsArea.setText("Broadcasting stopped.\n\nStart broadcasting to allow students to join.");
    }
    
    private void updateClassStudentsList() {
        if (server == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Connected Students:\n\n");
        
        Map<PeerConnection, String> connMap = server.getConnectionUsernames();
        if (connMap.isEmpty()) {
            sb.append("No students connected yet.");
        } else {
            int count = 1;
            for (String username : connMap.values()) {
                sb.append(count++).append(". ").append(username).append("\n");
            }
        }
        
        classStudentsArea.setText(sb.toString());
    }
    
    private void joinClass() {
        if (serverClient == null || !serverClient.isConnected()) {
            JOptionPane.showMessageDialog(this,
                "You must be connected to the server first!",
                "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Start UDP receiver on an available local port (port 0 = auto-assign)
            UDPReceiver.ScreenFrameListener listener = new UDPReceiver.ScreenFrameListener() {
                @Override
                public void onFrameReceived(java.awt.image.BufferedImage image, ScreenFrame frame) {
                    SwingUtilities.invokeLater(() -> {
                        ImageIcon icon = new ImageIcon(image);
                        screenImageLabel.setIcon(icon);
                        screenImageLabel.setText("");
                    });
                }
                
                @Override
                public void onError(String error) {
                    SwingUtilities.invokeLater(() -> {
                        receiveStatusLabel.setText("Status: Error - " + error);
                        receiveStatusLabel.setForeground(Color.RED);
                    });
                }
            };
            
            udpReceiver = new UDPReceiver(0, listener); // Port 0 = auto-assign available port
            udpReceiver.start();
            
            // Get the actual port that was assigned
            int localUdpPort = udpReceiver.getLocalPort();
            
            // Get client's local IP address
            String clientIP = null;
            try {
                clientIP = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                System.err.println("[Join Class] Could not get local IP: " + e.getMessage());
                clientIP = "unknown";
            }
            
            // Send CLASS_JOIN message with our local UDP port and IP
            Message joinMsg = new Message(currentUser.getUsername(), "admin", 
                "JOIN_CLASS", Message.MessageType.CLASS_JOIN);
            joinMsg.setUdpPort(localUdpPort); // Tell server our UDP port
            joinMsg.setClientIP(clientIP);     // Tell server our IP address
            serverClient.sendMessage(joinMsg);
            
            joinClassButton.setEnabled(false);
            leaveClassButton.setEnabled(true);
            receiveStatusLabel.setText("Status: Joined Class (UDP port " + localUdpPort + ")");
            receiveStatusLabel.setForeground(new Color(33, 150, 243));
            
            System.out.println("[Join Class] Listening on UDP port " + localUdpPort + " at IP " + clientIP + ", sent CLASS_JOIN message");
            
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Failed to join class: " + ex.getMessage(),
                "Join Error", JOptionPane.ERROR_MESSAGE);
            joinClassButton.setEnabled(true);
        }
    }
    
    private void leaveClass() {
        int localUdpPort = 0;
        String clientIP = null;
        
        if (udpReceiver != null) {
            localUdpPort = udpReceiver.getLocalPort();
            udpReceiver.stop();
            udpReceiver = null;
        }
        
        // Get client IP
        try {
            clientIP = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            clientIP = "unknown";
        }
        
        // Send CLASS_LEAVE message to server with our UDP port and IP
        if (serverClient != null && serverClient.isConnected()) {
            Message leaveMsg = new Message(currentUser.getUsername(), "admin", 
                "LEAVE_CLASS", Message.MessageType.CLASS_LEAVE);
            leaveMsg.setUdpPort(localUdpPort); // Tell server which port to remove
            leaveMsg.setClientIP(clientIP);     // Tell server our IP
            serverClient.sendMessage(leaveMsg);
        }
        
        joinClassButton.setEnabled(true);
        leaveClassButton.setEnabled(false);
        receiveStatusLabel.setText("Status: Not Joined");
        receiveStatusLabel.setForeground(new Color(100, 100, 100));
        
        screenImageLabel.setIcon(null);
        screenImageLabel.setText("Left class. Click 'Join Class' to rejoin.");
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
        
        // Clear admin's peer list
        SwingUtilities.invokeLater(() -> {
            peerListModel.clear();
            connectionUsernames.clear();
            peerConnections.clear();
        });
        
        startServerButton.setEnabled(true);
        stopServerButton.setEnabled(false);
        portField.setEnabled(true);
    }

    // Overloaded method that accepts IP and port directly
    private void connectToPeer(String ip, int port) {
        // Check if already connected
        if (!connectedPeers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Already connected to a server. Disconnect first.",
                    "Already Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }

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

                // Send USER_JOIN message to let server know our username
                Message greeting = new Message(currentUser.getUsername(), "all",
                        currentUser.getUsername() + " has joined",
                        Message.MessageType.USER_JOIN);
                client.sendMessage(greeting);

                // Update file selector to show server/admin
                SwingUtilities.invokeLater(() -> {
                    connectToServerButton.setVisible(false);
                    disconnectFromServerButton.setVisible(true);
                    if (serverIpField != null) {
                        serverIpField.setEnabled(false);
                    }
                    if (serverPortField != null) {
                        serverPortField.setEnabled(false);
                    }
                });

                appendToChat("[SYSTEM] Connected to " + ip + ":" + port);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to connect: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnectFromServer() {
        if (connectedPeers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Not connected to any server",
                    "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to disconnect from the server?",
                "Confirm Disconnect", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            // Send leave message
            Message leaveMsg = new Message(currentUser.getUsername(), "all",
                    currentUser.getUsername() + " has left the chat",
                    Message.MessageType.USER_LEAVE);

            for (Client client : connectedPeers) {
                try {
                    client.sendMessage(leaveMsg);
                    client.disconnect();
                } catch (Exception e) {
                    System.err.println("Error disconnecting: " + e.getMessage());
                }
            }

            // Clear connections
            connectedPeers.clear();
            peerListModel.clear();

            // Clear peer selector
            SwingUtilities.invokeLater(() -> {
                peerSelector.removeAllItems();
                peerSelector.addItem("Select a peer...");

                fileTargetSelector.removeAllItems();
                fileTargetSelector.addItem("Select recipient...");

                // Toggle buttons
                connectToServerButton.setVisible(true);
                disconnectFromServerButton.setVisible(false);
                serverIpField.setEnabled(true);
                serverPortField.setEnabled(true);
            });

            appendToChat("[SYSTEM] ❌ Disconnected from server");
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

    private void sendGroupMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        Message message = new Message(currentUser.getUsername(), "all", text, Message.MessageType.TEXT);
        
        // If we're running a server (admin), broadcast to all server clients
        if (server != null && server.isRunning()) {
            server.broadcast(message);
        }
        
        // If we're a client connected to a server, send through serverClient
        if (serverClient != null && serverClient.isConnected()) {
            serverClient.sendMessage(message);
        }
        
        // Display in chat area
        appendToChat("You: " + text);
        messageField.setText("");
    }

    private void sendBroadcast() {
        String text = broadcastField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        Message message = new Message(currentUser.getUsername(), "all", text, Message.MessageType.BROADCAST);
        
        // If we're running a server (admin), broadcast to all server clients
        if (server != null && server.isRunning()) {
            server.broadcast(message);
        }
        
        // If we're a client connected to a server, send through serverClient
        if (serverClient != null && serverClient.isConnected()) {
            serverClient.sendMessage(message);
        }
        
        // Display locally only once (we won't see it come back because of sender filter)
        appendToBroadcast("[BROADCAST] You: " + text);
        broadcastField.setText("");
    }
    
    /**
     * Disconnects client from server
     */
    private void disconnectFromServer() {
        if (connectedPeers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Not connected to any server",
                "Disconnect Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to disconnect from the server?",
            "Confirm Disconnect",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Send disconnect message
        Message leaveMessage = new Message(currentUser.getUsername(), "all",
            currentUser.getUsername() + " has left the chat",
            Message.MessageType.USER_LEAVE);
        
        // Disconnect all clients
        for (Client client : connectedPeers) {
            try {
                client.sendMessage(leaveMessage);
                client.disconnect();
            } catch (Exception e) {
                System.err.println("Error disconnecting client: " + e.getMessage());
            }
        }
        
        // Clear connections
        connectedPeers.clear();
        peerListModel.clear();
        peerConnections.clear();
        peerUsernames.clear();
        connectionUsernames.clear();
        
        // Update UI
        connectToServerButton.setEnabled(true);
        connectToServerButton.setText("Connect to Server");
        disconnectFromServerButton.setEnabled(false);
        statusLabel.setText("Status: Disconnected");
        statusLabel.setForeground(new Color(244, 67, 54));
        
        // Clear chat areas
        appendToChat("\n--- Disconnected from server ---\n");
        
        JOptionPane.showMessageDialog(this,
            "Successfully disconnected from server",
            "Disconnected",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void updateLeaderboard() {
        if (quizResults.isEmpty()) {
            leaderboardArea.setText("No quiz results yet.\n\nWait for a quiz to be completed to see rankings here.");
            return;
        }

        // Filter out admin from results and sort by score
        List<Map.Entry<String, QuizResult>> sortedResults = new ArrayList<>();
        for (Map.Entry<String, QuizResult> entry : quizResults.entrySet()) {
            // Exclude admin from leaderboard
            if (!entry.getKey().equalsIgnoreCase("admin")) {
                sortedResults.add(entry);
            }
        }

        if (sortedResults.isEmpty()) {
            leaderboardArea.setText("No quiz results yet.\n\nWait for students to complete the quiz.");
            return;
        }

        sortedResults.sort((a, b) -> Integer.compare(b.getValue().getEarnedPoints(), a.getValue().getEarnedPoints()));

        StringBuilder sb = new StringBuilder();
        sb.append("=======================================================\n");
        sb.append("                    === QUIZ LEADERBOARD ===\n");
        sb.append("=======================================================\n\n");
        sb.append(String.format("%-5s %-20s %-10s %-10s %-10s\n", "Rank", "Player", "Score", "Percent", "Grade"));
        sb.append("-------------------------------------------------------\n");

        int rank = 1;
        for (Map.Entry<String, QuizResult> entry : sortedResults) {
            String username = entry.getKey();
            QuizResult result = entry.getValue();

            String medal = "";
            if (rank == 1)
                medal = "[1st]";
            else if (rank == 2)
                medal = "[2nd]";
            else if (rank == 3)
                medal = "[3rd]";

            sb.append(String.format("%-5s %-20s %-10d %-10.1f%% %-10s\n",
                    medal + rank, username, result.getEarnedPoints(),
                    result.getPercentage(), result.getGrade()));
            rank++;
        }

        sb.append("───────────────────────────────────────────────────────\n");
        sb.append(String.format("\nTotal Participants: %d\n", sortedResults.size()));

        if (leaderboardArea != null) {
            leaderboardArea.setText(sb.toString());
        }
    }

    private void sendP2PMessage() {
        String recipient = (peerSelector.getSelectedItem() != null) ? peerSelector.getSelectedItem().toString() : null;
        if (recipient == null || recipient.equals("Select a peer...")) {
            JOptionPane.showMessageDialog(this, "Please select a recipient first", "No Recipient",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String text = p2pMessageField.getText().trim();
        if (text.isEmpty())
            return;

        Message message = new Message(currentUser.getUsername(), recipient, text, Message.MessageType.PEER_TO_PEER);

        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin")
                && currentUser.getPassword().equals("admin");

        if (isAdmin) {
            // Admin sends directly to peer connection
            PeerConnection conn = peerConnections.get(recipient);
            if (conn != null) {
                conn.sendMessage(message);
                appendToP2PChat("You → " + recipient + ": " + text);
            } else {
                appendToP2PChat("[ERROR] Peer not found: " + recipient);
            }
        } else {
            // Client sends via connected server
            if (!connectedPeers.isEmpty()) {
                connectedPeers.get(0).sendMessage(message);
                appendToP2PChat("You → " + recipient + ": " + text);
            } else {
                appendToP2PChat("[ERROR] Not connected to server!");
            }
        }

        p2pMessageField.setText("");
    }

    /**
     * Share leaderboard with all students
     */
    private void shareLeaderboard() {
        if (quizResults.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No quiz results to share yet!",
                    "No Results", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get leaderboard text
        String leaderboardText = leaderboardArea.getText();

        // Broadcast leaderboard to all students
        Message leaderboardMsg = new Message(
                "Admin",
                "all",
                leaderboardText,
                Message.MessageType.BROADCAST);

        // Send to all connected peers
        for (Client client : connectedPeers) {
            client.sendMessage(leaderboardMsg);
        }

        if (server != null && server.isRunning()) {
            server.broadcast(leaderboardMsg);
        }

        appendToBroadcast("[LEADERBOARD] Shared quiz results with all students");
    }

    /**
     * Select and send a file to a specific peer
     */
    private void selectAndSendFile() {
        String targetPeer = (String) fileTargetSelector.getSelectedItem();

        if (targetPeer == null || targetPeer.equals("Select recipient...")) {
            JOptionPane.showMessageDialog(this,
                    "Please select a recipient first",
                    "No Recipient", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Open file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Send to " + targetPeer);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();

            // Show confirmation dialog
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Send file to " + targetPeer + "?\n\n" +
                            "File: " + selectedFile.getName() + "\n" +
                            "Size: " + formatFileSize(selectedFile.length()) + "\n" +
                            "Recipient: " + targetPeer,
                    "Confirm File Send",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                sendFileToUser(targetPeer, selectedFile);
            }
        }
    }

    /**
     * Send file to a specific user
     */
    private void sendFileToUser(String targetUser, java.io.File file) {
        try {
            // Read file data
            byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());

            // Create file transfer object
            FileTransfer fileTransfer = new FileTransfer(
                    file.getName(),
                    file.length(),
                    fileData,
                    currentUser.getUsername(),
                    targetUser);

            // Create file message
            Message fileMessage = new Message(
                    currentUser.getUsername(),
                    targetUser,
                    "Sending file: " + file.getName(),
                    Message.MessageType.FILE);
            fileMessage.setFileTransfer(fileTransfer);

            boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") &&
                    currentUser.getPassword().equals("admin");

            if (isAdmin) {
                // Admin sends to specific client
                PeerConnection targetConnection = peerConnections.get(targetUser);
                if (targetConnection != null) {
                    targetConnection.sendMessage(fileMessage);
                    appendToFileHistory("[" + getCurrentTime() + "] Sent '" + file.getName() +
                            "' to " + targetUser + " (" + formatFileSize(file.length()) + ")");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Target user not found!",
                            "Send Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Client sends through server (server will route it)
                if (!connectedPeers.isEmpty()) {
                    connectedPeers.get(0).sendMessage(fileMessage);
                    appendToFileHistory("[" + getCurrentTime() + "] Sent '" + file.getName() +
                            "' to " + targetUser + " (" + formatFileSize(file.length()) + ")");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Not connected to server!",
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to send file: " + e.getMessage(),
                    "File Send Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Save received file to disk
     */
    private void saveReceivedFile(FileTransfer fileTransfer) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Received File");
        fileChooser.setSelectedFile(new java.io.File(fileTransfer.getFileName()));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File saveFile = fileChooser.getSelectedFile();
            try {
                java.nio.file.Files.write(saveFile.toPath(), fileTransfer.getFileData());
                JOptionPane.showMessageDialog(this,
                        "File saved successfully!\n" + saveFile.getAbsolutePath(),
                        "File Saved", JOptionPane.INFORMATION_MESSAGE);
                appendToFileHistory("[" + getCurrentTime() + "] Saved '" +
                        fileTransfer.getFileName() + "' as: " + saveFile.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save file: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Request list of shared files from server
     */
    private void requestFileList() {
        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                         currentUser.getPassword().equals("admin");
        
        if (isAdmin) {
            // Admin: Get file list directly from server
            if (server != null && server.isRunning()) {
                List<FileMetadata> fileList = server.getSharedFiles();
                SwingUtilities.invokeLater(() -> {
                    sharedFilesListModel.clear();
                    availableFiles.clear();
                    for (FileMetadata file : fileList) {
                        sharedFilesListModel.addElement(file);
                        availableFiles.add(file);
                    }
                    uploadStatusLabel.setText("✓ Found " + fileList.size() + " shared file(s)");
                    uploadStatusLabel.setForeground(new Color(100, 100, 100));
                });
            } else {
                uploadStatusLabel.setText("Server not running");
                uploadStatusLabel.setForeground(Color.RED);
            }
        } else {
            // Client: Request from server
            if (serverClient == null || !serverClient.isConnected()) {
                uploadStatusLabel.setText("Not connected to server");
                uploadStatusLabel.setForeground(Color.RED);
                return;
            }
            
            Message request = new Message(currentUser.getUsername(), "server",
                "REQUEST_FILE_LIST", Message.MessageType.FILE_LIST_REQUEST);
            serverClient.sendMessage(request);
            uploadStatusLabel.setText("Refreshing file list...");
            uploadStatusLabel.setForeground(new Color(100, 100, 100));
        }
    }
    
    /**
     * Upload file to server
     */
    private void uploadFileToServer() {
        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                         currentUser.getPassword().equals("admin");
        
        // Check connection
        if (!isAdmin && (serverClient == null || !serverClient.isConnected())) {
            JOptionPane.showMessageDialog(this,
                "You must be connected to the server first!",
                "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (isAdmin && (server == null || !server.isRunning())) {
            JOptionPane.showMessageDialog(this,
                "Server is not running!",
                "Server Not Running", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Upload");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            
            // Check file size (limit to 50MB)
            if (selectedFile.length() > 50 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this,
                    "File is too large! Maximum size is 50MB.",
                    "File Too Large", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Confirm upload
            int confirm = JOptionPane.showConfirmDialog(this,
                "Upload this file to shared resources?\n\n" +
                "File: " + selectedFile.getName() + "\n" +
                "Size: " + formatFileSize(selectedFile.length()),
                "Confirm Upload",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                performFileUpload(selectedFile);
            }
        }
    }
    
    /**
     * Perform the actual file upload with progress
     */
    private void performFileUpload(java.io.File file) {
        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                         currentUser.getPassword().equals("admin");
        
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    uploadProgressBar.setVisible(true);
                    uploadStatusLabel.setText("Reading file...");
                    publish(10);
                    
                    // Read file data
                    byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
                    publish(50);
                    
                    uploadStatusLabel.setText("Uploading to server...");
                    
                    // Create file metadata
                    String fileId = java.util.UUID.randomUUID().toString();
                    FileMetadata metadata = new FileMetadata(fileId, file.getName(),
                        file.length(), currentUser.getUsername());
                    
                    // Create file transfer
                    FileTransfer fileTransfer = new FileTransfer(
                        file.getName(),
                        file.length(),
                        fileData,
                        currentUser.getUsername(),
                        "server"
                    );
                    
                    // Send to server
                    Message uploadMsg = new Message(currentUser.getUsername(), "server",
                        "UPLOAD_FILE", Message.MessageType.FILE_UPLOAD);
                    uploadMsg.setFileMetadata(metadata);
                    uploadMsg.setFileTransfer(fileTransfer);
                    
                    publish(75);
                    
                    if (isAdmin) {
                        // Admin: Handle upload directly via server
                        server.handleFileUploadDirect(uploadMsg);
                    } else {
                        // Client: Send to server
                        serverClient.sendMessage(uploadMsg);
                    }
                    
                    publish(100);
                    
                    Thread.sleep(500); // Brief pause to show 100%
                    
                } catch (Exception e) {
                    throw e;
                }
                return null;
            }
            
            @Override
            protected void process(java.util.List<Integer> chunks) {
                for (int progress : chunks) {
                    uploadProgressBar.setValue(progress);
                }
            }
            
            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    uploadStatusLabel.setText("✓ Upload successful!");
                    uploadStatusLabel.setForeground(new Color(76, 175, 80));
                    uploadProgressBar.setValue(0);
                    uploadProgressBar.setVisible(false);
                    
                    // Refresh file list
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Thread.sleep(1000);
                            requestFileList();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                    
                } catch (Exception e) {
                    uploadStatusLabel.setText("✗ Upload failed: " + e.getMessage());
                    uploadStatusLabel.setForeground(Color.RED);
                    uploadProgressBar.setValue(0);
                    uploadProgressBar.setVisible(false);
                    JOptionPane.showMessageDialog(MainDashboard.this,
                        "Failed to upload file: " + e.getMessage(),
                        "Upload Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Download selected file from server
     */
    private void downloadSelectedFile() {
        FileMetadata selectedFile = sharedFilesList.getSelectedValue();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a file to download",
                "No File Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                         currentUser.getPassword().equals("admin");
        
        if (!isAdmin && (serverClient == null || !serverClient.isConnected())) {
            JOptionPane.showMessageDialog(this,
                "You must be connected to the server first!",
                "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (isAdmin && (server == null || !server.isRunning())) {
            JOptionPane.showMessageDialog(this,
                "Server is not running!",
                "Server Not Running", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Ask where to save
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Downloaded File");
        fileChooser.setSelectedFile(new java.io.File(selectedFile.getFileName()));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File saveLocation = fileChooser.getSelectedFile();
            
            if (isAdmin) {
                // Admin: Download directly from server
                byte[] fileData = server.getFileData(selectedFile.getFileId(), selectedFile.getFilePath());
                if (fileData != null) {
                    try {
                        java.nio.file.Files.write(saveLocation.toPath(), fileData);
                        uploadStatusLabel.setText("✓ Downloaded: " + selectedFile.getFileName());
                        uploadStatusLabel.setForeground(new Color(76, 175, 80));
                        JOptionPane.showMessageDialog(this,
                            "File downloaded successfully!\n" + saveLocation.getAbsolutePath(),
                            "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        uploadStatusLabel.setText("✗ Download failed");
                        uploadStatusLabel.setForeground(Color.RED);
                        JOptionPane.showMessageDialog(this,
                            "Failed to save file: " + e.getMessage(),
                            "Download Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                        "File not found on server!",
                        "File Not Found", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Client: Send download request and store save location
                Message downloadRequest = new Message(currentUser.getUsername(), "server",
                    selectedFile.getFileId(), Message.MessageType.FILE_DOWNLOAD_REQUEST);
                downloadRequest.setFileMetadata(selectedFile);
                
                // Store save location in map for when file arrives
                pendingDownloads.put(selectedFile.getFileId(), saveLocation.getAbsolutePath());
                
                serverClient.sendMessage(downloadRequest);
                uploadStatusLabel.setText("Downloading " + selectedFile.getFileName() + "...");
                uploadStatusLabel.setForeground(new Color(33, 150, 243));
            }
        }
    }
    
    /**
     * Delete selected file (admin only or own files)
     */
    private void deleteSelectedFile() {
        FileMetadata selectedFile = sharedFilesList.getSelectedValue();
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a file to delete",
                "No File Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        boolean isAdminUser = currentUser.getUsername().equalsIgnoreCase("admin");
        boolean isOwner = selectedFile.getUploader().equals(currentUser.getUsername());
        
        if (!isAdminUser && !isOwner) {
            JOptionPane.showMessageDialog(this,
                "You can only delete your own files!",
                "Permission Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this file?\n\n" + selectedFile.toString(),
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (isAdminUser && server != null && server.isRunning()) {
                // Admin: Delete directly via server
                Message deleteRequest = new Message(currentUser.getUsername(), "server",
                    selectedFile.getFileId(), Message.MessageType.FILE_DELETE_REQUEST);
                deleteRequest.setFileMetadata(selectedFile);
                server.handleFileDeleteDirect(deleteRequest, currentUser.getUsername());
                
                uploadStatusLabel.setText("✓ Deleted: " + selectedFile.getFileName());
                uploadStatusLabel.setForeground(new Color(244, 67, 54));
                
                // Refresh file list
                requestFileList();
            } else if (serverClient != null && serverClient.isConnected()) {
                // Client: Send delete request to server
                Message deleteRequest = new Message(currentUser.getUsername(), "server",
                    selectedFile.getFileId(), Message.MessageType.FILE_DELETE_REQUEST);
                deleteRequest.setFileMetadata(selectedFile);
                
                serverClient.sendMessage(deleteRequest);
                uploadStatusLabel.setText("Deleting " + selectedFile.getFileName() + "...");
                uploadStatusLabel.setForeground(new Color(244, 67, 54));
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
    
    /**
     * Appends a chat message using modern bubble UI
     * @param text Message in format "Sender: Message" or just "Message"
     */
    private void appendToChat(String text) {
        // Parse message format: "Sender: Message" or "You: Message"
        String sender = "";
        String message = text;
        boolean isOwn = false;
        
        if (text.contains(": ")) {
            int colonIndex = text.indexOf(": ");
            sender = text.substring(0, colonIndex);
            message = text.substring(colonIndex + 2);
            
            // Check if it's the current user's message
            isOwn = sender.equalsIgnoreCase("You") || 
                    sender.equalsIgnoreCase(currentUser.getUsername());
            
            // Skip system messages in chat (those without proper sender format)
            if (text.startsWith("---") || text.startsWith("[SYSTEM]")) {
                appendToSystemMessages(text);
                return;
            }
        }
        
        // Add message bubble to chat
        addChatBubble(sender.isEmpty() ? "Unknown" : sender, message, isOwn);
    }

    private void appendToBroadcast(String text) {
        SwingUtilities.invokeLater(() -> {
            broadcastArea.append(text + "\n");
            broadcastArea.setCaretPosition(broadcastArea.getDocument().getLength());
        });
    }

    private void appendToFileHistory(String text) {
        SwingUtilities.invokeLater(() -> {
            fileHistoryArea.append(text + "\n");
            fileHistoryArea.setCaretPosition(fileHistoryArea.getDocument().getLength());
        });
    }

    private void appendToP2PChat(String text) {
        SwingUtilities.invokeLater(() -> {
            if (p2pChatArea != null) {
                p2pChatArea.append(text + "\n");
                p2pChatArea.setCaretPosition(p2pChatArea.getDocument().getLength());
            }
        });
    }

    private void updatePeerSelector(PeerConnection connection) {
        SwingUtilities.invokeLater(() -> {
            // Update file target selector
            fileTargetSelector.removeAllItems();
            fileTargetSelector.addItem("Select recipient...");

            boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") &&
                    currentUser.getPassword().equals("admin");

            // If this is NOT admin (client), add "Server (Admin)" option
            if (!isAdmin && !connectedPeers.isEmpty()) {
                fileTargetSelector.addItem("Server (Admin)");
            }

            // Add all server connections with usernames (admin side)
            for (Map.Entry<PeerConnection, String> entry : connectionUsernames.entrySet()) {
                String username = entry.getValue();
                if (!username.equals(currentUser.getUsername())) {
                    fileTargetSelector.addItem(username);
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
                    fileTargetSelector.addItem(username);
                }
            }

            // Also update peerSelector (P2P dropdown) so admin can send messages
            // Build a unique set of peers
            java.util.Set<String> uniquePeers = new java.util.LinkedHashSet<>();

            // For clients, include Server (Admin) option
            if (!isAdmin && !connectedPeers.isEmpty()) {
                uniquePeers.add("Server (Admin)");
            }

            // Add server-side connected usernames
            for (Map.Entry<PeerConnection, String> entry : connectionUsernames.entrySet()) {
                String uname = entry.getValue();
                if (uname != null && !uname.equals(currentUser.getUsername())) {
                    uniquePeers.add(uname);
                    // ensure mapping available for admin
                    peerConnections.put(uname, entry.getKey());
                }
            }

            // Add any client-known usernames (from client connections)
            for (Client client : connectedPeers) {
                String addr = client.getHost() + ":" + client.getPort();
                String uname = peerUsernames.get(addr);
                if (uname != null && !uname.equals(currentUser.getUsername())) {
                    uniquePeers.add(uname);
                }
            }

            // Populate the peerSelector combo box
            peerSelector.removeAllItems();
            peerSelector.addItem("Select a peer...");
            for (String p : uniquePeers) {
                peerSelector.addItem(p);
            }
        });
    }

    // MessageHandler implementation
    @Override
    public void onMessageReceived(Message message, PeerConnection connection) {
        switch (message.getType()) {
            case TEXT:
                // Don't show our own messages again (we already showed "You: message")
                if (!message.getSender().equals(currentUser.getUsername())) {
                    appendToChat(message.getSender() + ": " + message.getContent());
                }
                break;

            case BROADCAST:
                // Don't show our own broadcasts again
                if (!message.getSender().equals(currentUser.getUsername())) {
                    String content = message.getContent();

                    System.out.println("[DEBUG BROADCAST] Received from: " + message.getSender());
                    System.out.println("[DEBUG BROADCAST] Content contains leaderboard: "
                            + content.contains("=== QUIZ LEADERBOARD ==="));
                    System.out.println(
                            "[DEBUG BROADCAST] studentLeaderboardArea null?: " + (studentLeaderboardArea == null));

                    // Check if this is a shared leaderboard (starts with specific markers)
                    if (content.contains("QUIZ LEADERBOARD") && studentLeaderboardArea != null) {
                        System.out.println("[DEBUG BROADCAST] Updating student leaderboard!");
                        // Update student leaderboard tab and switch to it
                        SwingUtilities.invokeLater(() -> {
                            studentLeaderboardArea.setText(content);
                            // Auto-switch to leaderboard tab (index 4 for students: Chat, File, Broadcast,
                            // Take Quiz, Leaderboard)
                            tabbedPane.setSelectedIndex(4);
                            System.out.println("[DEBUG BROADCAST] Leaderboard tab updated and switched!");
                        });
                    } else {
                        System.out.println("[DEBUG BROADCAST] Showing in broadcast tab only");
                        // Only show in broadcast tab, not in chat

                        LocalTime time = LocalTime.now();
                        String timestamp = time.format(DateTimeFormatter.ofPattern("hh:mm a"));
                        String displayMessage = "[" + timestamp + "] Announcement from " + message.getSender() + ": "
                                + message.getContent();
                        appendToBroadcast(displayMessage);
                        appendToBroadcast("--------------------------------------------------"); 
                    }
                }
                break;
                
            case PEER_TO_PEER:
                // P2P message received
                String p2pSender = message.getSender();
                String p2pContent = message.getContent();
                
                // Check if chat panel exists for this peer, if not create it
                SwingUtilities.invokeLater(() -> {
                    if (!p2pChatPanels.containsKey(p2pSender)) {
                        createP2PChatPanel(p2pSender);
                    }
                    
                    // Add message bubble to the chat panel
                    JPanel messagesPanel = p2pChatPanels.get(p2pSender);
                    if (messagesPanel != null) {
                        addP2PBubble(messagesPanel, p2pSender, p2pContent, false);
                    }
                });
                break;
                
            case FILE:
                // File received
                FileTransfer fileTransfer = message.getFileTransfer();
                if (fileTransfer != null) {
                    String sender = message.getSender();
                    String fileName = fileTransfer.getFileName();
                    long fileSize = fileTransfer.getFileData().length;
                    
                    appendToFileHistory("[" + getCurrentTime() + "] Received '" + fileName + 
                        "' from " + sender + " (" + formatFileSize(fileSize) + ")");
                    
                    // Check if this is a response to a download request
                    String savedPath = pendingDownloads.remove(fileName);
                    if (savedPath != null) {
                        // Auto-save to pre-selected location (from download request)
                        SwingUtilities.invokeLater(() -> {
                            try {
                                java.nio.file.Files.write(
                                    java.nio.file.Paths.get(savedPath), 
                                    fileTransfer.getFileData()
                                );
                                uploadStatusLabel.setText("✓ Downloaded: " + fileName);
                                JOptionPane.showMessageDialog(
                                    this,
                                    "File downloaded successfully!\nSaved to: " + savedPath,
                                    "Download Complete",
                                    JOptionPane.INFORMATION_MESSAGE
                                );
                            } catch (Exception e) {
                                uploadStatusLabel.setText("✗ Download failed: " + fileName);
                                JOptionPane.showMessageDialog(
                                    this,
                                    "Failed to save file: " + e.getMessage(),
                                    "Download Error",
                                    JOptionPane.ERROR_MESSAGE
                                );
                            }
                        });
                    } else {
                        // P2P file transfer - show download dialog
                        SwingUtilities.invokeLater(() -> {
                            int choice = JOptionPane.showConfirmDialog(this,
                                "Received file: " + fileName + "\n" +
                                "From: " + sender + "\n" +
                                "Size: " + formatFileSize(fileSize) + "\n\n" +
                                "Do you want to save this file?",
                                "File Received", 
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                            
                            if (choice == JOptionPane.YES_OPTION) {
                                saveReceivedFile(fileTransfer);
                            }
                        });
                    }
                }
                break;

            case USER_JOIN:
                // Track username for this connection
                String username = message.getSender();
                String peerAddr = connection.getPeerAddress();
                connectionUsernames.put(connection, username);
                peerUsernames.put(peerAddr, username);
                
                // Update admin's peer list in real-time
                updateAdminPeerList();
                
                // Show system message (admin only)
                boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                                  currentUser.getPassword().equals("admin");
                if (isAdmin) {
                    appendToSystemMessages("✅ Client connected: " + username + " (" + peerAddr + ")");
                }

                // Peer list will be updated via server's PEER_LIST broadcast
                // (avoid updating selectors here to prevent redundant UI rebuilds)
                break;

            case USER_LEAVE:
                String leavingUser = connectionUsernames.get(connection);
                if (leavingUser != null) {
                    String leavingAddr = connection.getPeerAddress();
                    
                    // Remove from connectionUsernames
                    connectionUsernames.remove(connection);
                    
                    // Update admin's peer list in real-time
                    updateAdminPeerList();
                    
                    // Show system message (admin only)
                    boolean isAdminLeave = currentUser.getUsername().equalsIgnoreCase("admin") && 
                                          currentUser.getPassword().equals("admin");
                    if (isAdminLeave) {
                        appendToSystemMessages("❌ Client disconnected: " + leavingUser + " (" + leavingAddr + ")");
                    }
                    
                    // Update peer selector
                    updatePeerSelector(connection);
                }
                break;
                
            case CLASS_JOIN:
                // Student wants to join the class - get their UDP port and IP from message
                if (udpBroadcaster != null && udpBroadcaster.isBroadcasting()) {
                    // Get the client's IP from the message (client knows their real IP)
                    String clientIP = message.getClientIP();
                    
                    // Fallback to connection IP if not provided in message
                    if (clientIP == null || clientIP.equals("unknown")) {
                        String joinPeerAddr = connection.getPeerAddress();
                        // Extract IP (might be in format "IP:Port")
                        clientIP = joinPeerAddr.contains(":") ? joinPeerAddr.split(":")[0] : joinPeerAddr;
                    }
                    
                    // Get the client's UDP port from the message
                    int clientUdpPort = message.getUdpPort();
                    
                    if (clientUdpPort > 0) {
                        // Add client to broadcaster with their specific UDP port
                        udpBroadcaster.addClient(clientIP, clientUdpPort);
                        updateClassStudentsList();
                        
                        System.out.println("[Screen Sharing] Student " + message.getSender() + 
                            " joined class from " + clientIP + ":" + clientUdpPort);
                    } else {
                        System.err.println("[Screen Sharing] Invalid UDP port from student: " + message.getSender());
                    }
                }
                break;
                
            case CLASS_LEAVE:
                // Student leaves the class - remove them from UDP broadcaster
                if (udpBroadcaster != null) {
                    // Get the client's IP from the message (client knows their real IP)
                    String clientIP = message.getClientIP();
                    
                    // Fallback to connection IP if not provided in message
                    if (clientIP == null || clientIP.equals("unknown")) {
                        String leavePeerAddr = connection.getPeerAddress();
                        // Extract IP (might be in format "IP:Port")
                        clientIP = leavePeerAddr.contains(":") ? leavePeerAddr.split(":")[0] : leavePeerAddr;
                    }
                    
                    // Get the client's UDP port from the message (if provided)
                    int clientUdpPort = message.getUdpPort();
                    
                    if (clientUdpPort > 0) {
                        udpBroadcaster.removeClient(clientIP, clientUdpPort);
                        updateClassStudentsList();
                        
                        System.out.println("[Screen Sharing] Student " + message.getSender() + 
                            " left class from " + clientIP + ":" + clientUdpPort);
                    }
                }
                break;
                
            case CLASS_INFO:
                // Not used in this implementation - kept for compatibility
                System.out.println("[Screen Sharing] Received CLASS_INFO message (ignored)");
                break;
                
            case PEER_LIST:
                // Received list of connected peers from server - update UI for clients
                String[] peers = message.getContent().split(",");

                SwingUtilities.invokeLater(() -> {
                    // Check if client (not admin)
                    boolean isClientMode = !currentUser.getUsername().equalsIgnoreCase("admin") || 
                                          !currentUser.getPassword().equals("admin");
                    
                    if (isClientMode) {
                        // Build a set of unique peer names (avoid duplicates)
                        java.util.Set<String> uniquePeers = new java.util.LinkedHashSet<>();
                        
                        for (String peer : peers) {
                            String trimmedPeer = peer.trim();
                            if (!trimmedPeer.isEmpty() && 
                                !trimmedPeer.equals(currentUser.getUsername())) {
                                uniquePeers.add(trimmedPeer);
                            }
                        }
                        
                        // Only update if the list actually changed
                        boolean needsUpdate = false;
                        if (peerListModel.size() != uniquePeers.size()) {
                            needsUpdate = true;
                        } else {
                            // Check if content is different
                            int i = 0;
                            for (String peer : uniquePeers) {
                                if (i >= peerListModel.size() || !peerListModel.get(i).equals(peer)) {
                                    needsUpdate = true;
                                    break;
                                }
                                i++;
                            }
                        }
                        
                        // Update peer list UI only if changed
                        if (needsUpdate) {
                            peerListModel.clear();
                            for (String peer : uniquePeers) {
                                peerListModel.addElement(peer);
                            }
                            System.out.println("[DEBUG] Client peer list updated: " + uniquePeers);
                        }
                        
                        // Also update P2P chat peer list
                        boolean p2pNeedsUpdate = false;
                        if (p2pPeerListModel.size() != uniquePeers.size()) {
                            p2pNeedsUpdate = true;
                        } else {
                            int i = 0;
                            for (String peer : uniquePeers) {
                                if (i >= p2pPeerListModel.size() || !p2pPeerListModel.get(i).equals(peer)) {
                                    p2pNeedsUpdate = true;
                                    break;
                                }
                                i++;
                            }
                        }
                        
                        if (p2pNeedsUpdate) {
                            p2pPeerListModel.clear();
                            for (String peer : uniquePeers) {
                                p2pPeerListModel.addElement(peer);
                            }
                        }
                    }
                    
                    // Update file target selector for all users
                    fileTargetSelector.removeAllItems();
                    fileTargetSelector.addItem("Select recipient...");
                    
                    // Use set to avoid duplicates in dropdown
                    java.util.Set<String> uniqueTargets = new java.util.LinkedHashSet<>();
                    for (String peer : peers) {
                        String trimmedPeer = peer.trim();
                        if (!trimmedPeer.isEmpty() && 
                            !trimmedPeer.equals(currentUser.getUsername())) {
                            uniqueTargets.add(trimmedPeer);
                        }
                    }
                    
                    for (String peer : uniqueTargets) {
                        fileTargetSelector.addItem(peer);
                    }
                });
                break;
                
            case SERVER_SHUTDOWN:
                // Server is shutting down - disconnect gracefully
                SwingUtilities.invokeLater(() -> {
                    // Close all client connections
                    for (Client client : connectedPeers) {
                        try {
                            client.disconnect();
                        } catch (Exception e) {
                            System.err.println("Error disconnecting client: " + e.getMessage());
                        }
                    }
                    
                    // Clear all connection data
                    connectedPeers.clear();
                    peerListModel.clear();
                    peerConnections.clear();
                    peerUsernames.clear();
                    connectionUsernames.clear();
                    
                    // Re-enable connect button
                    if (connectToServerButton != null) {
                        connectToServerButton.setEnabled(true);
                        connectToServerButton.setText("Connect to Server");
                    }
                    
                    if (disconnectFromServerButton != null) {
                        disconnectFromServerButton.setEnabled(false);
                    }
                    
                    // Update status label
                    if (statusLabel != null) {
                        statusLabel.setText("Status: Server shut down");
                        statusLabel.setForeground(new Color(244, 67, 54));
                    }
                    
                    // Show notification to user
                    appendToChat("\n--- Server has shut down. You have been disconnected. ---\n");
                    
                    JOptionPane.showMessageDialog(this,
                        "The server has shut down.\nYou have been disconnected.",
                        "Server Shutdown",
                        JOptionPane.WARNING_MESSAGE);
                });
                break;

            case QUIZ_START:
                // New quiz started
                Quiz quiz = message.getQuizData();

                // Check if user is admin - admins should not take quizzes
                boolean isAdminUser = currentUser.getUsername().equalsIgnoreCase("admin") &&
                        currentUser.getPassword().equals("admin");

                if (quiz != null && quizParticipationPanel != null && !isAdminUser) {
                    activeQuiz = quiz;
                    quizParticipationPanel.startQuiz(quiz);
                    // Don't show quiz messages in group chat

                    // Automatically switch to quiz tab for students
                    // "Take Quiz" tab is at index 4 (after Study Chat, P2P Chat, Resources,
                    // Announcements)
                    tabbedPane.setSelectedIndex(4);
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
                            earnedPoints);

                    quizResults.put(message.getSender(), result);
                    updateLeaderboard();

                    // Automatically broadcast updated leaderboard to all students in real-time
                    if (leaderboardArea != null) {
                        String leaderboardText = leaderboardArea.getText();
                        Message leaderboardMsg = new Message(
                                currentUser.getUsername(),
                                "all",
                                leaderboardText,
                                Message.MessageType.BROADCAST);

                        // Send to all connected peers
                        for (Client client : connectedPeers) {
                            client.sendMessage(leaderboardMsg);
                        }

                        if (server != null && server.isRunning()) {
                            server.broadcast(leaderboardMsg);
                        }
                    }

                    // Don't show quiz completion messages in group chat

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
                    // Store the result in the local quizResults map (but admin results won't be
                    // shown in leaderboard)
                    quizResults.put(currentUser.getUsername(), myResult);

                    // Update the leaderboard to show the result (if admin)
                    if (currentUser.getUsername().equalsIgnoreCase("admin") &&
                            currentUser.getPassword().equals("admin")) {
                        updateLeaderboard();
                    } else {
                        // For non-admin users, automatically switch to leaderboard tab
                        // The leaderboard will be updated in real-time via broadcast
                        // "My Results" tab is at index 5 (after Study Chat, P2P Chat, Resources,
                        // Announcements, Take Quiz)
                        SwingUtilities.invokeLater(() -> {
                            tabbedPane.setSelectedIndex(5);
                        });
                    }

                    // Don't show quiz messages in group chat
                }
                break;
                
            case FILE_LIST_RESPONSE:
                // Received list of shared files from server
                java.util.List<FileMetadata> fileList = message.getFileList();
                if (fileList != null) {
                    SwingUtilities.invokeLater(() -> {
                        sharedFilesListModel.clear();
                        availableFiles.clear();
                        for (FileMetadata file : fileList) {
                            sharedFilesListModel.addElement(file);
                            availableFiles.add(file);
                        }
                        uploadStatusLabel.setText("✓ Found " + fileList.size() + " shared file(s)");
                        uploadStatusLabel.setForeground(new Color(100, 100, 100));
                    });
                }
                break;
                
            case FILE_UPLOAD:
                // Server received file upload (server-side handling)
                // This should be handled in Server.java
                break;
                
            case FILE_DOWNLOAD_REQUEST:
                // Server received download request (server-side handling)
                // This should be handled in Server.java
                break;
                
            case FILE_DELETE_REQUEST:
                // Server received delete request (server-side handling)
                // This should be handled in Server.java
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
    
    @Override
    public void onConnectionLost(PeerConnection connection) {
        SwingUtilities.invokeLater(() -> {
            // Check if this is our server connection (client side)
            if (serverClient != null && serverClient.isConnected()) {
                // Connection was lost while we thought we were connected
                // This happens when server crashes or network fails
                
                // Reset connection state
                serverClient = null;
                connectedPeers.clear();
                
                // Update UI
                connectToServerButton.setEnabled(true);
                connectToServerButton.setText("Connect to Server");
                disconnectFromServerButton.setEnabled(false);
                statusLabel.setText("Status: Connection Lost");
                statusLabel.setForeground(new Color(244, 67, 54));
                
                // Clear peer lists
                peerListModel.clear();
                p2pPeerListModel.clear();
                
                // Notify user
                appendToChat("\n⚠️ Connection to server lost! Please reconnect.\n");
                
                // Show admin system message if admin
                boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && 
                                  currentUser.getPassword().equals("admin");
                if (isAdmin && systemMessagesArea != null) {
                    String username = connectionUsernames.get(connection);
                    if (username != null) {
                        appendToSystemMessages("❌ Client disconnected: " + username + 
                            " (" + connection.getPeerAddress() + ")");
                    } else {
                        appendToSystemMessages("❌ Client disconnected: " + connection.getPeerAddress());
                    }
                }
            }
        });
    }
    
    /**
     * Get public IP address from external service
     */
    private String getPublicIP() {
        try {
            java.net.URL url = new java.net.URL("https://api.ipify.org");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(url.openStream()));
            String ip = reader.readLine();
            reader.close();
            return ip;
        } catch (Exception e) {
            return "Unavailable";
        }
    }
}
