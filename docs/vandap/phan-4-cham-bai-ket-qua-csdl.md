# Phần 4 – Chấm bài, kết quả, CSDL và bảo mật

> Người phụ trách: **Phan Tùng Dương (B24DCCE070)** · Đọc kèm: `docs/PHAN-CONG.md` (câu hỏi chung)

Quy ước trong tài liệu: `oes/` = `src/main/java/com/group2/oes/`, `fxml/` = `src/main/resources/fxml/`,
`test/` = `src/test/java/com/group2/oes/`. Mọi đoạn mã trích nguyên văn từ tệp ghi ngay phía trên.
Các câu đã có trong "Câu hỏi chung" của `PHAN-CONG.md` (ba tầng, DIP, `BackgroundTasks`, SQL injection...) chỉ được
nhắc lại ngắn, không giải thích lại.

---

## 0. Nói trong 1 phút

"Phần em là **chấm bài, kết quả và cơ sở dữ liệu**.

Máy thí sinh **không chấm bài**, vì muốn chấm thì phải đọc được đáp án. Máy thí sinh chỉ ghi câu trả lời rồi đánh dấu
bài là `SUBMITTED`. Việc chấm chạy trên máy giảng viên: lớp `GradingWorker` cứ 3 giây một lượt làm hai việc. Một là
đóng mọi bài của ca đã quá giờ bằng một câu `UPDATE`. Hai là tìm bài `SUBMITTED`, chấm bằng `SubmissionGrader`, ghi điểm
và chuyển sang `GRADED`. Việc hẹn giờ do `GradingScheduler` lo; `GradingDaemon` là bản không giao diện chạy trên máy chủ.

Chấm từng câu dùng **mẫu Strategy**: interface `GradingStrategy` có ba cài đặt `ExactMatchGrading`,
`PartialMatchGrading`, `TextMatchGrading`. Đề thi chọn chiến lược theo loại câu và cấu hình `ALL_OR_NOTHING`/`PARTIAL`.

Giảng viên xem kết quả qua `StatisticsService`: bảng điểm, bài vi phạm tô đỏ, phổ điểm, câu khó nhất, xuất CSV có BOM
để Excel đọc đúng tiếng Việt.

Em cũng phụ trách CSDL: 12 bảng trong `schema.sql`, và `security.sql` với hai tài khoản MySQL. `oes_staff` toàn quyền.
`oes_client` của máy thí sinh chỉ đọc qua 4 khung nhìn – đề không có đáp án, đáp án chỉ hiện trong `v_review` sau khi
ca đã đóng – chỉ được ghi một số cột, và có 4 trigger chặn
những gì quyền theo cột không chặn được. Nhờ vậy, dù ai sửa mã ứng dụng thi, cũng không đọc được đáp án và không
tự sửa được điểm – đó là NFR-04."

---

## 1. Phạm vi

### Yêu cầu chức năng (mã từ bảng truy vết trong `docs/PLAN.md` và `docs/PROGRESS.md`)

| Mã | Nội dung | Phần 4 làm gì |
|---|---|---|
| FR-26 | Tự nộp khi ca đóng | `GradingDao.closeExpired` (một câu `UPDATE`), gọi từ `GradingWorker.runOnce`; lặp bằng `GradingScheduler` |
| FR-30 | Giảng viên xem vi phạm | Cột "Vi phạm", dòng tô đỏ trong `SessionResultsController`; `ResultRow.terminatedByViolation` |
| FR-31 | Chấm tự động | `SubmissionGrader.grade`, `GradingWorker.runOnce` |
| FR-32 | Nhiều chiến lược chấm | `GradingStrategy` + 3 lớp cài đặt (việc chọn chiến lược nằm trong `Exam` – phần 2) |
| FR-33 | Thí sinh xem kết quả, đáp án | Màn hình `ResultController` (chờ chấm, hiện điểm); dữ liệu đáp án từ khung nhìn `v_review` |
| FR-34 | Thống kê | `ResultStatistics`, `StatisticsService.statsFor` |
| FR-35 | Xuất CSV | `StatisticsService.exportCsv`, `ResultStatistics.toCsvLines` |

### Yêu cầu phi chức năng

| Mã | Nội dung | Phần 4 làm gì |
|---|---|---|
| NFR-01 | 60 máy, lưu bài < 2 giây | Kiểm thử tải `tools/LoadSimulator` (`docs/load-test.md`) |
| NFR-02 | Chấm bài 50 câu < 3 giây | `SubmissionGraderTest.chấmBàiNămMươiCâuDướiBaGiây`; số đo 60 bài / 2 162 ms |
| NFR-04 | Máy thí sinh dùng tài khoản riêng, chỉ đọc khung nhìn không có đáp án | Toàn bộ `db/security.sql`; chấm chỉ ở máy giảng viên |
| NFR-05 | Nhật ký | Máy chấm ghi `AUTO_SUBMIT`, `AUTO_GRADE`, `GRADED_VIOLATION`; xuất CSV ghi `EXPORT_CSV` |
| NFR-10 | Thêm cách chấm không sửa lớp cũ | Mẫu Strategy |
| NFR-12 | Dữ liệu tập trung trên MySQL, có script tạo bảng | `db/schema.sql`, `db/security.sql` |

### Quy tắc nghiệp vụ (BR) và use case

- **BR-03** (giờ kết thúc = giờ đóng ca): `closeExpired` ghi `submitted_at = es.close_time`.
- **BR-04** (ca đóng thì khóa bài, từ chối câu trả lời gửi sau): `closeExpired` + trigger `trg_answer_client_insert`/`_update`.
- **BR-06** (bài vi phạm vẫn chấm nhưng đánh dấu riêng): `SubmissionGrader` chấm như thường; dòng tô đỏ, cột CSV `ket_thuc_do_vi_pham`.
- **BR-07, BR-08** (một đáp án không điểm âm; nhiều đáp án theo cấu hình): `ExactMatchGrading`, `PartialMatchGrading`.
- **BR-10** (chỉ giảng viên phụ trách môn / quản trị viên xem kết quả): `Permissions.requireManage` trong mọi hàm của `StatisticsService`.
- **UC-16** (nộp bài và chấm tự động) – nửa "chấm"; **UC-17** (xem kết quả, thống kê); **UC-18** (xuất CSV).

### Phần 4 sở hữu gì

Model `Submission`, `SubmissionStatus`, `Answer`, `EndReason`, `ResultRow`, `ResultStatistics`, `SessionSummary`;
toàn bộ gói `grading`; service `GradingWorker`, `GradingScheduler`, `StatisticsService`; DAO `GradingDao`,
`SubmissionDao` và các cài đặt JDBC/bộ nhớ; tầng dữ liệu chung `ConnectionManager`, `JdbcErrors`, `InMemoryDatabase`,
`ManualClock`, `SubmissionRow`; `GradingDaemon`; màn hình kết quả hai phía; **toàn bộ** `db/schema.sql` và `db/security.sql`.

---

## 2. Ranh giới với các phần khác

| Chỗ nối | Phía phần khác | Phía phần 4 | Ghi chú |
|---|---|---|---|
| Phần 3 nộp bài → phần 4 chấm | `StudentExamService.submit` → `StudentExamDao.markSubmitted` (`UPDATE submission SET status = 'SUBMITTED' ... WHERE id = ? AND status = 'IN_PROGRESS'`) | `GradingDao.findSubmittedIds` đọc `WHERE status = 'SUBMITTED'` | Hai phía không gọi nhau trực tiếp, chỉ gặp nhau ở cột `submission.status`. |
| Phần 3 xem lại bài | `JdbcStudentExamDao.loadReview` đọc `answer.earned_point` và khung nhìn `v_review` | `JdbcGradingDao.saveGrades` ghi `earned_point`; `security.sql` định nghĩa `v_review` | Phần 4 quyết định *khi nào* đáp án được lộ ra (điều kiện trong khung nhìn). |
| Phần 3 chờ điểm | `StudentExamService.refresh` (đọc `total_score` chỉ khi `GRADED`) | `ResultController.poll` gọi hàm này mỗi 2 giây | Màn hình thuộc phần 4, hàm đọc thuộc phần 3. |
| Phần 3 vi phạm | `ProctoringService.report` gọi `StudentExamService.submit(actor, attempt, EndReason.VIOLATION)` → `StudentExamDao.markSubmitted` | `EndReason.VIOLATION`, `Submission.isTerminatedByViolation`, nhật ký `GRADED_VIOLATION` | Bài vi phạm đi đúng đường chấm như bài thường. |
| Phần 3 trigger bị chặn | `StudentExamService.saveAnswer` nhận `SessionRuleException` | `JdbcErrors.translate` đổi SQLSTATE 45000 thành `SessionRuleException(SUBMISSION_LOCKED)` | |
| Phần 2 chọn chiến lược | `Exam` (trường `Map<QuestionType, GradingStrategy> strategies = new EnumMap<>(QuestionType.class)`, constructor điền chiến lược cho từng loại), `Exam.strategyFor`, `MultiChoicePolicy` | `SubmissionGrader.grade` gọi `exam.strategyFor(item.getQuestion())` | Lớp chiến lược của phần 4, việc đăng ký chiến lược ở phần 2. |
| Phần 2 công thức tỷ lệ | `MultiChoiceQuestion.correctRatio`, `Question.isCorrect`, `ExamItem.grade` | `PartialMatchGrading.grade` gọi `question.correctRatio(response)` | Câu hỏi tự biết mình đúng bao nhiêu; chiến lược quyết định quy ra điểm thế nào. |
| Phần 1 phân quyền | `Permissions.requireManage(actor, subject)` | `StatisticsService.statsFor`, `summarize`, `listResults` | BR-10. |
| Phần 1 lắp ráp | `AppContext` tạo `new GradingWorker(grading, sessions, new SubmissionGrader())` và `new StatisticsService(submissions)` | Các lớp phần 4 chỉ nhận interface DAO | DIP. |
| Phần 1 khởi động | `StaffApp.onConnected` tạo và `start()` một `GradingScheduler`; `StaffApp.gradeNow` | `GradingScheduler.start`, `runNow`, `close` | |
| Phần 1 giờ máy chủ | `ExamSessionDao.serverTime` (`SELECT NOW()`, cài trong `JdbcExamSessionDao`) | `GradingWorker.runOnce` lấy `now` từ đây | Quyết định số 6. |
| Phần 1 nhật ký | `AuditLog.record(actor, action, target)` | `GradingWorker`, `StatisticsService.exportCsv` | `actor = null` ghi thành `-`. |
| Phần 3 danh sách ca | `StaffSessionsController` gọi `statistics.summarize`, nút "Xem kết quả" mở `session-results.fxml` | `StatisticsService.summarize`, `SessionSummary`, `SessionResultsController.init` | |

---

## 3. Thứ tự đọc mã

1. `db/schema.sql` – 12 bảng; đọc kỹ `submission` (trạng thái, `UNIQUE (session_id, student_id)`) và `answer`.
2. `db/security.sql` – 4 khung nhìn, thủ tục, 2 tài khoản, GRANT theo cột, 4 trigger.
3. `oes/model/SubmissionStatus.java`, `EndReason.java` – hai enum: vòng đời bài và lý do kết thúc.
4. `oes/model/Answer.java` – một câu trả lời, tự chấm mình bằng chiến lược được truyền vào.
5. `oes/model/Submission.java` – bài làm phía giảng viên: dữ liệu + hai phép chuyển trạng thái.
6. `oes/grading/GradingStrategy.java` – interface một hàm `grade`.
7. `oes/grading/ExactMatchGrading.java`, `PartialMatchGrading.java`, `TextMatchGrading.java` – ba cách chấm.
8. (phần 2) `oes/model/Exam.java` hàm khởi tạo và `strategyFor`; `oes/model/question/MultiChoiceQuestion.java` hàm `correctRatio` – hiểu công thức PARTIAL.
9. `oes/grading/SubmissionGrader.java` – chấm cả bài, cô lập lỗi từng câu.
10. `oes/dao/GradingDao.java` – bốn thao tác của máy chấm (đọc javadoc trước khi đọc SQL).
11. `oes/dao/jdbc/ConnectionManager.java` – mở kết nối có timeout, `execute`, `inTransaction`.
12. `oes/dao/jdbc/JdbcErrors.java` – dịch `SQLException` thành ngoại lệ của hệ thống.
13. `oes/dao/jdbc/JdbcGradingDao.java` – SQL thật của máy chấm.
14. `oes/dao/SubmissionDao.java`, `oes/dao/jdbc/JdbcSubmissionDao.java` – đọc bài làm kèm câu trả lời (dùng chung với máy chấm).
15. `oes/service/GradingWorker.java` – một lượt chấm, `RunResult`, xử lý lỗi.
16. `oes/service/GradingScheduler.java` – lặp lại lượt chấm, cái bẫy của `ScheduledExecutorService`.
17. `oes/app/GradingDaemon.java` – máy chấm không giao diện.
18. `oes/model/ResultRow.java`, `ResultStatistics.java`, `SessionSummary.java` – dữ liệu cho màn hình kết quả.
19. `oes/service/StatisticsService.java` – kiểm quyền, thống kê, xuất CSV có BOM.
20. `oes/ui/controller/SessionResultsController.java` + `fxml/session-results.fxml` – màn hình giảng viên.
21. `oes/ui/controller/ResultController.java` + `fxml/result.fxml` – màn hình thí sinh chờ điểm.
22. `oes/dao/memory/InMemoryDatabase.java`, `SubmissionRow.java`, `ManualClock.java`, `MemoryGradingDao.java`, `MemorySubmissionDao.java` – CSDL giả trong bộ nhớ.
23. `test/SubmissionGraderTest`, `GradingTest`, `GradingSchedulerTest` – kiểm thử không cần MySQL.
24. `test/JdbcSecurityTest`, `JdbcIntegrationTest` – kiểm thử với MySQL thật.
25. `test/tools/LoadSimulator.java` + `docs/load-test.md` – kiểm thử tải.

---

## 4. Kiến thức Java/OOP/SQL nền

### 4.1. Interface và mẫu Strategy

*Interface* là một "hợp đồng": chỉ khai báo hàm, không có cài đặt. *Strategy* là mẫu thiết kế: gom mỗi cách làm
một việc (ở đây: cách tính điểm) vào một lớp riêng cùng cài một interface, nơi dùng chỉ biết interface nên đổi cách
làm bằng cách đổi đối tượng, không cần `if/else`.

`oes/grading/GradingStrategy.java`
```java
public interface GradingStrategy {

    /**
     * @param maxPoint điểm tối đa của câu này trong đề (lấy từ ExamItem, không phải điểm mặc định)
     * @return điểm đạt được, luôn nằm trong [0, maxPoint]
     */
    double grade(Question question, String response, double maxPoint);
}
```

Nơi dùng (`Answer.grade`) chỉ gọi `strategy.grade(...)` qua `ExamItem.grade`; nó không biết đang cầm
`ExactMatchGrading` hay `PartialMatchGrading`. Đó là **đa hình**.

### 4.2. Enum có dữ liệu

Enum Java là lớp: có trường, constructor, phương thức. `EndReason` mang sẵn tên tiếng Việt để mọi màn hình dùng chung:

`oes/model/EndReason.java`
```java
public enum EndReason {
    MANUAL("Thí sinh tự nộp"),
    TIME_OUT("Hết giờ, tự động nộp"),
    VIOLATION("Kết thúc do vi phạm quy chế");   // rời cửa sổ làm bài đủ 5 lần
```

Tên hằng (`MANUAL`, `TIME_OUT`, `VIOLATION`) trùng đúng giá trị `ENUM(...)` của cột `submission.end_reason`, nên đọc
lên chỉ cần `EndReason.valueOf(rs.getString("end_reason"))`.

### 4.3. `record`

`ResultRow`, `SessionSummary`, `GradingWorker.RunResult` là record: lớp dữ liệu bất biến, tự có constructor,
getter (`score()`, `closed()`...), `equals`, `hashCode`. Dùng cho dữ liệu "chỉ để đọc" chuyển từ service lên giao diện.
Record vẫn thêm phương thức được, ví dụ `ResultRow.terminatedByViolation()`, `RunResult.hasWork()`.

### 4.4. Collection: `TreeMap`, `EnumMap`, Stream

- `TreeMap` giữ khóa đã sắp xếp. `Submission.answers` dùng `TreeMap` để duyệt câu theo đúng thứ tự 1, 2, 3...;
  `ResultStatistics.histogram` dùng `TreeMap` để phổ điểm tự xếp tăng dần.
- `EnumMap` (trong `Exam`, phần 2): Map có khóa là enum, nhanh và gọn hơn `HashMap`.
- Stream `groupingBy(..., TreeMap::new, counting())` đếm số bài theo khoảng điểm trong `ResultStatistics`.

### 4.5. Luồng nền: `ScheduledExecutorService`, luồng daemon, `volatile`

`ScheduledExecutorService` là bộ chạy việc theo lịch của Java. `GradingScheduler` dùng
`scheduleWithFixedDelay(việc, 0, chu kỳ, đơn vị)`: chạy ngay lần đầu, sau đó **mỗi lần chạy xong** mới đếm 3 giây.
(Khác `scheduleAtFixedRate` đếm từ lúc bắt đầu: nếu một lượt chậm hơn 3 giây thì các lượt sẽ dồn nhau.)

Luồng *daemon* là luồng phụ: khi mọi luồng thường kết thúc, JVM thoát mà không chờ nó. Đặt `setDaemon(true)` để
đóng cửa sổ StaffApp là chương trình thoát, không bị luồng máy chấm giữ lại.

`volatile` trên `GradingScheduler.failing`: hai luồng (luồng hẹn giờ và luồng nền của nút "Làm mới") cùng đọc ghi
biến này; `volatile` bảo đảm luồng này ghi thì luồng kia thấy ngay giá trị mới.

### 4.6. Functional interface và lambda trong JDBC

`ConnectionManager.SqlWork<T>` giống `Function` của JDK (nhận `Connection`, trả `T`) nhưng được phép ném `SQLException`
(`Function` của JDK không cho ném checked exception). Nhờ vậy mỗi DAO chỉ viết phần SQL dưới dạng lambda;
phần mở/đóng kết nối và dịch lỗi viết một lần trong `ConnectionManager`.

`oes/dao/jdbc/ConnectionManager.java`
```java
    /** Giống java.util.function.Function nhưng được phép ném SQLException. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }
```

### 4.7. try-with-resources và giao dịch JDBC

