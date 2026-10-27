package com.group2.oes.ui.controller;

import com.group2.oes.ui.AppShell;
import com.group2.oes.ui.BackgroundTasks;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Màn hình đăng nhập dùng chung cho hai ứng dụng.
 * Ứng dụng nào (và vì vậy nhận vai trò nào) do AppShell quyết định; controller không xét vai trò.
 */
public class LoginController {

    @FXML
    private Label appTitleLabel;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox form;

    private AppShell shell;

    public void init(AppShell shell) {
        this.shell = shell;
        appTitleLabel.setText("Ứng dụng " + shell.title().toLowerCase());
        Platform.runLater(usernameField::requestFocus);
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        BackgroundTasks.run(form, () -> shell.login(username, password), user -> {
            passwordField.clear();
            shell.onLoggedIn(user);
        });
    }

    @FXML
    private void onChangeServer() {
        shell.showServerSetup();
    }
}
