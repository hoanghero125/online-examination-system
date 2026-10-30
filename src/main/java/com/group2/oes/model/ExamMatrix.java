package com.group2.oes.model;

import com.group2.oes.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Ma trận đề: cần lấy bao nhiêu câu ở mỗi cặp (chương, mức độ khó). */
public class ExamMatrix {

    public record Entry(int chapter, Difficulty difficulty, int count) {
        public Entry {
            ValidationException.require(count > 0, "MATRIX_COUNT", "Số câu trong ma trận phải lớn hơn 0.");
            Objects.requireNonNull(difficulty);
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public ExamMatrix add(int chapter, Difficulty difficulty, int count) {
        boolean duplicate = entries.stream()
                .anyMatch(e -> e.chapter() == chapter && e.difficulty() == difficulty);
        ValidationException.require(!duplicate, "MATRIX_DUPLICATE",
                "Ma trận đã có dòng cho chương " + chapter + ", mức " + difficulty.displayName() + ".");
        entries.add(new Entry(chapter, difficulty, count));
        return this;
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int totalRequired() {
        return entries.stream().mapToInt(Entry::count).sum();
    }

    /** Ngân hàng có đủ câu cho mọi dòng của ma trận hay không. */
    public boolean check(QuestionBank bank) {
        return bank.findShortages(this).isEmpty();
    }
}
