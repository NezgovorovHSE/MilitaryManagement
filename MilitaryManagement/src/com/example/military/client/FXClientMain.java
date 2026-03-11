package com.example.military.client;

import com.example.military.client.AddPersonDialog;
import com.example.military.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import java.util.Timer;
import java.util.TimerTask;
import javafx.scene.layout.Region;
import java.util.HashMap;
import java.util.Map;

public class FXClientMain extends Application {

    private TableView<MilitaryPerson> table;
    private ObservableList<MilitaryPerson> personData;
    private ServerConnector connector;
    private Stage primaryStage;
    private ComboBox<String> filterCombo;
    private String currentFilterType = "Все записи";
    private ScheduledService<Void> backgroundService;
    private final Object dataLock = new Object();
    private boolean isRefreshing = false;
    private TextField searchField;
    private User currentUser;
    private Label userLabel;  // делаем поле класса

    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== start() вызван ===");
        System.out.println("Текущий поток: " + Thread.currentThread().getName());
        this.primaryStage = primaryStage;

        try {
            // Инициализация подключения к серверу
            connector = new ServerConnector();

            // Показываем окно входа
            User loggedUser = LoginDialog.show(primaryStage, connector);
            System.out.println("=== ВЕРНУЛИСЬ ИЗ LOGINDIALOG ===");
            System.out.println("loggedUser = " + (loggedUser != null ? loggedUser.getFullName() : "null"));

            if (loggedUser == null) {
                System.out.println("Пользователь отменил вход, выходим");
                Platform.exit();
                return;
            }
            System.out.println("Продолжаем с пользователем: " + loggedUser.getFullName());

            this.currentUser = loggedUser;

            List<MilitaryPerson> testList = connector.getAllPersons();
            System.out.println("Загружено записей после логина: " + (testList != null ? testList.size() : "null"));
            System.out.println("Вошёл пользователь: " + loggedUser.getFullName());

            searchField = new TextField();
            searchField.setPromptText("Поиск...");
            searchField.getStyleClass().add("search-field");
            searchField.setPrefWidth(200);

            startAutoRefresh(10);

// Инициализация фильтра (ВАЖНО: до loadData!)
            filterCombo = new ComboBox<>();
            filterCombo.getItems().addAll("Все записи", "Военнослужащие", "Командование", "Контрактники", "Награждённые");
            filterCombo.setValue("Все записи");
            filterCombo.setOnAction(e -> loadData());

// Добавляем слушатель на изменение текста
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                applySearchFilter(newValue);
            });

// Проверка подключения
            checkServerConnection();

// Создание таблицы
            table = new TableView<>();

// Обработчик двойного клика для редактирования
            table.setRowFactory(tv -> {
                TableRow<MilitaryPerson> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        System.out.println("Двойной клик по строке!");
                        MilitaryPerson selected = row.getItem();
                        if (connector.lockRecord(selected.getId())) {
                            showEditDialog(selected);
                        } else {
                            showInfoDialog(primaryStage, "Занято", "Запись редактируется другим пользователем");
                        }
                    }
                });
                return row;
            });

            updateTableColumns(currentFilterType);
            loadData();

// Верхняя панель с фильтром и кнопками в один ряд
            HBox topPanel = new HBox(20);
            topPanel.setPadding(new Insets(10));
            topPanel.setAlignment(Pos.CENTER_LEFT);

// Кнопки управления
            Button btnAdd = new Button("\uD83D\uDFA3 Добавить");
            Button btnDelete = new Button("✕ Удалить");
            Button btnImport = new Button("▴ Выгрузить из файла");
            btnImport.setOnAction(e -> showImportDialog());
            Button btnExport = new Button("▾ Сохранить");
            btnExport.setOnAction(e -> showExportDialog());

            btnAdd.setOnAction(e -> showAddDialog());
            btnDelete.setOnAction(e -> deleteSelected());

