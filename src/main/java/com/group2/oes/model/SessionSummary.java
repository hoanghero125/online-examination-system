package com.group2.oes.model;

/**
 * Một dòng của danh sách ca thi phía giảng viên: trạng thái so với giờ máy chủ, số thí sinh, số bài đã nộp.
 * Không cập nhật theo thời gian thực (quyết định số 7): giảng viên bấm "Làm mới" để đọc lại.
 */
public record SessionSummary(ExamSession session, SessionPhase phase, int enrolled, int started, int submitted) {
}
