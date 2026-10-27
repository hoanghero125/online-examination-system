package com.group2.oes.ui;

import com.group2.oes.ui.controller.LoginController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Đổi màn hình trong một cửa sổ (Stage) duy nhất.
 *
 * Dữ liệu truyền giữa các màn hình đi qua tham số {@code init}: nơi mở màn hình nhận controller vừa tạo
 * và gọi hàm init(...) của nó với đúng service và dữ liệu cần thiết. Không dùng biến static toàn cục
 * nào ngoài UserSession, nên đọc chỗ mở màn hình là biết màn hình đó dùng gì.
 */
public class Navigator {

    private static final String CSS = "/css/app.css";

    private final Stage stage;

    public Navigator(Stage stage) {
        this.stage = Objects.requireNonNull(stage);
    }

    /** Thay toàn bộ nội dung cửa sổ bằng màn hình mới. */
    public <C> C show(String fxml, Consumer<C> init) {
        Loaded<C> loaded = load(fxml);
        init.accept(loaded.controller());
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(loaded.root(), 1100, 720);
            scene.getStylesheets().add(stylesheet());
            stage.setScene(scene);
        } else {
            scene.setRoot(loaded.root());   // giữ nguyên kích thước cửa sổ
        }
        return loaded.controller();
    }

    public void showLogin(AppShell shell) {
        show("login.fxml", (LoginController c) -> c.init(shell));
    }

    /** Nạp một màn hình con để đặt vào vùng nội dung của màn hình khác (ví dụ khung giữa của trang chủ). */
    public <C> Parent embed(String fxml, Consumer<C> init) {
        Loaded<C> loaded = load(fxml);
        init.accept(loaded.controller());
        return loaded.root();
    }

    public Stage stage() {
        return stage;
    }

    public static String stylesheet() {
        return Objects.requireNonNull(Navigator.class.getResource(CSS), CSS).toExternalForm();
    }

    private record Loaded<C>(Parent root, C controller) {
    }

    private <C> Loaded<C> load(String fxml) {
        URL url = Navigator.class.getResource("/fxml/" + fxml);
        if (url == null) {
            throw new IllegalStateException("Không có tệp giao diện /fxml/" + fxml);
        }
        FXMLLoader loader = new FXMLLoader(url);
        try {
            Parent root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            // tệp FXML hỏng là lỗi lập trình, không phải lỗi người dùng
            throw new IllegalStateException("Không nạp được giao diện " + fxml, e);
        }
    }
}
