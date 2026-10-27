package com.group2.oes.ui;

import com.group2.oes.app.AppContext;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.model.Administrator;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.util.PasswordHasher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Chạy thử giao diện không cần người bấm (chỉ dùng khi phát triển, không phải kiểm thử JUnit).
 * Chạy trong Xvfb, tự gõ, tự bấm, chụp màn hình bằng lệnh "import" của ImageMagick.
 *
 * <pre>
 *   xvfb-run java -cp ... com.group2.oes.ui.UiSmoke staff|client|deaddb &lt;config.properties&gt; &lt;thư-mục-ảnh&gt; [staff-config]
 * </pre>
 */
public final class UiSmoke {

    private static Path shots;

    private UiSmoke() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path config = Path.of(args[1]);
        shots = Files.createDirectories(Path.of(args[2]));
        if (!mode.equals("deaddb")) {
            seed(args.length > 3 ? Path.of(args[3]) : config);
        }
        Class<? extends Application> app = mode.equals("client") ? ExamClientApp.class : StaffApp.class;
        Thread fx = new Thread(() -> Application.launch(app, "--config=" + config));
        fx.setDaemon(true);
        fx.start();
        awaitToolkit();
        try {
            switch (mode) {
                case "staff" -> staff();
                case "client" -> client();
                default -> deadDb();
            }
            System.out.println("UI SMOKE OK: " + mode);
        } catch (Throwable t) {
            shot(mode + "-failure");
            System.out.println("UI SMOKE FAILED: " + mode + " " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }
        Platform.exit();
        System.exit(0);
    }

    /** Ba tài khoản cố định cho lần chạy thử; chạy lại thì đặt lại mật khẩu. */
    private static void seed(Path staffConfig) {
        AppContext app = AppContext.jdbc(ConnectionManager.fromFile(staffConfig));
        ensure(app, new Administrator(0, "ui_admin", PasswordHasher.hash("admin123"), "Quản Trị Viên Thử", null, true), "admin123", false);
        Student sv = new Student(0, "ui_sv", PasswordHasher.hash("ui_sv123"), "Nguyễn Thị Thử", null, true, "UISV001");
        ensure(app, sv, "ui_sv123", true);
    }

    private static void ensure(AppContext app, User fresh, String password, boolean mustChange) {
        User u = app.users.findByUsername(fresh.getUsername()).orElseGet(() -> app.users.insert(fresh));
        u.changePassword(password);
        u.setActive(true);
        u.setMustChangePassword(mustChange);
        app.users.update(u);
    }

    // ------------------------------------------------------------ kịch bản

    private static void staff() throws Exception {
        waitFor("#usernameField");
        shot("staff-1-login");
        login("ui_admin", "admin123");
        waitFor("#userLabel");
        Thread.sleep(500);
        shot("staff-2-home-admin");
        System.out.println("  trang chủ: " + text("#userLabel") + " | " + text("#gradingLabel"));
        clickButton("Đăng xuất");
        waitForDialog();
        shot("staff-3-confirm-logout");
        clickDialogButton("Đăng xuất");
        waitFor("#usernameField");

        login("ui_sv", "ui_sv123");
        waitForDialog();
        System.out.println("  thí sinh đăng nhập ứng dụng quản lý: " + dialogText());
        shot("staff-4-student-rejected");
        clickDialogButton("OK");
    }

    private static void client() throws Exception {
        waitFor("#usernameField");
        login("ui_sv", "ui_sv123");
        waitFor("#confirmField");
        shot("client-1-forced-change");
        type("#oldField", "ui_sv123");
        type("#newField", "matkhau1");
        type("#confirmField", "khac123");
        clickButton("Lưu mật khẩu");
        waitForDialog();
        System.out.println("  nhập lại không khớp: " + dialogText());
        clickDialogButton("OK");
        type("#confirmField", "matkhau1");
        clickButton("Lưu mật khẩu");
        waitForDialog();
        clickDialogButton("OK");
        waitFor("#userLabel");
        Thread.sleep(300);
        shot("client-2-home");
        System.out.println("  trang chủ: " + text("#userLabel"));
        clickButton("Đăng xuất");
        waitForDialog();
        clickDialogButton("Đăng xuất");
        waitFor("#usernameField");
        login("ui_admin", "admin123");
        waitForDialog();
        System.out.println("  quản trị viên đăng nhập ứng dụng thi: " + dialogText());
        shot("client-3-admin-rejected");
        clickDialogButton("OK");
    }

