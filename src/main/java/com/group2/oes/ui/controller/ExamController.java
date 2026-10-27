package com.group2.oes.ui.controller;

import com.group2.oes.app.ClientContext;
import com.group2.oes.client.AnswerFormat;
import com.group2.oes.client.AnswerSheet;
import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.client.PaperItem;
import com.group2.oes.client.PaperOption;
import com.group2.oes.client.ServerClock;
import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.User;
import com.group2.oes.model.ViolationPolicy;
import com.group2.oes.service.ProctoringService.ViolationResult;
import com.group2.oes.ui.AnswerSaver;
import com.group2.oes.ui.AppShell;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.ExamProctor;
import com.group2.oes.ui.Formats;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Màn hình làm bài (FR-21 → FR-29).
 *
 * Controller chỉ vẽ và nối các phần lại; các việc khó tách ra lớp riêng:
 * - AnswerSheet (client): câu trả lời đang hiện trên màn hình, đếm câu đã làm / đánh dấu;
 * - AnswerSaver (ui): hàng đợi lưu bài, thử lại khi mất mạng;
 * - ExamProctor (ui): toàn màn hình và phát hiện rời cửa sổ;
 * - ServerClock (client): giờ máy chủ cho đồng hồ đếm ngược;
 * - StudentExamService, ProctoringService: mọi quyết định nghiệp vụ.
 */
public class ExamController {

    private static final int RESYNC_EVERY_SECONDS = 60;

    @FXML
    private Label examTitleLabel;
    @FXML
    private Label clockLabel;
    @FXML
    private Label violationLabel;
    @FXML
    private Label offlineBanner;
    @FXML
    private Label noticeBanner;
    @FXML
    private FlowPane questionGrid;
    @FXML
    private Label questionHeaderLabel;
    @FXML
    private Label questionContentLabel;
    @FXML
    private VBox answerBox;
    @FXML
    private ToggleButton flagButton;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button submitButton;

    private AppShell shell;
    private ClientContext client;
    private User user;
    private ExamAttempt attempt;
    private List<PaperItem> items;

    private AnswerSheet sheet;
    private AnswerSaver saver;
    private ExamProctor proctor;
    private ServerClock clock;
    private Timeline ticker;
    private final List<Button> gridButtons = new ArrayList<>();

    private int current;              // vị trí câu đang xem trong đề đã trộn (0..n-1)
    private boolean populating;       // đang điền sẵn câu trả lời cũ vào ô: không coi là thí sinh đổi ý
    private boolean warned;
    private boolean submitting;
    private boolean finished;
    private int secondsSinceSync;
    private boolean reporting;        // đang gửi một lần vi phạm lên CSDL
    private Instant queuedLeave;      // sự kiện tới trong lúc đang gửi, gửi tiếp khi xong

