package com.example.military.client;

import com.example.military.model.*;
import com.example.military.server.RequestParser;
import com.example.military.server.ResponseBuilder;
import com.example.military.shared.JsonConverter;
import com.example.military.shared.Protocol;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ServerConnector {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Integer currentUserId = null;

    public ServerConnector() {
        this.host = "192.168.31.170";
        this.port = Protocol.DEFAULT_PORT;
    }

    public ServerConnector(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getServerAddress() {
        return host + ":" + port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка подключения к серверу: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public boolean lockRecord(int recordId) {
        System.out.println("📤 Клиент: отправка LOCK для ID=" + recordId);
        System.out.println("currentUserId = " + currentUserId);
        if (!connect()) return false;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("id", recordId);
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }

            String request = JsonConverter.createRequest("LOCK", data);
            out.println(request);
            out.flush();

            String response = in.readLine();
            System.out.println("📥 Клиент: ответ на LOCK: " + response);

            disconnect();

            return response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response));
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    public boolean unlockRecord(int recordId) {
        System.out.println("📤 Клиент: отправка UNLOCK для ID=" + recordId);
        System.out.println("currentUserId = " + currentUserId);

        if (!connect()) return false;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("id", recordId);
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }

            String request = JsonConverter.createRequest("UNLOCK", data);
            out.println(request);
            out.flush();

            String response = in.readLine();
            System.out.println("📥 Клиент: ответ на UNLOCK: " + response);

            disconnect();

            return response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response));
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    public boolean testConnection() {
        boolean connected = connect();
        disconnect();
        return connected;
    }

    public List<MilitaryPerson> getAllPersons() {
        System.out.println("=== getAllPersons вызван ===");
        System.out.println("currentUserId = " + currentUserId);
        if (!connect()) return null;

        try {
            // Добавляем userId в запрос
            JsonObject data = new JsonObject();
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }

            String request = JsonConverter.createRequest(Protocol.CMD_GET_ALL, data);
            out.println(request);
            out.flush();

            String response = in.readLine();

            if (response == null) {
                disconnect();
                return null;
            }

            // Парсим JSON-ответ
            JsonObject jsonResponse = new com.google.gson.JsonParser().parse(response).getAsJsonObject();
            String status = jsonResponse.get(Protocol.FIELD_STATUS).getAsString();

            if (Protocol.STATUS_OK.equals(status)) {
                if (jsonResponse.has(Protocol.FIELD_DATA)) {
                    String dataJson = jsonResponse.get(Protocol.FIELD_DATA).toString();
                    List<MilitaryPerson> result = JsonConverter.listFromJson(dataJson);
                    disconnect();
                    return result;
                }
                disconnect();
                return new ArrayList<>();
            } else {
                String message = jsonResponse.has(Protocol.FIELD_MESSAGE) ?
                        jsonResponse.get(Protocol.FIELD_MESSAGE).getAsString() : "Unknown error";
                System.out.println("❌ Ошибка сервера: " + message);
                disconnect();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            return null;
        }
    }

    public int addPerson(MilitaryPerson person) {
        System.out.println("=== addPerson вызван ===");
        System.out.println("currentUserId = " + currentUserId);
        if (!connect()) return -1;

        try {
            // Добавляем userId в запрос
            JsonObject data = new JsonObject();
            data.add("person", JsonConverter.getGson().toJsonTree(person));
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }
            // Используем правильный метод для создания запроса
            String request = JsonConverter.createRequest(Protocol.CMD_ADD, data);
            System.out.println("=== request ===");
            System.out.println(request);

            out.println(request);
            out.flush();

            String response = in.readLine();
            System.out.println("=== response ===");
            System.out.println(response);

            disconnect();

            if (response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response))) {
                String message = JsonConverter.extractMessage(response);
                if (message != null && message.contains("ID:")) {
                    String idStr = message.replaceAll("[^0-9]", "");
                    if (!idStr.isEmpty()) {
                        return Integer.parseInt(idStr);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public MilitaryPerson getPersonById(int id) {
        if (!connect()) return null;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("id", id);
            String request = JsonConverter.createRequest(Protocol.CMD_GET_BY_ID, data);
            out.println(request);

            String response = in.readLine();
            disconnect();

            if (response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response))) {
                JsonObject responseData = JsonConverter.extractData(response);
                if (responseData != null) {
                    return JsonConverter.fromJson(responseData.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updatePerson(MilitaryPerson person) {
        System.out.println("=== updatePerson вызван ===");
        System.out.println("currentUserId = " + currentUserId);
        if (!connect()) return false;

        try {
            // Создаём объект data с person и userId
            JsonObject data = new JsonObject();
            data.add("person", JsonConverter.getGson().toJsonTree(person));
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }
            String request = JsonConverter.createRequest(Protocol.CMD_UPDATE, data);
            System.out.println("=== request ===");
            System.out.println(request);

            out.println(request);
            out.flush();

            String response = in.readLine();
            System.out.println("=== response ===");
            System.out.println(response);

            disconnect();

            return response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response));
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    public boolean deletePerson(int id) {
        System.out.println("=== deletePerson вызван ===");
        System.out.println("currentUserId = " + currentUserId);
        if (!connect()) return false;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("id", id);
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }

            String request = JsonConverter.createRequest(Protocol.CMD_DELETE, data);
            System.out.println("=== request ===");
            System.out.println(request);

            out.println(request);
            out.flush();

            String response = in.readLine();
            System.out.println("=== response ===");
            System.out.println(response);

            disconnect();

            return response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response));
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    public int getCount() {
        if (!connect()) return 0;

        try {
            String request = JsonConverter.createRequest(Protocol.CMD_COUNT, null);
            out.println(request);

            String response = in.readLine();
            disconnect();

            if (response != null) {
                JsonObject data = JsonConverter.extractData(response);
                if (data != null && data.has("data")) {
                    return data.get("data").getAsInt();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean exportToFile(String filePath, List<MilitaryPerson> data) {
        if (data == null || data.isEmpty()) return false;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Заголовки CSV
            writer.println("ID,Фамилия,Рота,Звание,Дата рождения,Дата призыва,Часть,Зарплата,Тип,Дополнительно");

            // Данные
            for (MilitaryPerson p : data) {
                StringBuilder line = new StringBuilder();
                line.append(p.getId()).append(",");
                line.append(escapeCsv(p.getLastName())).append(",");
                line.append(escapeCsv(p.getCompany())).append(",");
                line.append(escapeCsv(p.getRank())).append(",");
                line.append(formatDate(p.getBirthDate())).append(",");
                line.append(formatDate(p.getEnlistmentDate())).append(",");
                line.append(escapeCsv(p.getUnit())).append(",");
                line.append(p.getSalary()).append(",");
                line.append(getTypeString(p)).append(",");
                line.append(escapeCsv(getExtraFields(p)));

                writer.println(line);
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
    }

    public void reset() {
        this.currentUserId = null;
    }

    private String getTypeString(MilitaryPerson p) {
        if (p instanceof MilitaryCommand) return "Командование";
        if (p instanceof MilitaryContract) return "Контракт";
        if (p instanceof MilitaryAwarded) return "Награждён";
        return "Военнослужащий";
    }

    public boolean logout() {
        System.out.println("=== logout вызван ===");
        System.out.println("currentUserId до сброса = " + currentUserId);

        // Получаем имя метода, который вызвал logout
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 2) {
            System.out.println("Вызван из: " + stackTrace[2].getClassName() + "." + stackTrace[2].getMethodName());
        }

        if (!connect()) {
            this.currentUserId = null;
            System.out.println("Не удалось подключиться, currentUserId сброшен");
            return false;
        }

        try {
            JsonObject data = new JsonObject();
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }

            String request = JsonConverter.createRequest(Protocol.CMD_LOGOUT, data);
            out.println(request);
            out.flush();

            String response = in.readLine();
            disconnect();

            // Сбрасываем текущего пользователя независимо от ответа сервера
            this.currentUserId = null;
            System.out.println("currentUserId сброшен после запроса");

            boolean success = response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response));
            System.out.println("Результат logout: " + (success ? "успех" : "неудача"));
            return success;

        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            this.currentUserId = null;
            System.out.println("currentUserId сброшен после исключения");
            return false;
        }
    }

    public User login(String username, String password) {
        if (!connect()) return null;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", username);
            data.addProperty("password", password);

            String request = JsonConverter.createRequest(Protocol.CMD_LOGIN, data);
            out.println(request);
            out.flush();

            String response = in.readLine();
            disconnect();

            if (response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response))) {
                String message = JsonConverter.extractMessage(response);
                JsonObject userData = new com.google.gson.JsonParser().parse(message).getAsJsonObject();
                int userId = userData.get("userId").getAsInt();
                String fullName = userData.get("fullName").getAsString();

                // Сохраняем userId для последующих запросов
                this.currentUserId = userId;
                return new User(userId, username, fullName, "");
            } else {
                this.currentUserId = null;
                return null;// Сбрасываем при ошибке
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.currentUserId = null;
            return null;
        }
    }

    private String getExtraFields(MilitaryPerson p) {
        if (p instanceof MilitaryCommand) {
            MilitaryCommand cmd = (MilitaryCommand) p;
            return String.format("%s, %s, %d лет, надб. %.2f",
                    cmd.getMilitaryDistrict(), cmd.getPosition(),
                    cmd.getYearsOfService(), cmd.getAllowance());
        }
        if (p instanceof MilitaryContract) {
            MilitaryContract contract = (MilitaryContract) p;
            return String.format("%s, прот. %s",
                    contract.getContractPeriod(), contract.getProtocolNumber());
        }
        if (p instanceof MilitaryAwarded) {
            MilitaryAwarded awarded = (MilitaryAwarded) p;
            return String.format("%s, премия %.2f, надб. %.2f",
                    awarded.getAwardName(), awarded.getPrize(), awarded.getAllowance());
        }
        return "";
    }

    public Integer getCurrentUserId() {
        return currentUserId;
    }

    public int importFromFile(String filePath) {
        if (!connect()) return -1;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("filePath", filePath);

            String request = JsonConverter.createRequest(Protocol.CMD_IMPORT, data);
            out.println(request);
            out.flush();

            String response = in.readLine();
            disconnect();

            if (response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response))) {
                String message = JsonConverter.extractMessage(response);
                if (message != null && message.contains(":")) {
                    return Integer.parseInt(message.split(":")[1].trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void close() {
        disconnect();
    }
}
