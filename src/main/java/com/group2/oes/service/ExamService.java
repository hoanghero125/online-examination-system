package com.group2.oes.service;

import com.group2.oes.dao.ExamDao;
import com.group2.oes.exception.ValidationException;
import com.group2.oes.model.Exam;
import com.group2.oes.model.ExamMatrix;
import com.group2.oes.model.MultiChoicePolicy;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;
import com.group2.oes.model.question.Question;
import com.group2.oes.util.AuditLog;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ExamService {

    private final ExamDao examDao;

    public ExamService(ExamDao examDao) {
        this.examDao = examDao;
    }

    /**
     * Tạo đề thủ công. picks giữ thứ tự câu trong đề, nên phía gọi nên dùng LinkedHashMap:
     * khóa là mã câu hỏi, giá trị là điểm của câu đó trong đề.
     */
    public Exam createManual(User actor, QuestionBank bank, String title, MultiChoicePolicy policy,
                             Map<Integer, Double> picks) {
        Permissions.requireManage(actor, bank.getSubject());
        ValidationException.require(!picks.isEmpty(), "EXAM_EMPTY", "Chưa chọn câu hỏi nào cho đề.");
        Exam exam = Exam.draft(title, bank.getSubject(), policy);
        picks.forEach((questionId, point) -> exam.addItem(bank.get(questionId), point));
        exam.validateForUse();
        Exam saved = examDao.insert(exam);
        AuditLog.record(actor, "CREATE_EXAM", saved.getExamId() + " " + title);
        return saved;
    }

    /** Các đề của một môn, cho màn hình danh sách đề. */
    public List<Exam> list(User actor, Subject subject) {
        Permissions.requireManage(actor, subject);
        return examDao.findBySubject(subject);
    }

    /** BR-09: đề đã khóa muốn sửa thì tạo phiên bản mới (bản chép chưa khóa, số phiên bản tăng 1). */
    public Exam newVersion(User actor, Exam exam) {
        Permissions.requireManage(actor, exam.getSubject());
        Exam saved = examDao.insert(exam.newVersion());
        AuditLog.record(actor, "NEW_EXAM_VERSION", saved.getExamId() + " " + saved.getTitle() + " v" + saved.getVersion());
        return saved;
    }

    /** Sinh đề tự động theo ma trận, mọi câu cùng một mức điểm. */
    public Exam generate(User actor, QuestionBank bank, String title, MultiChoicePolicy policy,
                         ExamMatrix matrix, double pointEach, Random random) {
        Permissions.requireManage(actor, bank.getSubject());
        List<Question> picked = bank.pick(matrix, random);   // ném ValidationException nếu thiếu câu
        Exam exam = Exam.draft(title, bank.getSubject(), policy);
        picked.forEach(q -> exam.addItem(q, pointEach));
        Exam saved = examDao.insert(exam);
        AuditLog.record(actor, "GENERATE_EXAM", saved.getExamId() + " " + title + " (" + picked.size() + " câu)");
        return saved;
    }
}
