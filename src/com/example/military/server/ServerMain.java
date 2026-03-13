package com.example.military.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static ServerConfig config;
    private static DatabaseManager dbManager;
    private static MilitaryService service;
    private static ServerLogger logger;
    private static ExecutorService threadPool;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   СЕРВЕР УПРАВЛЕНИЯ ВОЕННЫМ СОСТАВОМ");
        System.out.println("========================================");

        initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(ServerMain::shutdown));
        startServer();
    }

    private static void initialize() {
        System.out.println("\n🔧 Инициализация сервера...");

        config = new ServerConfig();
        System.out.println("📋 Конфигурация: " + config);

        logger = new ServerLogger(config.isLogEnabled());
        logger.log("========================================");
        logger.log("ЗАПУСК СЕРВЕРА");
        logger.log("========================================");

        dbManager = new DatabaseManager(config.getDbFile());
        AuditLogger.init(dbManager.getConnection());
        service = new MilitaryService(dbManager, logger);

        threadPool = Executors.newFixedThreadPool(config.getMaxThreads());

        logger.log("✅ Инициализация завершена");
    }

    private static void startServer() {
        System.out.println("\n🚀 Запуск сервера на порту " + config.getPort() + "...");
        logger.log("🚀 Запуск сервера на порту " + config.getPort());

        try (ServerSocket serverSocket = new ServerSocket(config.getPort())) {
            System.out.println("✅ Сервер запущен и ожидает подключения...");
            System.out.println("   Нажмите Ctrl+C для остановки сервера\n");

            logger.log("✅ Сервер запущен, ожидание подключений...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, service, logger);
                    threadPool.execute(handler);
                } catch (IOException e) {
                    if (running) {
                        logger.error("Ошибка при принятии подключения", e);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка запуска сервера: " + e.getMessage());
            logger.error("Ошибка запуска сервера", e);
        } finally {
            shutdown();
        }
    }

    private static void shutdown() {
        if (!running) return;

        System.out.println("\n🛑 Остановка сервера...");
        logger.log("🛑 Остановка сервера...");

        running = false;

        if (threadPool != null) {
            threadPool.shutdown();
            logger.log("✅ Пул потоков остановлен");
        }

        if (dbManager != null) {
            dbManager.close();
        }

        AuditLogger.close();

        if (logger != null) {
            logger.log("✅ Сервер остановлен");
            logger.close();
        }

        System.out.println("✅ Сервер остановлен");
    }
}