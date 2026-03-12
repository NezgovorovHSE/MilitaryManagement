package com.example.military.shared;

import com.example.military.model.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonConverter {

    private static final Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        builder.registerTypeAdapter(MilitaryPerson.class, new MilitaryPersonAdapter());
        builder.registerTypeAdapter(MilitaryCommand.class, new MilitaryPersonAdapter());
        builder.registerTypeAdapter(MilitaryContract.class, new MilitaryPersonAdapter());
        builder.registerTypeAdapter(MilitaryAwarded.class, new MilitaryPersonAdapter());
        gson = builder.create();
    }

    public static Gson getGson() {
        return gson;
    }

    public static String toJson(MilitaryPerson person) {
        return gson.toJson(person);
    }

    public static String listToJson(List<MilitaryPerson> list) {
        return gson.toJson(list);
    }

    public static MilitaryPerson fromJson(String json) {
        return gson.fromJson(json, MilitaryPerson.class);
    }

    public static List<MilitaryPerson> listFromJson(String json) {
        System.out.println("=== listFromJson ===");
        System.out.println("JSON length: " + json.length());
        System.out.println("First 100 chars: " + json.substring(0, Math.min(100, json.length())));

        try {
            Type listType = new TypeToken<List<MilitaryPerson>>(){}.getType();
            List<MilitaryPerson> result = gson.fromJson(json, listType);
            System.out.println("Parsed " + (result != null ? result.size() : 0) + " items");
            return result;
        } catch (Exception e) {
            System.err.println("Error parsing JSON list: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static String createRequest(String command, Object data) {
        System.out.println("=== createRequest ===");
        System.out.println("data class: " + (data != null ? data.getClass().getName() : "null"));

        JsonObject request = new JsonObject();
        request.addProperty(Protocol.FIELD_COMMAND, command);

        if (data != null) {
            JsonElement dataJson = gson.toJsonTree(data);
            System.out.println("dataJson: " + dataJson);
            request.add(Protocol.FIELD_DATA, dataJson);
        }

        String result = gson.toJson(request);
        System.out.println("result: " + result);
        return result;
    }

    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Список возможных форматов
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") // для ISO с временем
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // пробуем следующий формат
            }
        }

        throw new DateTimeParseException("Не удалось распарсить дату: " + dateStr, dateStr, 0);
    }

    public static String createResponse(String status, String message, Object data) {
        JsonObject response = new JsonObject();
        response.addProperty(Protocol.FIELD_STATUS, status);

        if (message != null) {
            response.addProperty(Protocol.FIELD_MESSAGE, message);
        }

        if (data != null) {
            response.add(Protocol.FIELD_DATA, gson.toJsonTree(data));
        }

        return gson.toJson(response);
    }

    public static String extractCommand(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        return obj.has(Protocol.FIELD_COMMAND) ? obj.get(Protocol.FIELD_COMMAND).getAsString() : null;
    }

    public static JsonObject extractData(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        return obj != null && obj.has(Protocol.FIELD_DATA) ? obj.get(Protocol.FIELD_DATA).getAsJsonObject() : null;
    }

    public static String extractDataAsString(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        return obj != null && obj.has(Protocol.FIELD_DATA) ? obj.get(Protocol.FIELD_DATA).toString() : "[]";
    }

    public static String extractStatus(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        return obj.has(Protocol.FIELD_STATUS) ? obj.get(Protocol.FIELD_STATUS).getAsString() : null;
    }

    public static String extractMessage(String json) {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        return obj.has(Protocol.FIELD_MESSAGE) ? obj.get(Protocol.FIELD_MESSAGE).getAsString() : null;
    }

    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString()); // ISO format
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDate.parse(in.nextString()); // ISO format
        }
    }

    private static class MilitaryPersonAdapter implements JsonSerializer<MilitaryPerson>, JsonDeserializer<MilitaryPerson> {

        @Override
        public JsonElement serialize(MilitaryPerson src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();

            if (src instanceof MilitaryCommand) {
                result.addProperty(Protocol.FIELD_TYPE, Protocol.TYPE_COMMAND);
            } else if (src instanceof MilitaryContract) {
                result.addProperty(Protocol.FIELD_TYPE, Protocol.TYPE_CONTRACT);
            } else if (src instanceof MilitaryAwarded) {
                result.addProperty(Protocol.FIELD_TYPE, Protocol.TYPE_AWARDED);
            } else {
                result.addProperty(Protocol.FIELD_TYPE, Protocol.TYPE_BASE);
            }

            JsonObject data = new JsonObject();
            data.addProperty("id", src.getId());
            data.addProperty("lastName", src.getLastName());
            data.addProperty("company", src.getCompany());
            data.addProperty("rank", src.getRank());
            data.addProperty("birthDate", src.getBirthDate() != null ? src.getBirthDate().toString() : null);
            data.addProperty("enlistmentDate", src.getEnlistmentDate() != null ? src.getEnlistmentDate().toString() : null);
            data.addProperty("unit", src.getUnit());
            data.addProperty("salary", src.getSalary());

            if (src instanceof MilitaryCommand) {
                MilitaryCommand cmd = (MilitaryCommand) src;
                data.addProperty("militaryDistrict", cmd.getMilitaryDistrict());
                data.addProperty("position", cmd.getPosition());
                data.addProperty("yearsOfService", cmd.getYearsOfService());
                data.addProperty("allowance", cmd.getAllowance());
            } else if (src instanceof MilitaryContract) {
                MilitaryContract contract = (MilitaryContract) src;
                data.addProperty("contractPeriod", contract.getContractPeriod());
                // При сериализации (записи в JSON) используем ISO
                data.addProperty("contractDate", contract.getContractDate() != null ? contract.getContractDate().toString() : null);
                data.addProperty("protocolNumber", contract.getProtocolNumber());
            } else if (src instanceof MilitaryAwarded) {
                MilitaryAwarded awarded = (MilitaryAwarded) src;
                data.addProperty("awardName", awarded.getAwardName());
                data.addProperty("prize", awarded.getPrize());
                data.addProperty("allowance", awarded.getAllowance());
            }

            result.add("data", data);
            return result;
        }

        @Override
        public MilitaryPerson deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String type = jsonObject.get(Protocol.FIELD_TYPE).getAsString();
            JsonObject data = jsonObject.get("data").getAsJsonObject();

            String lastName = data.get("lastName").getAsString();
            String company = data.has("company") ? data.get("company").getAsString() : null;
            String rank = data.has("rank") ? data.get("rank").getAsString() : null;
            int id = data.has("id") ? data.get("id").getAsInt() : 0;

            LocalDate birthDate = null;
            if (data.has("birthDate") && !data.get("birthDate").isJsonNull()) {
                birthDate = JsonConverter.parseDate(data.get("birthDate").getAsString());
            }

            LocalDate enlistmentDate = null;
            if (data.has("enlistmentDate") && !data.get("enlistmentDate").isJsonNull()) {
                enlistmentDate = JsonConverter.parseDate(data.get("enlistmentDate").getAsString());
            }

            String unit = data.has("unit") ? data.get("unit").getAsString() : null;
            double salary = data.has("salary") ? data.get("salary").getAsDouble() : 0;

            switch (type) {
                case Protocol.TYPE_COMMAND:
                    int cmdId = data.has("id") ? data.get("id").getAsInt() : 0;
                    MilitaryCommand cmd = new MilitaryCommand(
                            lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                            data.get("militaryDistrict").getAsString(),
                            data.get("position").getAsString(),
                            data.get("yearsOfService").getAsInt(),
                            data.get("allowance").getAsDouble()
                    );
                    cmd.setId(cmdId);  // добавить
                    return cmd;

                case Protocol.TYPE_CONTRACT:
                    LocalDate contractDate = null;
                    if (data.has("contractDate") && !data.get("contractDate").isJsonNull()) {
                        contractDate = JsonConverter.parseDate(data.get("contractDate").getAsString());
                    }
                    MilitaryContract contract = new MilitaryContract(
                            lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                            data.get("contractPeriod").getAsString(),
                            contractDate,
                            data.get("protocolNumber").getAsString()
                    );
                    contract.setId(id);
                    return contract;

                case Protocol.TYPE_AWARDED:
                    MilitaryAwarded awarded = new MilitaryAwarded(
                            lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                            data.get("awardName").getAsString(),
                            data.get("prize").getAsDouble(),
                            data.get("allowance").getAsDouble()
                    );
                    awarded.setId(id);
                    return awarded;

                case Protocol.TYPE_BASE:
                default:
                    MilitaryPerson person = new MilitaryPerson(
                            lastName, company, rank, birthDate, enlistmentDate, unit, salary
                    );
                    person.setId(id);
                    return person;
            }
        }
    }
}