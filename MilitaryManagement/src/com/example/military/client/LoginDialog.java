package com.example.military.client;

import com.example.military.model.User;
import com.google.gson.JsonObject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginDialog {

    public static User show(Stage owner, ServerConnector connector) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Вход в систему");

        // Основной контейнер
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        // Заголовок
        Label titleLabel = new Label("АРМ «Воинский учёт»");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Форма входа
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Пользователь:");
        TextField userField = new TextField();
        userField.setPromptText("Имя пользователя");
        userField.setPrefWidth(200);

        Label passLabel = new Label("Пароль:");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Пароль");

        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0);
        grid.add(passLabel, 0, 1);
        grid.add(passField, 1, 1);

        // Метка для ошибок
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff6b6b;");
        errorLabel.setVisible(false);

        // Кнопки
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button loginBtn = new Button("Войти");
        Button cancelBtn = new Button("Отмена");

        loginBtn.setPrefWidth(100);
        cancelBtn.setPrefWidth(100);

        buttonBox.getChildren().addAll(loginBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, errorLabel, buttonBox);

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
                errorLabel.setText("Введите имя пользователя и пароль");
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
                errorLabel.setText("Неверное имя пользователя или пароль");
                errorLabel.setVisible(true);
                connector.reset();
            }
        });

        cancelBtn.setOnAction(e -> {
            dialog.close();
        });

        // Нажатие Enter в поле пароля
        passField.setOnAction(loginBtn.getOnAction());

        Scene scene = new Scene(root, 400, 300);
        dialog.setScene(scene);
        dialog.showAndWait();
        System.out.println("=== ДИАЛОГ ЗАКРЫТ, возвращаем: " +
                (loggedUser[0] != null ? loggedUser[0].getFullName() : "null"));
        return loggedUser[0];
    }
}
