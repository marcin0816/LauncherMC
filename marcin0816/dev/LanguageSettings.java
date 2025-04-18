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
            case "username_label" -> currentLanguage == Language.POLISH
                    ? "Nazwa użytkownika:"
                    : "Username:";

            case "version_label" -> currentLanguage == Language.POLISH
                    ? "Wybierz wersję Minecrafta:"
                    : "Select Minecraft version:";

            case "custom_java_path" -> currentLanguage == Language.POLISH
                    ? "Utwórz własną ścieżkę do Java"
                    : "Set custom Java path";

            case "java_path_label" -> currentLanguage == Language.POLISH
                    ? "Ścieżka do Java:"
                    : "Java path:";

            case "download_version_button" -> currentLanguage == Language.POLISH
                    ? "Pobierz wersję"
                    : "Download Version";

            case "run_game_button" -> currentLanguage == Language.POLISH
                    ? "Uruchom grę"
                    : "Run Game";

            case "error_no_username" -> currentLanguage == Language.POLISH
                    ? "Proszę podać nazwę użytkownika!"
                    : "Please enter a username!";

            case "download_version_progress" -> currentLanguage == Language.POLISH
                    ? "Pobieranie wersji..."
                    : "Downloading version...";

            case "error_missing_jar" -> currentLanguage == Language.POLISH
                    ? "Plik JAR nie istnieje: "
                    : "JAR file not found: ";

            case "language_label" -> currentLanguage == Language.POLISH
                    ? "Wybierz język:"
                    : "Select language:";

            case "launcher_title" -> currentLanguage == Language.POLISH
                    ? "Minecraft Launcher - Nieoficjalny"
                    : "Minecraft Non-Premium Launcher";

            case "error_java_path" -> currentLanguage == Language.POLISH
                    ? "Nie można znaleźć prawidłowej ścieżki Java"
                    : "Cannot find a valid Java path";

            // New memory-related strings
            case "memory_allocation_label" -> currentLanguage == Language.POLISH
                    ? "Przydział pamięci RAM:"
                    : "Memory allocation:";

            case "memory_warning_title" -> currentLanguage == Language.POLISH
                    ? "Ostrzeżenie o pamięci"
                    : "Memory Warning";

            case "memory_warning_message" -> currentLanguage == Language.POLISH
                    ? "Przydzielasz dużo pamięci RAM. Może to spowodować problemy z systemem. Kontynuować?"
                    : "You are allocating a large amount of memory. This might cause system instability. Continue anyway?";

            case "view_distance_warning_title" -> currentLanguage == Language.POLISH
                    ? "Ostrzeżenie o odległości renderowania"
                    : "View Distance Warning";

            case "view_distance_warning_message" -> currentLanguage == Language.POLISH
                    ? "Duża odległość renderowania (>16) wymaga znacznej ilości pamięci. Zalecane minimum 4GB RAM."
                    : "High view distance (>16) requires significant memory. Recommended minimum is 4GB RAM.";

            default -> "Key not found: " + key;
        };
    }

    public static Language getCurrentLanguageEnum() {
        return currentLanguage;
    }
}