package com.group2.oes.service;

import java.util.List;

/**
 * Kết quả nhập dữ liệu từ tệp: số dòng nhập được và danh sách lỗi theo từng dòng.
 * Dùng chung cho nhập câu hỏi (FR-12) và nhập người dùng (FR-06).
 */
public record ImportResult(int imported, List<String> errors) {

    public ImportResult {
        errors = List.copyOf(errors);
    }
}
