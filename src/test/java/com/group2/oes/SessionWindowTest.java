package com.group2.oes;

import com.group2.oes.exception.SessionRuleException;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.SessionPhase;
import com.group2.oes.model.SessionWindow;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionWindowTest {

    private static final LocalDateTime OPEN = LocalDateTime.of(2026, 11, 20, 8, 0);
    private final SessionWindow window = new SessionWindow(OPEN, OPEN.plusMinutes(60));

    @Test
    void phânNhómTheoGiờMáyChủ() {
        assertEquals(SessionPhase.UPCOMING, window.phaseAt(OPEN.minusSeconds(1)));
        assertEquals(SessionPhase.OPEN, window.phaseAt(OPEN), "đúng giờ mở là đã mở");
        assertEquals(SessionPhase.OPEN, window.phaseAt(OPEN.plusMinutes(59)));
        assertEquals(SessionPhase.CLOSED, window.phaseAt(OPEN.plusMinutes(60)), "đúng giờ đóng là đã đóng");
    }

    @Test
    void vàoMuộnThìCònÍtThờiGianHơn() {
        assertEquals(3600, window.remainingSeconds(OPEN));
        assertEquals(600, window.remainingSeconds(OPEN.plusMinutes(50)));
        assertEquals(0, window.remainingSeconds(OPEN.plusHours(2)));
    }

    @Test
    void báoLýDoCụThểKhiChưaMởHoặcĐãĐóng() {
        assertEquals(SessionRuleException.NOT_OPEN_YET, assertThrows(SessionRuleException.class,
                () -> window.ensureOpen(OPEN.minusMinutes(1))).getCode());
        assertEquals(SessionRuleException.CLOSED, assertThrows(SessionRuleException.class,
                () -> window.ensureOpen(OPEN.plusHours(1))).getCode());
        assertDoesNotThrow(() -> window.ensureOpen(OPEN.plusMinutes(1)));
    }

    @Test
    void giờĐóngPhảiSauGiờMở() {
        assertEquals("SESSION_TIME", assertThrows(ValidationException.class,
                () -> new SessionWindow(OPEN, OPEN)).getCode());
    }
}
