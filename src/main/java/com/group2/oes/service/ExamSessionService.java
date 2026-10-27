package com.group2.oes.service;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.util.Collection;

/** Lập lịch ca thi phía giảng viên (UC-10, UC-11). Danh sách ca của thí sinh nằm ở StudentExamService. */
public class ExamSessionService {

    private final ExamSessionDao sessionDao;
    private final ExamDao examDao;

    public ExamSessionService(ExamSessionDao sessionDao, ExamDao examDao) {
        this.sessionDao = sessionDao;
        this.examDao = examDao;
    }

    public ExamSession createSession(User actor, Exam exam, SessionWindow window, SessionPolicy policy,
                                     Collection<Student> students) {
        Permissions.requireManage(actor, exam.getSubject());
        ValidationException.require(!students.isEmpty(), "NO_STUDENTS", "Ca thi chưa có thí sinh nào.");
        ValidationException.require(window.closeTime().isAfter(sessionDao.serverTime()), "SESSION_IN_PAST",
                "Giờ đóng ca thi đã ở trong quá khứ.");

        ExamSession s = new ExamSession(0, exam, window, policy);
        s.enrollAll(students);
        ExamSession saved = sessionDao.insert(s);

        // BR-09: từ lúc có ca thi, đề không được sửa nữa
        if (!exam.isLocked()) {
            exam.lock();
            examDao.lock(exam.getExamId());
        }
        AuditLog.record(actor, "CREATE_SESSION", saved.getSessionId() + " đề " + exam.getExamId()
                + ", " + students.size() + " thí sinh");
        return saved;
    }

    public ExamSession get(int sessionId) {
        return sessionDao.findById(sessionId)
                .orElseThrow(() -> new ValidationException("SESSION_NOT_FOUND", "Không tìm thấy ca thi " + sessionId + "."));
    }
}
