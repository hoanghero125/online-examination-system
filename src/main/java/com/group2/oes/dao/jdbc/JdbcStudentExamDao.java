package com.group2.oes.dao.jdbc;

import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamPaper;
import com.group2.oes.client.PaperItem;
import com.group2.oes.client.PaperOption;
import com.group2.oes.client.ReviewItem;
import com.group2.oes.client.SessionInfo;
import com.group2.oes.dao.StudentExamDao;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.question.QuestionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.group2.oes.dao.jdbc.JdbcErrors.ldt;
import static com.group2.oes.dao.jdbc.JdbcErrors.ts;

/**
 * Cài đặt StudentExamDao cho máy thí sinh, chạy bằng tài khoản MySQL oes_client.
 *
 * Chỉ dùng các khung nhìn v_session_for_student, v_exam_paper, v_review và hai bảng submission, answer
 * (với quyền theo cột). Mọi câu SELECT tới question / answer_option đều bị MySQL từ chối,
 * nên dù có sửa lớp này cũng không đọc được đáp án (NFR-04).
 */
public class JdbcStudentExamDao implements StudentExamDao {

    private static final String SELECT_SESSION =
            "SELECT v.session_id, v.exam_id, v.exam_title, v.subject_code, v.subject_name, v.open_time, v.close_time,"
                    + " v.shuffle_questions, v.shuffle_options, v.show_answer, v.total_point, v.question_count"
                    + " FROM v_session_for_student v";

    private final ConnectionManager cm;

    public JdbcStudentExamDao(ConnectionManager cm) {
        this.cm = cm;
    }

