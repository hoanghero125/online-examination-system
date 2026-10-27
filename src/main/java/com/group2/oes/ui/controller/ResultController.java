package com.group2.oes.ui.controller;

import com.group2.oes.app.ClientContext;
import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ReviewItem;
import com.group2.oes.client.SessionInfo;
import com.group2.oes.model.User;
import com.group2.oes.ui.AppShell;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.Formats;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Kết quả sau khi nộp (FR-33). Máy thí sinh không chấm: chờ máy chấm của giảng viên, hỏi lại 2 giây một lần,
 * tối đa 60 giây. Đáp án đúng chỉ hiện khi ca đã đóng và giảng viên cho xem (CSDL quyết định qua v_review).
 */
public class ResultController {

    private static final Duration POLL_EVERY = Duration.seconds(2);
    private static final int MAX_POLLS = 30;   // 30 x 2 giây = 60 giây

    @FXML
    private Label titleLabel;
    @FXML
    private VBox waitingBox;
    @FXML
    private ProgressIndicator spinner;
    @FXML
    private Label waitingLabel;
    @FXML
    private Button retryButton;
    @FXML
    private VBox resultBox;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label correctLabel;
    @FXML
    private Label reasonLabel;
    @FXML
    private Label violationLabel;
    @FXML
    private Label reviewNote;
    @FXML
    private VBox reviewList;

    private AppShell shell;
    private ClientContext client;
    private User user;
    private SessionInfo session;
    private Timeline poller;
    private int polls;

    public void init(AppShell shell, ClientContext client, User user, SessionInfo session, AttemptState state) {
        this.shell = shell;
        this.client = client;
        this.user = user;
        this.session = session;
        titleLabel.setText(session.subjectCode() + " – " + session.examTitle());
        violationLabel.setVisible(state.isTerminatedByViolation());
        violationLabel.setManaged(state.isTerminatedByViolation());
        if (state.isGraded()) {
            showResult(state);
        } else {
            startPolling();
        }
    }

    private void startPolling() {
        polls = 0;
        waitingBox.setVisible(true);
        resultBox.setVisible(false);
        spinner.setVisible(true);
        retryButton.setVisible(false);
        waitingLabel.setText("Đã nộp bài. Đang chấm bài…");
        poller = new Timeline(new KeyFrame(POLL_EVERY, e -> poll()));
        poller.setCycleCount(MAX_POLLS);
        poller.setOnFinished(e -> {
            spinner.setVisible(false);
            retryButton.setVisible(true);
            waitingLabel.setText("Bài đã được nộp. Kết quả sẽ có khi giảng viên mở máy chấm.");
        });
        poller.play();
    }

    private void poll() {
        BackgroundTasks.run(() -> client.exams.refresh(user, session.sessionId()), state -> {
            if (state.isGraded() && poller != null) {
                poller.stop();
                poller = null;
                showResult(state);
            }
        }, error -> waitingLabel.setText("Đang chấm bài… (mất kết nối, đang thử lại)"));
    }

    private void showResult(AttemptState state) {
        waitingBox.setVisible(false);
        resultBox.setVisible(true);
        scoreLabel.setText(Formats.score(state.getTotalScore()) + " / " + Formats.score(session.totalPoint()));
        reasonLabel.setText(state.getEndReason() == null ? "" : state.getEndReason().displayName());
        BackgroundTasks.run(() -> client.exams.review(user, session.sessionId()), this::showReview,
                error -> reviewNote.setText("Không tải được chi tiết bài làm."));
    }

    private void showReview(List<ReviewItem> items) {
        correctLabel.setText(ReviewItem.fullMarkCount(items) + " / " + items.size() + " câu đúng hoàn toàn");
        reviewList.getChildren().clear();
        boolean hasAnswers = items.stream().anyMatch(ReviewItem::hasCorrectAnswer);
        if (!hasAnswers) {
            reviewNote.setText(session.policy().showAnswer()
                    ? "Đáp án sẽ hiện sau khi ca thi kết thúc."
                    : "Ca thi này không cho xem đáp án.");
            return;
        }
        reviewNote.setText("Đáp án từng câu:");
        for (ReviewItem it : items) {
            reviewList.getChildren().add(reviewCard(it));
        }
    }

    private VBox reviewCard(ReviewItem it) {
        Label head = new Label("Câu " + it.order() + "  ·  " + Formats.score(it.earnedPoint() == null ? 0 : it.earnedPoint())
                + "/" + Formats.score(it.point()) + " điểm");
        head.getStyleClass().add("card-title");
        Label content = new Label(it.content());
        content.setWrapText(true);
        Label yours = new Label("Bạn trả lời: " + (it.responseText().isEmpty() ? "(bỏ trống)" : it.responseText()));
        yours.setWrapText(true);
        yours.getStyleClass().add(it.isFullMark() ? "answer-right" : "answer-wrong");
        Label correct = new Label("Đáp án đúng: " + it.correctText());
        correct.setWrapText(true);
        VBox card = new VBox(4, head, content, yours, correct);
        card.getStyleClass().add("review-card");
        return card;
    }

    @FXML
    private void onRetry() {
        startPolling();
    }

    @FXML
    private void onBack() {
        if (poller != null) {
            poller.stop();
        }
        shell.showHome(user);
    }
}
