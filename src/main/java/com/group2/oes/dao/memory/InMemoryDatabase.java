package com.group2.oes.dao.memory;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.QuestionDao;
import com.group2.oes.dao.SubjectDao;
import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.dao.UserDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.Submission;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.User;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Bản cài đặt các DAO bằng Map trong bộ nhớ, không cần MySQL.
 * Dùng cho kiểm thử tự động và để chạy thử chương trình trên máy chưa cài CSDL.
 * Hành vi được giữ giống bản JDBC ở các điểm quan trọng: sinh mã tự tăng,
 * chặn trùng (ca thi, thí sinh), không ghi đè bài đã nộp.
 */
public class InMemoryDatabase {

    private final AtomicInteger seq = new AtomicInteger(100);
    private final Supplier<LocalDateTime> clock;

    private final Map<Integer, User> users = new LinkedHashMap<>();
    private final Map<String, Subject> subjects = new LinkedHashMap<>();
    private final Map<Integer, Set<Subject>> instructorSubjects = new HashMap<>();
    private final Map<Integer, Integer> studentClass = new HashMap<>();
    private final Map<Integer, Question> questions = new LinkedHashMap<>();
    private final Map<Integer, Exam> exams = new HashMap<>();
    private final Map<Integer, ExamSession> sessions = new LinkedHashMap<>();
    private final Map<Integer, Submission> submissions = new LinkedHashMap<>();
    // trạng thái "đã ghi xuống CSDL" của từng bài, tách khỏi đối tượng đang được service sửa
    private final Map<Integer, SubmissionStatus> savedStatus = new HashMap<>();
    private final Map<Integer, Integer> savedViolations = new HashMap<>();
    private final Map<Integer, Map<Integer, String>> savedAnswers = new HashMap<>();
    // mô phỏng ràng buộc UNIQUE(session_id, student_id) của bảng submission
    private final Map<String, Integer> uniqueIndex = new HashMap<>();

