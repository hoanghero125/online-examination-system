package com.group2.oes.service;

import com.group2.oes.dao.StudentLoginDao;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Role;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

/**
 * Đăng nhập trên ứng dụng thi. Chỉ nhận tài khoản thí sinh.
 */
public class StudentAuthService {

    private final StudentLoginDao loginDao;

    public StudentAuthService(StudentLoginDao loginDao) {
        this.loginDao = loginDao;
    }

    /**
     * Sai tên đăng nhập hay sai mật khẩu đều báo cùng một câu, để người dò mật khẩu
     * không biết tên đăng nhập nào có thật.
     * Riêng tài khoản giảng viên/quản trị viên được báo rõ là dùng nhầm ứng dụng: khung nhìn v_login
     * không trả mã băm mật khẩu của các tài khoản này nên máy thí sinh cũng không thể kiểm tra mật khẩu.
     */
    public User login(String username, String password) {
        User u = loginDao.findByUsername(username == null ? "" : username.strip())
                .orElseThrow(StudentAuthService::invalidLogin);
        if (u.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("WRONG_APP",
                    "Tài khoản giảng viên và quản trị viên hãy dùng ứng dụng quản lý (StaffApp).");
        }
        if (!u.checkPassword(password)) {
            throw invalidLogin();
        }
        if (!u.isActive()) {
            throw new AccessDeniedException("ACCOUNT_LOCKED", "Tài khoản đã bị khóa. Hãy liên hệ quản trị viên.");
        }
        AuditLog.record(u, "LOGIN", "ứng dụng thi");
        return u;
    }

    /**
     * Thí sinh tự đổi mật khẩu (FR-04), kể cả lần đăng nhập đầu bị buộc đổi.
     * Máy thí sinh không có quyền sửa bảng người dùng; DAO gọi thủ tục trong CSDL, thủ tục chỉ đổi được
     * khi mã băm cũ còn khớp. Mã băm mới chỉ gán vào đối tượng sau khi CSDL đã nhận.
     */
    public void changePassword(User actor, String oldPassword, String newPassword) {
        Permissions.requireStudent(actor);
        ValidationException.require(actor.checkPassword(oldPassword), "WRONG_PASSWORD", "Mật khẩu cũ không đúng.");
        String newHash = User.hashNewPassword(newPassword);
        if (!loginDao.changePassword(actor.getUserId(), actor.getPasswordHash(), newHash)) {
            throw new ValidationException("PASSWORD_CHANGED_ELSEWHERE",
                    "Mật khẩu vừa được đổi ở nơi khác. Hãy đăng xuất rồi đăng nhập lại.");
        }
        actor.applyNewPasswordHash(newHash);
        AuditLog.record(actor, "CHANGE_PASSWORD", actor.getUsername());
    }

    private static ValidationException invalidLogin() {
        return new ValidationException("INVALID_LOGIN", "Sai tên đăng nhập hoặc mật khẩu. Hãy kiểm tra lại (chú ý phím Caps Lock).");
    }
}
