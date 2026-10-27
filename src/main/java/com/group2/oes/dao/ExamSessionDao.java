package com.group2.oes.dao;

import com.group2.oes.model.ExamSession;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ExamSessionDao {

    ExamSession insert(ExamSession session);

    Optional<ExamSession> findById(int sessionId);

    /** Giờ chuẩn của hệ thống, lấy từ máy chủ CSDL chứ không lấy từ máy trạm. */
    LocalDateTime serverTime();
}
