package com.example.military.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {
    private static Connection connection;
    private static PrintWriter fileWriter;
    private static final String LOG_FILE = "audit.log";

    static {
        try {
            fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true));
        } catch (IOException e) {
            System.err.println("Не удалось создать файл логов: " + e.getMessage());
        }
    }

    public static void init(Connection conn) {
        connection = conn;
    }

    public static void log(Integer userId, String action, String details) {
        if (connection == null) return;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String userInfo = (userId != null) ? "user:" + userId : "system";
        String logLine = String.format("[%s] [%s] %s - %s", timestamp, userInfo, action, details);

        String sql = "INSERT INTO audit_log (user_id, action, details) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            if (userId != null) {
                pstmt.setInt(1, userId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setString(2, action);
            pstmt.setString(3, details);
            pstmt.executeUpdate();

            System.out.println("📝 LOG: [" + (userId != null ? userId : "SYSTEM") + "] " + action + " - " + details);

        } catch (SQLException e) {
            System.err.println("Ошибка записи в аудит БД: " + e.getMessage());
        }
        // Запись в файл
        if (fileWriter != null) {
            fileWriter.println(logLine);
            fileWriter.flush();
        }

        // В консоль
        System.out.println("📝 " + logLine);
    }

    public static void logLogin(Integer userId, String username, boolean success) {
        String status = success ? "УСПЕШНО" : "НЕУДАЧНО";
        log(userId, "ВХОД", "Пользователь: " + username + " - " + status);
    }

    public static void logAction(Integer userId, String action, String target) {
        log(userId, action, target);
    }

    public static void close() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}
