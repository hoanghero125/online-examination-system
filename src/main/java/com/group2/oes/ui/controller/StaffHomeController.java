package com.group2.oes.ui.controller;

import com.group2.oes.app.AppContext;
import com.group2.oes.model.Role;
import com.group2.oes.model.User;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.StaffApp;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Trang chủ ứng dụng quản lý: menu bên trái theo vai trò, vùng giữa hiện màn hình chức năng,
 * thanh dưới hiện trạng thái máy chấm.
 *
 * Ẩn menu theo vai trò chỉ để giao diện gọn; quyền thật vẫn do service kiểm tra (Permissions).
 */
public class StaffHomeController {

    @FXML
    private Label userLabel;
    @FXML
    private Label gradingLabel;
    @FXML
    private VBox menu;
    @FXML
    private StackPane content;

    private StaffApp shell;
    private AppContext app;
    private User user;
    private final List<Button> menuButtons = new ArrayList<>();

    private record MenuItem(String title, Runnable open) {
    }

    public void init(StaffApp shell, AppContext app, User user, ReadOnlyStringProperty gradingStatus) {
        this.shell = shell;
        this.app = app;
        this.user = user;
        userLabel.setText(user.getFullName() + "  ·  " + user.getRole().displayName());
        gradingLabel.textProperty().bind(gradingStatus);

        List<MenuItem> items = new ArrayList<>();
        if (user.getRole() == Role.ADMIN) {
            items.add(new MenuItem("Người dùng", () -> placeholder("Người dùng")));
            items.add(new MenuItem("Môn học & lớp", () -> placeholder("Môn học & lớp")));
            items.add(new MenuItem("Nhật ký", () -> placeholder("Nhật ký")));
        } else {
            items.add(new MenuItem("Ngân hàng câu hỏi", () -> placeholder("Ngân hàng câu hỏi")));
            items.add(new MenuItem("Đề thi", () -> placeholder("Đề thi")));
            items.add(new MenuItem("Ca thi", () -> placeholder("Ca thi")));
        }
        items.add(new MenuItem("Hồ sơ", () -> placeholder("Hồ sơ")));
        items.forEach(this::addMenuButton);
        menuButtons.get(0).fire();
    }

    private void addMenuButton(MenuItem item) {
        Button b = new Button(item.title());
        b.getStyleClass().add("menu-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> {
            menuButtons.forEach(other -> other.getStyleClass().remove("selected"));
            b.getStyleClass().add("selected");
            item.open().run();
        });
        menuButtons.add(b);
        menu.getChildren().add(b);
    }

    /** Đặt một màn hình chức năng vào vùng giữa. */
    void setContent(Node node) {
        content.getChildren().setAll(node);
    }

    private void placeholder(String title) {
        Label l = new Label(title + ": chức năng đang được xây dựng.");
        l.getStyleClass().add("placeholder");
        setContent(l);
    }

    @FXML
    private void onChangePassword() {
        shell.navigator().show("change-password.fxml", (ChangePasswordController c) -> c.init(shell, user, false));
    }

    @FXML
    private void onLogout() {
        if (ErrorDialogs.confirm("Đăng xuất", "Bạn có chắc muốn đăng xuất?", "Đăng xuất")) {
            shell.logout();
        }
    }
}
