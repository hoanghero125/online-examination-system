package com.group2.oes.ui.controller;

import com.group2.oes.model.Criteria;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.service.ImportResult;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Formats;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Ngân hàng câu hỏi của một môn: lọc, thêm, sửa, xóa, nhập từ tệp (FR-08, 11, 12, 13). */
public class QuestionBankController {

    private static final int MAX_ERRORS_SHOWN = 15;

    @FXML
    private ComboBox<Subject> subjectBox;
    @FXML
    private ComboBox<Integer> chapterBox;
    @FXML
    private ComboBox<Difficulty> difficultyBox;
    @FXML
    private ComboBox<QuestionType> typeBox;
    @FXML
    private TextField keywordField;
    @FXML
    private TableView<Question> table;
    @FXML
    private Label countLabel;
    @FXML
    private VBox page;

    private StaffWorkspace ws;
    private QuestionBank bank;

    public void init(StaffWorkspace ws) {
        this.ws = ws;
        Tables.labels(chapterBox, ch -> "Chương " + ch, "Mọi chương");
        Tables.labels(difficultyBox, Difficulty::displayName, "Mọi mức độ");
        Tables.labels(typeBox, QuestionType::displayName, "Mọi loại");
        difficultyBox.getItems().add(null);
        difficultyBox.getItems().addAll(Difficulty.values());
        typeBox.getItems().add(null);
        typeBox.getItems().addAll(QuestionType.values());

        table.getColumns().add(Tables.column("Mã", 60, Question::getQuestionId));
        table.getColumns().add(Tables.column("Loại", 120, q -> q.getType().displayName()));
        table.getColumns().add(Tables.column("Chương", 70, Question::getChapter));
        table.getColumns().add(Tables.column("Mức độ", 100, q -> q.getDifficulty().displayName()));
        table.getColumns().add(Tables.column("Điểm", 60, q -> Formats.score(q.getDefaultPoint())));
        table.getColumns().add(Tables.column("Nội dung", 520, Question::getContent));
        table.setRowFactory(tv -> {
            TableRow<Question> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });

        chapterBox.valueProperty().addListener((o, a, b) -> applyFilter());
        difficultyBox.valueProperty().addListener((o, a, b) -> applyFilter());
        typeBox.valueProperty().addListener((o, a, b) -> applyFilter());
        keywordField.textProperty().addListener((o, a, b) -> applyFilter());
        subjectBox.valueProperty().addListener((o, a, s) -> load());
        BackgroundTasks.run(page, () -> ws.app().subjectService.listSubjects(ws.user()), subjects -> {
            subjectBox.getItems().setAll(subjects);
            if (!subjects.isEmpty()) {
                subjectBox.getSelectionModel().selectFirst();
            }
        });
    }

    private void load() {
        Subject subject = subjectBox.getValue();
        if (subject == null) {
            return;
        }
        BackgroundTasks.run(page, () -> ws.app().bankService.loadBank(ws.user(), subject), b -> {
            bank = b;
            List<Integer> chapters = new ArrayList<>();
            chapters.add(null);
            b.findBy(Criteria.any()).stream().map(Question::getChapter).distinct().sorted().forEach(chapters::add);
            Integer keep = chapterBox.getValue();
            chapterBox.getItems().setAll(chapters);
            chapterBox.setValue(chapters.contains(keep) ? keep : null);
            applyFilter();
        });
    }

    private void applyFilter() {
        if (bank == null) {
            return;
        }
        List<Question> found = bank.findBy(new Criteria(chapterBox.getValue(), difficultyBox.getValue(),
                typeBox.getValue(), keywordField.getText()));
        table.setItems(FXCollections.observableArrayList(found));
        countLabel.setText(found.size() + " / " + bank.size() + " câu");
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onEdit() {
        Question q = table.getSelectionModel().getSelectedItem();
        if (q == null) {
            ErrorDialogs.info("Sửa câu hỏi", "Hãy chọn một câu hỏi trong bảng.");
            return;
        }
        openForm(q);
    }

    private void openForm(Question existing) {
        if (subjectBox.getValue() == null) {
            ErrorDialogs.info("Câu hỏi", "Bạn chưa được phân công môn nào.");
            return;
        }
        ws.navigator().openDialog("question-form.fxml", existing == null ? "Thêm câu hỏi" : "Sửa câu hỏi #" + existing.getQuestionId(),
                (QuestionFormController c) -> c.init(ws, subjectBox.getValue(), existing, this::load));
    }

    @FXML
    private void onDelete() {
        Question q = table.getSelectionModel().getSelectedItem();
        if (q == null) {
            ErrorDialogs.info("Xóa câu hỏi", "Hãy chọn một câu hỏi trong bảng.");
            return;
        }
        if (ErrorDialogs.confirm("Xóa câu hỏi", "Xóa câu hỏi #" + q.getQuestionId() + "?\n" + q.getContent(), "Xóa")) {
            BackgroundTasks.runVoid(page, () -> ws.app().bankService.deleteQuestion(ws.user(), q), this::load);
        }
    }

    /** FR-12: nhập từ tệp văn bản, mỗi dòng một câu; dòng lỗi được liệt kê, các dòng khác vẫn nhập. */
    @FXML
    private void onImport() {
        Subject subject = subjectBox.getValue();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn tệp câu hỏi");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp văn bản", "*.txt", "*.csv"));
        File file = chooser.showOpenDialog(ws.navigator().stage());
        if (file == null || subject == null) {
            return;
        }
        BackgroundTasks.run(page, () -> {
            try {
                return ws.app().bankService.importFile(ws.user(), subject, file.toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, r -> {
            showImportResult(r);
            load();
        });
    }

    static void showImportResult(ImportResult r) {
        StringBuilder sb = new StringBuilder("Nhập được " + r.imported() + " dòng, " + r.errors().size() + " dòng lỗi.");
        r.errors().stream().limit(MAX_ERRORS_SHOWN).forEach(e -> sb.append("\n• ").append(e));
        if (r.errors().size() > MAX_ERRORS_SHOWN) {
            sb.append("\n… và ").append(r.errors().size() - MAX_ERRORS_SHOWN).append(" dòng lỗi khác.");
        }
        ErrorDialogs.info("Kết quả nhập tệp", sb.toString());
    }

    /** Gợi ý định dạng tệp nhập, hiện khi bấm nút "?". */
    @FXML
    private void onImportHelp() {
        ErrorDialogs.info("Định dạng tệp câu hỏi", String.join("\n", Arrays.asList(
                "Mỗi dòng một câu, các trường cách nhau bằng dấu |:",
                "LOẠI | chương | mức độ | điểm | nội dung | đáp án",
                "Phương án đúng có dấu * ở đầu, các phương án cách nhau bằng dấu ;",
                "",
                "SINGLE_CHOICE|1|EASY|1|Java chạy trên gì?|*JVM;CLR;V8",
                "MULTI_CHOICE|2|MEDIUM|2|Chọn kiểu nguyên thủy|*int;String;*boolean",
                "TRUE_FALSE|1|EASY|1|Java hỗ trợ đa kế thừa lớp|false",
                "FILL_IN_BLANK|3|HARD|1|Từ khóa để kế thừa lớp là ...|extends")));
    }
}
