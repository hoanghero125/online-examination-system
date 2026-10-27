package com.group2.oes.dao;

import com.group2.oes.model.Exam;

import java.util.Optional;

public interface ExamDao {

    Exam insert(Exam exam);

    Optional<Exam> findById(int examId);

    void lock(int examId);
}
