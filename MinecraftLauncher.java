package LuncherMC_Dev.LuncherMC;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public class MinecraftLauncher extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String VERSIONS_URL = "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private JButton actionButton;
    private JTextField usernameField;
    private JList<String> versionList;
    private DefaultListModel<String> versionListModel;
    private JSONArray versionsArray;
    private JProgressBar progressBar;
    private JCheckBox customJavaPathCheckbox;
    private JTextField customJavaPathField;

    public MinecraftLauncher() {
        setTitle("Minecraft Non-Premium Launcher");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));

        // Username field
        panel.add(new JLabel("Nazwa użytkownika:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        // Version selection list
        panel.add(new JLabel("Wybierz wersję Minecrafta:"));
        versionListModel = new DefaultListModel<>();
        versionList = new JList<>(versionListModel);
        versionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        versionList.addListSelectionListener(e -> updateActionButton());

        JScrollPane versionScrollPane = new JScrollPane(versionList);
        versionScrollPane.setPreferredSize(new Dimension(200, 100));
        panel.add(versionScrollPane);

        // Custom Java Path checkbox and field
        customJavaPathCheckbox = new JCheckBox("Utwórz własną ścieżkę do Java");
        customJavaPathCheckbox.addActionListener(e -> updateJavaPathFieldState());
        panel.add(customJavaPathCheckbox);

        panel.add(new JLabel("Ścieżka do Java:"));
        customJavaPathField = new JTextField(System.getProperty("java.home") + "/bin/java");
        customJavaPathField.setEnabled(false);
        panel.add(customJavaPathField);

        // Action button (either download or play)
        actionButton = new JButton("Pobierz wersję");
        actionButton.setEnabled(false);
        actionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedVersion = versionList.getSelectedValue();
                if (selectedVersion != null && !selectedVersion.isEmpty()) {
                    if (actionButton.getText().equals("Pobierz wersję")) {
                        new Thread(() -> downloadSelectedVersion(selectedVersion)).start();
                    } else if (actionButton.getText().equals("Uruchom grę")) {
                        String username = usernameField.getText();
                        if (!username.isEmpty()) {
                            new Thread(() -> launchGame(username)).start();
                        } else {
                            JOptionPane.showMessageDialog(null, "Proszę podać nazwę użytkownika!", "Błąd", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        // Add components to frame
        add(panel, BorderLayout.CENTER);
        add(actionButton, BorderLayout.SOUTH);

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.NORTH);

        // Load available Minecraft versions
        loadAvailableVersions();
    }


    private void log(String message) {
        System.out.println(message);
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

                if (versionType.equals("release")) { // Only add release versions
                    versionListModel.addElement(versionId);
                }
            }
        } catch (IOException e) {
            log("Wystąpił błąd podczas pobierania dostępnych wersji: " + e.getMessage());
        }
    }

    private void updateActionButton() {
        String selectedVersion = versionList.getSelectedValue();
        if (selectedVersion != null && !selectedVersion.isEmpty()) {
            String versionPath = "C:/Users/MarciNN/AppData/Roaming/.minecraft/versions/" + selectedVersion;
            File versionDir = new File(versionPath);
            if (versionDir.exists() && versionDir.isDirectory()) {
                actionButton.setText("Uruchom grę");
            } else {
                actionButton.setText("Pobierz wersję");
            }
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
            customJavaPathField.setText(System.getProperty("java.home") + "/bin/java");
        }
    }

    private void downloadSelectedVersion(String versionId) {
        try {
            progressBar.setValue(0);
            progressBar.setString("Pobieranie wersji...");

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
                log("Pobieranie informacji o wersji z: " + versionUrl);
                JSONObject versionInfo = new JSONObject(downloadJson(versionUrl));

                String clientUrl = versionInfo.getJSONObject("downloads").getJSONObject("client").getString("url");
                String jarFilePath = "C:/Users/MarciNN/AppData/Roaming/.minecraft/versions/" + versionId + "/" + versionId + ".jar";
                createParentDirectories(jarFilePath);
                downloadJar(clientUrl, jarFilePath);
                log("Pobrano wersję: " + versionId);

                progressBar.setValue(50);
                progressBar.setString("Pobieranie bibliotek...");

                downloadLibraries(versionInfo);
                progressBar.setValue(75);

                log("Pobieranie assetów...");
                downloadAssets(versionInfo);
                progressBar.setValue(100);
                progressBar.setString("Pobieranie zakończone");

                updateActionButton();
            }
        } catch (IOException e) {
            log("Wystąpił błąd podczas pobierania wersji: " + e.getMessage());
        }
    }

    private void downloadAssets(JSONObject versionInfo) throws IOException {
        JSONObject assetIndex = versionInfo.getJSONObject("assetIndex");
        String assetJsonUrl = assetIndex.getString("url");

        log("Pobieranie assetów z: " + assetJsonUrl);

        String baseAssetDir = "C:/Users/MarciNN/AppData/Roaming/.minecraft/assets/";
        String indexesDir = baseAssetDir + "indexes/";
        createParentDirectories(indexesDir);

        // Pobieranie pliku indeksu assetów
        String indexPath = indexesDir + assetIndex.getString("id") + ".json";
        createParentDirectories(indexPath); // Make sure the directory exists before saving the index file
        downloadJar(assetJsonUrl, indexPath);

        JSONObject assetJson = new JSONObject(downloadJson(assetJsonUrl));
        JSONObject objects = assetJson.getJSONObject("objects");

        for (String assetKey : objects.keySet()) {
            JSONObject assetObj = objects.getJSONObject(assetKey);
            String hash = assetObj.getString("hash");
            String assetDownloadUrl = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
            String targetPath = baseAssetDir + assetKey; // Keep the exact path from assetKey
            File targetFile = new File(targetPath);

            createParentDirectories(targetPath);
            if (!targetFile.exists()) {
                log("Pobieranie zasobu: " + assetDownloadUrl);
                downloadJar(assetDownloadUrl, targetPath);
                log("Zapisano asset w: " + targetPath);
            } else {
                log("Asset już istnieje: " + targetPath);
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
                String libraryPath = "C:/Users/MarciNN/AppData/Roaming/.minecraft/libraries/" + artifact.getString("path");

                createParentDirectories(libraryPath);

                File libraryFile = new File(libraryPath);
                if (!libraryFile.exists()) {
                    downloadJar(libraryUrl, libraryPath);
                    log("Pobrano bibliotekę: " + libraryPath);
                } else {
                    log("Biblioteka już istnieje: " + libraryPath);
                }
            }
        }
    }

    private void createParentDirectories(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();
            if (dirsCreated) {
                log("Utworzono brakujące foldery dla: " + filePath);
            } else {
                log("Nie udało się utworzyć folderów dla: " + filePath);
            }
        }
    }

    private void downloadJar(String urlString, String saveDir) throws IOException {
        URL website = URI.create(urlString).toURL();
        try (InputStream in = website.openStream();
             FileOutputStream out = new FileOutputStream(new File(saveDir))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            log("Pobrano: " + saveDir);
        }
    }

    private String downloadJson(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.connect();

        if (connection.getResponseCode() != 200) {
            throw new IOException("Nie udało się pobrać danych: " + connection.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        reader.close();
        return jsonBuilder.toString();
    }

    private void launchGame(String username) {
        String javaPath = customJavaPathCheckbox.isSelected() ? customJavaPathField.getText() : System.getProperty("java.home") + "/bin/java";
        String minecraftJar = "C:/Users/MarciNN/AppData/Roaming/.minecraft/versions/" + versionList.getSelectedValue() + "/" + versionList.getSelectedValue() + ".jar";
        String gameDir = "C:/Users/MarciNN/AppData/Roaming/.minecraft";

        File jarFile = new File(minecraftJar);
        if (!jarFile.exists()) {
            JOptionPane.showMessageDialog(null, "Plik JAR nie istnieje: " + minecraftJar, "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File assetsDir = new File(gameDir + "/assets");
        if (!assetsDir.exists()) {
            JOptionPane.showMessageDialog(null, "Folder assets nie istnieje: " + assetsDir.getPath(), "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File librariesDir = new File(gameDir + "/libraries");
        StringBuilder classpath = new StringBuilder();
        addLibrariesToClasspath(librariesDir, classpath);
        classpath.append(minecraftJar);

        log("Classpath: " + classpath.toString()); // Added for debugging

        ProcessBuilder processBuilder = new ProcessBuilder(
            javaPath,
            "-Xmx1024M",  // Reduced memory size to handle potential memory issues
            "-Xms512M",
            "-Djava.library.path=" + gameDir + "/natives",
            "-cp", classpath.toString(),
            "net.minecraft.client.main.Main",
            "--username", username,
            "--version", versionList.getSelectedValue(),
            "--gameDir", gameDir,
            "--assetsDir", gameDir + "/assets",
            "--accessToken", "dummy_access_token"
        );

        log("Uruchamianie Minecrafta z następującymi argumentami:");
        log("Java Path: " + javaPath);
        log("Game Directory: " + gameDir);
        log("Arguments: " + String.join(" ", processBuilder.command()));

        try {
            Process process = processBuilder.start();
            JFrame consoleFrame = new JFrame("Konsola gry");
            consoleFrame.setSize(600, 400);
            consoleFrame.setLocationRelativeTo(null);
            consoleFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JTextArea consoleOutput = new JTextArea();
            consoleOutput.setEditable(false);
            consoleFrame.add(new JScrollPane(consoleOutput));
            consoleFrame.setVisible(true);

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        consoleOutput.append("Błąd: " + line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        consoleOutput.append(line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            process.waitFor();
            log("Minecraft uruchomiony.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            log("Wystąpił błąd podczas uruchamiania Minecrafta.");
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

    public static void main(String[] args) {
        try {
            System.out.println("Program uruchomiony");
            SwingUtilities.invokeLater(() -> new MinecraftLauncher().setVisible(true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
