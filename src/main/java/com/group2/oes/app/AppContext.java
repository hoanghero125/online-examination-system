package com.group2.oes.app;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.QuestionDao;
import com.group2.oes.dao.SubjectDao;
import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.dao.UserDao;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.dao.jdbc.JdbcExamDao;
import com.group2.oes.dao.jdbc.JdbcExamSessionDao;
import com.group2.oes.dao.jdbc.JdbcQuestionDao;
import com.group2.oes.dao.jdbc.JdbcSubjectDao;
import com.group2.oes.dao.jdbc.JdbcSubmissionDao;
import com.group2.oes.dao.jdbc.JdbcUserDao;
import com.group2.oes.dao.memory.InMemoryDatabase;
import com.group2.oes.service.AuthService;
import com.group2.oes.service.ExamService;
import com.group2.oes.service.ExamSessionService;
import com.group2.oes.service.ProctoringService;
import com.group2.oes.service.QuestionBankService;
import com.group2.oes.service.StatisticsService;
import com.group2.oes.service.SubmissionService;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Nơi lắp ráp toàn bộ đối tượng của ứng dụng.
 * Service chỉ biết interface DAO, nên đổi từ MySQL sang bộ nhớ (để kiểm thử) chỉ là đổi hàm khởi tạo ở đây.
 */
public final class AppContext {

    public final UserDao users;
    public final SubjectDao subjects;
    public final QuestionDao questions;
    public final ExamDao exams;
    public final ExamSessionDao sessions;
    public final SubmissionDao submissions;

    public final AuthService auth;
    public final QuestionBankService bankService;
    public final ExamService examService;
    public final ExamSessionService sessionService;
    public final SubmissionService submissionService;
    public final ProctoringService proctoring;
    public final StatisticsService statistics;

    private AppContext(UserDao users, SubjectDao subjects, QuestionDao questions, ExamDao exams,
                       ExamSessionDao sessions, SubmissionDao submissions) {
        this.users = users;
        this.subjects = subjects;
        this.questions = questions;
        this.exams = exams;
        this.sessions = sessions;
        this.submissions = submissions;

        this.auth = new AuthService(users, subjects);
        this.bankService = new QuestionBankService(questions);
        this.examService = new ExamService(exams);
        this.sessionService = new ExamSessionService(sessions, exams);
        this.submissionService = new SubmissionService(submissions, sessions);
        this.proctoring = new ProctoringService(submissions, submissionService);
        this.statistics = new StatisticsService(submissions);
    }

    public static AppContext inMemory(Supplier<LocalDateTime> clock) {
        InMemoryDatabase db = new InMemoryDatabase(clock);
        return new AppContext(db.userDao(), db.subjectDao(), db.questionDao(), db.examDao(),
                db.sessionDao(), db.submissionDao());
    }

    public static AppContext jdbc(ConnectionManager cm) {
        QuestionDao q = new JdbcQuestionDao(cm);
        ExamDao e = new JdbcExamDao(cm, q);
        return new AppContext(new JdbcUserDao(cm), new JdbcSubjectDao(cm), q, e,
                new JdbcExamSessionDao(cm, e), new JdbcSubmissionDao(cm));
    }
}
