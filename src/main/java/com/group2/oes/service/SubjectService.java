package com.group2.oes.service;

import com.group2.oes.dao.SubjectDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Role;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Môn học, lớp học phần, phân công giảng viên (UC-05, FR-07).
 * Thêm, sửa, xóa do quản trị viên làm; giảng viên chỉ xem các môn mình phụ trách.
 */
public class SubjectService {

    private final SubjectDao subjectDao;

    public SubjectService(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
    }

    /** Quản trị viên thấy mọi môn; giảng viên thấy môn được phân công. */
    public List<Subject> listSubjects(User actor) {
        if (actor instanceof Instructor ins) {
            return new ArrayList<>(ins.getSubjects());
        }
        Permissions.requireRole(actor, Role.ADMIN);
        return subjectDao.findAll();
    }

    public Subject createSubject(User actor, String code, String name) {
        Permissions.requireRole(actor, Role.ADMIN);
        String c = requireCode(code, "mã môn");
        try {
            Subject s = subjectDao.insert(c, requireName(name, "Tên môn"));
            AuditLog.record(actor, "CREATE_SUBJECT", c);
            return s;
        } catch (DuplicateKeyException e) {
            throw new ValidationException("SUBJECT_CODE_TAKEN", "Mã môn " + c + " đã tồn tại.");
        }
    }

    /** Chỉ đổi tên: mã môn đã in trên đề và bảng điểm nên giữ nguyên. */
    public Subject renameSubject(User actor, Subject subject, String newName) {
        Permissions.requireRole(actor, Role.ADMIN);
        Subject renamed = new Subject(subject.getSubjectId(), subject.getCode(), requireName(newName, "Tên môn"));
        subjectDao.update(renamed);
        AuditLog.record(actor, "RENAME_SUBJECT", subject.getCode());
        return renamed;
    }

    /** Không xóa môn đã có câu hỏi, đề thi hoặc lớp học phần: dữ liệu thi cũ phải còn nguyên. */
    public void deleteSubject(User actor, Subject subject) {
        Permissions.requireRole(actor, Role.ADMIN);
        if (subjectDao.isInUse(subject.getSubjectId())) {
            throw new ValidationException("SUBJECT_IN_USE", "Môn " + subject.getCode()
                    + " đã có câu hỏi, đề thi hoặc lớp học phần nên không xóa được.");
        }
        subjectDao.delete(subject.getSubjectId());
        AuditLog.record(actor, "DELETE_SUBJECT", subject.getCode());
    }

    /** @param subject null thì lấy lớp của mọi môn (chỉ quản trị viên) */
    public List<CourseClass> listClasses(User actor, Subject subject) {
        if (subject == null) {
            Permissions.requireRole(actor, Role.ADMIN);
        } else {
            Permissions.requireManage(actor, subject);
        }
        return subjectDao.findClasses(subject);
    }

    public CourseClass createClass(User actor, String code, Subject subject, String semester) {
        Permissions.requireRole(actor, Role.ADMIN);
        String c = requireCode(code, "mã lớp");
        try {
            CourseClass cls = subjectDao.insertClass(c, subject, blankToNull(semester));
            AuditLog.record(actor, "CREATE_CLASS", c);
            return cls;
        } catch (DuplicateKeyException e) {
            throw new ValidationException("CLASS_CODE_TAKEN", "Mã lớp " + c + " đã tồn tại.");
        }
    }

    /** Lớp không đổi môn: thí sinh và ca thi đã gắn với lớp theo môn đó. */
    public CourseClass updateClass(User actor, CourseClass cls, String newCode, String newSemester) {
        Permissions.requireRole(actor, Role.ADMIN);
        CourseClass updated = new CourseClass(cls.getClassId(), requireCode(newCode, "mã lớp"), cls.getSubject(),
                blankToNull(newSemester));
        try {
            subjectDao.updateClass(updated);
        } catch (DuplicateKeyException e) {
            throw new ValidationException("CLASS_CODE_TAKEN", "Mã lớp " + updated.getCode() + " đã tồn tại.");
        }
        AuditLog.record(actor, "UPDATE_CLASS", updated.getCode());
        return updated;
    }

    public void assignInstructor(User actor, Instructor instructor, Subject subject) {
        Permissions.requireRole(actor, Role.ADMIN);
        subjectDao.assignInstructor(instructor, subject);
        AuditLog.record(actor, "ASSIGN_INSTRUCTOR", instructor.getUsername() + " -> " + subject.getCode());
    }

    public void unassignInstructor(User actor, Instructor instructor, Subject subject) {
        Permissions.requireRole(actor, Role.ADMIN);
        subjectDao.unassignInstructor(instructor, subject);
        AuditLog.record(actor, "UNASSIGN_INSTRUCTOR", instructor.getUsername() + " -/- " + subject.getCode());
    }

    public List<Instructor> instructorsOf(User actor, Subject subject) {
        Permissions.requireRole(actor, Role.ADMIN);
        return subjectDao.findInstructorsOf(subject);
    }

    private static String requireCode(String code, String label) {
        ValidationException.require(code != null && !code.isBlank() && !code.strip().contains(" ")
                        && code.strip().length() <= 20,
                "CODE_INVALID", "Hãy nhập " + label + " (không có khoảng trắng, tối đa 20 ký tự).");
        return code.strip();
    }

    private static String requireName(String name, String label) {
        ValidationException.require(name != null && !name.isBlank(), "NAME_EMPTY", label + " không được để trống.");
        return name.strip();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
