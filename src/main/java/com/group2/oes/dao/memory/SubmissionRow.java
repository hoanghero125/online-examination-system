package com.group2.oes.dao.memory;

import com.group2.oes.model.EndReason;
import com.group2.oes.model.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * Một dòng của bảng submission cùng các dòng answer của nó, trong bản CSDL bộ nhớ.
 * Lưu dạng dòng (không lưu thẳng đối tượng Submission) để phía thí sinh và phía giảng viên
 * mỗi bên dựng đối tượng của riêng mình, giống như khi đọc từ MySQL.
 */
final class SubmissionRow {

    final int id;
    final int sessionId;
    final int studentId;
    final LocalDateTime startedAt;
    LocalDateTime submittedAt;
    SubmissionStatus status = SubmissionStatus.IN_PROGRESS;
    EndReason endReason;
    double totalScore;
    int violationCount;
    final Map<Integer, AnswerRow> answers = new TreeMap<>();

    SubmissionRow(int id, int sessionId, int studentId, LocalDateTime startedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.startedAt = startedAt;
    }

    AnswerRow answer(int order) {
        return answers.computeIfAbsent(order, k -> new AnswerRow());
    }

    /** Một dòng của bảng answer. */
    static final class AnswerRow {
        String response;
        boolean flagged;
        Double earnedPoint;
    }
}