`try (Connection c = open()) { ... }`: hết khối `try` thì `c.close()` được gọi tự động, kể cả khi có ngoại lệ.
*Giao dịch* (transaction): nhiều câu lệnh "hoặc cùng thành công, hoặc như chưa chạy câu nào".
JDBC mặc định *auto-commit* (mỗi câu tự ghi ngay); muốn giao dịch thì tắt nó, cuối cùng `commit()`, lỗi thì `rollback()`.

`oes/dao/jdbc/ConnectionManager.java`
```java
    public <T> T inTransaction(SqlWork<T> work) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(c, e);
                throw e;
            }
        } catch (SQLException e) {
            throw JdbcErrors.translate(e);
        }
    }
```

`rollbackQuietly` gắn lỗi rollback (nếu có) vào lỗi gốc bằng `addSuppressed`, để lỗi gốc không bị che mất.

### 4.8. Các câu SQL đặc biệt trong phần 4

- `UPDATE ... JOIN ...`: sửa bảng này dựa trên điều kiện ở bảng khác (MySQL cho phép). Dùng trong `closeExpired`.
- `INSERT ... ON DUPLICATE KEY UPDATE`: chèn; nếu trùng khóa chính thì sửa dòng đã có. Dùng trong `saveGrades`.
- *Batch* JDBC: `addBatch()` gom nhiều lần chạy một câu lệnh, `executeBatch()` gửi một lần.
- *UPDATE có điều kiện trạng thái* (`... WHERE id = ? AND status = 'SUBMITTED'`) rồi xem `executeUpdate()` trả về
  bao nhiêu dòng: 0 dòng nghĩa là có người khác đã đổi trạng thái trước. Đây là cách khóa lạc quan
  (*optimistic locking*) đơn giản, không cần `SELECT ... FOR UPDATE`.

### 4.9. Khung nhìn (view), GRANT theo cột, trigger, thủ tục lưu trữ

- **Khung nhìn** là một câu `SELECT` được đặt tên, dùng như một bảng. Mặc định MySQL tạo khung nhìn với
  `SQL SECURITY DEFINER`: khi chạy, nó dùng quyền của **người tạo** (root). Vì vậy cấp cho `oes_client` quyền đọc
  khung nhìn là đủ, không cần (và không được) cấp quyền đọc bảng gốc `question`, `answer_option`.
- **GRANT theo cột**: `GRANT UPDATE (status, submitted_at, end_reason, violation_count) ON submission TO ...` –
  chỉ được sửa đúng các cột này. Câu `UPDATE submission SET total_score = 10` bị MySQL từ chối trước khi chạy.
- **Trigger** `BEFORE INSERT/UPDATE`: đoạn mã MySQL tự chạy trước mỗi dòng được ghi; `NEW` là giá trị sắp ghi,
  `OLD` là giá trị cũ. `SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '...'` ném lỗi, câu lệnh bị hủy.
- **Thủ tục lưu trữ** (stored procedure) với `SQL SECURITY DEFINER`: chạy bằng quyền người tạo, người gọi chỉ cần
  quyền `EXECUTE`.
- `DELIMITER //` là lệnh của **công cụ dòng lệnh `mysql`**, không phải SQL: đổi dấu kết thúc câu lệnh để các dấu `;`
  bên trong thân trigger/thủ tục không cắt câu. Vì vậy `security.sql` phải chạy bằng `mysql ... < db/security.sql`,
  không chạy qua JDBC được.

### 4.10. Ràng buộc trong bảng

`PRIMARY KEY`, `UNIQUE`, `FOREIGN KEY ... ON DELETE CASCADE` (xóa dòng cha thì xóa luôn dòng con), `INDEX`
(tăng tốc tìm kiếm), `CHECK (close_time > open_time)` (MySQL 8.0.16 trở lên mới thực sự kiểm tra `CHECK`).

### 4.11. JavaFX dùng trong hai màn hình

- `Timeline` + `KeyFrame`: đồng hồ của JavaFX, gọi hàm trên luồng giao diện sau mỗi khoảng thời gian; `setCycleCount`
  đặt số lần lặp. `ResultController` dùng để hỏi lại điểm.
- `TableView.setRowFactory`: tự tạo từng dòng của bảng, cho phép thêm lớp CSS (`row-violation`) theo dữ liệu dòng.
- `FileChooser.showSaveDialog`: hộp thoại chọn nơi lưu tệp.

### 4.12. BOM UTF-8

BOM (*byte order mark*) là ký tự U+FEFF đặt đầu tệp; viết ở UTF-8 là ba byte `EF BB BF`. Excel trên Windows mở tệp
CSV mà không có BOM thì đoán là bảng mã ANSI cũ, tên "Nguyễn" hiện thành ký tự lỗi. Có BOM thì Excel biết là UTF-8.

---

## 5. Từng lớp chính

### 5.1. `SubmissionStatus` – vòng đời bài làm

`oes/model/SubmissionStatus.java`
```java
public enum SubmissionStatus {
    IN_PROGRESS, SUBMITTED, GRADED
}
```

Ba trạng thái, đi một chiều: `IN_PROGRESS → SUBMITTED → GRADED`. Ai được chuyển:

| Chuyển | Ai làm | Ở đâu |
|---|---|---|
| tạo `IN_PROGRESS` | máy thí sinh | `StudentExamDao.createAttempt` (phần 3); trigger `trg_submission_client_insert` bắt buộc trạng thái này |
| `IN_PROGRESS → SUBMITTED` (MANUAL, VIOLATION) | máy thí sinh | `StudentExamDao.markSubmitted` (phần 3) |
| `IN_PROGRESS → SUBMITTED` (TIME_OUT) | máy chấm | `GradingDao.closeExpired` |
| `SUBMITTED → GRADED` | **chỉ** máy chấm | `Submission.markGraded` + `GradingDao.saveGrades`; trigger chặn `oes_client` đặt `GRADED` |

**Vì sao có trạng thái trung gian `SUBMITTED`?** Vì nộp và chấm nay xảy ra trên hai máy khác nhau. `SUBMITTED` là
"hàng đợi" giữa chúng: máy thí sinh bỏ bài vào, máy chấm lấy ra. Nếu máy chấm đang tắt, bài nằm chờ ở `SUBMITTED`,
không mất (tình huống "StaffApp không chạy khi thí sinh nộp bài" trong PLAN Phase 7).

### 5.2. `EndReason` – lý do kết thúc

Ba giá trị (trích ở 4.2). `displayName()` trả câu tiếng Việt để hiện ở bảng điểm và màn hình kết quả.
`VIOLATION` là cơ sở của BR-06 (đánh dấu riêng). Cột `end_reason` cho phép `NULL`: bài đang làm chưa có lý do.

### 5.3. `Answer` – một câu trả lời

**Dữ liệu:** `examItem` (câu trong đề, có điểm tối đa và câu hỏi), `response` (chuỗi thí sinh trả lời, `final`),
`earnedPoint`, `graded`, `gradingError`.

`oes/model/Answer.java`
```java
    public void grade(GradingStrategy strategy) {
        this.earnedPoint = examItem.grade(response, strategy);
        this.graded = true;
        this.gradingError = false;
    }

    /** Câu này chấm lỗi: cho 0 điểm tạm thời và đánh dấu để giảng viên rà lại. */
    public void markGradingError() {
        this.earnedPoint = 0;
        this.graded = false;
        this.gradingError = true;
    }
```

- `response` là `final`: từ M5 không còn ai sửa câu trả lời sau khi dựng (ghi câu trả lời là việc của máy thí sinh,
  đi thẳng xuống CSDL).
- `markGradingError` là `public` vì lớp gọi (`SubmissionGrader`) nằm ở gói khác (`grading`) – DESIGN-CHANGES Phase 1 mục 1.
- `restoreResult(point)`: DAO dựng lại câu đã chấm từ CSDL.
- `isFullMark()`: `graded && Math.abs(earnedPoint - examItem.getPoint()) < 1e-9`. So số thực bằng sai số nhỏ
  thay vì `==`, vì `double` có sai số làm tròn.
- `isAnswered()`: `response != null && !response.isBlank()`.

**Vì sao `Answer` nhận chiến lược từ ngoài?** `Answer` không tự chọn cách chấm; cách chấm là cấu hình của **đề**
(cùng một câu hỏi nhiều đáp án có thể chấm toàn phần ở đề A, theo tỷ lệ ở đề B). Đây là quyết định từ M2: "tách
`GradingStrategy` khỏi `Question`".

### 5.4. `Submission` – bài làm phía giảng viên

**Vai trò:** giữ dữ liệu một bài làm và chỉ cho phép hai phép chuyển trạng thái hợp lệ. Không chấm, không ghi
câu trả lời, không đếm vi phạm (SRP – xem mục 9).

**Dữ liệu:** `submissionId`, `student`, `session`, `startedAt`, `submittedAt`, `status`, `endReason`, `totalScore`,
`violationCount` (`final` – phía giảng viên không bao giờ sửa), và `answers` (`Map<Integer, Answer>` cài bằng `TreeMap`) theo số thứ tự câu trong đề gốc.

`oes/model/Submission.java`
```java
    public boolean markSubmitted(EndReason reason, LocalDateTime now) {
        Objects.requireNonNull(reason);
        if (isLocked()) {
            return false;
        }
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = now;
        this.endReason = reason;
        return true;
    }

    /** SUBMITTED -> GRADED. Chỉ SubmissionGrader gọi, sau khi đã chấm từng câu. */
    public void markGraded(double total) {
        if (status != SubmissionStatus.SUBMITTED) {
            throw new IllegalStateException("Chỉ chấm được bài ở trạng thái SUBMITTED, hiện là " + status);
        }
        this.totalScore = total;
        this.status = SubmissionStatus.GRADED;
    }
```

**Bất biến và vì sao:**
- `markSubmitted` trả `false` (không ném lỗi) khi bài đã khóa: nộp lần hai bị bỏ qua, giữ kết quả lần đầu
  (UC-16 luồng ngoại lệ 3a). Trả `boolean` vì đây là tình huống bình thường (hai máy cùng nộp), không phải lỗi.
- `markGraded` **ném** `IllegalStateException` nếu chưa `SUBMITTED`: chấm một bài đang làm là lỗi lập trình,
  phải lộ ra ngay. Kiểm thử `SubmissionGraderTest.chỉChấmĐượcBàiĐãNộp`.
- `answerFor(item)` dùng `computeIfAbsent`: câu thí sinh bỏ trống không có dòng `answer`, ta tạo `Answer` rỗng để
  câu đó vẫn được chấm (0 điểm) và vẫn được thống kê.
- `scoreOn10()` kiểm tra tổng điểm đề > 0 (mã lỗi `EXAM_ZERO_POINT`) – kiểm tra phòng hờ, BR-11 đã chặn từ lúc tạo đề.
- `assignId` chỉ gán một lần (quy ước chung của dự án).
- `getAnswers()` trả `Collections.unmodifiableCollection(...)`: bên ngoài không sửa được danh sách.

**Hai constructor:** một cho bài mới (`IN_PROGRESS`, mã 0), một để DAO dựng lại bài đã có. Constructor dùng
`Objects.requireNonNull` cho các trường bắt buộc.

### 5.5. Ba chiến lược chấm

`oes/grading/ExactMatchGrading.java`
```java
public class ExactMatchGrading implements GradingStrategy {

    @Override
    public double grade(Question question, String response, double maxPoint) {
        return question.isCorrect(response) ? maxPoint : 0.0;
    }
}
```
Dùng cho câu một đáp án, đúng/sai, và câu nhiều đáp án khi đề chọn `ALL_OR_NOTHING`. Đúng hết thì trọn điểm, còn lại 0
– không bao giờ âm (BR-07). Câu hỏi tự biết thế nào là đúng (`isCorrect` của từng lớp con – phần 2).

`oes/grading/PartialMatchGrading.java`
```java
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
```

Công thức tỷ lệ nằm ở câu hỏi (phần 2), trích để hiểu đầy đủ:

`oes/model/question/MultiChoiceQuestion.java`
```java
    /**
     * Tỷ lệ đúng = (số phương án đúng đã chọn - số phương án sai đã chọn) / tổng số phương án đúng,
     * chặn dưới ở 0. Trừ điểm phần chọn sai để thí sinh không thể chọn hết mọi phương án mà vẫn có điểm.
     */
    @Override
    public double correctRatio(String response) {
        Set<Integer> chosen = parse(response);
        Set<Integer> correct = correctIds();

        Set<Integer> hit = new HashSet<>(chosen);
        hit.retainAll(correct);              // giao: chọn đúng
        Set<Integer> wrong = new HashSet<>(chosen);
        wrong.removeAll(correct);            // hiệu: chọn sai

        double ratio = (double) (hit.size() - wrong.size()) / correct.size();
        return Math.max(0.0, ratio);
    }
```

**Ví dụ số** – câu `Fixtures.multi` trong kiểm thử: phương án 21, 22 đúng; 23, 24 sai; câu được 4 điểm trong đề.

| Thí sinh chọn | đúng đã chọn | sai đã chọn | tỷ lệ | `PARTIAL` | `ALL_OR_NOTHING` |
|---|---|---|---|---|---|
| `21,22` | 2 | 0 | (2−0)/2 = 1 | 4.0 | 4.0 |
| `21` | 1 | 0 | (1−0)/2 = 0.5 | 2.0 | 0.0 |
| `21,22,23` | 2 | 1 | (2−1)/2 = 0.5 | 2.0 | 0.0 |
| `21,23` | 1 | 1 | (1−1)/2 = 0 | 0.0 | 0.0 |
| `23` | 0 | 1 | (0−1)/2 = −0.5 → chặn về 0 | 0.0 | 0.0 |
| `21,22,23,24` (chọn hết) | 2 | 2 | 0 | 0.0 | 0.0 |
| (bỏ trống) | 0 | 0 | 0 | 0.0 | 0.0 |

Dòng "chọn hết" là lý do phải **trừ phần chọn sai**: nếu chỉ tính "đúng đã chọn / tổng đúng" thì chọn cả bốn được trọn
điểm. Bốn dòng đầu có trong `GradingTest.cùngMộtCâuHaiChiếnLượcChoHaiKếtQuả`.

Ví dụ làm tròn (`GradingTest.điểmTheoTỷLệĐượcLàmTrònHaiChữSố`): 3 phương án đúng, chọn đúng 2, câu 1 điểm →
tỷ lệ 2/3 → `Math.round(0.6666… × 100) / 100` = **0.67**.

Chặn dưới 0 có ở **hai nơi**: `Math.max(0.0, ratio)` trong `correctRatio` và `Math.min(1.0, Math.max(0.0, ratio))`
trong `PartialMatchGrading`. Lớp chiến lược chặn lại lần nữa vì nó không tin lớp câu hỏi (một lớp con mới viết sai
cũng không làm điểm âm hay vượt trần) – đúng như hợp đồng "luôn nằm trong [0, maxPoint]" ghi ở interface.

`oes/grading/TextMatchGrading.java`
```java
    @Override
    public double grade(Question question, String response, double maxPoint) {
        if (!(question instanceof FillInBlankQuestion fill)) {
            throw new ValidationException("WRONG_STRATEGY",
                    "TextMatchGrading chỉ dùng cho câu điền khuyết, nhận được " + question.getType());
        }
        return fill.isCorrect(response) ? maxPoint : 0.0;
    }
```
Dùng *pattern matching* `instanceof` của Java 17 (`question instanceof FillInBlankQuestion fill` vừa kiểm tra kiểu
vừa ép kiểu). Gắn nhầm chiến lược vào loại câu khác là lỗi cấu hình → ném lỗi có mã `WRONG_STRATEGY`
(kiểm thử `GradingTest.textMatchTừChốiCâuKhôngPhảiĐiềnKhuyết`). Việc chuẩn hóa chuỗi (bỏ khoảng trắng thừa, hoa
thường) nằm trong `FillInBlankQuestion.isCorrect` (phần 2).

**Ai chọn chiến lược?** `Exam` (phần 2), một lần trong constructor:

`oes/model/Exam.java`
```java
    // Các chiến lược không giữ trạng thái nên dùng chung một thể hiện cho cả chương trình.
    private static final GradingStrategy EXACT = new ExactMatchGrading();
    private static final GradingStrategy PARTIAL = new PartialMatchGrading();
    private static final GradingStrategy TEXT = new TextMatchGrading();
    // ...
        strategies.put(QuestionType.SINGLE_CHOICE, EXACT);
        strategies.put(QuestionType.TRUE_FALSE, EXACT);
        strategies.put(QuestionType.FILL_IN_BLANK, TEXT);
        strategies.put(QuestionType.MULTI_CHOICE,
                multiChoicePolicy == MultiChoicePolicy.PARTIAL ? PARTIAL : EXACT);
```
Ba lớp chiến lược **không có trường nào** (không giữ trạng thái), nên dùng chung một đối tượng cho mọi đề và mọi luồng
mà không sợ xung đột. `GradingTest.đềChọnChiếnLượcTheoLoạiCâuVàCấuHình` kiểm tra bằng `assertSame`.

### 5.6. `SubmissionGrader` – chấm cả bài

`oes/grading/SubmissionGrader.java`
```java
    public double grade(Submission submission) {
        Exam exam = submission.getSession().getExam();
        double total = 0;
        for (ExamItem item : exam.getItems()) {
            Answer answer = submission.answerFor(item);
            try {
                answer.grade(exam.strategyFor(item.getQuestion()));
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Lỗi chấm câu " + item.getOrder() + " của bài " + submission.getSubmissionId(), e);
                answer.markGradingError();
            }
            total += answer.getEarnedPoint();
        }
        double rounded = Math.round(total * 100.0) / 100.0;
        submission.markGraded(rounded);
        return rounded;
    }
```

**Vì sao thiết kế vậy:**
- Duyệt **các câu của đề** (`exam.getItems()`), không duyệt các câu trả lời: câu bỏ trống cũng được chấm 0 và có mặt
  trong thống kê (`SubmissionGraderTest.câuBỏTrốngĐược0ĐiểmVàVẫnCóCâuTrảLờiĐểThốngKê`).
- `try/catch` **bên trong vòng lặp**: một câu dữ liệu hỏng chỉ mất điểm câu đó, các câu khác vẫn được chấm
  (UC-16: "một câu chấm lỗi không làm hỏng cả bài"; kiểm thử `mộtCâuNémLỗiThìCácCâuCònLạiVẫnĐượcChấm`).
- Làm tròn tổng hai chữ số: cộng nhiều `double` (0.67 + 0.33...) sinh sai số kiểu 0.9999999.
- Lớp này **không truy cập CSDL**, chỉ tính toán – nên kiểm thử thuần trong bộ nhớ và đo được NFR-02 riêng.
- Không có trạng thái → `AppContext` tạo một đối tượng `new SubmissionGrader()` truyền cho `GradingWorker`.

