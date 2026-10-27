package com.group2.oes.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Quy chế rời cửa sổ làm bài (BR-05, UC-15): khi nào gộp sự kiện, khi nào kết thúc bài.
 *
 * Trước đây hằng số nằm rải ở Submission (MAX_VIOLATIONS) và ProctoringService (MERGE_WINDOW).
 * Gom về một lớp (SRP) để quy chế đọc được ở một chỗ và kiểm thử riêng không cần CSDL.
 */
public final class ViolationPolicy {

    public static final int MAX_VIOLATIONS = 5;

    /**
     * Một lần Alt+Tab thường làm JavaFX phát liền hai, ba sự kiện (thoát toàn màn hình, mất tiêu điểm...).
     * Các sự kiện cách nhau dưới khoảng này được tính là một lần vi phạm.
     */
    public static final Duration MERGE_WINDOW = Duration.ofSeconds(3);

    private final int maxViolations;
    private final Duration mergeWindow;

    public ViolationPolicy(int maxViolations, Duration mergeWindow) {
        if (maxViolations <= 0 || mergeWindow.isNegative()) {
            throw new IllegalArgumentException("Quy chế vi phạm không hợp lệ");
        }
        this.maxViolations = maxViolations;
        this.mergeWindow = mergeWindow;
    }

    /** Quy chế đã chốt: 5 lần, gộp trong 3 giây. */
    public static ViolationPolicy standard() {
        return new ViolationPolicy(MAX_VIOLATIONS, MERGE_WINDOW);
    }

    /**
     * Sự kiện mới có được gộp vào lần vi phạm được tính gần nhất hay không.
     * @param lastCounted thời điểm lần vi phạm được tính gần nhất, null nếu chưa có lần nào
     */
    public boolean isMerged(Instant lastCounted, Instant event) {
        return lastCounted != null && Duration.between(lastCounted, event).compareTo(mergeWindow) < 0;
    }

    /** Đủ số lần thì kết thúc bài ngay. */
    public boolean isTerminal(int violationCount) {
        return violationCount >= maxViolations;
    }

    public int remaining(int violationCount) {
        return Math.max(0, maxViolations - violationCount);
    }

    /** Câu quy chế hiện trên màn hình xác nhận trước khi thi (FR-27), lấy đúng con số đang áp dụng. */
    public String rulesText() {
        return "Rời khỏi cửa sổ làm bài " + maxViolations + " lần sẽ bị kết thúc bài thi ngay lập tức. "
                + "Các lần rời cửa sổ sát nhau trong " + mergeWindow.toSeconds() + " giây được tính là một lần.";
    }

    public int getMaxViolations() {
        return maxViolations;
    }

    public Duration getMergeWindow() {
        return mergeWindow;
    }
}