    private static void deadDb() throws Exception {
        waitForDialog();
        System.out.println("  mất kết nối: " + dialogText());
        shot("deaddb-1-error");
        clickDialogButton("OK");
        waitFor("#hostField");
        shot("deaddb-2-server-setup");
    }

    // ------------------------------------------------------------ thao tác

    /** Application.launch chạy trên luồng khác; chờ tới khi JavaFX nhận lệnh runLater. */
    private static void awaitToolkit() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            try {
                Platform.runLater(() -> { });
                return;
            } catch (IllegalStateException notStartedYet) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("JavaFX không khởi động được");
    }

    private static void login(String user, String password) throws Exception {
        type("#usernameField", user);
        type("#passwordField", password);
        clickButton("Đăng nhập");
    }

    private static Stage mainStage() {
        return (Stage) Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s.getScene() != null && !(s.getScene().getRoot() instanceof DialogPane))
                .findFirst().orElse(null);
    }

    private static <T> T onFx(Supplier<T> work) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(work.get());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("luồng giao diện không trả lời");
        }
        if (error.get() != null) {
            throw new IllegalStateException(error.get());
        }
        return result.get();
    }

    private static Optional<Node> find(String selector) throws Exception {
        return onFx(() -> {
            Stage s = mainStage();
            return s == null ? Optional.<Node>empty() : Optional.ofNullable(s.getScene().getRoot().lookup(selector));
        });
    }

    private static void waitFor(String selector) throws Exception {
        for (int i = 0; i < 200; i++) {
            if (find(selector).isPresent()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("không thấy " + selector);
    }

    private static void type(String selector, String text) throws Exception {
        onFx(() -> {
            ((TextInputControl) mainStage().getScene().getRoot().lookup(selector)).setText(text);
            return null;
        });
    }

    private static String text(String selector) throws Exception {
        return onFx(() -> ((Labeled) mainStage().getScene().getRoot().lookup(selector)).getText());
    }

    private static void clickButton(String label) throws Exception {
        Platform.runLater(() -> buttonIn(mainStage().getScene().getRoot(), label).fire());
    }

    private static Button buttonIn(Parent root, String label) {
        return root.lookupAll(".button").stream()
                .filter(n -> n instanceof Button b && label.equals(b.getText()))
                .map(n -> (Button) n)
                .findFirst().orElseThrow(() -> new IllegalStateException("không có nút " + label));
    }

    private static DialogPane dialog() throws Exception {
        return onFx(() -> Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s.getScene() != null && s.getScene().getRoot() instanceof DialogPane)
                .map(w -> (DialogPane) ((Stage) w).getScene().getRoot())
                .findFirst().orElse(null));
    }

    private static void waitForDialog() throws Exception {
        for (int i = 0; i < 200; i++) {
            if (dialog() != null) {
                Thread.sleep(300);
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("không thấy hộp thoại");
    }

    private static String dialogText() throws Exception {
        DialogPane d = dialog();
        return onFx(() -> ((Labeled) d.getContent()).getText());
    }

    private static void clickDialogButton(String label) throws Exception {
        DialogPane d = dialog();
        Platform.runLater(() -> buttonIn(d, label).fire());
        Thread.sleep(400);
    }

    private static void shot(String name) {
        try {
            Thread.sleep(400);
            new ProcessBuilder("import", "-window", "root", shots.resolve(name + ".png").toString())
                    .inheritIO().start().waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("không chụp được màn hình: " + e);
        }
    }
}
