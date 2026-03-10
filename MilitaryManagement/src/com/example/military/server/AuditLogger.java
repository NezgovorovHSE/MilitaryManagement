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

    // Основной метод логирования с username (может быть null)
    public static void log(String username, String action, String details) {
        if (connection == null) return;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String userPart = (username != null) ? username : "system";
        String logLine = String.format("[%s] [%s] %s - %s", timestamp, userPart, action, details);

        // Запись в БД (сохраняем userId, если есть возможность получить)
        Integer userId = null;
        // Здесь можно добавить получение userId по username, если нужно, но для простоты оставим null
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

        // Запись в файл
        if (fileWriter != null) {
            fileWriter.println(logLine);
            fileWriter.flush();
        }

        // В консоль
        System.out.println("📝 " + logLine);
    }

    // Старый метод для совместимости (можно оставить, но лучше постепенно убрать)
    public static void log(Integer userId, String action, String details) {
        // Получить username по userId можно, если есть доступ к сервису, но для простоты оставляем как есть
        // Вызываем новый метод с null username (будет записано как system)
        log((String) null, action, details);
    }

    // Специализированные методы
    public static void logAdd(String username, MilitaryPerson person, int newId) {
        String type = getTypeString(person);
        String userPart = (username != null) ? username : "system";
        String details = String.format("(user: %s) ID: %d, Фамилия: %s, Тип: %s",
                userPart, newId, person.getLastName(), type);
        log(username, "ДОБАВЛЕНИЕ", details);
    }
    public static void logDelete(String username, MilitaryPerson person) {
        String type = getTypeString(person);
        String details = String.format("ID: %d, Фамилия: %s, Тип: %s",
                person.getId(), person.getLastName(), type);
        log(username, "УДАЛЕНИЕ", details);
    }

    public static void logLock(String username, MilitaryPerson person) {
        String type = getTypeString(person);
        String details = String.format("ID: %d, Фамилия: %s, Тип: %s",
                person.getId(), person.getLastName(), type);
        log(username, "БЛОКИРОВКА", details);
    }

    public static void logUnlock(String username, int recordId, String lastName, String type) {
        String details = String.format("ID: %d, Фамилия: %s, Тип: %s",
                recordId, lastName, type);
        log(username, "РАЗБЛОКИРОВКА", details);
    }

    public static void logUpdate(String username, MilitaryPerson oldPerson, MilitaryPerson newPerson) {
        String oldType = getTypeString(oldPerson);
        String newType = getTypeString(newPerson);

        StringBuilder details = new StringBuilder();
        details.append(String.format("ID: %d, Фамилия: %s",
                newPerson.getId(), newPerson.getLastName()));

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
