package com.group2.oes.dao;

import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Submission;

import java.util.List;

/**
 * Đọc bài làm phía giảng viên, cho thống kê và bảng kết quả.
 * Các thao tác ghi đã chuyển sang StudentExamDao (máy thí sinh) và GradingDao (máy chấm).
 */
public interface SubmissionDao {

    /** Mọi bài làm của một ca thi, xếp theo mã sinh viên. */
    List<Submission> findAllOfSession(ExamSession session);
}
