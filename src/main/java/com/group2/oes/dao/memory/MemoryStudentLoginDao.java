package com.group2.oes.dao.memory;

import com.group2.oes.dao.StudentLoginDao;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;

import java.util.Optional;

class MemoryStudentLoginDao implements StudentLoginDao {

    private final InMemoryDatabase db;

    MemoryStudentLoginDao(InMemoryDatabase db) {
        this.db = db;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        synchronized (db) {
            return db.users.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }
    }

    /** Làm như thủ tục sp_student_change_password: chỉ thí sinh, chỉ khi mã băm cũ còn khớp. */
    @Override
    public boolean changePassword(int userId, String oldHash, String newHash) {
        synchronized (db) {
            User stored = db.users.get(userId);
            if (!(stored instanceof Student) || !stored.getPasswordHash().equals(oldHash)) {
                return false;
            }
            stored.applyNewPasswordHash(newHash);
            return true;
        }
    }
}
