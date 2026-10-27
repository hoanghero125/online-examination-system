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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.group2.oes.dao.jdbc.JdbcErrors.ldt;

public class JdbcSubmissionDao implements SubmissionDao {

    static final String SELECT_SUB =
            "SELECT sb.id AS sb_id, sb.started_at, sb.submitted_at, sb.status, sb.end_reason,"
                    + " sb.total_score, sb.violation_count, " + JdbcUserDao.USER_COLUMNS
                    + " FROM submission sb JOIN app_user u ON u.id = sb.student_id";

    private final ConnectionManager cm;

    public JdbcSubmissionDao(ConnectionManager cm) {
        this.cm = cm;
    }

    @Override
    public List<Submission> findAllOfSession(ExamSession session) {
        return cm.execute(c -> query(c, session,
                SELECT_SUB + " WHERE sb.session_id = ? ORDER BY u.student_code", session.getSessionId()));
    }

    /**
     * Dựng bài làm (kèm câu trả lời) từ kết quả của SELECT_SUB.
     * Dùng chung với JdbcGradingDao, vì máy chấm cũng cần bài làm đầy đủ như màn hình thống kê.
     */
    static List<Submission> query(Connection c, ExamSession session, String sql, int... params)
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
