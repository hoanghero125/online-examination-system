package com.group2.oes.app;

import com.group2.oes.dao.StudentExamDao;
import com.group2.oes.dao.StudentLoginDao;
import com.group2.oes.dao.jdbc.ConnectionManager;
import com.group2.oes.dao.jdbc.JdbcStudentExamDao;
import com.group2.oes.dao.jdbc.JdbcStudentLoginDao;
import com.group2.oes.dao.memory.InMemoryDatabase;
import com.group2.oes.model.ViolationPolicy;
import com.group2.oes.service.ProctoringService;
import com.group2.oes.service.StudentAuthService;
import com.group2.oes.service.StudentExamService;

/**
 * Nơi lắp ráp đối tượng của ứng dụng thi (ExamClientApp) trên máy thí sinh.
 *
 * Chỉ có các DAO phía thí sinh: không có QuestionDao, ExamDao hay GradingDao, nên ứng dụng thi
 * không có đường nào để đọc đáp án, kể cả khi kết nối CSDL bằng một tài khoản nhiều quyền hơn (NFR-04).
 */
public final class ClientContext {

    public final StudentLoginDao logins;
    public final StudentExamDao studentExams;

    public final StudentAuthService auth;
    public final StudentExamService exams;
    public final ProctoringService proctoring;

    private ClientContext(StudentLoginDao logins, StudentExamDao studentExams) {
        this.logins = logins;
        this.studentExams = studentExams;

        this.auth = new StudentAuthService(logins);
        this.exams = new StudentExamService(studentExams);
        this.proctoring = new ProctoringService(studentExams, exams, ViolationPolicy.standard());
    }

    public static ClientContext inMemory(InMemoryDatabase db) {
        return new ClientContext(db.studentLoginDao(), db.studentExamDao());
    }

    /** Kết nối bằng tài khoản MySQL oes_client. */
    public static ClientContext jdbc(ConnectionManager cm) {
        return new ClientContext(new JdbcStudentLoginDao(cm), new JdbcStudentExamDao(cm));
    }
}
