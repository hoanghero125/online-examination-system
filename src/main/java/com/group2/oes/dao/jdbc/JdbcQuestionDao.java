package com.group2.oes.dao.jdbc;

import com.group2.oes.dao.QuestionDao;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.factory.QuestionFactory;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.model.question.TrueFalseQuestion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JdbcQuestionDao implements QuestionDao {

    private static final String SELECT_QUESTION =
            "SELECT q.id, q.type, q.content, q.chapter, q.difficulty, q.default_point,"
                    + " q.correct_value, q.case_sensitive, s.id AS s_id, s.code AS s_code, s.name AS s_name"
                    + " FROM question q JOIN subject s ON s.id = q.subject_id";

    private final ConnectionManager cm;

    public JdbcQuestionDao(ConnectionManager cm) {
        this.cm = cm;
    }

    /** Câu hỏi và phương án phải được ghi cùng nhau, nên dùng giao dịch. */
    @Override
    public Question insert(Question q) {
        return cm.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO question(subject_id, type, content, chapter, difficulty, default_point,"
                            + " correct_value, case_sensitive) VALUES (?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, q.getSubject().getSubjectId());
                ps.setString(2, q.getType().name());
                ps.setString(3, q.getContent());
                ps.setInt(4, q.getChapter());
                ps.setString(5, q.getDifficulty().name());
                ps.setDouble(6, q.getDefaultPoint());
                if (q instanceof TrueFalseQuestion tf) {
                    ps.setBoolean(7, tf.getCorrectValue());
                } else {
                    ps.setNull(7, Types.BOOLEAN);
                }
                if (q instanceof FillInBlankQuestion fb) {
                    ps.setBoolean(8, fb.isCaseSensitive());
                } else {
                    ps.setNull(8, Types.BOOLEAN);
                }
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    q.assignId(k.getInt(1));
                }
            }
            insertOptions(c, q);
            return q;
        });
    }

    private static void insertOptions(Connection c, Question q) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO answer_option(question_id, opt_order, content, is_correct) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            if (q instanceof FillInBlankQuestion fb) {
                // đáp án chấp nhận của câu điền khuyết cũng lưu vào answer_option, đánh dấu đúng
                int order = 1;
                for (String a : fb.getAccepted()) {
                    ps.setInt(1, q.getQuestionId());
                    ps.setInt(2, order++);
                    ps.setString(3, a);
                    ps.setBoolean(4, true);
                    ps.addBatch();
                }
                ps.executeBatch();
                return;
            }
            // từng phương án cần lấy lại mã sinh tự động, nên chèn lần lượt thay vì chạy batch
            int order = 1;
            for (AnswerOption o : q.getOptions()) {
                ps.setInt(1, q.getQuestionId());
                ps.setInt(2, order++);
                ps.setString(3, o.getContent());
                ps.setBoolean(4, o.isCorrect());
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    o.assignId(k.getInt(1));
                }
            }
        }
    }

    @Override
    public Optional<Question> findById(int questionId) {
        Map<Integer, Question> m = findByIds(List.of(questionId));
        return Optional.ofNullable(m.get(questionId));
    }

    @Override
    public Map<Integer, Question> findByIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        String in = ids.stream().map(i -> "?").collect(Collectors.joining(","));
        return cm.execute(c -> load(c, SELECT_QUESTION + " WHERE q.id IN (" + in + ")", new ArrayList<>(ids)));
    }

    @Override
    public List<Question> findBySubject(Subject subject) {
        return cm.execute(c -> new ArrayList<>(load(c,
                SELECT_QUESTION + " WHERE q.subject_id = ? ORDER BY q.id",
                List.of(subject.getSubjectId())).values()));
    }

    /**
     * Đọc câu hỏi bằng hai truy vấn: một cho bảng question, một cho toàn bộ phương án liên quan.
     * Tránh kiểu "mỗi câu hỏi một truy vấn phương án", với 200 câu sẽ thành 201 lượt gọi CSDL.
     */
    private Map<Integer, Question> load(Connection c, String sql, List<Integer> params) throws SQLException {
        List<QuestionRow> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setInt(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(QuestionRow.read(rs));
                }
            }
        }
        if (rows.isEmpty()) {
            return Map.of();
        }

        Map<Integer, List<AnswerOption>> options = loadOptions(c, rows);

        Map<Integer, Question> result = new LinkedHashMap<>();
        for (QuestionRow r : rows) {
            List<AnswerOption> opts = options.getOrDefault(r.id, List.of());
            QuestionData data = QuestionData.basic(r.subject, r.content, r.chapter, r.difficulty, r.point)
                    .withId(r.id);
            data = switch (r.type) {
                case SINGLE_CHOICE, MULTI_CHOICE -> data.withOptions(opts);
                case TRUE_FALSE -> data.withCorrectValue(r.correctValue);
                case FILL_IN_BLANK -> data.withAccepted(
                        opts.stream().map(AnswerOption::getContent).toList(), r.caseSensitive);
            };
            result.put(r.id, QuestionFactory.create(r.type, data));
        }
        return result;
    }

    private static Map<Integer, List<AnswerOption>> loadOptions(Connection c, List<QuestionRow> rows)
            throws SQLException {
        String in = rows.stream().map(r -> "?").collect(Collectors.joining(","));
        Map<Integer, List<AnswerOption>> byQuestion = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, question_id, content, is_correct FROM answer_option"
                        + " WHERE question_id IN (" + in + ") ORDER BY question_id, opt_order")) {
            for (int i = 0; i < rows.size(); i++) {
                ps.setInt(i + 1, rows.get(i).id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byQuestion.computeIfAbsent(rs.getInt("question_id"), k -> new ArrayList<>())
                            .add(new AnswerOption(rs.getInt("id"), rs.getString("content"), rs.getBoolean("is_correct")));
                }
            }
        }
        return byQuestion;
    }

    @Override
    public void delete(int questionId) {
        cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM question WHERE id = ?")) {
                ps.setInt(1, questionId);
                return ps.executeUpdate();
            }
        });
    }

    @Override
    public boolean isUsedInLockedExam(int questionId) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM exam_item ei JOIN exam e ON e.id = ei.exam_id"
                            + " WHERE ei.question_id = ? AND e.locked = TRUE LIMIT 1")) {
                ps.setInt(1, questionId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** Một dòng của bảng question, đọc ra trước khi biết sẽ dựng lớp con nào. */
    private record QuestionRow(int id, QuestionType type, Subject subject, String content, int chapter,
                               Difficulty difficulty, double point, boolean correctValue, boolean caseSensitive) {

        static QuestionRow read(ResultSet rs) throws SQLException {
            return new QuestionRow(
                    rs.getInt("id"),
                    QuestionType.valueOf(rs.getString("type")),
                    new Subject(rs.getInt("s_id"), rs.getString("s_code"), rs.getString("s_name")),
                    rs.getString("content"),
                    rs.getInt("chapter"),
                    Difficulty.valueOf(rs.getString("difficulty")),
                    rs.getDouble("default_point"),
                    rs.getBoolean("correct_value"),     // NULL đọc ra false, chỉ dùng khi type = TRUE_FALSE
                    rs.getBoolean("case_sensitive"));
        }
    }
}
