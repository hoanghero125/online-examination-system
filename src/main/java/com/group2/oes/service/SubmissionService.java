package com.group2.oes.service;

import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.exception.DuplicateKeyException;
import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.Student;
import com.group2.oes.model.Submission;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

public class SubmissionService {

    private final SubmissionDao submissionDao;
    private final ExamSessionDao sessionDao;

    public SubmissionService(SubmissionDao submissionDao, ExamSessionDao sessionDao) {
        this.submissionDao = submissionDao;
        this.sessionDao = sessionDao;
    }

    /**
     * Vào ca thi (UC-13). Ba khả năng:
     * - chưa có bài: kiểm tra điều kiện rồi tạo bài mới;
     * - có bài đang làm dở (thoát ra rồi vào lại): mở lại đúng bài đó, giữ câu trả lời và số lần vi phạm;
     * - bài đã nộp: từ chối, mỗi thí sinh chỉ thi một lần.
     */
    public Submission start(User actor, int sessionId) {
        Student student = Permissions.requireStudent(actor);
        ExamSession session = loadSession(sessionId);
        LocalDateTime now = sessionDao.serverTime();

        var existing = submissionDao.findBySessionAndStudent(session, student);
        if (existing.isPresent()) {
            Submission s = existing.get();
            if (s.isLocked()) {
                throw new SessionRuleException(SessionRuleException.ALREADY_SUBMITTED,
                        "Bạn đã nộp bài của ca thi này. Mỗi thí sinh chỉ được thi một lần.");
            }
            session.ensureCanEnter(student, now);
            return s;
        }

        session.ensureCanEnter(student, now);
        try {
            Submission created = submissionDao.insert(new Submission(student, session, now));
            AuditLog.record(student, "START_EXAM", "ca " + sessionId);
            return created;
        } catch (DuplicateKeyException e) {
            // Hai máy cùng bấm "Vào thi" gần như một lúc: máy kia đã tạo bài trước.
            // Không báo lỗi, chỉ mở lại bài vừa được tạo.
            return submissionDao.findBySessionAndStudent(session, student).orElseThrow(() -> e);
        }
    }

    /** Lưu một câu trả lời (FR-24). Bài đã khóa hoặc đã hết giờ thì bị từ chối (BR-04). */
    public void saveAnswer(User actor, int sessionId, int itemOrder, String response) {
        Submission s = loadOwn(actor, sessionId);
        s.saveAnswer(itemOrder, response, sessionDao.serverTime());   // ném SessionRuleException nếu không ghi được
        submissionDao.saveAnswer(s.getSubmissionId(), itemOrder, response);
    }

    /**
     * Nộp bài (UC-16). Nộp lần hai không lỗi, chỉ trả về kết quả của lần đầu.
     * Nếu máy khác đã nộp trước (saveResult trả về false) thì đọc lại kết quả đã lưu.
     */
    public Submission submit(User actor, int sessionId, EndReason reason) {
        Submission s = loadOwn(actor, sessionId);
        if (!s.submit(reason, sessionDao.serverTime())) {
            return s;
        }
        if (!submissionDao.saveResult(s)) {
            return loadOwn(actor, sessionId);
        }
        AuditLog.record(actor, "SUBMIT_" + reason, "ca " + sessionId + ", điểm " + s.getTotalScore());
        return s;
    }

    /**
     * Chạy định kỳ trên máy giảng viên: nộp mọi bài còn mở của các ca đã quá giờ đóng (FR-26).
     * Trả về số bài vừa được nộp.
     */
    public int closeExpiredSessions() {
        LocalDateTime now = sessionDao.serverTime();
        int total = 0;
        for (ExamSession session : sessionDao.findClosedWithOpenSubmissions(now)) {
            List<Submission> open = submissionDao.findInProgressOfSession(session);
            session.closeAndSubmitAll(open, now);
            for (Submission s : open) {
                if (submissionDao.saveResult(s)) {
                    total++;
                }
            }
        }
        if (total > 0) {
            AuditLog.record(null, "AUTO_SUBMIT", total + " bài");
        }
        return total;
    }

    /** Kết quả của thí sinh sau khi nộp (FR-33). */
    public Submission resultOf(User actor, int sessionId) {
        Submission s = loadOwn(actor, sessionId);
        if (s.getStatus() != SubmissionStatus.GRADED) {
            throw new SessionRuleException("NOT_GRADED", "Bài làm chưa được chấm.");
        }
        return s;
    }

    Submission loadOwn(User actor, int sessionId) {
        Student student = Permissions.requireStudent(actor);
        ExamSession session = loadSession(sessionId);
        return submissionDao.findBySessionAndStudent(session, student)
                .orElseThrow(() -> new SessionRuleException("NOT_STARTED", "Bạn chưa vào ca thi này."));
    }

    private ExamSession loadSession(int sessionId) {
        return sessionDao.findById(sessionId)
                .orElseThrow(() -> new ValidationException("SESSION_NOT_FOUND", "Không tìm thấy ca thi " + sessionId + "."));
    }
}