### 5.7. `GradingDao` và `JdbcGradingDao` – SQL của máy chấm

`oes/dao/GradingDao.java` có đúng 4 hàm: `closeExpired(now)`, `findSubmittedIds()`, `load(id)`, `saveGrades(submission)`.
Interface riêng chỉ máy giảng viên có (ISP): `ClientContext` không bao giờ được lắp `GradingDao`.

**Đóng ca hết giờ – một câu lệnh:**

`oes/dao/jdbc/JdbcGradingDao.java`
```java
    /** Giờ nộp ghi bằng giờ đóng ca, không phải lúc máy chấm chạy, để bảng điểm phản ánh đúng BR-03. */
    @Override
    public int closeExpired(LocalDateTime now) {
        return cm.execute(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE submission sb JOIN exam_session es ON es.id = sb.session_id"
                            + " SET sb.status = 'SUBMITTED', sb.submitted_at = es.close_time, sb.end_reason = 'TIME_OUT'"
                            + " WHERE sb.status = 'IN_PROGRESS' AND es.close_time <= ?")) {
                ps.setTimestamp(1, ts(now));
                return ps.executeUpdate();
            }
        });
    }
```
- **Vì sao một câu UPDATE** thay vì "đọc danh sách ca hết giờ rồi sửa từng bài"? Vì giữa lúc đọc và lúc sửa, có thể có
  bài vừa được tạo hoặc vừa được nộp; một câu lệnh thì MySQL tự xử lý nguyên khối, không sót bài nào
  (javadoc của `GradingDao.closeExpired`). Câu lệnh cũng không đụng bài đã `SUBMITTED` (điều kiện `IN_PROGRESS`).
- `submitted_at = es.close_time`: máy chấm có thể chạy trễ (StaffApp mở muộn 10 phút), nhưng giờ nộp vẫn là giờ đóng
  ca – BR-03.
- `now` truyền vào là giờ **máy chủ MySQL** (`GradingWorker` lấy qua `serverTime()`), không phải giờ máy giảng viên.

**Tìm bài chờ chấm:** `"SELECT id FROM submission WHERE status = 'SUBMITTED' ORDER BY submitted_at, id"` – bài nộp
trước chấm trước. Chỉ mục `idx_submission_status` trong `schema.sql` có để câu này nhanh dù chạy 3 giây/lần.

**Nạp bài:** `load(id)` đọc `session_id` của bài, gọi `sessionDao.findById` để dựng `ExamSession` kèm **đề có đáp án**,
rồi dùng chung hàm `JdbcSubmissionDao.query` với màn hình thống kê. Mỗi bài vài truy vấn; DESIGN-CHANGES Phase 1
mục 4 ghi rõ "chưa cần tối ưu" (60 bài chấm trong 2,2 giây theo `load-test.md`).

**Ghi điểm – trong giao dịch, có khóa lạc quan:**

`oes/dao/jdbc/JdbcGradingDao.java`
```java
    @Override
    public boolean saveGrades(Submission s) {
        return cm.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE submission SET status = ?, total_score = ? WHERE id = ? AND status = 'SUBMITTED'")) {
                ps.setString(1, s.getStatus().name());
                ps.setDouble(2, s.getTotalScore());
                ps.setInt(3, s.getSubmissionId());
                if (ps.executeUpdate() == 0) {
                    return false;
                }
            }
            // Chỉ ghi earned_point; câu trả lời là dữ liệu của thí sinh nên không ghi đè.
            // Câu bỏ trống chưa có dòng nào thì thêm dòng mới với response NULL để thống kê đếm đủ số câu.
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO answer(submission_id, item_order, response, earned_point) VALUES (?,?,NULL,?)"
                            + " ON DUPLICATE KEY UPDATE earned_point = ?")) {
```
- **Giao dịch**: tổng điểm và điểm từng câu phải cùng được ghi. Nếu mất mạng giữa chừng, rollback, bài vẫn `SUBMITTED`,
  lượt sau chấm lại – không bao giờ có bài `GRADED` mà thiếu điểm câu.
- **`AND status = 'SUBMITTED'`**: hai máy chấm cùng chạy (hai giảng viên cùng mở StaffApp, hoặc nút "Làm mới" chạy
  đúng lúc lịch 3 giây chạy) thì chỉ một bên ghi được; bên kia nhận 0 dòng, trả `false`, không ghi đè.
  `JdbcIntegrationTest.nộpVàChấmQuaCSDL` kiểm tra: `assertFalse(app.grading.saveGrades(reloaded), "bài đã chấm không bị ghi đè")`.
- `ON DUPLICATE KEY UPDATE earned_point = ?`: chỉ cập nhật điểm, không chạm `response` (dữ liệu của thí sinh).

### 5.8. `SubmissionDao` và `JdbcSubmissionDao`

`SubmissionDao` chỉ còn một hàm `findAllOfSession(session)` (đọc cho bảng kết quả, thống kê). Các thao tác ghi đã
chuyển sang `StudentExamDao` (phần 3) và `GradingDao` – ISP.

`JdbcSubmissionDao.query` là hàm `static` package-private dùng chung với `JdbcGradingDao`: một câu `SELECT_SUB`
(`submission JOIN app_user`) dựng các `Submission`, sau đó `loadAnswers` đọc **một lần** mọi dòng `answer` của các bài đó
bằng `WHERE submission_id IN (?,?,...)`. Danh sách `?` được ghép theo số bài, còn giá trị vẫn truyền bằng `setInt` –
không nối giá trị vào SQL.

`oes/dao/jdbc/JdbcSubmissionDao.java`
```java
                    Answer a = s.restoreAnswer(rs.getInt("item_order"), rs.getString("response"));
                    double p = rs.getDouble("earned_point");
                    if (!rs.wasNull() && s.getStatus() == SubmissionStatus.GRADED) {
                        a.restoreResult(p);
                    }
```
`rs.wasNull()` cần vì `getDouble` trả 0 khi cột `NULL` – phải phân biệt "0 điểm" với "chưa chấm".

### 5.9. `GradingWorker` – một lượt của máy chấm

`oes/service/GradingWorker.java`
```java
    public RunResult runOnce() {
        LocalDateTime now = sessionDao.serverTime();
        int closed = gradingDao.closeExpired(now);

        int graded = 0;
        int failed = 0;
        for (int id : gradingDao.findSubmittedIds()) {
            try {
                Submission s = gradingDao.load(id);
                grader.grade(s);
                if (gradingDao.saveGrades(s)) {
                    graded++;
                    if (s.isTerminatedByViolation()) {
                        // ...
                        AuditLog.record(null, "GRADED_VIOLATION", "ca " + s.getSession().getSessionId() + ", "
                                + s.getStudent().getStudentCode() + ", " + s.getViolationCount() + " lần vi phạm, điểm "
                                + s.getTotalScore());
                    }
                }
            } catch (DataAccessException e) {
                throw e;
            } catch (RuntimeException e) {
                failed++;
                if (reportedFailures.add(id)) {
                    LOG.log(Level.WARNING, "Không chấm được bài " + id + " (sẽ thử lại ở các lượt sau)", e);
                } else {
                    LOG.fine(() -> "Vẫn chưa chấm được bài " + id + ": " + e.getMessage());
                }
            }
        }
```

**Các quyết định và lý do:**
1. **Đóng ca trước, chấm sau** trong cùng lượt: bài vừa bị đóng vì hết giờ được chấm ngay, không phải chờ lượt sau.
2. **Giờ lấy từ MySQL** (`sessionDao.serverTime()` → `SELECT NOW()`): quyết định số 6, mọi máy cùng một đồng hồ.
3. **Hai loại lỗi, hai cách xử lý:**
   - `DataAccessException` (mất kết nối CSDL) → ném ra, **dừng cả lượt**. Mất mạng thì bài nào cũng lỗi; chạy tiếp chỉ
     sinh thêm lỗi. Lượt sau (3 giây) thử lại.
   - Lỗi khác (`RuntimeException`, ví dụ dữ liệu một bài hỏng) → bỏ qua **bài đó**, chấm tiếp bài khác.
   - Kiểm thử: `GradingSchedulerTest.mộtBàiLỗiKhôngChặnCácBàiKhác`, `mấtKếtNốiThìDừngCảLượtNhưngLịchVẫnChạyTiếp`.
4. **Không ghi nhật ký lặp:** `reportedFailures` nhớ mã bài đã báo lỗi. Bài lỗi được thử lại mỗi lượt nhưng chỉ ghi
   stack trace lần đầu (`WARNING`), các lần sau ở mức `FINE` (không ghi ra tệp vì `logging.properties` đặt
   `.level=INFO`). Không làm vậy thì một bài hỏng trong ca 2 giờ sinh khoảng 2.400 stack trace trong `oes-audit.log`
   (DESIGN-CHANGES Phase 2 mục 3). Dùng `ConcurrentHashMap.newKeySet()` vì `runOnce` có thể chạy trên hai luồng cùng lúc
   (lịch định kỳ và nút "Làm mới").
5. **Nhật ký `GRADED_VIOLATION`:** tệp nhật ký là riêng từng máy; sự kiện "kết thúc do vi phạm" ghi ở máy thí sinh thì
   quản trị viên không xem được trên StaffApp. Máy chấm ghi thêm một dòng để màn hình Nhật ký (phần 1) thấy được – NFR-05.
   Kiểm thử `SubmissionFlowTest.đủNămLầnViPhạmThìKếtThúcBài` bắt dòng này.
6. **`RunResult`** – record ba số `closed`, `graded`, `failed`:

`oes/service/GradingWorker.java`
```java
    public record RunResult(int closed, int graded, int failed) {

        /** Lượt này có bài vừa được nộp hoặc vừa được chấm (bài lỗi thử lại không tính). */
        public boolean hasWork() {
            return closed > 0 || graded > 0;
        }
    }
```
   `hasWork()` không tính `failed`: nếu tính, một bài lỗi làm StaffApp và `GradingDaemon` in thông báo mỗi 3 giây.
7. **An toàn khi hai máy chấm chạy cùng lúc** nhờ `saveGrades` có điều kiện `status = 'SUBMITTED'` (javadoc của lớp).
8. **Lớp này chỉ chạy một lượt**; lặp lại là việc của `GradingScheduler` (SRP).

### 5.10. `GradingScheduler` – hẹn giờ

`oes/service/GradingScheduler.java`
```java
    public static final Duration DEFAULT_PERIOD = Duration.ofSeconds(3);
    // ...
    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "grading-worker");
            t.setDaemon(true);   // không giữ chương trình lại khi người dùng đóng ứng dụng
            return t;
        });
        executor.scheduleWithFixedDelay(this::runNow, 0, period.toMillis(), TimeUnit.MILLISECONDS);
    }
```

`oes/service/GradingScheduler.java`
```java
    /**
     * Chạy một lượt ngay (lịch định kỳ gọi hàm này; giao diện có thể gọi khi bấm "Chấm ngay").
     * Phải bắt mọi ngoại lệ: ScheduledExecutorService sẽ lặng lẽ dừng hẳn việc lặp lại
     * nếu một lượt ném ngoại lệ ra ngoài.
     */
    public void runNow() {
        try {
            GradingWorker.RunResult r = worker.runOnce();
            if (failing) {
                failing = false;
                LOG.info("Máy chấm đã kết nối lại CSDL.");
            }
            if (r.hasWork()) {
                onWork.accept(r);
            }
        } catch (RuntimeException e) {
            if (!failing) {
                LOG.log(Level.WARNING, "Máy chấm không chạy được lượt này, sẽ thử lại sau " + period.toSeconds() + " giây", e);
            }
            failing = true;
        }
    }
```

- **Chu kỳ 3 giây** (`DEFAULT_PERIOD`), FR-26. Thí sinh nộp xong chờ tối đa khoảng 3 giây là có điểm.
- **Cái bẫy quan trọng nhất**: nếu việc được hẹn giờ ném ngoại lệ ra ngoài, `ScheduledExecutorService` **lặng lẽ hủy
  lịch** – không in gì, không chạy lại. `runNow` bắt mọi `RuntimeException` để một lần mất mạng không làm máy chấm
  chết vĩnh viễn. Kiểm thử: `assertDoesNotThrow(() -> scheduler.runNow(), "một lượt lỗi không được làm dừng hẳn lịch chạy")`.
- **Cờ `failing`**: mất kết nối chỉ ghi nhật ký **lần đầu** và một dòng "Máy chấm đã kết nối lại CSDL." khi có lại.
- **`onWork`** là `Consumer<GradingWorker.RunResult>`: StaffApp truyền hàm cập nhật nhãn trạng thái; `GradingDaemon` truyền hàm in ra
  màn hình. Lớp này không biết ai dùng nó (không import `javafx`).
- **`start()` gọi hai lần không tạo hai lịch** (kiểm tra `executor != null`); `start`, `close`, `isRunning` là
  `synchronized`.
- **`implements AutoCloseable`**: dùng được trong try-with-resources (`lịchChạyĐịnhKỳGọiMáyChấm`); `close()` gọi
  `shutdownNow()`. `StaffApp.stop()` gọi `close()` khi đóng ứng dụng.
- **Constructor nhận `Duration period`** để kiểm thử chạy 20 ms một lần thay vì chờ 3 giây.

### 5.11. `GradingDaemon` – máy chấm không giao diện

`main` đọc tệp cấu hình (mặc định `db-staff.properties`), dựng `AppContext.jdbc(...)`, gọi `app.sessions.serverTime()`
để **thử kết nối ngay** (sai mật khẩu thì báo lỗi lúc khởi động, không phải 3 giây sau), rồi tạo `GradingScheduler`
với `onWork` in một dòng mỗi lượt có việc. Một *shutdown hook* (luồng JVM chạy khi nhận Ctrl+C) gọi `scheduler.close()`
và `CountDownLatch` giữ luồng `main` sống tới lúc đó (luồng máy chấm là daemon, không tự giữ JVM).

**Vì sao cần?** StaffApp chỉ chấm khi đang mở. Nếu giờ thi không có giảng viên nào mở StaffApp, bài nằm ở
`SUBMITTED` và thí sinh thấy "Kết quả sẽ có khi giảng viên mở máy chấm". `GradingDaemon` chạy trên máy chủ CSDL giải
quyết việc này (script `dist/run-grading-daemon`).

### 5.12. `ResultRow`, `ResultStatistics`, `SessionSummary`

`oes/model/ResultRow.java`
```java
public record ResultRow(int submissionId, String studentCode, String fullName, SubmissionStatus status,
                        Double score, Double scoreOn10, long answeredCount, int violationCount, EndReason endReason) {

    public static ResultRow of(Submission s) {
        boolean graded = s.getStatus() == SubmissionStatus.GRADED;
        return new ResultRow(s.getSubmissionId(), s.getStudent().getStudentCode(), s.getStudent().getFullName(),
                s.getStatus(), graded ? s.getTotalScore() : null, graded ? s.scoreOn10() : null,
                s.answeredCount(), s.getViolationCount(), s.getEndReason());
    }
```
`score` kiểu `Double` (có thể `null`), không phải `double`: bài chưa chấm phải hiện ô trống, không hiện "0" – nếu không
giảng viên tưởng thí sinh được 0 điểm. Bảng kết quả gồm **mọi** bài, kể cả đang làm.

`ResultStatistics` tính từ **bài đã chấm** (`GRADED`):
- `count`, `max`, `min`, `average` qua `DoubleSummaryStatistics`; danh sách rỗng thì trả 0 (tránh giá trị vô cực của
  `summary.getMax()` khi không có phần tử).
- `histogram`: điểm hệ 10 làm tròn xuống, `(int) Math.min(10, Math.floor(s.scoreOn10()))` – `Math.min(10, ...)` là chặn
  phòng hờ (điểm hệ 10 bình thường không vượt 10), bảo đảm chỉ có các cột 0..10.
- `correctRateByItem`: với mỗi câu, tỷ lệ bài được **trọn điểm** câu đó (`isFullMark`). Với đề `PARTIAL`, câu được nửa
  điểm không tính là "đúng" ở đây. Nhãn trên màn hình ghi đúng: "Tỷ lệ trọn điểm từng câu (câu khó nhất ở trên)".
- `hardestItems(limit)`: sắp tăng theo tỷ lệ, câu khó nhất lên đầu – để giảng viên phát hiện câu quá khó hoặc đáp án sai.
- `toCsvLines()`: tiêu đề `ma_sv,ho_ten,diem,diem_he_10,so_cau_tra_loi,so_lan_vi_pham,ket_thuc_do_vi_pham`; xếp theo
  mã SV; số ghi bằng `Locale.ROOT` (dấu chấm thập phân, không phụ thuộc máy cài tiếng Việt dùng dấu phẩy, vốn sẽ làm
  vỡ cột CSV); họ tên có dấu phẩy hoặc nháy kép thì được bọc trong `"..."` (`csvEscape`); cột cuối `co`/`khong`.

`SessionSummary` (record): ca, `SessionPhase`, số ghi danh, số đã vào thi, số đã nộp – cho danh sách ca thi. Javadoc ghi
rõ: không cập nhật thời gian thực (quyết định số 7), giảng viên bấm "Làm mới".

### 5.13. `StatisticsService`

Bốn hàm, **hàm nào cũng gọi `Permissions.requireManage(actor, session.getExam().getSubject())` đầu tiên** (BR-10):
`statsFor`, `summarize`, `listResults`, `exportCsv` (qua `statsFor`). Thí sinh hay giảng viên môn khác gọi thì nhận
`AccessDeniedException` mã `SUBJECT_FORBIDDEN`, câu "Bạn không phụ trách môn ...".

`oes/service/StatisticsService.java`
```java
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
```
**Lưu ý khi giảng viên chỉ vào dòng `w.write('﻿');`:** giữa hai dấu nháy là **ký tự U+FEFF (BOM) thật**, không nhìn
thấy được trong trình soạn thảo; trong tệp nguồn nó là ba byte `EF BB BF`, không phải chuỗi `﻿`. Writer UTF-8 ghi
ký tự này thành ba byte BOM ở đầu tệp.

`exportCsv` **ném `IOException`** (checked) thay vì bọc lại: ổ đầy, thư mục không có quyền ghi là tình huống người
dùng tự khắc phục được (chọn chỗ khác), nên để màn hình xử lý (javadoc của hàm).

### 5.14. `ConnectionManager` – kết nối và thời gian chờ