// Группа фильтра (слева)
            HBox filterGroup = new HBox(10);
            filterGroup.setAlignment(Pos.CENTER_LEFT);
            filterGroup.getChildren().addAll(new Label("Фильтр:"), filterCombo);

// Кнопка выхода и метка пользователя (справа)
            Button btnLogout = new Button("➜] Выйти");
            btnLogout.setOnAction(e -> logout());

            userLabel = new Label(currentUser.getFullName());  // без Label
            userLabel.setStyle("-fx-text-fill: #E5E9F0; -fx-font-weight: bold;");

// Группа кнопок операций
            HBox operationsGroup = new HBox(10);
            operationsGroup.setAlignment(Pos.CENTER_LEFT);
            operationsGroup.getChildren().addAll(btnAdd, btnDelete, btnImport, btnExport);

// Правая группа (пользователь + выход)
            HBox rightGroup = new HBox(10);
            rightGroup.setAlignment(Pos.CENTER_RIGHT);
            rightGroup.getChildren().addAll(userLabel, btnLogout);

// Растягивающийся разделитель (заполняет пространство между левой и правой группами)
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

// Собираем всё: фильтр → поиск → кнопки операций → разделитель → правая группа
            topPanel.getChildren().addAll(filterGroup, searchField, operationsGroup, spacer, rightGroup);

            BorderPane root = new BorderPane();
            root.setTop(topPanel);
            root.setCenter(table);

// Установка иконки приложения
            try {
                Image icon = new Image("file:star-icon.png");
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                System.out.println("Не удалось загрузить иконку: " + e.getMessage());
            }

