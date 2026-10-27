package com.group2.oes.dao.memory;

import com.group2.oes.dao.StudentLoginDao;
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
}
