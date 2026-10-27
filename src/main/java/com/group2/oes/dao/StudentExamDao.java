package com.group2.oes.dao;

import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamPaper;
import com.group2.oes.client.ReviewItem;
import com.group2.oes.client.SessionInfo;
import com.group2.oes.model.EndReason;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Mọi thao tác CSDL của máy thí sinh, gom trong một interface riêng (ISP).
 *
 * Cài đặt JDBC chạy bằng tài khoản MySQL oes_client và chỉ đọc qua khung nhìn không có đáp án (NFR-04),
 * nên không có hàm nào ở đây trả về đề có đáp án hay ghi được điểm.
 */
public interface StudentExamDao {

    Optional<SessionInfo> findSession(int sessionId);

    /** Mọi ca thi thí sinh có tên, kể cả ca chưa mở và ca đã đóng, xếp theo giờ mở. */
    List<SessionInfo> findSessionsOf(int studentId);

    boolean isEnrolled(int sessionId, int studentId);

    /** Đề theo thứ tự gốc, chưa trộn. Chỉ đọc được khi đề đã có ca thi tới giờ mở. */
    ExamPaper loadPaper(int examId);

    Optional<AttemptState> findAttempt(int sessionId, int studentId);

    /**
     * Tạo bài làm mới ở trạng thái IN_PROGRESS.
     * @throws com.group2.oes.exception.DuplicateKeyException nếu thí sinh đã có bài trong ca này
     */
    AttemptState createAttempt(int sessionId, int studentId, LocalDateTime now);

    /**
     * Ghi đè câu trả lời nếu đã có, thêm mới nếu chưa.
     * @throws com.group2.oes.exception.SessionRuleException bài đã nộp hoặc ca đã quá giờ đóng (BR-04)
     */
    void saveAnswer(int submissionId, int order, String response);

    /** Đánh dấu / bỏ đánh dấu câu cần xem lại (FR-22). Cùng điều kiện ghi như saveAnswer. */
    void setFlag(int submissionId, int order, boolean flagged);

    /**
     * Tăng số lần vi phạm một cách nguyên tử ngay trong CSDL và trả về giá trị mới.
     * Bài đã nộp thì không tăng, trả về giá trị hiện có.
     */
    int increaseViolation(int submissionId);

    /**
     * IN_PROGRESS -> SUBMITTED. Không chấm: việc chấm do máy giảng viên làm.
     * @return false nếu bài đã được nộp từ trước (máy khác nộp, hoặc máy chấm đã đóng ca)
     */
    boolean markSubmitted(int submissionId, EndReason reason, LocalDateTime now);

    /** Câu trả lời, điểm từng câu và đáp án đúng (đáp án chỉ có khi ca đã đóng và cho xem đáp án). */
    List<ReviewItem> loadReview(int submissionId);

    /** Giờ chuẩn của hệ thống, lấy từ máy chủ CSDL chứ không lấy từ máy trạm. */
    LocalDateTime serverTime();
}
