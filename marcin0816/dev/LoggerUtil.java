package marcin0816.dev;

import java.io.IOException;
import java.util.logging.*;

public class LoggerUtil {

    private static volatile Logger logger;

    private LoggerUtil() {
        // Konstruktor prywatny, aby zapobiec tworzeniu instancji klasy
    }

    public static Logger getLogger() {
        if (logger == null) {
            synchronized (LoggerUtil.class) {
                if (logger == null) {
                    logger = Logger.getLogger("MinecraftLauncherLogger");
                    setupLogger();
                }
            }
        }
        return logger;
    }

    private static void setupLogger() {
        logger.setLevel(Level.ALL);

        // Ustaw format logów
        SimpleFormatter formatter = new SimpleFormatter();

        // Dodaj Handler do logowania na konsolę
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        // Dodaj Handler do logowania do pliku
        try {
            FileHandler fileHandler = new FileHandler("minecraft_launcher.log", true); // `true` dołącz do istniejącego pliku
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Nie udało się utworzyć pliku logów: " + e.getMessage());
        }

        // Wyłącz domyślny Handler, aby zapobiec powieleniu logów
        logger.setUseParentHandlers(false);
    }
}
