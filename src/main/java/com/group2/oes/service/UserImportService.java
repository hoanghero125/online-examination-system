package com.group2.oes.service;

import com.group2.oes.dao.SubjectDao;
import com.group2.oes.dao.UserDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;
import com.group2.oes.util.CsvLine;
import com.group2.oes.util.PasswordHasher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Nhập danh sách thí sinh từ tệp CSV (FR-06).
 *
 * <pre>
 * ma_sv,ho_ten,email,ma_lop
 * B24DCCE001,Nguyễn Văn An,an.nv@stu.ptit.edu.vn,INT1332-01
 * </pre>
 * Dòng đầu là tiêu đề thì được bỏ qua; email và mã lớp có thể để trống.
 * Tên đăng nhập và mật khẩu mặc định đều là mã sinh viên viết thường; tài khoản bị buộc đổi mật khẩu
 * ở lần đăng nhập đầu. Dòng nào sai thì ghi lỗi và nhập tiếp, giống nhập câu hỏi (FR-12).
 */
public class UserImportService {

    private static final String HEADER = "ma_sv";
    private static final char BOM = '﻿';   // Excel lưu "CSV UTF-8" có ký tự này ở đầu tệp

    private final UserDao userDao;
    private final SubjectDao subjectDao;

    public UserImportService(UserDao userDao, SubjectDao subjectDao) {
        this.userDao = userDao;
        this.subjectDao = subjectDao;
    }

    /** Lỗi đọc tệp là lỗi người dùng phải xử lý (chọn tệp khác), nên ném IOException ra ngoài. */
    public ImportResult importStudents(User actor, Path csv) throws IOException {
        Permissions.requireRole(actor, Role.ADMIN);
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        List<String> errors = new ArrayList<>();
        int imported = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && !line.isEmpty() && line.charAt(0) == BOM) {
                line = line.substring(1);
            }
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#") || (i == 0 && line.toLowerCase(Locale.ROOT).startsWith(HEADER))) {
                continue;
            }
            int lineNo = i + 1;
            try {
                Student s = parse(CsvLine.split(line));
                userDao.insert(s);
                imported++;
            } catch (ValidationException e) {
                errors.add("Dòng " + lineNo + ": " + e.getUserMessage());
            } catch (DuplicateKeyException e) {
                errors.add("Dòng " + lineNo + ": mã sinh viên hoặc tên đăng nhập đã tồn tại.");
            }
        }
        AuditLog.record(actor, "IMPORT_STUDENTS", csv.getFileName() + ": " + imported + " thí sinh, " + errors.size() + " lỗi");
        return new ImportResult(imported, errors);
    }

    private Student parse(List<String> f) {
        ValidationException.require(f.size() == 4, "CSV_COLUMNS", "cần 4 cột (ma_sv,ho_ten,email,ma_lop), có " + f.size() + ".");
        String code = f.get(0);
        String name = f.get(1);
        String email = f.get(2);
        String classCode = f.get(3);
        ValidationException.require(!code.isEmpty() && !code.contains(" "), "STUDENT_CODE_INVALID",
                "mã sinh viên trống hoặc có khoảng trắng.");
        AuthService.validateProfile(name, email);

        String username = code.toLowerCase(Locale.ROOT);
        Student s = new Student(0, username, PasswordHasher.hash(username), name, email.isEmpty() ? null : email,
                true, code.toUpperCase(Locale.ROOT));
        s.setMustChangePassword(true);
        if (!classCode.isEmpty()) {
            CourseClass cls = subjectDao.findClassByCode(classCode)
                    .orElseThrow(() -> new ValidationException("CLASS_NOT_FOUND", "không có lớp học phần " + classCode + "."));
            s.setCourseClass(cls);
        }
        return s;
    }
}
