package com.group2.oes.service;

import com.group2.oes.dao.QuestionDao;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.factory.QuestionData;
import com.group2.oes.factory.QuestionFactory;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;
import com.group2.oes.model.question.AnswerOption;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.util.AuditLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestionBankService {

    private final QuestionDao questionDao;

    public QuestionBankService(QuestionDao questionDao) {
        this.questionDao = questionDao;
    }

    public Question addQuestion(User actor, QuestionType type, QuestionData data) {
        Permissions.requireManage(actor, data.subject());
        Question q = QuestionFactory.create(type, data);   // ném ValidationException nếu dữ liệu sai
        return questionDao.insert(q);
    }

    public QuestionBank loadBank(User actor, Subject subject) {
        Permissions.requireManage(actor, subject);
        QuestionBank bank = new QuestionBank(subject);
        bank.addAll(questionDao.findBySubject(subject));
        return bank;
    }

    /**
     * FR-08: sửa câu hỏi. {@code data} mang mã câu hỏi cần sửa (QuestionData.withId); được đổi cả loại câu hỏi.
     * Cùng điều kiện với xóa (FR-13): câu đã nằm trong đề dùng cho ca thi thì không sửa được,
     * vì bài làm đã lưu tham chiếu tới phương án cũ và kết quả thi không được thay đổi về sau.
     */
    public Question updateQuestion(User actor, QuestionType type, QuestionData data) {
        Question original = questionDao.findById(data.questionId())
                .orElseThrow(() -> new ValidationException("QUESTION_NOT_FOUND",
                        "Không tìm thấy câu hỏi #" + data.questionId() + "."));
        Permissions.requireManage(actor, original.getSubject());
        ValidationException.require(original.getSubject().equals(data.subject()), "SUBJECT_CHANGED",
                "Không chuyển câu hỏi sang môn khác được. Hãy tạo câu hỏi mới ở môn kia.");
        if (questionDao.isUsedInLockedExam(original.getQuestionId())) {
            throw new ValidationException("QUESTION_IN_USE", "Câu hỏi #" + original.getQuestionId()
                    + " đã có trong đề được dùng cho ca thi, không sửa được. Hãy tạo câu hỏi mới.");
        }
        Question updated = QuestionFactory.create(type, data);   // ném ValidationException nếu dữ liệu sai
        questionDao.update(updated);
        AuditLog.record(actor, "UPDATE_QUESTION", String.valueOf(updated.getQuestionId()));
        return updated;
    }

    /** FR-13: không xóa câu hỏi đã nằm trong đề dùng cho ca thi. */
    public void deleteQuestion(User actor, Question q) {
        Permissions.requireManage(actor, q.getSubject());
        if (questionDao.isUsedInLockedExam(q.getQuestionId())) {
            throw new ValidationException("QUESTION_IN_USE",
                    "Câu hỏi #" + q.getQuestionId() + " đã có trong đề được dùng cho ca thi, không thể xóa.");
        }
        questionDao.delete(q.getQuestionId());
        AuditLog.record(actor, "DELETE_QUESTION", String.valueOf(q.getQuestionId()));
    }

    /**
     * FR-12: nhập câu hỏi từ tệp văn bản, mỗi dòng một câu, các trường cách nhau bằng "|":
     * <pre>
     * LOAI | chuong | muc_do | diem | noi_dung | dap_an
     * SINGLE_CHOICE|1|EASY|1|Java chạy trên gì?|*JVM;CLR;V8
     * MULTI_CHOICE|2|MEDIUM|2|Chọn kiểu nguyên thủy|*int;String;*boolean
     * TRUE_FALSE|1|EASY|1|Java hỗ trợ đa kế thừa lớp|false
     * FILL_IN_BLANK|3|HARD|1|Từ khóa để kế thừa lớp là ...|extends
     * </pre>
     * Phương án đúng đánh dấu bằng "*" ở đầu. Dòng trống và dòng bắt đầu bằng "#" được bỏ qua.
     *
     * Dòng nào sai thì ghi lại lỗi và nhập tiếp các dòng sau, không dừng cả tệp vì một dòng.
     * Lỗi đọc tệp (không có tệp, không có quyền đọc) là lỗi người dùng phải xử lý nên ném IOException ra ngoài.
     */
    public ImportResult importFile(User actor, Subject subject, Path file) throws IOException {
        Permissions.requireManage(actor, subject);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> errors = new ArrayList<>();
        int imported = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int lineNo = i + 1;
            try {
                String[] f = line.split("\\|", -1);
                if (f.length != 6) {
                    throw new ValidationException("CSV_COLUMNS", "cần 6 trường, có " + f.length);
                }
                QuestionType type = QuestionType.valueOf(f[0].strip());
                QuestionData data = QuestionData.basic(subject, f[4],
                        Integer.parseInt(f[1].strip()), Difficulty.valueOf(f[2].strip()),
                        Double.parseDouble(f[3].strip()));
                data = withAnswers(type, data, f[5]);
                addQuestion(actor, type, data);
                imported++;
            } catch (ValidationException e) {
                errors.add("Dòng " + lineNo + ": " + e.getUserMessage());
            } catch (IllegalArgumentException e) {
                // NumberFormatException là lớp con của IllegalArgumentException, nên một nhánh bắt được cả hai:
                // số chương/điểm không phải số, hoặc tên loại câu hỏi / mức độ gõ sai
                errors.add("Dòng " + lineNo + ": giá trị không hợp lệ (" + e.getMessage() + ")");
            }
        }
        AuditLog.record(actor, "IMPORT_QUESTIONS", file.getFileName() + ": " + imported + " câu, " + errors.size() + " lỗi");
        return new ImportResult(imported, errors);
    }

    private static QuestionData withAnswers(QuestionType type, QuestionData data, String raw) {
        List<String> parts = Arrays.stream(raw.split(";")).map(String::strip).filter(s -> !s.isEmpty()).toList();
        return switch (type) {
            case SINGLE_CHOICE, MULTI_CHOICE -> data.withOptions(parts.stream()
                    .map(p -> p.startsWith("*")
                            ? AnswerOption.of(p.substring(1), true)
                            : AnswerOption.of(p, false))
                    .toList());
            case TRUE_FALSE -> {
                String v = raw.strip().toLowerCase();
                if (!v.equals("true") && !v.equals("false")) {
                    throw new ValidationException("TF_VALUE", "câu đúng/sai cần đáp án true hoặc false");
                }
                yield data.withCorrectValue(Boolean.parseBoolean(v));
            }
            case FILL_IN_BLANK -> data.withAccepted(parts, false);
        };
    }
}
