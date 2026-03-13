package com.example.military.server;

import com.example.military.model.MilitaryPerson;
import com.example.military.shared.JsonConverter;
import com.example.military.shared.Protocol;
import java.util.List;

public class ResponseBuilder {

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

    public static String successWithMessage(String message) {
        return JsonConverter.createResponse(Protocol.STATUS_OK, message, null);
    }

    public static String error(String message) {
        return JsonConverter.createResponse(Protocol.STATUS_ERROR, message, null);
    }

    public static String notFound(String message) {
        return JsonConverter.createResponse(Protocol.STATUS_NOT_FOUND, message, null);
    }

    public static String personList(List<MilitaryPerson> list) {
        return JsonConverter.createResponse(Protocol.STATUS_OK, null, list);
    }

    public static String singlePerson(MilitaryPerson person) {
        return JsonConverter.createResponse(Protocol.STATUS_OK, null, person);
    }

    public static String countResponse(int count) {
        return JsonConverter.createResponse(Protocol.STATUS_OK, null, count);
    }
}