package com.group2.oes.dao;

import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Student;
import com.group2.oes.model.Submission;

import java.util.List;
import java.util.Optional;

public interface SubmissionDao {

    /**
     * Tạo bài làm mới.
     * @throws com.group2.oes.exception.DuplicateKeyException nếu thí sinh đã có bài trong ca này
     */
    Submission insert(Submission submission);

    Optional<Submission> findBySessionAndStudent(ExamSession session, Student student);

    void saveAnswer(int submissionId, int itemOrder, String response);

    /**
     * Tăng số lần vi phạm một cách nguyên tử ngay trong CSDL và trả về giá trị mới.
     * Bài đã nộp thì không tăng, trả về giá trị hiện có.
     */
    int increaseViolation(int submissionId);

    /**
     * Ghi trạng thái nộp bài và điểm của từng câu trong một giao dịch.
     * @return false nếu bài đã được nộp từ trước (máy khác nộp trước), không ghi gì cả
     */
    boolean saveResult(Submission submission);

    List<Submission> findAllOfSession(ExamSession session);

    List<Submission> findInProgressOfSession(ExamSession session);
}
