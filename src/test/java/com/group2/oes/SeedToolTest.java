package com.group2.oes;

import com.group2.oes.app.AppContext;
import com.group2.oes.app.ClientContext;
import com.group2.oes.client.ExamAttempt;
import com.group2.oes.client.SessionEntry;
import com.group2.oes.dao.memory.InMemoryDatabase;
import com.group2.oes.model.Criteria;
import com.group2.oes.model.Difficulty;
import com.group2.oes.model.ExamMatrix;
import com.group2.oes.model.QuestionBank;
import com.group2.oes.model.Role;
import com.group2.oes.model.Student;
import com.group2.oes.model.Subject;
import com.group2.oes.model.User;
import com.group2.oes.model.question.Question;
import com.group2.oes.model.question.QuestionType;
import com.group2.oes.service.ImportResult;
import com.group2.oes.tools.SeedTool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dữ liệu mẫu cho buổi demo phải tạo được trọn vẹn: tệp câu hỏi không có dòng lỗi, ma trận đủ câu, thí sinh vào thi được.
 * Chế độ ngân hàng trống (thuyết trình nhập câu hỏi từ tệp) phải khớp đúng những gì kịch bản cho hiện trên màn hình.
 */
class SeedToolTest {

    @Test
    void dữLiệuMẫuTạoĐủVàThíSinhVàoThiĐượcNgay() {
        InMemoryDatabase db = new InMemoryDatabase(LocalDateTime::now);
        AppContext app = AppContext.inMemory(db);
        ClientContext client = ClientContext.inMemory(db);
        SeedTool.seed(app);

        User admin = app.auth.login("admin", "admin123");
        assertEquals(40, app.auth.searchUsers(admin, null, Role.STUDENT).size(), "đúng 40 sinh viên của lớp thật");
        Map.of("b24dcce098", "Đỗ Phạm Bảo Hoàng", "b24dcce007", "Đỗ Thị Vân Anh",
                "b24dcce273", "Nguyễn Hoàng Trung", "b24dcce070", "Phan Tùng Dương").forEach((username, name) ->
                assertEquals(name, app.users.findByUsername(username).orElseThrow().getFullName(), "thành viên nhóm có trong lớp"));
        assertEquals("Trần Đức Lương", app.users.findByUsername("b24dcce168").orElseThrow().getFullName(),
                "Tab lẫn trong họ tên được gộp thành một dấu cách");
        assertEquals("B24LKCN-S01", ((Student) app.users.findByUsername("b24lkcn-s01").orElseThrow()).getStudentCode(),
                "mã liên kết có gạch nối vẫn dùng được");

        User gv = app.auth.login("gv.viet", "gv123456");
        Subject oop = app.subjects.findByCode("INT1332").orElseThrow();
        List<Question> questions = app.bankService.loadBank(gv, oop).findBy(Criteria.any());
        assertEquals(60, questions.size());
        assertEquals(Set.of(QuestionType.values()), questions.stream().map(Question::getType).collect(Collectors.toSet()));
        assertEquals(Set.of(Difficulty.values()), questions.stream().map(Question::getDifficulty).collect(Collectors.toSet()));

        User member = client.auth.login("b24dcce098", "sv123456");
        SessionEntry entry = client.exams.overview(member).get(0);
        assertTrue(entry.canEnter(), "ca thi mở ngay khi tạo dữ liệu");
        ExamAttempt attempt = client.exams.enter(member, entry.session().sessionId());
        assertEquals(20, attempt.paper().size());
        assertEquals(10.0, attempt.paper().totalPoint());
    }

    @Test
    void ngânHàngTrốngRồiNhậpTệpDemoThìĐược60CâuVàBáoRõHaiDòngSai() throws IOException {
        InMemoryDatabase db = new InMemoryDatabase(LocalDateTime::now);
        AppContext app = AppContext.inMemory(db);
        ClientContext client = ClientContext.inMemory(db);
        SeedTool.seed(app, false);

        User gv = app.auth.login("gv.viet", "gv123456");
        Subject oop = app.subjects.findByCode("INT1332").orElseThrow();
        assertTrue(app.bankService.loadBank(gv, oop).findBy(Criteria.any()).isEmpty(), "ngân hàng câu hỏi trống");
        assertTrue(client.exams.overview(client.auth.login("b24dcce098", "sv123456")).isEmpty(), "chưa có ca thi");

        // Đúng tệp dùng khi thuyết trình (docs/KICH-BAN-THUYET-TRINH.md, phần B2)
        ImportResult r = app.bankService.importFile(gv, oop, Path.of("docs/demo/cau-hoi-demo.txt"));
        assertEquals(60, r.imported());
        assertEquals(2, r.errors().size());
        assertTrue(r.errors().get(0).contains("phải có đúng 1 phương án đúng, hiện có 2"), r.errors().get(0));
        assertTrue(r.errors().get(1).contains("phải có ít nhất 1 phương án đúng"), r.errors().get(1));

        // Ma trận trong kịch bản: xin 10 câu khó chương 3 thì báo thiếu đúng câu chữ người thuyết trình đọc
        QuestionBank bank = app.bankService.loadBank(gv, oop);
        assertEquals(List.of("chương 3 mức Khó: cần 10, có 7"), bank.findShortages(new ExamMatrix()
                .add(1, Difficulty.EASY, 3).add(2, Difficulty.MEDIUM, 3).add(3, Difficulty.HARD, 10)));
    }
}
