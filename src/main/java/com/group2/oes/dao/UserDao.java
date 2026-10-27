package com.group2.oes.dao;

import com.group2.oes.model.Student;
import com.group2.oes.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    Optional<User> findByUsername(String username);

    Optional<User> findById(int userId);

    /** Thêm người dùng mới, gán mã do CSDL sinh ra vào đối tượng rồi trả lại chính nó. */
    <T extends User> T insert(T user);

    void update(User user);

    List<Student> findStudentsOfClass(int classId);

    List<User> findAll();
}
