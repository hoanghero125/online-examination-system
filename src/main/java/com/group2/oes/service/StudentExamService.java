package com.group2.oes.service;

import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.client.ExamPaper;
import com.group2.oes.client.ReviewItem;
import com.group2.oes.client.SessionEntry;
import com.group2.oes.client.SessionInfo;
import com.group2.oes.dao.StudentExamDao;
import com.group2.oes.exception.AccessDeniedException;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.SessionPhase;
import com.group2.oes.model.Student;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Nghiệp vụ làm bài trên máy thí sinh (UC-12 → UC-16): xem ca thi, vào thi, lưu câu trả lời, nộp bài, xem kết quả.
 *
 * Thay cho SubmissionService của Milestone 4. Khác biệt chính: KHÔNG chấm bài.
 * Nộp bài chỉ chuyển trạng thái sang SUBMITTED; GradingWorker trên máy giảng viên chấm sau đó (NFR-04).
 * Mọi kiểm tra ở đây đều được CSDL kiểm tra lại bằng trigger, phòng khi ứng dụng thi bị sửa.
 */
public class StudentExamService {

    private final StudentExamDao dao;

    public StudentExamService(StudentExamDao dao) {
        this.dao = dao;
    }

    /** Ca thi của thí sinh, chia ba nhóm (FR-20). EnumMap giữ thứ tự khai báo của enum khi duyệt. */
    public Map<SessionPhase, List<SessionInfo>> sessionsOf(User actor) {
        Student student = Permissions.requireStudent(actor);
        LocalDateTime now = dao.serverTime();
        Map<SessionPhase, List<SessionInfo>> result = new EnumMap<>(SessionPhase.class);
        for (SessionPhase p : SessionPhase.values()) {
            result.put(p, new ArrayList<>());
        }
        for (SessionInfo s : dao.findSessionsOf(student.getUserId())) {
            result.get(s.window().phaseAt(now)).add(s);
        }
        return result;
    }

    /**
     * Danh sách ca thi kèm bài làm của thí sinh, cho màn hình danh sách ca thi (FR-20).
     * Mỗi thí sinh chỉ có vài ca, nên đọc bài làm từng ca một là đủ nhanh.
     */
    public List<SessionEntry> overview(User actor) {
        Student student = Permissions.requireStudent(actor);
        LocalDateTime now = dao.serverTime();
        return dao.findSessionsOf(student.getUserId()).stream()
                .map(s -> new SessionEntry(s, s.window().phaseAt(now),
                        dao.findAttempt(s.sessionId(), student.getUserId()).orElse(null)))
                .toList();
    }

    public SessionInfo getSession(User actor, int sessionId) {
        Permissions.requireStudent(actor);
        return loadSession(sessionId);
    }

    /** Bài đang có (nếu có), để màn hình quy chế hiện "Tiếp tục bài đang làm (đã vi phạm N lần)". */
    public Optional<AttemptState> findAttempt(User actor, int sessionId) {
        Student student = Permissions.requireStudent(actor);
        return dao.findAttempt(sessionId, student.getUserId());
    }

    /**
     * Vào ca thi (UC-13). Ba khả năng:
     * - chưa có bài: kiểm tra điều kiện rồi tạo bài mới;
     * - có bài đang làm dở (thoát ra rồi vào lại, hoặc đổi máy): mở lại đúng bài đó,
     *   giữ câu trả lời, câu đánh dấu và số lần vi phạm;
     * - bài đã nộp: từ chối, mỗi thí sinh chỉ thi một lần (BR-02).
     */
    public ExamAttempt enter(User actor, int sessionId) {
        Student student = Permissions.requireStudent(actor);
        SessionInfo session = loadSession(sessionId);
        LocalDateTime now = dao.serverTime();

        Optional<AttemptState> existing = dao.findAttempt(sessionId, student.getUserId());
        if (existing.isPresent() && existing.get().isLocked()) {
            throw new SessionRuleException(SessionRuleException.ALREADY_SUBMITTED,
                    "Bạn đã nộp bài của ca thi này. Mỗi thí sinh chỉ được thi một lần.");
        }
        if (!dao.isEnrolled(sessionId, student.getUserId())) {
            throw SessionRuleException.notEnrolled();
        }
        session.window().ensureOpen(now);

        AttemptState state = existing.orElseGet(() -> create(student, sessionId, now));
        ExamPaper paper = dao.loadPaper(session.examId())
                .shuffledFor(sessionId, student.getUserId(), session.policy());
        return new ExamAttempt(session, paper, state);
    }

