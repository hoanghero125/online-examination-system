package com.group2.oes.app;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.GradingDao;
import com.group2.oes.dao.QuestionDao;
import com.group2.oes.dao.SubjectDao;
import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.dao.UserDao;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.dao.jdbc.JdbcExamDao;
import com.group2.oes.dao.jdbc.JdbcExamSessionDao;
import com.group2.oes.dao.jdbc.JdbcGradingDao;
import com.group2.oes.dao.jdbc.JdbcQuestionDao;
import com.group2.oes.dao.jdbc.JdbcSubjectDao;
import com.group2.oes.dao.jdbc.JdbcSubmissionDao;
import com.group2.oes.dao.jdbc.JdbcUserDao;
import com.group2.oes.dao.memory.InMemoryDatabase;
import com.group2.oes.grading.SubmissionGrader;
import com.group2.oes.service.AuditLogService;
import com.group2.oes.service.AuthService;
import com.group2.oes.service.ExamService;
import com.group2.oes.service.ExamSessionService;
import com.group2.oes.service.GradingWorker;
import com.group2.oes.service.QuestionBankService;
import com.group2.oes.service.StatisticsService;
import com.group2.oes.service.SubjectService;
import com.group2.oes.service.UserImportService;

import java.nio.file.Path;

/**
 * Nơi lắp ráp đối tượng của ứng dụng phía giảng viên / quản trị viên (StaffApp, GradingDaemon).
 * Service chỉ biết interface DAO, nên đổi từ MySQL sang bộ nhớ (để kiểm thử) chỉ là đổi hàm khởi tạo ở đây.
 *
 * Phía thí sinh lắp ráp riêng trong ClientContext, với tài khoản CSDL riêng và DAO riêng (NFR-04).
 */
public final class AppContext {

    /** Tên tệp nhật ký, phải trùng java.util.logging.FileHandler.pattern trong logging.properties. */
    public static final String AUDIT_LOG_FILE = "oes-audit.log";

    public final UserDao users;
    public final SubjectDao subjects;
    public final QuestionDao questions;
    public final ExamDao exams;
    public final ExamSessionDao sessions;
    public final SubmissionDao submissions;
    public final GradingDao grading;

    public final AuthService auth;
    public final AuditLogService auditLog;
    public final UserImportService userImport;
    public final SubjectService subjectService;
    public final QuestionBankService bankService;
    public final ExamService examService;
    public final ExamSessionService sessionService;
    public final StatisticsService statistics;
    public final GradingWorker gradingWorker;

    private AppContext(UserDao users, SubjectDao subjects, QuestionDao questions, ExamDao exams,
                       ExamSessionDao sessions, SubmissionDao submissions, GradingDao grading) {
        this.users = users;
        this.subjects = subjects;
        this.questions = questions;
        this.exams = exams;
        this.sessions = sessions;
        this.submissions = submissions;
        this.grading = grading;

        this.auth = new AuthService(users, subjects);
        this.auditLog = new AuditLogService(Path.of(AUDIT_LOG_FILE));
        this.userImport = new UserImportService(users, subjects);
        this.subjectService = new SubjectService(subjects);
        this.bankService = new QuestionBankService(questions);
        this.examService = new ExamService(exams);
        this.sessionService = new ExamSessionService(sessions, exams, users);
        this.statistics = new StatisticsService(submissions);
        this.gradingWorker = new GradingWorker(grading, sessions, new SubmissionGrader());
    }

    public static AppContext inMemory(InMemoryDatabase db) {
        return new AppContext(db.userDao(), db.subjectDao(), db.questionDao(), db.examDao(),
                db.sessionDao(), db.submissionDao(), db.gradingDao());
    }

    /** Kết nối bằng tài khoản MySQL oes_staff. */
    public static AppContext jdbc(ConnectionManager cm) {
        QuestionDao q = new JdbcQuestionDao(cm);
        ExamDao e = new JdbcExamDao(cm, q);
        ExamSessionDao s = new JdbcExamSessionDao(cm, e);
        return new AppContext(new JdbcUserDao(cm), new JdbcSubjectDao(cm), q, e, s,
                new JdbcSubmissionDao(cm), new JdbcGradingDao(cm, s));
    }
}
