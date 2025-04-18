package marcin0816.dev;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class MinecraftLauncher extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String VERSIONS_URL = "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private static final Logger LOGGER = LoggerUtil.getLogger();
    private static final String LAUNCHER_DIR = "C:\\MCLauncher";

    private final JButton actionButton;
    private final JTextField usernameField;
    private final JList<String> versionList;
    private final DefaultListModel<String> versionListModel;
    private JSONArray versionsArray;
    private final JProgressBar progressBar;
    private final JCheckBox customJavaPathCheckbox;
    private final JTextField customJavaPathField;

    // New memory-related variables
    private JSlider memorySlider;
    private JLabel memoryLabel;
    private int memoryAllocation = 2048; // Default to 2GB

    public MinecraftLauncher() {
        setTitle(LanguageSettings.getMessage("launcher_title"));
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(0, 1));

        panel.add(new JLabel(LanguageSettings.getMessage("language_label")));
        panel.add(getLanguageJComboBox());

        panel.add(new JLabel(LanguageSettings.getMessage("username_label")));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel(LanguageSettings.getMessage("version_label")));
        versionListModel = new DefaultListModel<>();
        versionList = new JList<>(versionListModel);
        versionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        versionList.addListSelectionListener(_e -> updateActionButton());
        JScrollPane scroll = new JScrollPane(versionList);
        scroll.setPreferredSize(new Dimension(200, 100));
        panel.add(scroll);

        customJavaPathCheckbox = new JCheckBox(LanguageSettings.getMessage("custom_java_path"));
        customJavaPathCheckbox.addActionListener(_e -> updateJavaPathFieldState());
        panel.add(customJavaPathCheckbox);

        panel.add(new JLabel(LanguageSettings.getMessage("java_path_label")));
        customJavaPathField = new JTextField(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        customJavaPathField.setEnabled(false);
        panel.add(customJavaPathField);

        // Add memory allocation panel
        panel.add(createMemoryPanel());

        actionButton = new JButton(LanguageSettings.getMessage("download_version_button"));
        actionButton.setEnabled(false);
        actionButton.addActionListener(_e -> {
            if (actionButton.getText().equals(LanguageSettings.getMessage("download_version_button"))) {
                new Thread(() -> {
                    String version = versionList.getSelectedValue();
                    if (version != null && !version.isEmpty()) {
                        downloadSelectedVersion(version);
                    }
                }).start();
            } else {
                new Thread(() -> {
                    String user = usernameField.getText().trim();
                    if (!user.isEmpty()) {
                        launchGame(user);
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                null,
                                LanguageSettings.getMessage("error_no_username"),
                                "Błąd",
                                JOptionPane.ERROR_MESSAGE));
                    }
                }).start();
            }
        });

        add(panel, BorderLayout.CENTER);
        add(actionButton, BorderLayout.SOUTH);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.NORTH);

        loadAvailableVersions();
    }
    // Create memory panel with slider
    private JPanel createMemoryPanel() {
        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.add(new JLabel(LanguageSettings.getMessage("memory_allocation_label")), BorderLayout.WEST);

        // Create slider with range from 1GB to 8GB, default to 2GB
        memorySlider = new JSlider(JSlider.HORIZONTAL, 1024, 8192, memoryAllocation);
        memorySlider.setMajorTickSpacing(1024);
        memorySlider.setPaintTicks(true);
        memorySlider.setSnapToTicks(true);

        // Update the memory allocation when slider changes
        memorySlider.addChangeListener(e -> {
            memoryAllocation = memorySlider.getValue();
            updateMemoryLabel();
        });

        memoryPanel.add(memorySlider, BorderLayout.CENTER);

        // Add label to show current memory value
        memoryLabel = new JLabel(memoryAllocation / 1024 + " GB");
        memoryPanel.add(memoryLabel, BorderLayout.EAST);

        return memoryPanel;
    }

    // Method to update the memory label
    private void updateMemoryLabel() {
        memoryLabel.setText(memoryAllocation / 1024 + " GB");
    }

    private JComboBox<LanguageSettings.Language> getLanguageJComboBox() {
        LanguageSettings.Language[] langs = {LanguageSettings.Language.ENGLISH, LanguageSettings.Language.POLISH};
        JComboBox<LanguageSettings.Language> combo = new JComboBox<>(langs);
        combo.setSelectedItem(LanguageSettings.getCurrentLanguageEnum());
        combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                LanguageSettings.setLanguage((LanguageSettings.Language) e.getItem());
                refreshUI();
            }
        });
        return combo;
    }

    private void refreshUI() {
        setTitle(LanguageSettings.getMessage("launcher_title"));
        JPanel p = (JPanel) getContentPane().getComponent(0);
        ((JLabel) p.getComponent(0)).setText(LanguageSettings.getMessage("language_label"));
        ((JLabel) p.getComponent(2)).setText(LanguageSettings.getMessage("username_label"));
        ((JLabel) p.getComponent(4)).setText(LanguageSettings.getMessage("version_label"));
        customJavaPathCheckbox.setText(LanguageSettings.getMessage("custom_java_path"));
        ((JLabel) p.getComponent(7)).setText(LanguageSettings.getMessage("java_path_label"));
        ((JLabel) p.getComponent(9)).setText(LanguageSettings.getMessage("memory_allocation_label"));
        updateActionButton();
    }

    private void updateActionButton() {
        String version = versionList.getSelectedValue();
        if (version != null && !version.isEmpty()) {
            Path verPath = Paths.get(LAUNCHER_DIR, "versions", version);
            actionButton.setText(verPath.toFile().exists()
                    ? LanguageSettings.getMessage("run_game_button")
                    : LanguageSettings.getMessage("download_version_button"));
            actionButton.setEnabled(true);
        } else {
            actionButton.setEnabled(false);
        }
    }

    private void updateJavaPathFieldState() {
        if (customJavaPathCheckbox.isSelected()) {
            customJavaPathField.setEnabled(true);
        } else {
            customJavaPathField.setEnabled(false);
            customJavaPathField.setText(System.getProperty("java.home")
                    + File.separator + "bin" + File.separator + "java");
        }
    }

    private void loadAvailableVersions() {
        try {
            String json = downloadJson(VERSIONS_URL);
            JSONObject root = new JSONObject(json);
            versionsArray = root.getJSONArray("versions");
            for (int i = 0; i < versionsArray.length(); i++) {
                JSONObject v = versionsArray.getJSONObject(i);
                if ("release".equals(v.getString("type"))) {
                    versionListModel.addElement(v.getString("id"));
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                    LanguageSettings.getMessage("error_missing_jar") + e.getMessage(), e);
        }
    }

    // Pobierz JSON z podanego URL
    private String downloadJson(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + conn.getResponseCode());
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    // Pobierz i zapisz wybraną wersję Minecrafta
    private void downloadSelectedVersion(String versionId) {
        try {
            progressBar.setValue(0);
            progressBar.setString(LanguageSettings.getMessage("download_version_progress"));
            JSONObject selected = null;
            for (int i = 0; i < versionsArray.length(); i++) {
                if (versionsArray.getJSONObject(i).getString("id").equals(versionId)) {
                    selected = versionsArray.getJSONObject(i);
                    break;
                }
            }
            if (selected == null) return;
            JSONObject info = new JSONObject(downloadJson(selected.getString("url")));
            // klient
            Path verDir = Paths.get(LAUNCHER_DIR, "versions", versionId);
            Files.createDirectories(verDir);
            Path jarPath = verDir.resolve(versionId + ".jar");
            String clientUrl = info.getJSONObject("downloads").getJSONObject("client").getString("url");
            downloadJar(clientUrl, jarPath.toString());
            progressBar.setValue(50);
            // biblioteki
            downloadLibraries(info);
            progressBar.setValue(75);
            // assety
            downloadAssets(info);
            progressBar.setValue(100);
            progressBar.setString(LanguageSettings.getMessage("download_version_progress") + " zakończone");
            updateActionButton();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Błąd pobierania wersji: " + versionId, e);
        }
    }

    // Metoda pobierająca plik .jar pod podanym URL
    private void downloadJar(String urlString, String savePath) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        conn.setRequestMethod("GET");
        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[4096]; int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    // Pobieranie bibliotek z JSON-a wersji
    private void downloadLibraries(JSONObject info) throws IOException {
        JSONArray libs = info.getJSONArray("libraries");
        for (int i = 0; i < libs.length(); i++) {
            JSONObject lib = libs.getJSONObject(i);
            if (lib.has("downloads") && lib.getJSONObject("downloads").has("artifact")) {
                JSONObject art = lib.getJSONObject("downloads").getJSONObject("artifact");
                String url = art.getString("url");
                Path libPath = Paths.get(LAUNCHER_DIR, "libraries", art.getString("path"));
                Files.createDirectories(libPath.getParent());
                if (!Files.exists(libPath)) downloadJar(url, libPath.toString());
            }
        }
    }

    // Pobieranie assetów
    private void downloadAssets(JSONObject info) throws IOException {
        JSONObject assetIndex = info.getJSONObject("assetIndex");
        String idxUrl = assetIndex.getString("url");
        Path idxDir = Paths.get(LAUNCHER_DIR, "assets", "indexes");
        Files.createDirectories(idxDir);
        Path idxPath = idxDir.resolve(assetIndex.getString("id") + ".json");
        if (!Files.exists(idxPath)) downloadJar(idxUrl, idxPath.toString());
        JSONObject assetsJson = new JSONObject(Files.readString(idxPath, StandardCharsets.UTF_8));
        JSONObject objects = assetsJson.getJSONObject("objects");
        for (String key : objects.keySet()) {
            JSONObject obj = objects.getJSONObject(key);
            String hash = obj.getString("hash");
            String url = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
            Path target = Paths.get(LAUNCHER_DIR, "assets", key);
            Files.createDirectories(target.getParent());
            if (!Files.exists(target)) downloadJar(url, target.toString());
        }
    }

    // Method to validate memory allocation before running the game
    private boolean validateMemorySettings() {
        // Get amount of available system memory in MB
        long maxSystemMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);

        // If user is trying to allocate more than 80% of available memory, show warning
        if (memoryAllocation > (maxSystemMemory * 0.8)) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    LanguageSettings.getMessage("memory_warning_message"),
                    LanguageSettings.getMessage("memory_warning_title"),
                    JOptionPane.YES_NO_OPTION
            );
            return choice == JOptionPane.YES_OPTION;
        }
        return true;
    }

    // Helper method to extract view distance value from log message
    private int extractViewDistance(String logLine) {
        // Example log format: "[17:05:41] [Server thread/INFO]: Changing view distance to 32, from 12"
        String[] parts = logLine.split("to ");
        if (parts.length >= 2) {
            String distancePart = parts[1].split(",")[0].trim();
            return Integer.parseInt(distancePart);
        }
        return 0;
    }

    private void launchGame(String username) {
        try {
            // Validate memory settings before proceeding
            if (!validateMemorySettings()) {
                return;
            }

            String javaPath;
            if (customJavaPathCheckbox.isSelected()) {
                javaPath = customJavaPathField.getText();
            } else {
                javaPath = findShortestJavaPath();
            }

            if (javaPath == null) {
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        "Nie można znaleźć Java automatycznie. Wybierasz ręcznie?",
                        "Błąd Java",
                        JOptionPane.YES_NO_OPTION
                );
                if (choice == JOptionPane.YES_OPTION) {
                    javaPath = selectJavaExecutable();
                }
            }
            if (javaPath == null) {
                JOptionPane.showMessageDialog(
                        null,
                        LanguageSettings.getMessage("error_java_path"),
                        "Błąd",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            String version = versionList.getSelectedValue();
            Path gameDir = Paths.get(LAUNCHER_DIR);
            Path versionJar = gameDir.resolve("versions").resolve(version).resolve(version + ".jar");
            if (!Files.exists(versionJar)) {
                JOptionPane.showMessageDialog(
                        null,
                        LanguageSettings.getMessage("error_missing_jar") + versionJar,
                        "Błąd",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            StringBuilder classpath = new StringBuilder();
            File libs = gameDir.resolve("libraries").toFile();
            addLibs(libs, classpath);
            classpath.append(File.pathSeparator).append(versionJar.toAbsolutePath());

            // Updated JVM arguments with improved memory settings
            List<String> args = new ArrayList<>(Arrays.asList(
                    "-Xmx" + memoryAllocation + "M",
                    "-Xms" + (memoryAllocation / 2) + "M",
                    "-XX:+UseG1GC",
                    "-XX:+ParallelRefProcEnabled",
                    "-XX:MaxGCPauseMillis=200",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+DisableExplicitGC",
                    "-XX:+AlwaysPreTouch",
                    "-XX:G1NewSizePercent=30",
                    "-XX:G1MaxNewSizePercent=40",
                    "-XX:G1HeapRegionSize=8M",
                    "-XX:G1ReservePercent=20",
                    "-XX:G1HeapWastePercent=5",
                    "-XX:G1MixedGCCountTarget=4",
                    "-XX:InitiatingHeapOccupancyPercent=15",
                    "-XX:G1MixedGCLiveThresholdPercent=90",
                    "-XX:G1RSetUpdatingPauseTimePercent=5",
                    "-XX:SurvivorRatio=32",
                    "-XX:+PerfDisableSharedMem",
                    "-XX:MaxTenuringThreshold=1",
                    "-cp",
                    classpath.toString(),
                    "net.minecraft.client.main.Main",
                    "--username", username,
                    "--version", version,
                    "--gameDir", gameDir.toString(),
                    "--assetsDir", gameDir.resolve("assets").toString(),
                    "--uuid", "0",
                    "--accessToken", "dummy_access_token"
            ));

            Path tmp = Files.createTempFile("minecraft_args", ".txt");
            Files.write(tmp, args, StandardCharsets.UTF_8);

            List<String> command = Arrays.asList(javaPath, "@" + tmp.toAbsolutePath().toString());
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // Poprawiony kod konsoli - nie używamy try-with-resources aby nie zamykać readera
            JFrame console = new JFrame("Minecraft Console");
            console.setSize(600, 400);
            JTextArea output = new JTextArea();
            output.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(output);
            console.add(scrollPane);

            // Dodaj notatkę o pamięci na początku
            output.append("NOTE: If you experience lag or crashes, try increasing memory allocation in the launcher.\n\n");
            console.setVisible(true);

            // Uruchom wątek odczytujący wyjście - NIE zamykaj readera w try-with-resources
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String logLine = line;

                        // Zapisz linię do konsoli
                        SwingUtilities.invokeLater(() -> {
                            output.append(logLine + "\n");
                            // Przewiń na dół, aby zawsze pokazywać najnowsze logi
                            output.setCaretPosition(output.getDocument().getLength());
                        });

                        // Monitoruj zmiany odległości renderowania
                        if (logLine.contains("Changing view distance") && memoryAllocation < 4096) {
                            try {
                                int viewDistance = extractViewDistance(logLine);
                                if (viewDistance > 16) {
                                    SwingUtilities.invokeLater(() ->
                                            JOptionPane.showMessageDialog(
                                                    null,
                                                    LanguageSettings.getMessage("view_distance_warning_message"),
                                                    LanguageSettings.getMessage("view_distance_warning_title"),
                                                    JOptionPane.WARNING_MESSAGE
                                            )
                                    );
                                }
                            } catch (Exception e) {
                                // Ignore parsing errors
                            }
                        }

                        // Sprawdź, czy występują błędy pamięci
                        if (logLine.contains("OutOfMemoryError") || logLine.contains("Can't keep up! Is the server overloaded?")) {
                            SwingUtilities.invokeLater(() -> {
                                output.append("\nWARNING: Memory issues detected. Try increasing memory allocation in launcher settings.\n");
                                output.setCaretPosition(output.getDocument().getLength());
                            });
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error reading process output", e);
                    SwingUtilities.invokeLater(() -> {
                        output.append("\nERROR: Could not read game output: " + e.getMessage() + "\n");
                        output.setCaretPosition(output.getDocument().getLength());
                    });
                }
            }).start();

            int exit = proc.waitFor();
            LOGGER.log(Level.INFO, "Minecraft exited kodem: " + exit);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Nieoczekiwany błąd podczas uruchamiania gry", e);
            JOptionPane.showMessageDialog(null,
                    "Błąd uruchamiania: " + e.getMessage(),
                    "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addLibs(File dir, StringBuilder cp) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) addLibs(f, cp);
            else if (f.getName().endsWith(".jar")) {
                cp.append(f.getAbsolutePath()).append(File.pathSeparator);
            }
        }
    }

    private String findShortestJavaPath() {
        String full = "C:\\Program Files\\Java\\jdk-24\\bin\\java.exe";
        String[] variants = {"java", full, full.replace("Program Files", "PROGRA~1"), "C:\\j\\bin\\java.exe"};
        for (String p : variants) {
            try {
                Process pbc = new ProcessBuilder(p, "-version").start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(pbc.getErrorStream()))) {
                    String l;
                    while ((l = r.readLine()) != null) {
                        if (l.contains("version")) return p;
                    }
                }
            } catch (IOException ignored) {}
        }
        return null;
    }

    private String selectJavaExecutable() {
        JFileChooser chooser = new JFileChooser("C:\\Program Files\\Java");
        chooser.setDialogTitle("Wybierz plik java.exe");
        chooser.setFileFilter(new FileNameExtensionFilter("Java Executable", "exe"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Launcher start");
        SwingUtilities.invokeLater(() -> new MinecraftLauncher().setVisible(true));
    }
}