`oes/dao/jdbc/ConnectionManager.java`
```java
    static final int CONNECT_TIMEOUT_MS = 5_000;
    static final int SOCKET_TIMEOUT_MS = 30_000;
    // ...
        if (!url.contains("connectTimeout")) {
            p.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_MS));
        }
        if (!url.contains("socketTimeout")) {
            p.setProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT_MS));
        }
        try {
            return DriverManager.getConnection(url, p);
        } catch (SQLException e) {
            throw new DataAccessException("Không mở được kết nối tới " + url, e);
        }
```
- `connectTimeout` 5 giây: chờ bắt tay tối đa 5 giây. `socketTimeout` 30 giây: một câu lệnh không có trả lời quá 30 giây
  thì bỏ. Mặc định của driver là chờ vô hạn – máy chủ bật nhưng MySQL không nghe cổng thì ứng dụng treo khoảng 2 phút
  (DESIGN-CHANGES Phase 3 mục 4). Kiểm thử `JdbcIntegrationTest.máyChủKhôngTrảLờiThìBáoLỗiSauVàiGiâyKhôngTreo`.
- Chỉ đặt khi URL chưa ghi: người dựng phòng máy vẫn chỉnh được trong `db.url`.
- **Mỗi thao tác một kết nối, không có connection pool**: không được thêm thư viện; số đo tải cho thấy chưa cần
  (`load-test.md`, "Nhận xét").
- `fromFile(path)` đọc `db.url`, `db.user`, `db.password`; thiếu `db.url` thì báo lỗi rõ ràng.

### 5.15. `JdbcErrors` – dịch lỗi SQL

`final class` package-private (chỉ các DAO JDBC cùng gói dùng), hàm `static`, constructor `private` (không tạo đối tượng).

`oes/dao/jdbc/JdbcErrors.java`
```java
    static ExamAppException translate(SQLException e) {
        if (SQLSTATE_TRIGGER_REJECTED.equals(e.getSQLState())
                && e.getMessage() != null && e.getMessage().startsWith(SessionRuleException.SUBMISSION_LOCKED)) {
            return new SessionRuleException(SessionRuleException.SUBMISSION_LOCKED,
                    "Bài làm đã nộp hoặc ca thi đã hết giờ, không ghi thêm được câu trả lời.", e);
        }
        if (e.getErrorCode() == MYSQL_DUPLICATE_ENTRY) {
            return new DuplicateKeyException("Bản ghi bị trùng: " + e.getMessage(), e);
        }
        if (e instanceof SQLIntegrityConstraintViolationException) {
            return new DataAccessException("Vi phạm ràng buộc dữ liệu: " + e.getMessage(), e);
        }
        return new DataAccessException("Lỗi truy cập CSDL: " + e.getMessage(), e);
    }
```

| Lỗi MySQL | Thành | Ai dùng |
|---|---|---|
| SQLSTATE `45000` **và** thông điệp bắt đầu bằng `SUBMISSION_LOCKED` (trigger) | `SessionRuleException(SUBMISSION_LOCKED)` | `StudentExamService` coi như "bài đã khóa", giống khi service tự phát hiện |
| mã `1062` (`MYSQL_DUPLICATE_ENTRY`, trùng khóa duy nhất) | `DuplicateKeyException` | `StudentExamService.enter`: hai máy cùng "Vào thi" → máy sau mở bài của máy trước |
| vi phạm ràng buộc khác (khóa ngoại...) | `DataAccessException` "Vi phạm ràng buộc dữ liệu: ..." | |
| còn lại (mất mạng, **bị từ chối quyền**...) | `DataAccessException` | Người dùng thấy "Không kết nối được tới máy chủ dữ liệu. Vui lòng thử lại hoặc báo giám thị." |

**Vì sao kiểm tra cả tiền tố thông điệp, không chỉ SQLSTATE 45000?** 45000 là mã "lỗi do người dùng định nghĩa" chung.
Nếu sau này có trigger/thủ tục khác cũng `SIGNAL 45000`, lỗi đó không bị báo nhầm thành "bài đã nộp" (DESIGN-CHANGES
Phase 2 mục 1). **Vì sao kiểu trả về là `ExamAppException`** (lớp cha chung) chứ không phải `DataAccessException`?
Vì `SessionRuleException` không phải lớp con của `DataAccessException`.

Nhờ lớp này, service và view không import gì từ `java.sql` (xem Câu hỏi chung số 10).

### 5.16. `SessionResultsController` + `session-results.fxml` (màn hình giảng viên)

- `init(ws, session)`: dựng cột bảng điểm ("Mã SV", "Họ tên", "Điểm", "Hệ 10", "Số câu trả lời", "Vi phạm",
  "Trạng thái"), đặt `setRowFactory` thêm lớp CSS `row-violation` cho dòng có `terminatedByViolation()`, rồi `load()`.
- `load()`: **một** lần `BackgroundTasks.run` gọi cả `statistics.listResults` và `statistics.statsFor`, gói vào record
  `Loaded`, rồi `render` trên luồng giao diện.
- `render`: bảng điểm; nhãn tóm tắt; phổ điểm luôn đủ 11 cột 0..10 (`getOrDefault(bucket, 0L)`) để nhìn được hình dạng
  phân bố; bảng tỷ lệ từng câu (record `ItemRate`).
- `onRefresh()` (nút **Làm mới**): chạy `ws.gradeNow()` trên luồng nền (→ `StaffApp.gradeNow` → `GradingScheduler.runNow`)
  rồi `load()`. Bài vừa nộp hiện ra ngay, không phải chờ lượt 3 giây.
- `onExport()` (nút **Xuất CSV…**): `FileChooser` gợi ý tên `bang-diem-ca-<mã ca>.csv`; `IOException` được bọc thành
  `UncheckedIOException` vì `BackgroundTasks.run` nhận `Supplier` (không được ném checked exception); xong thì
  `ErrorDialogs.info("Xuất bảng điểm", "Đã lưu bảng điểm vào ...")`.
- `statusText`: "Đang làm bài" / "Chờ chấm – <lý do>" / <lý do>.
- CSS trong `css/app.css`: `.table-row-cell.row-violation { -fx-background-color: #fde7e5; }` (đỏ nhạt).
- **Controller không tính gì**: thống kê ở `ResultStatistics`, quyền ở `StatisticsService` (NFR-09).

`oes/ui/controller/SessionResultsController.java`
```java
        resultTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ResultRow row, boolean empty) {
                super.updateItem(row, empty);
                getStyleClass().remove("row-violation");
                if (!empty && row != null && row.terminatedByViolation()) {
                    getStyleClass().add("row-violation");
                }
            }
        });
```
Phải `remove` trước rồi mới `add`: JavaFX **tái sử dụng** đối tượng dòng khi cuộn; không xóa thì một dòng từng hiển thị bài
vi phạm sẽ giữ màu đỏ khi chuyển sang hiển thị bài khác.

### 5.17. `ResultController` + `result.fxml` (màn hình thí sinh)

`oes/ui/controller/ResultController.java`
```java
    private static final Duration POLL_EVERY = Duration.seconds(2);
    private static final int MAX_POLLS = 30;   // 30 x 2 giây = 60 giây
    // ...
        waitingLabel.setText("Đã nộp bài. Đang chấm bài…");
        poller = new Timeline(new KeyFrame(POLL_EVERY, e -> poll()));
        poller.setCycleCount(MAX_POLLS);
        poller.setOnFinished(e -> {
            spinner.setVisible(false);
            retryButton.setVisible(true);
            waitingLabel.setText("Bài đã được nộp. Kết quả sẽ có khi giảng viên mở máy chấm.");
        });
        poller.play();
```
- Máy thí sinh **không chấm**, chỉ hỏi lại 2 giây/lần qua `client.exams.refresh(...)` (đọc `submission.status`).
  Thấy `isGraded()` thì dừng và `showResult`.
- Giới hạn 60 giây để không hỏi CSDL mãi; quá giờ thì hiện nút **Kiểm tra lại** (`onRetry` → `startPolling` lại từ đầu).
- Mất mạng giữa lúc hỏi: nhãn "Đang chấm bài… (mất kết nối, đang thử lại)", vẫn hỏi tiếp lượt sau.
- `showResult` hiện điểm `x / tổng`, lý do kết thúc, rồi gọi `client.exams.review(...)` lấy chi tiết từng câu.
  Không có đáp án (ca chưa đóng hoặc không cho xem) thì ghi "Đáp án sẽ hiện sau khi ca thi kết thúc." hoặc
  "Ca thi này không cho xem đáp án." **Quyết định có đáp án hay không là của CSDL** (khung nhìn `v_review`), màn hình chỉ
  xem danh sách có đáp án không (`ReviewItem::hasCorrectAnswer`).
- Bài kết thúc do vi phạm: hiện dải đỏ `violationLabel` "Bài thi bị kết thúc do vi phạm quy chế (rời cửa sổ làm bài đủ số
  lần cho phép)."

### 5.18. CSDL trong bộ nhớ: `InMemoryDatabase`, `SubmissionRow`, `ManualClock`

- `InMemoryDatabase`: các "bảng" là `Map` (package-private để các `Memory...Dao` cùng gói đọc ghi trực tiếp). Mỗi DAO
  một lớp, mọi DAO `synchronized (db)` trên cùng đối tượng – "như cùng nối một CSDL". `AppContext.inMemory(db)` và
  `ClientContext.inMemory(db)` dùng chung một `db` – như hai máy cùng một MySQL.
- `SubmissionRow`: bài làm lưu **dạng dòng** (giống dòng `submission` + các dòng `answer`), không lưu thẳng đối tượng
  `Submission`. Lý do: phía giảng viên dựng `Submission` (có đề có đáp án), phía thí sinh dựng `AttemptState`
  (không có đáp án) – hai kiểu khác nhau từ cùng dữ liệu, đúng như khi đọc MySQL (DESIGN-CHANGES Phase 1 mục 8).
  `InMemoryDatabase.toSubmission(row)` làm việc của `JdbcSubmissionDao.query`.
- `ManualClock implements Supplier<LocalDateTime>`: đồng hồ chỉnh bằng tay (`advance`, `set`) – kiểm thử "hết giờ" mà
  không phải chờ thật. `InMemoryDatabase.now()` là "giờ máy chủ" của bản bộ nhớ.
- **Mô phỏng trigger và khung nhìn** (để kiểm thử nghiệp vụ không cần MySQL vẫn kiểm được BR-04, NFR-04):

| Trong MySQL | Trong bộ nhớ |
|---|---|
| trigger `trg_answer_client_insert`/`_update` | `MemoryStudentExamDao.writableRow` ném `SessionRuleException(SUBMISSION_LOCKED)` khi bài không `IN_PROGRESS` hoặc ca đã đóng |
| `v_exam_paper` (không có cờ đúng, chỉ đề đã tới giờ mở) | `MemoryStudentExamDao.loadPaper` + `toPaperItem` (chép sang `PaperOption` không có cờ đúng) |
| `v_review` (chỉ khi ca đóng và cho xem) | `MemoryStudentExamDao.loadReview` (`showAnswer() && isClosed(db.now())`) |
| `UNIQUE (session_id, student_id)` | `MemoryStudentExamDao.createAttempt` ném `DuplicateKeyException` |
| `closeExpired` một câu UPDATE | `MemoryGradingDao.closeExpired` duyệt trong `synchronized (db)` |
| `saveGrades ... AND status = 'SUBMITTED'` | `MemoryGradingDao.saveGrades` trả `false` nếu `r.status != SUBMITTED` |

  **Giới hạn:** bản bộ nhớ không có khái niệm "tài khoản CSDL", nên **không** mô phỏng được GRANT. Chỉ `JdbcSecurityTest`
  chứng minh được phần quyền.

---

## 6. Luồng chạy đầu–cuối

### Luồng A – Bài `SUBMITTED` được chấm, thí sinh thấy điểm

| # | Ở đâu | Gọi gì | SQL / CSDL |
|---|---|---|---|
| 1 | Máy B, nút "Nộp bài" (phần 3) | `ExamController` → `StudentExamService.submit` → `StudentExamDao.markSubmitted` | `UPDATE submission SET status = 'SUBMITTED', submitted_at = ?, end_reason = ? WHERE id = ? AND status = 'IN_PROGRESS'` – tài khoản `oes_client`; GRANT cho phép 3 cột này; trigger `trg_submission_client_update` kiểm tra `OLD.status` là `IN_PROGRESS`, `NEW.status` không phải `GRADED` → cho qua |
| 2 | Máy B, màn hình kết quả | `ResultController.init`: `state.isGraded()` sai → `startPolling()` | Hiện "Đã nộp bài. Đang chấm bài…" |
| 3 | Máy A, luồng `grading-worker` (sau ≤ 3 giây) | `GradingScheduler.runNow` → `GradingWorker.runOnce` → `ExamSessionDao.serverTime` | `SELECT NOW()` |
| 4 | | `GradingDao.closeExpired(now)` | `UPDATE submission sb JOIN exam_session es ... WHERE sb.status = 'IN_PROGRESS' AND es.close_time <= ?` (thường 0 dòng nếu ca còn mở) |
| 5 | | `GradingDao.findSubmittedIds()` | `SELECT id FROM submission WHERE status = 'SUBMITTED' ORDER BY submitted_at, id` (dùng chỉ mục `idx_submission_status`) |
| 6 | | `GradingDao.load(id)` → `ExamSessionDao.findById` → `JdbcSubmissionDao.query` | `SELECT session_id FROM submission WHERE id = ?`; đọc ca + đề **có đáp án** (bảng `question`, `answer_option` – chỉ `oes_staff` đọc được); `SELECT_SUB + " WHERE sb.id = ?"`; `SELECT submission_id, item_order, response, earned_point FROM answer WHERE submission_id IN (?)` |
| 7 | | `SubmissionGrader.grade(s)` → mỗi câu `Answer.grade(exam.strategyFor(q))` → `ExamItem.grade` → `GradingStrategy.grade` | Không có SQL – chỉ tính toán. Cuối cùng `Submission.markGraded(total)` |
| 8 | | `GradingDao.saveGrades(s)` trong `ConnectionManager.inTransaction` | `UPDATE submission SET status = ?, total_score = ? WHERE id = ? AND status = 'SUBMITTED'` rồi batch `INSERT INTO answer(...) VALUES (?,?,NULL,?) ON DUPLICATE KEY UPDATE earned_point = ?`; `COMMIT`. Trigger trên `answer` chạy nhưng `USER()` là `oes_staff` nên không chặn |
| 9 | | `AuditLog.record(null, "AUTO_GRADE", ...)`; nếu bài vi phạm thêm `GRADED_VIOLATION`; `onWork` cập nhật nhãn trạng thái StaffApp qua `Platform.runLater` | Tệp `oes-audit.log` của máy A |
| 10 | Máy B, `Timeline` 2 giây | `ResultController.poll` → `StudentExamService.refresh` → `StudentExamDao.findAttempt` | `SELECT id, status, violation_count, end_reason, total_score FROM submission WHERE session_id = ? AND student_id = ?` – thấy `GRADED` |
| 11 | | `showResult` → `StudentExamService.review` → `StudentExamDao.loadReview` | đọc `v_exam_paper`, `answer` (`earned_point`), `v_review` – `v_review` rỗng nếu ca chưa đóng → "Đáp án sẽ hiện sau khi ca thi kết thúc." |

**Nhánh lỗi:**
- Không máy chấm nào chạy: sau 30 lần hỏi (60 giây) hiện "Bài đã được nộp. Kết quả sẽ có khi giảng viên mở máy chấm." và
  nút "Kiểm tra lại". Bài vẫn an toàn ở `SUBMITTED`.
- Máy A mất kết nối ở bước 3–6: `DataAccessException` → `runOnce` dừng lượt → `runNow` bắt, ghi nhật ký lần đầu,
  đặt `failing = true` → 3 giây sau thử lại.
- Mất kết nối giữa bước 8: giao dịch rollback, bài vẫn `SUBMITTED`, lượt sau chấm lại từ đầu.
- Một bài dữ liệu hỏng ở bước 6–7: `failed++`, ghi stack trace một lần, các bài khác vẫn chấm.
- Một **câu** hỏng ở bước 7: câu đó 0 điểm (`markGradingError`), cả bài vẫn `GRADED`.
- Máy B mất mạng khi hỏi: "Đang chấm bài… (mất kết nối, đang thử lại)".

### Luồng B – Ca hết giờ → máy chấm đóng bài → chấm

| # | Ở đâu | Gọi gì | SQL / CSDL |
|---|---|---|---|
| 1 | Máy B, sau giờ đóng (thí sinh mất mạng, tắt máy, hoặc ứng dụng chưa kịp tự nộp) | `AnswerSaver` (phần 3) còn gửi câu trả lời → `StudentExamService.saveAnswer` → `StudentExamDao.saveAnswer` (service không tự kiểm tra giờ; chú thích trong mã: "CSDL ném SessionRuleException nếu đã hết giờ") | `INSERT INTO answer(submission_id, item_order, response) VALUES (?,?,?) ON DUPLICATE KEY UPDATE response = ?` |
| 2 | MySQL | trigger `trg_answer_client_insert` (hoặc `_update` nếu dòng đã có) | `USER()` là `oes_client...` và **không** tồn tại bài `IN_PROGRESS` có `es.close_time > NOW()` → `SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: submission locked or session closed'` |
| 3 | Máy B | `ConnectionManager.execute` bắt `SQLException` → `JdbcErrors.translate` | → `SessionRuleException` mã `SUBMISSION_LOCKED`, câu "Bài làm đã nộp hoặc ca thi đã hết giờ, không ghi thêm được câu trả lời." Câu trả lời muộn bị từ chối (BR-04) |
| 4 | Máy A, lượt kế tiếp | `GradingWorker.runOnce` → `closeExpired(now)` | `UPDATE submission sb JOIN exam_session es ON es.id = sb.session_id SET sb.status = 'SUBMITTED', sb.submitted_at = es.close_time, sb.end_reason = 'TIME_OUT' WHERE sb.status = 'IN_PROGRESS' AND es.close_time <= ?` – một câu đóng mọi bài của mọi ca hết giờ |
| 5 | | cùng lượt: `findSubmittedIds` → `load` → `grade` → `saveGrades` (như luồng A bước 5–8) | Chỉ những câu đã lưu **trước** giờ đóng được tính |
| 6 | | `AuditLog.record(null, "AUTO_SUBMIT", closed + " bài hết giờ")`, `AUTO_GRADE` | |

