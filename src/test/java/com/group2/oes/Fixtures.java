package com.group2.oes;

import com.group2.oes.model.Difficulty;
import com.group2.oes.model.Subject;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.FillInBlankQuestion;
import com.group2.oes.model.question.MultiChoiceQuestion;
import com.group2.oes.model.question.SingleChoiceQuestion;
import com.group2.oes.model.question.TrueFalseQuestion;

import java.util.List;

/** Dữ liệu mẫu dùng chung cho các lớp kiểm thử. Mã phương án được gán sẵn để viết câu trả lời. */
public final class Fixtures {

    public static final Subject OOP = new Subject(1, "INT1332", "Lập trình hướng đối tượng");
    public static final Subject DB = new Subject(2, "INT1313", "Cơ sở dữ liệu");

    private Fixtures() {
    }

    /** Phương án đúng có mã 11; các phương án sai 12, 13. */
    public static SingleChoiceQuestion single(int id, int chapter, Difficulty d) {
        return new SingleChoiceQuestion(id, OOP, "Câu một đáp án " + id, chapter, d, 1,
                List.of(new AnswerOption(11, "Đúng", true),
                        new AnswerOption(12, "Sai 1", false),
                        new AnswerOption(13, "Sai 2", false)));
    }

    /** Hai phương án đúng 21, 22; hai phương án sai 23, 24. */
    public static MultiChoiceQuestion multi(int id) {
        return new MultiChoiceQuestion(id, OOP, "Câu nhiều đáp án " + id, 2, Difficulty.MEDIUM, 2,
                List.of(new AnswerOption(21, "A", true),
                        new AnswerOption(22, "B", true),
                        new AnswerOption(23, "C", false),
                        new AnswerOption(24, "D", false)));
    }

    public static TrueFalseQuestion trueFalse(int id, boolean value) {
        return new TrueFalseQuestion(id, OOP, "Câu đúng sai " + id, 1, Difficulty.EASY, 1, value);
    }

    public static FillInBlankQuestion fill(int id, String... accepted) {
        return new FillInBlankQuestion(id, OOP, "Câu điền khuyết " + id, 3, Difficulty.HARD, 1,
                List.of(accepted), false);
    }
}
