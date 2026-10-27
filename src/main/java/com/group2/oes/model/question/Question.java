package com.group2.oes.model.question;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;

import java.util.List;
import java.util.Objects;

/**
 * Phần chung của mọi loại câu hỏi.
 *
 * Mỗi lớp con tự quyết định cách so khớp câu trả lời thông qua isCorrect và correctRatio.
 * Câu trả lời của thí sinh luôn được truyền dưới dạng chuỗi:
 * - một đáp án: mã phương án, ví dụ "12"
 * - nhiều đáp án: các mã cách nhau bằng dấu phẩy, ví dụ "12,14"
 * - đúng/sai: "true" hoặc "false"
 * - điền khuyết: văn bản thí sinh gõ
 */
public abstract class Question {

    private int questionId;
    private final Subject subject;
    private final String content;
    private final int chapter;
    private final Difficulty difficulty;
    private final double defaultPoint;

    protected Question(int questionId, Subject subject, String content,
                       int chapter, Difficulty difficulty, double defaultPoint) {
        ValidationException.require(content != null && !content.isBlank(),
                "QUESTION_EMPTY", "Nội dung câu hỏi không được để trống.");
        ValidationException.require(chapter > 0,
                "CHAPTER_INVALID", "Số chương phải lớn hơn 0.");
        ValidationException.require(defaultPoint > 0,
                "POINT_INVALID", "Điểm mặc định của câu hỏi phải lớn hơn 0.");
        this.questionId = questionId;
        this.subject = Objects.requireNonNull(subject, "subject");
        this.content = content.strip();
        this.chapter = chapter;
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        this.defaultPoint = defaultPoint;
    }

    public abstract QuestionType getType();

    /** Câu trả lời có hoàn toàn đúng hay không. */
    public abstract boolean isCorrect(String response);

    /** Mức đúng trong khoảng [0, 1], dùng cho chiến lược chấm theo tỷ lệ. */
    public abstract double correctRatio(String response);

    /** Mặc định không có phương án; hai lớp trắc nghiệm ghi đè. */
    public List<AnswerOption> getOptions() {
        return List.of();
    }

    public List<String> getDisplayOptions() {
        return getOptions().stream().map(AnswerOption::getContent).toList();
    }

    public void assignId(int id) {
        if (this.questionId != 0) {
            throw new IllegalStateException("Câu hỏi đã có mã " + questionId);
        }
        this.questionId = id;
    }

    protected static boolean isBlank(String response) {
        return response == null || response.isBlank();
    }

    public int getQuestionId() {
        return questionId;
    }

    public Subject getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public int getChapter() {
        return chapter;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public double getDefaultPoint() {
        return defaultPoint;
    }

    @Override
    public String toString() {
        return "#" + questionId + " [" + getType() + "] " + content;
    }
}