Kiểm thử: `SubmissionFlowTest.hếtGiờThìKhôngGhiĐượcVàMáyChấmTựNộp` (bộ nhớ, dùng `ManualClock.advance`),
`JdbcIntegrationTest.máyChấmNộpBàiQuáGiờBằngMộtCâuLệnh` (MySQL thật),
`JdbcSecurityTest.ghiCâuTrảLờiSauGiờĐóngBịTriggerChặn` (gọi thẳng DAO, bỏ qua service – chỉ còn trigger chặn).

### Luồng C – Giảng viên xem kết quả, Làm mới, xuất CSV

| # | Bấm | Gọi gì | SQL / CSDL |
|---|---|---|---|
| 1 | Ca thi → chọn ca → **Xem kết quả** | `StaffSessionsController.onResults` → `ws.open("session-results.fxml", c -> c.init(ws, s.session()))` | |
| 2 | (tự động) | `SessionResultsController.load` → `BackgroundTasks.run` → `StatisticsService.listResults` + `statsFor` | Mỗi hàm: `Permissions.requireManage` trước; rồi `SubmissionDao.findAllOfSession` → `SELECT_SUB + " WHERE sb.session_id = ? ORDER BY u.student_code"` + đọc `answer ... WHERE submission_id IN (...)` |
| 3 | | `ResultRow.of` cho từng bài; `new ResultStatistics(session, subs)` | Không SQL |
| 4 | | `render` (luồng giao diện): bảng, tô đỏ dòng vi phạm, phổ điểm, tỷ lệ từng câu | |
| 5 | **Làm mới** | `onRefresh` → `ws.gradeNow()` → `StaffHomeController.gradeNow` → `StaffApp.gradeNow` → `GradingScheduler.runNow` → `GradingWorker.runOnce`; xong thì `load()` | Như luồng A bước 3–8, rồi như bước 2 |
| 6 | **Xuất CSV…** → chọn tệp | `onExport` → `StatisticsService.exportCsv` → `statsFor` (kiểm quyền + đọc lại) → ghi BOM + `toCsvLines()` → `AuditLog.record(actor, "EXPORT_CSV", tên tệp)` | đọc như bước 2 |
| 7 | | `ErrorDialogs.info("Xuất bảng điểm", "Đã lưu bảng điểm vào ...")` | |

**Nhánh lỗi:** không phụ trách môn → `AccessDeniedException` (`SUBJECT_FORBIDDEN`) → hộp thoại "Không thực hiện được"
với câu "Bạn không phụ trách môn ...". Mất kết nối → `DataAccessException` → "Không kết nối được tới máy chủ dữ liệu.
Vui lòng thử lại hoặc báo giám thị.". Ghi tệp lỗi → `IOException` bọc trong `UncheckedIOException` → `ErrorDialogs`
hiện hộp thoại "Lỗi tệp". Bấm Làm mới khi mất kết nối: lỗi thoát khỏi `runOnce` (`DataAccessException`) bị `runNow` bắt và chỉ ghi nhật ký, nên
bước chấm không hiện lỗi; nhưng `load()` ngay sau đó cũng thất bại và hiện hộp thoại "Không kết nối được..." như trên.

### Luồng D – Máy thí sinh bị sửa, cố đọc đáp án / sửa điểm

Giả sử ai đó lấy được mật khẩu `oes_client` từ tệp `db-client.properties` trên máy trạm và gõ lệnh trực tiếp
(đúng như đoạn demo phần 4, hoặc sửa mã ứng dụng thi để gửi lệnh tùy ý).

| Lệnh của kẻ tấn công | Bị chặn ở đâu | Kết quả |
|---|---|---|
| `SELECT * FROM answer_option` / `SELECT correct_value FROM question` / `SELECT * FROM exam_item` | **GRANT**: `oes_client` không có quyền gì trên các bảng này | MySQL báo `SELECT command denied ...` (RUNBOOK mục 1); trong Java thành `DataAccessException` |
| `SELECT password_hash FROM app_user` | **GRANT** | Từ chối. Qua `v_login` chỉ thấy mã băm của thí sinh; giảng viên/quản trị viên là chuỗi rỗng |
| `SELECT * FROM v_exam_paper` | được phép, nhưng **khung nhìn** không có cột `is_correct`, `correct_value`, `case_sensitive`, không có đáp án chấp nhận của câu điền khuyết, và chỉ hiện đề có ca đã tới giờ mở | Không có gì để lộ |
| `SELECT * FROM v_review` | **khung nhìn** lọc `es.close_time <= NOW() AND es.show_answer = TRUE` | Rỗng khi ca còn mở |
| `UPDATE submission SET total_score = 10 WHERE id = 1` | **GRANT theo cột**: `total_score` không nằm trong danh sách `UPDATE (...)` | MySQL báo `UPDATE command denied ... 'total_score'`; Java: `DataAccessException` |
| `UPDATE answer SET earned_point = 10 ...` | **GRANT theo cột** | Từ chối |
| `UPDATE submission SET status = 'GRADED' ...` | GRANT cho phép cột `status` → **trigger** `trg_submission_client_update` | `SUBMISSION_LOCKED: client cannot grade` → `SessionRuleException` |
| `UPDATE submission SET violation_count = 0 ...` | **trigger** | `SUBMISSION_LOCKED: violation count cannot decrease` |
| `UPDATE submission SET status = 'IN_PROGRESS'` (mở lại bài đã nộp) | **trigger**: `OLD.status <> 'IN_PROGRESS'` | `SUBMISSION_LOCKED: submission is no longer in progress` |
| `INSERT INTO submission(..., status) VALUES (..., 'SUBMITTED')` | **trigger** `trg_submission_client_insert` | `SUBMISSION_LOCKED: new submission must be IN_PROGRESS` |
| ghi `answer` sau giờ đóng | **trigger** `trg_answer_client_*` | xem luồng B |
| `UPDATE app_user SET password_hash = ...` | **GRANT** | Từ chối; chỉ có `EXECUTE` thủ tục `sp_student_change_password`, thủ tục chỉ sửa dòng `role = 'STUDENT'` |

Thứ tự kiểm tra trong MySQL: **quyền** được kiểm trước khi câu lệnh chạy; **trigger** chạy cho từng dòng sắp ghi.
Vì vậy hai loại lỗi khác nhau trong Java: bị từ chối quyền → `DataAccessException` (SQLSTATE 42000, không phải 45000);
trigger chặn → `SessionRuleException(SUBMISSION_LOCKED)`. `JdbcSecurityTest.thíSinhKhôngTựChoMìnhĐiểm` kiểm tra đúng sự
khác biệt này.

**Còn hở gì?** Mọi máy thí sinh dùng chung một tài khoản, nên kẻ tấn công vẫn đọc/ghi được **câu trả lời** của thí sinh
khác (bảng `answer`) – xem mục 12. Đáp án và điểm thì không.

---

## 7. CSDL của phần này

### 7.1. Mười hai bảng (`db/schema.sql`)

| Bảng | Khóa chính | Khóa ngoại | Ràng buộc / ghi chú |
|---|---|---|---|
| `subject` | `id` | – | `code` UNIQUE |
| `course_class` | `id` | `subject_id` → `subject` | `code` UNIQUE |
| `app_user` | `id` | `class_id` → `course_class` | `username` UNIQUE, `student_code` UNIQUE; `role ENUM('ADMIN','INSTRUCTOR','STUDENT')`; ba vai trò chung một bảng |
| `instructor_subject` | (`instructor_id`, `subject_id`) | → `app_user`, → `subject` | bảng nối nhiều–nhiều |
| `question` | `id` | `subject_id` → `subject` | `type ENUM` 4 loại; cột riêng từng loại để `NULL`; `INDEX idx_question_pick (subject_id, chapter, difficulty)` |
| `answer_option` | `id` | `question_id` → `question` `ON DELETE CASCADE` | `is_correct`; còn chứa đáp án chấp nhận của câu điền khuyết |
| `exam` | `id` | `subject_id` → `subject` | `multi_policy ENUM('ALL_OR_NOTHING','PARTIAL')`, `locked`, `version` |
| `exam_item` | (`exam_id`, `item_order`) | → `exam` CASCADE, → `question` | `UNIQUE (exam_id, question_id)`: một câu không vào đề hai lần; `point` |
| `exam_session` | `id` | `exam_id` → `exam` | `CHECK (close_time > open_time)`; `shuffle_questions`, `shuffle_options`, `show_answer` |
| `session_student` | (`session_id`, `student_id`) | → `exam_session` CASCADE, → `app_user` | danh sách ghi danh |
| `submission` | `id` | `session_id` → `exam_session`, `student_id` → `app_user` | **`UNIQUE (session_id, student_id)`**; `status ENUM('IN_PROGRESS','SUBMITTED','GRADED')`; `end_reason ENUM('MANUAL','TIME_OUT','VIOLATION')` cho phép NULL; `total_score DECIMAL(6,2) DEFAULT 0`; `violation_count`; `INDEX idx_submission_status (status)` |
| `answer` | (`submission_id`, `item_order`) | → `submission` CASCADE | `response TEXT`, `flagged` (FR-22), `earned_point DECIMAL(5,2)` cho phép NULL (chưa chấm) |

Tất cả `ENGINE=InnoDB` (có giao dịch và khóa ngoại) và `DEFAULT CHARSET=utf8mb4` (đủ mọi ký tự tiếng Việt).

**Sơ đồ quan hệ (dạng chữ):**
```
subject 1──n course_class 1──n app_user(STUDENT)
subject 1──n question 1──n answer_option
subject n──n app_user(INSTRUCTOR)          qua instructor_subject
subject 1──n exam 1──n exam_item n──1 question
exam 1──n exam_session 1──n session_student n──1 app_user
exam_session 1──n submission n──1 app_user ; mỗi cặp (session, student) tối đa 1 submission
submission 1──n answer                       khóa answer = (submission_id, item_order)
```

**Các quyết định trong bảng của phần 4:**
- **`UNIQUE (session_id, student_id)`** là chốt chặn cuối cùng của quyết định số 1 ("mỗi thí sinh thi đúng một lần"):
  dù hai máy cùng bấm "Vào thi" một lúc, CSDL chỉ cho một dòng đi qua; dòng thứ hai bị lỗi 1062 → `DuplicateKeyException`
  → service mở lại bài đã có. Kiểm tra ở tầng Java (đọc trước rồi mới chèn) không đủ vì có khoảng hở giữa đọc và chèn.
- `answer` dùng **`item_order`** (số thứ tự câu trong đề gốc), không dùng mã câu hỏi: đề trộn thứ tự cho từng thí sinh
  nhưng câu trả lời luôn lưu theo thứ tự gốc, nên chấm không phụ thuộc thứ tự hiển thị.
- `earned_point` cho phép `NULL`, khác `0`: phân biệt "chưa chấm" với "chấm được 0 điểm".
- `total_score DEFAULT 0` nên khi đọc, phía thí sinh chỉ coi là điểm thật khi `status = 'GRADED'` (chú thích trong
  `JdbcStudentExamDao.findAttempt`).
- `ON DELETE CASCADE` từ `submission` sang `answer`: xóa bài thì xóa câu trả lời (không có câu trả lời mồ côi).
- Tên `app_user` chứ không `user`: tránh nhầm với bảng hệ thống `mysql.user`.
- Thứ tự `DROP` ngược thứ tự `CREATE` để không vướng khóa ngoại; đầu tệp còn `DROP VIEW IF EXISTS v_exam_item_for_student`
  – khung nhìn cũ của M4, đã bỏ.

### 7.2. Bốn khung nhìn (`db/security.sql`, mục 1)

| Khung nhìn | Cho ai đọc | Lọc gì | Vì sao |
|---|---|---|---|
| `v_login` | `oes_client` | `CASE WHEN role = 'STUDENT' THEN password_hash ELSE '' END` | Mật khẩu `oes_client` nằm trên mọi máy trạm; không được lộ mã băm của giảng viên/quản trị viên. Vẫn trả dòng các vai trò khác để báo "hãy dùng ứng dụng quản lý" |
| `v_session_for_student` | `oes_client` | Ca + tên đề + môn + tổng điểm + số câu | Đủ cho danh sách ca và màn hình quy chế, không cần đọc `exam_item` |
| `v_exam_paper` | `oes_client` | Không có `is_correct`, `correct_value`, `case_sensitive`; phương án chỉ của câu một/nhiều đáp án; `WHERE EXISTS (... es.open_time <= NOW())` | Đề không có đáp án; không đọc trước đề của ca chưa mở |
| `v_review` | `oes_client` | Phương án đúng, `correct_value`, đáp án chấp nhận; `WHERE es.close_time <= NOW() AND es.show_answer = TRUE` | Đáp án chỉ lộ khi ca **đã đóng** (chặt hơn M1 "sau khi nộp": nộp sớm không lộ cho người còn thi) |

`db/security.sql`
```sql
CREATE OR REPLACE VIEW v_review AS
SELECT es.id AS session_id, ei.item_order, q.type, q.correct_value,
       CASE WHEN q.type IN ('SINGLE_CHOICE', 'MULTI_CHOICE') THEN ao.id END      AS correct_option_id,
       CASE WHEN q.type = 'FILL_IN_BLANK' THEN ao.content END                     AS accepted_answer
FROM exam_session es
JOIN exam_item ei ON ei.exam_id = es.exam_id
JOIN question q   ON q.id = ei.question_id
LEFT JOIN answer_option ao ON ao.question_id = q.id AND ao.is_correct = TRUE
WHERE es.close_time <= NOW() AND es.show_answer = TRUE;
```
`LEFT JOIN` vì câu đúng/sai không có dòng `answer_option` – vẫn phải có một dòng để trả `correct_value`.
`NOW()` trong khung nhìn là giờ máy chủ, nên chỉnh đồng hồ máy trạm không mở sớm được đáp án.

**Vì sao dùng khung nhìn chứ không lọc trong mã Java?** Lọc trong Java thì máy thí sinh vẫn phải có quyền đọc bảng gốc,
và người sửa mã chỉ cần bỏ đoạn lọc là có đáp án. Khung nhìn đặt việc lọc ở **máy chủ**: tài khoản `oes_client` không
có đường nào khác tới bảng gốc. Khung nhìn chạy với quyền người tạo (`SQL SECURITY DEFINER` mặc định), nên chỉ cần
`GRANT SELECT ON v_... TO 'oes_client'@'%'`.

### 7.3. Tài khoản và quyền (`db/security.sql`, mục 3)

`db/security.sql`
```sql
CREATE USER IF NOT EXISTS 'oes_staff'@'%'  IDENTIFIED BY 'doi_mat_khau_staff';
CREATE USER IF NOT EXISTS 'oes_client'@'%' IDENTIFIED BY 'doi_mat_khau_client';

-- "ON *" = mọi bảng của CSDL đang chọn.
GRANT ALL PRIVILEGES ON * TO 'oes_staff'@'%';

-- Máy thí sinh: chỉ đọc khung nhìn, cấu hình ca thi và danh sách ghi danh.
GRANT SELECT ON v_login               TO 'oes_client'@'%';
GRANT SELECT ON v_session_for_student TO 'oes_client'@'%';
GRANT SELECT ON v_exam_paper          TO 'oes_client'@'%';
GRANT SELECT ON v_review              TO 'oes_client'@'%';
GRANT SELECT ON exam_session          TO 'oes_client'@'%';
GRANT SELECT ON session_student       TO 'oes_client'@'%';

-- Bài làm: tạo bài, nộp bài, tăng số lần vi phạm. KHÔNG có quyền ghi total_score.
GRANT SELECT,
      INSERT (session_id, student_id, started_at, status),
      UPDATE (status, submitted_at, end_reason, violation_count)
      ON submission TO 'oes_client'@'%';

-- Câu trả lời: ghi câu trả lời và cờ đánh dấu. KHÔNG có quyền ghi earned_point.
GRANT SELECT,
      INSERT (submission_id, item_order, response, flagged),
      UPDATE (response, flagged)
      ON answer TO 'oes_client'@'%';
```

| Tài khoản | Dùng ở | Quyền |
|---|---|---|
| `oes_staff` | StaffApp, `GradingDaemon` (tệp `db-staff.properties`) | `ALL PRIVILEGES` trên CSDL đang chọn |
| `oes_client` | ExamClientApp (tệp `db-client.properties`) | SELECT 4 khung nhìn + `exam_session` + `session_student`; SELECT/INSERT/UPDATE theo cột trên `submission`, `answer`; `EXECUTE` thủ tục đổi mật khẩu. **Không có gì** trên `question`, `answer_option`, `exam_item`, `app_user` |

- Các tên bảng viết **không kèm tên CSDL** (`ON *`, `ON submission`) để cùng tệp chạy được cho `oes` và `oes_test`.
- `CREATE USER IF NOT EXISTS`, `CREATE OR REPLACE VIEW`, `DROP ... IF EXISTS`: chạy lại nhiều lần không lỗi.
- Mật khẩu trong tệp chỉ là giá trị khởi tạo; RUNBOOK mục 1 bắt đổi ngay bằng `ALTER USER`.
- `'%'` = đăng nhập từ máy nào cũng được (máy trạm trong LAN).

### 7.4. Bốn trigger (`db/security.sql`, mục 4)

`db/security.sql`
```sql
CREATE TRIGGER trg_submission_client_update BEFORE UPDATE ON submission
FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) = 'oes_client' THEN
        IF OLD.status <> 'IN_PROGRESS' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: submission is no longer in progress';
        END IF;
        IF NEW.status = 'GRADED' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: client cannot grade';
        END IF;
        IF NEW.violation_count < OLD.violation_count THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: violation count cannot decrease';
        END IF;
    END IF;
END //
```

| Trigger | Khi | Chặn gì (chỉ với `oes_client`) |
|---|---|---|
| `trg_submission_client_insert` | `BEFORE INSERT ON submission` | `NEW.status <> 'IN_PROGRESS'` |
| `trg_submission_client_update` | `BEFORE UPDATE ON submission` | sửa bài đã không còn `IN_PROGRESS`; đặt `GRADED`; giảm `violation_count` |
| `trg_answer_client_insert` | `BEFORE INSERT ON answer` | ghi khi bài không còn `IN_PROGRESS` hoặc `es.close_time > NOW()` sai (BR-04) |
| `trg_answer_client_update` | `BEFORE UPDATE ON answer` | như trên; `INSERT ... ON DUPLICATE KEY UPDATE` đi qua cả hai trigger |

