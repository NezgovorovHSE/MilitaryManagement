package com.example.military.ui;

import com.example.military.model.*;
import com.example.military.repository.MilitaryList;
import com.example.military.service.MilitarySorter;
import com.example.military.service.FileManager;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Menu {
    private MilitaryList militaryList;
    private Scanner scanner;
    private DateTimeFormatter dateFormatter;

    public Menu() {
        militaryList = new MilitaryList();
        scanner = new Scanner(System.in);
        dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    }

    public void start() {
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
                    sortByLastNameAsc();
                    break;
                case 4:
                    sortByLastNameDesc();
                    break;
                case 5:
                    sortBySalaryAsc();
                    break;
                case 6:
                    sortBySalaryDesc();
                    break;
                case 7:
                    saveToFile();
                    break;
                case 8:
                    loadFromFile();
                    break;
                case 9:
                    showDataFiles();
                    break;
                case 0:
                    System.out.println("\n👋 Программа завершена. До свидания!");
                    running = false;
                    break;
                default:
                    System.out.println("\n❌ Неверный пункт меню. Попробуйте снова.");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           ГЛАВНОЕ МЕНЮ УПРАВЛЕНИЯ");
        System.out.println("=".repeat(50));
        System.out.println("1. ➕ Добавить военнослужащего");
        System.out.println("2. 📋 Показать всех военнослужащих");
        System.out.println("3. 🔤 Сортировать по фамилии (А→Я)");
        System.out.println("4. 🔤 Сортировать по фамилии (Я→А)");
        System.out.println("5. 💰 Сортировать по зарплате (возрастание)");
        System.out.println("6. 💰 Сортировать по зарплате (убывание)");
        System.out.println("7. 💾 Сохранить данные в файл");
        System.out.println("8. 📂 Загрузить данные из файла");
        System.out.println("9. 📁 Показать доступные файлы");
        System.out.println("0. 🚪 Выход");
        System.out.println("-".repeat(50));
    }

    private void addMilitaryPerson() {
        System.out.println("\n--- ДОБАВЛЕНИЕ ВОЕННОСЛУЖАЩЕГО ---");
        System.out.println("Выберите тип:");
        System.out.println("1. Обычный военнослужащий");
        System.out.println("2. Органы военного управления");
        System.out.println("3. Военная служба по контракту");
        System.out.println("4. Награжденный");

        int type = getIntInput("Ваш выбор: ");

        // Ввод общих полей для всех типов
        System.out.println("\n--- Введите общие данные ---");
        String lastName = getStringInput("Фамилия: ");
        String company = getStringInput("Рота: ");
        String rank = getStringInput("Звание: ");
        LocalDate birthDate = getDateInput("Дата рождения (дд.мм.гггг): ");
        LocalDate enlistmentDate = getDateInput("Дата поступления на службу (дд.мм.гггг): ");
        String unit = getStringInput("Часть: ");
        double salary = getDoubleInput("Зарплата: ");

        switch (type) {
            case 1:
                MilitaryPerson person = new MilitaryPerson(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary
                );
                militaryList.addMilitary(person);
                System.out.println("✅ Обычный военнослужащий добавлен!");
                break;

            case 2:
                System.out.println("\n--- Данные для органов управления ---");
                String district = getStringInput("Название округа: ");
                String position = getStringInput("Должность: ");
                int yearsOfService = getIntInput("Выслуга лет: ");
                double allowance = getDoubleInput("Сумма надбавки: ");

                MilitaryCommand command = new MilitaryCommand(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        district, position, yearsOfService, allowance
                );
                militaryList.addMilitary(command);
                System.out.println("✅ Военнослужащий (органы управления) добавлен!");
                break;

            case 3:
                System.out.println("\n--- Данные для контрактника ---");
                String contractPeriod = getStringInput("Период договора: ");
                LocalDate contractDate = getDateInput("Дата договора (дд.мм.гггг): ");
                String protocolNumber = getStringInput("Номер протокола: ");

                MilitaryContract contract = new MilitaryContract(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        contractPeriod, contractDate, protocolNumber
                );
                militaryList.addMilitary(contract);
                System.out.println("✅ Военнослужащий (контрактник) добавлен!");
                break;

            case 4:
                System.out.println("\n--- Данные для награжденного ---");
                String awardName = getStringInput("Название награды: ");
                double prize = getDoubleInput("Премия: ");
                double awardAllowance = getDoubleInput("Надбавка: ");

                MilitaryAwarded awarded = new MilitaryAwarded(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        awardName, prize, awardAllowance
                );
                militaryList.addMilitary(awarded);
                System.out.println("✅ Награжденный военнослужащий добавлен!");
                break;

            default:
                System.out.println("❌ Неверный тип военнослужащего!");
        }
    }

    private void showAllMilitary() {
        System.out.println("\n--- ВСЕ ВОЕННОСЛУЖАЩИЕ ---");
        militaryList.printAll();
    }

    private void sortByLastNameAsc() {
        if (militaryList.isEmpty()) {
            System.out.println("❌ Список пуст. Сначала добавьте военнослужащих.");
            return;
        }
        MilitarySorter.sortByLastNameAscending(militaryList.getAll());
        System.out.println("\n--- СОРТИРОВКА ПО ФАМИЛИИ (А→Я) ---");
        militaryList.printAll();
    }

    private void sortByLastNameDesc() {
        if (militaryList.isEmpty()) {
            System.out.println("❌ Список пуст. Сначала добавьте военнослужащих.");
            return;
        }
        MilitarySorter.sortByLastNameDescending(militaryList.getAll());
        System.out.println("\n--- СОРТИРОВКА ПО ФАМИЛИИ (Я→А) ---");
        militaryList.printAll();
    }

    private void sortBySalaryAsc() {
        if (militaryList.isEmpty()) {
            System.out.println("❌ Список пуст. Сначала добавьте военнослужащих.");
            return;
        }
        MilitarySorter.sortBySalaryAscending(militaryList.getAll());
        System.out.println("\n--- СОРТИРОВКА ПО ЗАРПЛАТЕ (ВОЗРАСТАНИЕ) ---");
        militaryList.printAll();
    }

    private void sortBySalaryDesc() {
        if (militaryList.isEmpty()) {
            System.out.println("❌ Список пуст. Сначала добавьте военнослужащих.");
            return;
        }
        MilitarySorter.sortBySalaryDescending(militaryList.getAll());
        System.out.println("\n--- СОРТИРОВКА ПО ЗАРПЛАТЕ (УБЫВАНИЕ) ---");
        militaryList.printAll();
    }

    private void saveToFile() {
        if (militaryList.isEmpty()) {
            System.out.println("❌ Список пуст. Нечего сохранять.");
            return;
        }

        System.out.println("\n--- СОХРАНЕНИЕ В ФАЙЛ ---");
        String fileName = getStringInput("Введите имя файла (например, military_data.txt): ");

        // Добавляем расширение .txt если его нет
        if (!fileName.contains(".")) {
            fileName += ".txt";
        }

        boolean success = FileManager.saveToFile(militaryList.getAll(), fileName);
        if (success) {
            System.out.println("✅ Данные сохранены в файл: " + fileName);
        }
    }

    private void loadFromFile() {
        System.out.println("\n--- ЗАГРУЗКА ИЗ ФАЙЛА ---");
        String fileName = getStringInput("Введите имя файла для загрузки: ");

        // Добавляем расширение .txt если его нет
        if (!fileName.contains(".")) {
            fileName += ".txt";
        }

        if (!FileManager.fileExists(fileName)) {
            System.out.println("❌ Файл не существует: " + fileName);
            return;
        }

        List<MilitaryPerson> loadedList = FileManager.loadFromFile(fileName);
        if (loadedList != null && !loadedList.isEmpty()) {
            // Очищаем текущий список и добавляем загруженные данные
            militaryList.clear();
            for (MilitaryPerson person : loadedList) {
                militaryList.addMilitary(person);
            }
            System.out.println("✅ Загружено " + loadedList.size() + " записей");
        }
    }

    private void showDataFiles() {
        System.out.println("\n--- ДОСТУПНЫЕ ФАЙЛЫ С ДАННЫМИ ---");
        List<String> files = FileManager.getDataFiles();

        if (files.isEmpty()) {
            System.out.println("📁 Нет доступных файлов с расширением .txt или .csv");
        } else {
            System.out.println("Найдено файлов: " + files.size());
            for (int i = 0; i < files.size(); i++) {
                System.out.println((i + 1) + ". " + files.get(i));
            }
        }
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
                System.out.println("❌ Ошибка: введите число (например, 35000.50)");
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
                System.out.println("❌ Ошибка: введите дату в формате дд.мм.гггг (например, 15.05.1995)");
            }
        }
    }
}