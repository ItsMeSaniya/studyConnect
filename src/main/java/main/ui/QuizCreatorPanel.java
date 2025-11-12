package main.ui;

import main.model.Quiz;
import main.model.QuizQuestion;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz creator panel for server/host
 */
public class QuizCreatorPanel extends JPanel {
    private JTextField titleField;
    private JSpinner durationSpinner;
    private JList<String> questionsList;
    private DefaultListModel<String> questionsModel;
    private List<QuizQuestion> questions;
    private JButton createQuizButton;
    private JButton addQuestionButton;
    
    private QuizCreatedListener listener;
    
    public interface QuizCreatedListener {
        void onQuizCreated(Quiz quiz);
    }
    
    public QuizCreatorPanel() {
        questions = new ArrayList<>();
        initComponents();
    }
    
    public QuizCreatorPanel(QuizCreatedListener listener) {
        this.listener = listener;
        questions = new ArrayList<>();
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout(10, 5));
        titlePanel.add(new JLabel("Quiz Title:"), BorderLayout.WEST);
        titleField = new JTextField(20);
        titlePanel.add(titleField, BorderLayout.CENTER);
        
        // Duration panel
        JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        durationPanel.add(new JLabel("Duration:"));
        durationSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        durationPanel.add(durationSpinner);
        durationPanel.add(new JLabel("minutes"));
        
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.add(titlePanel);
        topPanel.add(durationPanel);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Questions list
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(new JLabel("Questions:"), BorderLayout.NORTH);
        
        questionsModel = new DefaultListModel<>();
        questionsList = new JList<>(questionsModel);
        questionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(questionsList);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        addQuestionButton = new JButton("Add Question");
        addQuestionButton.addActionListener(e -> showAddQuestionDialog());
        buttonsPanel.add(addQuestionButton);
        
        JButton removeQuestionButton = new JButton("Remove");
        removeQuestionButton.addActionListener(e -> removeSelectedQuestion());
        buttonsPanel.add(removeQuestionButton);
        
        createQuizButton = new JButton("Create & Start Quiz");
        createQuizButton.setBackground(new Color(76, 175, 80));
        createQuizButton.setForeground(Color.WHITE);
        createQuizButton.setFont(createQuizButton.getFont().deriveFont(Font.BOLD, 14f));
        createQuizButton.addActionListener(e -> createQuiz());
        buttonsPanel.add(createQuizButton);
        
        add(buttonsPanel, BorderLayout.SOUTH);
    }
    
    private void showAddQuestionDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                     "Add Quiz Question", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel contentPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Question text
        contentPanel.add(new JLabel("Question:"));
        JTextField questionField = new JTextField(30);
        contentPanel.add(questionField);
        
        // Options
        contentPanel.add(new JLabel("Options (4 options):"));
        JTextField[] optionFields = new JTextField[4];
        for (int i = 0; i < 4; i++) {
            JPanel optionPanel = new JPanel(new BorderLayout(5, 0));
            optionPanel.add(new JLabel((char)('A' + i) + "."), BorderLayout.WEST);
            optionFields[i] = new JTextField(25);
            optionPanel.add(optionFields[i], BorderLayout.CENTER);
            contentPanel.add(optionPanel);
        }
        
        // Correct answer
        contentPanel.add(new JLabel("Correct Answer:"));
        String[] choices = {"A", "B", "C", "D"};
        JComboBox<String> correctAnswerCombo = new JComboBox<>(choices);
        contentPanel.add(correctAnswerCombo);
        
        // Points
        contentPanel.add(new JLabel("Points:"));
        JSpinner pointsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 5));
        contentPanel.add(pointsSpinner);
        
        dialog.add(contentPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addButton = new JButton("Add Question");
        addButton.addActionListener(e -> {
            String question = questionField.getText().trim();
            if (question.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a question!");
                return;
            }
            
            List<String> options = new ArrayList<>();
            for (JTextField field : optionFields) {
                String option = field.getText().trim();
                if (option.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Please fill all options!");
                    return;
                }
                options.add(option);
            }
            
            int correctAnswer = correctAnswerCombo.getSelectedIndex();
            int points = (Integer) pointsSpinner.getValue();
            
            QuizQuestion quizQuestion = new QuizQuestion(question, options, correctAnswer, points);
            questions.add(quizQuestion);
            questionsModel.addElement(String.format("Q%d: %s (%d pts)", 
                questions.size(), question, points));
            
            dialog.dispose();
        });
        buttonPanel.add(addButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
        
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void removeSelectedQuestion() {
        int selectedIndex = questionsList.getSelectedIndex();
        if (selectedIndex != -1) {
            questions.remove(selectedIndex);
            questionsModel.remove(selectedIndex);
            
            // Update numbering
            questionsModel.clear();
            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion q = questions.get(i);
                questionsModel.addElement(String.format("Q%d: %s (%d pts)", 
                    i + 1, q.getQuestion(), q.getPoints()));
            }
        }
    }
    
    private void createQuiz() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a quiz title!");
            return;
        }
        
        if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one question!");
            return;
        }
        
        int durationMinutes = (Integer) durationSpinner.getValue();
        int durationSeconds = durationMinutes * 60; // Convert minutes to seconds
        Quiz quiz = new Quiz(title, durationSeconds);
        
        for (QuizQuestion question : questions) {
            quiz.addQuestion(question);
        }
        
        quiz.start();
        
        if (listener != null) {
            listener.onQuizCreated(quiz);
        }
        
        // Reset form for next quiz
        titleField.setText("");
        titleField.setEnabled(true);
        questions.clear();
        questionsModel.clear();
        durationSpinner.setValue(5);
        durationSpinner.setEnabled(true);
        addQuestionButton.setEnabled(true);
        createQuizButton.setEnabled(true);
        
        JOptionPane.showMessageDialog(this, 
            "Quiz created and started!\nSent to all connected peers.", 
            "Quiz Started", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void setQuizCreatedListener(QuizCreatedListener listener) {
        this.listener = listener;
    }
}
