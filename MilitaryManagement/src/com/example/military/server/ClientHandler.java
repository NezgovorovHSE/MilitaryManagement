package com.example.military.server;

import com.example.military.model.MilitaryPerson;
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

    private String handleLockCommand(String requestJson) {
        Integer id = RequestParser.extractIdFromRequest(requestJson);
        if (id == null) return ResponseBuilder.error("No ID");

        boolean locked = service.lockRecord(id, 1); // временно userId = 1

        return locked ? ResponseBuilder.success("Locked") : ResponseBuilder.error("Already locked");
    }

    private String handleUnlockCommand(String requestJson) {
        Integer id = RequestParser.extractIdFromRequest(requestJson);
        if (id == null) return ResponseBuilder.error("No ID");

        boolean unlocked = service.unlockRecord(id);

        return unlocked ? ResponseBuilder.success("Unlocked") : ResponseBuilder.error("Not locked");
    }

    // Вспомогательный метод для подсчета символов
    private int countChar(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) count++;
        }
        return count;
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

            switch (command) {
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

    /**
     * Обработка команды ADD
     */
    private String handleAddCommand(String requestJson) {
        MilitaryPerson person = RequestParser.extractPersonFromAddRequest(requestJson);

        if (person == null) {
            return ResponseBuilder.error("Не удалось извлечь данные военнослужащего");
        }

        int id = service.addPerson(person);

        if (id > 0) {
            return ResponseBuilder.successWithMessage("Военнослужащий добавлен с ID: " + id);
        } else {
            return ResponseBuilder.error("Ошибка при добавлении военнослужащего");
        }
    }

    /**
     * Обработка команды GET_ALL
     */
    private String handleGetAllCommand() {
        List<MilitaryPerson> list = service.getAllPersons();
        String response = ResponseBuilder.personList(list);
        return response;
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
            return ResponseBuilder.successWithMessage("Обновлено");
        } else {
            return ResponseBuilder.error("Ошибка обновления");
        }
    }

    /**
     * Обработка команды DELETE
     */
    private String handleDeleteCommand(String requestJson) {
        Integer id = RequestParser.extractIdFromRequest(requestJson);

        if (id == null) {
            return ResponseBuilder.error("Не указан ID военнослужащего для удаления");
        }

        boolean deleted = service.deletePerson(id);

        if (deleted) {
            return ResponseBuilder.successWithMessage("Военнослужащий с ID " + id + " удален");
        } else {
            return ResponseBuilder.error("Военнослужащий с ID " + id + " не найден или ошибка удаления");
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