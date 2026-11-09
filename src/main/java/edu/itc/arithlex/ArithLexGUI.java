package edu.itc.arithlex;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArithLexGUI extends JFrame {

    // ---------- Colors (Apple system palette) ----------
    private static final Color BACKGROUND = new Color(245, 245, 247);
    private static final Color SIDEBAR = new Color(238, 238, 243);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT = new Color(29, 29, 31);
    private static final Color MUTED = new Color(134, 134, 139);
    private static final Color BLUE = new Color(0, 122, 255);
    private static final Color BORDER = new Color(229, 229, 234);
    private static final Color RED = new Color(235, 50, 45);
    private static final Color GREEN = new Color(36, 160, 72);

    // ---------- Fonts (SF Pro on Apple, Segoe UI on Windows, ...) ----------
    private static final Set<String> FAMILIES = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()
    ));

    private static final String UI_FAMILY = pickFamily(
            "SF Pro Text", "SF Pro Display", "Helvetica Neue",
            "Segoe UI", "Inter", "SansSerif"
    );

    private static final String MONO_FAMILY = pickFamily(
            "SF Mono", "Menlo", "JetBrains Mono", "Consolas", "Monospaced"
    );

    private static String pickFamily(String... names) {
        for (String name : names) {
            if (FAMILIES.contains(name)) {
                return name;
            }
        }
        return names[names.length - 1];
    }

    private static Font ui(int style, int size) {
        return new Font(UI_FAMILY, style, size);
    }

    private static Font mono(int style, int size) {
        return new Font(MONO_FAMILY, style, size);
    }

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

        // macOS: unified, transparent title bar
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

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
        sidebar.setPreferredSize(new Dimension(236, 0));
        sidebar.setBackground(SIDEBAR);
        sidebar.setBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER)
        );

        sidebar.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(44, 18, 20, 18));

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        LogoBadge logo = new LogoBadge("⌘");

        JLabel brand = new JLabel("ArithLex");
        brand.setFont(ui(Font.BOLD, 20));
        brand.setForeground(TEXT);
        brand.setBorder(new EmptyBorder(0, 12, 0, 0));

        logoRow.add(logo);
        logoRow.add(brand);

        JLabel description = new JLabel("Lexical Analyzer");
        description.setFont(ui(Font.PLAIN, 12));
        description.setForeground(MUTED);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(logoRow);
        top.add(Box.createVerticalStrut(8));
        top.add(description);
        top.add(Box.createVerticalStrut(36));

        top.add(createNavButton("▣", "Workspace", true));
        top.add(Box.createVerticalStrut(4));
        top.add(createNavButton("▤", "Documentation", false));
        top.add(Box.createVerticalStrut(4));
        top.add(createNavButton("ⓘ", "About ArithLex", false));

        sidebar.add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(15, 24, 26, 15));

        JLabel version = new JLabel("VERSION 1.0.0");
        version.setFont(ui(Font.BOLD, 10));
        version.setForeground(MUTED);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel technology = new JLabel("Java  •  JFlex");
        technology.setFont(ui(Font.PLAIN, 12));
        technology.setForeground(MUTED);
        technology.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        NavButton button = new NavButton(icon + "    " + label, selected);

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

        JPanel workspace = new JPanel(new BorderLayout(0, 12));
        workspace.setBackground(BACKGROUND);
        workspace.setBorder(new EmptyBorder(40, 30, 18, 30));

        workspace.add(createHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.setOpaque(false);

        body.add(createEditorSection(), BorderLayout.NORTH);
        body.add(createResultsSection(), BorderLayout.CENTER);

        workspace.add(body, BorderLayout.CENTER);

        return workspace;
    }

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 6, 0, 6));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Arithmetic Expression Analyzer");
        title.setFont(ui(Font.BOLD, 28));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "Analyze the lexical structure of your arithmetic expressions."
        );
        subtitle.setFont(ui(Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);

        statusLabel = new PillLabel("●  Ready");
        statusLabel.setFont(ui(Font.BOLD, 12));
        statusLabel.setForeground(GREEN);
        statusLabel.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel statusWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        statusWrap.setOpaque(false);
        statusWrap.add(statusLabel);

        header.add(heading, BorderLayout.WEST);
        header.add(statusWrap, BorderLayout.EAST);

        return header;
    }

    // =========================================================
    // EDITOR
    // =========================================================

    private JPanel createEditorSection() {

        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(0, 6, 0, 6));

        JLabel label = new JLabel("Input Expression");
        label.setFont(ui(Font.BOLD, 16));
        label.setForeground(TEXT);

        JLabel hint = new JLabel("Enter an expression or open a .txt file");
        hint.setFont(ui(Font.PLAIN, 12));
        hint.setForeground(MUTED);

        titleRow.add(label, BorderLayout.WEST);
        titleRow.add(hint, BorderLayout.EAST);

        RoundedPanel editorCard = new RoundedPanel(16, WHITE, 8);
        editorCard.setLayout(new BorderLayout());

        inputArea = new JTextArea();
        inputArea.setFont(mono(Font.PLAIN, 14));
        inputArea.setForeground(TEXT);
        inputArea.setBackground(WHITE);
        inputArea.setCaretColor(BLUE);
        inputArea.setSelectionColor(new Color(179, 215, 255));
        inputArea.setLineWrap(false);
        inputArea.setTabSize(4);
        inputArea.setBorder(new EmptyBorder(8, 12, 8, 12));

        lineNumbers = new JTextArea("1");
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setFont(mono(Font.PLAIN, 14));
        lineNumbers.setForeground(new Color(174, 174, 178));
        lineNumbers.setBackground(WHITE);
        lineNumbers.setBorder(new EmptyBorder(8, 12, 8, 12));

        JScrollPane editorScroll = new JScrollPane(inputArea);
        editorScroll.setBorder(BorderFactory.createEmptyBorder());
        editorScroll.setRowHeaderView(lineNumbers);
        editorScroll.getRowHeader().setBackground(WHITE);
        editorScroll.getViewport().setBackground(WHITE);
        editorScroll.setPreferredSize(new Dimension(700, 160));

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
        actionRow.setBorder(new EmptyBorder(0, 6, 0, 6));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        JButton analyzeButton = createButton("▶  Analyze", AppleButton.Style.PRIMARY);
        JButton clearButton = createButton("Clear", AppleButton.Style.SECONDARY);
        JButton openButton = createButton("Open File", AppleButton.Style.OUTLINE);
        JButton saveButton = createButton("Save Result", AppleButton.Style.OUTLINE);

        analyzeButton.addActionListener(this::analyzeInput);
        clearButton.addActionListener(e -> clearAll());
        openButton.addActionListener(e -> openFile());
        saveButton.addActionListener(e -> saveResult());

        leftActions.add(analyzeButton);
        leftActions.add(clearButton);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);
        rightActions.add(openButton);
        rightActions.add(saveButton);

        actionRow.add(leftActions, BorderLayout.WEST);
        actionRow.add(rightActions, BorderLayout.EAST);

        JPanel editorContent = new JPanel(new BorderLayout(0, 6));
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

        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setOpaque(false);

        JPanel summary = new JPanel(new BorderLayout());
        summary.setOpaque(false);
        summary.setBorder(new EmptyBorder(10, 6, 0, 6));

        JLabel resultTitle = new JLabel("Analysis Results");
        resultTitle.setFont(ui(Font.BOLD, 16));
        resultTitle.setForeground(TEXT);

        JPanel counts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        counts.setOpaque(false);

        tokenCountLabel = createCountLabel("Tokens: 0", BLUE);
        errorCountLabel = createCountLabel("Errors: 0", RED);

        counts.add(tokenCountLabel);
        counts.add(errorCountLabel);

        summary.add(resultTitle, BorderLayout.WEST);
        summary.add(counts, BorderLayout.EAST);

        String[] columns = {"Token Type", "Lexeme", "Line", "Column"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tokenTable = new JTable(tableModel);
        tokenTable.setFont(ui(Font.PLAIN, 13));
        tokenTable.setBackground(WHITE);
        tokenTable.setRowHeight(38);
        tokenTable.setShowVerticalLines(false);
        tokenTable.setShowHorizontalLines(true);
        tokenTable.setGridColor(new Color(242, 242, 247));
        tokenTable.setSelectionBackground(new Color(224, 238, 255));
        tokenTable.setSelectionForeground(TEXT);
        tokenTable.setFillsViewportHeight(true);
        tokenTable.setIntercellSpacing(new Dimension(0, 1));

        tokenTable.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        tokenTable.getTableHeader().setReorderingAllowed(false);
        tokenTable.getTableHeader().setPreferredSize(
                new Dimension(0, 36)
        );

        tokenTable.getColumnModel().getColumn(0)
                .setCellRenderer(new ChipRenderer());

        tokenTable.getColumnModel().getColumn(1)
                .setCellRenderer(
                        new CellRenderer(SwingConstants.LEFT, true, false)
                );

        tokenTable.getColumnModel().getColumn(2)
                .setCellRenderer(
                        new CellRenderer(SwingConstants.CENTER, false, true)
                );

        tokenTable.getColumnModel().getColumn(3)
                .setCellRenderer(
                        new CellRenderer(SwingConstants.CENTER, false, true)
                );

        tokenTable.getColumnModel().getColumn(0)
                .setPreferredWidth(170);

        tokenTable.getColumnModel().getColumn(1)
                .setPreferredWidth(170);

        tokenTable.getColumnModel().getColumn(2)
                .setPreferredWidth(70);

        tokenTable.getColumnModel().getColumn(3)
                .setPreferredWidth(70);

        JScrollPane tableScroll = new JScrollPane(tokenTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(WHITE);

        RoundedPanel tokenCard = new RoundedPanel(16, WHITE, 10);
        tokenCard.setLayout(new BorderLayout());
        tokenCard.add(tableScroll, BorderLayout.CENTER);

        JPanel errorHeader = new JPanel(new BorderLayout());
        errorHeader.setOpaque(false);
        errorHeader.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel errorTitle = new JLabel("⚠  Lexical Errors");
        errorTitle.setFont(ui(Font.BOLD, 14));
        errorTitle.setForeground(RED);

        errorHeader.add(errorTitle, BorderLayout.WEST);

        errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setLineWrap(true);
        errorArea.setWrapStyleWord(true);
        errorArea.setFont(ui(Font.PLAIN, 13));
        errorArea.setForeground(new Color(150, 55, 65));
        errorArea.setBackground(WHITE);
        errorArea.setBorder(new EmptyBorder(2, 2, 2, 2));
        errorArea.setText("No lexical errors found.");

        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setBorder(BorderFactory.createEmptyBorder());
        errorScroll.getViewport().setBackground(WHITE);

        RoundedPanel errorCard = new RoundedPanel(16, WHITE, 14);
        errorCard.setLayout(new BorderLayout());
        errorCard.add(errorHeader, BorderLayout.NORTH);
        errorCard.add(errorScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                tokenCard,
                errorCard
        );

        splitPane.setResizeWeight(0.68);
        splitPane.setDividerSize(4);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);

        // Invisible divider so the two cards float on the background
        if (splitPane.getUI() instanceof BasicSplitPaneUI) {
            BasicSplitPaneUI splitUI = (BasicSplitPaneUI) splitPane.getUI();
            splitUI.getDivider().setBackground(BACKGROUND);
            splitUI.getDivider().setBorder(null);
        }

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
                errorArea.setForeground(GREEN);
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
                setStatus("●  Analysis Complete", GREEN);
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

        chooser.setDialogTitle("Import Arithmetic Expression File");

        chooser.setFileFilter(
                new FileNameExtensionFilter("Text Files (*.txt)", "txt")
        );

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path selectedFile = chooser.getSelectedFile().toPath();

        try {

            // Create the project's input directory if necessary.
            Path inputDirectory = Path.of("input");
            Files.createDirectories(inputDirectory);

            // Destination file inside the project.
            Path destination = inputDirectory.resolve(
                    selectedFile.getFileName()
            );

            // Check whether the selected file is already the destination.
            boolean sameFile = Files.exists(destination)
                    && Files.isSameFile(selectedFile, destination);

            // Ask before replacing an existing file.
            if (!sameFile && Files.exists(destination)) {

                int answer = JOptionPane.showConfirmDialog(
                        this,
                        "A file with this name already exists in input/.\n"
                                + "Do you want to replace it?",
                        "Confirm Replacement",
                        JOptionPane.YES_NO_OPTION
                );

                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Copy the selected file into input/.
            if (!sameFile) {
                Files.copy(
                        selectedFile,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            // Load the copied file into the editor.
            String content = Files.readString(
                    destination,
                    StandardCharsets.UTF_8
            );

            inputArea.setText(content);
            currentFile = destination.toString();

            setStatus(
                    "●  Imported: " + destination.getFileName(),
                    GREEN
            );

            JOptionPane.showMessageDialog(
                    this,
                    "File imported successfully!\n\n"
                            + "Saved to: " + destination,
                    "Import Successful",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Could not import file: " + ex.getMessage(),
                    "Import Error",
                    JOptionPane.ERROR_MESSAGE
            );

            setStatus("●  Import Failed", RED);
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

        setStatus("●  Ready", GREEN);
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

    private JButton createButton(String text, AppleButton.Style style) {
        return new AppleButton(text, style);
    }

    private JLabel createCountLabel(String text, Color color) {

        JLabel label = new PillLabel(text);

        label.setFont(ui(Font.BOLD, 12));
        label.setForeground(color);
        label.setBorder(new EmptyBorder(6, 12, 6, 12));

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
    // CUSTOM UI COMPONENTS
    // =========================================================

    private static void enableAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
    }

    /** White card with rounded corners and a soft, Apple-style shadow. */
    private static class RoundedPanel extends JPanel {

        private static final int SHADOW = 6;

        private final int radius;
        private final Color backgroundColor;

        public RoundedPanel(int radius, Color backgroundColor, int padding) {

            this.radius = radius;
            this.backgroundColor = backgroundColor;

            setOpaque(false);

            int p = padding + SHADOW;
            setBorder(new EmptyBorder(p, p, p, p));
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g2 = (Graphics2D) graphics.create();
            enableAntialiasing(g2);

            int w = getWidth();
            int h = getHeight();

            // Soft layered shadow
            for (int e = 1; e <= SHADOW - 2; e++) {
                g2.setColor(new Color(0, 0, 0, 7));
                g2.fill(new RoundRectangle2D.Float(
                        SHADOW - e,
                        SHADOW - e + 2,
                        w - 2 * SHADOW + 2 * e,
                        h - 2 * SHADOW + 2 * e,
                        (radius + e) * 2,
                        (radius + e) * 2
                ));
            }

            // Card body
            g2.setColor(backgroundColor);
            g2.fill(new RoundRectangle2D.Float(
                    SHADOW, SHADOW,
                    w - 2 * SHADOW, h - 2 * SHADOW,
                    radius * 2, radius * 2
            ));

            // Hairline border
            g2.setColor(BORDER);
            g2.draw(new RoundRectangle2D.Float(
                    SHADOW + 0.5f, SHADOW + 0.5f,
                    w - 2 * SHADOW - 1, h - 2 * SHADOW - 1,
                    radius * 2, radius * 2
            ));

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    /** Label drawn as a pill tinted with its own foreground color. */
    private static class PillLabel extends JLabel {

        PillLabel(String text) {
            super(text);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g2 = (Graphics2D) graphics.create();
            enableAntialiasing(g2);

            Color fg = getForeground();
            g2.setColor(new Color(
                    fg.getRed(), fg.getGreen(), fg.getBlue(), 30
            ));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                    getHeight(), getHeight());

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    /** Rounded gradient app-icon style badge. */
    private static class LogoBadge extends JComponent {

        private final String symbol;

        LogoBadge(String symbol) {
            this.symbol = symbol;
            setPreferredSize(new Dimension(44, 44));
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g2 = (Graphics2D) graphics.create();
            enableAntialiasing(g2);

            int w = getWidth();
            int h = getHeight();

            g2.setPaint(new GradientPaint(
                    0, 0, new Color(90, 170, 255),
                    0, h, new Color(0, 100, 235)
            ));
            g2.fillRoundRect(0, 0, w, h, 22, 22);

            g2.setColor(WHITE);
            g2.setFont(ui(Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            int x = (w - fm.stringWidth(symbol)) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(symbol, x, y);

            g2.dispose();
        }
    }

    /** Sidebar navigation item with hover and selected states. */
    private static class NavButton extends JButton {

        private final boolean selected;
        private boolean hover = false;

        NavButton(String text, boolean selected) {

            super(text);

            this.selected = selected;

            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(ui(selected ? Font.BOLD : Font.PLAIN, 13));
            setForeground(selected ? BLUE : TEXT);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setBorder(new EmptyBorder(11, 14, 11, 10));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g2 = (Graphics2D) graphics.create();
            enableAntialiasing(g2);

            if (selected) {
                g2.setColor(new Color(0, 122, 255, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            } else if (hover) {
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    /** Pill-shaped button in three Apple-like styles. */
    private static class AppleButton extends JButton {

        enum Style { PRIMARY, SECONDARY, OUTLINE }

        private final Style style;
        private boolean hover = false;

        AppleButton(String text, Style style) {

            super(text);

            this.style = style;

            setFont(ui(Font.BOLD, 13));
            setForeground(style == Style.PRIMARY ? WHITE : TEXT);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setBorder(new EmptyBorder(9, 18, 9, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g2 = (Graphics2D) graphics.create();
            enableAntialiasing(g2);

            boolean pressed = getModel().isPressed();
            Color fill;

            switch (style) {
                case PRIMARY:
                    fill = pressed ? new Color(0, 95, 205)
                            : hover ? new Color(10, 132, 255)
                            : BLUE;
                    break;
                case SECONDARY:
                    fill = pressed ? new Color(208, 208, 215)
                            : hover ? new Color(222, 222, 228)
                            : new Color(232, 232, 237);
                    break;
                default:
                    fill = pressed ? new Color(236, 236, 242)
                            : hover ? new Color(247, 247, 250)
                            : WHITE;
                    break;
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                    getHeight(), getHeight());

            if (style == Style.OUTLINE) {
                g2.setColor(new Color(210, 210, 215));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        getHeight(), getHeight());
            }

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    // =========================================================
    // TABLE RENDERERS
    // =========================================================

    private static class HeaderRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column
        ) {

            super.getTableCellRendererComponent(
                    table,
                    value == null ? "" : value.toString().toUpperCase(),
                    false, false, row, column
            );

            setHorizontalAlignment(
                    column >= 2 ? SwingConstants.CENTER : SwingConstants.LEFT
            );
            setFont(ui(Font.BOLD, 11));
            setForeground(MUTED);
            setBackground(WHITE);
            setOpaque(true);
            setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(0, 14, 0, 14)
            ));

            return this;
        }
    }

    private static class CellRenderer extends DefaultTableCellRenderer {

        private final int alignment;
        private final boolean monospaced;
        private final boolean muted;

        CellRenderer(int alignment, boolean monospaced, boolean muted) {
            this.alignment = alignment;
            this.monospaced = monospaced;
            this.muted = muted;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column
        ) {

            super.getTableCellRendererComponent(
                    table, value, isSelected, false, row, column
            );

            setHorizontalAlignment(alignment);
            setFont(monospaced ? mono(Font.PLAIN, 13) : ui(Font.PLAIN, 13));
            setForeground(muted && !isSelected ? MUTED : TEXT);
            setBorder(new EmptyBorder(0, 14, 0, 14));

            return this;
        }
    }

    /** Draws the token type as a colored capsule. */
    private static class ChipRenderer extends DefaultTableCellRenderer {

        private static final Color[] PALETTE = {
                new Color(0, 122, 255),
                new Color(36, 138, 61),
                new Color(201, 52, 0),
                new Color(175, 82, 222),
                new Color(215, 0, 75),
                new Color(0, 130, 155),
                new Color(88, 86, 214)
        };

        private Color chip = BLUE;

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column
        ) {

            super.getTableCellRendererComponent(
                    table, value, isSelected, false, row, column
            );

            String text = String.valueOf(value);
            chip = PALETTE[Math.floorMod(text.hashCode(), PALETTE.length)];

            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(ui(Font.BOLD, 11));
            setForeground(chip);
            setBorder(new EmptyBorder(0, 23, 0, 12));
            setOpaque(false);

            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g2 = (Graphics2D) graphics.create();
            enableAntialiasing(g2);

            // Row background (we are not opaque, so paint it ourselves)
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            FontMetrics fm = getFontMetrics(getFont());
            int textWidth = fm.stringWidth(getText());
            int pillHeight = 22;
            int x = Math.max(0, getInsets().left - 10);
            int y = (getHeight() - pillHeight) / 2;

            g2.setColor(new Color(
                    chip.getRed(), chip.getGreen(), chip.getBlue(), 30
            ));
            g2.fillRoundRect(x, y, textWidth + 20, pillHeight,
                    pillHeight, pillHeight);

            g2.dispose();

            super.paintComponent(graphics);
        }
    }

    // =========================================================
    // APPLICATION ENTRY POINT
    // =========================================================

    public static void main(String[] args) {

        System.setProperty("apple.awt.application.name", "ArithLex");

        SwingUtilities.invokeLater(() -> {

            FlatLightLaf.setup();

            UIManager.put("defaultFont", ui(Font.PLAIN, 13));

            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 999);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("Table.rowHeight", 38);

            // Thin, rounded, macOS-style scrollbars
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.track", new Color(0, 0, 0, 0));
            UIManager.put("ScrollBar.thumb", new Color(0, 0, 0, 55));
            UIManager.put("ScrollBar.hoverThumbColor", new Color(0, 0, 0, 100));
            UIManager.put("ScrollBar.pressedThumbColor", new Color(0, 0, 0, 130));

            ArithLexGUI window = new ArithLexGUI();
            window.setVisible(true);
        });
    }
}