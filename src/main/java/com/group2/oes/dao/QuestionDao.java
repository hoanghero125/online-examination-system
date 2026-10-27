package com.group2.oes.dao;

import com.group2.oes.model.Subject;
import com.group2.oes.model.question.Question;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QuestionDao {

    /** Lưu câu hỏi cùng các phương án, gán mã cho câu hỏi và từng phương án. */
    Question insert(Question q);

    Optional<Question> findById(int questionId);

    /** Trả về Map mã -> câu hỏi, dùng khi dựng lại đề thi. */
    Map<Integer, Question> findByIds(Collection<Integer> ids);

    List<Question> findBySubject(Subject subject);

    void delete(int questionId);

    /** Câu hỏi đã nằm trong một đề đã khóa (đề đã dùng cho ca thi) hay chưa. */
    boolean isUsedInLockedExam(int questionId);
}
