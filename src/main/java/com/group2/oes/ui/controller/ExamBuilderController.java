package com.group2.oes.ui.controller;

import com.group2.oes.model.Criteria;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamMatrix;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.Question;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Formats;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tạo đề thi: chọn tay từng câu (FR-14, FR-16) hoặc sinh theo ma trận chương × mức độ (FR-15).
 * Đề tạo xong chưa khóa; đề chỉ khóa khi được dùng mở ca thi (BR-09).
 */
public class ExamBuilderController {

    @FXML
    private Label subjectLabel;
    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<MultiChoicePolicy> policyBox;
    @FXML
    private TabPane tabs;
    @FXML
    private Tab manualTab;
    @FXML
    private TextField keywordField;
    @FXML
    private TableView<Question> bankTable;
    @FXML
    private TableView<Pick> pickedTable;
    @FXML
    private TextField defaultPointField;
    @FXML
    private Label totalLabel;
    @FXML
    private GridPane matrixGrid;
    @FXML
    private TextField pointEachField;
    @FXML
    private Label matrixResult;
    @FXML
    private VBox page;

    private StaffWorkspace ws;
    private Subject subject;
    private QuestionBank bank;
    private final ObservableList<Pick> picks = FXCollections.observableArrayList();
    private final Map<Cell, Spinner<Integer>> matrixInputs = new HashMap<>();

    /** Một câu đã chọn vào đề, điểm sửa được ngay trong bảng. */
    public static final class Pick {
        private final Question question;
        private final SimpleObjectProperty<Double> point;

        Pick(Question question, double point) {
            this.question = question;
            this.point = new SimpleObjectProperty<>(point);
        }
    }

    private record Cell(int chapter, Difficulty difficulty) {
    }

    public void init(StaffWorkspace ws, Subject subject) {
        this.ws = ws;
        this.subject = subject;
        subjectLabel.setText(subject.toString());
        Tables.labels(policyBox, MultiChoicePolicy::displayName, "");
        policyBox.getItems().setAll(MultiChoicePolicy.values());
        policyBox.setValue(MultiChoicePolicy.PARTIAL);

        bankTable.getColumns().add(Tables.column("Mã", 55, Question::getQuestionId));
        bankTable.getColumns().add(Tables.column("Chương", 60, Question::getChapter));
        bankTable.getColumns().add(Tables.column("Mức độ", 90, q -> q.getDifficulty().displayName()));
        bankTable.getColumns().add(Tables.column("Nội dung", 300, Question::getContent));

        pickedTable.setEditable(true);
        pickedTable.getColumns().add(Tables.column("STT", 45, p -> picks.indexOf(p) + 1));
        pickedTable.getColumns().add(Tables.column("Nội dung", 280, p -> p.question.getContent()));
        TableColumn<Pick, Double> pointCol = new TableColumn<>("Điểm (sửa được)");
        pointCol.setPrefWidth(110);
        pointCol.setCellValueFactory(c -> c.getValue().point);
        pointCol.setCellFactory(TextFieldTableCell.forTableColumn(new PointConverter()));
        pointCol.setOnEditCommit(e -> {
            if (e.getNewValue() != null && e.getNewValue() > 0) {
                e.getRowValue().point.set(e.getNewValue());
            }
            pickedTable.refresh();
            updateTotal();
        });
        pickedTable.getColumns().add(pointCol);
        pickedTable.setItems(picks);

        keywordField.textProperty().addListener((o, a, b) -> filterBank());
        BackgroundTasks.run(page, () -> ws.app().bankService.loadBank(ws.user(), subject), b -> {
            bank = b;
            filterBank();
            buildMatrix();
        });
        updateTotal();
    }

    /** Ô điểm gõ sai (chữ, số âm) thì giữ điểm cũ thay vì làm hỏng bảng. */
    private static final class PointConverter extends StringConverter<Double> {
        @Override
        public String toString(Double d) {
            return d == null ? "" : Formats.score(d);
        }

