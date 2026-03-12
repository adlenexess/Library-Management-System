package com.library.view;

import com.library.exception.LibraryException;
import com.library.model.User;
import com.library.model.UserRole;
import com.library.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;

import java.util.function.Consumer;

public class LoginView extends BorderPane {

    private final AuthService authService;
    private final Consumer<User> onLoginSuccess;
    private final Label statusLabel = new Label();

    public LoginView(AuthService authService, Consumer<User> onLoginSuccess) {
        this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;
        setPadding(new Insets(32));
        getStyleClass().add("login-root");
        URL css = getClass().getResource("/com/library/view/dashboard.css");
        if (css != null) {
            getStylesheets().add(css.toExternalForm());
        }
        setCenter(buildContent());
    }

    private Node buildContent() {
        VBox outer = new VBox(20);
        outer.setAlignment(Pos.CENTER);

        Label title = new Label("Library Management System");
        title.getStyleClass().add("headline");

        Label subtitle = new Label("Issue, return and manage books in one place");
        subtitle.getStyleClass().add("muted-text");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(16);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(440);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
            new Tab("Sign in", buildLoginForm()),
            new Tab("Create account", buildRegistrationForm())
        );
        tabPane.getStyleClass().add("glass-tabs");

        statusLabel.getStyleClass().add("status-label");

        if (!authService.hasLibrarian()) {
            Label hint = new Label("Tip: create a librarian account first to manage catalogues.");
            hint.getStyleClass().add("hint-label");
            card.getChildren().addAll(tabPane, statusLabel, hint);
        } else {
            card.getChildren().addAll(tabPane, statusLabel);
        }

        outer.getChildren().addAll(title, subtitle, card);
        return outer;
    }

    private Node buildLoginForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(14);
        grid.setPadding(new Insets(16));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("accent-btn");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> {
            try {
                User user = authService.login(usernameField.getText().trim(), passwordField.getText());
                statusLabel.setText("");
                onLoginSuccess.accept(user);
            } catch (LibraryException ex) {
                showError(ex.getMessage());
            }
        });

        grid.add(new Label("Username"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginButton, 1, 2);

        return grid;
    }

    private Node buildRegistrationForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(14);
        grid.setPadding(new Insets(16));

        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Pick a username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Create a password");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm password");
        ComboBox<UserRole> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(UserRole.values());
        roleCombo.getSelectionModel().select(UserRole.STUDENT);

        Button registerButton = new Button("Register");
        registerButton.getStyleClass().add("accent-btn");
        registerButton.setOnAction(event -> {
            try {
                if (!passwordField.getText().equals(confirmField.getText())) {
                    throw new LibraryException("Passwords do not match");
                }
                User user = authService.register(
                    nameField.getText().trim(),
                    usernameField.getText().trim(),
                    passwordField.getText(),
                    roleCombo.getValue()
                );
                showInfo("Account created for " + user.getFullName());
                nameField.clear();
                usernameField.clear();
                passwordField.clear();
                confirmField.clear();
            } catch (LibraryException ex) {
                showError(ex.getMessage());
            }
        });

        grid.add(new Label("Full name"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Username"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("Password"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("Confirm password"), 0, 3);
        grid.add(confirmField, 1, 3);
        grid.add(new Label("Role"), 0, 4);
        grid.add(roleCombo, 1, 4);
        grid.add(registerButton, 1, 5);

        return grid;
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #b00020;");
        statusLabel.setText(message);
    }

    private void showInfo(String message) {
        statusLabel.setStyle("-fx-text-fill: #1b5e20;");
        statusLabel.setText(message);
    }
}

