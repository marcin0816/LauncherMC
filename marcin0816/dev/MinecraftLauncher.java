package marcin0816.dev;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class MinecraftLauncher extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final String VERSIONS_URL = "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private static final Logger LOGGER = Logger.getLogger(MinecraftLauncher.class.getName());
    private final JButton actionButton;
    private final JTextField usernameField;
    private final JList<String> versionList;
    private final DefaultListModel<String> versionListModel;
    private JSONArray versionsArray;
    private final JProgressBar progressBar;
    private final JCheckBox customJavaPathCheckbox;
    private final JTextField customJavaPathField;

    public MinecraftLauncher() {
        setTitle("Minecraft Non-Premium Launcher");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));

        // Wybór języka
        panel.add(new JLabel(LanguageSettings.getMessage("language_label")));
        JComboBox<LanguageSettings.Language> languageComboBox = getLanguageJComboBox();
        panel.add(languageComboBox);

        // Pole nazwy użytkownika
        panel.add(new JLabel(LanguageSettings.getMessage("username_label")));
        usernameField = new JTextField();
        panel.add(usernameField);

        // Lista wyboru wersji
        panel.add(new JLabel(LanguageSettings.getMessage("version_label")));
        versionListModel = new DefaultListModel<>();
        versionList = new JList<>(versionListModel);
        versionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        versionList.addListSelectionListener(_ -> updateActionButton());

        JScrollPane versionScrollPane = new JScrollPane(versionList);
        versionScrollPane.setPreferredSize(new Dimension(200, 100));
        panel.add(versionScrollPane);

        // Checkbox i pole dla własnej ścieżki Java
        customJavaPathCheckbox = new JCheckBox(LanguageSettings.getMessage("custom_java_path"));
        customJavaPathCheckbox.addActionListener(_ -> updateJavaPathFieldState());
        panel.add(customJavaPathCheckbox);

        panel.add(new JLabel(LanguageSettings.getMessage("java_path_label")));
        customJavaPathField = new JTextField(System.getProperty("java.home") + "/bin/java");
        customJavaPathField.setEnabled(false);
        panel.add(customJavaPathField);

        // Przycisk akcji (pobierz lub uruchom)
        actionButton = new JButton(LanguageSettings.getMessage("download_version_button"));
        actionButton.setEnabled(false);
        actionButton.addActionListener(_ -> {
            String selectedVersion = versionList.getSelectedValue();
            if (selectedVersion != null && !selectedVersion.isEmpty()) {
                if (actionButton.getText().equals(LanguageSettings.getMessage("download_version_button"))) {
                    new Thread(() -> downloadSelectedVersion(selectedVersion)).start();
                } else if (actionButton.getText().equals(LanguageSettings.getMessage("run_game_button"))) {
                    String username = usernameField.getText();
                    if (!username.isEmpty()) {
                        new Thread(() -> launchGame(username)).start();
                    } else {
                        JOptionPane.showMessageDialog(null, LanguageSettings.getMessage("error_no_username"), "Błąd", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Dodanie komponentów do ramki
        add(panel, BorderLayout.CENTER);
        add(actionButton, BorderLayout.SOUTH);

        // Pasek postępu
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.NORTH);

        // Załadowanie dostępnych wersji Minecrafta
        loadAvailableVersions();
    }

    private JComboBox<LanguageSettings.Language> getLanguageJComboBox() {
        LanguageSettings.Language[] languages = {LanguageSettings.Language.ENGLISH, LanguageSettings.Language.POLISH};
        final JComboBox<LanguageSettings.Language> languageComboBox = new JComboBox<>(languages);
        languageComboBox.setSelectedItem(LanguageSettings.getCurrentLanguageEnum());
        languageComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                LanguageSettings.Language selectedLanguage = (LanguageSettings.Language) e.getItem();
                LanguageSettings.setLanguage(selectedLanguage);
                refreshUI();
            }
        });
        return languageComboBox;
    }

    private void refreshUI() {
        setTitle(LanguageSettings.getMessage("launcher_title"));
        ((JLabel) ((JPanel) getContentPane().getComponent(0)).getComponent(0)).setText(LanguageSettings.getMessage("language_label"));
        ((JLabel) ((JPanel) getContentPane().getComponent(0)).getComponent(2)).setText(LanguageSettings.getMessage("username_label"));
        ((JLabel) ((JPanel) getContentPane().getComponent(0)).getComponent(4)).setText(LanguageSettings.getMessage("version_label"));
        customJavaPathCheckbox.setText(LanguageSettings.getMessage("custom_java_path"));
        ((JLabel) ((JPanel) getContentPane().getComponent(0)).getComponent(7)).setText(LanguageSettings.getMessage("java_path_label"));
        updateActionButton();
    }

    private void updateActionButton() {
        String selectedVersion = versionList.getSelectedValue();
        if (selectedVersion != null && !selectedVersion.isEmpty()) {
            String userHome = System.getProperty("user.home");
            Path versionPath = Paths.get(userHome, "AppData", "Roaming", ".minecraft", "versions", selectedVersion);

            File versionDir = versionPath.toFile();
            if (versionDir.exists() && versionDir.isDirectory()) {
                actionButton.setText(LanguageSettings.getMessage("run_game_button"));
            } else {
                actionButton.setText(LanguageSettings.getMessage("download_version_button"));
            }
            actionButton.setEnabled(true);
        } else {
            actionButton.setEnabled(false);
        }
    }

    private void loadAvailableVersions() {
        try {
            String jsonResponse = downloadJson(VERSIONS_URL);
            JSONObject jsonObject = new JSONObject(jsonResponse);
            versionsArray = jsonObject.getJSONArray("versions");

            for (int i = 0; i < versionsArray.length(); i++) {
                JSONObject version = versionsArray.getJSONObject(i);
                String versionId = version.getString("id");
                String versionType = version.getString("type");

                if (versionType.equals("release")) { // Dodaj tylko stabilne wersje
                    versionListModel.addElement(versionId);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, LanguageSettings.getMessage("error_missing_jar") + e.getMessage(), e);
        }
    }

    private void updateJavaPathFieldState() {
        if (customJavaPathCheckbox.isSelected()) {
            customJavaPathField.setEnabled(true);
        } else {
            customJavaPathField.setEnabled(false);
            customJavaPathField.setText(System.getProperty("java.home") + "/bin/java");
        }
    }

    private void downloadSelectedVersion(String versionId) {
        try {
            progressBar.setValue(0);
            progressBar.setString(LanguageSettings.getMessage("download_version_progress"));

            JSONObject selectedVersion = null;
            for (int i = 0; i < versionsArray.length(); i++) {
                JSONObject version = versionsArray.getJSONObject(i);
                if (version.getString("id").equals(versionId)) {
                    selectedVersion = version;
                    break;
                }
            }

            if (selectedVersion != null) {
                String versionUrl = selectedVersion.getString("url");
                LOGGER.log(Level.INFO, "Pobieranie informacji o wersji z: " + versionUrl);
                JSONObject versionInfo = new JSONObject(downloadJson(versionUrl));

                // Zapisz plik wersji do katalogu index
                String userHome = System.getProperty("user.home");
                String indexDirPath = Paths.get(userHome, "AppData", "Roaming", ".minecraft", "assets", "indexes").toString();
                createParentDirectories(indexDirPath); // Tworzenie katalogu 'indexes'

                String versionJsonPath = Paths.get(indexDirPath, versionId + ".json").toString();
                createParentDirectories(Paths.get(versionJsonPath).getParent().toString()); // Tworzenie folderu nadrzędnego dla JSON

                try (FileWriter writer = new FileWriter(versionJsonPath)) {
                    writer.write(versionInfo.toString(4));
                }

                String clientUrl = versionInfo.getJSONObject("downloads").getJSONObject("client").getString("url");
                String jarFilePath = Paths.get(userHome, "AppData", "Roaming", ".minecraft", "versions", versionId, versionId + ".jar").toString();
                createParentDirectories(Paths.get(jarFilePath).getParent().toString());
                downloadJar(clientUrl, jarFilePath);
                LOGGER.log(Level.INFO, "Pobrano wersję: " + versionId);

                progressBar.setValue(50);
                progressBar.setString(LanguageSettings.getMessage("download_version_progress"));

                downloadLibraries(versionInfo);
                progressBar.setValue(75);

                LOGGER.log(Level.INFO, "Pobieranie assetów...");
                downloadAssets(versionInfo);
                progressBar.setValue(100);
                progressBar.setString(LanguageSettings.getMessage("download_version_progress") + " zakończone");

                updateActionButton();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Wystąpił błąd podczas pobierania wersji: " + versionId, e);
        }
    }

    private void downloadAssets(JSONObject versionInfo) throws IOException {
        JSONObject assetIndex = versionInfo.getJSONObject("assetIndex");
        String assetJsonUrl = assetIndex.getString("url");

        LOGGER.info("Pobieranie indeksu assetów z: " + assetJsonUrl);

        String userHome = System.getProperty("user.home");
        String baseAssetDir = Paths.get(userHome, "AppData", "Roaming", ".minecraft", "assets").toString();
        String indexesDir = Paths.get(baseAssetDir, "indexes").toString();
        createParentDirectories(indexesDir);

        Path indexPath = Paths.get(indexesDir, assetIndex.getString("id") + ".json");
        if (!Files.exists(indexPath)) {
            downloadJar(assetJsonUrl, indexPath.toString());
            LOGGER.info("Zapisano indeks assetów w: " + indexPath);
        } else {
            LOGGER.info("Indeks assetów już istnieje: " + indexPath);
        }

        JSONObject assetJson = new JSONObject(new String(Files.readAllBytes(indexPath)));
        JSONObject objects = assetJson.getJSONObject("objects");

        for (String assetKey : objects.keySet()) {
            JSONObject assetObj = objects.getJSONObject(assetKey);
            String hash = assetObj.getString("hash");
            String assetDownloadUrl = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;

            Path targetPath = Paths.get(baseAssetDir, assetKey);
            createParentDirectories(targetPath.getParent().toString());

            if (!Files.exists(targetPath)) {
                LOGGER.info("Pobieranie assetu: " + assetKey + " z " + assetDownloadUrl);
                downloadJar(assetDownloadUrl, targetPath.toString());
                LOGGER.info("Zapisano asset w: " + targetPath);
            } else {
                LOGGER.info("Asset już istnieje: " + targetPath);
            }
        }
    }

    private void downloadLibraries(JSONObject versionInfo) throws IOException {
        JSONArray libraries = versionInfo.getJSONArray("libraries");

        for (int i = 0; i < libraries.length(); i++) {
            JSONObject library = libraries.getJSONObject(i);
            if (library.has("downloads") && library.getJSONObject("downloads").has("artifact")) {
                JSONObject artifact = library.getJSONObject("downloads").getJSONObject("artifact");
                String libraryUrl = artifact.getString("url");
                String userHome = System.getProperty("user.home");
                String libraryPath = Paths.get(userHome, "AppData", "Roaming", ".minecraft", "libraries", artifact.getString("path")).toString();

                createParentDirectories(Paths.get(libraryPath).getParent().toString());

                File libraryFile = new File(libraryPath);
                if (!libraryFile.exists()) {
                    HttpURLConnection connection = (HttpURLConnection) URI.create(libraryUrl).toURL().openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (InputStream in = connection.getInputStream();
                             FileOutputStream fileOutputStream = new FileOutputStream(libraryPath)) {
                            byte[] dataBuffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = in.read(dataBuffer)) != -1) {
                                fileOutputStream.write(dataBuffer, 0, bytesRead);
                            }
                        }
                        LOGGER.log(Level.INFO, "Pobrano bibliotekę: " + libraryPath);
                    } else {
                        LOGGER.log(Level.SEVERE, "Błąd podczas pobierania biblioteki: " + libraryUrl + ", kod odpowiedzi: " + responseCode);
                    }
                } else {
                    LOGGER.log(Level.INFO, "Biblioteka już istnieje: " + libraryPath);
                }
            }
        }
    }

    private void createParentDirectories(String filePath) {
        File parentDir = new File(filePath);
        if (!parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();
            if (dirsCreated) {
                LOGGER.log(Level.INFO, "Utworzono brakujące foldery dla: " + filePath);
            } else {
                LOGGER.log(Level.WARNING, "Nie udało się utworzyć folderów dla: " + filePath);
            }
        }
    }

    private void downloadJar(String urlString, String saveDir) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(saveDir)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                LOGGER.log(Level.INFO, "Pobrano plik: " + saveDir);
            }
        } else {
            throw new IOException("Błąd podczas pobierania pliku: " + urlString + ", kod odpowiedzi: " + responseCode);
        }
    }

    private String downloadJson(String urlString) throws IOException {
        URI uri = URI.create(urlString);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.connect();

        if (connection.getResponseCode() != 200) {
            throw new IOException("Failed to fetch data: " + connection.getResponseCode());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            return jsonBuilder.toString();
        }
    }
    private void addLibrariesToClasspath(File dir, StringBuilder classpath) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addLibrariesToClasspath(file, classpath);
                } else if (file.getName().endsWith(".jar")) {
                    classpath.append(file.getAbsolutePath()).append(File.pathSeparator);
                }
            }
        }
    }

    private void launchGame(String username) {
        try {
            // Ustal ścieżkę do Javy
            String javaPath = customJavaPathCheckbox.isSelected() ? customJavaPathField.getText() : System.getProperty("java.home") + "/bin/java";
            String versionId = versionList.getSelectedValue();
            String userHome = System.getProperty("user.home");
            String gameDir = Paths.get(userHome, "AppData", "Roaming", ".minecraft").toString();
            String versionJar = Paths.get(gameDir, "versions", versionId, versionId + ".jar").toString();

            // Sprawdź, czy plik JAR istnieje
            File jarFile = new File(versionJar);
            if (!jarFile.exists()) {
                JOptionPane.showMessageDialog(null, LanguageSettings.getMessage("error_missing_jar") + versionJar, "Błąd", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Przygotuj classpath
            File librariesDir = new File(gameDir, "libraries");
            StringBuilder classpath = new StringBuilder();
            addLibrariesToClasspath(librariesDir, classpath);
            classpath.append(versionJar);

            LOGGER.log(Level.INFO, "Uruchamianie Minecrafta z następującymi argumentami:");
            LOGGER.log(Level.INFO, "Java Path: " + javaPath);
            LOGGER.log(Level.INFO, "Classpath: " + classpath);

            // Budowanie procesu uruchomienia gry
            ProcessBuilder processBuilder = new ProcessBuilder(
                    javaPath,
                    "-Xmx1024M", // Maksymalny przydział pamięci
                    "-Xms512M",  // Minimalny przydział pamięci
                    "-cp", classpath.toString(),
                    "net.minecraft.client.main.Main",
                    "--username", username,
                    "--version", versionList.getSelectedValue(),
                    "--gameDir", gameDir,
                    "--assetsDir", gameDir + "/assets",
                    "--uuid", "0",
                    "--accessToken", "dummy_access_token"
            );

            // Start procesu
            Process process = processBuilder.start();

            // Konsola wyjścia gry
            JFrame consoleFrame = new JFrame("Konsola gry");
            consoleFrame.setSize(600, 400);
            consoleFrame.setLocationRelativeTo(null);
            consoleFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JTextArea consoleOutput = new JTextArea();
            consoleOutput.setEditable(false);
            consoleFrame.add(new JScrollPane(consoleOutput));
            consoleFrame.setVisible(true);

            // Odczytywanie wyjścia procesu w osobnych wątkach
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        consoleOutput.append(line + "\n");
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Błąd podczas odczytywania wyjścia procesu.", e);
                }
            }).start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        consoleOutput.append("Błąd: " + line + "\n");
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Błąd podczas odczytywania błędów procesu.", e);
                }
            }).start();

            // Oczekiwanie na zakończenie procesu
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Wystąpił błąd podczas uruchamiania gry.", e);
        }
    }

    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Program uruchomiony");
        SwingUtilities.invokeLater(() -> new MinecraftLauncher().setVisible(true));
    }
}
