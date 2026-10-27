package com.group2.oes.service;

import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Role;

/**
 * Dữ liệu nhập ở biểu mẫu tạo tài khoản. Mã sinh viên và lớp chỉ dùng khi vai trò là thí sinh.
 * AuthService.createAccount dựng đúng lớp con (Administrator / Instructor / Student) từ dữ liệu này.
 */
public record NewAccount(Role role, String username, String password, String fullName, String email,
                         String studentCode, CourseClass courseClass) {
}
