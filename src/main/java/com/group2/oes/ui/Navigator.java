package com.group2.oes.ui;

import com.group2.oes.ui.controller.LoginController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
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

    /** Mở màn hình trong cửa sổ con chặn cửa sổ chính (biểu mẫu câu hỏi...). Controller tự đóng cửa sổ của nó. */
    public <C> Stage openDialog(String fxml, String title, Consumer<C> init) {
        Loaded<C> loaded = load(fxml);
        init.accept(loaded.controller());
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(title);
        Scene scene = new Scene(loaded.root());
        scene.getStylesheets().add(stylesheet());
        dialog.setScene(scene);
        dialog.show();
        return dialog;
    }

    public Stage stage() {
        return stage;
    }

    public static String stylesheet() {
        return Objects.requireNonNull(Navigator.class.getResource(CSS), CSS).toExternalForm();
    }

    private record Loaded<C>(Parent root, C controller) {
    }

    /** Bảng và danh sách rỗng mặc định hiện chữ tiếng Anh "No content in table"; đổi một chỗ cho mọi màn hình. */
    private static void vietnamesePlaceholders(Parent root) {
        for (Node n : root.lookupAll(".table-view")) {
            if (n instanceof TableView<?> table) {
                table.setPlaceholder(new Label("Chưa có dữ liệu."));
            }
        }
        for (Node n : root.lookupAll(".list-view")) {
            if (n instanceof ListView<?> list) {
                list.setPlaceholder(new Label("Chưa có dữ liệu."));
            }
        }
    }

    private <C> Loaded<C> load(String fxml) {
        URL url = Navigator.class.getResource("/fxml/" + fxml);
        if (url == null) {
            throw new IllegalStateException("Không có tệp giao diện /fxml/" + fxml);
        }
        FXMLLoader loader = new FXMLLoader(url);
        try {
            Parent root = loader.load();
            vietnamesePlaceholders(root);
            return new Loaded<>(root, loader.getController());
        } catch (IOException e) {
            // tệp FXML hỏng là lỗi lập trình, không phải lỗi người dùng
            throw new IllegalStateException("Không nạp được giao diện " + fxml, e);
        }
    }
}
