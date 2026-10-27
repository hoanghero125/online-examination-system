package com.group2.oes.grading;

import com.group2.oes.model.Answer;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.Submission;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chấm một bài làm đã nộp (FR-31).
 *
 * Tách ra khỏi Submission (SRP): Submission chỉ giữ dữ liệu, còn việc chấm cần đề thi có đáp án,
 * nên chỉ được chạy trên máy giảng viên (NFR-04). Lớp này không truy cập CSDL, chỉ tính toán.
 *
 * Mỗi câu chấm bằng chiến lược mà đề chọn cho loại câu đó (mẫu Strategy, xem Exam.strategyFor).
 */
public class SubmissionGrader {

    private static final Logger LOG = Logger.getLogger(SubmissionGrader.class.getName());

    /**
     * Chấm mọi câu trong đề, kể cả câu thí sinh bỏ trống (được 0 điểm), rồi chuyển bài sang GRADED.
     * Một câu chấm lỗi không được làm hỏng cả bài: bắt ngoại lệ, đánh dấu câu đó, chấm tiếp.
     *
     * @return tổng điểm, làm tròn hai chữ số
     */
    public double grade(Submission submission) {
        Exam exam = submission.getSession().getExam();
        double total = 0;
        for (ExamItem item : exam.getItems()) {
            Answer answer = submission.answerFor(item);
            try {
                answer.grade(exam.strategyFor(item.getQuestion()));
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Lỗi chấm câu " + item.getOrder() + " của bài " + submission.getSubmissionId(), e);
                answer.markGradingError();
            }
            total += answer.getEarnedPoint();
        }
        double rounded = Math.round(total * 100.0) / 100.0;
        submission.markGraded(rounded);
        return rounded;
    }
}
