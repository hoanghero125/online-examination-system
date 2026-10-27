package com.group2.oes.dao;

import com.group2.oes.model.CourseClass;
import com.group2.oes.model.Instructor;
import com.group2.oes.model.Subject;

import java.util.List;
import java.util.Optional;

/** Môn học, lớp học phần và phân công giảng viên. */
public interface SubjectDao {

    Subject insert(String code, String name);

    Optional<Subject> findByCode(String code);

    List<Subject> findAll();

    CourseClass insertClass(String code, Subject subject, String semester);

    void assignInstructor(Instructor instructor, Subject subject);

    /** Nạp danh sách môn được phân công vào đối tượng giảng viên. */
    void loadSubjectsOf(Instructor instructor);
}
