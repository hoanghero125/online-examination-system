package com.group2.oes.service;

import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.GradingDao;
import com.group2.oes.exception.DataAccessException;
import com.group2.oes.grading.SubmissionGrader;
import com.group2.oes.model.Submission;
import com.group2.oes.util.AuditLog;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Máy chấm, chạy trên máy giảng viên hoặc máy chủ (FR-26, FR-31). Mỗi lượt làm hai việc:
 * <ol>
 *   <li>ca nào quá giờ đóng: chuyển bài đang làm sang SUBMITTED với lý do TIME_OUT;</li>
 *   <li>bài nào SUBMITTED: chấm, ghi điểm, chuyển GRADED.</li>
 * </ol>
 * Máy thí sinh không bao giờ chấm, nên không cần đọc đáp án (NFR-04).
 *
 * Lớp này chỉ chạy một lượt; việc lặp lại định kỳ do nơi gọi quyết định (StaffApp, GradingDaemon, kiểm thử).
 * Hai máy chấm chạy cùng lúc vẫn an toàn: GradingDao.saveGrades chỉ ghi khi bài còn SUBMITTED.
 */
public class GradingWorker {

    private static final Logger LOG = Logger.getLogger(GradingWorker.class.getName());

    /** Kết quả một lượt chạy. */
    public record RunResult(int closed, int graded, int failed) {

        public boolean hasWork() {
            return closed > 0 || graded > 0 || failed > 0;
        }
    }

    private final GradingDao gradingDao;
    private final ExamSessionDao sessionDao;
    private final SubmissionGrader grader;

    public GradingWorker(GradingDao gradingDao, ExamSessionDao sessionDao, SubmissionGrader grader) {
        this.gradingDao = gradingDao;
        this.sessionDao = sessionDao;
        this.grader = grader;
    }

    /**
     * Chạy một lượt.
     * Lỗi kết nối CSDL thì dừng cả lượt (ném ra cho nơi gọi ghi log, lượt sau thử lại);
     * lỗi riêng của một bài thì bỏ qua bài đó, chấm tiếp các bài khác.
     */
    public RunResult runOnce() {
        LocalDateTime now = sessionDao.serverTime();
        int closed = gradingDao.closeExpired(now);

        int graded = 0;
        int failed = 0;
        for (int id : gradingDao.findSubmittedIds()) {
            try {
                Submission s = gradingDao.load(id);
                grader.grade(s);
                if (gradingDao.saveGrades(s)) {
                    graded++;
                }
            } catch (DataAccessException e) {
                throw e;
            } catch (RuntimeException e) {
                failed++;
                LOG.log(Level.WARNING, "Không chấm được bài " + id, e);
            }
        }

        if (closed > 0) {
            AuditLog.record(null, "AUTO_SUBMIT", closed + " bài hết giờ");
        }
        if (graded > 0) {
            AuditLog.record(null, "AUTO_GRADE", graded + " bài");
        }
        return new RunResult(closed, graded, failed);
    }
}
