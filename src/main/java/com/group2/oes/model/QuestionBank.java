package com.group2.oes.model;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.question.Question;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/** Ngân hàng câu hỏi của một môn học. */
public class QuestionBank {

    /** Khóa nhóm câu hỏi theo (chương, mức độ khó) khi sinh đề. */
    private record PoolKey(int chapter, Difficulty difficulty) {
    }

    private final Subject subject;
    // tra theo mã câu hỏi O(1), LinkedHashMap giữ thứ tự thêm vào để hiển thị ổn định
    private final Map<Integer, Question> questions = new LinkedHashMap<>();

    public QuestionBank(Subject subject) {
        this.subject = Objects.requireNonNull(subject);
    }

    public void add(Question q) {
        ValidationException.require(q.getSubject().equals(subject), "WRONG_SUBJECT",
                "Câu hỏi thuộc môn " + q.getSubject().getCode() + ", không thuộc ngân hàng môn " + subject.getCode() + ".");
        ValidationException.require(q.getQuestionId() > 0, "QUESTION_NOT_SAVED",
                "Câu hỏi phải được lưu trước khi đưa vào ngân hàng.");
        questions.put(q.getQuestionId(), q);
    }

    public void addAll(Collection<? extends Question> qs) {
        qs.forEach(this::add);
    }

    public boolean remove(int questionId) {
        return questions.remove(questionId) != null;
    }

    public Question get(int questionId) {
        Question q = questions.get(questionId);
        ValidationException.require(q != null, "QUESTION_NOT_FOUND",
                "Không có câu hỏi mã " + questionId + " trong ngân hàng môn " + subject.getCode() + ".");
        return q;
    }

    public List<Question> findBy(Criteria c) {
        return questions.values().stream().filter(c::matches).toList();
    }

    public int size() {
        return questions.size();
    }

    public Subject getSubject() {
        return subject;
    }

    /** Liệt kê các dòng của ma trận mà ngân hàng không đủ câu. Danh sách rỗng nghĩa là đủ. */
    public List<String> findShortages(ExamMatrix matrix) {
        Map<PoolKey, List<Question>> pool = groupByChapterAndLevel();
        List<String> shortages = new ArrayList<>();
        for (ExamMatrix.Entry e : matrix.getEntries()) {
            int have = pool.getOrDefault(new PoolKey(e.chapter(), e.difficulty()), List.of()).size();
            if (have < e.count()) {
                shortages.add("chương " + e.chapter() + " mức " + e.difficulty()
                        + ": cần " + e.count() + ", có " + have);
            }
        }
        return shortages;
    }

    /**
     * Chọn ngẫu nhiên câu hỏi theo ma trận.
     * Truyền Random từ ngoài vào để kiểm thử có thể cố định seed và nhận lại đúng một bộ đề.
     */
    public List<Question> pick(ExamMatrix matrix, Random random) {
        List<String> shortages = findShortages(matrix);
        if (!shortages.isEmpty()) {
            throw new ValidationException("BANK_SHORTAGE",
                    "Ngân hàng không đủ câu hỏi: " + String.join("; ", shortages) + ".");
        }
        Map<PoolKey, List<Question>> pool = groupByChapterAndLevel();
        List<Question> picked = new ArrayList<>();
        for (ExamMatrix.Entry e : matrix.getEntries()) {
            // sao chép rồi mới trộn, không làm xáo trộn dữ liệu gốc của ngân hàng
            List<Question> candidates = new ArrayList<>(pool.get(new PoolKey(e.chapter(), e.difficulty())));
            Collections.shuffle(candidates, random);
            picked.addAll(candidates.subList(0, e.count()));
        }
        return picked;
    }

    private Map<PoolKey, List<Question>> groupByChapterAndLevel() {
        return questions.values().stream()
                .collect(Collectors.groupingBy(q -> new PoolKey(q.getChapter(), q.getDifficulty())));
    }
}
