package marcin0816.dev;

import java.io.IOException;
import java.util.logging.*;

public class LoggerUtil {
    private static volatile Logger logger;

    // Private constructor to prevent instantiation
    private LoggerUtil() {}

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
        // Set logging level
        logger.setLevel(Level.ALL);

        // Create a formatter for log messages
        SimpleFormatter formatter = new SimpleFormatter();

        // Add Console Handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        // Add File Handler for logging
        try {
            // 'true' parameter means append to existing log file
            FileHandler fileHandler = new FileHandler("minecraft_launcher.log", true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
        }

        // Disable parent handlers to prevent duplicate logging
        logger.setUseParentHandlers(false);
    }
}
