package com.example.military.client;
//TEST
import com.example.military.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Map;

public class FXClientMain extends Application {
//TEST
    private TableView<MilitaryPerson> table;
    private ObservableList<MilitaryPerson> personData;
    private ServerConnector connector;
    private Stage primaryStage;
    private ComboBox<String> filterCombo;
    private String currentFilterType = "Все записи";
    private TextField searchField;
    private User currentUser;
    private Label userLabel;
    private Timer refreshTimer;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            connector = new ServerConnector();

            User loggedUser = LoginDialog.show(primaryStage, connector);

            if (loggedUser == null) {
                Platform.exit();
                return;
            }

            this.currentUser = loggedUser;

            connector.getAllPersons();

            searchField = new TextField();
            searchField.setPromptText("Поиск...");
            searchField.getStyleClass().add("search-field");
            searchField.setPrefWidth(200);

            startAutoRefresh(10);

            filterCombo = new ComboBox<>();
            filterCombo.getItems().addAll("Все записи", "Военнослужащие", "Командование", "Контрактники", "Награждённые");
            filterCombo.setValue("Все записи");
            filterCombo.setOnAction(e -> loadData());

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                applySearchFilter(newValue);
            });

            checkServerConnection();
            table = new TableView<>();

            table.setRowFactory(tv -> {
                TableRow<MilitaryPerson> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
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

            HBox topPanel = new HBox(20);
            topPanel.setPadding(new Insets(10));
            topPanel.setAlignment(Pos.CENTER_LEFT);

            Button btnAdd = new Button("\uD83D\uDFA3 Добавить");
            Button btnDelete = new Button("✕ Удалить");
            Button btnImport = new Button("▴ Выгрузить из файла");
            btnImport.setOnAction(e -> showImportDialog());
            Button btnExport = new Button("▾ Сохранить");
            btnExport.setOnAction(e -> showExportDialog());

            btnAdd.setOnAction(e -> showAddDialog());
            btnDelete.setOnAction(e -> deleteSelected());

            HBox filterGroup = new HBox(10);
            filterGroup.setAlignment(Pos.CENTER_LEFT);
            filterGroup.getChildren().addAll(new Label("Фильтр:"), filterCombo);

            Button btnLogout = new Button("➜] Выйти");
            btnLogout.setOnAction(e -> logout());

            userLabel = new Label(currentUser.getFullName());
            userLabel.setStyle("-fx-text-fill: #E5E9F0; -fx-font-weight: bold;");

            HBox operationsGroup = new HBox(10);
            operationsGroup.setAlignment(Pos.CENTER_LEFT);
            operationsGroup.getChildren().addAll(btnAdd, btnDelete, btnImport, btnExport);

            HBox rightGroup = new HBox(10);
            rightGroup.setAlignment(Pos.CENTER_RIGHT);
            rightGroup.getChildren().addAll(userLabel, btnLogout);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            topPanel.getChildren().addAll(filterGroup, searchField, operationsGroup, spacer, rightGroup);

            BorderPane root = new BorderPane();
            root.setTop(topPanel);
            root.setCenter(table);

            try {
                InputStream iconStream = getClass().getResourceAsStream("/com/example/military/client/star-icon.png");
                if (iconStream != null) {
                    Image icon = new Image(iconStream);
                    primaryStage.getIcons().add(icon);
                }
            } catch (Exception ignored) {}

            Scene scene = new Scene(root, 1750, 840);
            try {
                String cssPath = "/com/example/military/client/style.css";
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            } catch (Exception e) {
                System.err.println("Не удалось загрузить CSS: " + e.getMessage());
            }

            primaryStage.setTitle("АРМ «Военный состав»");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);

            primaryStage.setOnCloseRequest(event -> {
                if (currentUser != null) {
                    connector.logout();
                }
                Platform.exit();
            });
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void logout() {
        showConfirmDialog(primaryStage, "Подтверждение", "Вы уверены, что хотите выйти?", () -> {
            connector.logout();
            connector.reset();

            primaryStage.close();

            User newUser = LoginDialog.show(new Stage(), connector);
            if (newUser != null) {
                this.currentUser = newUser;
                userLabel.setText(currentUser.getFullName());
                loadData();
                primaryStage.show();
            } else {
                Platform.exit();
            }
        });
    }

    private void startAutoRefresh(int seconds) {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    ObservableList<TableColumn<MilitaryPerson, ?>> sortOrder =
                            FXCollections.observableArrayList(table.getSortOrder());

                    Map<TableColumn<MilitaryPerson, ?>, TableColumn.SortType> sortTypes = new HashMap<>();
                    for (TableColumn<MilitaryPerson, ?> col : sortOrder) {
                        sortTypes.put(col, col.getSortType());
                    }

                    String currentSearchText = searchField.getText();
                    String currentFilter = filterCombo.getValue();

                    List<MilitaryPerson> serverList = connector.getAllPersons();
                    if (serverList == null) return;

                    List<MilitaryPerson> filteredByType = new ArrayList<>();
                    for (MilitaryPerson p : serverList) {
                        if (matchesFilter(p, currentFilter)) {
                            filteredByType.add(p);
                        }
                    }

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

                    for (int i = 0; i < finalList.size(); i++) {
                        finalList.get(i).setDisplayNumber(i + 1);
                    }

                    List<MilitaryPerson> currentDisplayList = new ArrayList<>(personData);

                    if (!listsAreEqual(currentDisplayList, finalList)) {
                        personData.setAll(finalList);

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

    private boolean listsAreEqual(List<MilitaryPerson> list1, List<MilitaryPerson> list2) {
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            MilitaryPerson p1 = list1.get(i);
            MilitaryPerson p2 = list2.get(i);

            if (p1.getId() != p2.getId()) return false;
            if (!p1.getLastName().equals(p2.getLastName())) return false;
            if (!p1.getCompany().equals(p2.getCompany())) return false;
            if (!p1.getRank().equals(p2.getRank())) return false;
            if (p1.getSalary() != p2.getSalary()) return false;

            if (p1 instanceof MilitaryCommand && p2 instanceof MilitaryCommand) {
                MilitaryCommand cmd1 = (MilitaryCommand) p1;
                MilitaryCommand cmd2 = (MilitaryCommand) p2;
                if (!cmd1.getMilitaryDistrict().equals(cmd2.getMilitaryDistrict())) return false;
                if (!cmd1.getPosition().equals(cmd2.getPosition())) return false;
                if (cmd1.getYearsOfService() != cmd2.getYearsOfService()) return false;
                if (cmd1.getAllowance() != cmd2.getAllowance()) return false;
            }
        }
        return true;
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

        TableColumn<MilitaryPerson, Integer> numberCol = new TableColumn<>("№");
        numberCol.setCellValueFactory(cellData -> {
            int index = table.getItems().indexOf(cellData.getValue()) + 1;
            return new javafx.beans.property.ReadOnlyObjectWrapper<>(index);
        });
        numberCol.setPrefWidth(60);

        TableColumn<MilitaryPerson, String> lastNameCol = new TableColumn<>("Фамилия");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lastNameCol.setPrefWidth(180);

        TableColumn<MilitaryPerson, String> companyCol = new TableColumn<>("Рота");
        companyCol.setCellValueFactory(new PropertyValueFactory<>("company"));
        companyCol.setPrefWidth(300);

        TableColumn<MilitaryPerson, String> rankCol = new TableColumn<>("Звание");
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankCol.setPrefWidth(180);

        TableColumn<MilitaryPerson, LocalDate> birthDateCol = new TableColumn<>("Дата рождения");
        birthDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getBirthDate();
            return new javafx.beans.property.SimpleObjectProperty<>(date);
        });

        birthDateCol.setCellFactory(col -> new TableCell<MilitaryPerson, LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText("");
                } else {
                    setText(formatter.format(date));
                }
            }
        });

        birthDateCol.setComparator((date1, date2) -> {
            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;
            return date1.compareTo(date2);
        });

        birthDateCol.setPrefWidth(135);

        TableColumn<MilitaryPerson, LocalDate> enlistDateCol = new TableColumn<>("Дата поступления");
        enlistDateCol.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getEnlistmentDate();
            return new javafx.beans.property.SimpleObjectProperty<>(date);
        });

        enlistDateCol.setCellFactory(col -> new TableCell<MilitaryPerson, LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText("");
                } else {
                    setText(formatter.format(date));
                }
            }
        });

        enlistDateCol.setComparator((date1, date2) -> {
            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;
            return date1.compareTo(date2);
        });

        enlistDateCol.setPrefWidth(150);

        TableColumn<MilitaryPerson, String> unitCol = new TableColumn<>("Часть");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(75);

        TableColumn<MilitaryPerson, String> salaryCol = new TableColumn<>("Зарплата");
        salaryCol.setCellValueFactory(cellData -> {
            double salary = cellData.getValue().getSalary();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.0f", salary));
        });
        salaryCol.setComparator(Comparator.comparingDouble(Double::parseDouble));
        salaryCol.setPrefWidth(95);

        table.getColumns().addAll(numberCol, lastNameCol, companyCol, rankCol,
                birthDateCol, enlistDateCol, unitCol, salaryCol);

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

            TableColumn<MilitaryPerson, Integer> yearsCol = new TableColumn<>("Выслуга лет");
            yearsCol.setCellValueFactory(cellData -> {
                MilitaryCommand cmd = (MilitaryCommand) cellData.getValue();
                return new javafx.beans.property.SimpleIntegerProperty(cmd.getYearsOfService()).asObject();
            });
            yearsCol.setPrefWidth(150);

            TableColumn<MilitaryPerson, String> cmdAllowanceCol = new TableColumn<>("Надбавка");
            cmdAllowanceCol.setCellValueFactory(cellData -> {
                MilitaryCommand cmd = (MilitaryCommand) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(
                        String.format("%.0f", cmd.getAllowance())
                );
            });
            cmdAllowanceCol.setComparator((s1, s2) -> {
                try {
                    double d1 = Double.parseDouble(s1);
                    double d2 = Double.parseDouble(s2);
                    return Double.compare(d1, d2);
                } catch (NumberFormatException e) {
                    return s1.compareTo(s2);
                }
            });
            cmdAllowanceCol.setPrefWidth(100);

            table.getColumns().addAll(districtCol, positionCol, yearsCol, cmdAllowanceCol);
        }
        else if (filterType.equals("Контрактники")) {
            TableColumn<MilitaryPerson, String> periodCol = new TableColumn<>("Период");
            periodCol.setCellValueFactory(cellData -> {
                MilitaryContract contract = (MilitaryContract) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(contract.getContractPeriod());
            });
            periodCol.setPrefWidth(300);

            TableColumn<MilitaryPerson, LocalDate> contractDateCol = new TableColumn<>("Дата договора");
            contractDateCol.setCellValueFactory(cellData -> {
                MilitaryContract contract = (MilitaryContract) cellData.getValue();
                return new javafx.beans.property.SimpleObjectProperty<>(contract.getContractDate());
            });

            contractDateCol.setCellFactory(col -> new TableCell<MilitaryPerson, LocalDate>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                @Override
                protected void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (empty || date == null) {
                        setText("");
                    } else {
                        setText(formatter.format(date));
                    }
                }
            });

            contractDateCol.setComparator((date1, date2) -> {
                if (date1 == null && date2 == null) return 0;
                if (date1 == null) return 1;
                if (date2 == null) return -1;
                return date1.compareTo(date2);
            });
            contractDateCol.setPrefWidth(180);

            TableColumn<MilitaryPerson, String> protocolCol = new TableColumn<>("Номер протокола");
            protocolCol.setCellValueFactory(cellData -> {
                MilitaryContract contract = (MilitaryContract) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(contract.getProtocolNumber());
            });
            protocolCol.setPrefWidth(150);

            table.getColumns().addAll(periodCol, contractDateCol, protocolCol);
        }
        else if (filterType.equals("Награждённые")) {
            TableColumn<MilitaryPerson, String> awardCol = new TableColumn<>("Награда");
            awardCol.setCellValueFactory(cellData -> {
                MilitaryAwarded awarded = (MilitaryAwarded) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(awarded.getAwardName());
            });
            awardCol.setPrefWidth(300);

            TableColumn<MilitaryPerson, String> prizeCol = new TableColumn<>("Премия");
            prizeCol.setCellValueFactory(cellData -> {
                MilitaryAwarded awarded = (MilitaryAwarded) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(
                        String.format("%.0f", awarded.getPrize())
                );
            });
            prizeCol.setComparator((s1, s2) -> {
                try {
                    double d1 = Double.parseDouble(s1);
                    double d2 = Double.parseDouble(s2);
                    return Double.compare(d1, d2);
                } catch (NumberFormatException e) {
                    return s1.compareTo(s2);
                }
            });
            prizeCol.setPrefWidth(180);

            TableColumn<MilitaryPerson, String> allowanceCol = new TableColumn<>("Надбавка");
            allowanceCol.setCellValueFactory(cellData -> {
                MilitaryAwarded awarded = (MilitaryAwarded) cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(
                        String.format("%.0f", awarded.getAllowance())
                );
            });
            allowanceCol.setComparator((s1, s2) -> {
                try {
                    double d1 = Double.parseDouble(s1);
                    double d2 = Double.parseDouble(s2);
                    return Double.compare(d1, d2);
                } catch (NumberFormatException e) {
                    return s1.compareTo(s2);
                }
            });
            allowanceCol.setPrefWidth(150);

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

        for (int i = 0; i < filteredList.size(); i++) {
            filteredList.get(i).setDisplayNumber(i + 1);
        }

        updateTableColumns(currentFilterType);

        if (personData == null) {
            personData = FXCollections.observableArrayList(filteredList);
            table.setItems(personData);
        } else {
            personData.setAll(filteredList);
        }
    }

    private void showEditDialog(MilitaryPerson person) {
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

    private void applySearchFilter(String searchText) {
        if (personData == null) return;

        currentFilterType = filterCombo.getValue();

        List<MilitaryPerson> allData = connector.getAllPersons();
        if (allData == null) return;

        List<MilitaryPerson> filteredByType = new ArrayList<>();
        for (MilitaryPerson p : allData) {
            if (matchesFilter(p, currentFilterType)) {
                filteredByType.add(p);
            }
        }

        if (searchText == null || searchText.trim().isEmpty()) {
            for (int i = 0; i < filteredByType.size(); i++) {
                filteredByType.get(i).setDisplayNumber(i + 1);
            }
            updateTableColumns(currentFilterType);
            personData.setAll(filteredByType);
            return;
        }

        String lowerSearch = searchText.toLowerCase().trim();
        List<MilitaryPerson> searchResults = new ArrayList<>();

        for (MilitaryPerson p : filteredByType) {
            if (matchesSearch(p, lowerSearch)) {
                searchResults.add(p);
            }
        }

        for (int i = 0; i < searchResults.size(); i++) {
            searchResults.get(i).setDisplayNumber(i + 1);
        }

        updateTableColumns(currentFilterType);
        personData.setAll(searchResults);
    }

    private void showInfoDialog(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        try {
            String cssPath = "/com/example/military/client/style.css";
            dialogPane.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        } catch (Exception e) {
            System.err.println("Не удалось загрузить CSS для диалога: " + e.getMessage());
        }
        dialogPane.getStyleClass().add("info-dialog");

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

        DialogPane dialogPane = alert.getDialogPane();
        try {
            String cssPath = "/com/example/military/client/style.css";
            dialogPane.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        } catch (Exception e) {
            System.err.println("Не удалось загрузить CSS для диалога: " + e.getMessage());
        }
        dialogPane.getStyleClass().add("info-dialog");

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
        if (p.getLastName() != null && p.getLastName().toLowerCase().contains(searchText)) return true;
        if (p.getCompany() != null && p.getCompany().toLowerCase().contains(searchText)) return true;
        if (p.getRank() != null && p.getRank().toLowerCase().contains(searchText)) return true;
        if (p.getUnit() != null && p.getUnit().toLowerCase().contains(searchText)) return true;
        if (String.valueOf(p.getSalary()).contains(searchText)) return true;

        if (p.getBirthDate() != null) {
            String birthDateStr = p.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            if (birthDateStr.contains(searchText)) return true;
        }

        if (p.getEnlistmentDate() != null) {
            String enlistDateStr = p.getEnlistmentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            if (enlistDateStr.contains(searchText)) return true;
        }

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
                connector.logImport(file.getName(), added);
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
                connector.logExport(file.getName(), personData.size());
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