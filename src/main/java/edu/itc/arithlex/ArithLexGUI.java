package edu.itc.arithlex;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ArithLexGUI extends JFrame {

    // ---------- Colors ----------
    private static final Color BACKGROUND = new Color(246, 247, 251);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT = new Color(35, 39, 60);
    private static final Color MUTED = new Color(135, 140, 158);
    private static final Color BLUE = new Color(55, 112, 245);
    private static final Color BORDER = new Color(230, 232, 240);
    private static final Color RED = new Color(220, 65, 75);

    // ---------- Components ----------
    private JTextArea inputArea;
    private JTextArea lineNumbers;
    private JTable tokenTable;
    private DefaultTableModel tableModel;
    private JTextArea errorArea;

    private JLabel statusLabel;
    private JLabel tokenCountLabel;
    private JLabel errorCountLabel;

    private final LexerService lexerService = new LexerService();

    private String currentFile = null;
    private LexerResult lastResult = null;

    public ArithLexGUI() {

        setTitle("ArithLex");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 820);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);

        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);
        add(createWorkspace(), BorderLayout.CENTER);

        loadSampleInput();
    }

    // =========================================================
    // SIDEBAR
    // =========================================================

    private JPanel createSidebar() {

        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(WHITE);
        sidebar.setBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER)
        );

        sidebar.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(30, 20, 20, 20));

        // Logo
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoRow.setOpaque(false);

        JLabel logo = new JLabel("⌘");
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 27));
        logo.setForeground(WHITE);
        logo.setOpaque(true);
        logo.setBackground(BLUE);
        logo.setPreferredSize(new Dimension(46, 46));

        JLabel brand = new JLabel("ArithLex");
        brand.setFont(new Font("SansSerif", Font.BOLD, 21));
        brand.setForeground(TEXT);

        logoRow.add(logo);
        logoRow.add(brand);

        JLabel description = new JLabel("  Lexical Analyzer");
        description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        description.setForeground(MUTED);

        top.add(logoRow);
        top.add(Box.createVerticalStrut(5));
        top.add(description);
        top.add(Box.createVerticalStrut(40));

        top.add(createNavButton("▣", "Workspace", true));
        top.add(Box.createVerticalStrut(8));
        top.add(createNavButton("▤", "Documentation", false));
        top.add(Box.createVerticalStrut(8));
        top.add(createNavButton("ⓘ", "About ArithLex", false));

        sidebar.add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(15, 22, 25, 15));

        JLabel version = new JLabel("VERSION 1.0.0");
        version.setFont(new Font("SansSerif", Font.BOLD, 10));
        version.setForeground(MUTED);

        JLabel technology = new JLabel("Java  •  JFlex");
        technology.setFont(new Font("SansSerif", Font.PLAIN, 12));
        technology.setForeground(MUTED);

        bottom.add(version);
        bottom.add(Box.createVerticalStrut(5));
        bottom.add(technology);

        sidebar.add(bottom, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(
            String icon,
            String label,
            boolean selected
    ) {

        JButton button = new JButton(icon + "    " + label);

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 14, 12, 10));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (selected) {
            button.setBackground(new Color(235, 241, 255));
            button.setForeground(BLUE);
            button.setFont(new Font("SansSerif", Font.BOLD, 13));
        } else {
            button.setBackground(WHITE);
            button.setForeground(TEXT);
        }

        button.setOpaque(true);
        button.setBorderPainted(false);

        button.addActionListener(e -> {
            if (label.equals("Documentation")) {
                showDocumentation();
            } else if (label.equals("About ArithLex")) {
                showAbout();
            }
        });

        return button;
    }

    // =========================================================
    // MAIN WORKSPACE
    // =========================================================

    private JPanel createWorkspace() {

        JPanel workspace = new JPanel(new BorderLayout(0, 22));
        workspace.setBackground(BACKGROUND);
        workspace.setBorder(new EmptyBorder(28, 30, 25, 30));

        workspace.add(createHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);

        body.add(createEditorSection(), BorderLayout.NORTH);
        body.add(createResultsSection(), BorderLayout.CENTER);

        workspace.add(body, BorderLayout.CENTER);

        return workspace;
    }

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Arithmetic Expression Analyzer");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel(
                "Analyze the lexical structure of your arithmetic expressions."
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);

        heading.add(title);
        heading.add(Box.createVerticalStrut(7));
        heading.add(subtitle);

        statusLabel = new JLabel("●  Ready");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setForeground(new Color(35, 155, 100));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(231, 248, 239));
        statusLabel.setBorder(new EmptyBorder(9, 14, 9, 14));

        header.add(heading, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);

        return header;
    }

    // =========================================================
    // EDITOR
    // =========================================================

    private JPanel createEditorSection() {

        JPanel section = new JPanel(new BorderLayout(0, 12));
        section.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel label = new JLabel("Input Expression");
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        label.setForeground(TEXT);

        JLabel hint = new JLabel("Enter an expression or open a .txt file");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(MUTED);

        titleRow.add(label, BorderLayout.WEST);
        titleRow.add(hint, BorderLayout.EAST);

        RoundedPanel editorCard = new RoundedPanel(16, WHITE);
        editorCard.setLayout(new BorderLayout());
        editorCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        inputArea = new JTextArea();
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        inputArea.setForeground(new Color(40, 48, 75));
        inputArea.setBackground(WHITE);
        inputArea.setCaretColor(BLUE);
        inputArea.setLineWrap(false);
        inputArea.setTabSize(4);
        inputArea.setBorder(new EmptyBorder(8, 12, 8, 12));

        lineNumbers = new JTextArea("1");
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumbers.setForeground(new Color(165, 170, 185));
        lineNumbers.setBackground(new Color(249, 250, 253));
        lineNumbers.setBorder(new EmptyBorder(8, 12, 8, 12));

        JScrollPane editorScroll = new JScrollPane(inputArea);
        editorScroll.setBorder(BorderFactory.createEmptyBorder());
        editorScroll.setRowHeaderView(lineNumbers);
        editorScroll.getViewport().setBackground(WHITE);
        editorScroll.setPreferredSize(new Dimension(700, 175));

        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateLineNumbers();
            }

            public void removeUpdate(DocumentEvent e) {
                updateLineNumbers();
            }

            public void changedUpdate(DocumentEvent e) {
                updateLineNumbers();
            }
        });

        editorCard.add(editorScroll, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        leftActions.setOpaque(false);

        JButton analyzeButton = createButton("▶  Analyze", BLUE, WHITE);
        JButton clearButton = createButton("Clear", new Color(235, 237, 243), TEXT);
        JButton openButton = createButton("Open File", WHITE, TEXT);
        JButton saveButton = createButton("Save Result", WHITE, TEXT);

        analyzeButton.addActionListener(this::analyzeInput);
        clearButton.addActionListener(e -> clearAll());
        openButton.addActionListener(e -> openFile());
        saveButton.addActionListener(e -> saveResult());

        leftActions.add(analyzeButton);
        leftActions.add(clearButton);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        rightActions.add(openButton);
        rightActions.add(saveButton);

        actionRow.add(leftActions, BorderLayout.WEST);
        actionRow.add(rightActions, BorderLayout.EAST);

        JPanel editorContent = new JPanel(new BorderLayout(0, 12));
        editorContent.setOpaque(false);
        editorContent.add(editorCard, BorderLayout.CENTER);
        editorContent.add(actionRow, BorderLayout.SOUTH);

        section.add(titleRow, BorderLayout.NORTH);
        section.add(editorContent, BorderLayout.CENTER);

        return section;
    }

    private void updateLineNumbers() {

        int lines = Math.max(1, inputArea.getLineCount());

        StringBuilder numbers = new StringBuilder();

        for (int i = 1; i <= lines; i++) {
            numbers.append(i);

            if (i < lines) {
                numbers.append("\n");
            }
        }

        lineNumbers.setText(numbers.toString());
    }

    // =========================================================
    // RESULTS
    // =========================================================

    private JPanel createResultsSection() {

        JPanel section = new JPanel(new BorderLayout(0, 12));
        section.setOpaque(false);

        JPanel summary = new JPanel(new BorderLayout());
        summary.setOpaque(false);

        JLabel resultTitle = new JLabel("Analysis Results");
        resultTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        resultTitle.setForeground(TEXT);

        JPanel counts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        counts.setOpaque(false);

        tokenCountLabel = createCountLabel("Tokens: 0", BLUE);
        errorCountLabel = createCountLabel("Errors: 0", RED);

        counts.add(tokenCountLabel);
        counts.add(errorCountLabel);

        summary.add(resultTitle, BorderLayout.WEST);
        summary.add(counts, BorderLayout.EAST);

        // Token table
        String[] columns = {"Token Type", "Lexeme", "Line", "Column"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tokenTable = new JTable(tableModel);
        tokenTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tokenTable.setRowHeight(34);
        tokenTable.setShowVerticalLines(false);
        tokenTable.setGridColor(BORDER);
        tokenTable.setSelectionBackground(new Color(232, 240, 255));
        tokenTable.setSelectionForeground(TEXT);
        tokenTable.setFillsViewportHeight(true);
        tokenTable.setIntercellSpacing(new Dimension(0, 1));

        tokenTable.getTableHeader().setFont(
                new Font("SansSerif", Font.BOLD, 12)
        );
        tokenTable.getTableHeader().setBackground(
                new Color(247, 248, 252)
        );
        tokenTable.getTableHeader().setForeground(TEXT);
        tokenTable.getTableHeader().setPreferredSize(
                new Dimension(0, 38)
        );

        DefaultTableCellRenderer centerRenderer =
                new DefaultTableCellRenderer();

        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        tokenTable.getColumnModel().getColumn(2)
                .setCellRenderer(centerRenderer);

        tokenTable.getColumnModel().getColumn(3)
                .setCellRenderer(centerRenderer);

        tokenTable.getColumnModel().getColumn(0)
                .setPreferredWidth(150);

        tokenTable.getColumnModel().getColumn(1)
                .setPreferredWidth(150);

        JScrollPane tableScroll = new JScrollPane(tokenTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(WHITE);

        RoundedPanel tokenCard = new RoundedPanel(16, WHITE);
        tokenCard.setLayout(new BorderLayout());
        tokenCard.setBorder(new EmptyBorder(10, 10, 10, 10));
        tokenCard.add(tableScroll, BorderLayout.CENTER);

        // Error panel
        JPanel errorHeader = new JPanel(new BorderLayout());
        errorHeader.setOpaque(false);
        errorHeader.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel errorTitle = new JLabel("⚠  Lexical Errors");
        errorTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        errorTitle.setForeground(new Color(190, 55, 65));

        errorHeader.add(errorTitle, BorderLayout.WEST);

        errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setLineWrap(true);
        errorArea.setWrapStyleWord(true);
        errorArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        errorArea.setForeground(new Color(150, 55, 65));
        errorArea.setBackground(WHITE);
        errorArea.setText("No lexical errors found.");

        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setBorder(BorderFactory.createEmptyBorder());
        errorScroll.getViewport().setBackground(WHITE);

        RoundedPanel errorCard = new RoundedPanel(16, WHITE);
        errorCard.setLayout(new BorderLayout());
        errorCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        errorCard.add(errorHeader, BorderLayout.NORTH);
        errorCard.add(errorScroll, BorderLayout.CENTER);

        // Split result panels
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                tokenCard,
                errorCard
        );

        splitPane.setResizeWeight(0.68);
        splitPane.setDividerSize(9);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);

        section.add(summary, BorderLayout.NORTH);
        section.add(splitPane, BorderLayout.CENTER);

        return section;
    }

    // =========================================================
    // ANALYSIS ACTION
    // =========================================================

    private void analyzeInput(ActionEvent event) {

        String input = inputArea.getText();

        if (input.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter an expression first.",
                    "Empty Input",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            LexerResult result = lexerService.analyze(input);

            lastResult = result;

            tableModel.setRowCount(0);

            for (Token token : result.getTokens()) {
                tableModel.addRow(new Object[]{
                        token.getType(),
                        token.getLexeme(),
                        token.getLine(),
                        token.getColumn()
                });
            }

            List<LexicalError> errors = result.getErrors();

            if (errors.isEmpty()) {
                errorArea.setText("✓  No lexical errors found.");
                errorArea.setForeground(new Color(35, 145, 95));
            } else {
                StringBuilder errorText = new StringBuilder();

                for (LexicalError error : errors) {
                    errorText.append("●  ")
                            .append(error)
                            .append("\n\n");
                }

                errorArea.setText(errorText.toString());
                errorArea.setForeground(RED);
            }

            tokenCountLabel.setText(
                    "Tokens: " + result.getTokens().size()
            );

            errorCountLabel.setText(
                    "Errors: " + errors.size()
            );

            if (errors.isEmpty()) {
                setStatus("●  Analysis Complete", new Color(35, 155, 100));
            } else {
                setStatus("●  Completed with Errors", RED);
            }

        } catch (IOException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Analysis failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            setStatus("●  Analysis Failed", RED);
        }
    }

    // =========================================================
    // FILE ACTIONS
    // =========================================================

    private void openFile() {

        JFileChooser chooser = new JFileChooser();

        chooser.setDialogTitle("Open Arithmetic Expression File");
        chooser.setFileFilter(
                new FileNameExtensionFilter("Text Files (*.txt)", "txt")
        );

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            Path path = chooser.getSelectedFile().toPath();

            try {
                String content = Files.readString(
                        path,
                        StandardCharsets.UTF_8
                );

                inputArea.setText(content);
                currentFile = path.toString();

                setStatus("●  File Loaded", new Color(35, 155, 100));

            } catch (IOException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "Could not open file: " + ex.getMessage(),
                        "File Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void saveResult() {

        if (lastResult == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please analyze your input before saving the result.",
                    "No Results",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Analysis Result");

        chooser.setSelectedFile(
                new java.io.File("arithlex-result.txt")
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter("Text Files (*.txt)", "txt")
        );

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

            Path path = chooser.getSelectedFile().toPath();

            if (!path.toString().toLowerCase().endsWith(".txt")) {
                path = Path.of(path.toString() + ".txt");
            }

            try {
                StringBuilder output = new StringBuilder();

                output.append("ARITHLEX - LEXICAL ANALYSIS RESULTS\n");
                output.append("====================================\n\n");

                output.append("TOKENS\n");
                output.append("------------------------------------\n");

                for (Token token : lastResult.getTokens()) {
                    output.append(token).append("\n");
                }

                output.append("\nLEXICAL ERRORS\n");
                output.append("------------------------------------\n");

                if (lastResult.getErrors().isEmpty()) {
                    output.append("No lexical errors found.\n");
                } else {
                    for (LexicalError error : lastResult.getErrors()) {
                        output.append(error).append("\n");
                    }
                }

                output.append("\nTotal tokens: ")
                        .append(lastResult.getTokens().size());

                output.append("\nTotal errors: ")
                        .append(lastResult.getErrors().size());

                Files.writeString(
                        path,
                        output.toString(),
                        StandardCharsets.UTF_8
                );

                JOptionPane.showMessageDialog(
                        this,
                        "Analysis result saved successfully!",
                        "Saved",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (IOException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "Could not save result: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    // =========================================================
    // CLEAR AND UTILITIES
    // =========================================================

    private void clearAll() {

        inputArea.setText("");
        tableModel.setRowCount(0);

        errorArea.setText("No analysis performed yet.");
        errorArea.setForeground(MUTED);

        tokenCountLabel.setText("Tokens: 0");
        errorCountLabel.setText("Errors: 0");

        lastResult = null;
        currentFile = null;

        setStatus("●  Ready", new Color(35, 155, 100));
    }

    private void loadSampleInput() {

        inputArea.setText(
                "total = 12.5 + qty * 2;\n" +
                "result = (10 + 5) / 3;\n" +
                "average = total / count;\n" +
                "price = 100 @ 5;\n" +
                "discount = 20 # 2;"
        );
    }

    private JButton createButton(
            String text,
            Color background,
            Color foreground
    ) {

        JButton button = new JButton(text);

        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorderPainted(false);

        return button;
    }

    private JLabel createCountLabel(String text, Color color) {

        JLabel label = new JLabel(text);

        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(color);
        label.setOpaque(true);
        label.setBackground(WHITE);
        label.setBorder(new EmptyBorder(7, 11, 7, 11));

        return label;
    }

    private void setStatus(String text, Color color) {

        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void showDocumentation() {

        JOptionPane.showMessageDialog(
                this,
                """
                ArithLex Documentation

                1. Enter an arithmetic expression.
                2. Click Analyze.
                3. View recognized tokens in the table.
                4. Check invalid characters in the error panel.
                5. Open a .txt file or save your analysis result.

                ArithLex uses Java and JFlex.
                """,
                "Documentation",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showAbout() {

        JOptionPane.showMessageDialog(
                this,
                """
                ArithLex
                Arithmetic Expression Lexical Analyzer

                Developed using:
                • Java
                • JFlex
                • Java Swing
                • FlatLaf

                Compilation Course Project
                """,
                "About ArithLex",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // =========================================================
    // CUSTOM ROUNDED PANEL
    // =========================================================

    private static class RoundedPanel extends JPanel {

        private final int radius;
        private final Color backgroundColor;

        public RoundedPanel(int radius, Color backgroundColor) {

            this.radius = radius;
            this.backgroundColor = backgroundColor;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g2 = (Graphics2D) graphics.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(backgroundColor);

            g2.fillRoundRect(
                    0, 0,
                    getWidth(),
                    getHeight(),
                    radius,
                    radius
            );

            g2.setColor(BORDER);

            g2.drawRoundRect(
                    0, 0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius,
                    radius
            );

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    // =========================================================
    // APPLICATION ENTRY POINT
    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            FlatLightLaf.setup();

            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Table.rowHeight", 34);

            ArithLexGUI window = new ArithLexGUI();
            window.setVisible(true);
        });
    }
}