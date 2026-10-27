package com.group2.oes.dao;

import com.group2.oes.model.Submission;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thao tác CSDL của máy chấm (GradingWorker), chỉ có trên máy giảng viên / máy chủ.
 */
public interface GradingDao {

    /**
     * Ca nào đã quá giờ đóng: chuyển mọi bài IN_PROGRESS sang SUBMITTED với lý do TIME_OUT (FR-26, BR-04).
     * Làm bằng một câu UPDATE để không bỏ sót bài nào giữa hai lần đọc.
     * @return số bài vừa được nộp
     */
    int closeExpired(LocalDateTime now);

    /** Mã các bài đã nộp nhưng chưa chấm, bài nộp trước chấm trước. */
    List<Integer> findSubmittedIds();

    /** Nạp bài làm kèm đề có đáp án để chấm. */
    Submission load(int submissionId);

    /**
     * Ghi điểm từng câu và tổng điểm, chuyển bài sang GRADED, trong một giao dịch.
     * @return false nếu bài không còn ở trạng thái SUBMITTED (máy chấm khác đã chấm trước), không ghi gì cả
     */
    boolean saveGrades(Submission submission);
}
