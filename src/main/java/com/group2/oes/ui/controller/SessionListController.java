package com.group2.oes.ui.controller;

import com.group2.oes.app.ClientContext;
import com.group2.oes.client.SessionEntry;
import com.group2.oes.client.SessionInfo;
import com.group2.oes.model.SessionPhase;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.ui.AppShell;
import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.ErrorDialogs;
import com.group2.oes.ui.Formats;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Trang chủ ứng dụng thi: ca thi chia ba nhóm sắp diễn ra / đang mở / đã kết thúc (FR-20).
 * Từ đây tới lúc làm bài chỉ cần hai thao tác nữa: "Vào thi" rồi "Bắt đầu làm bài" (NFR-07).
 */
public class SessionListController {

    @FXML
    private Label userLabel;
    @FXML
    private VBox openBox;
    @FXML
    private VBox upcomingBox;
    @FXML
    private VBox closedBox;
    @FXML
    private VBox page;

    private AppShell shell;
    private ClientContext client;
    private User user;

    public void init(AppShell shell, ClientContext client, User user) {
        this.shell = shell;
        this.client = client;
        this.user = user;
        String code = user instanceof Student s ? " (" + s.getStudentCode() + ")" : "";
        userLabel.setText(user.getFullName() + code);
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        BackgroundTasks.run(page, () -> client.exams.overview(user), this::render);
    }

    private void render(List<SessionEntry> entries) {
        fill(openBox, entries, SessionPhase.OPEN, "Không có ca thi nào đang mở.");
        fill(upcomingBox, entries, SessionPhase.UPCOMING, "Không có ca thi sắp diễn ra.");
        fill(closedBox, entries, SessionPhase.CLOSED, "Chưa có ca thi nào đã kết thúc.");
    }

    private void fill(VBox box, List<SessionEntry> entries, SessionPhase phase, String emptyText) {
        box.getChildren().clear();
        entries.stream().filter(e -> e.phase() == phase).forEach(e -> box.getChildren().add(card(e)));
        if (box.getChildren().isEmpty()) {
            Label empty = new Label(emptyText);
            empty.getStyleClass().add("muted");
            box.getChildren().add(empty);
        }
    }

    private HBox card(SessionEntry e) {
        SessionInfo s = e.session();
        Label title = new Label(s.subjectCode() + " – " + s.examTitle());
        title.getStyleClass().add("card-title");
        Label time = new Label("Mở " + Formats.dateTime(s.window().openTime())
                + "   ·   Đóng " + Formats.dateTime(s.window().closeTime()));
        Label info = new Label(s.questionCount() + " câu  ·  " + Formats.score(s.totalPoint()) + " điểm");
        info.getStyleClass().add("muted");
        VBox text = new VBox(4, title, time, info);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox card = new HBox(16, text, spacer);
        card.getStyleClass().add("session-card");

        if (e.canEnter()) {
            Button enter = new Button(e.isResuming() ? "Tiếp tục làm bài" : "Vào thi");
            enter.getStyleClass().add("primary");
            enter.setOnAction(ev -> shell.navigator().show("rules.fxml",
                    (RulesController c) -> c.init(shell, client, user, e)));
            card.getChildren().add(enter);
        } else if (e.canViewResult()) {
            Button result = new Button("Xem kết quả");
            result.setOnAction(ev -> shell.navigator().show("result.fxml",
                    (ResultController c) -> c.init(shell, client, user, s, e.attempt())));
            card.getChildren().add(result);
        } else if (e.phase() == SessionPhase.CLOSED) {
            Label absent = new Label("Không dự thi");
            absent.getStyleClass().add("muted");
            card.getChildren().add(absent);
        }
        return card;
    }

    @FXML
    private void onChangePassword() {
        shell.navigator().show("change-password.fxml", (ChangePasswordController c) -> c.init(shell, user, false));
    }

    @FXML
    private void onLogout() {
        if (ErrorDialogs.confirm("Đăng xuất", "Bạn có chắc muốn đăng xuất?", "Đăng xuất")) {
            shell.logout();
        }
    }
}
