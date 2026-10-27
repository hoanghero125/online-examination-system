package com.group2.oes.ui.controller;

import com.group2.oes.model.ExamSession;
import com.group2.oes.model.ResultRow;
import com.group2.oes.model.ResultStatistics;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Formats;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * Kết quả một ca thi (FR-30, 34, 35): bảng điểm (bài kết thúc do vi phạm tô màu – BR-06), thống kê,
 * phổ điểm, tỷ lệ đúng từng câu (câu khó nhất lên đầu), xuất CSV.
 */
public class SessionResultsController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label summaryLabel;
    @FXML
    private TableView<ResultRow> resultTable;
    @FXML
    private BarChart<String, Number> histogram;
    @FXML
    private TableView<ItemRate> itemTable;
    @FXML
    private VBox page;

    private StaffWorkspace ws;
    private ExamSession session;

    /** Một dòng bảng tỷ lệ đúng từng câu. */
    public record ItemRate(int order, String content, double rate) {
    }

    private record Loaded(List<ResultRow> rows, ResultStatistics stats) {
    }

    public void init(StaffWorkspace ws, ExamSession session) {
        this.ws = ws;
        this.session = session;
        titleLabel.setText("Ca #" + session.getSessionId() + " – " + session.getExam().getTitle() + " ("
                + Formats.dateTime(session.getWindow().openTime()) + " → " + Formats.dateTime(session.getWindow().closeTime()) + ")");

        resultTable.getColumns().add(Tables.column("Mã SV", 110, ResultRow::studentCode));
        resultTable.getColumns().add(Tables.column("Họ tên", 190, ResultRow::fullName));
        resultTable.getColumns().add(Tables.column("Điểm", 65, r -> r.score() == null ? "" : Formats.score(r.score())));
        resultTable.getColumns().add(Tables.column("Hệ 10", 65, r -> r.scoreOn10() == null ? "" : Formats.score(r.scoreOn10())));
        resultTable.getColumns().add(Tables.column("Số câu trả lời", 105, ResultRow::answeredCount));
        resultTable.getColumns().add(Tables.column("Vi phạm", 70, ResultRow::violationCount));
        resultTable.getColumns().add(Tables.column("Trạng thái", 210, SessionResultsController::statusText));
        resultTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ResultRow row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("row-violation");
                if (!empty && row != null && row.terminatedByViolation()) {
                    getStyleClass().add("row-violation");
                }
            }
        });

        itemTable.getColumns().add(Tables.column("Câu", 55, ItemRate::order));
        itemTable.getColumns().add(Tables.column("Tỷ lệ đúng", 90, r -> Math.round(r.rate() * 100) + "%"));
        itemTable.getColumns().add(Tables.column("Nội dung", 420, ItemRate::content));
        load();
    }

    private static String statusText(ResultRow r) {
        if (r.status() == SubmissionStatus.IN_PROGRESS) {
            return "Đang làm bài";
        }
        String reason = r.endReason() == null ? "" : r.endReason().displayName();
        return r.status() == SubmissionStatus.SUBMITTED ? "Chờ chấm – " + reason : reason;
    }

    private void load() {
        BackgroundTasks.run(page, () -> new Loaded(ws.app().statistics.listResults(ws.user(), session),
                ws.app().statistics.statsFor(ws.user(), session)), this::render);
    }

    private void render(Loaded data) {
        resultTable.setItems(FXCollections.observableArrayList(data.rows()));
        ResultStatistics st = data.stats();
        summaryLabel.setText(st.count() + " bài đã chấm   ·   cao nhất " + Formats.score(st.max())
                + "   ·   thấp nhất " + Formats.score(st.min()) + "   ·   trung bình " + Formats.score(st.average())
                + "   ·   kết thúc do vi phạm: " + st.terminatedByViolation());

        // phổ điểm hệ 10: đủ 11 cột 0..10 kể cả cột không có bài, để nhìn được hình dạng phân bố
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int bucket = 0; bucket <= 10; bucket++) {
            series.getData().add(new XYChart.Data<>(String.valueOf(bucket), st.getHistogram().getOrDefault(bucket, 0L)));
        }
        histogram.getData().setAll(List.of(series));

        List<ItemRate> rates = st.hardestItems(session.getExam().getItems().size()).stream()
                .map((Map.Entry<Integer, Double> e) -> new ItemRate(e.getKey(),
                        session.getExam().getItem(e.getKey()).getQuestion().getContent(), e.getValue()))
                .toList();
        itemTable.setItems(FXCollections.observableArrayList(rates));
    }

    /** Chấm ngay một lượt rồi đọc lại, để bài vừa nộp hiện ra không phải chờ máy chấm định kỳ. */
    @FXML
    private void onRefresh() {
        BackgroundTasks.run(page, () -> {
            ws.gradeNow();
            return Boolean.TRUE;
        }, ok -> load());
    }

    /** FR-35: xuất bảng điểm CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt). */
    @FXML
    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu bảng điểm");
        chooser.setInitialFileName("bang-diem-ca-" + session.getSessionId() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp CSV", "*.csv"));
        File file = chooser.showSaveDialog(ws.navigator().stage());
        if (file == null) {
            return;
        }
        BackgroundTasks.run(page, () -> {
            try {
                return ws.app().statistics.exportCsv(ws.user(), session, file.toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, path -> ErrorDialogs.info("Xuất bảng điểm", "Đã lưu bảng điểm vào " + path + "."));
    }

    @FXML
    private void onBack() {
        ws.open("staff-sessions.fxml", (StaffSessionsController c) -> c.init(ws));
    }
}
