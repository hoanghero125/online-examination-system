package com.group2.oes.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Trộn thứ tự câu hỏi và phương án cho từng thí sinh.
 *
 * Seed tính từ (mã ca thi, mã thí sinh), nên:
 * - thí sinh thoát ra vào lại vẫn thấy đúng thứ tự cũ;
 * - hai thí sinh ngồi cạnh nhau thấy thứ tự khác nhau.
 *
 * Viết generic để dùng chung cho cả ExamItem (máy giảng viên, có đáp án) và PaperItem (máy thí sinh, không có đáp án):
 * cùng seed và cùng danh sách ban đầu thì hai bên ra cùng một thứ tự.
 */
public final class PaperShuffler {

    private PaperShuffler() {
    }

    public static long seedFor(int sessionId, int studentId) {
        return Objects.hash(sessionId, studentId);
    }

    /** Seed riêng cho phương án của từng câu, để các câu không bị đảo phương án giống hệt nhau. */
    public static long optionSeed(long paperSeed, int questionId) {
        return paperSeed * 31L + questionId;
    }

    /** Trả về bản sao đã trộn; danh sách gốc giữ nguyên. */
    public static <T> List<T> shuffle(List<T> items, long seed) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy, new Random(seed));
        return copy;
    }
}