**Vì sao cần trigger khi đã có GRANT theo cột?** GRANT chỉ nói được "cột nào được ghi", không nói được "ghi **giá trị**
gì" hay "ghi **khi nào**". `oes_client` phải được ghi cột `status` (để nộp bài) và `violation_count` (để tăng vi phạm),
nên GRANT không chặn được `status = 'GRADED'` hay `violation_count = 0`. Ghi `response` sau giờ đóng cũng là cột được
phép. Trigger là "lưới an toàn cuối cùng" cho những trường hợp đó.

**Vì sao `USER()` chứ không `CURRENT_USER()`?** Bên trong trigger, `CURRENT_USER()` trả về **người tạo trigger** (root
– trigger chạy với quyền người tạo), nên điều kiện `= 'oes_client'` sẽ không bao giờ đúng và trigger không chặn gì cả.
`USER()` trả về tên đăng nhập thật của kết nối kèm máy, dạng "oes_client@192.168.1.23"; `SUBSTRING_INDEX(USER(), '@', 1)`
cắt lấy phần trước `@`. Kế hoạch ban đầu ghi `CURRENT_USER()`; mã đã sửa và kiểm chứng trên MySQL 8.0.40
(DESIGN-CHANGES Phase 1 mục 7).

**Vì sao `SIGNAL SQLSTATE '45000'` và tiền tố `SUBMISSION_LOCKED`?** `45000` là SQLSTATE dành cho "lỗi do người dùng định
nghĩa". Thông điệp bắt đầu bằng đúng mã lỗi của hệ thống (`SessionRuleException.SUBMISSION_LOCKED`) để `JdbcErrors`
nhận ra và đổi thành ngoại lệ nghiệp vụ, giống hệt khi service tự phát hiện bài đã khóa. Phần sau dấu `:` bằng tiếng
Anh chỉ để ghi nhật ký; người dùng thấy câu tiếng Việt của `JdbcErrors`.

**Vì sao `oes_staff` không bị chặn?** Máy chấm phải đổi `IN_PROGRESS → SUBMITTED` sau giờ đóng và đặt `GRADED`, ghi
`earned_point`. Mọi điều kiện trong trigger đều bắt đầu bằng `SUBSTRING_INDEX(USER(), '@', 1) = 'oes_client'`.

### 7.5. Thủ tục `sp_student_change_password` (chi tiết đăng nhập: phần 1)

`db/security.sql`
```sql
CREATE PROCEDURE sp_student_change_password(IN p_user_id INT, IN p_old_hash VARCHAR(100),
                                            IN p_new_hash VARCHAR(100), OUT p_changed INT)
SQL SECURITY DEFINER
BEGIN
    UPDATE app_user
    SET password_hash = p_new_hash, must_change_password = FALSE
    WHERE id = p_user_id AND role = 'STUDENT' AND password_hash = p_old_hash
      AND p_new_hash IS NOT NULL AND LOCATE('$', p_new_hash) > 0;
    SET p_changed = ROW_COUNT();
END //
```
Thí sinh phải đổi mật khẩu lần đầu nhưng chỉ dùng ứng dụng thi; cấp `UPDATE app_user` cho `oes_client` thì sửa được mật
khẩu của cả quản trị viên. Thủ tục chạy với quyền người tạo, `oes_client` chỉ có `EXECUTE`, và câu `UPDATE` bên trong
chỉ sửa được **một dòng thí sinh có mã băm cũ khớp** (so rồi mới ghi). `LOCATE('$', p_new_hash) > 0` chặn ghi chuỗi
không phải dạng mã băm của `PasswordHasher`. `ROW_COUNT()` = 1 nếu đổi được, 0 nếu không.

---

## 8. Quy tắc nghiệp vụ

| Quy tắc | Dòng mã thực thi |
|---|---|
| Quyết định 1 / BR-02: mỗi thí sinh một bài trong một ca | `UNIQUE (session_id, student_id)` trong bảng `submission`; `JdbcErrors.translate` đổi mã 1062 thành `DuplicateKeyException` |
| Quyết định 3: mọi bài chấm tự động | `GradingWorker.runOnce` chấm mọi bài `SUBMITTED`, không có bước chấm tay |
| Quyết định 4 / BR-08: nhiều đáp án theo cấu hình đề | `Exam` đặt `PARTIAL ? PARTIAL : EXACT` cho `MULTI_CHOICE`; công thức ở `MultiChoiceQuestion.correctRatio`, chặn ở `PartialMatchGrading.grade` |
| BR-07: một đáp án không điểm âm | `ExactMatchGrading.grade` trả `maxPoint` hoặc `0.0` |
| Quyết định 5 / BR-06: đủ 5 lần vi phạm thì khóa bài, **chấm phần đã làm** | Bài `VIOLATION` vào `SUBMITTED` như bài thường → `SubmissionGrader.grade` chấm các câu đã lưu; `ResultRow.terminatedByViolation` → dòng `row-violation`; CSV cột `ket_thuc_do_vi_pham`; nhật ký `GRADED_VIOLATION` |
| Quyết định 6: giờ chuẩn từ MySQL | `GradingWorker.runOnce`: `LocalDateTime now = sessionDao.serverTime();`; `NOW()` trong `v_exam_paper`, `v_review` và trigger `trg_answer_client_*` |
| Quyết định 7: không giám sát thời gian thực | `SessionSummary` và bảng kết quả chỉ đọc lại khi bấm "Làm mới" |
| BR-03: thời gian kết thúc = giờ đóng ca | `closeExpired`: `sb.submitted_at = es.close_time` |
| BR-04: ca đóng thì khóa, câu trả lời sau đó bị từ chối | `closeExpired` (khóa) + `trg_answer_client_insert`/`_update` (từ chối) + `MemoryStudentExamDao.writableRow` (bản bộ nhớ) |
| BR-10: chỉ giảng viên phụ trách / quản trị viên xem kết quả | `Permissions.requireManage(actor, session.getExam().getSubject())` đầu mỗi hàm `StatisticsService` |
| UC-16 3a: nộp hai lần giữ lần đầu | `Submission.markSubmitted` trả `false`; SQL `... AND status = 'IN_PROGRESS'` |
| UC-16: một câu chấm lỗi không hỏng cả bài | `try/catch` trong vòng lặp `SubmissionGrader.grade` → `Answer.markGradingError` |
| NFR-04: máy thí sinh không đọc được đáp án, không tự sửa điểm | GRANT + khung nhìn + trigger trong `db/security.sql`; `ClientContext` không có `GradingDao` |

---

## 9. SOLID và mẫu thiết kế

| Nguyên lý / mẫu | Ở đâu | Vì sao | Nếu không làm vậy |
|---|---|---|---|
| **S** – Single Responsibility | `Submission` chỉ giữ dữ liệu + chuyển trạng thái; chấm ở `SubmissionGrader`; một lượt chấm ở `GradingWorker`; lặp lại ở `GradingScheduler` | Mỗi lớp một lý do để đổi: đổi cách chấm chỉ sửa `SubmissionGrader`; đổi chu kỳ chỉ sửa `GradingScheduler`. Việc chấm và việc ghi câu trả lời còn phải chạy trên **hai máy khác nhau** | M4: `Submission` có `gradeAll` nên đối tượng tới máy thí sinh phải mang đáp án; kiểm thử chấm phải chờ đồng hồ 3 giây |
| **O** – Open/Closed | `GradingStrategy` + 3 lớp | Thêm cách chấm = thêm một lớp mới, không sửa 3 lớp cũ hay `SubmissionGrader` (NFR-10) | Một hàm `grade` với `switch` theo loại câu, thêm cách chấm phải sửa hàm đang chạy đúng. **Trung thực:** vẫn phải đăng ký lớp mới trong constructor `Exam` (phần 2) |
| **L** – Liskov | Ba chiến lược cùng hợp đồng "trả điểm trong [0, maxPoint]"; `SubmissionGrader` dùng chiến lược nào cũng đúng | `PartialMatchGrading` tự chặn hai đầu để giữ hợp đồng dù câu hỏi tính sai | Một chiến lược trả điểm âm làm tổng bài sai mà nơi gọi không biết. (`TextMatchGrading` từ chối câu không phải điền khuyết – đó là bảo vệ cấu hình, vì `Exam` chỉ gắn nó cho `FILL_IN_BLANK`) |
| **I** – Interface Segregation | `GradingDao` (máy chấm), `SubmissionDao` (thống kê), `StudentExamDao` (máy thí sinh – phần 3) | Ứng dụng thi chỉ được lắp DAO của nó; không có đường gọi `saveGrades` hay đọc đề có đáp án, kể cả do lỗi lập trình | Một `SubmissionDao` to cho mọi bên như M4: máy thí sinh "thấy" hàm ghi điểm |
| **D** – Dependency Inversion | `GradingWorker(GradingDao, ExamSessionDao, SubmissionGrader)`, `StatisticsService(SubmissionDao)`, `GradingScheduler(..., Consumer<GradingWorker.RunResult>)` | Service nhận interface qua constructor; `AppContext` chọn JDBC hay bộ nhớ; kiểm thử truyền `FakeGradingDao` | Muốn kiểm thử "mất kết nối giữa lượt" phải rút dây mạng thật |
| **Strategy** | `GradingStrategy` | Cách chấm đổi theo đề, lúc chạy | `if (policy == PARTIAL) ... else ...` rải ở nhiều nơi |
| **DAO** | `GradingDao`/`JdbcGradingDao`/`MemoryGradingDao` | Tách SQL khỏi nghiệp vụ; hai cài đặt thay nhau được | SQL lẫn trong `GradingWorker`, không chạy được không cần MySQL |
| **Callback** (hàm truyền vào) | `GradingScheduler.onWork` | Lớp hẹn giờ không biết ai dùng (StaffApp cập nhật nhãn, `GradingDaemon` in ra màn hình) | `GradingScheduler` phải import `javafx` – vi phạm quy ước "service không biết giao diện" |
| **Khóa lạc quan** | `saveGrades ... WHERE id = ? AND status = 'SUBMITTED'` | Hai máy chấm chạy cùng lúc không ghi đè nhau, không cần khóa bảng | Bài bị chấm hai lần, tổng điểm có thể bị ghi đè lẫn lộn |
| **Đối tượng không trạng thái dùng chung** | `Exam.EXACT`, `PARTIAL`, `TEXT` là `static final` | Không có trường nào nên dùng chung an toàn cho mọi đề, mọi luồng | Mỗi đề tạo 3 đối tượng thừa |

---

## 10. Khác với thiết kế M3/M4

Theo `docs/DESIGN-CHANGES.md` (chỉ các mục liên quan phần 4):

| Thay đổi | Lý do | Ảnh hưởng |
|---|---|---|
| **Máy thí sinh không còn chấm bài**; `GradingWorker` trên máy giảng viên chấm (Phase 1, đầu mục) | M4 chấm trên máy thí sinh nên máy thí sinh phải đọc được đáp án – trái NFR-04 mà M1 đã hứa | Sau khi nộp, bài ở `SUBMITTED` tới khi máy chấm chạy; màn hình kết quả hiện "Đang chấm bài…" |
| `Submission` chỉ còn dữ liệu + `markSubmitted`/`markGraded`; bỏ `saveAnswer`, `addViolation`, `reachedViolationLimit`, `submit`, `gradeAll`, `MAX_VIOLATIONS` (Phase 1 mục 1, SRP) | Mỗi lớp một lý do để thay đổi | Lớp mới `SubmissionGrader` (giữ nguyên logic `gradeAll`); `Answer.markGradingError` thành `public`, `Answer.response` thành `final` |
| `GradingDao` mới, `SubmissionDao` thu gọn còn `findAllOfSession` (Phase 1 mục 4, ISP) | Không ép bên dùng phụ thuộc hàm nó không dùng | `ExamSession.closeAndSubmitAll` bị bỏ – thay bằng một câu UPDATE trong `GradingDao.closeExpired` |
| `SubmissionService` (M4) bị xóa, thay bằng `StudentExamService` + `GradingWorker` (Phase 1 mục 5) | Hai phía chạy trên hai máy, hai tài khoản CSDL | `closeExpiredSessions` của M4 ("chưa có ai gọi") nay được gọi 3 giây/lần; `submitted_at` khi hết giờ = giờ đóng ca |
| `db/security.sql` mới: 4 khung nhìn (thay `v_exam_item_for_student` của M4), 2 tài khoản, GRANT theo cột, 4 trigger; `answer.flagged`, chỉ mục `status`; `JdbcErrors` đổi SQLSTATE 45000 (Phase 1 mục 7) | NFR-04 phải do CSDL bảo đảm, không phụ thuộc mã ứng dụng thi | Trigger dùng `USER()` thay `CURRENT_USER()`; chặt hơn kế hoạch: `v_login` không lộ mã băm giảng viên, `v_exam_paper` chỉ hiện đề ca đã mở, trigger chặn giảm vi phạm và tạo bài sai trạng thái; đáp án chỉ sau khi ca đóng |
| `InMemoryDatabase` lưu theo dòng `SubmissionRow`, mỗi DAO một lớp (Phase 1 mục 8) | Hai phía dựng hai kiểu đối tượng khác nhau từ cùng dữ liệu; bản bộ nhớ làm đúng việc của khung nhìn và trigger | Kiểm thử nghiệp vụ không cần MySQL vẫn kiểm được BR-04, NFR-04 |
| `JdbcErrors` chỉ đổi 45000 khi thông điệp bắt đầu bằng `SUBMISSION_LOCKED` (Phase 2 mục 1) | Lỗi khác không bị báo nhầm "bài đã nộp" | |
| `GradingScheduler` tách khỏi `GradingWorker` (Phase 2 mục 3, SRP) | Kiểm thử gọi thẳng `runOnce()`; cái bẫy "ngoại lệ làm dừng lịch" cần chỗ riêng | Nhật ký không bị ngập: bài lỗi ghi stack trace một lần; mất kết nối ghi một lần + một dòng khi kết nối lại |
| `ConnectionManager.open` đặt `connectTimeout = 5 s`, `socketTimeout = 30 s` (Phase 3 mục 4) | Driver mặc định chờ vô hạn, ứng dụng treo khoảng 2 phút | Kiểm thử `máyChủKhôngTrảLờiThìBáoLỗiSauVàiGiâyKhôngTreo` |
| `SessionSummary` + `StatisticsService.summarize` (Phase 4–5 mục 4) | Danh sách ca thi cần số thí sinh / đã vào / đã nộp | |
| `DuplicateKeyException.getUserMessage()` riêng (Phase 7) | Trước đó kế thừa câu "Không kết nối được..." của lớp cha – trùng khóa bị báo nhầm là mất mạng | |
| Nhật ký `GRADED_VIOLATION` ở máy chấm (commit "sửa: nhật ký máy chấm ghi bài kết thúc do vi phạm") | Nhật ký là tệp riêng từng máy | Quản trị viên xem được trên StaffApp |

---

## 11. Kiểm thử

### 11.1. Không cần MySQL (chạy bằng `./mvnw test`)

| Lớp test | Phương thức | Chứng minh gì |
|---|---|---|
| `SubmissionGraderTest` | `mộtCâuNémLỗiThìCácCâuCònLạiVẫnĐượcChấm` | Câu 2 là câu hỏi "hỏng" (lớp con ẩn danh ném lỗi trong `isCorrect`); tổng vẫn 7.0 = 2 + 0 + 5, câu 2 có `hasGradingError()` |
| | `câuBỏTrốngĐược0ĐiểmVàVẫnCóCâuTrảLờiĐểThốngKê` | Câu không trả lời vẫn có `Answer`, `isGraded()` đúng, 0 điểm |
| | `chiếnLượcChấmCâuNhiềuĐápÁnTheoCấuHìnhĐề` | Đề `PARTIAL`, câu 4 điểm, chọn đúng 1/2 → 2.0 |
| | `chỉChấmĐượcBàiĐãNộp` | Chấm bài `IN_PROGRESS` → `IllegalStateException` |
| | `nộpHaiLầnThìLầnSauBịBỏQua` | `markSubmitted` lần hai trả `false`, giữ lý do `MANUAL` |
| | `chấmBàiNămMươiCâuDướiBaGiây` | NFR-02 bằng `assertTimeout(Duration.ofSeconds(3), ...)`; đề 50 câu đủ 4 loại, tổng 50.0 |
| `GradingTest` | `cùngMộtCâuHaiChiếnLượcChoHaiKếtQuả` (`@ParameterizedTest`, 4 bộ dữ liệu) | Bảng ở mục 5.5: `21,22` → 4/4; `21` → 0/2; `21,23` → 0/0; trống → 0/0 |
| | `điểmTheoTỷLệĐượcLàmTrònHaiChữSố` | 2/3 → 0.67 |
| | `textMatchTừChốiCâuKhôngPhảiĐiềnKhuyết` | Mã lỗi `WRONG_STRATEGY` |
| | `đềChọnChiếnLượcTheoLoạiCâuVàCấuHình` | `PARTIAL` → `PartialMatchGrading`, `ALL_OR_NOTHING` → `ExactMatchGrading`; `assertSame` chứng minh dùng chung thể hiện |
| `GradingSchedulerTest` | `mộtBàiLỗiKhôngChặnCácBàiKhác` | `FakeGradingDao`: bài 1 hỏng, bài 2 tốt → `graded = 1`, `failed = 1`; lượt sau `hasWork()` sai |
| | `mấtKếtNốiThìDừngCảLượtNhưngLịchVẫnChạyTiếp` | `runOnce` ném `DataAccessException`; `runNow` không ném; kết nối lại thì chấm tiếp |
| | `lịchChạyĐịnhKỳGọiMáyChấm` | Chu kỳ 20 ms, `CountDownLatch` chờ `onWork` được gọi; dùng try-with-resources đóng lịch |
| `SubmissionFlowTest` (phần 3, nhưng kiểm phần 4) | `hếtGiờThìKhôngGhiĐượcVàMáyChấmTựNộp` | `ManualClock.advance(55 phút)` → ghi bị `SUBMISSION_LOCKED`; `runOnce` cho `closed = 2`, `graded = 2`; chỉ câu lưu trước giờ đóng được tính |
| | `đủNămLầnViPhạmThìKếtThúcBài` | Bài vi phạm vẫn được chấm phần đã làm (4.0); nhật ký có `GRADED_VIOLATION` |
| | `đápÁnChỉHiệnSauKhiCaĐóng` | Bản bộ nhớ làm đúng việc của `v_review` |
| `StaffServicesTest` | `bảngKếtQuảCóCảBàiChưaChấmVàĐánhDấuViPhạm` | `listResults` có cả bài đang làm (`score()` null), bài vi phạm có `terminatedByViolation()`; thí sinh gọi bị từ chối |
| `AdminAndResultsTest` | `tómTắtCaThiĐếmThíSinhĐãVàoVàĐãNộp` | `summarize`: `UPCOMING` → `OPEN`, 3 ghi danh / 2 đã vào / 1 đã nộp; thí sinh gọi → `AccessDeniedException` |

