package com.group2.oes.client;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.PaperShuffler;
import com.group2.oes.model.SessionPolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đề thi trên máy thí sinh: danh sách câu theo thứ tự hiển thị, tra được theo số thứ tự câu gốc.
 */
public class ExamPaper {

    private final List<PaperItem> items;                                  // thứ tự hiển thị
    private final Map<Integer, PaperItem> byOrder = new LinkedHashMap<>(); // số câu gốc -> câu

    public ExamPaper(List<PaperItem> items) {
        this.items = List.copyOf(items);
        for (PaperItem it : this.items) {
            byOrder.put(it.order(), it);
        }
    }

    /**
     * Bản đề riêng của một thí sinh. Dùng cùng PaperShuffler và cùng seed với ExamSession.buildPaperFor
     * phía giảng viên, nên hai bên thấy cùng một thứ tự.
     */
    public ExamPaper shuffledFor(int sessionId, int studentId, SessionPolicy policy) {
        long seed = PaperShuffler.seedFor(sessionId, studentId);
        List<PaperItem> list = policy.shuffleQuestions() ? PaperShuffler.shuffle(items, seed) : items;
        if (policy.shuffleOptions()) {
            list = list.stream()
                    .map(it -> it.withOptions(PaperShuffler.shuffle(it.options(),
                            PaperShuffler.optionSeed(seed, it.questionId()))))
                    .toList();
        }
        return new ExamPaper(list);
    }

    /** Câu theo số thứ tự gốc. */
    public PaperItem get(int order) {
        PaperItem it = byOrder.get(order);
        ValidationException.require(it != null, "ITEM_NOT_FOUND", "Đề không có câu số " + order + ".");
        return it;
    }

    public boolean contains(int order) {
        return byOrder.containsKey(order);
    }

    /** Vị trí hiển thị (bắt đầu từ 0) của câu có số thứ tự gốc {@code order}. */
    public int indexOf(int order) {
        return items.indexOf(get(order));
    }

    public List<PaperItem> getItems() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public double totalPoint() {
        return items.stream().mapToDouble(PaperItem::point).sum();
    }
}
