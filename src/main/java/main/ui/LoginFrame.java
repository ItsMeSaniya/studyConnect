package main.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import main.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Login and Registration Frame with modern UI
 */
public class LoginFrame extends JFrame {
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JTextField registerUsernameField;
    private JPasswordField registerPasswordField;
    private JButton loginButton;
    private JButton registerButton;
    private JButton switchModeButton;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private Map<String, User> userDatabase;
    private boolean isLoginMode = true;
    private static final String USERS_FILE = "users.txt";
    
    public LoginFrame() {
        userDatabase = new HashMap<>();
        loadUsers(); // Load existing users from file
        
        // Add default admin user if not exists
        if (!userDatabase.containsKey("admin")) {
            userDatabase.put("admin", new User("admin", "admin", "admin@studyconnect.com"));
            saveUsers();
        }
        
        initComponents();
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        setTitle("StudyConnect - Login");
        setSize(450, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 245, 245));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Card layout for login/register forms
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        
        cardPanel.add(createLoginPanel(), "login");
        cardPanel.add(createRegisterPanel(), "register");
        
        mainPanel.add(cardPanel, BorderLayout.CENTER);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(66, 133, 244));
        panel.setBorder(new EmptyBorder(30, 20, 30, 20));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("StudyConnect");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Study Together, Anywhere");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(230, 230, 230));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(subtitleLabel);
        
        return panel;
    }
    
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(40, 50, 40, 50));
        panel.setOpaque(false);
        
        // Title
        JLabel titleLabel = new JLabel("Sign In");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(30));
        
        // Username Label
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(userLabel);
        panel.add(Box.createVerticalStrut(5));
        
        // Username Field
        loginUsernameField = new JTextField();
        loginUsernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loginUsernameField.setMaximumSize(new Dimension(300, 35));
        loginUsernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginUsernameField.setHorizontalAlignment(JTextField.CENTER);
        panel.add(loginUsernameField);
        panel.add(Box.createVerticalStrut(20));
        
        // Password Label
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(passLabel);
        panel.add(Box.createVerticalStrut(5));
        
        // Password Field
        loginPasswordField = new JPasswordField();
        loginPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loginPasswordField.setMaximumSize(new Dimension(300, 35));
        loginPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginPasswordField.setHorizontalAlignment(JTextField.CENTER);
        panel.add(loginPasswordField);
        panel.add(Box.createVerticalStrut(30));
        
        // Login button
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginButton.setBackground(new Color(66, 133, 244));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setMaximumSize(new Dimension(300, 45));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> handleLogin());
        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(20));
        
        // Switch to register
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        switchPanel.setOpaque(false);
        switchPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel switchLabel = new JLabel("Don't have an account? ");
        switchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        switchModeButton = new JButton("Register");
        switchModeButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        switchModeButton.setForeground(new Color(66, 133, 244));
        switchModeButton.setBorderPainted(false);
        switchModeButton.setContentAreaFilled(false);
        switchModeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchModeButton.addActionListener(e -> switchToRegister());
        switchPanel.add(switchLabel);
        switchPanel.add(switchModeButton);
        panel.add(switchPanel);
        
        return panel;
    }
    
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(40, 50, 40, 50));
        panel.setOpaque(false);
        
        // Title
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(30));
        
        // Username Label
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(userLabel);
        panel.add(Box.createVerticalStrut(5));
        
        // Username Field
        registerUsernameField = new JTextField();
        registerUsernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        registerUsernameField.setMaximumSize(new Dimension(300, 35));
        registerUsernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(registerUsernameField);
        panel.add(Box.createVerticalStrut(20));
        
        // Password Label
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(passLabel);
        panel.add(Box.createVerticalStrut(5));
        
        // Password Field
        registerPasswordField = new JPasswordField();
        registerPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        registerPasswordField.setMaximumSize(new Dimension(300, 35));
        registerPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(registerPasswordField);
        panel.add(Box.createVerticalStrut(30));
        
        // Register button
        registerButton = new JButton("Create Account");
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        registerButton.setBackground(new Color(34, 139, 34));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setMaximumSize(new Dimension(300, 45));
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.addActionListener(e -> handleRegister());
        panel.add(registerButton);
        panel.add(Box.createVerticalStrut(20));
        
        // Switch to login
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        switchPanel.setOpaque(false);
        switchPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel switchLabel = new JLabel("Already have an account? ");
        switchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        switchModeButton = new JButton("Sign In");
        switchModeButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        switchModeButton.setForeground(new Color(66, 133, 244));
        switchModeButton.setBorderPainted(false);
        switchModeButton.setContentAreaFilled(false);
        switchModeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchModeButton.addActionListener(e -> switchToLogin());
        switchPanel.add(switchLabel);
        switchPanel.add(switchModeButton);
        panel.add(switchPanel);
        
        return panel;
    }
    
    private void switchToRegister() {
        cardLayout.show(cardPanel, "register");
        setTitle("StudyConnect - Register");
    }
    
    private void switchToLogin() {
        cardLayout.show(cardPanel, "login");
        setTitle("StudyConnect - Login");
    }
    
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both username and password", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        User user = userDatabase.get(username);
        if (user != null && user.getPassword().equals(password)) {
            // Login successful
            dispose();
            SwingUtilities.invokeLater(() -> {
                MainDashboard dashboard = new MainDashboard(user);
                dashboard.setVisible(true);
            });
        } else {
            JOptionPane.showMessageDialog(this, 
                "Invalid username or password", 
                "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void handleRegister() {
        String username = registerUsernameField.getText().trim();
        String password = new String(registerPasswordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all fields",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (userDatabase.containsKey(username)) {
            JOptionPane.showMessageDialog(this,
                    "Username already exists",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Create new user (email not required anymore)
        User newUser = new User(username, password, "");
        userDatabase.put(username, newUser);
        saveUsers();
        
        JOptionPane.showMessageDialog(this,
                "Registration successful! Please login.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        
        switchToLogin();
    }
    
    /**
     * Save users to file
     */
    private void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (Map.Entry<String, User> entry : userDatabase.entrySet()) {
                User user = entry.getValue();
                // Format: username:password:email
                writer.write(user.getUsername() + ":" + user.getPassword() + ":" + user.getEmail());
                writer.newLine();
            }
            System.out.println("✅ Users saved successfully to " + USERS_FILE);
        } catch (IOException e) {
            System.err.println("❌ Error saving users: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error saving user data: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * Load users from file
     */
    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            System.out.println("ℹ️ No existing users file found. Starting fresh.");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    String username = parts[0];
                    String password = parts[1];
                    String email = parts.length > 2 ? parts[2] : "";
                    
                    userDatabase.put(username, new User(username, password, email));
                    count++;
                }
            }
            System.out.println("✅ Loaded " + count + " users from " + USERS_FILE);
        } catch (IOException e) {
            System.err.println("❌ Error loading users: " + e.getMessage());
        }
    }
}
