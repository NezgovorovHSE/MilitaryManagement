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

    public ClientHandler(Socket socket, MilitaryService service, ServerLogger logger) {
        this.clientSocket = socket;
        this.service = service;
        this.logger = logger;
        this.clientId = ++clientCounter;
    }

    @Override
    public void run() {
        String clientInfo = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        logger.logClientAction(clientInfo, "Клиент #" + clientId + " подключился");

        // Логируем подключение в аудит
        logUserAction("ПОДКЛЮЧЕНИЕ", "Клиент " + clientInfo);

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

                // Логируем отключение в аудит
                logUserAction("ОТКЛЮЧЕНИЕ", "Клиент " + clientInfo);

            } catch (IOException e) {
                logger.error("Ошибка при закрытии соединения с клиентом #" + clientId, e);
            }
        }
    }

    private String handleLockCommand(String requestJson) {
        try {
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            int recordId = data.get("id").getAsInt();
            int userId = data.get("userId").getAsInt();  // получаем userId из запроса

            boolean locked = service.lockRecord(recordId, userId);

            if (locked) {
                logUserAction("БЛОКИРОВКА", "Запись ID=" + recordId);
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
            int userId = data.get("userId").getAsInt();  // получаем userId

            boolean unlocked = service.unlockRecord(recordId, userId);

            if (unlocked) {
                logUserAction("РАЗБЛОКИРОВКА", "Запись ID=" + recordId);
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
            logUserAction("ВЫХОД", "Пользователь " + currentUser.getFullName() + " вышел");
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
                if (data == null || !data.has("userId")) {
                    return ResponseBuilder.error("Требуется аутентификация");
                }
                int userId = data.get("userId").getAsInt();
                // Здесь можно загрузить пользователя по ID, если нужно
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

    private String handleLogoutCommand(String requestJson) {
        if (currentUser != null) {
            logUserAction("ВЫХОД", "Пользователь " + currentUser.getFullName() + " вышел");
            currentUser = null;
        }
        return ResponseBuilder.successWithMessage("Выход выполнен");
    }

    private String handleLoginCommand(String requestJson) {
        try {
            // Если аутентификация отключена - пропускаем всех
            if (!AUTH_ENABLED) {
                JsonObject responseData = new JsonObject();
                responseData.addProperty("userId", 1);
                responseData.addProperty("fullName", "Тестовый пользователь");
                return ResponseBuilder.success(responseData); // здесь проблема!
            }

            // Правильный способ извлечения данных
            JsonObject request = new com.google.gson.JsonParser().parse(requestJson).getAsJsonObject();
            JsonObject data = request.getAsJsonObject(Protocol.FIELD_DATA);

            String username = data.get("username").getAsString();
            String password = data.get("password").getAsString();

            User user = service.authenticate(username, password);

            if (user != null) {
                this.currentUser = user;

                // Создаём простую строку ответа вместо JsonObject
                String responseString = "{\"userId\":" + user.getId() +
                        ",\"fullName\":\"" + user.getFullName() + "\"}";
                return ResponseBuilder.success(responseString);
            } else {
                return ResponseBuilder.error("Неверное имя пользователя или пароль");
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
        MilitaryPerson person = RequestParser.extractPersonFromAddRequest(requestJson);
        Integer userId = RequestParser.extractUserIdFromRequest(requestJson);

        if (person == null) {
            return ResponseBuilder.error("Не удалось извлечь данные военнослужащего");
        }

        // Здесь можно использовать userId для логирования или проверки прав
        int id = service.addPerson(person);

        if (id > 0) {
            logUserAction("ДОБАВЛЕНИЕ", "ID=" + id + ", " + person.getLastName());
            return ResponseBuilder.successWithMessage("Военнослужащий добавлен с ID: " + id);
        } else {
            logUserAction("ОШИБКА_ДОБАВЛЕНИЯ", person.getLastName());
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
        logUserAction("ПРОСМОТР", "Все записи (" + list.size() + ")");
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
        MilitaryPerson person = RequestParser.extractPersonFromAddRequest(requestJson);

        if (person == null) {
            return ResponseBuilder.error("Не удалось извлечь данные");
        }

        boolean updated = service.updatePerson(person);

        if (updated) {
            logUserAction("ОБНОВЛЕНИЕ", "ID=" + person.getId() + ", " + person.getLastName());
            return ResponseBuilder.successWithMessage("Обновлено");
        } else {
            logUserAction("ОШИБКА_ОБНОВЛЕНИЯ", "ID=" + person.getId());
            return ResponseBuilder.error("Ошибка обновления");
        }
    }

    /**
     * Обработка команды DELETE
     */
    private String handleDeleteCommand(String requestJson) {
        Integer id = RequestParser.extractIdFromRequest(requestJson);

        if (id == null) {
            return ResponseBuilder.error("Не указан ID");
        }

        boolean deleted = service.deletePerson(id);

        if (deleted) {
            logUserAction("УДАЛЕНИЕ", "ID=" + id);
            return ResponseBuilder.successWithMessage("Удалено");
        } else {
            logUserAction("ОШИБКА_УДАЛЕНИЯ", "ID=" + id);
            return ResponseBuilder.error("Не найдено");
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