    @Override
    public Optional<SessionInfo> findSession(int sessionId) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_SESSION + " WHERE v.session_id = ?")) {
                ps.setInt(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapSession(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<SessionInfo> findSessionsOf(int studentId) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT_SESSION
                    + " JOIN session_student ss ON ss.session_id = v.session_id"
                    + " WHERE ss.student_id = ? ORDER BY v.open_time")) {
                ps.setInt(1, studentId);
                List<SessionInfo> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapSession(rs));
                    }
                }
                return list;
            }
        });
    }

    private static SessionInfo mapSession(ResultSet rs) throws SQLException {
        return new SessionInfo(rs.getInt("session_id"), rs.getInt("exam_id"), rs.getString("exam_title"),
                rs.getString("subject_code"), rs.getString("subject_name"),
                new SessionWindow(ldt(rs.getTimestamp("open_time")), ldt(rs.getTimestamp("close_time"))),
                new SessionPolicy(rs.getBoolean("shuffle_questions"), rs.getBoolean("shuffle_options"),
                        rs.getBoolean("show_answer")),
                rs.getDouble("total_point"), rs.getInt("question_count"));
    }

    @Override
    public boolean isEnrolled(int sessionId, int studentId) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM session_student WHERE session_id = ? AND student_id = ?")) {
                ps.setInt(1, sessionId);
                ps.setInt(2, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public ExamPaper loadPaper(int examId) {
        return cm.execute(c -> loadPaper(c, examId));
    }

    /** Khung nhìn trả mỗi phương án một dòng; gom các dòng cùng số câu thành một PaperItem. */
    private static ExamPaper loadPaper(Connection c, int examId) throws SQLException {
        record Head(double point, int questionId, QuestionType type, String content) {
        }
        Map<Integer, Head> heads = new LinkedHashMap<>();
        Map<Integer, List<PaperOption>> options = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT item_order, point, question_id, type, content, option_id, option_content"
                        + " FROM v_exam_paper WHERE exam_id = ? ORDER BY item_order, opt_order")) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int order = rs.getInt("item_order");
                    if (!heads.containsKey(order)) {   // dòng đầu tiên của một câu
                        heads.put(order, new Head(rs.getDouble("point"), rs.getInt("question_id"),
                                QuestionType.valueOf(rs.getString("type")), rs.getString("content")));
                        options.put(order, new ArrayList<>());
                    }
                    int optionId = rs.getInt("option_id");
                    if (!rs.wasNull()) {   // câu đúng/sai, điền khuyết có option_id NULL
                        options.get(order).add(new PaperOption(optionId, rs.getString("option_content")));
                    }
                }
            }
        }
        List<PaperItem> items = new ArrayList<>();
        heads.forEach((order, h) -> items.add(
                new PaperItem(order, h.point(), h.questionId(), h.type(), h.content(), options.get(order))));
        return new ExamPaper(items);
    }

    @Override
    public Optional<AttemptState> findAttempt(int sessionId, int studentId) {
        return cm.execute(c -> {
            AttemptState state;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, status, violation_count, end_reason, total_score FROM submission"
                            + " WHERE session_id = ? AND student_id = ?")) {
                ps.setInt(1, sessionId);
                ps.setInt(2, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    SubmissionStatus status = SubmissionStatus.valueOf(rs.getString("status"));
                    String reason = rs.getString("end_reason");
                    // total_score mặc định 0 trong CSDL; chỉ coi là điểm thật khi bài đã chấm
                    Double score = status == SubmissionStatus.GRADED ? rs.getDouble("total_score") : null;
                    state = new AttemptState(rs.getInt("id"), sessionId, studentId, status,
                            rs.getInt("violation_count"), reason == null ? null : EndReason.valueOf(reason), score);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT item_order, response, flagged FROM answer WHERE submission_id = ?")) {
                ps.setInt(1, state.getSubmissionId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        state.recordAnswer(rs.getInt("item_order"), rs.getString("response"));
                        state.setFlag(rs.getInt("item_order"), rs.getBoolean("flagged"));
                    }
                }
            }
            return Optional.of(state);
        });
    }

    /** Ràng buộc UNIQUE(session_id, student_id) sẽ sinh DuplicateKeyException nếu đã có bài. */
    @Override
    public AttemptState createAttempt(int sessionId, int studentId, LocalDateTime now) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO submission(session_id, student_id, started_at, status) VALUES (?,?,?,'IN_PROGRESS')",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, sessionId);
                ps.setInt(2, studentId);
                ps.setTimestamp(3, ts(now));
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    return AttemptState.started(k.getInt(1), sessionId, studentId);
                }
            }
        });
    }

    /** Ghi đè câu trả lời nếu đã có (thí sinh đổi ý), thêm mới nếu chưa có. */
    @Override
    public void saveAnswer(int submissionId, int order, String response) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO answer(submission_id, item_order, response) VALUES (?,?,?)"
                            + " ON DUPLICATE KEY UPDATE response = ?")) {
                ps.setInt(1, submissionId);
                ps.setInt(2, order);
                ps.setString(3, response);
                ps.setString(4, response);
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public void setFlag(int submissionId, int order, boolean flagged) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO answer(submission_id, item_order, flagged) VALUES (?,?,?)"
                            + " ON DUPLICATE KEY UPDATE flagged = ?")) {
                ps.setInt(1, submissionId);
                ps.setInt(2, order);
                ps.setBoolean(3, flagged);
                ps.setBoolean(4, flagged);
                return ps.executeUpdate();
            }
        });
    }

    /**
     * Tăng bộ đếm ngay trong câu UPDATE thay vì đọc lên, cộng một, rồi ghi xuống.
     * Cách đọc-sửa-ghi sẽ mất lượt đếm khi hai sự kiện tới gần như cùng lúc.
     * LAST_INSERT_ID(expr) là cách của MySQL để lấy lại giá trị vừa ghi mà không cần truy vấn thêm;
     * giá trị này gắn với từng kết nối nên luồng khác không đọc nhầm.
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
            if (updated == 1) {
                try (PreparedStatement ps = c.prepareStatement("SELECT LAST_INSERT_ID()");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT violation_count FROM submission WHERE id = ?")) {
                ps.setInt(1, submissionId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public boolean markSubmitted(int submissionId, EndReason reason, LocalDateTime now) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE submission SET status = 'SUBMITTED', submitted_at = ?, end_reason = ?"
                            + " WHERE id = ? AND status = 'IN_PROGRESS'")) {
                ps.setTimestamp(1, ts(now));
                ps.setString(2, reason.name());
                ps.setInt(3, submissionId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /**
     * Ghép ba nguồn: đề (v_exam_paper), câu trả lời và điểm (answer), đáp án đúng (v_review).
     * v_review chỉ có dòng khi ca đã đóng và cho xem đáp án, nên trước đó correctAnswer luôn là null.
     */
    @Override
    public List<ReviewItem> loadReview(int submissionId) {
        return cm.execute(c -> {
            int sessionId;
            int examId;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT sb.session_id, es.exam_id FROM submission sb"
                            + " JOIN exam_session es ON es.id = sb.session_id WHERE sb.id = ?")) {
                ps.setInt(1, submissionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return List.of();
                    }
                    sessionId = rs.getInt(1);
                    examId = rs.getInt(2);
                }
            }
            ExamPaper paper = loadPaper(c, examId);

            Map<Integer, String> responses = new TreeMap<>();
            Map<Integer, Double> points = new TreeMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT item_order, response, earned_point FROM answer WHERE submission_id = ?")) {
                ps.setInt(1, submissionId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int order = rs.getInt("item_order");
                        responses.put(order, rs.getString("response"));
                        double p = rs.getDouble("earned_point");
                        if (!rs.wasNull()) {
                            points.put(order, p);
                        }
                    }
                }
            }

            Map<Integer, List<String>> correct = new TreeMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT item_order, type, correct_value, correct_option_id, accepted_answer"
                            + " FROM v_review WHERE session_id = ? ORDER BY item_order")) {
                ps.setInt(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        List<String> parts = correct.computeIfAbsent(rs.getInt("item_order"), k -> new ArrayList<>());
                        String part = switch (QuestionType.valueOf(rs.getString("type"))) {
                            case SINGLE_CHOICE, MULTI_CHOICE -> rs.getString("correct_option_id");
                            case TRUE_FALSE -> String.valueOf(rs.getBoolean("correct_value"));
                            case FILL_IN_BLANK -> rs.getString("accepted_answer");
                        };
                        if (part != null && !parts.contains(part)) {
                            parts.add(part);
                        }
                    }
                }
            }

            List<ReviewItem> items = new ArrayList<>();
            for (PaperItem it : paper.getItems()) {
                List<String> parts = correct.get(it.order());
                String correctAnswer = parts == null ? null
                        : String.join(it.type() == QuestionType.FILL_IN_BLANK ? " / " : ",", parts);
                items.add(new ReviewItem(it.order(), it.point(), it.type(), it.content(), it.options(),
                        responses.get(it.order()), points.get(it.order()), correctAnswer));
            }
            return items;
        });
    }

    @Override
    public LocalDateTime serverTime() {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT NOW()");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return ldt(rs.getTimestamp(1));
            }
        });
    }
}
