package com.group2.oes.ui;

import javafx.application.Application;

/** Điểm chạy của ứng dụng thi khi đóng gói thành jar (xem StaffLauncher). */
public final class ClientLauncher {

    private ClientLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(ExamClientApp.class, args);
    }
}
