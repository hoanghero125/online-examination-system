package com.group2.oes.ui.controller;

import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.service.ImportResult;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Quản lý người dùng (FR-01, 02, 06): tìm, lọc theo vai trò, thêm, sửa, khóa, đặt lại mật khẩu, nhập CSV. */
public class UsersController {

    @FXML
    private TextField keywordField;
    @FXML
    private ComboBox<Role> roleBox;
    @FXML
    private TableView<User> table;
    @FXML
    private VBox page;

    private StaffWorkspace ws;

    public void init(StaffWorkspace ws) {
        this.ws = ws;
        Tables.labels(roleBox, Role::displayName, "Mọi vai trò");
        roleBox.getItems().add(null);
        roleBox.getItems().addAll(Role.values());
        table.getColumns().add(Tables.column("Tên đăng nhập", 140, User::getUsername));
        table.getColumns().add(Tables.column("Họ tên", 190, User::getFullName));
        table.getColumns().add(Tables.column("Vai trò", 110, u -> u.getRole().displayName()));
        table.getColumns().add(Tables.column("Mã SV", 110, u -> u instanceof Student s ? s.getStudentCode() : ""));
        table.getColumns().add(Tables.column("Lớp", 110,
                u -> u instanceof Student s && s.getCourseClass() != null ? s.getCourseClass().getCode() : ""));
        table.getColumns().add(Tables.column("Email", 180, u -> u.getEmail() == null ? "" : u.getEmail()));
        table.getColumns().add(Tables.column("Trạng thái", 140, u -> !u.isActive() ? "Đã khóa"
                : u.isMustChangePassword() ? "Chờ đổi mật khẩu" : "Hoạt động"));
        table.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });
        keywordField.setOnAction(e -> onSearch());
        roleBox.valueProperty().addListener((o, a, b) -> onSearch());
        onSearch();
    }

    @FXML
    private void onSearch() {
        String keyword = keywordField.getText();
        Role role = roleBox.getValue();
        BackgroundTasks.run(page, () -> ws.app().auth.searchUsers(ws.user(), keyword, role),
                users -> table.setItems(FXCollections.observableArrayList(users)));
    }

    @FXML
    private void onAdd() {
        ws.navigator().openDialog("user-form.fxml", "Thêm người dùng",
                (UserFormController c) -> c.init(ws, null, this::onSearch));
    }

    @FXML
    private void onEdit() {
        User u = selected("Sửa người dùng");
        if (u != null) {
            ws.navigator().openDialog("user-form.fxml", "Sửa " + u.getUsername(),
                    (UserFormController c) -> c.init(ws, u, this::onSearch));
        }
    }

    @FXML
    private void onToggleActive() {
        User u = selected("Khóa / mở khóa");
        if (u == null) {
            return;
        }
        boolean lock = u.isActive();
        if (ErrorDialogs.confirm(lock ? "Khóa tài khoản" : "Mở khóa tài khoản",
                (lock ? "Khóa" : "Mở khóa") + " tài khoản " + u.getUsername() + "?", lock ? "Khóa" : "Mở khóa")) {
            BackgroundTasks.runVoid(page, () -> ws.app().auth.setActive(ws.user(), u, !lock), this::onSearch);
        }
    }

    @FXML
    private void onResetPassword() {
        User u = selected("Đặt lại mật khẩu");
        if (u != null && ErrorDialogs.confirm("Đặt lại mật khẩu", "Đặt mật khẩu tạm mới cho " + u.getUsername()
                + "? Người dùng phải đổi mật khẩu ở lần đăng nhập sau.", "Đặt lại")) {
            BackgroundTasks.run(page, () -> ws.app().auth.resetPassword(ws.user(), u), temp -> {
                ErrorDialogs.info("Đặt lại mật khẩu", "Mật khẩu tạm của " + u.getUsername() + " là:  " + temp
                        + "\nHãy báo cho người dùng; họ sẽ phải đổi mật khẩu khi đăng nhập.");
                onSearch();
            });
        }
    }

    /** FR-06: cột ma_sv,ho_ten,email,ma_lop; dòng lỗi được liệt kê, các dòng khác vẫn nhập. */
    @FXML
    private void onImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn tệp danh sách thí sinh (CSV)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp CSV", "*.csv", "*.txt"));
        File file = chooser.showOpenDialog(ws.navigator().stage());
        if (file == null) {
            return;
        }
        BackgroundTasks.run(page, () -> {
            try {
                return ws.app().userImport.importStudents(ws.user(), file.toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, (ImportResult r) -> {
            QuestionBankController.showImportResult(r);
            onSearch();
        });
    }

    @FXML
    private void onImportHelp() {
        ErrorDialogs.info("Định dạng tệp thí sinh", "Tệp CSV (UTF-8), dòng đầu là tiêu đề:\n"
                + "ma_sv,ho_ten,email,ma_lop\nB24DCCE001,Nguyễn Văn An,an@stu.ptit.edu.vn,INT1332-01\n\n"
                + "Email và mã lớp có thể để trống. Tên đăng nhập và mật khẩu ban đầu đều là mã sinh viên viết thường; "
                + "thí sinh phải đổi mật khẩu ở lần đăng nhập đầu.");
    }

    private User selected(String action) {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) {
            ErrorDialogs.info(action, "Hãy chọn một người dùng trong bảng.");
        }
        return u;
    }
}
