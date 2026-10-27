package com.group2.oes.ui;

import com.group2.oes.app.ClientContext;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.model.User;
import com.group2.oes.ui.controller.SessionListController;

/**
 * Ứng dụng thi, cài trên máy thí sinh. Dùng tài khoản MySQL oes_client: chỉ đọc được đề không có đáp án,
 * không chấm bài (NFR-04).
 */
public class ExamClientApp extends OesApplication<ClientContext> {

    @Override
    public String title() {
        return "Thi";
    }

    @Override
    protected String configFileName() {
        return AppConfig.CLIENT_FILE;
    }

    @Override
    protected String defaultDbUser() {
        return "oes_client";
    }

    @Override
    protected ClientContext connectTo(ConnectionManager cm) {
        ClientContext client = ClientContext.jdbc(cm);
        client.exams.serverTime();
        return client;
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
        navigator().show("session-list.fxml", (SessionListController c) -> c.init(this, context, user));
    }
}
