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

    /**
     * Sửa câu hỏi (FR-08): ghi lại mọi trường và thay toàn bộ phương án, trong một giao dịch.
     * Phương án mới được gán mã mới. Nơi gọi phải kiểm tra isUsedInLockedExam trước,
     * vì câu trả lời đã lưu tham chiếu tới mã phương án cũ.
     */
    void update(Question q);

    void delete(int questionId);

    /** Câu hỏi đã nằm trong một đề đã khóa (đề đã dùng cho ca thi) hay chưa. */
    boolean isUsedInLockedExam(int questionId);

    /** Câu hỏi có nằm trong đề nào không (kể cả đề chưa khóa). */
    boolean isUsedInAnyExam(int questionId);
}
