package com.group2.oes.service;

import com.group2.oes.dao.SubjectDao;
import com.group2.oes.dao.UserDao;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.util.List;

/** Tài khoản phía ứng dụng quản lý: đăng nhập, quản lý người dùng (UC-01 → UC-04). */
public class AuthService {

    private final UserDao userDao;
    private final SubjectDao subjectDao;

    public AuthService(UserDao userDao, SubjectDao subjectDao) {
        this.userDao = userDao;
        this.subjectDao = subjectDao;
    }

    /**
     * Sai tên đăng nhập hay sai mật khẩu đều báo cùng một câu,
     * để người dò mật khẩu không biết được tên đăng nhập nào có thật.
     * Tài khoản thí sinh được báo rõ là dùng nhầm ứng dụng (sau khi đã đúng mật khẩu).
     */
    public User login(String username, String password) {
        User u = userDao.findByUsername(username == null ? "" : username.strip())
                .filter(x -> x.checkPassword(password))
                .orElseThrow(() -> new ValidationException("INVALID_LOGIN", "Sai tên đăng nhập hoặc mật khẩu."));
        if (!u.isActive()) {
            throw new AccessDeniedException("ACCOUNT_LOCKED", "Tài khoản đã bị khóa. Hãy liên hệ quản trị viên.");
        }
        if (u.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("WRONG_APP", "Tài khoản thí sinh hãy dùng ứng dụng thi (ExamClientApp).");
        }
        if (u instanceof Instructor ins) {
            subjectDao.loadSubjectsOf(ins);
        }
        AuditLog.record(u, "LOGIN", u.getUsername());
        return u;
    }

    public <T extends User> T createUser(User actor, T newUser) {
        Permissions.requireRole(actor, Role.ADMIN);
        validateProfile(newUser.getFullName(), newUser.getEmail());
        try {
            T saved = userDao.insert(newUser);
            AuditLog.record(actor, "CREATE_USER", saved.getUsername());
            return saved;
        } catch (DuplicateKeyException e) {
            throw new ValidationException("USERNAME_TAKEN",
                    "Tên đăng nhập \"" + newUser.getUsername() + "\" hoặc mã sinh viên đã có người dùng.");
        }
    }

    /** FR-01: tìm theo tên đăng nhập, họ tên, mã sinh viên; lọc theo vai trò (null = mọi vai trò). */
    public List<User> searchUsers(User actor, String keyword, Role role) {
        Permissions.requireRole(actor, Role.ADMIN);
        return userDao.search(keyword, role);
    }

    /** FR-01: quản trị viên sửa họ tên, email; với thí sinh sửa cả lớp học phần (null = chưa xếp lớp). */
    public void updateUser(User actor, User target, String fullName, String email, CourseClass courseClass) {
        Permissions.requireRole(actor, Role.ADMIN);
        validateProfile(fullName, email);
        target.setFullName(fullName.strip());
        target.setEmail(blankToNull(email));
        if (target instanceof Student s) {
            s.setCourseClass(courseClass);
        }
        userDao.update(target);
        AuditLog.record(actor, "UPDATE_USER", target.getUsername());
    }

    public void setActive(User actor, User target, boolean active) {
        Permissions.requireRole(actor, Role.ADMIN);
        ValidationException.require(!actor.equals(target), "SELF_LOCK", "Không thể tự khóa tài khoản của chính mình.");
        target.setActive(active);
        userDao.update(target);
        AuditLog.record(actor, active ? "UNLOCK_USER" : "LOCK_USER", target.getUsername());
    }

    /** Quản trị viên đặt mật khẩu tạm; người dùng phải đổi mật khẩu ở lần đăng nhập sau. */
    public void resetPassword(User actor, User target, String temporaryPassword) {
        Permissions.requireRole(actor, Role.ADMIN);
        target.resetPassword(temporaryPassword);
        userDao.update(target);
        AuditLog.record(actor, "RESET_PASSWORD", target.getUsername());
    }

    /** FR-04: người dùng tự đổi mật khẩu, phải nhập đúng mật khẩu cũ. */
    public void changePassword(User actor, String oldPassword, String newPassword) {
        requireLoggedIn(actor);
        ValidationException.require(actor.checkPassword(oldPassword), "WRONG_PASSWORD", "Mật khẩu cũ không đúng.");
        actor.changePassword(newPassword);
        userDao.update(actor);
        AuditLog.record(actor, "CHANGE_PASSWORD", actor.getUsername());
    }

    /** FR-04: người dùng tự sửa họ tên, email. */
    public void updateProfile(User actor, String fullName, String email) {
        requireLoggedIn(actor);
        validateProfile(fullName, email);
        actor.setFullName(fullName.strip());
        actor.setEmail(blankToNull(email));
        userDao.update(actor);
    }

    private static void requireLoggedIn(User actor) {
        if (actor == null) {
            throw new AccessDeniedException("NOT_LOGGED_IN", "Bạn chưa đăng nhập.");
        }
    }

    static void validateProfile(String fullName, String email) {
        ValidationException.require(fullName != null && !fullName.isBlank(), "NAME_EMPTY", "Họ tên không được để trống.");
        ValidationException.require(User.isValidEmail(email), "EMAIL_INVALID",
                "Email \"" + email + "\" không đúng dạng, ví dụ đúng: ten@ptit.edu.vn.");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
