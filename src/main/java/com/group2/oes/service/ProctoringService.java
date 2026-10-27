package com.group2.oes.service;

import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Submission;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ghi nhận vi phạm quy chế thi (UC-15).
 *
 * Một lần Alt+Tab thường làm JavaFX phát liền hai, ba sự kiện (thoát toàn màn hình, mất tiêu điểm...).
 * Nếu đếm hết thì một lần thoát thành ba lần vi phạm, nên các sự kiện trong cùng một khoảng ngắn
 * được gộp làm một.
 */
public class ProctoringService {

    public static final Duration MERGE_WINDOW = Duration.ofSeconds(3);

    /** Kết quả trả về cho màn hình làm bài để hiện cảnh báo. */
    public record ViolationResult(int count, int remaining, boolean terminated, boolean merged) {
    }

    private final SubmissionDao submissionDao;
    private final SubmissionService submissionService;
    // mã bài làm -> thời điểm của lần vi phạm được tính gần nhất.
    // ConcurrentHashMap vì sự kiện giao diện và luồng tự nộp bài có thể chạy song song.
    private final Map<Integer, Instant> lastCounted = new ConcurrentHashMap<>();

    public ProctoringService(SubmissionDao submissionDao, SubmissionService submissionService) {
        this.submissionDao = submissionDao;
        this.submissionService = submissionService;
    }

    public ViolationResult report(User actor, int sessionId, Instant eventTime) {
        Submission s = submissionService.loadOwn(actor, sessionId);
        if (s.isLocked()) {
            return new ViolationResult(s.getViolationCount(), 0, s.isTerminatedByViolation(), false);
        }

        Instant last = lastCounted.get(s.getSubmissionId());
        if (last != null && Duration.between(last, eventTime).compareTo(MERGE_WINDOW) < 0) {
            int current = s.getViolationCount();
            return new ViolationResult(current, Submission.MAX_VIOLATIONS - current, false, true);
        }
        lastCounted.put(s.getSubmissionId(), eventTime);

        int count = submissionDao.increaseViolation(s.getSubmissionId());
        if (count >= Submission.MAX_VIOLATIONS) {
            Submission done = submissionService.submit(actor, sessionId, EndReason.VIOLATION);
            lastCounted.remove(s.getSubmissionId());
            AuditLog.record(actor, "TERMINATED_BY_VIOLATION", "ca " + sessionId + ", điểm " + done.getTotalScore());
            return new ViolationResult(count, 0, true, false);
        }
        return new ViolationResult(count, Submission.MAX_VIOLATIONS - count, false, false);
    }
}
