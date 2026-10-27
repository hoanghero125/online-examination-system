package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.exception.DataAccessException;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.group2.oes.dao.jdbc.JdbcErrors.ldt;
import static com.group2.oes.dao.jdbc.JdbcErrors.ts;

public class JdbcExamSessionDao implements ExamSessionDao {

    private record SessionRow(int id, int examId, LocalDateTime open, LocalDateTime close,
                              boolean shuffleQ, boolean shuffleO, boolean showAnswer, List<Integer> students) {
    }

    private final ConnectionManager cm;
    private final ExamDao examDao;

    public JdbcExamSessionDao(ConnectionManager cm, ExamDao examDao) {
        this.cm = cm;
        this.examDao = examDao;
    }

    @Override
    public ExamSession insert(ExamSession s) {
        return cm.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO exam_session(exam_id, open_time, close_time, shuffle_questions, shuffle_options, show_answer)"
                            + " VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, s.getExam().getExamId());
                ps.setTimestamp(2, ts(s.getOpenTime()));
                ps.setTimestamp(3, ts(s.getCloseTime()));
                ps.setBoolean(4, s.isShuffleQuestions());
                ps.setBoolean(5, s.isShuffleOptions());
                ps.setBoolean(6, s.isShowAnswer());
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    s.assignId(k.getInt(1));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO session_student(session_id, student_id) VALUES (?, ?)")) {
                for (int studentId : s.getEnrolledStudentIds()) {
                    ps.setInt(1, s.getSessionId());
                    ps.setInt(2, studentId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return s;
        });
    }

    @Override
    public Optional<ExamSession> findById(int sessionId) {
        Optional<SessionRow> row = cm.execute(c -> {
            SessionRow r;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, exam_id, open_time, close_time, shuffle_questions, shuffle_options, show_answer"
                            + " FROM exam_session WHERE id = ?")) {
                ps.setInt(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    r = new SessionRow(rs.getInt(1), rs.getInt(2), ldt(rs.getTimestamp(3)), ldt(rs.getTimestamp(4)),
                            rs.getBoolean(5), rs.getBoolean(6), rs.getBoolean(7), new ArrayList<>());
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT student_id FROM session_student WHERE session_id = ?")) {
                ps.setInt(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        r.students().add(rs.getInt(1));
                    }
                }
            }
            return Optional.of(r);
        });

        return row.map(r -> {
            Exam exam = examDao.findById(r.examId())
                    .orElseThrow(() -> new DataAccessException("Ca thi " + sessionId + " trỏ tới đề không tồn tại", null));
            ExamSession s = new ExamSession(r.id(), exam, r.open(), r.close(), r.shuffleQ(), r.shuffleO(), r.showAnswer());
            r.students().forEach(s::enrollById);
            return s;
        });
    }

    @Override
    public List<ExamSession> findOfStudent(int studentId) {
        List<Integer> ids = cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT es.id FROM exam_session es JOIN session_student ss ON ss.session_id = es.id"
                            + " WHERE ss.student_id = ? ORDER BY es.open_time")) {
                ps.setInt(1, studentId);
                return readIds(ps);
            }
        });
        return loadAll(ids);
    }

    @Override
    public List<ExamSession> findClosedWithOpenSubmissions(LocalDateTime now) {
        List<Integer> ids = cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT DISTINCT es.id FROM exam_session es JOIN submission sb ON sb.session_id = es.id"
                            + " WHERE es.close_time <= ? AND sb.status = 'IN_PROGRESS'")) {
                ps.setTimestamp(1, ts(now));
                return readIds(ps);
            }
        });
        return loadAll(ids);
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

    private static List<Integer> readIds(PreparedStatement ps) throws java.sql.SQLException {
        List<Integer> ids = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private List<ExamSession> loadAll(List<Integer> ids) {
        List<ExamSession> list = new ArrayList<>();
        for (int id : ids) {
            findById(id).ifPresent(list::add);
        }
        return list;
    }
}
