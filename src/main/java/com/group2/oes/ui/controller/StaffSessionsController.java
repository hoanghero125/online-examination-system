package com.group2.oes.ui.controller;

import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.SessionSummary;
import com.group2.oes.model.Subject;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Formats;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Danh sách ca thi của một môn: trạng thái, số thí sinh, số bài đã nộp. Bấm "Làm mới" để đọc lại. */
public class StaffSessionsController {

    @FXML
    private ComboBox<Subject> subjectBox;
    @FXML
    private TableView<SessionSummary> table;
    @FXML
    private VBox page;

    private StaffWorkspace ws;

    public void init(StaffWorkspace ws) {
        this.ws = ws;
        table.getColumns().add(Tables.column("Ca", 55, s -> s.session().getSessionId()));
        table.getColumns().add(Tables.column("Đề", 230, s -> s.session().getExam().getTitle()
                + " (v" + s.session().getExam().getVersion() + ")"));
        table.getColumns().add(Tables.column("Mở", 140, s -> Formats.dateTime(s.session().getWindow().openTime())));
        table.getColumns().add(Tables.column("Đóng", 140, s -> Formats.dateTime(s.session().getWindow().closeTime())));
        table.getColumns().add(Tables.column("Trạng thái", 110, s -> s.phase().displayName()));
        table.getColumns().add(Tables.column("Thí sinh", 75, SessionSummary::enrolled));
        table.getColumns().add(Tables.column("Đã vào thi", 85, SessionSummary::started));
        table.getColumns().add(Tables.column("Đã nộp", 70, SessionSummary::submitted));
        table.setRowFactory(tv -> {
            TableRow<SessionSummary> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onResults();
                }
            });
            return row;
        });
        subjectBox.valueProperty().addListener((o, a, s) -> onRefresh());
        BackgroundTasks.run(page, () -> ws.app().subjectService.listSubjects(ws.user()), subjects -> {
            subjectBox.getItems().setAll(subjects);
            if (!subjects.isEmpty()) {
                subjectBox.getSelectionModel().selectFirst();
            }
        });
    }

    @FXML
    private void onRefresh() {
        Subject subject = subjectBox.getValue();
        if (subject == null) {
            return;
        }
        BackgroundTasks.run(page, () -> {
            LocalDateTime now = ws.app().sessionService.serverTime();
            List<SessionSummary> list = new ArrayList<>();
            for (Exam exam : ws.app().examService.list(ws.user(), subject)) {
                for (ExamSession s : ws.app().sessionService.listOfExam(ws.user(), exam)) {
                    list.add(ws.app().statistics.summarize(ws.user(), s, now));
                }
            }
            list.sort(Comparator.comparing((SessionSummary s) -> s.session().getWindow().openTime()).reversed());
            return list;
        }, list -> table.setItems(FXCollections.observableArrayList(list)));
    }

    @FXML
    private void onResults() {
        SessionSummary s = table.getSelectionModel().getSelectedItem();
        if (s == null) {
            ErrorDialogs.info("Kết quả ca thi", "Hãy chọn một ca thi trong bảng.");
            return;
        }
        ws.open("session-results.fxml", (SessionResultsController c) -> c.init(ws, s.session()));
    }
}
