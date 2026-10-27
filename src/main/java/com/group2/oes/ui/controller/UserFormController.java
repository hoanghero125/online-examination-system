package com.group2.oes.ui.controller;

import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.service.NewAccount;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/** Thêm / sửa người dùng. Vai trò, tên đăng nhập, mã sinh viên chỉ nhập lúc tạo, không sửa được về sau. */
public class UserFormController {

    @FXML
    private ComboBox<Role> roleBox;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label passwordLabel;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private Label studentCodeLabel;
    @FXML
    private TextField studentCodeField;
    @FXML
    private Label classLabel;
    @FXML
    private ComboBox<CourseClass> classBox;
    @FXML
    private GridPane grid;
    @FXML
    private VBox form;

    private StaffWorkspace ws;
    private User existing;
    private Runnable onSaved;

    public void init(StaffWorkspace ws, User existing, Runnable onSaved) {
        this.ws = ws;
        this.existing = existing;
        this.onSaved = onSaved;
        Tables.labels(roleBox, Role::displayName, "");
        Tables.labels(classBox, CourseClass::getCode, "(chưa xếp lớp)");
        roleBox.getItems().setAll(Role.values());
        roleBox.valueProperty().addListener((o, a, role) -> showStudentFields(role == Role.STUDENT));
        BackgroundTasks.run(form, () -> ws.app().subjectService.listClasses(ws.user(), null), classes -> {
            classBox.getItems().add(null);
            classBox.getItems().addAll(classes);
            if (existing instanceof Student s && s.getCourseClass() != null) {
                classBox.getItems().stream().filter(c -> c != null && c.getClassId() == s.getCourseClass().getClassId())
                        .findFirst().ifPresent(classBox::setValue);
            }
        });

        if (existing == null) {
            roleBox.setValue(Role.STUDENT);
        } else {
            roleBox.setValue(existing.getRole());
            roleBox.setDisable(true);
            usernameField.setText(existing.getUsername());
            usernameField.setDisable(true);
            passwordLabel.setVisible(false);
            passwordLabel.setManaged(false);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            fullNameField.setText(existing.getFullName());
            emailField.setText(existing.getEmail() == null ? "" : existing.getEmail());
            if (existing instanceof Student s) {
                studentCodeField.setText(s.getStudentCode());
                studentCodeField.setDisable(true);
            }
        }
    }

    private void showStudentFields(boolean student) {
        for (Node node : List.of(studentCodeLabel, studentCodeField, classLabel, classBox)) {
            node.setVisible(student);
            node.setManaged(student);
        }
    }

    @FXML
    private void onSave() {
        String name = fullNameField.getText();
        String email = emailField.getText();
        CourseClass cls = classBox.getValue();
        if (existing == null) {
            NewAccount a = new NewAccount(roleBox.getValue(), usernameField.getText(), passwordField.getText(), name,
                    email, studentCodeField.getText(), cls);
            BackgroundTasks.run(form, () -> ws.app().auth.createAccount(ws.user(), a), u -> done());
        } else {
            BackgroundTasks.runVoid(form, () -> ws.app().auth.updateUser(ws.user(), existing, name, email, cls), this::done);
        }
    }

    private void done() {
        close();
        onSaved.run();
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) form.getScene().getWindow()).close();
    }
}
