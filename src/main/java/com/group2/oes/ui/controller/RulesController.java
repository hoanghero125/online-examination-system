package com.group2.oes.ui.controller;

import com.group2.oes.app.ClientContext;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.client.SessionEntry;
import com.group2.oes.client.SessionInfo;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.User;
import com.group2.oes.ui.AppShell;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.Formats;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

/** Xác nhận quy chế trước khi vào thi (FR-27). Vào lại bài đang làm dở thì báo số lần đã vi phạm. */
public class RulesController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label subjectLabel;
    @FXML
    private Label questionsLabel;
    @FXML
    private Label closeLabel;
    @FXML
    private Label remainingLabel;
    @FXML
    private Label rulesLabel;
    @FXML
    private Label resumeLabel;
    @FXML
    private CheckBox agreeBox;
    @FXML
    private Button startButton;
    @FXML
    private VBox form;

    private AppShell shell;
    private ClientContext client;
    private User user;
    private SessionInfo session;

    /** Kết quả vào thi: bài làm và giờ máy chủ lúc vào, để đồng hồ đếm ngược bắt đầu đúng. */
    private record Entered(ExamAttempt attempt, LocalDateTime serverNow) {
    }

    public void init(AppShell shell, ClientContext client, User user, SessionEntry entry) {
        this.shell = shell;
        this.client = client;
        this.user = user;
        this.session = entry.session();
        titleLabel.setText(session.examTitle());
        subjectLabel.setText(session.subjectCode() + " – " + session.subjectName());
        questionsLabel.setText(session.questionCount() + " câu, tổng " + Formats.score(session.totalPoint()) + " điểm");
        closeLabel.setText(Formats.dateTime(session.window().closeTime()));
        rulesLabel.setText(client.proctoring.getPolicy().rulesText()
                + " Bài làm được lưu sau mỗi câu trả lời; mất điện hay mất mạng thì đăng nhập lại để làm tiếp.");
        if (entry.isResuming()) {
            resumeLabel.setText("Tiếp tục bài đang làm (đã vi phạm " + entry.attempt().getViolationCount() + " lần).");
            startButton.setText("Tiếp tục làm bài");
        } else {
            resumeLabel.setVisible(false);
            resumeLabel.setManaged(false);
        }
        startButton.disableProperty().bind(agreeBox.selectedProperty().not());
        BackgroundTasks.run(client.exams::serverTime,
                now -> remainingLabel.setText(SessionWindow.formatRemaining(session.window().remainingSeconds(now))),
                error -> remainingLabel.setText("không lấy được giờ máy chủ"));
    }

    @FXML
    private void onStart() {
        int sid = session.sessionId();
        BackgroundTasks.run(form, () -> new Entered(client.exams.enter(user, sid), client.exams.serverTime()),
                e -> shell.navigator().show("exam.fxml",
                        (ExamController c) -> c.init(shell, client, user, e.attempt(), e.serverNow())));
    }

    @FXML
    private void onBack() {
        shell.showHome(user);
    }
}
