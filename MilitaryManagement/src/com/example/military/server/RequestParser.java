package com.example.military.server;

import com.example.military.model.MilitaryPerson;
import com.example.military.shared.JsonConverter;
import com.example.military.shared.Protocol;
import com.google.gson.JsonObject;

public class RequestParser {

    /**
     * Разбирает входящий запрос и возвращает команду
     */
    public static String getCommand(String requestJson) {
        return JsonConverter.extractCommand(requestJson);
    }

    /**
     * Извлекает данные военнослужащего из запроса ADD
     */
    public static MilitaryPerson extractPersonFromAddRequest(String requestJson) {
        JsonObject data = JsonConverter.extractData(requestJson);
        if (data != null) {
            String personJson = data.toString();
            return JsonConverter.fromJson(personJson);
        }
        return null;
    }

    /**
     * Извлекает ID из запроса DELETE/GET
     */
    public static Integer extractIdFromRequest(String requestJson) {
        JsonObject data = JsonConverter.extractData(requestJson);
        if (data != null && data.has("id")) {
            return data.get("id").getAsInt();
        }
        return null;
    }

    /**
     * Проверяет, является ли запрос корректным
     */
    public static boolean isValidRequest(String requestJson) {
        try {
            String command = getCommand(requestJson);
            return command != null && !command.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}