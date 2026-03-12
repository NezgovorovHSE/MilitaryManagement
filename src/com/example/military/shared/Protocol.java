package com.example.military.shared;

/**
 * Единый протокол обмена данными между клиентом и сервером
 */
public class Protocol {

    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_LOGOUT = "LOGOUT";

    // Команды от клиента к серверу
    public static final String CMD_ADD = "ADD";                 // Добавить военнослужащего
    public static final String CMD_UPDATE = "UPDATE";
    public static final String CMD_GET_ALL = "GET_ALL";        // Получить всех
    public static final String CMD_GET_BY_ID = "GET_BY_ID";    // Получить по индексу
    public static final String CMD_DELETE = "DELETE";          // Удалить
    public static final String CMD_CLEAR = "CLEAR";            // Очистить список
    public static final String CMD_COUNT = "COUNT";
    public static final String CMD_LOG_EXPORT = "LOG_EXPORT";
    public static final String CMD_LOG_IMPORT = "LOG_IMPORT";// Получить количество

    // Ответы сервера
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_NOT_FOUND = "NOT_FOUND";

    // Поля JSON
    public static final String FIELD_COMMAND = "command";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_COUNT = "count";

    // Типы военнослужащих (для JSON)
    public static final String TYPE_BASE = "BASE";
    public static final String TYPE_COMMAND = "COMMAND";
    public static final String TYPE_CONTRACT = "CONTRACT";
    public static final String TYPE_AWARDED = "AWARDED";

    // Настройки сервера по умолчанию
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_HOST = "192.168.31.170";
    public static final String DB_FILE_NAME = "military.db";

    public static final String CMD_IMPORT = "IMPORT";

    /**
     * Приватный конструктор, чтобы нельзя было создать экземпляр
     */
    private Protocol() {
        // только статические константы
    }
}