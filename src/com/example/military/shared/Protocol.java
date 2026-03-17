package com.example.military.shared;

public class Protocol {

    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_LOGOUT = "LOGOUT";
    public static final String CMD_ADD = "ADD";
    public static final String CMD_UPDATE = "UPDATE";
    public static final String CMD_GET_ALL = "GET_ALL";
    public static final String CMD_GET_BY_ID = "GET_BY_ID";
    public static final String CMD_DELETE = "DELETE";
    public static final String CMD_CLEAR = "CLEAR";
    public static final String CMD_COUNT = "COUNT";
    public static final String CMD_LOG_EXPORT = "LOG_EXPORT";
    public static final String CMD_LOG_IMPORT = "LOG_IMPORT";
    public static final String CMD_IMPORT = "IMPORT";

    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_NOT_FOUND = "NOT_FOUND";

    public static final String FIELD_COMMAND = "command";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_COUNT = "count";

    public static final String TYPE_BASE = "BASE";
    public static final String TYPE_COMMAND = "COMMAND";
    public static final String TYPE_CONTRACT = "CONTRACT";
    public static final String TYPE_AWARDED = "AWARDED";

    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_HOST = "localhost"; //Замените на серверный IP
    public static final String DB_FILE_NAME = "military.db";

    private Protocol() {}
}