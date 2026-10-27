package com.group2.oes.client;

/**
 * Một phương án hiển thị trên máy thí sinh.
 * Cố ý KHÔNG có cờ đúng/sai: dữ liệu này đi qua mạng tới máy thí sinh (NFR-04).
 */
public record PaperOption(int optionId, String content) {

    @Override
    public String toString() {
        return content;
    }
}