**Kỹ thuật kiểm thử đáng nói:** `FakeGradingDao` trong `GradingSchedulerTest` là một cài đặt giả của `GradingDao` viết
ngay trong lớp test – chỉ làm được vì `GradingWorker` phụ thuộc interface (DIP). `ManualClock` thay đồng hồ thật để
"tua nhanh" 55 phút trong một dòng.

### 11.2. Với MySQL thật

`JdbcIntegrationTest` (13 ca, chỉ cần `OES_TEST_DB`). **Lưu ý:** trong lớp này cả `AppContext.jdbc(cm)` lẫn
`ClientContext.jdbc(cm)` dùng **cùng một kết nối `oes_staff`**, nên trigger không bao giờ chặn và GRANT không được kiểm
tra ở đây – lớp này kiểm SQL và luồng nghiệp vụ trên MySQL thật. Các ca liên quan phần 4:
`ràngBuộcDuyNhấtChặnBàiLàmThứHai` (UNIQUE → `DuplicateKeyException`), `haiMáyCùngBấmVàoThiChỉTạoMộtBài`,
`nộpVàChấmQuaCSDL` (điểm 7.0 = 6 × 0.5 + 4; `saveGrades` lần hai trả `false`), `máyChấmNộpBàiQuáGiờBằngMộtCâuLệnh`,
`giaoDịchLỗiGiữaChừngThìKhôngGhiGìCả` (rollback của `inTransaction`), `máyChủKhôngTrảLờiThìBáoLỗiSauVàiGiâyKhôngTreo`
(địa chỉ `10.255.255.1`, phải báo lỗi dưới 15 giây), `lỗiKếtNốiĐượcBọcThànhDataAccessException` (nguyên nhân gốc là
`SQLException`, câu cho người dùng không chứa "jdbc").

`JdbcSecurityTest` (8 ca, **chạy bằng chính tài khoản `oes_client`**, hàm `asClient` gửi câu SQL tùy ý như ứng dụng thi
bị sửa):

| Phương thức | Chứng minh |
|---|---|
| `thíSinhKhôngĐọcĐượcBảngĐápÁn` | `SELECT` trên `answer_option`, `question`, `exam_item`, `app_user` đều bị từ chối (GRANT) |
| `thíSinhKhôngTựChoMìnhĐiểm` | `total_score`, `earned_point` bị GRANT chặn (`DataAccessException`); `status = 'GRADED'` bị trigger chặn (`SUBMISSION_LOCKED`); `violation_count = 0` bị trigger chặn |
| `ghiCâuTrảLờiSauGiờĐóngBịTriggerChặn` | Gọi thẳng `client.studentExams.saveAnswer` (bỏ qua service) sau giờ đóng → `SUBMISSION_LOCKED`; `setFlag` cũng vậy |
| `bàiĐãNộpThìMáyThíSinhKhôngSửaĐược` | Không ghi câu trả lời, không mở lại `IN_PROGRESS` sau khi nộp |
| `luồngĐầyĐủQuaHaiTàiKhoảnVàĐápÁnChỉHiệnSauKhiĐóngCa` | Thí sinh làm bài qua khung nhìn → máy chấm (`oes_staff`) chấm 2.0 → `review` trước khi đóng ca không có đáp án → đóng ca thì có |
| `khungNhìnĐăngNhậpKhôngLộMãBămCủaGiảngViên` | `v_login` trả mã băm rỗng cho giảng viên; đăng nhập giảng viên ở ứng dụng thi → `WRONG_APP` |
| `thíSinhĐổiMậtKhẩuQuaThủTụcLưuTrữ` | `sp_student_change_password` đổi được mật khẩu thí sinh, từ chối khi mã băm cũ không khớp (`PASSWORD_CHANGED_ELSEWHERE`), không đổi được mật khẩu giảng viên |
| `thíSinhKhôngĐọcTrướcĐượcĐềCủaCaChưaMở` | `v_exam_paper` rỗng khi ca chưa tới giờ mở |

**Cách chạy** (theo `docs/PROGRESS.md` và `docs/SETUP.md` mục 8): tạo CSDL riêng `oes_test`, chạy `schema.sql` rồi
`security.sql`, tạo hai tệp properties (một bằng `oes_staff`, một bằng `oes_client`), rồi:

```bash
OES_TEST_DB=test-staff.properties OES_TEST_DB_CLIENT=test-client.properties ./mvnw test
```

Thiếu biến môi trường thì các ca JDBC **được bỏ qua** (`assumeTrue` trong `@BeforeAll`), không báo lỗi – nhờ vậy máy
không có MySQL vẫn chạy được 102 ca còn lại. Đủ biến: 123 ca, 0 lỗi, 0 bỏ qua (`SETUP.md`).

### 11.3. Kiểm thử tải (`test/tools/LoadSimulator.java`, `docs/load-test.md`)

Chương trình `main` (không phải JUnit), giả lập 60 thí sinh song song bằng `Executors.newFixedThreadPool(students)`.
Mỗi thí sinh đăng nhập bằng `oes_client` thật (đi qua khung nhìn, GRANT, trigger), vào thi, 2 giây lưu một câu trong
3 phút, nộp bài. Cuối cùng máy chấm (`oes_staff`) chấm tất cả trong **một** lượt `runOnce`. Đo bằng `System.nanoTime()`
quanh đúng lời gọi service, in p50/p95/p99/max.

Kết quả ngày 28/10/2026 (MySQL 8.0.40 **cùng máy**, localhost):

| Thao tác | Số lần | p50 (ms) | p95 (ms) | max (ms) |
|---|---|---|---|---|
| Vào thi | 60 | 59.4 | 70.9 | 80.5 |
| Lưu câu trả lời | 5 357 | 18.2 | 31.3 | 113.8 |
| Nộp bài | 60 | 24.6 | 36.2 | 43.6 |

0 lỗi. Máy chấm: **60 bài trong 2 162 ms** (khoảng 36 ms/bài, đề 20 câu, gồm cả đọc CSDL). Lưu câu trả lời chậm nhất
114 ms, thấp hơn ngưỡng 2 giây của NFR-01 khoảng 17 lần.

**Thiếu sót:** `ResultStatistics` (phổ điểm, `hardestItems`) và `exportCsv` chưa có kiểm thử JUnit riêng; chúng được
chạy thử qua `ConsoleDemo` (bước "9. Thống kê ca thi", in phổ điểm và nội dung tệp CSV) và qua `UiSmoke instructor`.

---

## 12. Giới hạn đã biết

Trả lời trung thực khi bị hỏi "còn lỗ hổng gì?":

1. **Mọi máy thí sinh dùng chung tài khoản `oes_client`.** CSDL không phân biệt được *thí sinh nào* gửi lệnh. Người sửa
   ứng dụng thi vẫn đọc/ghi được câu trả lời của thí sinh khác (trong lúc bài người đó còn `IN_PROGRESS` và ca còn mở),
   và đọc được mã băm mật khẩu của mọi thí sinh qua `v_login`. **Không** đọc được đáp án, **không** sửa được điểm –
   điều NFR-04 yêu cầu. Chặn hẳn cần một máy chủ ứng dụng ở giữa (mỗi thí sinh một phiên), ngoài công nghệ được phép
   (chỉ Java + JavaFX + JDBC).
2. **`security.sql` mới chạy thử trên MySQL 8.0.40, chưa chạy trên MariaDB**, dù kế hoạch yêu cầu cả hai (`PROGRESS.md`
   "Câu hỏi mở"; RUNBOOK mục 1 hướng dẫn kiểm tra tay nếu máy chủ là MariaDB).
3. **Số đo tải đo trên localhost**, không phải LAN phòng máy. Mỗi vòng đi về trong LAN thêm khoảng 1 ms; con số thật sẽ
   cao hơn chút nhưng còn xa ngưỡng. `load-test.md` có lệnh chạy lại trong phòng máy; nếu p95 vượt 500 ms thì xem
   việc "mỗi thao tác mở một kết nối" trước.
4. **Không có connection pool**: mỗi thao tác mở kết nối mới (bắt tay TCP + TLS + xác thực). Chưa cần theo số đo, và
   không được thêm thư viện.
5. **Cờ "câu chấm lỗi" (`Answer.hasGradingError`) chỉ sống trong bộ nhớ** lúc chấm và một dòng `WARNING` trong nhật ký;
   CSDL chỉ lưu 0 điểm, bảng kết quả không hiện cờ này. Bài vẫn thành `GRADED`, không tự chấm lại. Muốn chấm lại phải
   dùng `oes_staff` đặt `status` về `SUBMITTED` bằng SQL tay.
6. **Lỗi bị từ chối quyền** (chỉ xảy ra khi ứng dụng thi bị sửa) được `JdbcErrors` gộp vào `DataAccessException`, nên câu
   hiện ra là "Không kết nối được tới máy chủ dữ liệu..." – không đúng bản chất, nhưng người dùng bình thường không gặp.
7. `oes_staff` có `ALL PRIVILEGES` (kể cả `DROP`): tệp `db-staff.properties` chỉ được đặt trên máy giảng viên / máy chủ.
8. `GradingDao.load` đọc lại ca và đề cho **mỗi** bài (vài truy vấn/bài). Đo được 36 ms/bài nên chưa tối ưu.
9. Trong `runOnce`, mọi `DataAccessException` đều dừng cả lượt – kể cả `load` báo "Không có bài làm ..." cho một bài.
   Thực tế không xảy ra vì mã bài vừa đọc từ `findSubmittedIds` và ứng dụng không xóa bài làm.

---

## 13. Đoạn demo của mình (2 phút 30)

Chuẩn bị: mở sẵn cửa sổ dòng lệnh `mysql -h <ip> -u oes_client -p oes` (DEMO-SCRIPT, mục "Chạy thử trước buổi demo").

| Bấm | Nói | Phía sau là mã nào |
|---|---|---|
| Máy A: Ca thi → chọn ca → Xem kết quả → **Làm mới** | "Máy chấm chạy trên máy giảng viên 3 giây một lượt; Làm mới cho chấm ngay. Thí sinh bên kia đã thấy điểm." | `StaffSessionsController.onResults` → `SessionResultsController.init`/`load`; `onRefresh` → `StaffApp.gradeNow` → `GradingScheduler.runNow` → `GradingWorker.runOnce` → `JdbcGradingDao.saveGrades`. Bên máy B: `ResultController.poll` thấy `GRADED` |
| Chỉ dòng tô đỏ | "Bài kết thúc do vi phạm được đánh dấu riêng (BR-06). Thống kê, phổ điểm, câu khó nhất lên đầu." | `ResultRow.terminatedByViolation` → lớp CSS `row-violation` (`setRowFactory`); `ResultStatistics.getHistogram`, `hardestItems` |
| Xuất CSV… → mở bằng Excel | "Tiếng Việt hiển thị đúng nhờ ghi kèm BOM UTF-8." | `SessionResultsController.onExport` → `StatisticsService.exportCsv` (`w.write` ký tự BOM) → `ResultStatistics.toCsvLines`; nhật ký `EXPORT_CSV` |
| Cửa sổ dòng lệnh `oes_client`: `SELECT * FROM answer_option;` | "Đây là tài khoản CSDL của máy thí sinh. Đọc bảng đáp án: **bị từ chối**." | `security.sql` mục 3: không có `GRANT` nào trên `answer_option`; dòng chú thích "Không cấp gì trên question, answer_option, exam_item, app_user" |
| `UPDATE submission SET total_score = 10 WHERE id = 1;` | "Tự sửa điểm: **bị từ chối**. Máy thí sinh chỉ đọc được các khung nhìn không có đáp án; trigger chặn ghi sau giờ đóng. Dù sửa mã ứng dụng thi cũng không lấy được đáp án (NFR-04)." | `GRANT ... UPDATE (status, submitted_at, end_reason, violation_count) ON submission` – không có `total_score`; trigger `trg_answer_client_insert`/`_update`; kiểm chứng tự động ở `JdbcSecurityTest` |
| (slide) | "Milestone 5: tách Submission, ExamSession theo SRP; tách interface DAO theo người dùng (ISP); service chỉ biết interface (DIP). Chi tiết trong DESIGN-CHANGES." | `Submission` + `SubmissionGrader`; `GradingDao` / `SubmissionDao` / `StudentExamDao`; `AppContext` |

Nếu còn thời gian hoặc bị hỏi thêm, gõ tiếp trong cửa sổ `oes_client`:
`UPDATE submission SET status = 'GRADED' WHERE id = 1;` – lần này **trigger** báo `SUBMISSION_LOCKED: client cannot grade`
(minh họa "GRANT cho ghi cột `status`, trigger chặn giá trị `GRADED`"). Lưu ý: nếu bài mã 1 đã nộp, trigger báo câu
`SUBMISSION_LOCKED: submission is no longer in progress` trước.

---

## 14. Câu hỏi vấn đáp

### Cơ bản

**1. Vì sao máy thí sinh không tự chấm cho nhanh?**
Muốn chấm thì phải có đáp án. Máy thí sinh mà có đáp án thì người sửa ứng dụng thi đọc được đáp án – vi phạm NFR-04. Đó là
lỗi của bản M4, đã sửa ở M5: máy thí sinh chỉ đánh dấu `SUBMITTED`; `GradingWorker` trên máy giảng viên (tài khoản
`oes_staff`) chấm. Chậm hơn tối đa khoảng 3 giây.

**2. Bài làm có những trạng thái nào, ai chuyển?**
`IN_PROGRESS → SUBMITTED → GRADED` (`SubmissionStatus`). Máy thí sinh tạo bài và nộp (`MANUAL`, `VIOLATION`); máy chấm
nộp thay khi hết giờ (`TIME_OUT`, `closeExpired`) và là nơi duy nhất đặt `GRADED` (`Submission.markGraded` +
`saveGrades`). Trigger chặn `oes_client` đặt `GRADED`.

**3. Mẫu Strategy nằm ở đâu, giải thích?**
Interface `GradingStrategy` một hàm `grade(question, response, maxPoint)`; ba lớp `ExactMatchGrading`,
`PartialMatchGrading`, `TextMatchGrading`. `Exam` chọn lớp theo loại câu và `MultiChoicePolicy`; `Answer.grade` chỉ gọi
qua interface. Đổi cách chấm = đổi đối tượng, không có `if/else` theo loại.

**4. Công thức chấm `PARTIAL`? Câu có đáp án đúng A, B; thí sinh chọn A, B, C; câu 4 điểm thì được bao nhiêu?**
Tỷ lệ = (số đúng đã chọn − số sai đã chọn) / tổng số đúng, chặn dưới 0 (`MultiChoiceQuestion.correctRatio`), rồi
`PartialMatchGrading` chặn trong [0, 1], nhân điểm câu, làm tròn 2 chữ số. Chọn A, B, C: (2 − 1)/2 = 0.5 → **2 điểm**.
Nếu đề `ALL_OR_NOTHING` thì 0. Trừ phần chọn sai để không ai "chọn hết" mà vẫn trọn điểm.

**5. Máy chấm chạy ở đâu, bao lâu một lần?**
Trong StaffApp (khi kết nối CSDL xong, `StaffApp.onConnected` gọi `GradingScheduler.start()`), hoặc `GradingDaemon`
trên máy chủ. Chu kỳ `GradingScheduler.DEFAULT_PERIOD` = 3 giây.

**6. Thí sinh nộp xong mà không ai mở StaffApp thì sao?**
Bài nằm ở `SUBMITTED`, không mất. `ResultController` hỏi lại 2 giây/lần tối đa 30 lần (60 giây), rồi hiện "Bài đã được
nộp. Kết quả sẽ có khi giảng viên mở máy chấm." và nút "Kiểm tra lại". Khi StaffApp hoặc `GradingDaemon` chạy, bài được
chấm ở lượt đầu tiên.

**7. Hai tài khoản MySQL khác nhau thế nào?**
`oes_staff`: `ALL PRIVILEGES`, dùng ở StaffApp/`GradingDaemon`. `oes_client`: chỉ `SELECT` 4 khung nhìn + `exam_session`,
`session_student`; ghi theo cột trên `submission`, `answer`; `EXECUTE` một thủ tục. Không có quyền gì trên `question`,
`answer_option`, `exam_item`, `app_user`.

**8. Khung nhìn là gì, vì sao dùng?**
Câu `SELECT` có tên, dùng như bảng, chạy với quyền người tạo. `oes_client` chỉ được đọc khung nhìn đã bỏ cột đáp án
(`v_exam_paper`), nên dù sửa mã Java cũng không có đường tới bảng gốc. Lọc ở Java thì phải cho máy thí sinh quyền đọc
bảng gốc – vô nghĩa.

**9. Vì sao tệp CSV có BOM?**
Excel trên Windows mở CSV không BOM thì đoán sai bảng mã, tên tiếng Việt thành ký tự lỗi. `StatisticsService.exportCsv`
ghi ký tự U+FEFF đầu tệp (ba byte `EF BB BF`) để Excel biết là UTF-8.

**10. Dòng đỏ trong bảng kết quả nghĩa là gì, làm thế nào?**
Bài kết thúc do vi phạm quy chế (BR-06). `ResultRow.terminatedByViolation()` (= `endReason == EndReason.VIOLATION`);
`setRowFactory` thêm lớp CSS `row-violation`, `app.css` tô nền `#fde7e5`.

**11. CSDL có bao nhiêu bảng? `UNIQUE (session_id, student_id)` để làm gì?**
12 bảng (mục 7.1). Ràng buộc này bảo đảm mỗi thí sinh chỉ có một bài trong một ca, kể cả hai máy cùng bấm "Vào thi" một
mili giây: dòng thứ hai bị MySQL từ chối (mã 1062) → `DuplicateKeyException` → service mở bài đã có. Kiểm thử
`haiMáyCùngBấmVàoThiChỉTạoMộtBài`.

### Đào sâu

**12. Vì sao trigger dùng `USER()` chứ không `CURRENT_USER()`?**
Trong trigger, `CURRENT_USER()` là tài khoản **định nghĩa** trigger (root), không phải người đang kết nối; điều kiện
`= 'oes_client'` sẽ luôn sai, trigger vô dụng. `USER()` là tài khoản kết nối thật kèm máy ("oes_client@192.168.1.23");
`SUBSTRING_INDEX(USER(), '@', 1)` lấy phần tên. Đã kiểm chứng trên MySQL 8.0.40.

