package com.example.military.client;

import com.example.military.client.ServerConnector;
import com.example.military.model.*;
import com.example.military.service.MilitarySorter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ClientMenu {
    private ServerConnector connector;
    private Scanner scanner;
    private DateTimeFormatter dateFormatter;
    private List<MilitaryPerson> currentList; // Кешируем список для сортировки

    public ClientMenu() {
        this.connector = new ServerConnector();
        this.scanner = new Scanner(System.in);
        this.dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        this.currentList = null;
    }

    public void start() {
        System.out.println("=========================================");
        System.out.println("   КЛИЕНТ УПРАВЛЕНИЯ ВОЕННЫМ СОСТАВОМ");
        System.out.println("=========================================");

        // Проверяем подключение к серверу
        if (!checkServerConnection()) {
            System.out.println("❌ Не удалось подключиться к серверу.");
            System.out.println("   Убедитесь, что сервер запущен по адресу " +
                    connector.getServerAddress());
            return;
        }

        boolean running = true;

        while (running) {
            printMainMenu();
            int choice = getIntInput("Выберите пункт меню: ");

            switch (choice) {
                case 1:
                    addMilitaryPerson();
                    break;
                case 2:
                    showAllMilitary();
                    break;
                case 3:
                    refreshList(); // Обновляем список с сервера
                    if (currentList != null && !currentList.isEmpty()) {
                        MilitarySorter.sortByLastNameAscending(currentList);
                        printSortedList("ПО ФАМИЛИИ (А→Я)");
                    }
                    break;
                case 4:
                    refreshList();
                    if (currentList != null && !currentList.isEmpty()) {
                        MilitarySorter.sortByLastNameDescending(currentList);
                        printSortedList("ПО ФАМИЛИИ (Я→А)");
                    }
                    break;
                case 5:
                    refreshList();
                    if (currentList != null && !currentList.isEmpty()) {
                        MilitarySorter.sortBySalaryAscending(currentList);
                        printSortedList("ПО ЗАРПЛАТЕ (ВОЗРАСТАНИЕ)");
                    }
                    break;
                case 6:
                    refreshList();
                    if (currentList != null && !currentList.isEmpty()) {
                        MilitarySorter.sortBySalaryDescending(currentList);
                        printSortedList("ПО ЗАРПЛАТЕ (УБЫВАНИЕ)");
                    }
                    break;
                case 7:
                    deletePerson();
                    break;
                case 8:
                    showPersonById();
                    break;
                case 9:
                    showStats();
                    break;
                case 10:
                    refreshList();
                    break;
                case 0:
                    System.out.println("\n👋 Программа завершена. До свидания!");
                    running = false;
                    break;
                default:
                    System.out.println("\n❌ Неверный пункт меню. Попробуйте снова.");
            }
        }

        connector.close();
    }

    private void printMainMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           ГЛАВНОЕ МЕНЮ КЛИЕНТА");
        System.out.println("=".repeat(50));
        System.out.println("1. ➕ Добавить военнослужащего");
        System.out.println("2. 📋 Показать всех военнослужащих");
        System.out.println("3. 🔤 Сортировать по фамилии (А→Я)");
        System.out.println("4. 🔤 Сортировать по фамилии (Я→А)");
        System.out.println("5. 💰 Сортировать по зарплате (возрастание)");
        System.out.println("6. 💰 Сортировать по зарплате (убывание)");
        System.out.println("7. 🗑️ Удалить военнослужащего");
        System.out.println("8. 🔍 Найти по ID");
        System.out.println("9. 📊 Статистика");
        System.out.println("10. 🔄 Обновить список с сервера");
        System.out.println("0. 🚪 Выход");
        System.out.println("-".repeat(50));
        System.out.println("Сервер: " + connector.getServerAddress());
        if (currentList != null) {
            System.out.println("Загружено записей: " + currentList.size());
        }
        System.out.println("-".repeat(50));
    }

    private boolean checkServerConnection() {
        System.out.print("\n🔌 Проверка подключения к серверу... ");
        boolean connected = connector.testConnection();
        if (connected) {
            System.out.println("✅ Успешно!");
        } else {
            System.out.println("❌ Ошибка!");
        }
        return connected;
    }

    private void refreshList() {
        System.out.print("\n🔄 Загрузка данных с сервера... ");
        try {
            currentList = connector.getAllPersons();
            if (currentList != null) {
                System.out.println("✅ Загружено " + currentList.size() + " записей");
                if (currentList.isEmpty()) {
                    System.out.println("   📭 Список пуст (это нормально для первого запуска)");
                }
            } else {
                System.out.println("❌ Ошибка загрузки - получен null");
            }
        } catch (Exception e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addMilitaryPerson() {
        System.out.println("\n--- ДОБАВЛЕНИЕ ВОЕННОСЛУЖАЩЕГО ---");
        System.out.println("Выберите тип:");
        System.out.println("1. Обычный военнослужащий");
        System.out.println("2. Органы военного управления");
        System.out.println("3. Военная служба по контракту");
        System.out.println("4. Награжденный");

        int type = getIntInput("Ваш выбор: ");
        if (type < 1 || type > 4) {
            System.out.println("❌ Неверный тип!");
            return;
        }

        // Ввод общих полей для всех типов
        System.out.println("\n--- Введите общие данные ---");
        String lastName = getStringInput("Фамилия: ");
        String company = getStringInput("Рота: ");
        String rank = getStringInput("Звание: ");
        LocalDate birthDate = getDateInput("Дата рождения (дд.мм.гггг): ");
        LocalDate enlistmentDate = getDateInput("Дата поступления на службу (дд.мм.гггг): ");
        String unit = getStringInput("Часть: ");
        double salary = getDoubleInput("Зарплата: ");

        MilitaryPerson person = null;

        switch (type) {
            case 1:
                person = new MilitaryPerson(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary
                );
                break;

            case 2:
                System.out.println("\n--- Данные для органов управления ---");
                String district = getStringInput("Название округа: ");
                String position = getStringInput("Должность: ");
                int yearsOfService = getIntInput("Выслуга лет: ");
                double allowance = getDoubleInput("Сумма надбавки: ");

                person = new MilitaryCommand(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        district, position, yearsOfService, allowance
                );
                break;

            case 3:
                System.out.println("\n--- Данные для контрактника ---");
                String contractPeriod = getStringInput("Период договора: ");
                LocalDate contractDate = getDateInput("Дата договора (дд.мм.гггг): ");
                String protocolNumber = getStringInput("Номер протокола: ");

                person = new MilitaryContract(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        contractPeriod, contractDate, protocolNumber
                );
                break;

            case 4:
                System.out.println("\n--- Данные для награжденного ---");
                String awardName = getStringInput("Название награды: ");
                double prize = getDoubleInput("Премия: ");
                double awardAllowance = getDoubleInput("Надбавка: ");

                person = new MilitaryAwarded(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        awardName, prize, awardAllowance
                );
                break;
        }

        if (person != null) {
            System.out.print("\n📤 Отправка данных на сервер... ");
            int id = connector.addPerson(person);
            if (id > 0) {
                System.out.println("✅ Успешно! ID: " + id);
                refreshList(); // Обновляем список
            } else {
                System.out.println("❌ Ошибка при добавлении");
            }
        }
    }

    private void showAllMilitary() {
        refreshList();
        if (currentList == null || currentList.isEmpty()) {
            System.out.println("\n📭 Список военнослужащих пуст.");
            return;
        }

        System.out.println("\n========== ВСЕ ВОЕННОСЛУЖАЩИЕ ==========");
        for (int i = 0; i < currentList.size(); i++) {
            MilitaryPerson p = currentList.get(i);
            System.out.println("\n----- Запись #" + (i + 1) + " (ID: " + getPersonId(p) + ") -----");
            System.out.println(p);
        }
        System.out.println("\n========== ВСЕГО: " + currentList.size() + " ==========");
    }

    private void printSortedList(String sortType) {
        if (currentList == null || currentList.isEmpty()) {
            System.out.println("\n📭 Список пуст.");
            return;
        }

        System.out.println("\n========== СПИСОК, ОТСОРТИРОВАННЫЙ " + sortType + " ==========");
        for (int i = 0; i < currentList.size(); i++) {
            MilitaryPerson p = currentList.get(i);
            System.out.println("\n----- " + (i + 1) + ". " +
                    p.getLastName() + " (" + p.getSalary() + " руб.) -----");
            System.out.println(p);
        }
    }

    private void deletePerson() {
        int id = getIntInput("\nВведите ID военнослужащего для удаления: ");

        System.out.print("🗑️ Отправка запроса на удаление... ");
        boolean deleted = connector.deletePerson(id);

        if (deleted) {
            System.out.println("✅ Военнослужащий с ID " + id + " удален");
            refreshList(); // Обновляем список
        } else {
            System.out.println("❌ Военнослужащий с ID " + id + " не найден");
        }
    }

    private void showPersonById() {
        int id = getIntInput("\nВведите ID военнослужащего: ");

        MilitaryPerson person = connector.getPersonById(id);

        if (person != null) {
            System.out.println("\n--- ВОЕННОСЛУЖАЩИЙ С ID " + id + " ---");
            System.out.println(person);
        } else {
            System.out.println("❌ Военнослужащий с ID " + id + " не найден");
        }
    }

    private void showStats() {
        System.out.println("\n--- СТАТИСТИКА ---");

        int count = connector.getCount();
        System.out.println("Всего записей на сервере: " + count);

        if (currentList != null) {
            System.out.println("Загружено в кеш клиента: " + currentList.size());

            // Подсчет по типам
            int base = 0, command = 0, contract = 0, awarded = 0;
            for (MilitaryPerson p : currentList) {
                if (p instanceof MilitaryCommand) command++;
                else if (p instanceof MilitaryContract) contract++;
                else if (p instanceof MilitaryAwarded) awarded++;
                else base++;
            }

            System.out.println("\nПо типам:");
            System.out.println("  - Обычных: " + base);
            System.out.println("  - Органы управления: " + command);
            System.out.println("  - Контрактники: " + contract);
            System.out.println("  - Награжденные: " + awarded);
        }
    }

    /**
     * Получает ID военнослужащего (временное решение через хеш)
     * В реальном приложении ID должен храниться в объекте
     */
    private int getPersonId(MilitaryPerson person) {
        return System.identityHashCode(person);
    }

    // Вспомогательные методы для ввода данных
    private String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = Integer.parseInt(scanner.nextLine().trim());
                return value;
            } catch (NumberFormatException e) {
                System.out.println("❌ Ошибка: введите целое число");
            }
        }
    }

    private double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                double value = Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
                return value;
            } catch (NumberFormatException e) {
                System.out.println("❌ Ошибка: введите число");
            }
        }
    }

    private LocalDate getDateInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String dateStr = scanner.nextLine().trim();
                if (dateStr.isEmpty()) {
                    return null;
                }
                return LocalDate.parse(dateStr, dateFormatter);
            } catch (DateTimeParseException e) {
                System.out.println("❌ Ошибка: введите дату в формате дд.мм.гггг");
            }
        }
    }
}