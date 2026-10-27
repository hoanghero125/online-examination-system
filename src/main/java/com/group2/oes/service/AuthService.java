package com.group2.oes.service;

import com.group2.oes.dao.SubjectDao;
import com.group2.oes.dao.UserDao;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Role;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

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
     */
    public User login(String username, String password) {
        User u = userDao.findByUsername(username == null ? "" : username.strip())
                .filter(x -> x.checkPassword(password))
                .orElseThrow(() -> new ValidationException("INVALID_LOGIN", "Sai tên đăng nhập hoặc mật khẩu."));
        if (!u.isActive()) {
            throw new AccessDeniedException("ACCOUNT_LOCKED", "Tài khoản đã bị khóa. Hãy liên hệ quản trị viên.");
        }
        if (u instanceof Instructor ins) {
            subjectDao.loadSubjectsOf(ins);
        }
        AuditLog.record(u, "LOGIN", u.getUsername());
        return u;
    }

    public <T extends User> T createUser(User actor, T newUser) {
        Permissions.requireRole(actor, Role.ADMIN);
        try {
            T saved = userDao.insert(newUser);
            AuditLog.record(actor, "CREATE_USER", saved.getUsername());
            return saved;
        } catch (DuplicateKeyException e) {
            throw new ValidationException("USERNAME_TAKEN",
                    "Tên đăng nhập \"" + newUser.getUsername() + "\" đã có người dùng.");
        }
    }

    public void setActive(User actor, User target, boolean active) {
        Permissions.requireRole(actor, Role.ADMIN);
        ValidationException.require(!actor.equals(target), "SELF_LOCK", "Không thể tự khóa tài khoản của chính mình.");
        target.setActive(active);
        userDao.update(target);
        AuditLog.record(actor, active ? "UNLOCK_USER" : "LOCK_USER", target.getUsername());
    }
}
