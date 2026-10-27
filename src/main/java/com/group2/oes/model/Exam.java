package com.group2.oes.model;

import com.group2.oes.exception.ValidationException;
import com.group2.oes.grading.ExactMatchGrading;
import com.group2.oes.grading.GradingStrategy;
import com.group2.oes.grading.PartialMatchGrading;
import com.group2.oes.grading.TextMatchGrading;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Exam {

    // Các chiến lược không giữ trạng thái nên dùng chung một thể hiện cho cả chương trình.
    private static final GradingStrategy EXACT = new ExactMatchGrading();
    private static final GradingStrategy PARTIAL = new PartialMatchGrading();
    private static final GradingStrategy TEXT = new TextMatchGrading();

    private int examId;
    private final String title;
    private final Subject subject;
    private final int version;
    private final MultiChoicePolicy multiChoicePolicy;
    private final List<ExamItem> items = new ArrayList<>();
    private final Set<Integer> questionIds = new HashSet<>();   // chặn một câu bị thêm hai lần
    private final Map<QuestionType, GradingStrategy> strategies = new EnumMap<>(QuestionType.class);
    private boolean locked;

    public Exam(int examId, String title, Subject subject, int version,
                MultiChoicePolicy multiChoicePolicy, boolean locked) {
        ValidationException.require(title != null && !title.isBlank(), "EXAM_TITLE", "Tên đề thi không được để trống.");
        this.examId = examId;
        this.title = title.strip();
        this.subject = Objects.requireNonNull(subject);
        this.version = version;
        this.multiChoicePolicy = Objects.requireNonNull(multiChoicePolicy);
        this.locked = locked;

        // Mỗi loại câu hỏi ứng với một chiến lược. Riêng câu nhiều đáp án tùy cấu hình của đề.
        strategies.put(QuestionType.SINGLE_CHOICE, EXACT);
        strategies.put(QuestionType.TRUE_FALSE, EXACT);
        strategies.put(QuestionType.FILL_IN_BLANK, TEXT);
        strategies.put(QuestionType.MULTI_CHOICE,
                multiChoicePolicy == MultiChoicePolicy.PARTIAL ? PARTIAL : EXACT);
    }

    public static Exam draft(String title, Subject subject, MultiChoicePolicy policy) {
        return new Exam(0, title, subject, 1, policy, false);
    }

    public ExamItem addItem(Question q, double point) {
        ensureEditable();
        ValidationException.require(q.getSubject().equals(subject), "WRONG_SUBJECT",
                "Câu hỏi #" + q.getQuestionId() + " không thuộc môn " + subject.getCode() + ".");
        ValidationException.require(questionIds.add(q.getQuestionId()), "DUPLICATE_QUESTION",
                "Câu hỏi #" + q.getQuestionId() + " đã có trong đề.");
        ExamItem item = new ExamItem(items.size() + 1, point, q);
        items.add(item);
        return item;
    }

    /** Dùng khi DAO dựng lại đề từ CSDL: giữ nguyên thứ tự đã lưu, bỏ qua kiểm tra khóa. */
    public void restoreItem(ExamItem item) {
        questionIds.add(item.getQuestion().getQuestionId());
        items.add(item);
        items.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
    }

    public double getTotalPoint() {
        return items.stream().mapToDouble(ExamItem::getPoint).sum();
    }

    /** BR-11: đề phải có câu hỏi và tổng điểm lớn hơn 0 mới được dùng cho ca thi. */
    public void validateForUse() {
        ValidationException.require(!items.isEmpty(), "EXAM_EMPTY", "Đề thi chưa có câu hỏi nào.");
        ValidationException.require(getTotalPoint() > 0, "EXAM_ZERO_POINT", "Tổng điểm của đề phải lớn hơn 0.");
    }

    /** BR-09: đề đã dùng cho ca thi thì khóa lại, muốn sửa phải tạo phiên bản mới. */
    public void lock() {
        validateForUse();
        this.locked = true;
    }

    public Exam newVersion() {
        Exam copy = new Exam(0, title, subject, version + 1, multiChoicePolicy, false);
        for (ExamItem it : items) {
            copy.addItem(it.getQuestion(), it.getPoint());
        }
        return copy;
    }

    public GradingStrategy strategyFor(Question q) {
        return strategies.get(q.getType());
    }

    public ExamItem getItem(int order) {
        ValidationException.require(order >= 1 && order <= items.size(), "ITEM_NOT_FOUND",
                "Đề không có câu số " + order + ".");
        return items.get(order - 1);
    }

    private void ensureEditable() {
        ValidationException.require(!locked, "EXAM_LOCKED",
                "Đề \"" + title + "\" đã được dùng cho ca thi nên không sửa được. Hãy tạo phiên bản mới.");
    }

    public void assignId(int id) {
        if (examId != 0) {
            throw new IllegalStateException("Đề thi đã có mã " + examId);
        }
        this.examId = id;
    }

    public List<ExamItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getExamId() {
        return examId;
    }

    public String getTitle() {
        return title;
    }

    public Subject getSubject() {
        return subject;
    }

    public int getVersion() {
        return version;
    }

    public MultiChoicePolicy getMultiChoicePolicy() {
        return multiChoicePolicy;
    }

    public boolean isLocked() {
        return locked;
    }
}
