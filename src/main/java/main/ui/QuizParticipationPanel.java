package main.ui;

import main.model.Quiz;
import main.model.QuizAnswer;
import main.model.QuizQuestion;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quiz participation panel for clients
 */
public class QuizParticipationPanel extends JPanel {
    private Quiz quiz;
    private Map<Integer, ButtonGroup> answerGroups;
    private JLabel timerLabel;
    private JButton submitButton;
    private Timer timer;
    private QuizSubmittedListener listener;
    public String username; // Public so MainDashboard can set it
    private JPanel contentPanel;
    
    public interface QuizSubmittedListener {
        void onQuizSubmitted(QuizAnswer answer);
    }
    
    public QuizParticipationPanel(QuizSubmittedListener listener) {
        this.listener = listener;
        this.answerGroups = new HashMap<>();
        initEmptyPanel();
    }
    
    public QuizParticipationPanel(Quiz quiz, String username) {
        this.quiz = quiz;
        this.username = username;
        this.answerGroups = new HashMap<>();
        initComponents(username);
        startTimer();
    }
    
    private void initEmptyPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel waitingLabel = new JLabel("Waiting for quiz to start...");
        waitingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        waitingLabel.setForeground(new Color(100, 100, 100));
        
        contentPanel.add(waitingLabel, gbc);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    public void startQuiz(Quiz quiz) {
        this.quiz = quiz;
        
        // Stop any existing timer
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        
        // Clear and reset everything
        removeAll();
        answerGroups.clear();
        
        // Reinitialize with new quiz
        initComponents(username != null ? username : "Guest");
        startTimer();
        
        // Refresh the display
        revalidate();
        repaint();
    }
    
    private void initComponents(String username) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(quiz.getTitle());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        timerLabel = new JLabel("Time Remaining: " + quiz.getRemainingTime() + "s");
        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 16f));
        timerLabel.setForeground(new Color(76, 175, 80));
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(timerLabel, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Questions panel
        JPanel questionsPanel = new JPanel();
        questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));
        
        List<QuizQuestion> questions = quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion question = questions.get(i);
            JPanel questionPanel = createQuestionPanel(i, question);
            questionsPanel.add(questionPanel);
            questionsPanel.add(Box.createVerticalStrut(15));
        }
        
        JScrollPane scrollPane = new JScrollPane(questionsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
        
        // Submit button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        submitButton = new JButton("✓ Submit Quiz");
        submitButton.setBackground(new Color(33, 150, 243));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(submitButton.getFont().deriveFont(Font.BOLD, 14f));
        submitButton.addActionListener(e -> submitQuiz(username));
        buttonPanel.add(submitButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createQuestionPanel(int index, QuizQuestion question) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(Color.WHITE);
        
        // Question text
        JLabel questionLabel = new JLabel(String.format("Q%d. %s (%d points)", 
            index + 1, question.getQuestion(), question.getPoints()));
        questionLabel.setFont(questionLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(questionLabel);
        panel.add(Box.createVerticalStrut(10));
        
        // Options
        ButtonGroup group = new ButtonGroup();
        answerGroups.put(index, group);
        
        List<String> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            JRadioButton radioButton = new JRadioButton(
                String.format("%c. %s", (char)('A' + i), options.get(i))
            );
            radioButton.setActionCommand(String.valueOf(i));
            radioButton.setBackground(Color.WHITE);
            group.add(radioButton);
            panel.add(radioButton);
            panel.add(Box.createVerticalStrut(5));
        }
        
        return panel;
    }
    
    private void startTimer() {
        timer = new Timer(1000, e -> {
            int remaining = quiz.getRemainingTime();
            timerLabel.setText("Time Remaining: " + remaining + "s");
            
            if (remaining <= 10) {
                timerLabel.setForeground(Color.RED);
            } else if (remaining <= 30) {
                timerLabel.setForeground(new Color(255, 152, 0));
            }
            
            if (quiz.isExpired()) {
                timer.stop();
                JOptionPane.showMessageDialog(this, 
                    "Time's up! Quiz will be auto-submitted.", 
                    "Time Expired", JOptionPane.WARNING_MESSAGE);
                submitButton.doClick();
            }
        });
        timer.start();
    }
    
    private void submitQuiz(String username) {
        timer.stop();
        submitButton.setEnabled(false);
        
        QuizAnswer answer = new QuizAnswer(quiz.getQuizId(), username);
        
        for (Map.Entry<Integer, ButtonGroup> entry : answerGroups.entrySet()) {
            int questionIndex = entry.getKey();
            ButtonGroup group = entry.getValue();
            
            if (group.getSelection() != null) {
                int selectedAnswer = Integer.parseInt(group.getSelection().getActionCommand());
                answer.addAnswer(questionIndex, selectedAnswer);
            }
        }
        
        if (listener != null) {
            listener.onQuizSubmitted(answer);
        }
        
        // Hide quiz and show completion message
        removeAll();
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JPanel completionPanel = new JPanel();
        completionPanel.setLayout(new BoxLayout(completionPanel, BoxLayout.Y_AXIS));
        completionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel successIcon = new JLabel("✅");
        successIcon.setFont(new Font("Segoe UI", Font.PLAIN, 72));
        successIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        completionPanel.add(successIcon);
        
        completionPanel.add(Box.createVerticalStrut(20));
        
        JLabel submittedLabel = new JLabel("Quiz Submitted Successfully!");
        submittedLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        submittedLabel.setForeground(new Color(76, 175, 80));
        submittedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        completionPanel.add(submittedLabel);
        
        completionPanel.add(Box.createVerticalStrut(10));
        
        JLabel messageLabel = new JLabel("Your answers have been recorded.");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        messageLabel.setForeground(new Color(100, 100, 100));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        completionPanel.add(messageLabel);
        
        completionPanel.add(Box.createVerticalStrut(5));
        
        JLabel resultsLabel = new JLabel("Check the Leaderboard tab for results!");
        resultsLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        resultsLabel.setForeground(new Color(100, 100, 100));
        resultsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        completionPanel.add(resultsLabel);
        
        add(completionPanel, gbc);
        revalidate();
        repaint();
    }
    
    public void setQuizSubmittedListener(QuizSubmittedListener listener) {
        this.listener = listener;
    }
    
    public void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }
}
