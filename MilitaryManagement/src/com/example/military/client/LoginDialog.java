package com.example.military.client;

import com.example.military.model.User;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.InputStream;

public class LoginDialog {

    public static User show(Stage owner, ServerConnector connector) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("АРМ «Военный состав»");

        // Добавляем иконку приложения
        try {
            InputStream iconStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("com/example/military/client/star-icon.png");
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                dialog.getIcons().add(icon);
            }
        } catch (Exception e) {
            // игнорируем
        }

        // Основной контейнер
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("login-root");  // для CSS

        // Заголовок
        Label titleLabel = new Label("Вход в систему");
        titleLabel.getStyleClass().add("login-title");

        // Форма входа
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(225);  // ширина колонки = ширина поля
        col1.setMinWidth(225);
        col1.setMaxWidth(225);  // колонка будет растягиваться
        grid.getColumnConstraints().add(col1);

        TextField userField = new TextField();
        userField.setPromptText("Логин");  // меняем текст
        userField.setPrefWidth(150);  // увеличиваем ширину в 1.5 раза (было 200)
        userField.getStyleClass().add("login-field");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Пароль");
        passField.setPrefWidth(150);  // увеличиваем ширину в 1.5 раза
        passField.getStyleClass().add("login-field");

        grid.add(userField, 0, 0, 2, 1);  // colspan=2, чтобы поле заняло всю ширину
        grid.add(passField, 0, 1, 2, 1);

        userField.setFocusTraversable(false);
        Platform.runLater(() -> {
            root.requestFocus();  // фокус на корневой контейнер
        });

        // Метка для ошибок
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);

        // Кнопки
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button loginBtn = new Button("Войти");
        loginBtn.getStyleClass().add("login-button");
        loginBtn.setPrefWidth(100);

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("login-button");
        cancelBtn.setPrefWidth(100);

        buttonBox.getChildren().addAll(loginBtn, cancelBtn);
        root.getChildren().addAll(titleLabel, grid, errorLabel, buttonBox);

        Scene scene = new Scene(root, 400, 300);
        scene.getStylesheets().add("file:build/classes/com/example/military/client/style.css");
        dialog.setScene(scene);

        // Обработчики
        final User[] loggedUser = new User[1];

        loginBtn.setOnAction(e -> {
            String username = userField.getText().trim();
            String password = passField.getText().trim();

            System.out.println("=== ПОПЫТКА ВХОДА ===");
            System.out.println("Username: " + username);
            System.out.println("Password length: " + password.length());

            if (username.isEmpty() || password.isEmpty()) {
                System.out.println("❌ Пустые поля");
                errorLabel.setText("Введите логин и пароль");
                errorLabel.setVisible(true);
                return;
            }

            System.out.println("📞 Вызываем connector.login()");
            User user = connector.login(username, password);
            System.out.println("📞 connector.login() вернул: " + (user != null ? user.getFullName() : "null"));

            if (user != null) {
                System.out.println("✅ Успешный вход для: " + user.getFullName());
                loggedUser[0] = user;
                dialog.close();
                System.out.println("Окно логина закрыто");
            } else {
                System.out.println("❌ Ошибка входа");
                errorLabel.setText("Неверный логин или пароль");
                errorLabel.setVisible(true);
                connector.reset();
            }
        });

        cancelBtn.setOnAction(e -> {
            dialog.close();
        });

        passField.setOnAction(loginBtn.getOnAction());

        scene.getStylesheets().add("file:build/classes/com/example/military/client/style.css");

        dialog.showAndWait();

        System.out.println("=== ДИАЛОГ ЗАКРЫТ, возвращаем: " +
                (loggedUser[0] != null ? loggedUser[0].getFullName() : "null"));
        return loggedUser[0];
    }
}
