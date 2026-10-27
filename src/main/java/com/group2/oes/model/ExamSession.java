package com.group2.oes.model;

import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Một lần tổ chức thi: đề + khung giờ + tùy chọn + danh sách ghi danh.
 *
 * Từ Milestone 5, lớp này chỉ ghép các phần lại (SRP):
 * - giờ mở/đóng và các phép tính thời gian nằm ở SessionWindow;
 * - ba tùy chọn trộn câu/trộn phương án/cho xem đáp án nằm ở SessionPolicy;
 * - thuật toán trộn nằm ở PaperShuffler.
 */
public class ExamSession {

    private int sessionId;
    private final Exam exam;
    private final SessionWindow window;
    private final SessionPolicy policy;
    private final Set<Integer> enrolledStudentIds = new HashSet<>();   // tra cứu O(1) khi kiểm tra vào thi

    public ExamSession(int sessionId, Exam exam, SessionWindow window, SessionPolicy policy) {
        this.exam = Objects.requireNonNull(exam, "exam");
        this.window = Objects.requireNonNull(window, "window");
        this.policy = Objects.requireNonNull(policy, "policy");
        exam.validateForUse();
        this.sessionId = sessionId;
    }

    public void enroll(Student s) {
        enrolledStudentIds.add(s.getUserId());
    }

    public void enrollAll(Collection<Student> students) {
        students.forEach(this::enroll);
    }

    public void enrollById(int studentId) {
        enrolledStudentIds.add(studentId);
    }

    public boolean isEnrolled(Student s) {
        return enrolledStudentIds.contains(s.getUserId());
    }

    /** Dùng để bật/tắt nút "Vào thi". */
    public boolean canEnter(Student s, LocalDateTime now) {
        return isEnrolled(s) && window.isOpen(now);
    }

    /** BR-01: có tên trong danh sách dự thi và đang trong giờ mở. Ném ngoại lệ kèm lý do cụ thể. */
    public void ensureCanEnter(Student s, LocalDateTime now) {
        if (!isEnrolled(s)) {
            throw SessionRuleException.notEnrolled();
        }
        window.ensureOpen(now);
    }

    /** Bản đề riêng của một thí sinh, cùng thứ tự với bản thí sinh thấy trên máy trạm. */
    public List<ExamItem> buildPaperFor(Student s) {
        if (!policy.shuffleQuestions()) {
            return exam.getItems();
        }
        return PaperShuffler.shuffle(exam.getItems(), PaperShuffler.seedFor(sessionId, s.getUserId()));
    }

    public List<AnswerOption> optionsFor(Student s, Question q) {
        if (!policy.shuffleOptions()) {
            return q.getOptions();
        }
        long seed = PaperShuffler.optionSeed(PaperShuffler.seedFor(sessionId, s.getUserId()), q.getQuestionId());
        return PaperShuffler.shuffle(q.getOptions(), seed);
    }

    public void assignId(int id) {
        if (sessionId != 0) {
            throw new IllegalStateException("Ca thi đã có mã " + sessionId);
        }
        this.sessionId = id;
    }

    public Set<Integer> getEnrolledStudentIds() {
        return Collections.unmodifiableSet(enrolledStudentIds);
    }

    public int getSessionId() {
        return sessionId;
    }

    public Exam getExam() {
        return exam;
    }

    public SessionWindow getWindow() {
        return window;
    }

    public SessionPolicy getPolicy() {
        return policy;
    }
}
