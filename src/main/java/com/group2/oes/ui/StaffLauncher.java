package com.group2.oes.ui;

import javafx.application.Application;

/**
 * Điểm chạy của ứng dụng quản lý khi đóng gói thành jar.
 * Lớp main không được kế thừa Application, nếu không Java sẽ đòi JavaFX nằm trên module path
 * và jar chạy độc lập báo "JavaFX runtime components are missing".
 */
public final class StaffLauncher {

    private StaffLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(StaffApp.class, args);
    }
}
