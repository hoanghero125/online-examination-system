package com.group2.oes.model.question;

import com.group2.oes.exception.ValidationException;

/** Một phương án trả lời của câu hỏi trắc nghiệm. */
public class AnswerOption {

    private int optionId;
    private final String content;
    private final boolean correct;

    public AnswerOption(int optionId, String content, boolean correct) {
        ValidationException.require(content != null && !content.isBlank(),
                "OPTION_EMPTY", "Nội dung phương án không được để trống.");
        this.optionId = optionId;
        this.content = content.strip();
        this.correct = correct;
    }

    public static AnswerOption of(String content, boolean correct) {
        return new AnswerOption(0, content, correct);
    }

    /** Chỉ gán được một lần, khi DAO vừa lưu phương án xuống CSDL. */
    public void assignId(int id) {
        if (this.optionId != 0) {
            throw new IllegalStateException("Phương án đã có mã " + optionId);
        }
        this.optionId = id;
    }

    public int getOptionId() {
        return optionId;
    }

    public String getContent() {
        return content;
    }

    public boolean isCorrect() {
        return correct;
    }

    @Override
    public String toString() {
        return content;
    }
}
