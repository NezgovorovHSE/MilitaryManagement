package com.example.military.client;

import com.example.military.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddPersonDialog {

    public static void show(Stage owner, ServerConnector connector,
                            MilitaryPerson existingPerson, Runnable onSuccess) {
        boolean isEditMode = (existingPerson != null);

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(isEditMode ? "Редактирование военнослужащего" : "Добавление военнослужащего");

        try {
            Image icon = new Image("file:star-icon.png");
            dialog.getIcons().add(icon);
        } catch (Exception e) {
            // игнорируем, если файл не найден
        }

        if (isEditMode) {
            dialog.setOnCloseRequest(event -> {
                connector.unlockRecord(existingPerson.getId());
            });
        }

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Верхняя часть – выбор типа
        VBox topBox = new VBox(5);
        topBox.setPadding(new Insets(0, 0, 10, 0));

        Label typeLabel = new Label("Тип военнослужащего:");
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Военнослужащие", "Командование", "Контрактники", "Награждённые");

        HBox typeBoxContainer = new HBox(10);
        typeBoxContainer.getChildren().add(typeBox);

        // Правая колонка (StackPane, чтобы поля не накладывались, а переключались)
        StackPane rightStack = new StackPane();
        rightStack.setPrefWidth(350);
        //rightStack.setPrefHeight(312);
        rightStack.setMaxHeight(312);
        rightStack.setMinHeight(312);
        rightStack.setStyle("-fx-border-color: #ccc; -fx-border-width: 0 0 0 1; -fx-padding: 0 0 0 20;");
        rightStack.setVisible(false); // изначально скрыта

        // Поля для командования
        GridPane commandGrid = new GridPane();
        commandGrid.setHgap(10);
        commandGrid.setVgap(10);
        commandGrid.getColumnConstraints().addAll(
                new ColumnConstraints(160),
                new ColumnConstraints(300)
        );

        // Поля для контрактников
        GridPane contractGrid = new GridPane();
        contractGrid.setHgap(10);
        contractGrid.setVgap(10);
        contractGrid.getColumnConstraints().addAll(
                new ColumnConstraints(160),
                new ColumnConstraints(300)
        );

        // Поля для награждённых
        GridPane awardedGrid = new GridPane();
        awardedGrid.setHgap(10);
        awardedGrid.setVgap(10);
        awardedGrid.getColumnConstraints().addAll(
                new ColumnConstraints(160),
                new ColumnConstraints(300)
        );

        if (isEditMode) {
            if (existingPerson instanceof MilitaryCommand) typeBox.setValue("Командование");
            else if (existingPerson instanceof MilitaryContract) typeBox.setValue("Контрактники");
            else if (existingPerson instanceof MilitaryAwarded) typeBox.setValue("Награждённые");
            else typeBox.setValue("Военнослужащие");

            typeBox.setDisable(true);
            Button changeTypeBtn = new Button("Изменить тип");
            changeTypeBtn.setOnAction(e -> {
                typeBox.setDisable(false);
                changeTypeBtn.setDisable(true);
                // Скрываем все дочерние GridPane
                commandGrid.setVisible(false);
                contractGrid.setVisible(false);
                awardedGrid.setVisible(false);
                // Очищаем правую колонку
                String currentType = typeBox.getValue();

                if (currentType.equals("Командование")) {
                    commandGrid.setVisible(true);
                    rightStack.setVisible(true);
                    rightStack.setManaged(true);
                } else if (currentType.equals("Контрактники")) {
                    contractGrid.setVisible(true);
                    rightStack.setVisible(true);
                    rightStack.setManaged(true);
                } else if (currentType.equals("Награждённые")) {
                    awardedGrid.setVisible(true);
                    rightStack.setVisible(true);
                    rightStack.setManaged(true);
                } else {
                    // Если тип "Военнослужащие" — скрываем правую колонку
                    rightStack.setVisible(false);
                    rightStack.setManaged(false);
                }
            });
            typeBoxContainer.getChildren().add(changeTypeBtn);
        } else {
            typeBox.setValue("Военнослужащие");
        }
        topBox.getChildren().addAll(typeLabel, typeBoxContainer);
        root.setTop(topBox);

        // Центральная часть – левая колонка (общие поля) и правая колонка (дочерние поля)
        HBox centerBox = new HBox(20);
        centerBox.setPadding(new Insets(0, 0, 5, 0));

        // Левая колонка
        GridPane leftGrid = new GridPane();
        leftGrid.setHgap(10);
        leftGrid.setVgap(10);
        leftGrid.setPrefWidth(350);
        leftGrid.getColumnConstraints().addAll(
                new ColumnConstraints(160),
                new ColumnConstraints(300)
        );

        TextField lastNameField = new TextField();
        TextField companyField = new TextField();
        TextField rankField = new TextField();
        TextField salaryField = new TextField();
        TextField birthDateField = new TextField();
        birthDateField.setPromptText("дд.мм.гггг");
        TextField enlistmentDateField = new TextField();
        enlistmentDateField.setPromptText("дд.мм.гггг");
        TextField unitField = new TextField();

        leftGrid.add(new Label("Фамилия*:"), 0, 0);
        leftGrid.add(lastNameField, 1, 0);
        leftGrid.add(new Label("Рота*:"), 0, 1);
        leftGrid.add(companyField, 1, 1);
        leftGrid.add(new Label("Звание*:"), 0, 2);
        leftGrid.add(rankField, 1, 2);
        leftGrid.add(new Label("Зарплата*:"), 0, 3);
        leftGrid.add(salaryField, 1, 3);
        leftGrid.add(new Label("Дата рождения:"), 0, 4);
        leftGrid.add(birthDateField, 1, 4);
        leftGrid.add(new Label("Дата поступления:"), 0, 5);
        leftGrid.add(enlistmentDateField, 1, 5);
        leftGrid.add(new Label("Часть:"), 0, 6);
        leftGrid.add(unitField, 1, 6);

        TextField districtField = new TextField();
        TextField positionField = new TextField();
        TextField yearsField = new TextField();
        TextField cmdAllowanceField = new TextField();
        commandGrid.add(new Label("Округ*:"), 0, 0);
        commandGrid.add(districtField, 1, 0);
        commandGrid.add(new Label("Должность*:"), 0, 1);
        commandGrid.add(positionField, 1, 1);
        commandGrid.add(new Label("Выслуга лет*:"), 0, 2);
        commandGrid.add(yearsField, 1, 2);
        commandGrid.add(new Label("Надбавка*:"), 0, 3);
        commandGrid.add(cmdAllowanceField, 1, 3);

        TextField periodField = new TextField();
        TextField contractDateField = new TextField();
        contractDateField.setPromptText("дд.мм.гггг");
        TextField protocolField = new TextField();
        contractGrid.add(new Label("Период*:"), 0, 0);
        contractGrid.add(periodField, 1, 0);
        contractGrid.add(new Label("Дата договора:"), 0, 1);
        contractGrid.add(contractDateField, 1, 1);
        contractGrid.add(new Label("Номер протокола*:"), 0, 2);
        contractGrid.add(protocolField, 1, 2);

        TextField awardNameField = new TextField();
        TextField prizeField = new TextField();
        TextField awardedAllowanceField = new TextField();
        awardedGrid.add(new Label("Награда*:"), 0, 0);
        awardedGrid.add(awardNameField, 1, 0);
        awardedGrid.add(new Label("Премия*:"), 0, 1);
        awardedGrid.add(prizeField, 1, 1);
        awardedGrid.add(new Label("Надбавка*:"), 0, 2);
        awardedGrid.add(awardedAllowanceField, 1, 2);

        // Добавляем все дочерние GridPane в StackPane (они будут накладываться)
        rightStack.getChildren().addAll(commandGrid, contractGrid, awardedGrid);

        // Собираем центр
        centerBox.getChildren().addAll(leftGrid, rightStack);
        root.setCenter(centerBox);

        // Кнопки
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 5, 0));
        buttonBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Сохранить");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.setOnAction(e -> {
            if (isEditMode) connector.unlockRecord(existingPerson.getId());
            dialog.close();
        });

        buttonBox.getChildren().addAll(saveBtn, cancelBtn);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(1000);
        errorLabel.setVisible(false);

        VBox bottomBox = new VBox(5);
        bottomBox.getChildren().addAll(errorLabel, buttonBox);
        root.setBottom(bottomBox);

        // Обработчик выбора типа (для режима добавления)
        typeBox.setOnAction(ev -> {
            String selected = typeBox.getValue();

            // Скрываем все GridPane в StackPane
            commandGrid.setVisible(false);
            contractGrid.setVisible(false);
            awardedGrid.setVisible(false);

            if (selected.equals("Военнослужащие")) {
                rightStack.setVisible(false);
            } else {
                rightStack.setVisible(true);
                switch (selected) {
                    case "Командование":
                        commandGrid.setVisible(true);
                        break;
                    case "Контрактники":
                        contractGrid.setVisible(true);
                        break;
                    case "Награждённые":
                        awardedGrid.setVisible(true);
                        break;
                }
            }
        });

        // Если редактирование – заполняем поля и показываем нужный GridPane
        if (isEditMode) {
            lastNameField.setText(existingPerson.getLastName());
            companyField.setText(existingPerson.getCompany());
            rankField.setText(existingPerson.getRank());
            salaryField.setText(String.valueOf(existingPerson.getSalary()));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            if (existingPerson.getBirthDate() != null) {
                birthDateField.setText(existingPerson.getBirthDate().format(formatter));
            }
            if (existingPerson.getEnlistmentDate() != null) {
                enlistmentDateField.setText(existingPerson.getEnlistmentDate().format(formatter));
            }
            unitField.setText(existingPerson.getUnit());

            // Показываем нужный дочерний GridPane
            rightStack.setVisible(true);
            commandGrid.setVisible(false);
            contractGrid.setVisible(false);
            awardedGrid.setVisible(false);

            if (existingPerson instanceof MilitaryCommand) {
                commandGrid.setVisible(true);
                MilitaryCommand cmd = (MilitaryCommand) existingPerson;
                districtField.setText(cmd.getMilitaryDistrict());
                positionField.setText(cmd.getPosition());
                yearsField.setText(String.valueOf(cmd.getYearsOfService()));
                cmdAllowanceField.setText(String.valueOf(cmd.getAllowance()));
            } else if (existingPerson instanceof MilitaryContract) {
                contractGrid.setVisible(true);
                MilitaryContract contract = (MilitaryContract) existingPerson;
                periodField.setText(contract.getContractPeriod());
                if (contract.getContractDate() != null) {
                    contractDateField.setText(contract.getContractDate().format(formatter));
                }
                protocolField.setText(contract.getProtocolNumber());
            } else if (existingPerson instanceof MilitaryAwarded) {
                awardedGrid.setVisible(true);
                MilitaryAwarded awarded = (MilitaryAwarded) existingPerson;
                awardNameField.setText(awarded.getAwardName());
                prizeField.setText(String.valueOf(awarded.getPrize()));
                awardedAllowanceField.setText(String.valueOf(awarded.getAllowance()));
            }
        }

        // Логика сохранения (сокращена для ясности, но должна быть полная)
        saveBtn.setOnAction(e -> {
            try {
                String selectedType = typeBox.getValue();
                String lastName = lastNameField.getText().trim();
                String company = companyField.getText().trim();
                String rank = rankField.getText().trim();
                String salaryText = salaryField.getText().trim();

                // Валидация форматов
                String errors = validateFields(
                        selectedType,
                        birthDateField.getText().trim(),
                        enlistmentDateField.getText().trim(),
                        contractDateField.getText().trim(),
                        salaryField.getText().trim(),
                        yearsField.getText().trim(),
                        cmdAllowanceField.getText().trim(),
                        prizeField.getText().trim(),
                        awardedAllowanceField.getText().trim()
                );

                if (!errors.isEmpty()) {
                    errorLabel.setText("Неверный формат: " + errors);
                    errorLabel.setVisible(true);
                    return;
                } else {
                    errorLabel.setVisible(false);
                }

                if (lastName.isEmpty() || company.isEmpty() || rank.isEmpty() || salaryText.isEmpty()) {
                    showAlert(owner, "Ошибка", "Заполните обязательные поля");
                    return;
                }

                double salary = Double.parseDouble(salaryText);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                LocalDate birthDate = null;
                LocalDate enlistmentDate = null;
                if (!birthDateField.getText().trim().isEmpty()) {
                    birthDate = LocalDate.parse(birthDateField.getText().trim(), formatter);
                }
                if (!enlistmentDateField.getText().trim().isEmpty()) {
                    enlistmentDate = LocalDate.parse(enlistmentDateField.getText().trim(), formatter);
                }
                String unit = unitField.getText().trim();

                MilitaryPerson personToSave = null;

                switch (selectedType) {
                    case "Командование":
                        if (districtField.getText().trim().isEmpty() ||
                                positionField.getText().trim().isEmpty() ||
                                yearsField.getText().trim().isEmpty() ||
                                cmdAllowanceField.getText().trim().isEmpty()) {
                            showAlert(owner, "Ошибка", "Заполните все поля командования");
                            return;
                        }
                        int years = Integer.parseInt(yearsField.getText().trim());
                        double cmdAllowance = Double.parseDouble(cmdAllowanceField.getText().trim());
                        personToSave = new MilitaryCommand(
                                lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                                districtField.getText().trim(),
                                positionField.getText().trim(),
                                years, cmdAllowance
                        );
                        break;
                    case "Контрактники":
                        if (periodField.getText().trim().isEmpty() ||
                                protocolField.getText().trim().isEmpty()) {
                            showAlert(owner, "Ошибка", "Заполните обязательные поля контрактника");
                            return;
                        }
                        LocalDate contractDate = null;
                        if (!contractDateField.getText().trim().isEmpty()) {
                            contractDate = LocalDate.parse(contractDateField.getText().trim(), formatter);
                        }
                        personToSave = new MilitaryContract(
                                lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                                periodField.getText().trim(),
                                contractDate,
                                protocolField.getText().trim()
                        );
                        break;
                    case "Награждённые":
                        if (awardNameField.getText().trim().isEmpty() ||
                                prizeField.getText().trim().isEmpty() ||
                                awardedAllowanceField.getText().trim().isEmpty()) {
                            showAlert(owner, "Ошибка", "Заполните все поля награждённого");
                            return;
                        }
                        double prize = Double.parseDouble(prizeField.getText().trim());
                        double awdAllowance = Double.parseDouble(awardedAllowanceField.getText().trim());
                        personToSave = new MilitaryAwarded(
                                lastName, company, rank, birthDate, enlistmentDate, unit, salary,
                                awardNameField.getText().trim(),
                                prize, awdAllowance
                        );
                        break;
                    default:
                        personToSave = new MilitaryPerson(
                                lastName, company, rank, birthDate, enlistmentDate, unit, salary
                        );
                }

                if (isEditMode) {
                    personToSave.setId(existingPerson.getId());
                    boolean updated = connector.updatePerson(personToSave);
                    if (updated) {
                        connector.unlockRecord(existingPerson.getId());
                        dialog.close();
                        if (onSuccess != null) onSuccess.run();
                        showAlert(owner, "", "Запись успешно обновлена");
                    } else {
                        showAlert(owner, "Ошибка", "Не удалось обновить запись");
                    }
                } else {
                    int id = connector.addPerson(personToSave);
                    if (id > 0) {
                        dialog.close();
                        if (onSuccess != null) onSuccess.run();
                        showAlert(owner, "","Запись успешно создана");
                    } else {
                        showAlert(owner, "Ошибка", "Не удалось создать запись");
                    }
                }
            } catch (Exception ex) {
                errorLabel.setText("Ошибка: " + ex.getMessage());
                errorLabel.setVisible(true);
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(root, 1000, 508);
        scene.getStylesheets().add("file:build/classes/com/example/military/client/style.css");
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static String validateFields(String selectedType,
                                         String birthDateText, String enlistmentDateText,
                                         String contractDateText,
                                         String salaryText, String yearsText,
                                         String cmdAllowanceText, String prizeText,
                                         String awardedAllowanceText) {
        List<String> errors = new ArrayList<>();

        // Проверка дат
        if (!birthDateText.isEmpty() && !isValidDate(birthDateText)) {
            errors.add("Дата рождения - дд.мм.гггг");
        }
        if (!enlistmentDateText.isEmpty() && !isValidDate(enlistmentDateText)) {
            errors.add("Дата поступления - дд.мм.гггг");
        }
        if (selectedType.equals("Контрактники") &&
                !contractDateText.isEmpty() && !isValidDate(contractDateText)) {
            errors.add("Дата договора - дд.мм.гггг");
        }

        // Проверка чисел
        if (!isValidNumber(salaryText)) {
            errors.add("Зарплата - число");
        }
        if (selectedType.equals("Командование")) {
            if (!isValidInteger(yearsText)) {
                errors.add("Выслуга лет - целое число");
            }
            if (!isValidNumber(cmdAllowanceText)) {
                errors.add("Надбавка - число");
            }
        }
        if (selectedType.equals("Награждённые")) {
            if (!isValidNumber(prizeText)) {
                errors.add("Премия - число");
            }
            if (!isValidNumber(awardedAllowanceText)) {
                errors.add("Надбавка - число");
            }
        }

        return String.join("; ", errors);
    }

    private static boolean isValidDate(String text) {
        try {
            DateTimeFormatter.ofPattern("dd.MM.yyyy").parse(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidNumber(String text) {
        try {
            Double.parseDouble(text.replace(",", "."));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidInteger(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void showAlert(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }

        alert.showAndWait();
    }
}
