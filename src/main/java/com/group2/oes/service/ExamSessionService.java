package com.group2.oes.service;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.UserDao;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Role;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Lập lịch ca thi phía giảng viên (UC-10, UC-11). Danh sách ca của thí sinh nằm ở StudentExamService. */
public class ExamSessionService {

    private final ExamSessionDao sessionDao;
    private final ExamDao examDao;
    private final UserDao userDao;

    public ExamSessionService(ExamSessionDao sessionDao, ExamDao examDao, UserDao userDao) {
        this.sessionDao = sessionDao;
        this.examDao = examDao;
        this.userDao = userDao;
    }

    public ExamSession createSession(User actor, Exam exam, SessionWindow window, SessionPolicy policy,
                                     Collection<Student> students) {
        Permissions.requireManage(actor, exam.getSubject());
        ValidationException.require(!students.isEmpty(), "NO_STUDENTS", "Ca thi chưa có thí sinh nào. Hãy chọn ít nhất một thí sinh, hoặc chọn một lớp đã có thí sinh.");
        ValidationException.require(window.closeTime().isAfter(sessionDao.serverTime()), "SESSION_IN_PAST",
                "Giờ đóng ca thi đã ở trong quá khứ. Hãy chọn giờ đóng sau thời điểm hiện tại.");

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

    /** FR-19: ghi danh cả lớp học phần. Lớp phải thuộc môn của đề. */
    public ExamSession createSessionForClass(User actor, Exam exam, SessionWindow window, SessionPolicy policy,
                                             CourseClass courseClass) {
        ValidationException.require(courseClass.getSubject().equals(exam.getSubject()), "CLASS_WRONG_SUBJECT",
                "Lớp " + courseClass.getCode() + " không học môn " + exam.getSubject().getCode() + ".");
        return createSession(actor, exam, window, policy, studentsOfClass(actor, courseClass));
    }

    /** Danh sách thí sinh của một lớp, để màn hình mở ca thi đánh dấu sẵn hoặc cho chọn từng người. */
    public List<Student> studentsOfClass(User actor, CourseClass courseClass) {
        Permissions.requireManage(actor, courseClass.getSubject());
        return userDao.findStudentsOfClass(courseClass.getClassId());
    }

    /** Mọi thí sinh, cho trường hợp ghi danh từng người không theo lớp. */
    public List<Student> allStudents(User actor) {
        Permissions.requireStaff(actor);
        return userDao.search(null, Role.STUDENT).stream().map(u -> (Student) u).toList();
    }

    /** Các ca thi của một đề, cho màn hình danh sách ca thi. */
    public List<ExamSession> listOfExam(User actor, Exam exam) {
        Permissions.requireManage(actor, exam.getSubject());
        return sessionDao.findByExam(exam.getExamId());
    }

    /** Giờ máy chủ, để màn hình phân loại ca sắp diễn ra / đang mở / đã đóng. */
    public LocalDateTime serverTime() {
        return sessionDao.serverTime();
    }

    public ExamSession get(int sessionId) {
        return sessionDao.findById(sessionId)
                .orElseThrow(() -> new ValidationException("SESSION_NOT_FOUND", "Không tìm thấy ca thi " + sessionId + ". Hãy bấm Làm mới để tải lại danh sách."));
    }
}
