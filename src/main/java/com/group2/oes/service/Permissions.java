package com.group2.oes.service;

import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;

/** Các kiểm tra quyền dùng chung cho tầng service (FR-05, BR-10). */
final class Permissions {

    private Permissions() {
    }

    static void requireRole(User actor, Role role) {
        if (actor == null || actor.getRole() != role) {
            throw new AccessDeniedException("ROLE_REQUIRED",
                    "Chức năng này chỉ dành cho " + label(role) + ".");
        }
    }

    /** Đa hình ở đây: không cần biết actor là lớp con nào, canManage tự trả lời đúng. */
    static void requireManage(User actor, Subject subject) {
        if (actor == null || !actor.canManage(subject)) {
            throw new AccessDeniedException("SUBJECT_FORBIDDEN",
                    "Bạn không phụ trách môn " + subject.getCode() + ".");
        }
    }

    static Student requireStudent(User actor) {
        if (actor instanceof Student s) {
            return s;
        }
        throw new AccessDeniedException("ROLE_REQUIRED", "Chức năng này chỉ dành cho thí sinh.");
    }

    private static String label(Role r) {
        return switch (r) {
            case ADMIN -> "quản trị viên";
            case INSTRUCTOR -> "giảng viên";
            case STUDENT -> "thí sinh";
        };
    }
}
