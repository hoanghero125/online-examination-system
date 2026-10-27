package com.group2.oes.ui;

import com.group2.oes.exception.ExamAppException;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Window;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nơi duy nhất hiện thông báo lỗi cho người dùng (NFR-08).
 * - Lỗi của ứng dụng (ExamAppException): hiện đúng câu getUserMessage(), đã viết sẵn bằng tiếng Việt và nói rõ cách xử lý.
 * - Lỗi không lường trước: hiện câu chung, ghi stack trace vào nhật ký; người dùng không thấy chi tiết kỹ thuật.
 */
public final class ErrorDialogs {

    private static final Logger LOG = Logger.getLogger("oes.ui");

    private static Window owner;

    private ErrorDialogs() {
    }

    /** Cửa sổ chính, để hộp thoại hiện giữa cửa sổ và chặn thao tác phía sau. */
    public static void setOwner(Window window) {
        owner = window;
    }

    public static void show(Throwable error) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(error));
            return;
        }
        if (error instanceof ExamAppException e) {
            LOG.log(Level.INFO, "Lỗi nghiệp vụ " + e.getCode() + ": " + e.getMessage(), e.getCause());
            alert(Alert.AlertType.ERROR, "Không thực hiện được", e.getUserMessage()).showAndWait();
        } else {
            LOG.log(Level.SEVERE, "Lỗi không mong muốn", error);
            alert(Alert.AlertType.ERROR, "Lỗi", "Đã có lỗi không mong muốn. Hãy thử lại; nếu vẫn lỗi, "
                    + "báo cho giám thị hoặc quản trị viên.").showAndWait();
        }
    }

    public static void info(String title, String message) {
        alert(Alert.AlertType.INFORMATION, title, message).showAndWait();
    }

    /** Hộp thoại hỏi lại. @return true nếu người dùng bấm nút đồng ý */
    public static boolean confirm(String title, String message, String okText) {
        Alert a = alert(Alert.AlertType.CONFIRMATION, title, message);
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(ok, cancel);
        Optional<ButtonType> answer = a.showAndWait();
        return answer.isPresent() && answer.get() == ok;
    }

    public static Alert alert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        if (owner != null) {
            a.initOwner(owner);
        }
        a.setTitle(title);
        a.setHeaderText(null);
        // Label tự xuống dòng; nội dung mặc định của Alert hay bị cắt khi câu tiếng Việt dài
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(460);
        a.getDialogPane().setContent(content);
        a.getDialogPane().getStylesheets().add(Navigator.stylesheet());
        return a;
    }
}
