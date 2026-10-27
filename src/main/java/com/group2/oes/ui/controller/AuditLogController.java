package com.group2.oes.ui.controller;

import com.group2.oes.ui.BackgroundTasks;
import com.group2.oes.ui.StaffWorkspace;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Nhật ký thao tác (NFR-05): 500 dòng mới nhất của tệp nhật ký trên máy này, lọc theo từ khóa. */
public class AuditLogController {

    private static final int MAX_LINES = 500;

    @FXML
    private TextField keywordField;
    @FXML
    private ListView<String> lines;
    @FXML
    private Label fileLabel;
    @FXML
    private VBox page;

    private StaffWorkspace ws;

    public void init(StaffWorkspace ws) {
        this.ws = ws;
        fileLabel.setText("Tệp: " + ws.app().auditLog.getLogFile().toAbsolutePath() + " (dòng mới nhất ở trên)");
        keywordField.setOnAction(e -> onRefresh());
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        String keyword = keywordField.getText();
        BackgroundTasks.run(page, () -> {
            try {
                return ws.app().auditLog.recentLines(ws.user(), keyword, MAX_LINES);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, list -> lines.setItems(FXCollections.observableArrayList(list)));
    }
}
