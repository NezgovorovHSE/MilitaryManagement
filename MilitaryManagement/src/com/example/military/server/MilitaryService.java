package com.example.military.server;

import com.example.military.model.MilitaryPerson;
import java.sql.SQLException;
import java.util.List;

public class MilitaryService {
    private final DatabaseManager dbManager;
    private final ServerLogger logger;

    public MilitaryService(DatabaseManager dbManager, ServerLogger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    /**
     * Добавление нового военнослужащего
     */
    public int addPerson(MilitaryPerson person) {
        try {
            int id = dbManager.savePerson(person);
            logger.log("✅ Добавлен военнослужащий: " + person.getLastName() + " (ID: " + id + ")");
            return id;
        } catch (SQLException e) {
            logger.error("Ошибка при добавлении военнослужащего", e);
            return -1;
        }
    }

    /**
     * Получение всех военнослужащих
     */
    public List<MilitaryPerson> getAllPersons() {
        try {
            List<MilitaryPerson> list = dbManager.loadAllPersons();
            logger.log("📋 Запрошен список военнослужащих. Всего: " + list.size());
            return list;
        } catch (SQLException e) {
            logger.error("Ошибка при загрузке списка", e);
            return List.of(); // Пустой список
        }
    }

    /**
     * Получение военнослужащего по ID
     */
    public MilitaryPerson getPersonById(int id) {
        try {
            MilitaryPerson person = dbManager.loadPersonById(id);
            if (person != null) {
                logger.log("🔍 Запрошен военнослужащий ID " + id + ": " + person.getLastName());
            } else {
                logger.log("🔍 Военнослужащий ID " + id + " не найден");
            }
            return person;
        } catch (SQLException e) {
            logger.error("Ошибка при загрузке военнослужащего ID " + id, e);
            return null;
        }
    }

    public boolean updatePerson(MilitaryPerson person) {
        try {
            // TODO: реализовать UPDATE в DatabaseManager
            return dbManager.updatePerson(person);
        } catch (SQLException e) {
            logger.error("Ошибка обновления", e);
            return false;
        }
    }

    public boolean lockRecord(int recordId, int userId) {
        try {
            return dbManager.lockRecord(recordId, userId);
        } catch (SQLException e) {
            logger.error("Ошибка блокировки записи " + recordId, e);
            return false;
        }
    }

    public boolean unlockRecord(int recordId) {
        try {
            return dbManager.unlockRecord(recordId);
        } catch (SQLException e) {
            logger.error("Ошибка разблокировки записи " + recordId, e);
            return false;
        }
    }

    /**
     * Удаление военнослужащего по ID
     */
    public boolean deletePerson(int id) {
        try {
            boolean deleted = dbManager.deletePerson(id);
            if (deleted) {
                logger.log("🗑️ Удален военнослужащий ID " + id);
            } else {
                logger.log("🗑️ Военнослужащий ID " + id + " не найден для удаления");
            }
            return deleted;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении военнослужащего ID " + id, e);
            return false;
        }
    }

    /**
     * Получение количества военнослужащих
     */
    public int getCount() {
        try {
            int count = dbManager.getCount();
            logger.log("📊 Запрошено количество записей: " + count);
            return count;
        } catch (SQLException e) {
            logger.error("Ошибка при получении количества", e);
            return 0;
        }
    }

    /**
     * Очистка всех записей
     */
    public boolean clearAll() {
        try {
            dbManager.clearAll();
            logger.log("🧹 Все записи удалены");
            return true;
        } catch (SQLException e) {
            logger.error("Ошибка при очистке базы данных", e);
            return false;
        }
    }
}