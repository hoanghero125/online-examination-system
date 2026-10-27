package com.group2.oes.model;

import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;

import java.util.Locale;

/**
 * Tiêu chí lọc câu hỏi trong ngân hàng. Trường nào null thì không lọc theo trường đó.
 */
public record Criteria(Integer chapter, Difficulty difficulty, QuestionType type, String keyword) {

    public static Criteria any() {
        return new Criteria(null, null, null, null);
    }

    public boolean matches(Question q) {
        return (chapter == null || q.getChapter() == chapter)
                && (difficulty == null || q.getDifficulty() == difficulty)
                && (type == null || q.getType() == type)
                && (keyword == null || keyword.isBlank()
                    || q.getContent().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)));
    }
}
