package com.group2.oes.ui;

import com.group2.oes.app.AppContext;
import com.group2.oes.model.User;

import java.util.function.Consumer;

/**
 * Những gì một màn hình chức năng trong ứng dụng quản lý cần: service, người đang đăng nhập, và cách mở màn hình
 * khác vào vùng giữa của trang chủ. Trang chủ (StaffHomeController) cài đặt interface này.
 */
public interface StaffWorkspace {

    AppContext app();

    User user();

    Navigator navigator();

    /** Đặt màn hình {@code fxml} vào vùng giữa, gọi {@code init} để truyền dữ liệu cho controller của nó. */
    <C> void open(String fxml, Consumer<C> init);

    /** Chạy máy chấm ngay một lượt (nút "Làm mới" ở màn hình kết quả không phải chờ 3 giây). */
    void gradeNow();
}
