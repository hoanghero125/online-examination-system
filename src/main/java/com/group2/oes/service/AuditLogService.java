package com.group2.oes.service;

import com.group2.oes.model.Role;
import com.group2.oes.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Xem nhật ký thao tác (NFR-05) trên màn hình Nhật ký của quản trị viên.
 * Nhật ký do java.util.logging ghi ra tệp (cấu hình trong logging.properties), mỗi máy một tệp.
 */
public class AuditLogService {

    private final Path logFile;

    public AuditLogService(Path logFile) {
        this.logFile = logFile;
    }

    /**
     * Tối đa {@code limit} dòng mới nhất có chứa từ khóa (không phân biệt hoa thường), dòng mới nhất đứng đầu.
     * Chưa có tệp nhật ký thì trả danh sách rỗng.
     */
    public List<String> recentLines(User actor, String keyword, int limit) throws IOException {
        Permissions.requireRole(actor, Role.ADMIN);
        if (!Files.exists(logFile)) {
            return List.of();
        }
        String k = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        List<String> all = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        List<String> result = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && result.size() < limit; i--) {
            String line = all.get(i);
            if (!line.isBlank() && (k.isEmpty() || line.toLowerCase(Locale.ROOT).contains(k))) {
                result.add(line);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Path getLogFile() {
        return logFile;
    }
}
