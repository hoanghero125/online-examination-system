package com.group2.oes.ui;

import com.group2.oes.app.AppContext;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.model.User;
import com.group2.oes.service.GradingScheduler;
import com.group2.oes.ui.controller.StaffHomeController;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Ứng dụng quản lý, cho quản trị viên và giảng viên. Dùng tài khoản MySQL oes_staff.
 * Khi chạy, ứng dụng này đồng thời là máy chấm: GradingScheduler chấm bài nộp 3 giây một lần (FR-26).
 */
public class StaffApp extends OesApplication<AppContext> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private GradingScheduler grading;
    private final StringProperty gradingStatus = new SimpleStringProperty("Máy chấm chưa chạy");

    @Override
    public String title() {
        return "Quản lý";
    }

    @Override
    protected String configFileName() {
        return AppConfig.STAFF_FILE;
    }

    @Override
    protected String defaultDbUser() {
        return "oes_staff";
    }

    @Override
    protected AppContext connectTo(ConnectionManager cm) {
        AppContext app = AppContext.jdbc(cm);
        app.sessions.serverTime();
        return app;
    }

    /** Máy chấm báo kết quả trên luồng riêng, nên phải đẩy về luồng giao diện mới được sửa nhãn trạng thái. */
    @Override
    protected void onConnected() {
        grading = new GradingScheduler(context.gradingWorker, GradingScheduler.DEFAULT_PERIOD,
                r -> Platform.runLater(() -> gradingStatus.set("Máy chấm: lúc " + LocalTime.now().format(TIME)
                        + " đã chấm " + r.graded() + " bài, nộp do hết giờ " + r.closed() + " bài")));
        grading.start();
        gradingStatus.set("Máy chấm đang chạy, " + GradingScheduler.DEFAULT_PERIOD.toSeconds() + " giây một lượt");
    }

    @Override
    public User login(String username, String password) {
        return context.auth.login(username, password);
    }

    @Override
    public void changePassword(User user, String oldPassword, String newPassword) {
        context.auth.changePassword(user, oldPassword, newPassword);
    }

    @Override
    public void showHome(User user) {
        navigator().show("staff-home.fxml", (StaffHomeController c) -> c.init(this, context, user, gradingStatus()));
    }

    public ReadOnlyStringProperty gradingStatus() {
        return gradingStatus;
    }

    @Override
    public void stop() {
        if (grading != null) {
            grading.close();
        }
        super.stop();
    }
}
