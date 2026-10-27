package com.group2.oes.util;

import com.group2.oes.model.User;

import java.util.logging.Logger;

/**
 * Ghi nhật ký các thao tác nhạy cảm (NFR-05): đăng nhập, tạo đề, mở ca thi, kết thúc bài do vi phạm.
 * Dùng java.util.logging có sẵn trong JDK; nơi ghi (tệp, console) cấu hình bằng logging.properties.
 */
public final class AuditLog {

    private static final Logger LOG = Logger.getLogger("oes.audit");

    private AuditLog() {
    }

    public static void record(User actor, String action, String target) {
        String who = actor == null ? "-" : actor.getUsername() + "/" + actor.getRole();
        LOG.info(() -> String.format("%s | %s | %s", who, action, target));
    }
}
