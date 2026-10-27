package com.group2.oes;

import com.group2.oes.dao.ExamSessionDao;
import com.group2.oes.dao.GradingDao;
import com.group2.oes.exception.DataAccessException;
import com.group2.oes.grading.SubmissionGrader;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.Student;
import com.group2.oes.model.Submission;
import com.group2.oes.model.SubmissionStatus;
import com.group2.oes.service.GradingScheduler;
import com.group2.oes.service.GradingWorker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.group2.oes.Fixtures.OOP;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FR-26: máy chấm chạy định kỳ, chịu được lỗi của từng bài và lỗi mất kết nối. */
class GradingSchedulerTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 8, 0);

    /** GradingDao giả: bài mã 1 hỏng dữ liệu, bài mã 2 bình thường; có thể giả lập mất kết nối. */
    private static final class FakeGradingDao implements GradingDao {
        final List<Integer> pending = new ArrayList<>(List.of(1, 2));
        final List<Integer> saved = new ArrayList<>();
        boolean down;

        @Override
        public int closeExpired(LocalDateTime now) {
            if (down) {
                throw new DataAccessException("mất kết nối", null);
            }
            return 0;
        }

        @Override
        public List<Integer> findSubmittedIds() {
            return List.copyOf(pending);
        }

        @Override
        public Submission load(int submissionId) {
            if (submissionId == 1) {
                throw new IllegalStateException("dữ liệu hỏng");
            }
            Exam exam = Exam.draft("Đề", OOP, MultiChoicePolicy.PARTIAL);
            exam.addItem(Fixtures.single(5, 1, Difficulty.EASY), 1);
            ExamSession session = new ExamSession(9, exam, new SessionWindow(T0, T0.plusHours(1)),
                    new SessionPolicy(false, false, false));
            Submission s = new Submission(submissionId, new Student(3, "sv", "h$h", "SV", null, true, "B1"), session,
                    T0, null, SubmissionStatus.IN_PROGRESS, null, 0, 0);
            s.restoreAnswer(1, "11");
            s.markSubmitted(EndReason.MANUAL, T0);
            return s;
        }

        @Override
        public boolean saveGrades(Submission submission) {
            pending.remove(Integer.valueOf(submission.getSubmissionId()));
            saved.add(submission.getSubmissionId());
            return true;
        }
    }

    private static final ExamSessionDao CLOCK_ONLY = new ExamSessionDao() {
        @Override
        public ExamSession insert(ExamSession session) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ExamSession> findById(int sessionId) {
            return Optional.empty();
        }

        @Override
        public List<ExamSession> findByExam(int examId) {
            return List.of();
        }

        @Override
        public LocalDateTime serverTime() {
            return T0;
        }
    };

    @Test
    void mộtBàiLỗiKhôngChặnCácBàiKhác() {
        FakeGradingDao dao = new FakeGradingDao();
        GradingWorker worker = new GradingWorker(dao, CLOCK_ONLY, new SubmissionGrader());
        GradingWorker.RunResult r = worker.runOnce();
        assertEquals(1, r.graded());
        assertEquals(1, r.failed());
        assertEquals(List.of(2), dao.saved);
        assertFalse(worker.runOnce().hasWork(), "bài lỗi thử lại không tính là có việc");
    }

    @Test
    void mấtKếtNốiThìDừngCảLượtNhưngLịchVẫnChạyTiếp() {
        FakeGradingDao dao = new FakeGradingDao();
        dao.down = true;
        GradingWorker worker = new GradingWorker(dao, CLOCK_ONLY, new SubmissionGrader());
        assertThrows(DataAccessException.class, worker::runOnce);

        List<GradingWorker.RunResult> seen = new ArrayList<>();
        GradingScheduler scheduler = new GradingScheduler(worker, Duration.ofSeconds(1), seen::add);
        assertDoesNotThrow(() -> scheduler.runNow(), "một lượt lỗi không được làm dừng hẳn lịch chạy");
        dao.down = false;
        scheduler.runNow();
        assertEquals(1, seen.size(), "kết nối lại thì chấm tiếp");
    }

    @Test
    void lịchChạyĐịnhKỳGọiMáyChấm() throws InterruptedException {
        FakeGradingDao dao = new FakeGradingDao();
        GradingWorker worker = new GradingWorker(dao, CLOCK_ONLY, new SubmissionGrader());
        CountDownLatch done = new CountDownLatch(1);
        try (GradingScheduler scheduler = new GradingScheduler(worker, Duration.ofMillis(20), r -> done.countDown())) {
            scheduler.start();
            assertTrue(scheduler.isRunning());
            assertTrue(done.await(5, TimeUnit.SECONDS), "máy chấm được gọi trong vòng vài giây");
        }
        assertEquals(List.of(2), dao.saved);
    }
}
