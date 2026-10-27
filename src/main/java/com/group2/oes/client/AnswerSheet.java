package com.group2.oes.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bài làm như thí sinh đang thấy trên màn hình, kể cả câu trả lời vừa gõ chưa kịp lưu xuống CSDL.
 *
 * Tách khỏi AttemptState (bản sao những gì CSDL đã nhận): khi mạng chậm, thí sinh vẫn thấy đúng câu mình vừa chọn,
 * còn việc lưu do hàng đợi lưu bài lo. Chỉ dùng trên luồng giao diện.
 */
public class AnswerSheet {

    private final ExamPaper paper;
    private final Map<Integer, String> responses = new HashMap<>();
    private final Set<Integer> flagged = new HashSet<>();

    public AnswerSheet(ExamPaper paper, AttemptState saved) {
        this.paper = paper;
        responses.putAll(saved.getAnswers());
        flagged.addAll(saved.getFlagged());
    }

    public String response(int order) {
        return responses.get(order);
    }

    public void setResponse(int order, String response) {
        if (response == null || response.isBlank()) {
            responses.remove(order);
        } else {
            responses.put(order, response);
        }
    }

    public boolean isAnswered(int order) {
        return responses.containsKey(order);
    }

    public boolean isFlagged(int order) {
        return flagged.contains(order);
    }

    public void setFlag(int order, boolean value) {
        if (value) {
            flagged.add(order);
        } else {
            flagged.remove(order);
        }
    }

    public int answeredCount() {
        return (int) paper.getItems().stream().filter(it -> isAnswered(it.order())).count();
    }

    public int unansweredCount() {
        return paper.size() - answeredCount();
    }

    public int flaggedCount() {
        return flagged.size();
    }
}
