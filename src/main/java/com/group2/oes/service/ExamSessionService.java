package com.group2.oes.service;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ExamSessionService {

    /** Ba nhóm ca thi hiển thị cho thí sinh (FR-20). */
    public enum Phase {
        UPCOMING, OPEN, CLOSED
    }

    private final ExamSessionDao sessionDao;
    private final ExamDao examDao;

    public ExamSessionService(ExamSessionDao sessionDao, ExamDao examDao) {
        this.sessionDao = sessionDao;
        this.examDao = examDao;
    }

    public ExamSession createSession(User actor, Exam exam, LocalDateTime open, LocalDateTime close,
                                     boolean shuffleQuestions, boolean shuffleOptions, boolean showAnswer,
                                     Collection<Student> students) {
        Permissions.requireManage(actor, exam.getSubject());
        ValidationException.require(!students.isEmpty(), "NO_STUDENTS", "Ca thi chưa có thí sinh nào.");
        ValidationException.require(close.isAfter(sessionDao.serverTime()), "SESSION_IN_PAST",
                "Giờ đóng ca thi đã ở trong quá khứ.");

        ExamSession s = new ExamSession(0, exam, open, close, shuffleQuestions, shuffleOptions, showAnswer);
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

    /** Ca thi của thí sinh, chia theo trạng thái. EnumMap giữ thứ tự khai báo của enum khi duyệt. */
    public Map<Phase, List<ExamSession>> sessionsOf(Student student) {
        LocalDateTime now = sessionDao.serverTime();
        Map<Phase, List<ExamSession>> result = new EnumMap<>(Phase.class);
        for (Phase p : Phase.values()) {
            result.put(p, new ArrayList<>());
        }
        for (ExamSession s : sessionDao.findOfStudent(student.getUserId())) {
            Phase p = now.isBefore(s.getOpenTime()) ? Phase.UPCOMING
                    : s.isClosed(now) ? Phase.CLOSED : Phase.OPEN;
            result.get(p).add(s);
        }
        return result;
    }

    public ExamSession get(int sessionId) {
        return sessionDao.findById(sessionId)
                .orElseThrow(() -> new ValidationException("SESSION_NOT_FOUND", "Không tìm thấy ca thi " + sessionId + "."));
    }
}
