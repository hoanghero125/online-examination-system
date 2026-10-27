package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.model.Answer;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Student;
import com.group2.oes.model.Submission;
import com.group2.oes.model.SubmissionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.group2.oes.dao.jdbc.JdbcErrors.ldt;
import static com.group2.oes.dao.jdbc.JdbcErrors.ts;

public class JdbcSubmissionDao implements SubmissionDao {

    private static final String SELECT_SUB =
            "SELECT sb.id AS sb_id, sb.started_at, sb.submitted_at, sb.status, sb.end_reason,"
                    + " sb.total_score, sb.violation_count, " + JdbcUserDao.USER_COLUMNS
                    + " FROM submission sb JOIN app_user u ON u.id = sb.student_id";

    private final ConnectionManager cm;

    public JdbcSubmissionDao(ConnectionManager cm) {
        this.cm = cm;
    }

    /** Ràng buộc UNIQUE(session_id, student_id) sẽ sinh DuplicateKeyException nếu đã có bài. */
    @Override
    public Submission insert(Submission s) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO submission(session_id, student_id, started_at, status) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, s.getSession().getSessionId());
                ps.setInt(2, s.getStudent().getUserId());
                ps.setTimestamp(3, ts(s.getStartedAt()));
                ps.setString(4, s.getStatus().name());
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    s.assignId(k.getInt(1));
                }
            }
            return s;
        });
    }

    @Override
    public Optional<Submission> findBySessionAndStudent(ExamSession session, Student student) {
        List<Submission> list = cm.execute(c -> query(c, session,
                SELECT_SUB + " WHERE sb.session_id = ? AND sb.student_id = ?",
                session.getSessionId(), student.getUserId()));
        return list.stream().findFirst();
    }

    @Override
    public List<Submission> findAllOfSession(ExamSession session) {
        return cm.execute(c -> query(c, session,
                SELECT_SUB + " WHERE sb.session_id = ? ORDER BY u.student_code", session.getSessionId()));
    }

    @Override
    public List<Submission> findInProgressOfSession(ExamSession session) {
        return cm.execute(c -> query(c, session,
                SELECT_SUB + " WHERE sb.session_id = ? AND sb.status = 'IN_PROGRESS'", session.getSessionId()));
    }

    /** Ghi đè câu trả lời nếu đã có (thí sinh đổi ý), thêm mới nếu chưa có. */
    @Override
    public void saveAnswer(int submissionId, int itemOrder, String response) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO answer(submission_id, item_order, response) VALUES (?,?,?)"
                            + " ON DUPLICATE KEY UPDATE response = ?")) {
                ps.setInt(1, submissionId);
                ps.setInt(2, itemOrder);
                ps.setString(3, response);
                ps.setString(4, response);
                return ps.executeUpdate();
            }
        });
    }

    /**
     * Tăng bộ đếm ngay trong câu UPDATE thay vì đọc lên, cộng một, rồi ghi xuống.
     * Cách đọc-sửa-ghi sẽ mất lượt đếm khi hai sự kiện tới gần như cùng lúc.
     * LAST_INSERT_ID(expr) là mẹo của MySQL để lấy lại giá trị vừa ghi mà không cần truy vấn thêm.
     */
    @Override
    public int increaseViolation(int submissionId) {
        return cm.execute(c -> {
            int updated;
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE submission SET violation_count = LAST_INSERT_ID(violation_count + 1)"
                            + " WHERE id = ? AND status = 'IN_PROGRESS'")) {
                ps.setInt(1, submissionId);
                updated = ps.executeUpdate();
            }
            String sql = updated == 1
                    ? "SELECT LAST_INSERT_ID()"
                    : "SELECT violation_count FROM submission WHERE id = " + submissionId;
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    /**
     * Ghi kết quả nộp bài trong một giao dịch.
     * Câu UPDATE có điều kiện status = 'IN_PROGRESS': nếu máy khác đã nộp trước,
     * câu lệnh không sửa dòng nào và toàn bộ giao dịch dừng lại, kết quả lần đầu được giữ nguyên.
     */
    @Override
    public boolean saveResult(Submission s) {
        return cm.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE submission SET status=?, submitted_at=?, end_reason=?, total_score=?, violation_count=?"
                            + " WHERE id=? AND status='IN_PROGRESS'")) {
                ps.setString(1, s.getStatus().name());
                ps.setTimestamp(2, ts(s.getSubmittedAt()));
                ps.setString(3, s.getEndReason() == null ? null : s.getEndReason().name());
                ps.setDouble(4, s.getTotalScore());
                ps.setInt(5, s.getViolationCount());
                ps.setInt(6, s.getSubmissionId());
                if (ps.executeUpdate() == 0) {
                    return false;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO answer(submission_id, item_order, response, earned_point) VALUES (?,?,?,?)"
                            + " ON DUPLICATE KEY UPDATE response = ?, earned_point = ?")) {
                for (Answer a : s.getAnswers()) {
                    ps.setInt(1, s.getSubmissionId());
                    ps.setInt(2, a.getExamItem().getOrder());
                    ps.setString(3, a.getResponse());
                    ps.setDouble(4, a.getEarnedPoint());
                    ps.setString(5, a.getResponse());
                    ps.setDouble(6, a.getEarnedPoint());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return true;
        });
    }

    private static List<Submission> query(Connection c, ExamSession session, String sql, int... params)
            throws SQLException {
        Map<Integer, Submission> subs = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setInt(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student st = (Student) JdbcUserDao.mapUser(rs);
                    String reason = rs.getString("end_reason");
                    Submission s = new Submission(rs.getInt("sb_id"), st, session,
                            ldt(rs.getTimestamp("started_at")), ldt(rs.getTimestamp("submitted_at")),
                            SubmissionStatus.valueOf(rs.getString("status")),
                            reason == null ? null : EndReason.valueOf(reason),
                            rs.getDouble("total_score"), rs.getInt("violation_count"));
                    subs.put(s.getSubmissionId(), s);
                }
            }
        }
        if (!subs.isEmpty()) {
            loadAnswers(c, subs);
        }
        return new ArrayList<>(subs.values());
    }

    private static void loadAnswers(Connection c, Map<Integer, Submission> subs) throws SQLException {
        String in = String.join(",", subs.keySet().stream().map(k -> "?").toList());
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT submission_id, item_order, response, earned_point FROM answer"
                        + " WHERE submission_id IN (" + in + ")")) {
            int i = 1;
            for (int id : subs.keySet()) {
                ps.setInt(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Submission s = subs.get(rs.getInt("submission_id"));
                    Answer a = s.restoreAnswer(rs.getInt("item_order"), rs.getString("response"));
                    double p = rs.getDouble("earned_point");
                    if (!rs.wasNull() && s.getStatus() == SubmissionStatus.GRADED) {
                        a.restoreResult(p);
                    }
                }
            }
        }
    }
}
