package com.example.military.service;

import com.example.military.model.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String DELIMITER = ";";

    /**
     * Сохраняет список военнослужащих в файл
     * @param personnel список военнослужащих
     * @param fileName имя файла
     * @return true если сохранение успешно, false в случае ошибки
     */
    public static boolean saveToFile(List<MilitaryPerson> personnel, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {

            // Записываем заголовок с информацией
            writer.write("# Файл с данными военнослужащих");
            writer.newLine();
            writer.write("# Дата сохранения: " + LocalDate.now().format(DATE_FORMATTER));
            writer.newLine();
            writer.write("# Формат: Тип;Фамилия;Рота;Звание;Дата рождения;Дата поступления;Часть;Зарплата;[дополнительные поля]");
            writer.newLine();
            writer.write("=".repeat(100));
            writer.newLine();

            // Записываем каждого военнослужащего
            for (MilitaryPerson person : personnel) {
                String line = personToLine(person);
                writer.write(line);
                writer.newLine();
            }

            System.out.println("✅ Данные успешно сохранены в файл: " + fileName);
            System.out.println("   Всего записано: " + personnel.size() + " записей");
            return true;

        } catch (IOException e) {
            System.out.println("❌ Ошибка при сохранении в файл: " + e.getMessage());
            return false;
        }
    }

    /**
     * Загружает список военнослужащих из файла
     * @param fileName имя файла
     * @return список военнослужащих или null в случае ошибки
     */
    public static List<MilitaryPerson> loadFromFile(String fileName) {
        List<MilitaryPerson> personnel = new ArrayList<>();
        int lineNumber = 0;
        int successCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Пропускаем пустые строки и комментарии
                if (line.trim().isEmpty() || line.startsWith("#") || line.startsWith("=")) {
                    continue;
                }

                try {
                    MilitaryPerson person = lineToPerson(line);
                    if (person != null) {
                        personnel.add(person);
                        successCount++;
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠ Строка " + lineNumber + ": ошибка парсинга - " + e.getMessage());
                }
            }

            System.out.println("✅ Данные успешно загружены из файла: " + fileName);
            System.out.println("   Загружено записей: " + successCount + " из " + (lineNumber - countComments(fileName)));
            return personnel;

        } catch (FileNotFoundException e) {
            System.out.println("❌ Файл не найден: " + fileName);
            return null;
        } catch (IOException e) {
            System.out.println("❌ Ошибка при чтении файла: " + e.getMessage());
            return null;
        }
    }

    /**
     * Преобразует объект MilitaryPerson в строку для записи в файл
     */
    private static String personToLine(MilitaryPerson person) {
        StringBuilder sb = new StringBuilder();

        // Определяем тип объекта и базовые поля
        String type = getPersonType(person);
        sb.append(type).append(DELIMITER);
        sb.append(nullSafe(person.getLastName())).append(DELIMITER);
        sb.append(nullSafe(person.getCompany())).append(DELIMITER);
        sb.append(nullSafe(person.getRank())).append(DELIMITER);
        sb.append(formatDate(person.getBirthDate())).append(DELIMITER);
        sb.append(formatDate(person.getEnlistmentDate())).append(DELIMITER);
        sb.append(nullSafe(person.getUnit())).append(DELIMITER);
        sb.append(person.getSalary()).append(DELIMITER);

        // Добавляем специфичные для дочерних классов поля
        if (person instanceof MilitaryCommand) {
            MilitaryCommand cmd = (MilitaryCommand) person;
            sb.append(nullSafe(cmd.getMilitaryDistrict())).append(DELIMITER);
            sb.append(nullSafe(cmd.getPosition())).append(DELIMITER);
            sb.append(cmd.getYearsOfService()).append(DELIMITER);
            sb.append(cmd.getAllowance());
        }
        else if (person instanceof MilitaryContract) {
            MilitaryContract contract = (MilitaryContract) person;
            sb.append(nullSafe(contract.getContractPeriod())).append(DELIMITER);
            sb.append(formatDate(contract.getContractDate())).append(DELIMITER);
            sb.append(nullSafe(contract.getProtocolNumber())).append(DELIMITER);
            sb.append(contract.getSalary());
        }
        else if (person instanceof MilitaryAwarded) {
            MilitaryAwarded awarded = (MilitaryAwarded) person;
            sb.append(nullSafe(awarded.getAwardName())).append(DELIMITER);
            sb.append(awarded.getPrize()).append(DELIMITER);
            sb.append(awarded.getAllowance());
        }

        return sb.toString();
    }

    /**
     * Преобразует строку из файла в объект MilitaryPerson
     */
    private static MilitaryPerson lineToPerson(String line) {
        String[] parts = line.split(DELIMITER);
        if (parts.length < 8) {
            throw new IllegalArgumentException("Недостаточно полей в строке");
        }

        String type = parts[0];
        String lastName = parts[1];
        String company = parts[2];
        String rank = parts[3];
        LocalDate birthDate = parseDate(parts[4]);
        LocalDate enlistmentDate = parseDate(parts[5]);
        String unit = parts[6];
        double salary = Double.parseDouble(parts[7]);

        switch (type) {
            case "BASE":
                return new MilitaryPerson(lastName, company, rank, birthDate,
                        enlistmentDate, unit, salary);

            case "COMMAND":
                if (parts.length < 12) throw new IllegalArgumentException("Недостаточно полей для MilitaryCommand");
                return new MilitaryCommand(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        parts[8], parts[9], Integer.parseInt(parts[10]), Double.parseDouble(parts[11])
                );

            case "CONTRACT":
                if (parts.length < 11) throw new IllegalArgumentException("Недостаточно полей для MilitaryContract");
                return new MilitaryContract(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        parts[8], parseDate(parts[9]), parts[10]
                );

            case "AWARDED":
                if (parts.length < 11) throw new IllegalArgumentException("Недостаточно полей для MilitaryAwarded");
                return new MilitaryAwarded(
                        lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                        parts[8], Double.parseDouble(parts[9]), Double.parseDouble(parts[10])
                );

            default:
                throw new IllegalArgumentException("Неизвестный тип: " + type);
        }
    }

    /**
     * Определяет тип объекта MilitaryPerson
     */
    private static String getPersonType(MilitaryPerson person) {
        if (person instanceof MilitaryCommand) return "COMMAND";
        if (person instanceof MilitaryContract) return "CONTRACT";
        if (person instanceof MilitaryAwarded) return "AWARDED";
        return "BASE";
    }

    /**
     * Вспомогательные методы для работы с null и датами
     */
    private static String nullSafe(String value) {
        return value != null ? value : "";
    }

    private static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Подсчитывает количество комментариев в файле
     */
    private static int countComments(String fileName) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.startsWith("=")) {
                    count++;
                }
            }
        } catch (IOException e) {
            // Игнорируем ошибки при подсчете
        }
        return count;
    }

    /**
     * Проверяет существование файла
     */
    public static boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }

    /**
     * Получает список доступных файлов с данными
     */
    public static List<String> getDataFiles() {
        List<String> files = new ArrayList<>();
        File currentDir = new File(".");
        File[] fileList = currentDir.listFiles((dir, name) ->
                name.endsWith(".txt") || name.endsWith(".csv"));

        if (fileList != null) {
            for (File file : fileList) {
                files.add(file.getName());
            }
        }
        return files;
    }
}