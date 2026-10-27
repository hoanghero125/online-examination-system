package com.group2.oes.ui.controller;

import com.group2.oes.app.AppContext;
import com.group2.oes.model.Role;
import com.group2.oes.model.User;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Navigator;
import com.group2.oes.ui.StaffApp;
import com.group2.oes.ui.StaffWorkspace;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Trang chủ ứng dụng quản lý: menu bên trái theo vai trò, vùng giữa hiện màn hình chức năng,
 * thanh dưới hiện trạng thái máy chấm.
 *
 * Ẩn menu theo vai trò chỉ để giao diện gọn; quyền thật vẫn do service kiểm tra (Permissions).
 * Lớp này là StaffWorkspace của các màn hình chức năng: chúng mở nhau vào vùng giữa thông qua nó.
 */
public class StaffHomeController implements StaffWorkspace {

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
            items.add(new MenuItem("Người dùng", () -> open("users.fxml", (UsersController c) -> c.init(this))));
            items.add(new MenuItem("Môn học & lớp", () -> open("subjects.fxml", (SubjectsController c) -> c.init(this))));
            items.add(new MenuItem("Nhật ký", () -> open("audit-log.fxml", (AuditLogController c) -> c.init(this))));
        } else {
            items.add(new MenuItem("Ngân hàng câu hỏi", () -> open("question-bank.fxml", (QuestionBankController c) -> c.init(this))));
            items.add(new MenuItem("Đề thi", () -> open("exam-list.fxml", (ExamListController c) -> c.init(this))));
            items.add(new MenuItem("Ca thi", () -> open("staff-sessions.fxml", (StaffSessionsController c) -> c.init(this))));
        }
        items.add(new MenuItem("Hồ sơ", () -> open("profile.fxml", (ProfileController c) -> c.init(this, shell))));
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

    @Override
    public <C> void open(String fxml, Consumer<C> init) {
        content.getChildren().setAll(shell.navigator().embed(fxml, init));
    }

    @Override
    public AppContext app() {
        return app;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Navigator navigator() {
        return shell.navigator();
    }

    @Override
    public void gradeNow() {
        shell.gradeNow();
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
