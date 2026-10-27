package com.group2.oes.ui.controller;

import com.group2.oes.model.Exam;
import com.group2.oes.model.Subject;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Formats;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/** Danh sách đề theo môn (BR-09): đề đã dùng cho ca thi bị khóa, muốn sửa thì tạo phiên bản mới. */
public class ExamListController {

    @FXML
    private ComboBox<Subject> subjectBox;
    @FXML
    private TableView<Exam> table;
    @FXML
    private VBox page;

    private StaffWorkspace ws;

    public void init(StaffWorkspace ws) {
        this.ws = ws;
        table.getColumns().add(Tables.column("Mã", 60, Exam::getExamId));
        table.getColumns().add(Tables.column("Tên đề", 280, Exam::getTitle));
        table.getColumns().add(Tables.column("Phiên bản", 80, Exam::getVersion));
        table.getColumns().add(Tables.column("Số câu", 70, e -> e.getItems().size()));
        table.getColumns().add(Tables.column("Tổng điểm", 90, e -> Formats.score(e.getTotalPoint())));
        table.getColumns().add(Tables.column("Câu nhiều đáp án", 230, e -> e.getMultiChoicePolicy().displayName()));
        table.getColumns().add(Tables.column("Trạng thái", 140, e -> e.isLocked() ? "Đã khóa (đã mở ca)" : "Chưa dùng"));
        subjectBox.valueProperty().addListener((o, a, s) -> load());
        BackgroundTasks.run(page, () -> ws.app().subjectService.listSubjects(ws.user()), subjects -> {
            subjectBox.getItems().setAll(subjects);
            if (!subjects.isEmpty()) {
                subjectBox.getSelectionModel().selectFirst();
            }
        });
    }

    private void load() {
        Subject s = subjectBox.getValue();
        if (s != null) {
            BackgroundTasks.run(page, () -> ws.app().examService.list(ws.user(), s),
                    exams -> table.setItems(FXCollections.observableArrayList(exams)));
        }
    }

    @FXML
    private void onCreate() {
        Subject s = subjectBox.getValue();
        if (s == null) {
            ErrorDialogs.info("Tạo đề", "Bạn chưa được phân công môn nào.");
            return;
        }
        ws.open("exam-builder.fxml", (ExamBuilderController c) -> c.init(ws, s));
    }

    @FXML
    private void onNewVersion() {
        Exam e = selected("Tạo phiên bản mới");
        if (e == null) {
            return;
        }
        if (!e.isLocked()) {
            ErrorDialogs.info("Tạo phiên bản mới", "Đề này chưa dùng cho ca thi nào nên vẫn dùng được, không cần tạo phiên bản mới.");
            return;
        }
        BackgroundTasks.run(page, () -> ws.app().examService.newVersion(ws.user(), e), v -> {
            ErrorDialogs.info("Tạo phiên bản mới", "Đã tạo \"" + v.getTitle() + "\" phiên bản " + v.getVersion() + ".");
            load();
        });
    }

    @FXML
    private void onOpenSession() {
        Exam e = selected("Mở ca thi");
        if (e != null) {
            ws.open("session-form.fxml", (SessionFormController c) -> c.init(ws, e));
        }
    }

    private Exam selected(String action) {
        Exam e = table.getSelectionModel().getSelectedItem();
        if (e == null) {
            ErrorDialogs.info(action, "Hãy chọn một đề trong bảng.");
        }
        return e;
    }
}
