package com.group2.oes.ui.controller;

import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Role;
import com.group2.oes.model.Subject;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.FormDialog;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Môn học, lớp học phần và phân công giảng viên (FR-07). */
public class SubjectsController {

    @FXML
    private TableView<Subject> subjectTable;
    @FXML
    private TableView<CourseClass> classTable;
    @FXML
    private Label classHeader;
    @FXML
    private ListView<Instructor> instructorList;
    @FXML
    private ComboBox<Instructor> instructorBox;
    @FXML
    private HBox page;

    private StaffWorkspace ws;

    public void init(StaffWorkspace ws) {
        this.ws = ws;
        subjectTable.getColumns().add(Tables.column("Mã môn", 100, Subject::getCode));
        subjectTable.getColumns().add(Tables.column("Tên môn", 240, Subject::getName));
        classTable.getColumns().add(Tables.column("Mã lớp", 130, CourseClass::getCode));
        classTable.getColumns().add(Tables.column("Học kỳ", 110, c -> c.getSemester() == null ? "" : c.getSemester()));
        Tables.labels(instructorBox, i -> i.getFullName() + " (" + i.getUsername() + ")", "");
        instructorList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Instructor i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null : i.getFullName() + " (" + i.getUsername() + ")");
            }
        });
        subjectTable.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> loadDetails(s));
        loadSubjects();
        BackgroundTasks.run(page, () -> ws.app().auth.searchUsers(ws.user(), null, Role.INSTRUCTOR).stream()
                .map(u -> (Instructor) u).toList(), list -> instructorBox.getItems().setAll(list));
    }

    private void loadSubjects() {
        BackgroundTasks.run(page, () -> ws.app().subjectService.listSubjects(ws.user()), list -> {
            Subject keep = subjectTable.getSelectionModel().getSelectedItem();
            subjectTable.setItems(FXCollections.observableArrayList(list));
            if (keep != null) {
                subjectTable.getSelectionModel().select(keep);
            } else if (!list.isEmpty()) {
                subjectTable.getSelectionModel().selectFirst();
            }
        });
    }

    private void loadDetails(Subject s) {
        if (s == null) {
            classTable.getItems().clear();
            instructorList.getItems().clear();
            return;
        }
        classHeader.setText("Lớp học phần môn " + s.getCode());
        record Details(List<CourseClass> classes, List<Instructor> instructors) {
        }
        BackgroundTasks.run(page, () -> new Details(ws.app().subjectService.listClasses(ws.user(), s),
                ws.app().subjectService.instructorsOf(ws.user(), s)), d -> {
            classTable.setItems(FXCollections.observableArrayList(d.classes()));
            instructorList.setItems(FXCollections.observableArrayList(d.instructors()));
        });
    }

    private Subject currentSubject() {
        Subject s = subjectTable.getSelectionModel().getSelectedItem();
        if (s == null) {
            ErrorDialogs.info("Môn học", "Hãy chọn một môn trong bảng bên trái.");
        }
        return s;
    }

    @FXML
    private void onAddSubject() {
        FormDialog.ask("Thêm môn học", List.of("Mã môn", "Tên môn"), List.of("", ""), "Thêm").ifPresent(v ->
                BackgroundTasks.run(page, () -> ws.app().subjectService.createSubject(ws.user(), v.get(0), v.get(1)),
                        s -> loadSubjects()));
    }

    @FXML
    private void onRenameSubject() {
        Subject s = currentSubject();
        if (s != null) {
            FormDialog.ask("Đổi tên môn " + s.getCode(), List.of("Tên môn"), List.of(s.getName()), "Lưu").ifPresent(v ->
                    BackgroundTasks.run(page, () -> ws.app().subjectService.renameSubject(ws.user(), s, v.get(0)),
                            r -> loadSubjects()));
        }
    }

    @FXML
    private void onDeleteSubject() {
        Subject s = currentSubject();
        if (s != null && ErrorDialogs.confirm("Xóa môn học", "Xóa môn " + s + "?", "Xóa")) {
            BackgroundTasks.runVoid(page, () -> ws.app().subjectService.deleteSubject(ws.user(), s), () -> {
                subjectTable.getSelectionModel().clearSelection();
                loadSubjects();
            });
        }
    }

    @FXML
    private void onAddClass() {
        Subject s = currentSubject();
        if (s != null) {
            FormDialog.ask("Thêm lớp học phần môn " + s.getCode(), List.of("Mã lớp", "Học kỳ"), List.of(s.getCode() + "-", ""),
                    "Thêm").ifPresent(v -> BackgroundTasks.run(page,
                    () -> ws.app().subjectService.createClass(ws.user(), v.get(0), s, v.get(1)), c -> loadDetails(s)));
        }
    }

    @FXML
    private void onEditClass() {
        Subject s = currentSubject();
        CourseClass c = classTable.getSelectionModel().getSelectedItem();
        if (s == null) {
            return;
        }
        if (c == null) {
            ErrorDialogs.info("Sửa lớp", "Hãy chọn một lớp trong bảng.");
            return;
        }
        FormDialog.ask("Sửa lớp " + c.getCode(), List.of("Mã lớp", "Học kỳ"),
                List.of(c.getCode(), c.getSemester() == null ? "" : c.getSemester()), "Lưu").ifPresent(v ->
                BackgroundTasks.run(page, () -> ws.app().subjectService.updateClass(ws.user(), c, v.get(0), v.get(1)),
                        u -> loadDetails(s)));
    }

    @FXML
    private void onAssign() {
        Subject s = currentSubject();
        Instructor i = instructorBox.getValue();
        if (s == null) {
            return;
        }
        if (i == null) {
            ErrorDialogs.info("Phân công", "Hãy chọn giảng viên trong ô bên cạnh.");
            return;
        }
        BackgroundTasks.runVoid(page, () -> ws.app().subjectService.assignInstructor(ws.user(), i, s), () -> loadDetails(s));
    }

    @FXML
    private void onUnassign() {
        Subject s = currentSubject();
        Instructor i = instructorList.getSelectionModel().getSelectedItem();
        if (s == null) {
            return;
        }
        if (i == null) {
            ErrorDialogs.info("Bỏ phân công", "Hãy chọn giảng viên trong danh sách đang phụ trách.");
            return;
        }
        BackgroundTasks.runVoid(page, () -> ws.app().subjectService.unassignInstructor(ws.user(), i, s), () -> loadDetails(s));
    }
}
