package com.example.military.server;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final boolean enabled;
    private PrintWriter fileWriter;

    public ServerLogger(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            try {
                fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true));
            } catch (IOException e) {
                System.err.println("❌ Не удалось создать лог-файл: " + e.getMessage());
            }
        }
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = String.format("[%s] %s", timestamp, message);

        // В консоль всегда выводим
        System.out.println(logMessage);

        // В файл, если включено
        if (enabled && fileWriter != null) {
            fileWriter.println(logMessage);
            fileWriter.flush();
        }
    }

    public void logClientAction(String clientInfo, String action) {
        log(String.format("Клиент [%s]: %s", clientInfo, action));
    }

    public void error(String message, Exception e) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = String.format("[%s] ❌ ОШИБКА: %s - %s",
                timestamp, message, e.getMessage());

        System.err.println(logMessage);

        if (enabled && fileWriter != null) {
            fileWriter.println(logMessage);
            e.printStackTrace(fileWriter);
            fileWriter.flush();
        }
    }

    public void close() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}