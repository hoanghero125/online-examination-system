package com.group2.oes.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Các chỉ số của một ca thi, tính từ danh sách bài làm đã chấm. */
public class ResultStatistics {

    private final ExamSession session;
    private final List<Submission> graded;
    private final DoubleSummaryStatistics summary;
    private final Map<Integer, Long> histogram;          // điểm hệ 10 làm tròn xuống -> số bài
    private final Map<Integer, Double> correctRateByItem; // số thứ tự câu -> tỷ lệ được trọn điểm

    public ResultStatistics(ExamSession session, List<Submission> submissions) {
        this.session = session;
        this.graded = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                .toList();

        this.summary = graded.stream().mapToDouble(Submission::getTotalScore).summaryStatistics();

        // TreeMap để các khoảng điểm tự xếp tăng dần khi in phổ điểm
        this.histogram = graded.stream().collect(Collectors.groupingBy(
                s -> (int) Math.min(10, Math.floor(s.scoreOn10())),
                TreeMap::new,
                Collectors.counting()));

        Map<Integer, Double> rates = new TreeMap<>();
        for (ExamItem item : session.getExam().getItems()) {
            long full = graded.stream()
                    .map(s -> s.getAnswer(item.getOrder()))
                    .filter(a -> a != null && a.isFullMark())
                    .count();
            rates.put(item.getOrder(), graded.isEmpty() ? 0.0 : (double) full / graded.size());
        }
        this.correctRateByItem = rates;
    }

    public int count() {
        return graded.size();
    }

    public double max() {
        return graded.isEmpty() ? 0 : summary.getMax();
    }

    public double min() {
        return graded.isEmpty() ? 0 : summary.getMin();
    }

    public double average() {
        return graded.isEmpty() ? 0 : summary.getAverage();
    }

    public long terminatedByViolation() {
        return graded.stream().filter(Submission::isTerminatedByViolation).count();
    }

    public Map<Integer, Long> getHistogram() {
        return Collections.unmodifiableMap(histogram);
    }

    public Map<Integer, Double> getCorrectRateByItem() {
        return Collections.unmodifiableMap(correctRateByItem);
    }

    /** Câu có tỷ lệ đúng thấp nhất trước, để giảng viên xem câu nào quá khó hoặc đề sai. */
    public List<Map.Entry<Integer, Double>> hardestItems(int limit) {
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(correctRateByItem.entrySet());
        list.sort(Map.Entry.comparingByValue());
        return list.subList(0, Math.min(limit, list.size()));
    }

    /** Bảng điểm dạng CSV, mỗi phần tử là một dòng. Thí sinh xếp theo mã sinh viên. */
    public List<String> toCsvLines() {
        List<String> lines = new ArrayList<>();
        lines.add("ma_sv,ho_ten,diem,diem_he_10,so_cau_tra_loi,so_lan_vi_pham,ket_thuc_do_vi_pham");
        graded.stream()
                .sorted((a, b) -> a.getStudent().getStudentCode().compareTo(b.getStudent().getStudentCode()))
                .forEach(s -> lines.add(String.join(",",
                        s.getStudent().getStudentCode(),
                        csvEscape(s.getStudent().getFullName()),
                        fmt(s.getTotalScore()),
                        fmt(s.scoreOn10()),
                        String.valueOf(s.answeredCount()),
                        String.valueOf(s.getViolationCount()),
                        s.isTerminatedByViolation() ? "co" : "khong")));
        return lines;
    }

    private static String fmt(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    private static String csvEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    public ExamSession getSession() {
        return session;
    }
}
