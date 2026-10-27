package com.group2.oes.grading;

import com.group2.oes.model.question.Question;

/** Điểm tỷ lệ với mức đúng mà câu hỏi tự đánh giá qua correctRatio. */
public class PartialMatchGrading implements GradingStrategy {

    @Override
    public double grade(Question question, String response, double maxPoint) {
        double ratio = question.correctRatio(response);
        // chặn hai đầu phòng trường hợp lớp con tính sai, điểm không bao giờ âm hay vượt trần
        ratio = Math.min(1.0, Math.max(0.0, ratio));
        // làm tròn 2 chữ số để bảng điểm không hiện 0.6666666667
        return Math.round(ratio * maxPoint * 100.0) / 100.0;
    }
}
