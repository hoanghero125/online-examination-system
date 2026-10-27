package com.group2.oes.service;

import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.dao.StudentExamDao;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.User;
import com.group2.oes.model.ViolationPolicy;
import com.group2.oes.util.AuditLog;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ghi nhận vi phạm quy chế thi (UC-15, FR-28, FR-29) trên máy thí sinh.
 *
 * Quy chế (bao nhiêu lần thì kết thúc, gộp trong bao lâu) nằm ở ViolationPolicy;
 * lớp này chỉ điều phối: hỏi quy chế, tăng bộ đếm trong CSDL, kết thúc bài khi cần.
 */
public class ProctoringService {

    /** Kết quả trả về cho màn hình làm bài để hiện cảnh báo. */
    public record ViolationResult(int count, int remaining, boolean terminated, boolean merged) {
    }

    private final StudentExamDao dao;
    private final StudentExamService exams;
    private final ViolationPolicy policy;
    // mã bài làm -> thời điểm của lần vi phạm được tính gần nhất.
    // ConcurrentHashMap vì sự kiện giao diện và luồng nền có thể gọi song song.
    private final Map<Integer, Instant> lastCounted = new ConcurrentHashMap<>();

    public ProctoringService(StudentExamDao dao, StudentExamService exams, ViolationPolicy policy) {
        this.dao = dao;
        this.exams = exams;
        this.policy = policy;
    }

    public ViolationResult report(User actor, ExamAttempt attempt, Instant eventTime) {
        AttemptState state = exams.requireOwn(actor, attempt);
        if (state.isLocked()) {
            return new ViolationResult(state.getViolationCount(), 0, state.isTerminatedByViolation(), false);
        }

        int id = state.getSubmissionId();
        if (policy.isMerged(lastCounted.get(id), eventTime)) {
            int current = state.getViolationCount();
            return new ViolationResult(current, policy.remaining(current), false, true);
        }
        lastCounted.put(id, eventTime);

        int count = dao.increaseViolation(id);
        if (count == state.getViolationCount()) {
            // CSDL không tăng bộ đếm: bài đã bị khóa ở nơi khác (máy chấm đóng ca, máy khác nộp).
            // Không báo "vi phạm lần N" giả; đọc lại trạng thái thật để màn hình chuyển sang kết quả.
            AttemptState fresh = exams.refresh(actor, state.getSessionId());
            state.markSubmitted(fresh.getEndReason());
            return new ViolationResult(fresh.getViolationCount(), 0, fresh.isTerminatedByViolation(), false);
        }
        state.setViolationCount(count);
        if (policy.isTerminal(count)) {
            exams.submit(actor, attempt, EndReason.VIOLATION);
            lastCounted.remove(id);
            AuditLog.record(actor, "TERMINATED_BY_VIOLATION", "ca " + state.getSessionId() + ", bài " + id);
            return new ViolationResult(count, 0, true, false);
        }
        return new ViolationResult(count, policy.remaining(count), false, false);
    }

    public ViolationPolicy getPolicy() {
        return policy;
    }
}
