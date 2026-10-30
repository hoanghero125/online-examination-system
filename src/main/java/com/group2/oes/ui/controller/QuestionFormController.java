package com.group2.oes.ui.controller;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Formats;
import com.group2.oes.ui.StaffWorkspace;
import com.group2.oes.ui.Tables;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Biểu mẫu thêm / sửa câu hỏi (FR-09, FR-10). Chọn loại thì phần nhập đáp án đổi theo loại.
 *
 * Controller chỉ gom dữ liệu thành QuestionData; mọi kiểm tra (đủ phương án, đúng một đáp án đúng...)
 * nằm trong constructor của các lớp câu hỏi, gọi qua QuestionFactory ở service. Lỗi nào thuộc ô nào thì
 * hiện ngay dưới ô đó, dựa vào mã lỗi ổn định của ValidationException.
 */
public class QuestionFormController {

    private static final int DEFAULT_OPTIONS = 4;

    @FXML
    private ComboBox<QuestionType> typeBox;
    @FXML
    private TextArea contentArea;
    @FXML
    private Spinner<Integer> chapterSpinner;
    @FXML
    private ComboBox<Difficulty> difficultyBox;
    @FXML
    private TextField pointField;
    @FXML
    private VBox optionsPane;
    @FXML
    private VBox optionRows;
    @FXML
    private VBox trueFalsePane;
    @FXML
    private RadioButton trueRadio;
    @FXML
    private RadioButton falseRadio;
    @FXML
    private VBox fillPane;
    @FXML
    private TextArea acceptedArea;
    @FXML
    private CheckBox caseBox;
    @FXML
    private Label contentError;
    @FXML
    private Label chapterError;
    @FXML
    private Label pointError;
    @FXML
    private Label optionsError;
    @FXML
    private Label trueFalseError;
    @FXML
    private Label acceptedError;
    @FXML
    private VBox form;

    private StaffWorkspace ws;
    private Subject subject;
    private Question existing;
    private Runnable onSaved;
    private final List<OptionRow> rows = new ArrayList<>();
    private boolean ready;   // init xong chưa: lúc init, việc đặt sẵn loại câu hỏi không được tạo thêm dòng

    private record OptionRow(HBox box, TextField content, CheckBox correct) {
    }

    public void init(StaffWorkspace ws, Subject subject, Question existing, Runnable onSaved) {
        this.ws = ws;
        this.subject = subject;
        this.existing = existing;
        this.onSaved = onSaved;
        Tables.labels(typeBox, QuestionType::displayName, "");
        Tables.labels(difficultyBox, Difficulty::displayName, "");
        typeBox.getItems().setAll(QuestionType.values());
        difficultyBox.getItems().setAll(Difficulty.values());
        typeBox.valueProperty().addListener((o, a, type) -> showPaneFor(type));

        if (existing == null) {
            typeBox.setValue(QuestionType.SINGLE_CHOICE);
            difficultyBox.setValue(Difficulty.EASY);
            pointField.setText("1");
        } else {
            fill(QuestionData.from(existing), existing.getType());
        }
        addDefaultOptionsIfEmpty(typeBox.getValue());
        ready = true;   // từ đây, đổi loại câu hỏi là do người dùng chọn
        clearErrors();
    }

    private void fill(QuestionData d, QuestionType type) {
        typeBox.setValue(type);
        contentArea.setText(d.content());
        chapterSpinner.getValueFactory().setValue(d.chapter());
        difficultyBox.setValue(d.difficulty());
        pointField.setText(Formats.score(d.defaultPoint()));
        if (d.options() != null) {
            d.options().forEach(o -> addOptionRow(o.getContent(), o.isCorrect()));
        }
        if (d.correctValue() != null) {
            trueRadio.setSelected(d.correctValue());
            falseRadio.setSelected(!d.correctValue());
        }
        if (d.acceptedAnswers() != null) {
            acceptedArea.setText(String.join("\n", d.acceptedAnswers()));
            caseBox.setSelected(d.caseSensitive());
        }
    }

    private void showPaneFor(QuestionType type) {
        boolean choice = type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTI_CHOICE;
        show(optionsPane, choice);
        show(trueFalsePane, type == QuestionType.TRUE_FALSE);
        show(fillPane, type == QuestionType.FILL_IN_BLANK);
        if (ready) {
            addDefaultOptionsIfEmpty(type);
        }
        Stage window = form.getScene() == null ? null : (Stage) form.getScene().getWindow();
        if (window != null) {
            window.sizeToScene();
        }
    }

