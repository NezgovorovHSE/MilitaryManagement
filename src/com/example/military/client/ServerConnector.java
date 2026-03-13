package com.example.military.client;

import com.example.military.model.*;
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
        } catch (IOException ignored) {}
    }

    public boolean lockRecord(int recordId) {
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
            disconnect();

            return response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response));
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    public boolean unlockRecord(int recordId) {
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
        if (!connect()) return null;

        try {
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
        if (!connect()) return -1;

        try {
            JsonObject data = new JsonObject();
            data.add("person", JsonConverter.getGson().toJsonTree(person));
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }
            String request = JsonConverter.createRequest(Protocol.CMD_ADD, data);
            out.println(request);
            out.flush();

            String response = in.readLine();
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
        if (!connect()) return false;

        try {
            JsonObject data = new JsonObject();
            data.add("person", JsonConverter.getGson().toJsonTree(person));
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }
            String request = JsonConverter.createRequest(Protocol.CMD_UPDATE, data);
            out.println(request);
            out.flush();

            String response = in.readLine();
            disconnect();

            return response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response));
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    public boolean deletePerson(int id) {
        if (!connect()) return false;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("id", id);
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }

            String request = JsonConverter.createRequest(Protocol.CMD_DELETE, data);
            out.println(request);
            out.flush();

            String response = in.readLine();
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
            writer.println("ID,Фамилия,Рота,Звание,Дата рождения,Дата призыва,Часть,Зарплата,Тип,Дополнительно");

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

    public void logExport(String filename, int count) {
        if (!connect()) return;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("filename", filename);
            data.addProperty("count", count);
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }

            String request = JsonConverter.createRequest(Protocol.CMD_LOG_EXPORT, data);
            out.println(request);
            out.flush();

            String response = in.readLine();
            disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
        }
    }

    public void logImport(String filename, int added) {
        if (!connect()) return;

        try {
            JsonObject data = new JsonObject();
            data.addProperty("filename", filename);
            data.addProperty("added", added);
            if (currentUserId != null) {
                data.addProperty("userId", currentUserId);
            }

            String request = JsonConverter.createRequest(Protocol.CMD_LOG_IMPORT, data);
            out.println(request);
            out.flush();

            String response = in.readLine();
            disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
        }
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
        if (!connect()) {
            this.currentUserId = null;
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
            this.currentUserId = null;

            return response != null && Protocol.STATUS_OK.equals(JsonConverter.extractStatus(response));

        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
            this.currentUserId = null;
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

                this.currentUserId = userId;
                return new User(userId, username, fullName, "");
            } else {
                this.currentUserId = null;
                return null;
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