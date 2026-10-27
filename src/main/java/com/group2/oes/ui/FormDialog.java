package com.group2.oes.ui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Hộp thoại nhập vài ô chữ (mã môn, tên môn...), dùng cho các thao tác đơn giản không cần cả một màn hình. */
public final class FormDialog {

    private FormDialog() {
    }

    /**
     * @param labels  nhãn từng ô
     * @param initial giá trị ban đầu từng ô (cùng số phần tử với labels)
     * @return giá trị đã nhập theo đúng thứ tự, hoặc rỗng nếu người dùng bấm Hủy
     */
    public static Optional<List<String>> ask(String title, List<String> labels, List<String> initial, String okText) {
        Dialog<ButtonType> d = new Dialog<>();
        d.initOwner(ErrorDialogs.owner());
        d.setTitle(title);
        d.getDialogPane().getStylesheets().add(Navigator.stylesheet());
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        List<TextField> fields = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            TextField f = new TextField(initial.get(i));
            f.setPrefColumnCount(26);
            fields.add(f);
            grid.addRow(i, new Label(labels.get(i)), f);
        }
        d.getDialogPane().setContent(grid);
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().setAll(ok, new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE));
        Optional<ButtonType> answer = d.showAndWait();
        if (answer.isEmpty() || answer.get() != ok) {
            return Optional.empty();
        }
        return Optional.of(fields.stream().map(TextField::getText).toList());
    }
}
