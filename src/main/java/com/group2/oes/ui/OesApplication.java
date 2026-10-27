package com.group2.oes.ui;

import com.group2.oes.app.LoggingSetup;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.ui.controller.LoadingController;
import com.group2.oes.ui.controller.ServerSetupController;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Phần khởi động chung của hai ứng dụng (mẫu Template Method).
 *
 * start() cố định các bước: tìm tệp cấu hình → kết nối CSDL trên luồng nền → màn hình đăng nhập;
 * không có tệp hoặc không kết nối được thì hiện màn hình nhập địa chỉ máy chủ.
 * Lớp con chỉ điền những bước khác nhau: tên tệp cấu hình, tài khoản CSDL mặc định,
 * dựng context (AppContext hay ClientContext), đăng nhập, trang chủ.
 *
 * @param <C> loại context mà ứng dụng dùng
 */
public abstract class OesApplication<C> extends Application implements AppShell {

    private Navigator navigator;
    private Path configFile;
    protected C context;

    /** db-staff.properties hoặc db-client.properties. */
    protected abstract String configFileName();

    /** Tài khoản CSDL điền sẵn ở màn hình cài đặt máy chủ: oes_staff hoặc oes_client. */
    protected abstract String defaultDbUser();

    /** Dựng context và thử một truy vấn để chắc chắn kết nối được. Chạy trên luồng nền. */
    protected abstract C connectTo(ConnectionManager cm);

    /** Gọi trên luồng giao diện ngay sau khi kết nối thành công. Mặc định không làm gì. */
    protected void onConnected() {
    }

    @Override
    public final void start(Stage stage) {
        // Ô chọn ngày (DatePicker) hiện tên tháng theo ngôn ngữ của máy; máy Windows tiếng Anh sẽ hiện "January".
        // Số liệu (điểm, CSV) đã định dạng bằng Locale.ROOT nên không bị ảnh hưởng.
        Locale.setDefault(Locale.forLanguageTag("vi-VN"));
        LoggingSetup.configure();
        navigator = new Navigator(stage);
        ErrorDialogs.setOwner(stage);
        // Lưới an toàn cuối cùng (NFR-08): ngoại lệ lọt khỏi một nút bấm cũng hiện thành hộp thoại và ghi nhật ký,
        // thay vì chỉ in ra console mà người dùng không thấy gì.
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> ErrorDialogs.show(error));
        stage.setTitle("Hệ thống thi trực tuyến – " + title());
        stage.setMinWidth(960);
        stage.setMinHeight(640);

        Optional<Path> found = AppConfig.locate(getParameters().getNamed(), configFileName());
        configFile = found.orElse(AppConfig.defaultLocation(configFileName()));
        if (found.isPresent()) {
            connect(() -> connectTo(ConnectionManager.fromFile(configFile)));
        } else {
            showServerSetup();
        }
        stage.show();
    }

    @Override
    public Navigator navigator() {
        return navigator;
    }

    @Override
    public void showServerSetup() {
        navigator.show("server-setup.fxml", (ServerSetupController c) -> c.init(title(), defaultDbUser(), configFile,
                settings -> connect(() -> {
                    C ctx = connectTo(new ConnectionManager(settings.jdbcUrl(), settings.user(), settings.password()));
                    // chỉ lưu sau khi đã kết nối thử thành công, để không ghi lại một cấu hình sai
                    save(settings);
                    return ctx;
                })));
    }

    /** Kết nối trên luồng nền. Lỗi thì báo và quay về màn hình cài đặt máy chủ. */
    private void connect(Supplier<C> openContext) {
        navigator.show("loading.fxml", (LoadingController c) -> c.init("Đang kết nối tới máy chủ CSDL…"));
        BackgroundTasks.run(openContext, ctx -> {
            context = ctx;
            onConnected();
            showLogin();
        }, error -> {
            ErrorDialogs.show(error);
            showServerSetup();
        });
    }

    private void save(AppConfig.ServerSettings settings) {
        try {
            AppConfig.save(configFile, settings);
        } catch (IOException e) {
            throw new ValidationException("CONFIG_WRITE", "Đã kết nối được nhưng không ghi được tệp cấu hình "
                    + configFile + " (" + e.getMessage() + "). Hãy chạy ứng dụng từ thư mục có quyền ghi.");
        }
    }

    @Override
    public void stop() {
        BackgroundTasks.shutdown();
    }
}
