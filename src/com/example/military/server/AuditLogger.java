package com.example.military.server;

import com.example.military.model.*;

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

    public static void log(String username, String action, String details) {
        if (connection == null) return;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String userPart = (username != null) ? username : "system";
        String logLine = String.format("[%s] [%s] %s - %s", timestamp, userPart, action, details);

        Integer userId = null;
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
        } catch (SQLException e) {
            System.err.println("Ошибка записи в аудит БД: " + e.getMessage());
        }

        if (fileWriter != null) {
            fileWriter.println(logLine);
            fileWriter.flush();
        }

        System.out.println("📝 " + logLine);
    }

    public static void log(Integer userId, String action, String details) {
        log((String) null, action, details);
    }

    public static void logAdd(String username, MilitaryPerson person, int newId) {
        String type = getTypeString(person);
        String details = String.format("ID: %d, Фамилия: %s, Тип: %s", newId, person.getLastName(), type);
        log(username, "ДОБАВЛЕНИЕ", details);
    }

    public static void logDelete(String username, MilitaryPerson person) {
        String type = getTypeString(person);
        String details = String.format("ID: %d, Фамилия: %s, Тип: %s", person.getId(), person.getLastName(), type);
        log(username, "УДАЛЕНИЕ", details);
    }

    public static void logLock(String username, MilitaryPerson person) {
        String type = getTypeString(person);
        String details = String.format("ID: %d, Фамилия: %s, Тип: %s", person.getId(), person.getLastName(), type);
        log(username, "БЛОКИРОВКА", details);
    }

    public static void logUnlock(String username, int recordId, String lastName, String type) {
        String details = String.format("ID: %d, Фамилия: %s, Тип: %s", recordId, lastName, type);
        log(username, "РАЗБЛОКИРОВКА", details);
    }

    public static void logUpdate(String username, MilitaryPerson oldPerson, MilitaryPerson newPerson) {
        String oldType = getTypeString(oldPerson);
        String newType = getTypeString(newPerson);

        StringBuilder details = new StringBuilder();
        details.append(String.format("ID: %d, Фамилия: %s", newPerson.getId(), newPerson.getLastName()));

        if (!oldType.equals(newType)) {
            details.append(String.format(", Тип: %s -> %s", oldType, newType));
        }
        log(username, "ОБНОВЛЕНИЕ", details.toString());
    }

    public static void logLogin(String username, String ip, boolean success) {
        String status = success ? "УСПЕШНО" : "НЕУДАЧНО";
        String details = String.format("IP: %s - %s", ip, status);
        log(username, "ВХОД", details);
    }

    public static void logLogout(String username) {
        log(username, "ВЫХОД", "");
    }

    public static void logImport(String username, String filename, int count) {
        String details = String.format("Из файла: %s, добавлено: %d записей", filename, count);
        log(username, "ВЫГРУЗКА", details);
    }

    public static void logExport(String username, String filename, int count) {
        String details = String.format("В файл: %s, записей: %d", filename, count);
        log(username, "СОХРАНЕНИЕ", details);
    }

    public static String getTypeString(MilitaryPerson p) {
        if (p instanceof MilitaryCommand) return "Командование";
        if (p instanceof MilitaryContract) return "Контрактники";
        if (p instanceof MilitaryAwarded) return "Награждённые";
        return "Военнослужащие";
    }

    public static void close() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}