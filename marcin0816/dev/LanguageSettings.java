package marcin0816.dev;

public class LanguageSettings {
    public enum Language {
        POLISH, ENGLISH
    }

    private static Language currentLanguage = Language.ENGLISH;

    public static void setLanguage(Language language) {
        currentLanguage = language;
    }

    public static String getMessage(String key) {
        return switch (key) {
            case "username_label" -> currentLanguage == Language.POLISH ? "Nazwa użytkownika:" : "Username:";
            case "version_label" -> currentLanguage == Language.POLISH ? "Wybierz wersję Minecrafta:" : "Select Minecraft version:";
            case "custom_java_path" -> currentLanguage == Language.POLISH ? "Utwórz własną ścieżkę do Java" : "Set custom Java path";
            case "java_path_label" -> currentLanguage == Language.POLISH ? "Ścieżka do Java:" : "Java path:";
            case "download_version_button" -> currentLanguage == Language.POLISH ? "Pobierz wersję" : "Download Version";
            case "run_game_button" -> currentLanguage == Language.POLISH ? "Uruchom grę" : "Run Game";
            case "error_no_username" -> currentLanguage == Language.POLISH ? "Proszę podać nazwę użytkownika!" : "Please enter a username!";
            case "download_version_progress" -> currentLanguage == Language.POLISH ? "Pobieranie wersji..." : "Downloading version...";
            case "error_missing_jar" -> currentLanguage == Language.POLISH ? "Plik JAR nie istnieje: " : "JAR file not found: ";
            case "language_label" -> currentLanguage == Language.POLISH ? "Wybierz język:" : "Select language:";
            case "launcher_title" -> currentLanguage == Language.POLISH ? "Minecraft Launcher - Nieoficjalny" : "Minecraft Non-Premium Launcher";
            default -> "Key not found: " + key;
        };
    }

    public static Language getCurrentLanguageEnum() {
        return currentLanguage;
    }
}