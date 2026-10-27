package com.group2.oes.dao;

import com.group2.oes.model.Exam;
import com.group2.oes.model.Subject;

import java.util.List;
import java.util.Optional;

public interface ExamDao {

    Exam insert(Exam exam);

    Optional<Exam> findById(int examId);

    /** Mọi đề của một môn (mọi phiên bản), xếp theo tên rồi phiên bản. */
    List<Exam> findBySubject(Subject subject);

    void lock(int examId);
}
