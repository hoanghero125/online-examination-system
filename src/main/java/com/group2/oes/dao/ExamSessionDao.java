package com.group2.oes.dao;

import com.group2.oes.model.ExamSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamSessionDao {

    ExamSession insert(ExamSession session);

    Optional<ExamSession> findById(int sessionId);

    /** Mọi ca thi mà thí sinh có tên, kể cả ca chưa mở và ca đã đóng. */
    List<ExamSession> findOfStudent(int studentId);

    /** Ca thi đã quá giờ đóng nhưng vẫn còn bài đang làm dở. */
    List<ExamSession> findClosedWithOpenSubmissions(LocalDateTime now);

    /** Giờ chuẩn của hệ thống, lấy từ máy chủ CSDL chứ không lấy từ máy trạm. */
    LocalDateTime serverTime();
}
