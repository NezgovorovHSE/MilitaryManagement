package com.example.military.client;

import com.example.military.model.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.InputStream;

public class LoginDialog {

    public static User show(Stage owner, ServerConnector connector) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("АРМ «Военный состав»");

        try {
            InputStream iconStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("com/example/military/client/star-icon.png");
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                dialog.getIcons().add(icon);
            }
        } catch (Exception ignored) {}

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("login-root");

        Label titleLabel = new Label("Вход в систему");
        titleLabel.getStyleClass().add("login-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(225);
        col1.setMinWidth(225);
        col1.setMaxWidth(225);
        grid.getColumnConstraints().add(col1);

        TextField userField = new TextField();
        userField.setPromptText("Логин");
        userField.setPrefWidth(150);
        userField.getStyleClass().add("login-field");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Пароль");
        passField.setPrefWidth(150);
        passField.getStyleClass().add("login-field");

        grid.add(userField, 0, 0, 2, 1);
        grid.add(passField, 0, 1, 2, 1);

        userField.setFocusTraversable(false);
        Platform.runLater(() -> root.requestFocus());

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("login-error");
        errorLabel.setVisible(false);

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
        try {
            String cssPath = "/com/example/military/client/style.css";
            scene.getStylesheets().add(LoginDialog.class.getResource(cssPath).toExternalForm());
        } catch (Exception e) {
            System.err.println("Не удалось загрузить CSS: " + e.getMessage());
        }
        dialog.setScene(scene);

        final User[] loggedUser = new User[1];

        loginBtn.setOnAction(e -> {
            String username = userField.getText().trim();
            String password = passField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Введите логин и пароль");
                errorLabel.setVisible(true);
                return;
            }

            User user = connector.login(username, password);

            if (user != null) {
                loggedUser[0] = user;
                dialog.close();
            } else {
                errorLabel.setText("Неверный логин или пароль");
                errorLabel.setVisible(true);
                connector.reset();
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());
        passField.setOnAction(loginBtn.getOnAction());

        dialog.showAndWait();
        return loggedUser[0];
    }
}