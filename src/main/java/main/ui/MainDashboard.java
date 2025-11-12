package main.ui;

import main.model.*;
import main.network.Client;
import main.network.MessageHandler;
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
    
    // File sharing components
    private JComboBox<String> fileTargetSelector;
    private JTextArea fileHistoryArea;
    
    // Quiz components
    private QuizCreatorPanel quizCreatorPanel;
    private QuizParticipationPanel quizParticipationPanel;
    private JTextArea leaderboardArea;
    private JTextArea studentLeaderboardArea;  // For students to view shared leaderboard
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
            JLabel helpLabel = new JLabel("<html><i> Ask admin for their IP address<br/>(NOT 127.0.0.1 unless on same PC)</i></html>");
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
                    connectToPeer(ip, port);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid port number", 
                        "Connection Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            
            // Disconnect button
            disconnectFromServerButton = new JButton("Disconnect from Server");
            disconnectFromServerButton.setBackground(new Color(234, 67, 53));
            disconnectFromServerButton.setForeground(Color.WHITE);
            disconnectFromServerButton.setFocusPainted(false);
            disconnectFromServerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            disconnectFromServerButton.setMaximumSize(new Dimension(200, 35));
            disconnectFromServerButton.setVisible(false); // Hidden initially
            disconnectFromServerButton.addActionListener(e -> disconnectFromServer());
            
            connectPanel.add(ipPanel);
            connectPanel.add(Box.createVerticalStrut(3));
            connectPanel.add(helpLabel);
            connectPanel.add(Box.createVerticalStrut(5));
            connectPanel.add(portPanel);
            connectPanel.add(Box.createVerticalStrut(10));
            connectPanel.add(connectToServerButton);
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

    // Tab 1.5: P2P Chat
    tabbedPane.addTab("P2P Chat", createP2PChatTab());

        // Tab 2: Resource Sharing
        tabbedPane.addTab("Resources", createFileSharingTab());
        
        // Tab 3: Announcements
        tabbedPane.addTab("Announcements", createBroadcastTab());
        
        // Tab 4: Create Quiz (admin only)
        if (isAdmin) {
            tabbedPane.addTab("Create Quiz", createQuizCreatorTab());
            // Tab 5: Results Dashboard (admin can see all results)
            tabbedPane.addTab("Results", createLeaderboardTab());
        } else {
            // Tab 4: Quiz Participation (non-admin only)
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
        
        // Title
        JLabel titleLabel = new JLabel("Study Group Chat");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Info label
        JLabel infoLabel = new JLabel("Chat with your study group members");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
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
        messageField.addActionListener(e -> sendGroupMessage());
        
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(66, 133, 244));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(80, 35));
        sendButton.addActionListener(e -> sendGroupMessage());
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoLabel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
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

        // Info label
        JLabel infoLabel = new JLabel("Chat directly with a specific peer or admin");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Top selector panel
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        selectorPanel.setOpaque(false);
        selectorPanel.add(new JLabel("Chat with:"));

        peerSelector = new JComboBox<>();
        peerSelector.addItem("Select a peer...");
        peerSelector.setPreferredSize(new Dimension(220, 28));
        peerSelector.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        selectorPanel.add(peerSelector);

        // P2P chat area
        p2pChatArea = new JTextArea();
        p2pChatArea.setEditable(false);
        p2pChatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p2pChatArea.setLineWrap(true);
        p2pChatArea.setWrapStyleWord(true);
        JScrollPane p2pScroll = new JScrollPane(p2pChatArea);
        p2pScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        p2pMessageField = new JTextField();
        p2pMessageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p2pMessageField.addActionListener(e -> sendP2PMessage());

        p2pSendButton = new JButton("Send");
        p2pSendButton.setBackground(new Color(66, 133, 244));
        p2pSendButton.setForeground(Color.WHITE);
        p2pSendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p2pSendButton.setFocusPainted(false);
        p2pSendButton.setPreferredSize(new Dimension(80, 35));
        p2pSendButton.addActionListener(e -> sendP2PMessage());

        inputPanel.add(p2pMessageField, BorderLayout.CENTER);
        inputPanel.add(p2pSendButton, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoLabel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(selectorPanel, BorderLayout.BEFORE_FIRST_LINE);
        panel.add(p2pScroll, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    private JPanel createFileSharingTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Title
        JLabel titleLabel = new JLabel("Study Resources");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Info label
        JLabel infoLabel = new JLabel("Share study materials, notes, and documents");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // File history area
        fileHistoryArea = new JTextArea();
        fileHistoryArea.setEditable(false);
        fileHistoryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileHistoryArea.setLineWrap(true);
        fileHistoryArea.setWrapStyleWord(true);
        fileHistoryArea.setText("Resource sharing history will appear here...\n");
        JScrollPane historyScrollPane = new JScrollPane(fileHistoryArea);
        historyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // File sharing control panel
        JPanel fileControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        fileControlPanel.setBackground(Color.WHITE);
        fileControlPanel.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(10, 0, 0, 0),
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), 
                "Send File",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 12))));
        
        JLabel selectPeerLabel = new JLabel("Send to:");
        selectPeerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        fileTargetSelector = new JComboBox<>();
        fileTargetSelector.addItem("Select recipient...");
        fileTargetSelector.setPreferredSize(new Dimension(250, 30));
        fileTargetSelector.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JButton sendFileButton = new JButton("Choose & Send File");
        sendFileButton.setBackground(new Color(52, 168, 83));
        sendFileButton.setForeground(Color.WHITE);
        sendFileButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendFileButton.setFocusPainted(false);
        sendFileButton.setPreferredSize(new Dimension(180, 35));
        sendFileButton.addActionListener(e -> selectAndSendFile());
        
        fileControlPanel.add(selectPeerLabel);
        fileControlPanel.add(fileTargetSelector);
        fileControlPanel.add(sendFileButton);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoLabel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(historyScrollPane, BorderLayout.CENTER);
        panel.add(fileControlPanel, BorderLayout.SOUTH);
        
        return panel;
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
        String infoText = isAdmin ? 
            "Post important announcements to all students" :
            "View announcements from your instructor";
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
            JOptionPane.showMessageDialog(this, 
                "Quiz '" + quiz.getTitle() + "' has been started!\nAll connected peers will receive it.",
                "Quiz Started", JOptionPane.INFORMATION_MESSAGE);
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
            
            // Note: Admin should never take quizzes, so server.broadcast should not be called here
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
    
    private void sendBroadcast() {
        String text = broadcastField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        
        Message message = new Message(currentUser.getUsername(), "all", text, Message.MessageType.BROADCAST);
        
        // Send to all connected peers (clients)
        for (Client client : connectedPeers) {
            client.sendMessage(message);
        }
        
        // If we're running a server, broadcast to all server clients
        if (server != null && server.isRunning()) {
            server.broadcast(message);
        }
        
        // Display locally only once (we won't see it come back because of sender filter)
        appendToBroadcast("[BROADCAST] You: " + text);
        broadcastField.setText("");
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
            if (rank == 1) medal = "[1st]";
            else if (rank == 2) medal = "[2nd]";
            else if (rank == 3) medal = "[3rd]";
            
            sb.append(String.format("%-5s %-20s %-10d %-10.1f%% %-10s\n",
                medal + rank, username, result.getEarnedPoints(), 
                result.getPercentage(), result.getGrade()));
            rank++;
        }
        
        sb.append("───────────────────────────────────────────────────────\n");
        sb.append(String.format("\nTotal Participants: %d\n", quizResults.size()));
        
        if (leaderboardArea != null) {
            leaderboardArea.setText(sb.toString());
        }
    }

    private void sendP2PMessage() {
        String recipient = (peerSelector.getSelectedItem() != null) ? peerSelector.getSelectedItem().toString() : null;
        if (recipient == null || recipient.equals("Select a peer...")) {
            JOptionPane.showMessageDialog(this, "Please select a recipient first", "No Recipient", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String text = p2pMessageField.getText().trim();
        if (text.isEmpty()) return;

        Message message = new Message(currentUser.getUsername(), recipient, text, Message.MessageType.PEER_TO_PEER);

        boolean isAdmin = currentUser.getUsername().equalsIgnoreCase("admin") && currentUser.getPassword().equals("admin");

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
            Message.MessageType.BROADCAST
        );
        
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
     * Get current time as formatted string
     */
    private String getCurrentTime() {
        java.time.LocalTime now = java.time.LocalTime.now();
        return String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
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
                targetUser
            );
            
            // Create file message
            Message fileMessage = new Message(
                currentUser.getUsername(),
                targetUser,
                "Sending file: " + file.getName(),
                Message.MessageType.FILE
            );
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
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
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
                    System.out.println("[DEBUG BROADCAST] Content contains leaderboard: " + content.contains("=== QUIZ LEADERBOARD ==="));
                    System.out.println("[DEBUG BROADCAST] studentLeaderboardArea null?: " + (studentLeaderboardArea == null));
                    
                    // Check if this is a shared leaderboard (starts with specific markers)
                    if (content.contains("QUIZ LEADERBOARD") && studentLeaderboardArea != null) {
                        System.out.println("[DEBUG BROADCAST] Updating student leaderboard!");
                        // Update student leaderboard tab and switch to it
                        SwingUtilities.invokeLater(() -> {
                            studentLeaderboardArea.setText(content);
                            // Auto-switch to leaderboard tab (index 4 for students: Chat, File, Broadcast, Take Quiz, Leaderboard)
                            tabbedPane.setSelectedIndex(4);
                            System.out.println("[DEBUG BROADCAST] Leaderboard tab updated and switched!");
                        });
                    } else {
                        System.out.println("[DEBUG BROADCAST] Showing in broadcast tab only");
                        // Only show in broadcast tab, not in chat
                        appendToBroadcast("[BROADCAST] " + message.getSender() + ": " + content);
                    }
                }
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
                    
                    // Show download dialog
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
                
                // Peer list will be updated via server's PEER_LIST broadcast
                // (avoid updating selectors here to prevent redundant UI rebuilds)
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

            case PEER_TO_PEER:
                // Display received P2P message
                String p2pRecipient = message.getRecipient();
                boolean p2pForMe = p2pRecipient != null && p2pRecipient.equalsIgnoreCase(currentUser.getUsername());

                String p2pDisplay;
                if (p2pForMe) {
                    p2pDisplay = message.getSender() + " → You: " + message.getContent();
                } else {
                    p2pDisplay = message.getSender() + " → " + p2pRecipient + ": " + message.getContent();
                }
                appendToP2PChat(p2pDisplay);
                break;
                
            case PEER_LIST:
                // Received list of connected peers from server - update file target selector
                String[] peers = message.getContent().split(",");
                
                SwingUtilities.invokeLater(() -> {
                    // Clear and rebuild peer selector (prevents duplicates)
                    peerSelector.removeAllItems();
                    peerSelector.addItem("Select a peer...");
                    
                    // Use a Set to track unique peers
                    java.util.Set<String> uniquePeers = new java.util.HashSet<>();
                    
                    for (String peer : peers) {
                        String trimmedPeer = peer.trim();
                        if (!trimmedPeer.isEmpty() && 
                            !trimmedPeer.equals(currentUser.getUsername()) &&
                            uniquePeers.add(trimmedPeer)) { // Only add if not already in set
                            peerSelector.addItem(trimmedPeer);
                        }
                    }
                    
                    // Also update file target selector
                    fileTargetSelector.removeAllItems();
                    fileTargetSelector.addItem("Select recipient...");
                    for (String peer : uniquePeers) {
                        fileTargetSelector.addItem(peer);
                    }
                    
                    System.out.println("[DEBUG] Updated file recipient list: " + Arrays.toString(peers));
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
                    // "Take Quiz" tab is at index 4 (after Study Chat, P2P Chat, Resources, Announcements)
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
                        earnedPoints
                    );
                    
                    quizResults.put(message.getSender(), result);
                    updateLeaderboard();
                    
                    // Automatically broadcast updated leaderboard to all students in real-time
                    if (leaderboardArea != null) {
                        String leaderboardText = leaderboardArea.getText();
                        Message leaderboardMsg = new Message(
                            currentUser.getUsername(),
                            "all",
                            leaderboardText,
                            Message.MessageType.BROADCAST
                        );
                        
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
                    // Store the result in the local quizResults map (but admin results won't be shown in leaderboard)
                    quizResults.put(currentUser.getUsername(), myResult);
                    
                    // Update the leaderboard to show the result (if admin)
                    if (currentUser.getUsername().equalsIgnoreCase("admin") && 
                        currentUser.getPassword().equals("admin")) {
                        updateLeaderboard();
                    } else {
                        // For non-admin users, automatically switch to leaderboard tab
                        // The leaderboard will be updated in real-time via broadcast
                        // "My Results" tab is at index 5 (after Study Chat, P2P Chat, Resources, Announcements, Take Quiz)
                        SwingUtilities.invokeLater(() -> {
                            tabbedPane.setSelectedIndex(5);
                        });
                    }
                    
                    // Don't show quiz messages in group chat
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
