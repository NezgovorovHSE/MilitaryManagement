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

    public static void logUpdate(User user, MilitaryPerson oldPerson, MilitaryPerson newPerson) {
        String oldType = getTypeString(oldPerson);
        String newType = getTypeString(newPerson);

        StringBuilder message = new StringBuilder();
        message.append("ID:").append(oldPerson.getId())
                .append(", Фамилия:").append(newPerson.getLastName());

        if (!oldType.equals(newType)) {
            message.append(", Тип: ").append(oldType).append(" -> ").append(newType);
        }

        log(user.getId(), "ОБНОВЛЕНИЕ", message.toString());
    }

    public static void logAdd(User user, MilitaryPerson person, int newId) {
        String message = String.format("ID:%d, Фамилия:%s, Тип:%s",
                newId, person.getLastName(), getTypeString(person));
        log(user.getId(), "ДОБАВЛЕНИЕ", message);
    }

    public static void logDelete(User user, MilitaryPerson person) {
        String message = String.format("ID:%d, Фамилия:%s, Тип:%s",
                person.getId(), person.getLastName(), getTypeString(person));
        log(user.getId(), "УДАЛЕНИЕ", message);
    }

    public static void logLock(User user, MilitaryPerson person) {
        String message = String.format("ID:%d, Фамилия:%s",
                person.getId(), person.getLastName());
        log(user.getId(), "БЛОКИРОВКА", message);
    }

    public static void logUnlock(User user, int recordId, String lastName) {
        String message = String.format("ID:%d, Фамилия:%s", recordId, lastName);
        log(user.getId(), "РАЗБЛОКИРОВКА", message);
    }

    private static String getTypeString(MilitaryPerson p) {
        if (p instanceof MilitaryCommand) return "Командование";
        if (p instanceof MilitaryContract) return "Контрактники";
        if (p instanceof MilitaryAwarded) return "Награждённые";
        return "Военнослужащие";
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
