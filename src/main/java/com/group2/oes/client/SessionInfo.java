package com.group2.oes.client;

import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;

import java.util.Objects;

/**
 * Thông tin một ca thi mà máy thí sinh được phép biết: không có đề, không có đáp án.
 * Đọc từ khung nhìn v_session_for_student.
 */
public record SessionInfo(int sessionId, int examId, String examTitle, String subjectCode, String subjectName,
                          SessionWindow window, SessionPolicy policy, double totalPoint, int questionCount) {

    public SessionInfo {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(policy, "policy");
    }
}
