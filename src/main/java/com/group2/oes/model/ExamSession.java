package com.group2.oes.model;

import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Một lần tổ chức thi. Mọi thí sinh dùng chung giờ mở và giờ đóng,
 * vào muộn thì còn ít thời gian hơn chứ không được cộng thêm.
 *
 * Các hàm cần "bây giờ" đều nhận thời điểm từ bên ngoài thay vì tự gọi LocalDateTime.now():
 * thời gian chuẩn phải lấy từ máy chủ CSDL, và kiểm thử cần giả lập được thời gian.
 */
public class ExamSession {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private int sessionId;
    private final Exam exam;
    private final LocalDateTime openTime;
    private final LocalDateTime closeTime;
    private final boolean shuffleQuestions;
    private final boolean shuffleOptions;
    private final boolean showAnswer;
    private final Set<Integer> enrolledStudentIds = new HashSet<>();   // tra cứu O(1) khi kiểm tra vào thi

    public ExamSession(int sessionId, Exam exam, LocalDateTime openTime, LocalDateTime closeTime,
                       boolean shuffleQuestions, boolean shuffleOptions, boolean showAnswer) {
        Objects.requireNonNull(exam, "exam");
        Objects.requireNonNull(openTime, "openTime");
        Objects.requireNonNull(closeTime, "closeTime");
        ValidationException.require(closeTime.isAfter(openTime), "SESSION_TIME",
                "Giờ đóng ca thi phải sau giờ mở.");
        exam.validateForUse();
        this.sessionId = sessionId;
        this.exam = exam;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.shuffleQuestions = shuffleQuestions;
        this.shuffleOptions = shuffleOptions;
        this.showAnswer = showAnswer;
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

    public boolean isOpen(LocalDateTime now) {
        return !now.isBefore(openTime) && now.isBefore(closeTime);
    }

    public boolean isClosed(LocalDateTime now) {
        return !now.isBefore(closeTime);
    }

    /** Dùng để bật/tắt nút "Vào thi" trên giao diện. */
    public boolean canEnter(Student s, LocalDateTime now) {
        return isEnrolled(s) && isOpen(now);
    }

    /** Giống canEnter nhưng ném ngoại lệ kèm lý do cụ thể để báo cho thí sinh. */
    public void ensureCanEnter(Student s, LocalDateTime now) {
        if (!isEnrolled(s)) {
            throw new SessionRuleException(SessionRuleException.NOT_ENROLLED,
                    "Bạn không có tên trong danh sách dự thi của ca này. Hãy liên hệ giảng viên.");
        }
        if (now.isBefore(openTime)) {
            throw new SessionRuleException(SessionRuleException.NOT_OPEN_YET,
                    "Ca thi chưa mở. Giờ mở: " + openTime.format(FMT) + ".");
        }
        if (isClosed(now)) {
            throw new SessionRuleException(SessionRuleException.CLOSED,
                    "Ca thi đã kết thúc lúc " + closeTime.format(FMT) + ".");
        }
    }

    public long remainingSeconds(LocalDateTime now) {
        return Math.max(0, Duration.between(now, closeTime).getSeconds());
    }

    /**
     * Bản đề riêng của một thí sinh. Seed tính từ mã ca và mã thí sinh,
     * nên thí sinh thoát ra vào lại vẫn thấy đúng thứ tự cũ.
     */
    public List<ExamItem> buildPaperFor(Student s) {
        List<ExamItem> paper = new ArrayList<>(exam.getItems());
        if (shuffleQuestions) {
            Collections.shuffle(paper, new Random(seedFor(s)));
        }
        return paper;
    }

    public List<AnswerOption> optionsFor(Student s, Question q) {
        List<AnswerOption> opts = new ArrayList<>(q.getOptions());
        if (shuffleOptions) {
            Collections.shuffle(opts, new Random(seedFor(s) * 31L + q.getQuestionId()));
        }
        return opts;
    }

    /** Khi ca thi đóng: nộp toàn bộ bài còn mở. Trả về số bài vừa được nộp. */
    public int closeAndSubmitAll(Collection<Submission> inProgress, LocalDateTime now) {
        int count = 0;
        for (Submission sub : inProgress) {
            if (sub.submit(EndReason.TIME_OUT, now)) {
                count++;
            }
        }
        return count;
    }

    private long seedFor(Student s) {
        return Objects.hash(sessionId, s.getUserId());
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

    public LocalDateTime getOpenTime() {
        return openTime;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public boolean isShuffleQuestions() {
        return shuffleQuestions;
    }

    public boolean isShuffleOptions() {
        return shuffleOptions;
    }

    public boolean isShowAnswer() {
        return showAnswer;
    }
}
