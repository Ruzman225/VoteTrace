package com.votetrace.lite;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Arrays;

public final class Main extends JFrame {

    private static final Color BACKGROUND = new Color(241, 245, 249);
    private static final Color NAVY = new Color(24, 49, 83);
    private static final Color BLUE = new Color(38, 99, 174);
    private static final Color TEXT = new Color(45, 59, 76);
    private static final Color MUTED = new Color(103, 119, 139);
    private static final Color SUCCESS = new Color(27, 105, 69);
    private static final Color ERROR = new Color(172, 44, 44);

    private final CryptoService cryptoService = new CryptoService();
    private final KeyManager keyManager = new KeyManager();
    private final FileSigner fileSigner = new FileSigner(cryptoService, keyManager);
    private final ActivityLogger activityLogger;
    private final AuthService authService;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final JTextField filePathField = new JTextField();
    private final JTextArea hashArea = new JTextArea(4, 60);
    private final JTextArea statusArea = new JTextArea(3, 60);
    private final JTextField usernameField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JLabel loginStatus = new JLabel("Sign in with an account provisioned in MySQL.");
    private final JLabel signedInLabel = new JLabel();

    private Path selectedFile;
    private KeyManager.KeyFiles generatedKeyFiles;
    private AuthService.User currentUser;

