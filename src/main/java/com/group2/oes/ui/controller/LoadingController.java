package com.group2.oes.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Màn hình chờ khi đang kết nối CSDL lúc khởi động. */
public class LoadingController {

    @FXML
    private Label messageLabel;

    public void init(String message) {
        messageLabel.setText(message);
    }
}
