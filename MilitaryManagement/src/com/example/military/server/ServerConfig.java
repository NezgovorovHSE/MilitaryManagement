package com.example.military.server;

import com.example.military.shared.Protocol;
import java.io.*;
import java.util.Properties;

public class ServerConfig {
    private static final String CONFIG_FILE = "server.properties";

    private int port;
    private String dbFile;
    private int maxThreads;
    private boolean logEnabled;

    public ServerConfig() {
        // Значения по умолчанию
        this.port = Protocol.DEFAULT_PORT;
        this.dbFile = Protocol.DB_FILE_NAME;
        this.maxThreads = 10;
        this.logEnabled = true;

        // Загружаем из файла, если существует
        loadFromFile();
    }

    private void loadFromFile() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            saveToFile(); // Создаём файл с настройками по умолчанию
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);

            this.port = Integer.parseInt(props.getProperty("port", String.valueOf(port)));
            this.dbFile = props.getProperty("db.file", dbFile);
            this.maxThreads = Integer.parseInt(props.getProperty("max.threads", String.valueOf(maxThreads)));
            this.logEnabled = Boolean.parseBoolean(props.getProperty("log.enabled", String.valueOf(logEnabled)));

            System.out.println("⚙️ Конфигурация загружена из " + CONFIG_FILE);

        } catch (IOException e) {
            System.err.println("⚠️ Ошибка загрузки конфигурации: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("port", String.valueOf(port));
            props.setProperty("db.file", dbFile);
            props.setProperty("max.threads", String.valueOf(maxThreads));
            props.setProperty("log.enabled", String.valueOf(logEnabled));

            props.store(fos, "Server Configuration");
            System.out.println("⚙️ Создан файл конфигурации: " + CONFIG_FILE);

        } catch (IOException e) {
            System.err.println("⚠️ Не удалось создать файл конфигурации: " + e.getMessage());
        }
    }

    // Геттеры и сеттеры
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDbFile() { return dbFile; }
    public void setDbFile(String dbFile) { this.dbFile = dbFile; }

    public int getMaxThreads() { return maxThreads; }
    public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }

    public boolean isLogEnabled() { return logEnabled; }
    public void setLogEnabled(boolean logEnabled) { this.logEnabled = logEnabled; }

    @Override
    public String toString() {
        return String.format(
                "ServerConfig{port=%d, dbFile='%s', maxThreads=%d, logEnabled=%s}",
                port, dbFile, maxThreads, logEnabled
        );
    }
}