    public void init(AppShell shell, ClientContext client, User user, ExamAttempt attempt, LocalDateTime serverNow) {
        this.shell = shell;
        this.client = client;
        this.user = user;
        this.attempt = attempt;
        this.items = attempt.paper().getItems();
        this.sheet = new AnswerSheet(attempt.paper(), attempt.state());
        this.clock = new ServerClock(serverNow);
        this.saver = new AnswerSaver(new AnswerSaver.Sink() {
            @Override
            public void saveAnswer(int order, String response) {
                client.exams.saveAnswer(user, attempt, order, response);
            }

            @Override
            public void saveFlag(int order, boolean flagged) {
                client.exams.setFlag(user, attempt, order, flagged);
            }
        }, locked -> finishFromServer());

        examTitleLabel.setText(attempt.session().subjectCode() + " – " + attempt.session().examTitle());
        offlineBanner.visibleProperty().bind(saver.offlineProperty());
        offlineBanner.managedProperty().bind(saver.offlineProperty());
        showNotice(null);
        updateViolations(attempt.state().getViolationCount());
        buildGrid();
        show(0);

        ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> onTick()));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
        onTick();

        proctor = new ExamProctor(shell.navigator().stage(), this::onLeave);
        Platform.runLater(proctor::start);   // sau khi màn hình đã gắn vào cửa sổ
    }

    // ------------------------------------------------------------ câu hỏi và câu trả lời

    private void buildGrid() {
        for (int i = 0; i < items.size(); i++) {
            int index = i;
            Button b = new Button(String.valueOf(i + 1));
            b.getStyleClass().add("q-button");
            b.setOnAction(e -> show(index));
            gridButtons.add(b);
            questionGrid.getChildren().add(b);
        }
    }

    private void show(int index) {
        current = index;
        PaperItem item = items.get(index);
        questionHeaderLabel.setText("Câu " + (index + 1) + "/" + items.size() + "  ·  " + Formats.score(item.point()) + " điểm");
        questionContentLabel.setText(item.content());
        populating = true;
        answerBox.getChildren().setAll(switch (item.type()) {
            case SINGLE_CHOICE -> radios(item, item.options().stream().map(o -> new Choice(o.content(), String.valueOf(o.optionId()))).toList());
            case TRUE_FALSE -> radios(item, List.of(new Choice("Đúng", "true"), new Choice("Sai", "false")));
            case MULTI_CHOICE -> checkboxes(item);
            case FILL_IN_BLANK -> List.of(textInput(item));
        });
        flagButton.setSelected(sheet.isFlagged(item.order()));
        populating = false;
        prevButton.setDisable(index == 0);
        nextButton.setDisable(index == items.size() - 1);
        refreshGrid();
    }

    private record Choice(String label, String value) {
    }

    private List<Node> radios(PaperItem item, List<Choice> choices) {
        ToggleGroup group = new ToggleGroup();
        List<Node> nodes = new ArrayList<>();
        String saved = sheet.response(item.order());
        for (int i = 0; i < choices.size(); i++) {
            Choice c = choices.get(i);
            RadioButton r = new RadioButton(letter(i, item) + c.label());
            r.setUserData(c.value());
            r.setToggleGroup(group);
            r.setWrapText(true);
            r.setSelected(c.value().equals(saved));
            nodes.add(r);
        }
        group.selectedToggleProperty().addListener((obs, old, sel) ->
                onAnswer(item.order(), sel == null ? null : (String) sel.getUserData()));
        return nodes;
    }

    private List<Node> checkboxes(PaperItem item) {
        Set<Integer> chosen = AnswerFormat.parseOptionIds(sheet.response(item.order()));
        List<CheckBox> boxes = new ArrayList<>();
        for (int i = 0; i < item.options().size(); i++) {
            PaperOption o = item.options().get(i);
            CheckBox cb = new CheckBox(letter(i, item) + o.content());
            cb.setUserData(o.optionId());
            cb.setWrapText(true);
            cb.setSelected(chosen.contains(o.optionId()));
            boxes.add(cb);
        }
        for (CheckBox cb : boxes) {
            cb.selectedProperty().addListener((obs, old, sel) -> {
                Set<Integer> ids = new TreeSet<>();
                boxes.stream().filter(CheckBox::isSelected).forEach(b -> ids.add((Integer) b.getUserData()));
                onAnswer(item.order(), AnswerFormat.joinOptionIds(ids));
            });
        }
        Label hint = new Label("Chọn tất cả phương án đúng.");
        hint.getStyleClass().add("muted");
        List<Node> nodes = new ArrayList<>(boxes);
        nodes.add(0, hint);
        return nodes;
    }

    private TextField textInput(PaperItem item) {
        TextField field = new TextField(sheet.response(item.order()) == null ? "" : sheet.response(item.order()));
        field.setPromptText("Nhập câu trả lời");
        field.textProperty().addListener((obs, old, text) -> onAnswer(item.order(), text));
        Platform.runLater(field::requestFocus);
        return field;
    }

    /** A. B. C. ... chỉ để thí sinh dễ đọc; giá trị lưu vẫn là mã phương án. */
    private static String letter(int index, PaperItem item) {
        return item.options().isEmpty() ? "" : (char) ('A' + index) + ".  ";
    }

    private void onAnswer(int order, String response) {
        if (populating || finished) {
            return;
        }
        sheet.setResponse(order, response);
        saver.answerChanged(order, response);
        refreshGrid();
    }

    @FXML
    private void onFlag() {
        int order = items.get(current).order();
        sheet.setFlag(order, flagButton.isSelected());
        saver.flagChanged(order, flagButton.isSelected());
        refreshGrid();
    }

    @FXML
    private void onPrev() {
        show(current - 1);
    }

    @FXML
    private void onNext() {
        show(current + 1);
    }

    private void refreshGrid() {
        for (int i = 0; i < gridButtons.size(); i++) {
            Button b = gridButtons.get(i);
            int order = items.get(i).order();
            b.getStyleClass().removeAll("q-answered", "q-flagged", "q-current");
            if (sheet.isAnswered(order)) {
                b.getStyleClass().add("q-answered");
            }
            if (sheet.isFlagged(order)) {
                b.getStyleClass().add("q-flagged");
            }
            if (i == current) {
                b.getStyleClass().add("q-current");
            }
        }
    }

    // ------------------------------------------------------------ đồng hồ (FR-23)

    private void onTick() {
        if (finished) {
            return;
        }
        LocalDateTime now = clock.now();
        SessionWindow window = attempt.session().window();
        long remaining = window.remainingSeconds(now);
        clockLabel.setText(SessionWindow.formatRemaining(remaining));
        if (!warned && window.isAlmostOver(now)) {
            warned = true;
            clockLabel.getStyleClass().add("clock-warning");
            // dải thông báo trong màn hình, không dùng hộp thoại: hộp thoại làm mất tiêu điểm
            showNotice("Còn dưới " + SessionWindow.ALMOST_OVER.toMinutes() + " phút. Hãy kiểm tra lại bài làm.");
        }
        if (remaining == 0) {
            submit(EndReason.TIME_OUT);
            return;
        }
        if (++secondsSinceSync >= RESYNC_EVERY_SECONDS) {
            secondsSinceSync = 0;
            BackgroundTasks.run(client.exams::serverTime, clock::resync, error -> {
                // mất mạng thì vẫn đếm theo lần đồng bộ trước, lần sau thử lại
            });
        }
    }

    // ------------------------------------------------------------ vi phạm (FR-28, FR-29)

    /** Mỗi lần gửi một sự kiện; sự kiện tới giữa chừng thì gửi tiếp sau, để số lần hiện trên màn hình không nhảy lùi. */
    private void onLeave(Instant when) {
        if (finished) {
            return;
        }
        if (reporting) {
            queuedLeave = when;
            return;
        }
        reporting = true;
        BackgroundTasks.run(() -> client.proctoring.report(user, attempt, when), result -> {
            reporting = false;
            showNotice(null);
            handleViolation(result);
            if (queuedLeave != null && !finished) {
                Instant next = queuedLeave;
                queuedLeave = null;
                onLeave(next);
            }
        }, error -> {
            // Mất mạng đúng lúc rời cửa sổ: không được bỏ qua sự kiện, nếu không thí sinh cố ý ngắt mạng
            // mỗi lần Alt+Tab sẽ không bao giờ bị tính vi phạm. Giữ lại thời điểm gốc, 3 giây sau gửi lại.
            reporting = false;
            if (queuedLeave == null || when.isBefore(queuedLeave)) {
                queuedLeave = when;
            }
            showNotice("Mất kết nối. Lần rời cửa sổ vừa rồi sẽ được ghi nhận khi có kết nối lại.");
            PauseTransition retry = new PauseTransition(Duration.seconds(3));
            retry.setOnFinished(e -> retryQueuedLeave());
            retry.play();
        });
    }

    private void retryQueuedLeave() {
        if (queuedLeave != null && !reporting && !finished) {
            Instant next = queuedLeave;
            queuedLeave = null;
            onLeave(next);
        }
    }

    private void handleViolation(ViolationResult r) {
        AttemptState state = attempt.state();
        if (r.terminated() || state.isLocked()) {
            finish(state);
            return;
        }
        if (r.merged()) {
            return;
        }
        updateViolations(r.count());
        ButtonType back = new ButtonType("Quay lại làm bài", ButtonBar.ButtonData.OK_DONE);
        Alert alert = ErrorDialogs.alert(Alert.AlertType.WARNING, "Cảnh báo vi phạm quy chế",
                "Bạn đã rời cửa sổ làm bài (lần " + r.count() + "/" + ViolationPolicy.MAX_VIOLATIONS + "). "
                        + "Còn " + r.remaining() + " lần nữa bài thi sẽ bị kết thúc.");
        alert.getButtonTypes().setAll(back);
        proctor.showDialog(alert::showAndWait);
    }

    private void updateViolations(int count) {
        violationLabel.setText("Vi phạm: " + count + "/" + ViolationPolicy.MAX_VIOLATIONS);
        violationLabel.getStyleClass().remove("violation-active");
        if (count > 0) {
            violationLabel.getStyleClass().add("violation-active");
        }
    }

    // ------------------------------------------------------------ nộp bài (FR-25)

    @FXML
    private void onSubmit() {
        String message = "Còn " + sheet.unansweredCount() + " câu chưa trả lời, " + sheet.flaggedCount()
                + " câu đang đánh dấu xem lại.\nNộp xong sẽ không sửa được nữa. Bạn chắc chắn nộp bài?";
        boolean[] confirmed = {false};
        proctor.showDialog(() -> confirmed[0] = ErrorDialogs.confirm("Nộp bài", message, "Nộp bài"));
        if (confirmed[0]) {
            submit(EndReason.MANUAL);
        }
    }

    /** Lưu hết câu trả lời đang chờ rồi mới nộp. Mất mạng thì 3 giây thử lại, không mất bài. */
    private void submit(EndReason reason) {
        if (submitting || finished) {
            return;
        }
        submitting = true;
        submitButton.setDisable(true);
        saver.stop();
        showNotice("Đang nộp bài…");
        BackgroundTasks.run(() -> {
            try {
                saver.drain();
            } catch (SessionRuleException tooLate) {
                // ca đã đóng: câu chưa lưu kịp không còn được ghi (BR-04), vẫn nộp phần đã lưu
            }
            return client.exams.submit(user, attempt, reason);
        }, this::finish, error -> {
            submitting = false;
            showNotice("Chưa nộp được do mất kết nối. Đang thử lại…");
            PauseTransition retry = new PauseTransition(Duration.seconds(3));
            retry.setOnFinished(e -> submit(reason));
            retry.play();
        });
    }

    /** CSDL báo bài đã khóa (máy chấm đóng ca, hoặc máy khác đã nộp): đọc trạng thái thật rồi sang màn hình kết quả. */
    private void finishFromServer() {
        BackgroundTasks.run(() -> client.exams.refresh(user, attempt.state().getSessionId()), this::finish,
                error -> finish(attempt.state()));
    }

    private void finish(AttemptState state) {
        if (finished) {
            return;
        }
        finished = true;
        ticker.stop();
        saver.stop();
        proctor.stop();
        shell.navigator().show("result.fxml",
                (ResultController c) -> c.init(shell, client, user, attempt.session(), state));
    }

    private void showNotice(String text) {
        noticeBanner.setText(text == null ? "" : text);
        noticeBanner.setVisible(text != null);
        noticeBanner.setManaged(text != null);
    }
}
