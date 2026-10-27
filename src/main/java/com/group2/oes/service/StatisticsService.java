package com.group2.oes.service;

import com.group2.oes.dao.SubmissionDao;
import com.group2.oes.model.ExamSession;
import com.group2.oes.model.ResultStatistics;
import com.group2.oes.model.User;
import com.group2.oes.util.AuditLog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class StatisticsService {

    private final SubmissionDao submissionDao;

    public StatisticsService(SubmissionDao submissionDao) {
        this.submissionDao = submissionDao;
    }

    public ResultStatistics statsFor(User actor, ExamSession session) {
        Permissions.requireManage(actor, session.getExam().getSubject());
        return new ResultStatistics(session, submissionDao.findAllOfSession(session));
    }

    /**
     * Xuất bảng điểm ra CSV (FR-35).
     * Ghi thêm BOM ở đầu tệp: thiếu nó thì Excel trên Windows đọc UTF-8 thành ký tự lỗi với tên tiếng Việt.
     * IOException được ném ra cho màn hình xử lý (chọn thư mục khác, báo ổ đĩa đầy...),
     * vì đây là tình huống người dùng tự khắc phục được.
     */
    public Path exportCsv(User actor, ExamSession session, Path target) throws IOException {
        ResultStatistics stats = statsFor(actor, session);
        try (BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            w.write('﻿');
            for (String line : stats.toCsvLines()) {
                w.write(line);
                w.newLine();
            }
        }
        AuditLog.record(actor, "EXPORT_CSV", target.getFileName().toString());
        return target;
    }
}