**13. Đã có GRANT theo cột, sao còn cần trigger?**
GRANT chỉ nói "được ghi cột nào". `oes_client` buộc phải được ghi `status` (nộp bài) và `violation_count` (tăng vi phạm),
`response` (trả lời). GRANT không ngăn được ghi **giá trị** `GRADED`, giảm `violation_count`, hay ghi `response` **sau
giờ đóng**. Trigger kiểm tra giá trị và thời điểm.

**14. Hai máy chấm chạy cùng lúc (hai giảng viên cùng mở StaffApp) thì có chấm trùng không?**
Cả hai có thể cùng chấm một bài trong bộ nhớ, nhưng chỉ một bên **ghi** được: `saveGrades` chạy
`UPDATE ... WHERE id = ? AND status = 'SUBMITTED'`; bên đến sau nhận 0 dòng, trả `false`, giao dịch kết thúc không ghi gì.
`GradingWorker` chỉ tăng `graded` khi `saveGrades` trả `true`. Kiểm thử `nộpVàChấmQuaCSDL` (`assertFalse(app.grading.saveGrades(reloaded), ...)`).

**15. Vì sao `closeExpired` là một câu `UPDATE ... JOIN`? Vì sao `submitted_at = es.close_time`?**
Một câu thì MySQL xử lý nguyên khối, không có khoảng hở giữa "đọc danh sách" và "sửa" để sót bài. Giờ nộp ghi bằng giờ
đóng ca vì máy chấm có thể chạy trễ; BR-03 nói thời gian làm bài kết thúc tại giờ đóng ca.

**16. Vì sao `GradingScheduler.runNow` bắt mọi `RuntimeException`?**
Nếu việc được hẹn giờ ném ngoại lệ ra ngoài, `ScheduledExecutorService` lặng lẽ hủy lịch, máy chấm chết vĩnh viễn mà
không ai biết. Bắt lại, ghi nhật ký lần đầu (`failing`), lượt sau thử tiếp. Kiểm thử
`mấtKếtNốiThìDừngCảLượtNhưngLịchVẫnChạyTiếp`.

**17. Trong `runOnce`, vì sao `DataAccessException` dừng cả lượt mà lỗi khác chỉ bỏ qua một bài?**
`DataAccessException` thường là mất kết nối: bài nào cũng sẽ lỗi, chạy tiếp chỉ sinh hàng loạt lỗi. Lỗi khác (dữ liệu một
bài hỏng) là chuyện riêng bài đó, không được làm các thí sinh khác chờ điểm. Bài lỗi được thử lại mỗi lượt nhưng chỉ ghi
stack trace một lần (`reportedFailures`).

**18. Vì sao `saveGrades` cần giao dịch? Mất mạng giữa chừng thì sao?**
Tổng điểm (`submission`) và điểm từng câu (`answer`) phải nhất quán. `ConnectionManager.inTransaction` tắt auto-commit,
lỗi thì `rollback`: bài vẫn `SUBMITTED`, không có bài `GRADED` mà thiếu điểm câu; lượt sau chấm lại.

**19. `JdbcErrors` nhận ra lỗi do trigger thế nào? Vì sao kiểm tra cả tiền tố thông điệp?**
`e.getSQLState()` bằng `"45000"` **và** `e.getMessage()` bắt đầu bằng `SUBMISSION_LOCKED`. SQLSTATE 45000 là mã chung cho
mọi lỗi tự định nghĩa; kiểm tra tiền tố để lỗi `SIGNAL` khác (nếu sau này thêm) không bị báo nhầm "bài đã nộp".

**20. `UPDATE submission SET total_score = 10` và `UPDATE submission SET status = 'GRADED'` từ `oes_client` – Java nhận
ngoại lệ gì?**
Câu đầu bị **GRANT** chặn trước khi chạy (lỗi quyền, SQLSTATE 42000) → `DataAccessException`. Câu sau qua được GRANT (cột
`status` được phép), bị **trigger** chặn (45000 + `SUBMISSION_LOCKED`) → `SessionRuleException`. `JdbcSecurityTest.thíSinhKhôngTựChoMìnhĐiểm`
kiểm tra đúng hai kết quả này.

**21. Không có MySQL thì kiểm thử BR-04, NFR-04 bằng gì? Có kiểm được GRANT không?**
`InMemoryDatabase` + các `Memory...Dao` làm đúng việc của khung nhìn và trigger: `MemoryStudentExamDao.writableRow` ném
`SUBMISSION_LOCKED` khi bài khóa hoặc ca đóng; `toPaperItem` không chép cờ đúng; `loadReview` chỉ trả đáp án khi ca đóng.
**Không** kiểm được GRANT (bản bộ nhớ không có tài khoản CSDL) – việc đó chỉ `JdbcSecurityTest` làm.

**22. Vì sao `ResultRow.score` là `Double` chứ không `double`?**
Bài chưa chấm phải hiện ô trống (`null`), không phải "0" – nếu không giảng viên tưởng thí sinh 0 điểm. Tương tự cột
`answer.earned_point` cho phép `NULL` và `JdbcSubmissionDao` dùng `rs.wasNull()`.

**23. Vì sao đáp án chỉ hiện sau khi ca đóng, không phải ngay sau khi nộp như M1?**
Thí sinh nộp sớm xem đáp án rồi báo cho bạn còn đang thi. `v_review` lọc `es.close_time <= NOW() AND es.show_answer = TRUE`,
`NOW()` là giờ máy chủ nên chỉnh đồng hồ máy trạm không mở sớm được.

**24. Vì sao `scheduleWithFixedDelay` mà không `scheduleAtFixedRate`? Vì sao luồng daemon?**
`FixedDelay` đếm 3 giây **sau khi lượt trước xong**: lượt chậm (60 bài) không làm các lượt dồn lên nhau. Daemon để đóng
StaffApp là JVM thoát, không bị luồng `grading-worker` giữ lại.

**25. `TextMatchGrading` ném lỗi với câu không phải điền khuyết – có vi phạm Liskov không?**
Về lý thuyết là thêm điều kiện đầu vào. Nhưng `Exam` chỉ gắn `TextMatchGrading` cho `FILL_IN_BLANK`, nên trong chương
trình không có lời gọi nào vi phạm; ném lỗi `WRONG_STRATEGY` là để lỗi cấu hình lộ ra ngay thay vì chấm sai lặng lẽ. Với
câu đúng loại, mọi chiến lược cùng trả điểm trong [0, maxPoint].

**26. Bảng "tỷ lệ đúng từng câu" tính thế nào? Đề `PARTIAL` thì câu được nửa điểm có tính là đúng không?**
`ResultStatistics` đếm số bài **trọn điểm** câu đó (`Answer.isFullMark`) chia số bài đã chấm. Nửa điểm **không** tính.
Nhãn trên màn hình ghi "Tỷ lệ trọn điểm từng câu". `hardestItems` sắp tăng dần để câu khó nhất lên đầu.

**27. Vì sao mỗi thao tác mở một kết nối mới, không dùng connection pool?**
Không được thêm thư viện (pool như HikariCP là thư viện ngoài). Số đo 60 thí sinh: lưu câu trả lời p95 31 ms, đủ xa ngưỡng
2 giây. `ConnectionManager` gom việc mở kết nối vào một chỗ, nên sau này thay bằng pool chỉ sửa một lớp.

### Sửa trực tiếp

**28. "Thêm một cách chấm mới cho câu nhiều đáp án: đúng hết được trọn điểm, đúng một phần được nửa điểm."**
- Tạo lớp mới trong `oes/grading/`, cài `GradingStrategy`, ví dụ (mã gợi ý, chưa có trong dự án):
  ```java
  public class HalfCreditGrading implements GradingStrategy {
      @Override
      public double grade(Question question, String response, double maxPoint) {
          if (question.isCorrect(response)) {
              return maxPoint;
          }
          return question.correctRatio(response) > 0 ? maxPoint / 2 : 0.0;
      }
  }
  ```
- Đăng ký (thuộc phần 2): thêm giá trị vào enum `MultiChoicePolicy` (kèm `displayName`), sửa constructor `Exam` chọn lớp
  mới cho `MULTI_CHOICE`; thêm giá trị vào `ENUM('ALL_OR_NOTHING','PARTIAL')` của cột `exam.multi_policy` trong
  `schema.sql` (CSDL đang chạy thì `ALTER TABLE exam MODIFY multi_policy ...`). Ô chọn trên màn hình tạo đề tự có giá trị
  mới vì `ExamBuilderController` dùng `MultiChoicePolicy.values()`; `JdbcExamDao` đọc bằng `valueOf`.
- **Không sửa** `ExactMatchGrading`, `PartialMatchGrading`, `TextMatchGrading`, `SubmissionGrader` – đó là OCP.
- Kiểm thử: thêm một ca vào `GradingTest` (như `cùngMộtCâuHaiChiếnLượcChoHaiKếtQuả`), chạy `./mvnw test -Dtest=GradingTest`.

**29. "Đổi chu kỳ máy chấm thành 10 giây."**
Sửa `GradingScheduler.DEFAULT_PERIOD = Duration.ofSeconds(10)`. `StaffApp.onConnected` và `GradingDaemon.main` đều dùng
hằng này, nhãn trạng thái và dòng in tự đổi theo (`DEFAULT_PERIOD.toSeconds()`). Sửa thêm chú thích
`-- máy chấm tìm bài SUBMITTED mỗi 3 giây` trong `schema.sql` cho khớp. Màn hình thí sinh vẫn hỏi 2 giây/lần trong 60 giây
– vẫn đủ. Kiểm: `./mvnw test -Dtest=GradingSchedulerTest` (ca `lịchChạyĐịnhKỳGọiMáyChấm` tự truyền chu kỳ riêng nên không
phụ thuộc hằng).

**30. "Cho `oes_client` đọc thêm bảng `exam` ngay tại chỗ." / "Bỏ quyền đọc `exam_session`."**
Thêm một dòng ở mục 3 của `db/security.sql`: `GRANT SELECT ON exam TO 'oes_client'@'%';`, rồi chạy lại
`mysql -u root -p oes < db/security.sql` (tệp chạy lại không lỗi). Bỏ quyền thì `GRANT` không tự gỡ: phải chạy
`REVOKE SELECT ON exam_session FROM 'oes_client'@'%';` (và xóa dòng `GRANT` trong tệp). **Nói rõ cái giá**: `exam` không
có đáp án nên an toàn; nhưng cấp đọc `question`/`answer_option` là phá NFR-04, khi đó `JdbcSecurityTest.thíSinhKhôngĐọcĐượcBảngĐápÁn`
sẽ đỏ. Kiểm: `OES_TEST_DB=... OES_TEST_DB_CLIENT=... ./mvnw test -Dtest=JdbcSecurityTest`; bỏ `exam_session` thì ứng
dụng thi đọc ca sẽ lỗi – chạy lại cả `JdbcIntegrationTest`.

**31. "Thêm cột giờ nộp vào tệp CSV."**
Sửa `ResultStatistics.toCsvLines`: thêm một cột mới (ví dụ nop_luc) vào dòng tiêu đề và thêm `String.valueOf(s.getSubmittedAt())` (hoặc định
dạng bằng `DateTimeFormatter`) vào `String.join(",", ...)` đúng vị trí. Không sửa `StatisticsService` (BOM, ghi tệp giữ
nguyên). Kiểm: chạy `./mvnw compile exec:java` (`ConsoleDemo` in nội dung CSV) hoặc thêm ca kiểm thử gọi `toCsvLines()`.

**32. "Cho thí sinh chờ điểm tối đa 2 phút thay vì 60 giây."**
`ResultController`: đổi `MAX_POLLS` từ 30 thành 60 (60 × 2 giây). Không ảnh hưởng máy chấm. Kiểm bằng tay trên ứng dụng thi
(tắt StaffApp, nộp bài, đếm thời gian tới khi hiện nút "Kiểm tra lại").

**33. "Cho xem đáp án ngay khi thí sinh nộp, không chờ ca đóng."**
Sửa `v_review` trong `security.sql`: điều kiện phải dựa trên bài của thí sinh, nhưng khung nhìn hiện lọc theo ca, không
biết thí sinh nào (cùng tài khoản `oes_client`). Bỏ điều kiện `es.close_time <= NOW()` thì **mọi** thí sinh đọc được đáp
án ngay khi có một người nộp – kể cả người đang thi. Vì vậy không nên đổi; đây chính là lý do thiết kế chặt hơn M1. Nếu
vẫn bị yêu cầu: sửa khung nhìn, sửa `MemoryStudentExamDao.loadReview` cho khớp, chạy `SubmissionFlowTest` và
`JdbcSecurityTest` (ca `luồngĐầyĐủQuaHaiTàiKhoảnVàĐápÁnChỉHiệnSauKhiĐóngCa` sẽ phải sửa).

**34. "Đổi thời gian chờ kết nối thành 10 giây."**
`ConnectionManager.CONNECT_TIMEOUT_MS = 10_000`. Hoặc không sửa mã: thêm `connectTimeout=10000` vào `db.url` trong tệp
properties (mã chỉ đặt mặc định khi URL chưa ghi). Kiểm: `JdbcIntegrationTest.máyChủKhôngTrảLờiThìBáoLỗiSauVàiGiâyKhôngTreo`
(ngưỡng 15 giây vẫn đạt).

---

## 15. Thuật ngữ

| Thuật ngữ | Nghĩa trong dự án |
|---|---|
| Máy chấm | `GradingWorker` + `GradingScheduler`, chạy trong StaffApp hoặc `GradingDaemon` |
| Lượt chấm | Một lần `GradingWorker.runOnce`: đóng ca hết giờ + chấm mọi bài `SUBMITTED` |
| Strategy (chiến lược) | Mẫu thiết kế: mỗi cách tính điểm một lớp cài `GradingStrategy` |
| `ALL_OR_NOTHING` / `PARTIAL` | Chấm câu nhiều đáp án toàn phần / theo tỷ lệ (cấu hình của đề) |
| Khung nhìn (view) | Câu `SELECT` có tên, dùng như bảng; chạy với quyền người tạo |
| GRANT theo cột | Cấp quyền ghi chỉ trên một số cột của bảng |
| Trigger | Mã MySQL tự chạy trước khi ghi mỗi dòng; dùng `SIGNAL` để từ chối |
| SQLSTATE 45000 | Mã lỗi "do người dùng định nghĩa" mà trigger phát ra |
| `SQL SECURITY DEFINER` | Khung nhìn/thủ tục chạy bằng quyền người tạo, không phải người gọi |
| Giao dịch (transaction) | Nhiều câu lệnh cùng thành công hoặc cùng bị hủy (`commit`/`rollback`) |
| Khóa lạc quan | `UPDATE ... WHERE status = 'SUBMITTED'`, đếm số dòng bị sửa để biết có ai đổi trước |
| BOM | Ký tự U+FEFF đầu tệp báo bảng mã UTF-8 cho Excel |
| Luồng daemon | Luồng phụ, không giữ JVM khi chương trình kết thúc |
| p50 / p95 | 50% / 95% số lần đo nhanh hơn giá trị này |
| `oes_staff` / `oes_client` | Tài khoản MySQL của máy giảng viên / máy thí sinh |
| `InMemoryDatabase` | CSDL giả bằng `Map`, dùng cho kiểm thử và `ConsoleDemo` |

---

## 16. Tự kiểm tra trước buổi vấn đáp

- ☐ Nói được trong 1 phút phần mình làm gì (mục 0) mà không nhìn giấy.
- ☐ Vẽ được trên bảng: máy thí sinh → `SUBMITTED` trong CSDL → máy chấm → `GRADED` → máy thí sinh hỏi lại 2 giây/lần.
- ☐ Giải thích vì sao máy thí sinh không chấm (NFR-04, lỗi bản M4).
- ☐ Kể ba trạng thái bài và ai được chuyển trạng thái nào.
- ☐ Tính tay điểm `PARTIAL` cho mọi dòng trong bảng mục 5.5, kể cả trường hợp chọn hết và chặn dưới 0.
- ☐ Chỉ ra chỗ `Exam` chọn chiến lược, và vì sao ba chiến lược dùng chung một thể hiện.
- ☐ Giải thích `try/catch` trong vòng lặp của `SubmissionGrader.grade`.
- ☐ Đọc to được câu SQL của `closeExpired` và `saveGrades`, nói vì sao có `AND status = 'SUBMITTED'`.
- ☐ Phân biệt hai cách xử lý lỗi trong `GradingWorker.runOnce`, và vì sao `reportedFailures`.
- ☐ Giải thích cái bẫy của `ScheduledExecutorService` và cách `runNow` tránh nó.
- ☐ Kể tên 12 bảng; chỉ ra `UNIQUE (session_id, student_id)` và tác dụng.
- ☐ Kể 4 khung nhìn, mỗi cái lọc gì; vì sao dùng khung nhìn chứ không lọc trong Java.
- ☐ Đọc được các dòng `GRANT` của `oes_client`, nói cột nào **không** được ghi.
- ☐ Kể 4 trigger; vì sao cần trigger dù có GRANT; vì sao `USER()` chứ không `CURRENT_USER()`.
- ☐ Giải thích đường đi của lỗi trigger: `SIGNAL 45000 'SUBMISSION_LOCKED: ...'` → `JdbcErrors.translate` → `SessionRuleException`.
- ☐ Phân biệt lỗi "bị từ chối quyền" (`DataAccessException`) và "trigger chặn" (`SessionRuleException`).
- ☐ Giải thích `connectTimeout`/`socketTimeout` và vì sao phải đặt.
- ☐ Giải thích BOM và vì sao `w.write('…')` trông như rỗng.
- ☐ Nói được `InMemoryDatabase` mô phỏng gì của MySQL và **không** mô phỏng gì (GRANT).
- ☐ Kể 3 ca của `JdbcSecurityTest` và cách chạy kiểm thử JDBC (`OES_TEST_DB`, `OES_TEST_DB_CLIENT`).
- ☐ Đọc thuộc số đo tải: lưu bài p95 31 ms, max 114 ms; chấm 60 bài 2,2 giây; đo trên localhost.
- ☐ Trả lời trung thực ba giới hạn: dùng chung `oes_client`; chưa chạy MariaDB; số đo tải trên localhost.
- ☐ Làm thử tại chỗ ít nhất một câu "Sửa trực tiếp" (đổi `DEFAULT_PERIOD`, chạy `./mvnw test -Dtest=GradingSchedulerTest`).
- ☐ Chạy thử đoạn demo một lần trên đúng máy demo, kể cả hai lệnh SQL trong cửa sổ `oes_client`.