    public InMemoryDatabase(Supplier<LocalDateTime> clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    private int nextId() {
        return seq.incrementAndGet();
    }

    private static String key(int sessionId, int studentId) {
        return sessionId + ":" + studentId;
    }

    // ---------------------------------------------------------------- Users
    public UserDao userDao() {
        return new UserDao() {
            @Override
            public synchronized Optional<User> findByUsername(String username) {
                return users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
            }

            @Override
            public synchronized Optional<User> findById(int userId) {
                return Optional.ofNullable(users.get(userId));
            }

            @Override
            public synchronized <T extends User> T insert(T user) {
                if (findByUsername(user.getUsername()).isPresent()) {
                    throw new DuplicateKeyException("Tên đăng nhập đã tồn tại: " + user.getUsername(), null);
                }
                user.assignId(nextId());
                users.put(user.getUserId(), user);
                if (user instanceof Student s && s.getCourseClass() != null) {
                    studentClass.put(s.getUserId(), s.getCourseClass().getClassId());
                }
                return user;
            }

            @Override
            public synchronized void update(User user) {
                users.put(user.getUserId(), user);
            }

            @Override
            public synchronized List<Student> findStudentsOfClass(int classId) {
                return users.values().stream()
                        .filter(u -> u instanceof Student && Objects.equals(studentClass.get(u.getUserId()), classId))
                        .map(u -> (Student) u)
                        .sorted(Comparator.comparing(Student::getStudentCode))
                        .toList();
            }

            @Override
            public synchronized List<User> findAll() {
                return new ArrayList<>(users.values());
            }
        };
    }

    // ---------------------------------------------------------------- Subjects
    public SubjectDao subjectDao() {
        return new SubjectDao() {
            @Override
            public synchronized Subject insert(String code, String name) {
                if (subjects.containsKey(code)) {
                    throw new DuplicateKeyException("Mã môn đã tồn tại: " + code, null);
                }
                Subject s = new Subject(nextId(), code, name);
                subjects.put(code, s);
                return s;
            }

            @Override
            public synchronized Optional<Subject> findByCode(String code) {
                return Optional.ofNullable(subjects.get(code));
            }

            @Override
            public synchronized List<Subject> findAll() {
                return new ArrayList<>(subjects.values());
            }

            @Override
            public synchronized CourseClass insertClass(String code, Subject subject, String semester) {
                return new CourseClass(nextId(), code, subject, semester);
            }

            @Override
            public synchronized void assignInstructor(Instructor instructor, Subject subject) {
                instructorSubjects.computeIfAbsent(instructor.getUserId(), k -> new HashSet<>()).add(subject);
                instructor.assignSubject(subject);
            }

            @Override
            public synchronized void loadSubjectsOf(Instructor instructor) {
                instructorSubjects.getOrDefault(instructor.getUserId(), Set.of()).forEach(instructor::assignSubject);
            }
        };
    }

    // ---------------------------------------------------------------- Questions
    public QuestionDao questionDao() {
        return new QuestionDao() {
            @Override
            public synchronized Question insert(Question q) {
                q.assignId(nextId());
                for (AnswerOption o : q.getOptions()) {
                    o.assignId(nextId());
                }
                questions.put(q.getQuestionId(), q);
                return q;
            }

            @Override
            public synchronized Optional<Question> findById(int questionId) {
                return Optional.ofNullable(questions.get(questionId));
            }

            @Override
            public synchronized Map<Integer, Question> findByIds(Collection<Integer> ids) {
                Map<Integer, Question> m = new LinkedHashMap<>();
                for (int id : ids) {
                    Question q = questions.get(id);
                    if (q != null) {
                        m.put(id, q);
                    }
                }
                return m;
            }

            @Override
            public synchronized List<Question> findBySubject(Subject subject) {
                return questions.values().stream().filter(q -> q.getSubject().equals(subject)).toList();
            }

            @Override
            public synchronized void delete(int questionId) {
                questions.remove(questionId);
            }

            @Override
            public synchronized boolean isUsedInLockedExam(int questionId) {
                return exams.values().stream()
                        .filter(Exam::isLocked)
                        .flatMap(e -> e.getItems().stream())
                        .anyMatch(it -> it.getQuestion().getQuestionId() == questionId);
            }
        };
    }

    // ---------------------------------------------------------------- Exams
    public ExamDao examDao() {
        return new ExamDao() {
            @Override
            public synchronized Exam insert(Exam exam) {
                exam.assignId(nextId());
                exams.put(exam.getExamId(), exam);
                return exam;
            }

            @Override
            public synchronized Optional<Exam> findById(int examId) {
                return Optional.ofNullable(exams.get(examId));
            }

            @Override
            public synchronized void lock(int examId) {
                Exam e = exams.get(examId);
                if (e != null && !e.isLocked()) {
                    e.lock();
                }
            }
        };
    }

    // ---------------------------------------------------------------- Sessions
    public ExamSessionDao sessionDao() {
        return new ExamSessionDao() {
            @Override
            public synchronized ExamSession insert(ExamSession s) {
                s.assignId(nextId());
                sessions.put(s.getSessionId(), s);
                return s;
            }

            @Override
            public synchronized Optional<ExamSession> findById(int sessionId) {
                return Optional.ofNullable(sessions.get(sessionId));
            }

            @Override
            public synchronized List<ExamSession> findOfStudent(int studentId) {
                return sessions.values().stream()
                        .filter(s -> s.getEnrolledStudentIds().contains(studentId))
                        .sorted(Comparator.comparing(ExamSession::getOpenTime))
                        .toList();
            }

            @Override
            public synchronized List<ExamSession> findClosedWithOpenSubmissions(LocalDateTime now) {
                Set<Integer> withOpen = new HashSet<>();
                submissions.forEach((id, sub) -> {
                    if (savedStatus.get(id) == SubmissionStatus.IN_PROGRESS) {
                        withOpen.add(sub.getSession().getSessionId());
                    }
                });
                return sessions.values().stream()
                        .filter(s -> s.isClosed(now) && withOpen.contains(s.getSessionId()))
                        .toList();
            }

            @Override
            public LocalDateTime serverTime() {
                return clock.get();
            }
        };
    }

    // ---------------------------------------------------------------- Submissions
    public SubmissionDao submissionDao() {
        return new SubmissionDao() {

            @Override
            public synchronized Submission insert(Submission s) {
                String k = key(s.getSession().getSessionId(), s.getStudent().getUserId());
                if (uniqueIndex.containsKey(k)) {
                    throw new DuplicateKeyException("Thí sinh đã có bài trong ca thi này", null);
                }
                s.assignId(nextId());
                uniqueIndex.put(k, s.getSubmissionId());
                savedStatus.put(s.getSubmissionId(), SubmissionStatus.IN_PROGRESS);
                savedViolations.put(s.getSubmissionId(), 0);
                savedAnswers.put(s.getSubmissionId(), new LinkedHashMap<>());
                submissions.put(s.getSubmissionId(), s);
                return s;
            }

            @Override
            public synchronized Optional<Submission> findBySessionAndStudent(ExamSession session, Student student) {
                Integer id = uniqueIndex.get(key(session.getSessionId(), student.getUserId()));
                return id == null ? Optional.empty() : Optional.of(reload(id));
            }

            @Override
            public synchronized void saveAnswer(int submissionId, int itemOrder, String response) {
                savedAnswers.get(submissionId).put(itemOrder, response);
            }

            @Override
            public synchronized int increaseViolation(int submissionId) {
                if (savedStatus.get(submissionId) == SubmissionStatus.IN_PROGRESS) {
                    savedViolations.merge(submissionId, 1, Integer::sum);
                }
                return savedViolations.get(submissionId);
            }

            @Override
            public synchronized boolean saveResult(Submission s) {
                if (savedStatus.get(s.getSubmissionId()) != SubmissionStatus.IN_PROGRESS) {
                    return false;
                }
                savedStatus.put(s.getSubmissionId(), s.getStatus());
                submissions.put(s.getSubmissionId(), s);
                return true;
            }

            @Override
            public synchronized List<Submission> findAllOfSession(ExamSession session) {
                return submissions.keySet().stream()
                        .filter(id -> submissions.get(id).getSession().getSessionId() == session.getSessionId())
                        .map(this::reload)
                        .toList();
            }

            @Override
            public synchronized List<Submission> findInProgressOfSession(ExamSession session) {
                return findAllOfSession(session).stream()
                        .filter(s -> s.getStatus() == SubmissionStatus.IN_PROGRESS)
                        .toList();
            }

            /** Trả về bản sao như vừa đọc từ CSDL, để service không vô tình sửa trực tiếp dữ liệu đã lưu. */
            private Submission reload(int id) {
                Submission stored = submissions.get(id);
                if (savedStatus.get(id) != SubmissionStatus.IN_PROGRESS) {
                    return stored;   // bài đã nộp thì không còn ai sửa, trả thẳng bản đã chấm
                }
                Submission copy = new Submission(id, stored.getStudent(), stored.getSession(), stored.getStartedAt(),
                        null, SubmissionStatus.IN_PROGRESS, null, 0, savedViolations.get(id));
                savedAnswers.get(id).forEach(copy::restoreAnswer);
                return copy;
            }
        };
    }
}