// Сцена
            Scene scene = new Scene(root, 1750, 840);
            scene.getStylesheets().add("file:build/classes/com/example/military/client/style.css");
            primaryStage.setTitle("АРМ «Военный состав»");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private Timer refreshTimer;

    private void logout() {
        showConfirmDialog(primaryStage, "Подтверждение", "Вы уверены, что хотите выйти?", () -> {
            connector.logout();
            connector.reset();

            // Закрываем текущее окно
            primaryStage.close();

            // Показываем новое окно логина
            User newUser = LoginDialog.show(new Stage(), connector);
            if (newUser != null) {
                // Обновляем текущего пользователя
                this.currentUser = newUser;
                userLabel.setText(currentUser.getFullName());
                // Очищаем таблицу и перезагружаем данные
                loadData();
                // Показываем текущее окно заново
                primaryStage.show();
            } else {
                Platform.exit();
            }
        }); // ← закрываем вызов showConfirmDialog
    }

    private void startAutoRefresh(int seconds) {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    // Сохраняем текущее состояние сортировки
                    ObservableList<TableColumn<MilitaryPerson, ?>> sortOrder =
                            FXCollections.observableArrayList(table.getSortOrder());

                    // Сохраняем типы сортировки для каждой колонки
                    Map<TableColumn<MilitaryPerson, ?>, TableColumn.SortType> sortTypes = new HashMap<>();
                    for (TableColumn<MilitaryPerson, ?> col : sortOrder) {
                        sortTypes.put(col, col.getSortType());
                    }

                    String currentSearchText = searchField.getText();
                    String currentFilter = filterCombo.getValue();

                    List<MilitaryPerson> serverList = connector.getAllPersons();
                    if (serverList == null) return;

                    // Фильтруем по типу
                    List<MilitaryPerson> filteredByType = new ArrayList<>();
                    for (MilitaryPerson p : serverList) {
                        if (matchesFilter(p, currentFilter)) {
                            filteredByType.add(p);
                        }
                    }

                    // Применяем поиск
                    List<MilitaryPerson> finalList = filteredByType;
                    if (currentSearchText != null && !currentSearchText.trim().isEmpty()) {
                        String lowerSearch = currentSearchText.toLowerCase().trim();
                        finalList = new ArrayList<>();
                        for (MilitaryPerson p : filteredByType) {
                            if (matchesSearch(p, lowerSearch)) {
                                finalList.add(p);
                            }
                        }
                    }

                    // Присваиваем номера
                    for (int i = 0; i < finalList.size(); i++) {
                        finalList.get(i).setDisplayNumber(i + 1);
                    }

                    List<MilitaryPerson> currentDisplayList = new ArrayList<>(personData);

                    // Сравниваем списки
                    if (!listsAreEqual(currentDisplayList, finalList)) {
                        // Обновляем данные
                        personData.setAll(finalList);

                        // Восстанавливаем сортировку
                        if (!sortOrder.isEmpty()) {
                            table.getSortOrder().setAll(sortOrder);
                            for (TableColumn<MilitaryPerson, ?> col : sortOrder) {
                                TableColumn.SortType type = sortTypes.get(col);
                                if (type != null) {
                                    col.setSortType(type);
                                }
                            }
                            table.sort();
                        }
                    }
                });
            }
        }, 0, seconds * 1000);
    }

    @Override
    public void stop() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }

    private void checkForUpdates() {
        synchronized (dataLock) {
            if (isRefreshing) return;
            isRefreshing = true;
        }

        Platform.runLater(() -> {
            List<MilitaryPerson> currentList = new ArrayList<>(personData);
            List<MilitaryPerson> serverList = connector.getAllPersons();

            if (!listsAreEqual(currentList, serverList)) {
                loadData(); // обновляем таблицу
            }

            synchronized (dataLock) {
                isRefreshing = false;
            }
        });
    }

    private boolean listsAreEqual(List<MilitaryPerson> list1, List<MilitaryPerson> list2) {
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            MilitaryPerson p1 = list1.get(i);
            MilitaryPerson p2 = list2.get(i);

            // Сравниваем по ID и основным полям
            if (p1.getId() != p2.getId()) return false;
            if (!p1.getLastName().equals(p2.getLastName())) return false;
            if (!p1.getCompany().equals(p2.getCompany())) return false;
            if (!p1.getRank().equals(p2.getRank())) return false;
            if (p1.getSalary() != p2.getSalary()) return false;

            // Для дочерних классов добавляем проверку специфических полей
            if (p1 instanceof MilitaryCommand && p2 instanceof MilitaryCommand) {
                MilitaryCommand cmd1 = (MilitaryCommand) p1;
                MilitaryCommand cmd2 = (MilitaryCommand) p2;
                if (!cmd1.getMilitaryDistrict().equals(cmd2.getMilitaryDistrict())) return false;
                if (!cmd1.getPosition().equals(cmd2.getPosition())) return false;
                if (cmd1.getYearsOfService() != cmd2.getYearsOfService()) return false;
                if (cmd1.getAllowance() != cmd2.getAllowance()) return false;
            }
            // Добавь аналогично для других типов
        }
        return true;
    }

    private TableView<MilitaryPerson> createTable() {
        TableView<MilitaryPerson> tableView = new TableView<>();

        // Колонка № — выглядит как обычная, но сортирует по ID
        TableColumn<MilitaryPerson, Integer> numberCol = new TableColumn<>("№");
        numberCol.setCellValueFactory(cellData -> {
            int index = tableView.getItems().indexOf(cellData.getValue()) + 1;
            return new javafx.beans.property.ReadOnlyObjectWrapper<>(index);
        });
        numberCol.setPrefWidth(50);
        numberCol.setSortable(true);
        numberCol.setUserData(0); // 0 - ASC, 1 - DESC

        numberCol.sortTypeProperty().addListener((obs, oldType, newType) -> {
            if (newType == null) {
                // Достигнуто состояние без сортировки — переключаем на противоположное
                TableColumn.SortType next = (oldType == TableColumn.SortType.ASCENDING)
                        ? TableColumn.SortType.DESCENDING
                        : TableColumn.SortType.ASCENDING;
                // Используем Platform.runLater, чтобы избежать рекурсивного вызова
                Platform.runLater(() -> numberCol.setSortType(next));
            } else {
                // Нормальное изменение направления — переключаем userData
                int current = (int) numberCol.getUserData();
                numberCol.setUserData((current + 1) % 2);
            }
        });

        numberCol.setComparator((a, b) -> {
            int state = (int) numberCol.getUserData();

            MilitaryPerson p1 = tableView.getItems().get(a - 1);
            MilitaryPerson p2 = tableView.getItems().get(b - 1);

            if (state == 0) {
                return Integer.compare(p1.getId(), p2.getId()); // ASC
            } else {
                return Integer.compare(p2.getId(), p1.getId()); // DESC
            }
        });

        // Колонка ID (скрытая)
        TableColumn<MilitaryPerson, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        idCol.setVisible(true);
        idCol.setComparator((a, b) -> Integer.compare(a, b));

        // Остальные колонки
        TableColumn<MilitaryPerson, String> lastNameCol = new TableColumn<>("Фамилия");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lastNameCol.setPrefWidth(180);

        TableColumn<MilitaryPerson, String> companyCol = new TableColumn<>("Рота");
        companyCol.setCellValueFactory(new PropertyValueFactory<>("company"));
        companyCol.setPrefWidth(300);

        TableColumn<MilitaryPerson, String> rankCol = new TableColumn<>("Звание");
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankCol.setPrefWidth(180);

        TableColumn<MilitaryPerson, String> salaryCol = new TableColumn<>("Зарплата");
        salaryCol.setCellValueFactory(cellData -> {
            double salary = cellData.getValue().getSalary();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.0f", salary));
        });
        salaryCol.setPrefWidth(75);

        TableColumn<MilitaryPerson, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(cellData -> {
            MilitaryPerson p = cellData.getValue();
            String type = "Обычный";
            if (p instanceof MilitaryCommand) type = "Управление";
            else if (p instanceof MilitaryContract) type = "Контракт";
            else if (p instanceof MilitaryAwarded) type = "Награждён";
            return new javafx.beans.property.SimpleStringProperty(type);
        });
        typeCol.setPrefWidth(100);

        TableColumn<MilitaryPerson, String> birthDateCol = new TableColumn<>("Дата рождения");
        birthDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getBirthDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : ""
            );
        });
        birthDateCol.setPrefWidth(110);

        TableColumn<MilitaryPerson, String> enlistDateCol = new TableColumn<>("Дата поступления");
        enlistDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getEnlistmentDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : ""
            );
        });
        enlistDateCol.setPrefWidth(130);

        TableColumn<MilitaryPerson, String> unitCol = new TableColumn<>("Часть");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(60);

        TableColumn<MilitaryPerson, String> extraCol = new TableColumn<>("Доп. поля");
        extraCol.setCellValueFactory(cellData -> {
            MilitaryPerson p = cellData.getValue();
            String extraInfo = "";

            System.out.println("Тип объекта: " + p.getClass().getSimpleName());

            if (p instanceof MilitaryCommand) {
                MilitaryCommand cmd = (MilitaryCommand) p;
                extraInfo = cmd.getMilitaryDistrict() + ", " +
                        cmd.getPosition() + ", " +
                        cmd.getYearsOfService() + " лет, надб. " + cmd.getAllowance();
                System.out.println("Командование: " + extraInfo); // отладка
            } else if (p instanceof MilitaryContract) {
                MilitaryContract contract = (MilitaryContract) p;
                extraInfo = contract.getContractPeriod() + ", прот. " + contract.getProtocolNumber();
                System.out.println("Контракт: " + extraInfo); // отладка
            } else if (p instanceof MilitaryAwarded) {
                MilitaryAwarded awarded = (MilitaryAwarded) p;
                extraInfo = awarded.getAwardName() + ", премия " + awarded.getPrize() +
                        ", надб. " + awarded.getAllowance();
                System.out.println("Награда: " + extraInfo); // отладка
            }

            return new javafx.beans.property.SimpleStringProperty(extraInfo);
        });
        extraCol.setPrefWidth(250);

        tableView.getColumns().addAll(numberCol, idCol, lastNameCol, companyCol,
                rankCol, salaryCol, typeCol);

        // Двойной клик для деталей
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showDetailsDialog(tableView.getSelectionModel().getSelectedItem());
            }
        });

        return tableView;
    }

    private void checkServerConnection() {
        if (!connector.testConnection()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Предупреждение");
            alert.setHeaderText("Сервер недоступен");
            alert.setContentText("Не удалось подключиться к серверу. Данные могут быть недоступны.");
            if (primaryStage != null) {
                alert.initOwner(primaryStage);
            }
            alert.showAndWait();
        }
    }

    private void updateTableColumns(String filterType) {
        table.getColumns().clear();

        // Базовые колонки (есть всегда)
        TableColumn<MilitaryPerson, Integer> numberCol = new TableColumn<>("№");
        numberCol.setCellValueFactory(cellData -> {
            int index = table.getItems().indexOf(cellData.getValue()) + 1;
            return new javafx.beans.property.ReadOnlyObjectWrapper<>(index);
        });
        numberCol.setPrefWidth(50);

        TableColumn<MilitaryPerson, String> lastNameCol = new TableColumn<>("Фамилия");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lastNameCol.setPrefWidth(180);

        TableColumn<MilitaryPerson, String> companyCol = new TableColumn<>("Рота");
        companyCol.setCellValueFactory(new PropertyValueFactory<>("company"));
        companyCol.setPrefWidth(300);

        TableColumn<MilitaryPerson, String> rankCol = new TableColumn<>("Звание");
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankCol.setPrefWidth(180);

        TableColumn<MilitaryPerson, String> birthDateCol = new TableColumn<>("Дата рождения");
        birthDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getBirthDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : ""
            );
        });
        birthDateCol.setPrefWidth(110);

        TableColumn<MilitaryPerson, String> enlistDateCol = new TableColumn<>("Дата поступления");
        enlistDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getEnlistmentDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : ""
            );
        });
        enlistDateCol.setPrefWidth(130);

        TableColumn<MilitaryPerson, String> unitCol = new TableColumn<>("Часть");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(60);

        TableColumn<MilitaryPerson, String> salaryCol = new TableColumn<>("Зарплата");
        salaryCol.setCellValueFactory(cellData -> {
            double salary = cellData.getValue().getSalary();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.0f", salary));
        });
        salaryCol.setPrefWidth(75);

        // Добавляем базовые колонки
        table.getColumns().addAll(numberCol, lastNameCol, companyCol, rankCol,
                birthDateCol, enlistDateCol, unitCol, salaryCol);

        // Добавляем специфические колонки в зависимости от типа
        if (filterType.equals("Командование")) {
            TableColumn<MilitaryPerson, String> districtCol = new TableColumn<>("Округ");
            districtCol.setCellValueFactory(cellData -> {
                MilitaryCommand cmd = (MilitaryCommand) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(cmd.getMilitaryDistrict());
            });
            districtCol.setPrefWidth(300);

            TableColumn<MilitaryPerson, String> positionCol = new TableColumn<>("Должность");
            positionCol.setCellValueFactory(cellData -> {
                MilitaryCommand cmd = (MilitaryCommand) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(cmd.getPosition());
            });
            positionCol.setPrefWidth(180);

            TableColumn<MilitaryPerson, Integer> yearsCol = new TableColumn<>("Выслуга");
            yearsCol.setCellValueFactory(cellData -> {
                MilitaryCommand cmd = (MilitaryCommand) cellData.getValue();
                return new javafx.beans.property.SimpleIntegerProperty(cmd.getYearsOfService()).asObject();
            });
            yearsCol.setPrefWidth(70);

            TableColumn<MilitaryPerson, String> cmdAllowanceCol = new TableColumn<>("Надбавка");
            cmdAllowanceCol.setCellValueFactory(cellData -> {
                MilitaryCommand cmd = (MilitaryCommand) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(
                        String.format("%.0f", cmd.getAllowance())
                );
            });
            cmdAllowanceCol.setPrefWidth(70);

            table.getColumns().addAll(districtCol, positionCol, yearsCol);
        }
        else if (filterType.equals("Контрактники")) {
            // колонки для контрактников
            TableColumn<MilitaryPerson, String> periodCol = new TableColumn<>("Период");
            periodCol.setCellValueFactory(cellData -> {
                MilitaryContract contract = (MilitaryContract) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(contract.getContractPeriod());
            });
            periodCol.setPrefWidth(150);

            TableColumn<MilitaryPerson, String> protocolCol = new TableColumn<>("Протокол");
            protocolCol.setCellValueFactory(cellData -> {
                MilitaryContract contract = (MilitaryContract) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(contract.getProtocolNumber());
            });
            protocolCol.setPrefWidth(80);

            table.getColumns().addAll(periodCol, protocolCol);
        }
        else if (filterType.equals("Награждённые")) {
            // колонки для награждённых
            TableColumn<MilitaryPerson, String> awardCol = new TableColumn<>("Награда");
            awardCol.setCellValueFactory(cellData -> {
                MilitaryAwarded awarded = (MilitaryAwarded) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(awarded.getAwardName());
            });
            awardCol.setPrefWidth(250);

            TableColumn<MilitaryPerson, String> prizeCol = new TableColumn<>("Премия");
            prizeCol.setCellValueFactory(cellData -> {
                MilitaryAwarded awarded = (MilitaryAwarded) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(
                        String.format("%.0f", awarded.getPrize())
                );
            });
            prizeCol.setPrefWidth(70);

            TableColumn<MilitaryPerson, String> allowanceCol = new TableColumn<>("Надбавка");
            allowanceCol.setCellValueFactory(cellData -> {
                MilitaryAwarded awarded = (MilitaryAwarded) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(
                        String.format("%.0f", awarded.getAllowance())
                );
            });
            allowanceCol.setPrefWidth(80);

            table.getColumns().addAll(awardCol, prizeCol, allowanceCol);
        }
    }

    private void loadData() {
        List<MilitaryPerson> allList = connector.getAllPersons();
        if (allList == null) return;

        currentFilterType = filterCombo.getValue();
        List<MilitaryPerson> filteredList = new ArrayList<>();

        for (MilitaryPerson p : allList) {
            if (matchesFilter(p, currentFilterType)) {
                filteredList.add(p);
            }
        }

        // Применяем текущий поиск, если есть
        String currentSearch = searchField.getText();
        if (currentSearch != null && !currentSearch.trim().isEmpty()) {
            List<MilitaryPerson> searchResults = new ArrayList<>();
            String lowerSearch = currentSearch.toLowerCase().trim();
            for (MilitaryPerson p : filteredList) {
                if (matchesSearch(p, lowerSearch)) {
                    searchResults.add(p);
                }
            }
            filteredList = searchResults;
        }

        // Присваиваем порядковые номера
        for (int i = 0; i < filteredList.size(); i++) {
            filteredList.get(i).setDisplayNumber(i + 1);
        }

        // Перестраиваем колонки перед обновлением данных
        updateTableColumns(currentFilterType);

        if (personData == null) {
            personData = FXCollections.observableArrayList(filteredList);
            table.setItems(personData);
        } else {
            personData.setAll(filteredList);
        }
    }

    private void showAlert(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.showAndWait();
    }

    private void showEditDialog(MilitaryPerson person) {
        // Будем использовать ту же форму, но с предзаполненными полями
        AddPersonDialog.show(primaryStage, connector, person, this::loadData);
    }

    private void deleteSelected() {
        MilitaryPerson selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfoDialog(primaryStage, "Предупреждение", "Выберите запись для удаления");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить " + selected.getLastName() + " (ID: " + selected.getId() + ")?");

        confirm.initOwner(primaryStage);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean deleted = connector.deletePerson(selected.getId());
            if (deleted) {
                showInfoDialog(primaryStage, "Успех", "Запись удалена");
                loadData();
            } else {
                showInfoDialog(primaryStage, "Ошибка", "Не удалось удалить запись");
            }
        }
    }

    private void showAddDialog() {
        AddPersonDialog.show(primaryStage, connector, null, this::loadData);
    }

    private void showDetailsDialog(MilitaryPerson person) {
        if (person == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Детальная информация");
        alert.setHeaderText(person.getLastName() + " (" + person.getRank() + ")");

        StringBuilder content = new StringBuilder();
        content.append("Фамилия: ").append(person.getLastName()).append("\n");
        content.append("Рота: ").append(person.getCompany()).append("\n");
        content.append("TableColumn<>(\"Зарплата\");: ").append(person.getRank()).append("\n");
        content.append("Зарплата: ").append(person.getSalary()).append("\n");

        if (person instanceof MilitaryCommand) {
            MilitaryCommand cmd = (MilitaryCommand) person;
            content.append("Округ: ").append(cmd.getMilitaryDistrict()).append("\n");
            content.append("Должность: ").append(cmd.getPosition()).append("\n");
            content.append("Выслуга: ").append(cmd.getYearsOfService()).append("\n");
            content.append("Надбавка: ").append(cmd.getAllowance()).append("\n");
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private void applySearchFilter(String searchText) {
        if (personData == null) return;

        // Сохраняем текущий фильтр
        currentFilterType = filterCombo.getValue();

        // Получаем все данные
        List<MilitaryPerson> allData = connector.getAllPersons();
        if (allData == null) return;

        // Сначала фильтруем по типу
        List<MilitaryPerson> filteredByType = new ArrayList<>();
        for (MilitaryPerson p : allData) {
            if (matchesFilter(p, currentFilterType)) {
                filteredByType.add(p);
            }
        }

        // Если поиск пустой - показываем отфильтрованные по типу
        if (searchText == null || searchText.trim().isEmpty()) {
            for (int i = 0; i < filteredByType.size(); i++) {
                filteredByType.get(i).setDisplayNumber(i + 1);
            }

            // Перестраиваем колонки перед обновлением данных
            updateTableColumns(currentFilterType);
            personData.setAll(filteredByType);
            return;
        }

        // Применяем поиск
        String lowerSearch = searchText.toLowerCase().trim();
        List<MilitaryPerson> searchResults = new ArrayList<>();

        for (MilitaryPerson p : filteredByType) {
            if (matchesSearch(p, lowerSearch)) {
                searchResults.add(p);
            }
        }

        // Присваиваем номера
        for (int i = 0; i < searchResults.size(); i++) {
            searchResults.get(i).setDisplayNumber(i + 1);
        }

        // Перестраиваем колонки перед обновлением данных
        updateTableColumns(currentFilterType);
        personData.setAll(searchResults);
    }

    private void showInfoDialog(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Применяем стиль
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add("file:build/classes/com/example/military/client/style.css");
        dialogPane.getStyleClass().add("info-dialog");

        // Центрируем
        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.showAndWait();
    }

    private void showConfirmDialog(Stage owner, String title, String message, Runnable onConfirm) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Применяем стиль
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add("file:build/classes/com/example/military/client/style.css");
        dialogPane.getStyleClass().add("info-dialog");

        // Центрируем
        if (owner != null) {
            alert.initOwner(owner);
        }

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onConfirm.run();
        }
    }

    private boolean matchesFilter(MilitaryPerson p, String filter) {
        if (filter.equals("Все записи")) return true;
        if (filter.equals("Военнослужащие") && p.getClass() == MilitaryPerson.class) return true;
        if (filter.equals("Командование") && p instanceof MilitaryCommand) return true;
        if (filter.equals("Контрактники") && p instanceof MilitaryContract) return true;
        if (filter.equals("Награждённые") && p instanceof MilitaryAwarded) return true;
        return false;
    }

    private boolean matchesSearch(MilitaryPerson p, String searchText) {
        // Поиск по строковым полям
        if (p.getLastName() != null && p.getLastName().toLowerCase().contains(searchText)) return true;
        if (p.getCompany() != null && p.getCompany().toLowerCase().contains(searchText)) return true;
        if (p.getRank() != null && p.getRank().toLowerCase().contains(searchText)) return true;
        if (p.getUnit() != null && p.getUnit().toLowerCase().contains(searchText)) return true;
        if (String.valueOf(p.getSalary()).contains(searchText)) return true;

        // Поиск по датам
        if (p.getBirthDate() != null) {
            String birthDateStr = p.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            if (birthDateStr.contains(searchText)) return true;
        }

        if (p.getEnlistmentDate() != null) {
            String enlistDateStr = p.getEnlistmentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            if (enlistDateStr.contains(searchText)) return true;
        }

        // Поля дочерних классов
        if (p instanceof MilitaryCommand) {
            MilitaryCommand cmd = (MilitaryCommand) p;
            if (cmd.getMilitaryDistrict() != null && cmd.getMilitaryDistrict().toLowerCase().contains(searchText)) return true;
            if (cmd.getPosition() != null && cmd.getPosition().toLowerCase().contains(searchText)) return true;
            if (String.valueOf(cmd.getYearsOfService()).contains(searchText)) return true;
            if (String.valueOf(cmd.getAllowance()).contains(searchText)) return true;
        }

        if (p instanceof MilitaryContract) {
            MilitaryContract contract = (MilitaryContract) p;
            if (contract.getContractPeriod() != null && contract.getContractPeriod().toLowerCase().contains(searchText)) return true;
            if (contract.getProtocolNumber() != null && contract.getProtocolNumber().toLowerCase().contains(searchText)) return true;
            if (contract.getContractDate() != null) {
                String contractDateStr = contract.getContractDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                if (contractDateStr.contains(searchText)) return true;
            }
        }

        if (p instanceof MilitaryAwarded) {
            MilitaryAwarded awarded = (MilitaryAwarded) p;
            if (awarded.getAwardName() != null && awarded.getAwardName().toLowerCase().contains(searchText)) return true;
            if (String.valueOf(awarded.getPrize()).contains(searchText)) return true;
            if (String.valueOf(awarded.getAllowance()).contains(searchText)) return true;
        }

        return false;
    }

    private void showImportDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для импорта");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            int added = connector.importFromFile(file.getAbsolutePath());
            if (added >= 0) {
                showInfoDialog(primaryStage, "Импорт завершен", "Добавлено записей: " + added);
                loadData();
            } else {
                showInfoDialog(primaryStage, "Ошибка", "Не удалось импортировать файл");
            }
        }
    }

    private void showExportDialog() {
        if (personData == null || personData.isEmpty()) {
            showInfoDialog(primaryStage, "Экспорт", "Нет данных для экспорта");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить как");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        fileChooser.setInitialFileName("military_export.csv");

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            boolean success = connector.exportToFile(file.getAbsolutePath(),
                    new ArrayList<>(personData));
            if (success) {
                showInfoDialog(primaryStage, "Экспорт завершен", "Файл сохранен:\n" + file.getAbsolutePath());
            } else {
                showInfoDialog(primaryStage, "Ошибка", "Не удалось экспортировать данные");
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

