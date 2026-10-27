package com.group2.oes;

import com.group2.oes.client.AnswerFormat;
import com.group2.oes.client.AnswerSheet;
import com.group2.oes.client.AttemptState;
import com.group2.oes.client.ExamPaper;
import com.group2.oes.client.PaperItem;
import com.group2.oes.client.SessionEntry;
import com.group2.oes.client.SessionInfo;
import com.group2.oes.client.ServerClock;
import com.group2.oes.model.EndReason;
import com.group2.oes.model.SessionPhase;
import com.group2.oes.model.SessionPolicy;
import com.group2.oes.model.SessionWindow;
import com.group2.oes.model.ViolationPolicy;
import com.group2.oes.model.question.QuestionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Các lớp hỗ trợ màn hình làm bài: không có JavaFX nên kiểm thử được như lớp thường. */
class ClientModelTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 8, 0);

    @Test
    void đồngHồMáyChủKhôngPhụThuộcGiờMáyTrạm() {
        AtomicLong nanos = new AtomicLong(1_000);
        ServerClock clock = new ServerClock(T0, nanos::get);
        nanos.addAndGet(90_000_000_000L);   // 90 giây trôi qua
        assertEquals(T0.plusSeconds(90), clock.now());
        clock.resync(T0.plusSeconds(100));   // đồng bộ lại: máy trạm chạy chậm 10 giây
        assertEquals(T0.plusSeconds(100), clock.now());
    }

    @Test
    void cảnhBáoNămPhútCuốiVàĐịnhDạngĐồngHồ() {
        SessionWindow w = new SessionWindow(T0, T0.plusMinutes(60));
        assertFalse(w.isAlmostOver(T0.plusMinutes(54)));
        assertTrue(w.isAlmostOver(T0.plusMinutes(55)));
        assertFalse(w.isAlmostOver(T0.plusMinutes(60)), "đã đóng thì không còn là sắp hết giờ");
        assertEquals("05:09", SessionWindow.formatRemaining(309));
        assertEquals("1:00:00", SessionWindow.formatRemaining(3600));
    }

    @Test
    void câuTrảLờiNhiềuĐápÁnCùngĐịnhDạngVớiLúcChấm() {
        assertEquals("12,14", AnswerFormat.joinOptionIds(List.of(14, 12)));
        assertNull(AnswerFormat.joinOptionIds(List.of()), "không chọn gì là chưa trả lời");
        assertEquals(Set.of(12, 14), AnswerFormat.parseOptionIds(" 14,12,abc"));
    }

    @Test
    void bảnNhápĐếmCâuĐãLàmVàCâuĐánhDấu() {
        ExamPaper paper = new ExamPaper(List.of(
                new PaperItem(1, 1, 101, QuestionType.TRUE_FALSE, "a", List.of()),
                new PaperItem(2, 1, 102, QuestionType.FILL_IN_BLANK, "b", List.of()),
                new PaperItem(3, 1, 103, QuestionType.TRUE_FALSE, "c", List.of())));
        AttemptState saved = AttemptState.started(9, 1, 7);
        saved.recordAnswer(1, "true");
        saved.setFlag(3, true);
        AnswerSheet sheet = new AnswerSheet(paper, saved);
        assertEquals(1, sheet.answeredCount());
        sheet.setResponse(2, "extends");
        sheet.setResponse(1, "  ");   // xóa câu trả lời
        assertEquals(1, sheet.answeredCount());
        assertEquals(2, sheet.unansweredCount());
        assertEquals(1, sheet.flaggedCount());
    }

    @Test
    void nútTrênDanhSáchCaThiTheoTrạngThái() {
        SessionInfo info = new SessionInfo(1, 2, "Đề", "INT1332", "OOP", new SessionWindow(T0, T0.plusHours(1)),
                new SessionPolicy(true, true, true), 10, 5);
        AttemptState doing = AttemptState.started(9, 1, 7);
        AttemptState done = AttemptState.started(10, 1, 7);
        done.markSubmitted(EndReason.MANUAL);

        assertTrue(new SessionEntry(info, SessionPhase.OPEN, null).canEnter());
        assertTrue(new SessionEntry(info, SessionPhase.OPEN, doing).isResuming());
        assertFalse(new SessionEntry(info, SessionPhase.OPEN, done).canEnter(), "đã nộp thì không vào lại");
        assertTrue(new SessionEntry(info, SessionPhase.OPEN, done).canViewResult());
        assertFalse(new SessionEntry(info, SessionPhase.UPCOMING, null).canEnter());
        assertFalse(new SessionEntry(info, SessionPhase.CLOSED, null).canViewResult());
    }

    @Test
    void quyChếHiệnĐúngConSốĐangÁpDụng() {
        assertEquals("Rời khỏi cửa sổ làm bài 5 lần sẽ bị kết thúc bài thi ngay lập tức. "
                + "Các lần rời cửa sổ sát nhau trong 3 giây được tính là một lần.", ViolationPolicy.standard().rulesText());
    }
}
