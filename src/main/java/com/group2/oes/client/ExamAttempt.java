package com.group2.oes.client;

import java.util.Objects;

/**
 * Mọi thứ màn hình làm bài cần sau khi thí sinh vào thi: thông tin ca, đề đã trộn riêng cho thí sinh, trạng thái bài.
 */
public record ExamAttempt(SessionInfo session, ExamPaper paper, AttemptState state) {

    public ExamAttempt {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(paper, "paper");
        Objects.requireNonNull(state, "state");
    }

    public int submissionId() {
        return state.getSubmissionId();
    }

    public int unansweredCount() {
        return paper.size() - state.answeredCount();
    }
}
