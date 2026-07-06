package com.example.military.server;

import com.example.military.model.MilitaryPerson;
import com.example.military.shared.JsonConverter;
import com.example.military.shared.Protocol;
import java.util.List;

public class ResponseBuilder {

    /**
     * Создаёт успешный ответ с данными
     */
    public static String success(Object data) {
        if (data instanceof List) {
            return JsonConverter.createResponse(Protocol.STATUS_OK, null, data);
        } else if (data instanceof MilitaryPerson) {
            return JsonConverter.createResponse(Protocol.STATUS_OK, null, data);
        } else if (data instanceof Integer) {
            return JsonConverter.createResponse(Protocol.STATUS_OK, null, data);
        } else {
            return JsonConverter.createResponse(Protocol.STATUS_OK, (String) data, null);
        }
    }

    /**
     * Создаёт успешный ответ с сообщением
     */
    public static String successWithMessage(String message) {
        return JsonConverter.createResponse(Protocol.STATUS_OK, message, null);
    }

    /**
     * Создаёт ответ с ошибкой
     */
    public static String error(String message) {
        return JsonConverter.createResponse(Protocol.STATUS_ERROR, message, null);
    }

    /**
     * Создаёт ответ "не найдено"
     */
    public static String notFound(String message) {
        return JsonConverter.createResponse(Protocol.STATUS_NOT_FOUND, message, null);
    }

    /**
     * Создаёт ответ со списком военнослужащих
     */
    public static String personList(List<MilitaryPerson> list) {
        return JsonConverter.createResponse(Protocol.STATUS_OK, null, list);
    }

    /**
     * Создаёт ответ с одним военнослужащим
     */
    public static String singlePerson(MilitaryPerson person) {
        return JsonConverter.createResponse(Protocol.STATUS_OK, null, person);
    }

    /**
     * Создаёт ответ с количеством записей
     */
    public static String countResponse(int count) {
        return JsonConverter.createResponse(Protocol.STATUS_OK, null, count);
    }
}