    private AttemptState create(Student student, int sessionId, LocalDateTime now) {
        try {
            AttemptState created = dao.createAttempt(sessionId, student.getUserId(), now);
            AuditLog.record(student, "START_EXAM", "ca " + sessionId);
            return created;
        } catch (DuplicateKeyException e) {
            // Hai máy cùng bấm "Vào thi" gần như một lúc: máy kia đã tạo bài trước.
            // Không báo lỗi, chỉ mở lại bài vừa được tạo.
            return dao.findAttempt(sessionId, student.getUserId()).orElseThrow(() -> e);
        }
    }

    /** Lưu một câu trả lời (FR-24). Bài đã khóa hoặc đã hết giờ thì bị từ chối (BR-04). */
    public void saveAnswer(User actor, ExamAttempt attempt, int order, String response) {
        AttemptState state = writableState(actor, attempt, order);
        dao.saveAnswer(state.getSubmissionId(), order, response);   // CSDL ném SessionRuleException nếu đã hết giờ
        state.recordAnswer(order, response);
    }

    /** Đánh dấu câu cần xem lại (FR-22). */
    public void setFlag(User actor, ExamAttempt attempt, int order, boolean flagged) {
        AttemptState state = writableState(actor, attempt, order);
        dao.setFlag(state.getSubmissionId(), order, flagged);
        state.setFlag(order, flagged);
    }

    private AttemptState writableState(User actor, ExamAttempt attempt, int order) {
        AttemptState state = requireOwn(actor, attempt);
        if (state.isLocked()) {
            throw new SessionRuleException(SessionRuleException.SUBMISSION_LOCKED,
                    "Bài làm đã nộp, không thể sửa câu trả lời.");
        }
        ValidationException.require(attempt.paper().contains(order), "ITEM_NOT_FOUND",
                "Đề không có câu số " + order + ".");
        return state;
    }

    /**
     * Nộp bài (UC-16, FR-25): chỉ chuyển sang SUBMITTED, máy chấm sẽ chấm sau.
     * Nộp lần hai không lỗi, giữ nguyên lần đầu. Nếu máy khác (hoặc máy chấm, khi hết giờ) đã nộp trước
     * thì đọc lại trạng thái thật từ CSDL.
     *
     * @return trạng thái bài sau khi nộp
     */
    public AttemptState submit(User actor, ExamAttempt attempt, EndReason reason) {
        AttemptState state = requireOwn(actor, attempt);
        if (state.isLocked()) {
            return state;
        }
        if (!dao.markSubmitted(state.getSubmissionId(), reason, dao.serverTime())) {
            return refresh(actor, state.getSessionId());
        }
        state.markSubmitted(reason);
        AuditLog.record(actor, "SUBMIT_" + reason, "ca " + state.getSessionId());
        return state;
    }

    /** Đọc lại trạng thái bài từ CSDL. Màn hình "Đang chấm bài…" gọi hàm này định kỳ cho tới khi thấy GRADED. */
    public AttemptState refresh(User actor, int sessionId) {
        return findAttempt(actor, sessionId)
                .orElseThrow(() -> new SessionRuleException("NOT_STARTED", "Bạn chưa vào ca thi này. Hãy vào thi từ danh sách ca thi."));
    }

    /**
     * Xem lại bài sau khi nộp (FR-33): câu trả lời, điểm từng câu, đáp án đúng.
     * Đáp án chỉ có khi ca đã đóng và giảng viên cho xem; CSDL quyết định việc này qua khung nhìn v_review.
     */
    public List<ReviewItem> review(User actor, int sessionId) {
        AttemptState state = refresh(actor, sessionId);
        if (!state.isLocked()) {
            throw new SessionRuleException("NOT_SUBMITTED", "Bạn chưa nộp bài nên chưa xem được kết quả.");
        }
        return dao.loadReview(state.getSubmissionId());
    }

    /** Giờ máy chủ, để màn hình làm bài tính độ lệch với đồng hồ máy trạm. */
    public LocalDateTime serverTime() {
        return dao.serverTime();
    }

    /** Thí sinh chỉ thao tác trên bài của chính mình. */
    AttemptState requireOwn(User actor, ExamAttempt attempt) {
        Student student = Permissions.requireStudent(actor);
        AttemptState state = attempt.state();
        if (state.getStudentId() != student.getUserId()) {
            throw new AccessDeniedException("NOT_OWNER", "Đây không phải bài làm của bạn.");
        }
        return state;
    }

    private SessionInfo loadSession(int sessionId) {
        return dao.findSession(sessionId)
                .orElseThrow(() -> new ValidationException("SESSION_NOT_FOUND", "Không tìm thấy ca thi " + sessionId + ". Hãy bấm Làm mới để tải lại danh sách."));
    }
}
