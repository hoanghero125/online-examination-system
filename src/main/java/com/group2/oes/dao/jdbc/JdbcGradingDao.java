package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.GradingDao;
import com.group2.oes.exception.DataAccessException;
import com.group2.oes.model.Answer;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Submission;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.group2.oes.dao.jdbc.JdbcErrors.ts;

/** Thao tác CSDL của máy chấm. Chạy bằng tài khoản oes_staff. */
public class JdbcGradingDao implements GradingDao {

    private final ConnectionManager cm;
    private final ExamSessionDao sessionDao;

    public JdbcGradingDao(ConnectionManager cm, ExamSessionDao sessionDao) {
        this.cm = cm;
        this.sessionDao = sessionDao;
    }

    /** Giờ nộp ghi bằng giờ đóng ca, không phải lúc máy chấm chạy, để bảng điểm phản ánh đúng BR-03. */
    @Override
    public int closeExpired(LocalDateTime now) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE submission sb JOIN exam_session es ON es.id = sb.session_id"
                            + " SET sb.status = 'SUBMITTED', sb.submitted_at = es.close_time, sb.end_reason = 'TIME_OUT'"
                            + " WHERE sb.status = 'IN_PROGRESS' AND es.close_time <= ?")) {
                ps.setTimestamp(1, ts(now));
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public List<Integer> findSubmittedIds() {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM submission WHERE status = 'SUBMITTED' ORDER BY submitted_at, id");
                 ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
                return ids;
            }
        });
    }

    @Override
    public Submission load(int submissionId) {
        int sessionId = cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT session_id FROM submission WHERE id = ?")) {
                ps.setInt(1, submissionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new DataAccessException("Không có bài làm " + submissionId, null);
                    }
                    return rs.getInt(1);
                }
            }
        });
        ExamSession session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new DataAccessException("Bài làm " + submissionId + " trỏ tới ca thi không tồn tại", null));
        List<Submission> found = cm.execute(c -> JdbcSubmissionDao.query(c, session,
                JdbcSubmissionDao.SELECT_SUB + " WHERE sb.id = ?", submissionId));
        return found.get(0);
    }

    /**
     * Câu UPDATE có điều kiện status = 'SUBMITTED': nếu máy chấm khác đã chấm trước,
     * câu lệnh không sửa dòng nào và giao dịch dừng lại, không ghi đè kết quả đã có.
     */
    @Override
    public boolean saveGrades(Submission s) {
        return cm.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE submission SET status = ?, total_score = ? WHERE id = ? AND status = 'SUBMITTED'")) {
                ps.setString(1, s.getStatus().name());
                ps.setDouble(2, s.getTotalScore());
                ps.setInt(3, s.getSubmissionId());
                if (ps.executeUpdate() == 0) {
                    return false;
                }
            }
            // Chỉ ghi earned_point; câu trả lời là dữ liệu của thí sinh nên không ghi đè.
            // Câu bỏ trống chưa có dòng nào thì thêm dòng mới với response NULL để thống kê đếm đủ số câu.
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO answer(submission_id, item_order, response, earned_point) VALUES (?,?,NULL,?)"
                            + " ON DUPLICATE KEY UPDATE earned_point = ?")) {
                for (ExamItem item : s.getSession().getExam().getItems()) {
                    Answer a = s.answerFor(item);
                    ps.setInt(1, s.getSubmissionId());
                    ps.setInt(2, item.getOrder());
                    ps.setDouble(3, a.getEarnedPoint());
                    ps.setDouble(4, a.getEarnedPoint());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return true;
        });
    }
}