    private Main(DatabaseConfig databaseConfig) {
        super("VoteTrace");
        activityLogger = new ActivityLogger(databaseConfig);
        authService = new AuthService(databaseConfig, activityLogger);
        buildInterface();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException error) {
                System.err.println("Could not apply the system look and feel: " + error.getMessage());
            }
            try {
                Main window = new Main(DatabaseConfig.fromEnvironment());
                window.setVisible(true);
            } catch (SQLException | RuntimeException error) {
                JOptionPane.showMessageDialog(null,
                        "Could not start VoteTrace.\n" + error.getMessage(),
                        "VoteTrace startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void buildInterface() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(760, 580));
        setSize(940, 700);
        setLocationRelativeTo(null);

        cards.add(buildLoginPanel(), "login");
        cards.add(buildWorkspace(), "workspace");
        setContentPane(cards);
        cardLayout.show(cards, "login");

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeApplication();
            }
        });
    }

    private JPanel buildLoginPanel() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(38, 24, 38, 24));

        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 5));
        brand.setOpaque(false);
        JLabel title = new JLabel("VoteTrace");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Electoral Roll Integrity");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        subtitle.setForeground(MUTED);
        brand.add(title);
        brand.add(subtitle);
        page.add(brand, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 10));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 228, 237)),
                BorderFactory.createEmptyBorder(28, 32, 28, 32)));
        form.add(sectionLabel("Sign in"));
        form.add(label("Username"));
        styleInput(usernameField);
        form.add(usernameField);
        form.add(label("Password"));
        styleInput(passwordField);
        form.add(passwordField);

        JButton signIn = primaryButton("Sign in");
        signIn.addActionListener(event -> signIn());
        form.add(signIn);
        loginStatus.setForeground(MUTED);
        loginStatus.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        form.add(loginStatus);

        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.setBorder(BorderFactory.createEmptyBorder(50, 0, 35, 0));
        formWrap.add(form, BorderLayout.CENTER);
        page.add(formWrap, BorderLayout.CENTER);

        JLabel note = new JLabel("Accounts are provisioned locally; passwords are stored as BCrypt hashes.");
        note.setForeground(MUTED);
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        page.add(note, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(signIn);
        return page;
    }

    private JPanel buildWorkspace() {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 3));
        brand.setOpaque(false);
        JLabel heading = new JLabel("VoteTrace");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        heading.setForeground(NAVY);
        JLabel subtitle = new JLabel("Electoral Roll File Integrity and Signature Verification");
        subtitle.setForeground(MUTED);
        brand.add(heading);
        brand.add(subtitle);
        header.add(brand, BorderLayout.WEST);

        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        account.setOpaque(false);
        signedInLabel.setForeground(TEXT);
        account.add(signedInLabel);
        JButton logout = secondaryButton("Log out");
        logout.addActionListener(event -> logOut());
        account.add(logout);
        header.add(account, BorderLayout.EAST);

        JPanel center = new JPanel(new GridLayout(3, 1, 0, 12));
        center.setOpaque(false);
        center.add(buildFilePanel());
        center.add(buildHashPanel());
        center.add(buildStatusPanel());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(primaryButton("Generate RSA Keys", this::generateKeys));
        actions.add(primaryButton("Sign File", this::signFile));
        actions.add(primaryButton("Verify Signature", this::verifySignature));
        actions.add(secondaryButton("Clear / Reset", this::clearFields));

        content.add(header, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        return content;
    }

    private JPanel buildFilePanel() {
        JPanel panel = titledPanel("1. Select an electoral-roll file");
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        filePathField.setEditable(false);
        styleInput(filePathField);
        filePathField.setToolTipText("Selected CSV or TXT file path");
        row.add(filePathField, BorderLayout.CENTER);
        row.add(secondaryButton("Browse CSV / TXT", this::selectFile), BorderLayout.EAST);
        panel.add(row, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(primaryButton("Generate SHA-256", this::generateHash));
        buttons.add(secondaryButton("Save Hash", this::saveHash));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildHashPanel() {
        JPanel panel = titledPanel("2. SHA-256 digest");
        hashArea.setEditable(false);
        hashArea.setLineWrap(true);
        hashArea.setWrapStyleWord(true);
        hashArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        hashArea.setForeground(TEXT);
        hashArea.setBackground(Color.WHITE);
        panel.add(new JScrollPane(hashArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = titledPanel("3. Signature verification status");
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        statusArea.setForeground(TEXT);
        statusArea.setBackground(Color.WHITE);
        statusArea.setText("Select a CSV or TXT file to begin.");
        panel.add(new JScrollPane(statusArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel titledPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 228, 237)), title);
        titledBorder.setTitleColor(NAVY);
        panel.setBorder(BorderFactory.createCompoundBorder(
                titledBorder, BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        return panel;
    }

    private void styleInput(JTextField field) {
        field.setOpaque(true);
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT);
        field.setCaretColor(NAVY);
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        label.setForeground(NAVY);
        return label;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setForeground(TEXT);
        return label;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(BLUE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        return button;
    }

    private JButton primaryButton(String text, Runnable action) {
        JButton button = primaryButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(Color.WHITE);
        button.setForeground(NAVY);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(195, 207, 220)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return button;
    }

    private JButton secondaryButton(String text, Runnable action) {
        JButton button = secondaryButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void signIn() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        try {
            AuthService.User authenticatedUser = authService.authenticate(username, password);
            if (authenticatedUser == null) {
                loginStatus.setForeground(ERROR);
                loginStatus.setText("Sign-in failed. Check your username and password.");
                passwordField.setText("");
                return;
            }
            currentUser = authenticatedUser;
            signedInLabel.setText("Signed in as " + currentUser.username());
            loginStatus.setForeground(SUCCESS);
            loginStatus.setText("Signed in.");
            passwordField.setText("");
            cardLayout.show(cards, "workspace");
        } catch (SQLException | IllegalArgumentException error) {
            loginStatus.setForeground(ERROR);
            loginStatus.setText("Could not sign in: " + error.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an electoral-roll CSV or TXT file");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV and text files (*.csv, *.txt)", "csv", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile().toPath();
            filePathField.setText(selectedFile.toAbsolutePath().toString());
            hashArea.setText("");
            statusArea.setForeground(TEXT);
            statusArea.setText("Selected: " + selectedFile.getFileName());
            recordActivity("FILE_SELECTED", "SUCCESS", selectedFile, "File selected.");
        }
    }

    private void generateHash() {
        if (!requireSelectedFile()) {
            return;
        }
        if (!recordActivity("HASH_GENERATED", "STARTED", selectedFile, "SHA-256 calculation started.")) {
            return;
        }
        String hash;
        try {
            hash = cryptoService.sha256Hex(selectedFile);
        } catch (IOException | GeneralSecurityException error) {
            reportFailure("HASH_GENERATED", "Could not calculate the SHA-256 digest.", selectedFile, error);
            return;
        }
        if (recordActivity("HASH_GENERATED", "SUCCESS", selectedFile, "SHA-256 calculated.")) {
            hashArea.setText(hash);
            statusArea.setForeground(SUCCESS);
            statusArea.setText("SHA-256 calculated for " + selectedFile.getFileName()
                    + ". Generate or verify a digital signature to check authenticity.");
        }
    }

    private void saveHash() {
        String hash = hashArea.getText().trim();
        if (hash.isEmpty()) {
            showMessage("Generate a SHA-256 digest before saving it.", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save SHA-256 digest");
        chooser.setSelectedFile(new java.io.File("electoral_roll.sha256.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path output = chooser.getSelectedFile().toPath();
        if (selectedFile != null && isSamePath(output, selectedFile)) {
            showMessage("The hash text file must not replace the selected source file.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!confirmOverwrite(output)) {
            return;
        }
        if (!recordActivity("HASH_SAVED", "STARTED", selectedFile,
                "Digest save started as " + output.getFileName() + ".")) {
            return;
        }
        try {
            Files.writeString(output, hash + System.lineSeparator(), StandardCharsets.US_ASCII,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException error) {
            reportFailure("HASH_SAVED", "Could not save the SHA-256 digest.", selectedFile, error);
            return;
        }
        if (recordActivity("HASH_SAVED", "SUCCESS", selectedFile,
                "Digest saved as " + output.getFileName() + ".")) {
            statusArea.setForeground(SUCCESS);
            statusArea.setText("SHA-256 digest saved as " + output.getFileName());
        }
    }

    private void generateKeys() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a local folder for the RSA key pair");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path directory = chooser.getSelectedFile().toPath();
        Path privateKeyFile = directory.resolve("votetrace-private.pk8");
        Path publicKeyFile = directory.resolve("votetrace-public.x509");
        if (Files.exists(privateKeyFile) || Files.exists(publicKeyFile)) {
            showMessage("Key files already exist in that folder. Choose a different folder to avoid overwriting keys.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!recordActivity("KEY_PAIR_GENERATED", "STARTED", null, "RSA-2048 key generation started.")) {
            return;
        }
        try {
            generatedKeyFiles = keyManager.generateAndSave(directory);
        } catch (IOException | GeneralSecurityException error) {
            reportFailure("KEY_PAIR_GENERATED", "Could not generate or save the RSA key pair.", null, error);
            return;
        }
        if (recordActivity("KEY_PAIR_GENERATED", "SUCCESS", null, "RSA-2048 key pair generated.")) {
            statusArea.setForeground(SUCCESS);
            statusArea.setText("RSA-2048 keys generated.\nPrivate key (keep local and secret): "
                    + generatedKeyFiles.privateKey().toAbsolutePath()
                    + "\nPublic key: " + generatedKeyFiles.publicKey().toAbsolutePath());
        }
    }

    private void signFile() {
        Path input = selectedFile != null ? selectedFile : chooseFile("Select the file to sign", false);
        if (input == null) {
            return;
        }
        Path privateKey = chooseKeyFile("Select the PKCS#8 private key", "pk8",
                generatedKeyFiles == null ? null : generatedKeyFiles.privateKey());
        if (privateKey == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save detached signature");
        chooser.setSelectedFile(new java.io.File(input.getFileName() + ".sig"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path signature = chooser.getSelectedFile().toPath();
        if (isSamePath(signature, input)) {
            showMessage("The signature file must not replace the file being signed.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!confirmOverwrite(signature)) {
            return;
        }
        if (!recordActivity("FILE_SIGNED", "STARTED", input, "File signing started.")) {
            return;
        }
        try {
            fileSigner.sign(input, privateKey, signature);
        } catch (IOException | GeneralSecurityException error) {
            reportFailure("FILE_SIGNED", "Could not sign the selected file.", input, error);
            return;
        }
        if (recordActivity("FILE_SIGNED", "SUCCESS", input,
                "Detached signature saved as " + signature.getFileName() + ".")) {
            statusArea.setForeground(SUCCESS);
            statusArea.setText("File signed with SHA256withRSA.\nDetached signature saved to: "
                    + signature.toAbsolutePath());
        }
    }

    private void verifySignature() {
        Path input = selectedFile != null ? selectedFile : chooseFile("Select the file to verify", false);
        if (input == null) {
            return;
        }
        Path signature = chooseFile("Select the detached .sig file", true);
        if (signature == null) {
            return;
        }
        Path publicKey = chooseKeyFile("Select the X.509 public key", "x509",
                generatedKeyFiles == null ? null : generatedKeyFiles.publicKey());
        if (publicKey == null) {
            return;
        }

        if (!recordActivity("SIGNATURE_VERIFIED", "STARTED", input,
                "Detached signature verification started.")) {
            return;
        }
        boolean valid;
        try {
            valid = fileSigner.verify(input, signature, publicKey);
        } catch (IOException | GeneralSecurityException | IllegalArgumentException error) {
            reportFailure("SIGNATURE_VERIFIED", "Could not verify the selected signature.", input, error);
            return;
        }
        String outcome = valid ? "VALID" : "INVALID";
        if (recordActivity("SIGNATURE_VERIFIED", outcome, input,
                "Detached signature verification completed.")) {
            statusArea.setForeground(valid ? SUCCESS : ERROR);
            statusArea.setText(valid
                    ? "VALID SIGNATURE\nThe signature matches this file and the selected public key."
                    : "INVALID SIGNATURE\nThe file may have changed, or the signature does not match this public key.");
        }
    }

    private Path chooseFile(String title, boolean signatureFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        if (signatureFile) {
            chooser.setFileFilter(new FileNameExtensionFilter("Detached signatures (*.sig)", "sig"));
        } else {
            chooser.setFileFilter(new FileNameExtensionFilter("CSV and text files (*.csv, *.txt)", "csv", "txt"));
        }
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile().toPath()
                : null;
    }

    private Path chooseKeyFile(String title, String extension, Path suggestedFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("RSA key file (*." + extension + ")", extension));
        if (suggestedFile != null) {
            chooser.setSelectedFile(suggestedFile.toFile());
        }
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile().toPath()
                : null;
    }

    private boolean requireSelectedFile() {
        if (selectedFile == null) {
            showMessage("Browse and select a CSV or TXT file first.", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean confirmOverwrite(Path output) {
        if (!Files.exists(output)) {
            return true;
        }
        return JOptionPane.showConfirmDialog(this,
                "The file already exists. Replace it?\n" + output.toAbsolutePath(),
                "Confirm overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private boolean isSamePath(Path first, Path second) {
        return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
    }

    private boolean recordActivity(String action, String outcome, Path file, String details) {
        try {
            activityLogger.log(currentUser.id(), currentUser.username(), action, outcome,
                    file == null ? null : file.getFileName().toString(), details);
            return true;
        } catch (SQLException error) {
            showOperationError("Could not write the activity record to MySQL. The activity may not be audited.",
                    error);
            return false;
        }
    }

    private void reportFailure(String action, String message, Path file, Exception operationError) {
        String auditWarning = "";
        try {
            activityLogger.log(currentUser.id(), currentUser.username(), action, "FAILURE",
                    file == null ? null : file.getFileName().toString(),
                    operationError.getClass().getSimpleName());
        } catch (SQLException auditError) {
            operationError.addSuppressed(auditError);
            auditWarning = "\nThe failure could not be written to the MySQL activity log.";
        }
        showOperationError(message + auditWarning, operationError);
    }

    private void logOut() {
        if (!recordActivity("LOGOUT", "SUCCESS", null, "User signed out.")) {
            return;
        }
        currentUser = null;
        selectedFile = null;
        generatedKeyFiles = null;
        filePathField.setText("");
        hashArea.setText("");
        statusArea.setForeground(TEXT);
        statusArea.setText("Select a CSV or TXT file to begin.");
        usernameField.setText("");
        passwordField.setText("");
        loginStatus.setForeground(MUTED);
        loginStatus.setText("Signed out. Sign in to continue.");
        cardLayout.show(cards, "login");
    }

    private void closeApplication() {
        if (currentUser != null) {
            try {
                activityLogger.log(currentUser.id(), currentUser.username(),
                        "APPLICATION_CLOSED", "SUCCESS", null, "Application closed.");
            } catch (SQLException error) {
                JOptionPane.showMessageDialog(this,
                        "Could not write the application-close event to the activity log.\n" + error.getMessage(),
                        "Audit log warning", JOptionPane.WARNING_MESSAGE);
            }
        }
        dispose();
    }

    private void clearFields() {
        selectedFile = null;
        filePathField.setText("");
        hashArea.setText("");
        statusArea.setForeground(TEXT);
        statusArea.setText("Select a CSV or TXT file to begin.");
        recordActivity("FIELDS_CLEARED", "SUCCESS", null, "Workspace fields reset.");
    }

    private void showOperationError(String message, Exception error) {
        statusArea.setForeground(ERROR);
        statusArea.setText(message + "\n" + error.getMessage());
        showMessage(message + "\n" + error.getMessage(), JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message, int messageType) {
        JOptionPane.showMessageDialog(this, message, "VoteTrace", messageType);
    }
}
