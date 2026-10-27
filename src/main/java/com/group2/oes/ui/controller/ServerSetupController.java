package com.group2.oes.ui.controller;

import com.group2.oes.exception.ExamAppException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.ui.AppConfig.ServerSettings;
import com.group2.oes.ui.ErrorDialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Nhập địa chỉ máy chủ CSDL khi chưa có tệp cấu hình hoặc không kết nối được.
 * Ứng dụng thử kết nối trước, đúng thì mới lưu tệp cấu hình.
 */
public class ServerSetupController {

    @FXML
    private Label headerLabel;
    @FXML
    private Label fileLabel;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField databaseField;
    @FXML
    private TextField userField;
    @FXML
    private PasswordField passwordField;

    private Consumer<ServerSettings> onConnect;

    public void init(String appTitle, String defaultUser, Path configFile, Consumer<ServerSettings> onConnect) {
        this.onConnect = onConnect;
        headerLabel.setText("Kết nối máy chủ – ứng dụng " + appTitle.toLowerCase());
        fileLabel.setText("Cấu hình sẽ được lưu vào: " + configFile);
        userField.setText(defaultUser);
    }

    @FXML
    private void onConnect() {
        try {
            int port;
            try {
                port = Integer.parseInt(portField.getText().strip());
            } catch (NumberFormatException e) {
                throw new ValidationException("PORT_INVALID", "Cổng phải là số, MySQL thường dùng 3306.");
            }
            onConnect.accept(new ServerSettings(hostField.getText(), port, databaseField.getText().strip(),
                    userField.getText(), passwordField.getText()));
        } catch (ExamAppException e) {
            ErrorDialogs.show(e);
        }
    }
}
