package com.group2.oes.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Chuyển câu trả lời câu nhiều đáp án giữa dạng ô đánh dấu trên màn hình và dạng lưu trong CSDL ("12,14").
 * Đặt ở một chỗ để màn hình làm bài và màn hình xem lại hiểu cùng một định dạng với lúc chấm.
 */
public final class AnswerFormat {

    private AnswerFormat() {
    }

    /** Mã các phương án đã chọn, xếp tăng dần; không chọn gì thì trả về null (câu chưa trả lời). */
    public static String joinOptionIds(Collection<Integer> optionIds) {
        if (optionIds.isEmpty()) {
            return null;
        }
        return new TreeSet<>(optionIds).stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static Set<Integer> parseOptionIds(String response) {
        Set<Integer> ids = new TreeSet<>();
        if (response == null || response.isBlank()) {
            return ids;
        }
        Arrays.stream(response.split(",")).map(String::strip).filter(s -> s.matches("\\d+"))
                .forEach(s -> ids.add(Integer.valueOf(s)));
        return ids;
    }
}
