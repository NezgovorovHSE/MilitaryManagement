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
    private static final boolean AUTH_ENABLED = true;  // false - режим разработки, true - с аутентификацией
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

                // Обрабатываем запрос
                String response = processRequest(inputLine);

                // Отправляем ответ
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
        System.out.println("=== handleLockCommand ===");
        System.out.println("currentUser = " + (currentUser != null ? currentUser.getId() : "null"));
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            int recordId = data.get("id").getAsInt();
            int userId = data.get("userId").getAsInt();

            // Проверку на currentUser убираем

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
        System.out.println("=== handleUnlockCommand ===");
        System.out.println("currentUser = " + (currentUser != null ? currentUser.getId() : "null"));
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            int recordId = data.get("id").getAsInt();
            int userId = data.get("userId").getAsInt();

            // Проверку убираем

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

    // Вспомогательный метод для подсчета символов
    private int countChar(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) count++;
        }
        return count;
    }

    private String handleLogoutCommand() {
        if (currentUser != null) {
            AuditLogger.log(currentUser.getId(), "ВЫХОД", "");
            currentUser = null;
        }
        return ResponseBuilder.successWithMessage("Выход выполнен");
    }

    private String handleImportCommand(String requestJson) {
        JsonObject data = JsonConverter.extractData(requestJson);
        String filePath = data.get("filePath").getAsString();

        try {
            List<MilitaryPerson> importedList = FileManager.loadFromFile(filePath);
            if (importedList == null || importedList.isEmpty()) {
                return ResponseBuilder.error("Файл пуст или не прочитан");
            }

            int added = 0;
            for (MilitaryPerson person : importedList) {
                // Проверяем, есть ли уже такой (по фамилии + дате рождения)
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
            AuditLogger.logImport(getCurrentUsername(), filePath, added);

            return ResponseBuilder.successWithMessage("Импортировано: " + added);

        } catch (Exception e) {
            logger.error("Ошибка импорта", e);
            return ResponseBuilder.error("Ошибка импорта: " + e.getMessage());
        }
    }

    /**
     * Обработка запроса от клиента
     */
    private String processRequest(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            String command = request.get(Protocol.FIELD_COMMAND).getAsString();

            if (AUTH_ENABLED && !command.equals(Protocol.CMD_LOGIN)) {
                JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);
                if (data != null && data.has("userId")) {
                    int userId = data.get("userId").getAsInt();
                    if (currentUser == null || currentUser.getId() != userId) {
                        // Загружаем пользователя по ID
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
            // Если аутентификация отключена - пропускаем всех
            if (!AUTH_ENABLED) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("userId", 1);
                responseData.addProperty("fullName", "Тестовый пользователь");
                return ResponseBuilder.success(responseData);
            }

            // Правильный способ извлечения данных
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
                return ResponseBuilder.error("Неверное имя пользователя или пароль"); // ← добавить
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки логина", e);
            return ResponseBuilder.error("Ошибка сервера");
        }
    }

    /**
     * Обработка команды ADD
     */
    private String handleAddCommand(String requestJson) {
        System.out.println("=== handleAddCommand ===");
        System.out.println("currentUser = " + (currentUser != null ? currentUser.getId() : "null"));
        MilitaryPerson person = RequestParser.extractPersonFromAddRequest(requestJson);
        Integer userId = RequestParser.extractUserIdFromRequest(requestJson);

        if (person == null) {
            return ResponseBuilder.error("Не удалось извлечь данные военнослужащего");
        }

        // Здесь можно использовать userId для логирования или проверки прав
        int id = service.addPerson(person);

        if (id > 0) {
            AuditLogger.logAdd(getCurrentUsername(), person, id);
            return ResponseBuilder.successWithMessage("Военнослужащий добавлен с ID: " + id);
        } else {
            AuditLogger.log(getCurrentUsername(), "ОШИБКА_ДОБАВЛЕНИЯ", "Не удалось добавить: " + person.getLastName());
            return ResponseBuilder.error("Ошибка при добавлении военнослужащего");
        }
    }

    private void logUserAction(String action, String details) {
        Integer userId = (currentUser != null) ? currentUser.getId() : null;
        AuditLogger.log(userId, action, details);
    }

    /**
     * Обработка команды GET_ALL
     */
    private String handleGetAllCommand() {
        List<MilitaryPerson> list = service.getAllPersons();
        AuditLogger.log(getCurrentUsername(), "ПРОСМОТР", "Загружено записей: " + list.size());
        return ResponseBuilder.personList(list);
    }

    /**
     * Обработка команды GET_BY_ID
     */
    private String handleGetByIdCommand(String requestJson) {
        Integer id = RequestParser.extractIdFromRequest(requestJson);

        if (id == null) {
            return ResponseBuilder.error("Не указан ID военнослужащего");
        }

        MilitaryPerson person = service.getPersonById(id);

        if (person != null) {
            return ResponseBuilder.singlePerson(person);
        } else {
            return ResponseBuilder.notFound("Военнослужащий с ID " + id + " не найден");
        }
    }

    private String handleUpdateCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);
            JsonObject personData = data.getAsJsonObject("person");
            int userId = data.get("userId").getAsInt();

            // Получаем данные из запроса
            MilitaryPerson newPerson = JsonConverter.fromJson(personData.toString());

            // Получаем СТАРУЮ версию из БД
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

    /**
     * Обработка команды DELETE
     */
    private String handleDeleteCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            int id = data.get("id").getAsInt();
            int userId = data.get("userId").getAsInt();

            // Проверяем, не заблокирована ли запись другим пользователем
            Integer lockOwner = service.checkLockOwner(id);
            if (lockOwner != null && lockOwner != userId) {
                return ResponseBuilder.error("Запись редактируется другим пользователем");
            }

            // Получаем person ДО удаления
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

    /**
     * Обработка команды CLEAR
     */
    private String handleClearCommand() {
        boolean cleared = service.clearAll();

        if (cleared) {
            return ResponseBuilder.successWithMessage("Все записи удалены");
        } else {
            return ResponseBuilder.error("Ошибка при удалении всех записей");
        }
    }

    /**
     * Обработка команды COUNT
     */
    private String handleCountCommand() {
        int count = service.getCount();
        return ResponseBuilder.countResponse(count);
    }
}