        @Override
        public Double fromString(String s) {
            try {
                return Double.parseDouble(s.strip().replace(',', '.'));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // ------------------------------------------------------------ chọn tay

    private void filterBank() {
        if (bank != null) {
            bankTable.setItems(FXCollections.observableArrayList(
                    bank.findBy(new Criteria(null, null, null, keywordField.getText()))));
        }
    }

    @FXML
    private void onAddPick() {
        Question q = bankTable.getSelectionModel().getSelectedItem();
        if (q == null) {
            return;
        }
        if (picks.stream().anyMatch(p -> p.question.getQuestionId() == q.getQuestionId())) {
            ErrorDialogs.info("Chọn câu", "Câu #" + q.getQuestionId() + " đã có trong đề.");
            return;
        }
        double point = parsePoint(defaultPointField, q.getDefaultPoint());
        picks.add(new Pick(q, point));
        updateTotal();
    }

    @FXML
    private void onRemovePick() {
        Pick p = pickedTable.getSelectionModel().getSelectedItem();
        if (p != null) {
            picks.remove(p);
            pickedTable.refresh();
            updateTotal();
        }
    }

    private void updateTotal() {
        totalLabel.setText("Đã chọn " + picks.size() + " câu, tổng " + Formats.score(
                picks.stream().mapToDouble(p -> p.point.get()).sum()) + " điểm");
    }

    // ------------------------------------------------------------ theo ma trận

    /** Bảng chương × mức độ, mỗi ô một ô số và số câu ngân hàng đang có. */
    private void buildMatrix() {
        matrixGrid.getChildren().clear();
        matrixInputs.clear();
        matrixGrid.add(new Label("Chương"), 0, 0);
        Difficulty[] levels = Difficulty.values();
        for (int j = 0; j < levels.length; j++) {
            Label h = new Label(levels[j].displayName());
            h.getStyleClass().add("field-label");
            matrixGrid.add(h, j + 1, 0);
        }
        List<Integer> chapters = bank.findBy(Criteria.any()).stream().map(Question::getChapter).distinct().sorted().toList();
        for (int i = 0; i < chapters.size(); i++) {
            int ch = chapters.get(i);
            matrixGrid.add(new Label("Chương " + ch), 0, i + 1);
            for (int j = 0; j < levels.length; j++) {
                int have = bank.findBy(new Criteria(ch, levels[j], null, null)).size();
                Spinner<Integer> count = new Spinner<>(0, 99, 0);
                count.setPrefWidth(80);
                count.setEditable(true);
                matrixInputs.put(new Cell(ch, levels[j]), count);
                Label haveLabel = new Label("/ có " + have);
                haveLabel.getStyleClass().add("muted");
                matrixGrid.add(new HBox(6, count, haveLabel), j + 1, i + 1);
            }
        }
    }

    private ExamMatrix readMatrix() {
        ExamMatrix m = new ExamMatrix();
        matrixInputs.forEach((cell, spinner) -> {
            if (spinner.getValue() > 0) {
                m.add(cell.chapter(), cell.difficulty(), spinner.getValue());
            }
        });
        return m;
    }

    /** FR-15: báo thiếu câu ở ô nào trước khi sinh đề. */
    @FXML
    private void onCheckMatrix() {
        ExamMatrix m = readMatrix();
        List<String> shortages = bank.findShortages(m);
        matrixResult.setText(m.totalRequired() == 0 ? "Hãy nhập số câu cho ít nhất một ô."
                : shortages.isEmpty() ? "Đủ câu: ma trận lấy " + m.totalRequired() + " câu."
                : "Thiếu câu: " + String.join("; ", shortages) + ".");
    }

    // ------------------------------------------------------------ lưu

    @FXML
    private void onCreate() {
        String title = titleField.getText();
        MultiChoicePolicy policy = policyBox.getValue();
        if (tabs.getSelectionModel().getSelectedItem() == manualTab) {
            Map<Integer, Double> chosen = new LinkedHashMap<>();   // giữ đúng thứ tự đã chọn
            picks.forEach(p -> chosen.put(p.question.getQuestionId(), p.point.get()));
            BackgroundTasks.run(page, () -> ws.app().examService.createManual(ws.user(), bank, title, policy, chosen),
                    this::created);
        } else {
            ExamMatrix m = readMatrix();
            double each = parsePoint(pointEachField, 1);
            BackgroundTasks.run(page, () -> ws.app().examService.generate(ws.user(), bank, title, policy, m, each,
                    new Random()), this::created);
        }
    }

    private void created(Exam exam) {
        ErrorDialogs.info("Tạo đề", "Đã tạo đề \"" + exam.getTitle() + "\": " + exam.getItems().size() + " câu, "
                + Formats.score(exam.getTotalPoint()) + " điểm.");
        ws.open("exam-list.fxml", (ExamListController c) -> c.init(ws));
    }

    private static double parsePoint(TextField field, double fallback) {
        String s = field.getText() == null ? "" : field.getText().strip().replace(',', '.');
        if (s.isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new ValidationException("POINT_INVALID", "Điểm phải là số, ví dụ 1 hoặc 0.5.");
        }
    }

    @FXML
    private void onBack() {
        ws.open("exam-list.fxml", (ExamListController c) -> c.init(ws));
    }
}
