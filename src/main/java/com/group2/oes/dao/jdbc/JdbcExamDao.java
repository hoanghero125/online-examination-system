package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.QuestionDao;
import com.group2.oes.exception.DataAccessException;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamItem;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.Question;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcExamDao implements ExamDao {

    private record ItemRow(int order, int questionId, double point) {
    }

    private final ConnectionManager cm;
    private final QuestionDao questionDao;

    public JdbcExamDao(ConnectionManager cm, QuestionDao questionDao) {
        this.cm = cm;
        this.questionDao = questionDao;
    }

    @Override
    public Exam insert(Exam exam) {
        return cm.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO exam(title, subject_id, version, multi_policy, locked) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, exam.getTitle());
                ps.setInt(2, exam.getSubject().getSubjectId());
                ps.setInt(3, exam.getVersion());
                ps.setString(4, exam.getMultiChoicePolicy().name());
                ps.setBoolean(5, exam.isLocked());
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    exam.assignId(k.getInt(1));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO exam_item(exam_id, item_order, question_id, point) VALUES (?,?,?,?)")) {
                for (ExamItem it : exam.getItems()) {
                    ps.setInt(1, exam.getExamId());
                    ps.setInt(2, it.getOrder());
                    ps.setInt(3, it.getQuestion().getQuestionId());
                    ps.setDouble(4, it.getPoint());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return exam;
        });
    }

    @Override
    public Optional<Exam> findById(int examId) {
        // Bước 1: đọc đề và danh sách mục đề trong cùng một kết nối
        record Loaded(Exam exam, List<ItemRow> items) {
        }
        Optional<Loaded> loaded = cm.execute(c -> {
            Exam exam;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT e.id, e.title, e.version, e.multi_policy, e.locked,"
                            + " s.id AS s_id, s.code, s.name FROM exam e JOIN subject s ON s.id = e.subject_id"
                            + " WHERE e.id = ?")) {
                ps.setInt(1, examId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    Subject subject = new Subject(rs.getInt("s_id"), rs.getString("code"), rs.getString("name"));
                    exam = new Exam(rs.getInt("id"), rs.getString("title"), subject, rs.getInt("version"),
                            MultiChoicePolicy.valueOf(rs.getString("multi_policy")), rs.getBoolean("locked"));
                }
            }
            List<ItemRow> items = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT item_order, question_id, point FROM exam_item WHERE exam_id = ? ORDER BY item_order")) {
                ps.setInt(1, examId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(new ItemRow(rs.getInt(1), rs.getInt(2), rs.getDouble(3)));
                    }
                }
            }
            return Optional.of(new Loaded(exam, items));
        });

        // Bước 2: nạp toàn bộ câu hỏi của đề bằng một lần gọi, rồi ghép lại
        return loaded.map(l -> {
            Map<Integer, Question> questions = questionDao.findByIds(
                    l.items().stream().map(ItemRow::questionId).toList());
            for (ItemRow r : l.items()) {
                Question q = questions.get(r.questionId());
                if (q == null) {
                    throw new DataAccessException("Đề " + examId + " tham chiếu câu hỏi không tồn tại: " + r.questionId(), null);
                }
                l.exam().restoreItem(new ExamItem(r.order(), r.point(), q));
            }
            return l.exam();
        });
    }

    @Override
    public List<Exam> findBySubject(Subject subject) {
        List<Integer> ids = cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM exam WHERE subject_id = ? ORDER BY title, version")) {
                ps.setInt(1, subject.getSubjectId());
                List<Integer> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(rs.getInt(1));
                    }
                }
                return list;
            }
        });
        List<Exam> exams = new ArrayList<>();
        for (int id : ids) {
            findById(id).ifPresent(exams::add);
        }
        return exams;
    }

    @Override
    public void lock(int examId) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE exam SET locked = TRUE WHERE id = ?")) {
                ps.setInt(1, examId);
                return ps.executeUpdate();
            }
        });
    }
}