    /** Câu trắc nghiệm chưa có phương án nào thì tạo sẵn vài dòng trống cho dễ nhập. */
    private void addDefaultOptionsIfEmpty(QuestionType type) {
        boolean choice = type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTI_CHOICE;
        if (choice && rows.isEmpty()) {
            for (int i = 0; i < DEFAULT_OPTIONS; i++) {
                addOptionRow("", false);
            }
        }
    }

    private static void show(VBox pane, boolean visible) {
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    @FXML
    private void onAddOption() {
        addOptionRow("", false);
    }

    private void addOptionRow(String content, boolean correct) {
        TextField text = new TextField(content);
        text.setPromptText("Nội dung phương án");
        HBox.setHgrow(text, Priority.ALWAYS);
        CheckBox isCorrect = new CheckBox("Đúng");
        isCorrect.setSelected(correct);
        Button remove = new Button("Xóa");
        HBox box = new HBox(8, text, isCorrect, remove);
        box.getStyleClass().add("option-row");
        OptionRow row = new OptionRow(box, text, isCorrect);
        remove.setOnAction(e -> {
            rows.remove(row);
            optionRows.getChildren().remove(box);
        });
        rows.add(row);
        optionRows.getChildren().add(box);
    }

    @FXML
    private void onSave() {
        clearErrors();
        QuestionData data;
        try {
            data = collect();
        } catch (ValidationException e) {
            showError(e);
            return;
        }
        QuestionType type = typeBox.getValue();
        QuestionData toSave = data;
        BackgroundTasks.run(() -> existing == null
                ? ws.app().bankService.addQuestion(ws.user(), type, toSave)
                : ws.app().bankService.updateQuestion(ws.user(), type, toSave), saved -> {
            close();
            onSaved.run();
        }, error -> {
            if (error instanceof ValidationException v) {
                showError(v);
            } else {
                ErrorDialogs.show(error);
            }
        });
    }

    /** Đọc các ô nhập thành QuestionData. Chỉ đổi kiểu dữ liệu (chữ → số), không kiểm tra nghiệp vụ. */
    private QuestionData collect() {
        double point;
        try {
            point = Double.parseDouble(pointField.getText().strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidationException("POINT_INVALID", "Điểm phải là số, ví dụ 1 hoặc 0.5.");
        }
        QuestionData d = QuestionData.basic(subject, contentArea.getText(), chapterSpinner.getValue(),
                difficultyBox.getValue(), point);
        if (existing != null) {
            d = d.withId(existing.getQuestionId());
        }
        return switch (typeBox.getValue()) {
            case SINGLE_CHOICE, MULTI_CHOICE -> d.withOptions(rows.stream()
                    .map(r -> AnswerOption.of(r.content().getText(), r.correct().isSelected())).toList());
            case TRUE_FALSE -> {
                if (!trueRadio.isSelected() && !falseRadio.isSelected()) {
                    yield d;   // QuestionFactory sẽ báo TF_NEEDS_VALUE
                }
                yield d.withCorrectValue(trueRadio.isSelected());
            }
            case FILL_IN_BLANK -> d.withAccepted(Arrays.stream(acceptedArea.getText().split("\\R"))
                    .map(String::strip).filter(s -> !s.isEmpty()).toList(), caseBox.isSelected());
        };
    }

    /** Mã lỗi → ô hiện lỗi. Mã nào không có trong bảng thì hiện ở ô phương án/đáp án hoặc hộp thoại. */
    private void showError(ValidationException e) {
        Map<String, Label> byCode = Map.ofEntries(
                Map.entry("QUESTION_EMPTY", contentError),
                Map.entry("CHAPTER_INVALID", chapterError),
                Map.entry("POINT_INVALID", pointError),
                Map.entry("OPTIONS_TOO_FEW", optionsError),
                Map.entry("OPTION_EMPTY", optionsError),
                Map.entry("SINGLE_NEEDS_ONE_CORRECT", optionsError),
                Map.entry("MULTI_NEEDS_CORRECT", optionsError),
                Map.entry("TF_NEEDS_VALUE", trueFalseError),
                Map.entry("FILL_NEEDS_ANSWER", acceptedError));
        Label target = byCode.get(e.getCode());
        if (target == null) {
            ErrorDialogs.show(e);
            return;
        }
        target.setText(e.getUserMessage());
        target.setVisible(true);
        target.setManaged(true);
    }

    private void clearErrors() {
        for (Label l : List.of(contentError, chapterError, pointError, optionsError, trueFalseError, acceptedError)) {
            l.setText("");
            l.setVisible(false);
            l.setManaged(false);
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) form.getScene().getWindow()).close();
    }
}
