package com.group2.oes.model;

/**
 * Các tùy chọn giảng viên chọn khi mở ca thi (FR-18).
 * Gom ba cờ boolean thành một đối tượng để hàm tạo ca thi không phải nhận một dãy true/false dễ truyền nhầm thứ tự.
 */
public record SessionPolicy(boolean shuffleQuestions, boolean shuffleOptions, boolean showAnswer) {
}
