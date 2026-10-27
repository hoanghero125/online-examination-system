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

    /** Đổi tên môn. Mã môn không đổi vì đã in trên đề và bảng điểm. */
    void update(Subject subject);

    /** Xóa môn cùng các dòng phân công giảng viên. Nơi gọi phải kiểm tra isInUse trước. */
    void delete(int subjectId);

    /** Môn đã có câu hỏi, đề thi hoặc lớp học phần thì không xóa được. */
    boolean isInUse(int subjectId);

    CourseClass insertClass(String code, Subject subject, String semester);

    void updateClass(CourseClass courseClass);

    Optional<CourseClass> findClassByCode(String code);

    /** @param subject null thì lấy lớp của mọi môn */
    List<CourseClass> findClasses(Subject subject);

    void assignInstructor(Instructor instructor, Subject subject);

    void unassignInstructor(Instructor instructor, Subject subject);

    List<Instructor> findInstructorsOf(Subject subject);

    /** Nạp danh sách môn được phân công vào đối tượng giảng viên. */
    void loadSubjectsOf(Instructor instructor);
}
