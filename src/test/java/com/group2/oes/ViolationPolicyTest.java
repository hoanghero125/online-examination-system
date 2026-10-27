package com.group2.oes;

import com.group2.oes.model.ViolationPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViolationPolicyTest {

    private final ViolationPolicy policy = ViolationPolicy.standard();
    private final Instant t = Instant.parse("2026-11-20T01:00:00Z");

    @Test
    void sựKiệnTrongBaGiâyĐượcGộp() {
        assertTrue(policy.isMerged(t, t.plusMillis(2999)));
        assertFalse(policy.isMerged(t, t.plusSeconds(3)), "đúng 3 giây là một lần vi phạm mới");
        assertFalse(policy.isMerged(null, t), "lần đầu tiên không có gì để gộp");
    }

    @Test
    void đủNămLầnThìKếtThúc() {
        assertFalse(policy.isTerminal(4));
        assertTrue(policy.isTerminal(5));
        assertTrue(policy.isTerminal(6), "đếm vượt (hai máy cùng báo) vẫn là kết thúc");
    }

    @Test
    void sốLầnCònLạiKhôngÂm() {
        assertEquals(5, policy.remaining(0));
        assertEquals(2, policy.remaining(3));
        assertEquals(0, policy.remaining(7));
    }

    @Test
    void quyChếChuẩnLàNămLầnBaGiây() {
        assertEquals(5, policy.getMaxViolations());
        assertEquals(Duration.ofSeconds(3), policy.getMergeWindow());
        assertThrows(IllegalArgumentException.class, () -> new ViolationPolicy(0, Duration.ZERO));
    }
}
