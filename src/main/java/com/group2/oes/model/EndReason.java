package com.group2.oes.model;

/** Lý do một bài làm bị kết thúc. */
public enum EndReason {
    MANUAL,     // thí sinh tự bấm nộp
    TIME_OUT,   // ca thi đóng
    VIOLATION   // rời cửa sổ làm bài đủ 5 lần
}
