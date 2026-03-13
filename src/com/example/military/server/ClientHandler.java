package com.example.military.server;

import com.example.military.model.MilitaryPerson;
import com.example.military.model.User;
import com.example.military.service.FileManager;
import com.example.military.shared.Protocol;
import com.example.military.shared.JsonConverter;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final MilitaryService service;
    private final ServerLogger logger;
    private final int clientId;
    private static int clientCounter = 0;
    private static final boolean AUTH_ENABLED = true;
    private User currentUser = null;
    private String clientInfo;

    public ClientHandler(Socket socket, MilitaryService service, ServerLogger logger) {
        this.clientSocket = socket;
        this.service = service;
        this.logger = logger;
        this.clientId = ++clientCounter;
    }

    @Override
    public void run() {
        this.clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        logger.logClientAction(clientInfo, "Клиент #" + clientId + " подключился");

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                logger.logClientAction(clientInfo, "Получен запрос: " + inputLine);

                String response = processRequest(inputLine);

                out.println(response);
                out.flush();

                logger.logClientAction(clientInfo, "Отправлен ответ: " + response);
            }

        } catch (IOException e) {
            logger.error("Ошибка при обработке клиента #" + clientId, e);
        } finally {
            try {
                clientSocket.close();
                logger.logClientAction(clientInfo, "Клиент #" + clientId + " отключился");

            } catch (IOException e) {
                logger.error("Ошибка при закрытии соединения с клиентом #" + clientId, e);
            }
        }
    }

    private String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    private String handleLockCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            int recordId = data.get("id").getAsInt();
            int userId = data.get("userId").getAsInt();

            MilitaryPerson person = service.getPersonById(recordId);

            boolean locked = service.lockRecord(recordId, userId);

            if (locked) {
                if (person != null) {
                    AuditLogger.logLock(getCurrentUsername(), person);
                } else {
                    AuditLogger.log(getCurrentUsername(), "БЛОКИРОВКА", "Запись ID=" + recordId);
                }
                return ResponseBuilder.successWithMessage("Locked");
            } else {
                return ResponseBuilder.error("Already locked");
            }
        } catch (Exception e) {
            logger.error("Ошибка блокировки", e);
            return ResponseBuilder.error("Error");
        }
    }

    private String handleUnlockCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            int recordId = data.get("id").getAsInt();
            int userId = data.get("userId").getAsInt();

            MilitaryPerson person = service.getPersonById(recordId);
            String lastName = (person != null) ? person.getLastName() : "неизвестно";

            boolean unlocked = service.unlockRecord(recordId, userId);

            if (unlocked) {
                String type = person != null ? AuditLogger.getTypeString(person) : "неизвестно";
                AuditLogger.logUnlock(getCurrentUsername(), recordId, lastName, type);
                return ResponseBuilder.successWithMessage("Unlocked");
            } else {
                return ResponseBuilder.error("Not locked or wrong user");
            }
        } catch (Exception e) {
            logger.error("Ошибка разблокировки", e);
            return ResponseBuilder.error("Error");
        }
    }

    private String handleLogoutCommand() {
        if (currentUser != null) {
            AuditLogger.logLogout(currentUser.getUsername());
            currentUser = null;
        }
        return ResponseBuilder.successWithMessage("Выход выполнен");
    }

    private String handleImportCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            if (data == null) {
                return ResponseBuilder.error("Нет данных в запросе");
            }

            String filePath = data.get("filePath").getAsString();
            int userId = data.has("userId") ? data.get("userId").getAsInt() : 0;

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }

            List<MilitaryPerson> importedList = JsonConverter.listFromJson(content.toString());

            if (importedList == null || importedList.isEmpty()) {
                return ResponseBuilder.error("Файл пуст или не содержит записей");
            }

            int added = 0;
            for (MilitaryPerson person : importedList) {
                List<MilitaryPerson> existing = service.getAllPersons();
                boolean exists = existing.stream().anyMatch(p ->
                        p.getLastName().equals(person.getLastName()) &&
                                Objects.equals(p.getBirthDate(), person.getBirthDate())
                );

                if (!exists) {
                    service.addPerson(person);
                    added++;
                }
            }

            User user = service.getUserById(userId);
            String username = (user != null) ? user.getUsername() : "system";
            AuditLogger.log(username, "ВЫГРУЗКА", "Из файла: " + filePath + ", добавлено: " + added + " записей");

            return ResponseBuilder.successWithMessage("Импортировано: " + added);

        } catch (Exception e) {
            logger.error("Ошибка импорта", e);
            return ResponseBuilder.error("Ошибка импорта: " + e.getMessage());
        }
    }

    private String processRequest(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            String command = request.get(Protocol.FIELD_COMMAND).getAsString();

            if (AUTH_ENABLED && !command.equals(Protocol.CMD_LOGIN)) {
                JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);
                if (data != null && data.has("userId")) {
                    int userId = data.get("userId").getAsInt();
                    if (currentUser == null || currentUser.getId() != userId) {
                        currentUser = service.getUserById(userId);
                    }
                }
            }

            switch (command) {
                case Protocol.CMD_LOGIN:
                    return handleLoginCommand(requestJson);
                case Protocol.CMD_LOGOUT:
                    return handleLogoutCommand();
                case Protocol.CMD_GET_ALL:
                    List<MilitaryPerson> list = service.getAllPersons();
                    return ResponseBuilder.personList(list);
                case Protocol.CMD_ADD:
                    return handleAddCommand(requestJson);
                case Protocol.CMD_DELETE:
                    return handleDeleteCommand(requestJson);
                case "UPDATE":
                    return handleUpdateCommand(requestJson);
                case Protocol.CMD_IMPORT:
                    return handleImportCommand(requestJson);
                case "LOCK":
                    return handleLockCommand(requestJson);
                case "UNLOCK":
                    return handleUnlockCommand(requestJson);
                case Protocol.CMD_LOG_EXPORT:
                    return handleLogExportCommand(requestJson);
                case Protocol.CMD_LOG_IMPORT:
                    return handleLogImportCommand(requestJson);
                default:
                    return ResponseBuilder.error("Неизвестная команда: " + command);
            }
        } catch (Exception e) {
            logger.error("Ошибка парсинга запроса", e);
            return ResponseBuilder.error("Ошибка формата запроса");
        }
    }

    private String handleLoginCommand(String requestJson) {
        try {
            if (!AUTH_ENABLED) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("userId", 1);
                responseData.addProperty("fullName", "Тестовый пользователь");
                return ResponseBuilder.success(responseData);
            }

            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();

            User user = service.authenticate(username, password);

            if (user != null) {
                this.currentUser = user;
                String ip = clientInfo.split(":")[0];
                AuditLogger.logLogin(user.getUsername(), ip, true);

                String responseString = "{\"userId\":" + user.getId() +
                        ",\"fullName\":\"" + user.getFullName() + "\"}";
                return ResponseBuilder.success(responseString);
            } else {
                String ip = clientInfo.split(":")[0];
                AuditLogger.logLogin(null, ip, false);
                return ResponseBuilder.error("Неверное имя пользователя или пароль");
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки логина", e);
            return ResponseBuilder.error("Ошибка сервера");
        }
    }

    private String handleLogExportCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            String filename = data.get("filename").getAsString();
            int count = data.get("count").getAsInt();
            int userId = data.has("userId") ? data.get("userId").getAsInt() : 0;

            User user = service.getUserById(userId);
            String username = (user != null) ? user.getUsername() : "system";

            AuditLogger.log(username, "СОХРАНЕНИЕ", "В файл: " + filename + ", записей: " + count);

            return ResponseBuilder.successWithMessage("Logged");
        } catch (Exception e) {
            logger.error("Ошибка логирования экспорта", e);
            return ResponseBuilder.error("Error");
        }
    }

    private String handleLogImportCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            String filename = data.get("filename").getAsString();
            int added = data.get("added").getAsInt();
            int userId = data.has("userId") ? data.get("userId").getAsInt() : 0;

            User user = service.getUserById(userId);
            String username = (user != null) ? user.getUsername() : "system";

            AuditLogger.log(username, "ВЫГРУЗКА", "Из файла: " + filename + ", добавлено: " + added + " записей");

            return ResponseBuilder.successWithMessage("Logged");
        } catch (Exception e) {
            logger.error("Ошибка логирования импорта", e);
            return ResponseBuilder.error("Error");
        }
    }

    private String handleAddCommand(String requestJson) {
        MilitaryPerson person = RequestParser.extractPersonFromAddRequest(requestJson);
        Integer userId = RequestParser.extractUserIdFromRequest(requestJson);

        if (person == null) {
            return ResponseBuilder.error("Не удалось извлечь данные военнослужащего");
        }

        int id = service.addPerson(person);

        if (id > 0) {
            AuditLogger.logAdd(getCurrentUsername(), person, id);
            return ResponseBuilder.successWithMessage("Военнослужащий добавлен с ID: " + id);
        } else {
            AuditLogger.log(getCurrentUsername(), "ОШИБКА_ДОБАВЛЕНИЯ", "Не удалось добавить: " + person.getLastName());
            return ResponseBuilder.error("Ошибка при добавлении военнослужащего");
        }
    }

    private String handleUpdateCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);
            JsonObject personData = data.getAsJsonObject("person");
            int userId = data.get("userId").getAsInt();

            MilitaryPerson newPerson = JsonConverter.fromJson(personData.toString());

            MilitaryPerson oldPerson = service.getPersonById(newPerson.getId());

            boolean updated = service.updatePerson(newPerson);

            if (updated) {
                AuditLogger.logUpdate(getCurrentUsername(), oldPerson, newPerson);
                return ResponseBuilder.successWithMessage("Updated");
            } else {
                AuditLogger.log(getCurrentUsername(), "ОШИБКА_ОБНОВЛЕНИЯ", "ID=" + newPerson.getId());
                return ResponseBuilder.error("Update failed");
            }
        } catch (Exception e) {
            logger.error("Ошибка обновления", e);
            return ResponseBuilder.error("Error");
        }
    }

    private String handleDeleteCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            int id = data.get("id").getAsInt();
            int userId = data.get("userId").getAsInt();

            Integer lockOwner = service.checkLockOwner(id);
            if (lockOwner != null && lockOwner != userId) {
                return ResponseBuilder.error("Запись редактируется другим пользователем");
            }

            MilitaryPerson person = service.getPersonById(id);

            boolean deleted = service.deletePerson(id);

            if (deleted) {
                if (person != null) {
                    AuditLogger.logDelete(getCurrentUsername(), person);
                } else {
                    AuditLogger.log(getCurrentUsername(), "УДАЛЕНИЕ", "ID=" + id + " (запись не найдена)");
                }
                return ResponseBuilder.successWithMessage("Удалено");
            } else {
                AuditLogger.log(getCurrentUsername(), "ОШИБКА_УДАЛЕНИЯ", "ID=" + id);
                return ResponseBuilder.error("Не найдено");
            }
        } catch (Exception e) {
            logger.error("Ошибка удаления", e);
            return ResponseBuilder.error("Error");
        }
    }
}