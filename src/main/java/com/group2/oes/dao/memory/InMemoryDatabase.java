package com.group2.oes.dao.memory;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.GradingDao;
import com.group2.oes.dao.QuestionDao;
import com.group2.oes.dao.StudentExamDao;
import com.group2.oes.dao.StudentLoginDao;
import com.group2.oes.dao.SubjectDao;
import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.dao.UserDao;
import com.group2.oes.model.Answer;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.Submission;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.User;
import com.group2.oes.model.question.Question;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Bản cài đặt các DAO bằng Map trong bộ nhớ, không cần MySQL.
 * Dùng cho kiểm thử tự động và để chạy thử chương trình trên máy chưa cài CSDL.
 *
 * Hành vi được giữ giống bản JDBC ở các điểm quan trọng, kể cả những gì CSDL thật làm bằng ràng buộc và trigger:
 * sinh mã tự tăng, chặn trùng (ca thi, thí sinh), không ghi vào bài đã khóa hoặc ca đã đóng,
 * không cho máy thí sinh thấy đáp án trước khi ca đóng.
 *
 * Mọi DAO dùng chung đối tượng này làm khóa đồng bộ, giống như cùng nối tới một CSDL.
 */
public class InMemoryDatabase {

    private final AtomicInteger seq = new AtomicInteger(100);
    private final Supplier<LocalDateTime> clock;

    // Các "bảng". Package-private để các lớp DAO cùng gói đọc ghi trực tiếp.
    final Map<Integer, User> users = new LinkedHashMap<>();
    final Map<String, Subject> subjects = new LinkedHashMap<>();
    final Map<Integer, CourseClass> classes = new LinkedHashMap<>();
    final Map<Integer, Set<Subject>> instructorSubjects = new HashMap<>();
    final Map<Integer, Integer> studentClass = new HashMap<>();
    final Map<Integer, Question> questions = new LinkedHashMap<>();
    final Map<Integer, Exam> exams = new LinkedHashMap<>();
    final Map<Integer, ExamSession> sessions = new LinkedHashMap<>();
    final Map<Integer, SubmissionRow> submissions = new LinkedHashMap<>();

    public InMemoryDatabase(Supplier<LocalDateTime> clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    int nextId() {
        return seq.incrementAndGet();
    }

    /** Giờ "máy chủ CSDL" của bản bộ nhớ. */
    LocalDateTime now() {
        return clock.get();
    }

    // ------------------------------------------------------------ phía giảng viên (tài khoản oes_staff)

    public UserDao userDao() {
        return new MemoryUserDao(this);
    }

    public SubjectDao subjectDao() {
        return new MemorySubjectDao(this);
    }

    public QuestionDao questionDao() {
        return new MemoryQuestionDao(this);
    }

    public ExamDao examDao() {
        return new MemoryExamDao(this);
    }

    public ExamSessionDao sessionDao() {
        return new MemorySessionDao(this);
    }

    public SubmissionDao submissionDao() {
        return new MemorySubmissionDao(this);
    }

    public GradingDao gradingDao() {
        return new MemoryGradingDao(this);
    }

    // ------------------------------------------------------------ phía thí sinh (tài khoản oes_client)

    public StudentExamDao studentExamDao() {
        return new MemoryStudentExamDao(this);
    }

    public StudentLoginDao studentLoginDao() {
        return new MemoryStudentLoginDao(this);
    }

    // ------------------------------------------------------------ tiện ích dùng chung cho các DAO

    Optional<SubmissionRow> findRow(int sessionId, int studentId) {
        return submissions.values().stream()
                .filter(r -> r.sessionId == sessionId && r.studentId == studentId)
                .findFirst();
    }

    /** Dựng bài làm phía giảng viên từ một dòng, giống JdbcSubmissionDao.query. */
    Submission toSubmission(SubmissionRow r) {
        Student student = (Student) users.get(r.studentId);
        ExamSession session = sessions.get(r.sessionId);
        Submission s = new Submission(r.id, student, session, r.startedAt, r.submittedAt, r.status,
                r.endReason, r.totalScore, r.violationCount);
        r.answers.forEach((order, a) -> {
            Answer answer = s.restoreAnswer(order, a.response);
            if (r.status == SubmissionStatus.GRADED && a.earnedPoint != null) {
                answer.restoreResult(a.earnedPoint);
            }
        });
        return s;
    }
}
