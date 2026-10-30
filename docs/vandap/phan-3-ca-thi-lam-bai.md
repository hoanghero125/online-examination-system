# Phần 3 – Ca thi và làm bài (máy thí sinh)

> Người phụ trách: **Nguyễn Hoàng Trung** (B24DCCE273) · Đọc kèm: `docs/PHAN-CONG.md` (câu hỏi chung)

Quy ước đường dẫn giống `PHAN-CONG.md`: `oes/` = `src/main/java/com/group2/oes/`, `fxml/` = `src/main/resources/fxml/`,
`test/` = `src/test/java/com/group2/oes/`. Tài liệu không dùng số dòng; mọi chỗ dẫn bằng tên tệp + tên phương thức.

---

## 0. Nói trong 1 phút

> "Phần em là **ca thi và làm bài**, tức là mọi thứ từ lúc giảng viên bấm *Mở ca thi* tới lúc thí sinh bấm *Nộp bài*.
> Phía giảng viên, `ExamSessionService` tạo ca thi: một đề, một khung giờ mở – đóng **dùng chung cho mọi thí sinh**
> (`SessionWindow`), ba tùy chọn trộn câu / trộn phương án / cho xem đáp án (`SessionPolicy`), và danh sách ghi danh.
> Phía thí sinh, ứng dụng thi dùng một **mô hình riêng không có đáp án** (gói `client`), vì máy thí sinh chỉ được đọc
> các khung nhìn không chứa đáp án (NFR-04). `StudentExamService` lo vào thi, lưu câu trả lời, nộp bài; mỗi thí sinh
> chỉ có **đúng một bài** trong một ca – thoát ra vào lại thì mở tiếp bài cũ, nhờ ràng buộc `UNIQUE (session_id, student_id)`.
> Màn hình làm bài tách thành các lớp nhỏ: `AnswerSaver` lưu sau 500 ms và tự thử lại khi mất mạng, `ExamProctor` giữ
> toàn màn hình và phát hiện rời cửa sổ, `ServerClock` tính giờ theo **giờ máy chủ MySQL** chứ không theo đồng hồ máy
> trạm. Rời cửa sổ là vi phạm; các lần sát nhau dưới 3 giây gộp làm một; đủ 5 lần thì bài bị kết thúc – quy chế này nằm
> gọn trong `ViolationPolicy`. Nộp bài chỉ đánh dấu `SUBMITTED`; việc chấm điểm là của máy giảng viên (phần 4)."

---

## 1. Phạm vi

### 1.1. Yêu cầu chức năng (mã số trong `docs/PLAN.md`, mục 4 – bảng truy vết)

| FR | Nội dung | Lớp chính của phần 3 | Màn hình |
|---|---|---|---|
| FR-17 | Ca thi có giờ mở/đóng | `ExamSession`, `SessionWindow` | Mở ca thi |
| FR-18 | Cấu hình trộn, xem đáp án | `SessionPolicy` | Mở ca thi |
| FR-19 | Ghi danh | `ExamSessionService` | Mở ca thi |
| FR-20 | Danh sách ca thi của thí sinh | `StudentExamService.overview`, `SessionEntry` | Danh sách ca thi |
| FR-21 | Điều kiện vào thi | `StudentExamService.enter`, `SessionWindow.ensureOpen` | Xác nhận quy chế |
| FR-22 | Hiển thị đề, điều hướng, đánh dấu | `PaperShuffler`, `ExamPaper`, `AnswerSheet` | Làm bài |
| FR-23 | Đếm ngược, cảnh báo 5 phút | `SessionWindow`, `ServerClock`, `Timeline` | Làm bài |
| FR-24 | Lưu bài | `StudentExamDao.saveAnswer` + `AnswerSaver` | Làm bài |
| FR-25 | Nộp bài có xác nhận | `StudentExamService.submit` | Làm bài |
| FR-26 | Tự nộp khi ca đóng | **chia đôi**: máy thí sinh tự nộp khi đồng hồ về 0 (`ExamController.onTick`); máy chấm đóng ca quá giờ (`GradingWorker`, phần 4) | – |
| FR-27 | Xác nhận quy chế, toàn màn hình | `RulesController`, `ExamProctor` | Xác nhận quy chế, Làm bài |
| FR-28 | Ghi nhận vi phạm | `ProctoringService`, `ViolationPolicy` | Làm bài |
| FR-29 | 5 lần thì kết thúc | `ProctoringService`, `ViolationPolicy` | Làm bài |
| FR-33 | Thí sinh xem kết quả, đáp án | phần dữ liệu: `StudentExamService.review`, `StudentExamDao.loadReview`; màn hình `ResultController` thuộc phần 4 | Kết quả |

### 1.2. Use case, quy tắc nghiệp vụ, yêu cầu phi chức năng

- **Use case:** UC-10 (Lập lịch và cấu hình ca thi), UC-11 (Ghi danh), UC-12 (Xem danh sách ca thi), UC-13 (Vào ca thi),
  UC-14 (Làm bài), UC-15 (Ghi nhận vi phạm), UC-16 (Nộp bài – nửa "nộp"; nửa "chấm" thuộc phần 4).
- **BR:** BR-01 (vào thi khi có tên và đang trong giờ), BR-02 (mỗi thí sinh đúng một bài), BR-03 (giờ kết thúc chung là
  giờ đóng ca), BR-04 (ca đóng thì khóa bài, từ chối câu trả lời gửi muộn), BR-05 (rời cửa sổ là vi phạm, 5 lần thì kết
  thúc). BR-06 (đánh dấu riêng trong bảng điểm) là của phần 4.
- **NFR:** NFR-04 (máy thí sinh không đọc được đáp án – phần 3 lo mô hình `client` và DAO phía thí sinh, phần 4 lo tài
  khoản MySQL và khung nhìn), NFR-06 (tắt máy đột ngột không mất bài, đăng nhập máy khác làm tiếp), NFR-07 (tối đa 3
  thao tác để bắt đầu làm bài), NFR-01 (lưu câu trả lời < 2 giây; đo bằng `LoadSimulator` của phần 4).
- **Quyết định nghiệp vụ đã chốt trong `CLAUDE.md`:** số 1 (thi đúng một lần, vào lại mở bài cũ), số 2 (giờ mở/đóng
  chung), số 5 (toàn màn hình, 5 lần, gộp dưới 3 giây), số 6 (giờ chuẩn lấy từ `SELECT NOW()`).

### 1.3. Phần 3 sở hữu những tệp nào

Đúng bảng "Phần 3" trong `docs/PHAN-CONG.md`:

| Tầng | Tệp |
|---|---|
| Model | `oes/model/ExamSession`, `SessionWindow`, `SessionPolicy`, `SessionPhase`, `PaperShuffler`, `ViolationPolicy` |
| Mô hình máy thí sinh | toàn bộ `oes/client/`: `SessionInfo`, `SessionEntry`, `ExamPaper`, `PaperItem`, `PaperOption`, `ExamAttempt`, `AttemptState`, `AnswerSheet`, `AnswerFormat`, `ServerClock`, `ReviewItem` |
| Service | `oes/service/ExamSessionService`, `StudentExamService`, `ProctoringService` |
| DAO | `oes/dao/ExamSessionDao`, `StudentExamDao` + `JdbcExamSessionDao`, `JdbcStudentExamDao` + `MemorySessionDao`, `MemoryStudentExamDao` |
| Giao diện | `oes/ui/ExamProctor`, `AnswerSaver`, `AppConfig` |
| Màn hình | `SessionFormController`, `StaffSessionsController`, `SessionListController`, `RulesController`, `ExamController`, `ServerSetupController` + FXML cùng tên (`session-form.fxml`, `staff-sessions.fxml`, `session-list.fxml`, `rules.fxml`, `exam.fxml`, `server-setup.fxml`) |
| CSDL | bảng `exam_session`, `session_student`; khung nhìn `v_session_for_student`, `v_exam_paper`; quyền ghi của `oes_client` lên `submission`, `answer` |
| Kiểm thử | `test/SessionWindowTest`, `PaperShufflerTest`, `ViolationPolicyTest`, `ClientModelTest`, `SubmissionFlowTest`; `test/ui/UiSmoke` |

---

## 2. Ranh giới với các phần khác

Mỗi dòng: **bên phần 3** gọi / được gọi bởi **bên phần khác**.

### Với phần 1 (người dùng, khung ứng dụng – Đỗ Phạm Bảo Hoàng)

| Phía phần 3 | Phía phần 1 | Nối thế nào |
|---|---|---|
| Mọi hàm `public` của `StudentExamService` | `Permissions.requireStudent(actor)` | Kiểm tra vai trò thí sinh, trả về `Student`; sai vai trò ném `AccessDeniedException` mã `ROLE_REQUIRED` |
| `ExamSessionService.createSession`, `listOfExam`, `studentsOfClass` | `Permissions.requireManage(actor, subject)` | Chỉ giảng viên phụ trách môn hoặc quản trị viên; sai thì mã `SUBJECT_FORBIDDEN` |
| `ExamSessionService.allStudents` | `Permissions.requireStaff(actor)` | |
| Mọi controller của phần 3 | `BackgroundTasks.run(...)`, `ErrorDialogs.show / alert / confirm`, `Navigator.show(fxml, init)` | Gọi service trên luồng nền, hiện lỗi, chuyển màn hình |
| `ProctoringService`, `StudentExamService` | `ClientContext` (constructor riêng) | Phần 1 lắp ráp: `new ProctoringService(studentExams, exams, ViolationPolicy.standard())` |
| `ExamSessionService` | `AppContext` (trường `sessionService`) | Phần 1 lắp ráp: `new ExamSessionService(sessions, exams, users)` |
| `SessionListController.init` | `ExamClientApp.showHome(user)` | Đăng nhập xong thì mở danh sách ca thi |
| `ServerSetupController.init`, `AppConfig.locate / defaultLocation / save` | `OesApplication.start`, `OesApplication.showServerSetup` | Tìm tệp cấu hình, nhập địa chỉ máy chủ |
| `StudentExamService`, `ProctoringService` | `AuditLog.record(...)` | Ghi nhật ký `START_EXAM`, `SUBMIT_...`, `TERMINATED_BY_VIOLATION` |
| Ném `SessionRuleException` | Lớp `SessionRuleException` nằm trong danh sách tệp của phần 1 | Phần 3 dùng các hằng `NOT_OPEN_YET`, `CLOSED`, `NOT_ENROLLED`, `ALREADY_SUBMITTED`, `SUBMISSION_LOCKED` |

### Với phần 2 (câu hỏi, đề thi – Đỗ Thị Vân Anh)

| Phía phần 3 | Phía phần 2 | Nối thế nào |
|---|---|---|
| `SessionFormController.init(ws, exam)` | `ExamListController.onOpenSession` | Chọn đề trong "Đề thi" → bấm "Mở ca thi…" |
| `new ExamSession(...)` | `Exam.validateForUse()` | Đề phải dùng được (có câu, tổng điểm > 0) mới mở ca |
| `ExamSessionService.createSession` | `exam.lock()` + `ExamDao.lock(examId)` | BR-09: có ca thi thì khóa đề |
| `ExamSession.buildPaperFor`, `optionsFor` | `Exam.getItems()`, `Question.getOptions()` | Đề gốc có đáp án (chỉ phía giảng viên) |
| `JdbcStudentExamDao.loadPaper` | bảng `exam_item`, `question`, `answer_option` | Đọc qua khung nhìn `v_exam_paper`, không đọc thẳng bảng |

### Với phần 4 (chấm bài, kết quả, CSDL – Phan Tùng Dương)

| Phía phần 3 | Phía phần 4 | Nối thế nào |
|---|---|---|
| `StudentExamService.submit` → bài `SUBMITTED` | `GradingWorker.runOnce` → `GradingDao.findSubmittedIds` | Phần 3 chỉ đánh dấu đã nộp; phần 4 chấm, đặt `GRADED` |
| Ca hết giờ, máy thí sinh tắt | `GradingDao.closeExpired(now)` | Máy chấm tự nộp bài `IN_PROGRESS` của ca quá giờ với `end_reason = 'TIME_OUT'` |
| `ExamController.finish(state)` | `ResultController.init(shell, client, user, session, state)` | Chuyển sang màn hình kết quả |
| `StudentExamService.refresh`, `review` | `ResultController.poll`, `showResult` | Màn hình "Đang chấm bài…" hỏi lại 2 giây/lần |
| `StaffSessionsController.onRefresh` | `StatisticsService.summarize(actor, session, now)` | Đếm số thí sinh / đã vào thi / đã nộp |
| `StaffSessionsController.onResults` | `SessionResultsController.init(ws, session)` | Mở bảng kết quả |
| Mọi `Jdbc...Dao` của phần 3 | `ConnectionManager.execute / inTransaction`, `JdbcErrors.translate` | Mở kết nối, đổi `SQLException` thành `DuplicateKeyException`, `SessionRuleException(SUBMISSION_LOCKED)`, `DataAccessException` |
| `JdbcStudentExamDao` (tài khoản `oes_client`) | `db/security.sql`: quyền, khung nhìn, 4 trigger | Phần 3 *dùng*; phần 4 *giải thích chi tiết* trigger |
| `MemorySessionDao`, `MemoryStudentExamDao` | `InMemoryDatabase`, `ManualClock`, `SubmissionRow` | Bản bộ nhớ cho kiểm thử |

---

## 3. Thứ tự đọc mã

Đọc theo thứ tự này: từ đối tượng giá trị nhỏ nhất → service → DAO → giao diện.

1. `oes/model/SessionPhase.java` – ba trạng thái của ca so với giờ hiện tại, có `displayName()` tiếng Việt.
2. `oes/model/SessionWindow.java` – giờ mở/đóng chung, mọi phép tính thời gian, `ensureOpen` ném lỗi có lý do.
3. `oes/model/SessionPolicy.java` – ba cờ trộn câu / trộn phương án / cho xem đáp án, gom thành một đối tượng.
4. `oes/model/PaperShuffler.java` – trộn có seed, generic, vì sao vào lại vẫn đúng thứ tự cũ.
5. `oes/model/ExamSession.java` – ghép đề + khung giờ + tùy chọn + ghi danh, phía giảng viên.
6. `oes/model/ViolationPolicy.java` – quy chế 5 lần, gộp 3 giây; câu quy chế hiện trên màn hình.
7. `oes/client/PaperOption.java`, `PaperItem.java`, `ExamPaper.java` – đề trên máy thí sinh, không có đáp án.
8. `oes/client/SessionInfo.java`, `SessionEntry.java` – thông tin ca cho thí sinh; quy tắc bật nút "Vào thi".
9. `oes/client/AttemptState.java`, `ExamAttempt.java` – bản sao trạng thái bài làm, an toàn đa luồng.
10. `oes/client/AnswerSheet.java`, `AnswerFormat.java` – câu trả lời đang hiện trên màn hình; định dạng `"12,14"`.
11. `oes/client/ServerClock.java` – giờ máy chủ tính bằng `nanoTime`.
12. `oes/client/ReviewItem.java` – một dòng xem lại sau khi thi.
13. `oes/dao/ExamSessionDao.java`, `oes/dao/StudentExamDao.java` – hai interface, đọc phần chú thích của từng hàm.
14. `oes/service/ExamSessionService.java` – mở ca thi, ghi danh, khóa đề.
15. `oes/service/StudentExamService.java` – **tệp quan trọng nhất**: `enter`, `saveAnswer`, `submit`, `refresh`, `requireOwn`.
16. `oes/service/ProctoringService.java` – báo vi phạm, gộp, kết thúc bài.
17. `oes/dao/jdbc/JdbcStudentExamDao.java` – mọi câu SQL của máy thí sinh (đọc kèm `db/security.sql`).
18. `oes/dao/jdbc/JdbcExamSessionDao.java` – chèn ca thi + ghi danh trong một giao dịch, `SELECT NOW()`.
19. `oes/dao/memory/MemoryStudentExamDao.java`, `MemorySessionDao.java` – bản bộ nhớ làm đúng việc của khung nhìn, trigger.
20. `db/schema.sql` (bảng `exam_session`, `session_student`, `submission`, `answer`) và `db/security.sql` (mục 1, 3, 4).
21. `oes/ui/BackgroundTasks.java` (phần 1) – cách chạy việc trên luồng nền.
22. `oes/ui/AnswerSaver.java` – hàng đợi lưu bài.
23. `oes/ui/ExamProctor.java` – toàn màn hình, nghe 3 sự kiện, hộp thoại không tính vi phạm.
24. `oes/ui/controller/SessionListController.java` + `fxml/session-list.fxml` – trang chủ ứng dụng thi.
25. `oes/ui/controller/RulesController.java` + `fxml/rules.fxml` – xác nhận quy chế.
26. `oes/ui/controller/ExamController.java` + `fxml/exam.fxml` – màn hình làm bài, nối mọi thứ lại.
27. `oes/ui/controller/SessionFormController.java` + `fxml/session-form.fxml` – giảng viên mở ca thi.
28. `oes/ui/controller/StaffSessionsController.java` + `fxml/staff-sessions.fxml` – danh sách ca thi phía giảng viên.
29. `oes/ui/AppConfig.java`, `oes/ui/controller/ServerSetupController.java` + `fxml/server-setup.fxml` – tìm/ghi tệp cấu hình.
30. `test/SessionWindowTest`, `PaperShufflerTest`, `ViolationPolicyTest`, `ClientModelTest`, `SubmissionFlowTest` – đọc tên phương thức kiểm thử như đọc đặc tả.

---

## 4. Kiến thức Java/OOP nền

Chỉ những khái niệm thật sự xuất hiện trong phần 3, giải thích bằng chính mã của phần.

### 4.1. `record` và compact constructor

Record là lớp dữ liệu **bất biến**: các trường là `final`, Java tự sinh constructor, getter (tên trùng tên trường, không
có chữ `get`), `equals`, `hashCode`, `toString`. Phần 3 dùng record cho mọi *đối tượng giá trị*: `SessionWindow`,
`SessionPolicy`, `SessionInfo`, `SessionEntry`, `PaperItem`, `PaperOption`, `ExamAttempt`, `ReviewItem`,
`ProctoringService.ViolationResult`, `AppConfig.ServerSettings`.

*Compact constructor* là constructor không ghi danh sách tham số; dùng để kiểm tra dữ liệu trước khi gán:

`oes/model/SessionWindow.java`
```java
public record SessionWindow(LocalDateTime openTime, LocalDateTime closeTime) {
    // ...
    public SessionWindow {
        Objects.requireNonNull(openTime, "openTime");
        Objects.requireNonNull(closeTime, "closeTime");
        ValidationException.require(closeTime.isAfter(openTime), "SESSION_TIME",
                "Giờ đóng ca thi phải sau giờ mở.");
    }
```

Hệ quả: **không thể tồn tại** một `SessionWindow` có giờ đóng trước giờ mở – mọi nơi nhận `SessionWindow` không cần
kiểm tra lại.

Trong compact constructor có thể gán lại tham số trước khi Java gán vào trường. `PaperItem` dùng điều này để giữ bản
chỉ đọc của danh sách (quy ước "trả về bản chỉ đọc của collection" trong `CLAUDE.md`):

`oes/client/PaperItem.java`
```java
    public PaperItem {
        Objects.requireNonNull(type, "type");
        options = List.copyOf(options);
    }
```

Record cũng có thể khai báo **cục bộ trong phương thức** khi chỉ cần một chỗ: `record Head(...)` trong
`JdbcStudentExamDao.loadPaper`, `record Sync(...)` trong `ExamController.resync`, `record Choice(...)` trong
`ExamController`, `record Entered(...)` trong `RulesController` (gói hai kết quả của một lượt chạy nền).

### 4.2. `enum` có dữ liệu, `EnumMap`

`SessionPhase` là enum có trường `displayName` và constructor riêng:

`oes/model/SessionPhase.java`
```java
public enum SessionPhase {
    UPCOMING("Sắp diễn ra"),
    OPEN("Đang mở"),
    CLOSED("Đã kết thúc");

    private final String displayName;
```

`StudentExamService.sessionsOf` dùng `EnumMap<SessionPhase, List<SessionInfo>>`: map có khóa là enum, duyệt theo đúng
thứ tự khai báo (sắp diễn ra → đang mở → đã kết thúc) và nhanh hơn `HashMap` vì bên trong là một mảng.

### 4.3. Lớp tiện ích `final` + constructor `private`; hàm generic

`PaperShuffler` và `AnswerFormat` chỉ có hàm `static`, nên khai báo `final class` và constructor `private` để không ai
tạo đối tượng hay kế thừa. Hàm trộn là **generic**:

`oes/model/PaperShuffler.java`
```java
    /** Trả về bản sao đã trộn; danh sách gốc giữ nguyên. */
    public static <T> List<T> shuffle(List<T> items, long seed) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy, new Random(seed));
        return copy;
    }
```

`<T>` nghĩa là hàm làm việc với danh sách phần tử kiểu bất kỳ: `ExamItem` (phía giảng viên, có đáp án), `PaperItem`
(phía thí sinh, không có đáp án), `AnswerOption`, `PaperOption`. Một thuật toán dùng cho cả hai phía nên hai phía không
thể lệch nhau.

### 4.4. `Optional`

`StudentExamDao.findAttempt` trả `Optional<AttemptState>`: "có thể chưa có bài". Nơi gọi buộc phải xử lý cả hai trường
hợp, ví dụ trong `StudentExamService.enter`:

```java
        AttemptState state = existing.orElseGet(() -> create(student, sessionId, now));
```

`orElseGet` chỉ gọi `create` khi chưa có bài (khác `orElse`, vốn luôn tính giá trị trước).

### 4.5. Đa luồng: `synchronized`, `ConcurrentHashMap`, bản chép

Ứng dụng thi có hai loại luồng: **luồng giao diện** (JavaFX Application Thread – vẽ màn hình, nhận sự kiện bấm) và
**luồng nền** (`oes-bg-1`…`oes-bg-4` của `BackgroundTasks` – gọi CSDL). Khi hai luồng cùng đụng một đối tượng có thể
sửa, phải đồng bộ:

- `AttemptState`: mọi hàm `synchronized` – service ghi trên luồng nền (`recordAnswer`, `setViolationCount`,
  `markSubmitted`) trong lúc màn hình đọc trên luồng giao diện. `getAnswers()` trả `Map.copyOf(answers)` (bản chép tại
  thời điểm gọi) để nơi nhận duyệt map mà không bị luồng khác sửa giữa chừng.
- `ProctoringService.lastCounted` là `ConcurrentHashMap` vì có thể có hai lượt báo vi phạm chạy song song trên hai
  luồng nền.
- `AnswerSaver`: hai `Map` hàng đợi chỉ được đụng tới trong khối/hàm `synchronized`, vì luồng giao diện thêm vào
  (`answerChanged`) còn luồng nền lấy ra (`drain`).
- `ServerClock.now()` và `resync` là `synchronized` vì hai trường `serverBase`, `nanoBase` phải đổi cùng nhau.

### 4.6. Luồng giao diện JavaFX và `BackgroundTasks` (phần 1)

Không bao giờ gọi CSDL trên luồng giao diện (câu hỏi chung số 9). Phần 3 dùng hai dạng của `BackgroundTasks.run`:

`oes/ui/BackgroundTasks.java`
```java
    /** Dạng đầy đủ: nơi gọi tự xử lý lỗi. */
    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        // Task giữ ngoại lệ gốc ở getException(), không bọc thêm lớp nào
        task.setOnFailed(e -> onError.accept(task.getException()));
        POOL.execute(task);
    }
```

- `run(work, onSuccess, onError)`: màn hình làm bài dùng dạng này vì phải **tự** xử lý lỗi mạng (hiện dải vàng, thử lại)
  thay vì bật hộp thoại – hộp thoại làm mất tiêu điểm và bị tính là rời cửa sổ.
- `run(busy, work, onSuccess)`: các màn hình khác (danh sách ca, quy chế, mở ca thi) dùng dạng này: vô hiệu hóa khung
  nhập trong lúc chạy, lỗi tự hiện qua `ErrorDialogs`.

`onSuccess`/`onError` luôn chạy lại trên luồng giao diện (`Task` lo việc đó), nên trong đó được sửa `Label`, chuyển màn hình.

### 4.7. `Timeline`, `KeyFrame`, `PauseTransition`

Đều là bộ hẹn giờ của JavaFX, chạy **trên luồng giao diện** (không cần `Platform.runLater`):

- `Timeline` + `KeyFrame` lặp lại: đồng hồ đếm ngược 1 giây/lần trong `ExamController.init`:
  ```java
          ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> onTick()));
          ticker.setCycleCount(Timeline.INDEFINITE);
          ticker.play();
  ```
- `PauseTransition` chạy **một lần** sau khoảng chờ. `playFromStart()` khởi động lại từ đầu nếu đang chạy – đây chính là
  kỹ thuật **debounce** (chờ người dùng ngừng thao tác rồi mới làm) trong `AnswerSaver`: mỗi phím gõ ở câu điền khuyết
  gọi `debounce.playFromStart()`, nên chỉ khi ngừng gõ 500 ms thì `flush()` mới chạy.
- `PauseTransition` cũng dùng cho thử lại sau 3 giây (`AnswerSaver.retry`, các nhánh lỗi trong `ExamController`) và lưới
  an toàn 2 giây của `ExamProctor`.

Chú ý: `Duration` ở các lớp giao diện là `javafx.util.Duration`; ở `SessionWindow`, `ViolationPolicy` là
`java.time.Duration` (model không được import `javafx`).

### 4.8. Property, listener, binding

JavaFX cho phép "nghe" khi một thuộc tính đổi giá trị. `ExamProctor` nghe ba thuộc tính của `Stage`:

```java
        stage.fullScreenProperty().addListener(fullScreenListener);
        stage.focusedProperty().addListener(focusListener);
        stage.iconifiedProperty().addListener(iconifiedListener);
```

Đây là mẫu **Observer**. *Binding* là nối hai thuộc tính để chúng tự đồng bộ:

- `RulesController.init`: `startButton.disableProperty().bind(agreeBox.selectedProperty().not());` – nút "Bắt đầu làm bài"
  chỉ bật khi đã tick đồng ý, không cần viết sự kiện nào.
- `ExamController.init`: `offlineBanner.visibleProperty().bind(saver.offlineProperty());` – dải vàng tự hiện/ẩn theo cờ
  `offline` của `AnswerSaver`.

### 4.9. `showAndWait` và vòng lặp sự kiện lồng nhau

`Alert.showAndWait()` chặn **dòng mã** gọi nó cho tới khi hộp thoại đóng, nhưng không chặn giao diện: JavaFX chạy một
vòng lặp sự kiện lồng bên trong. Vì thế trong `ExamProctor.showDialog`, khối `finally` chạy **sau** khi thí sinh bấm
"Quay lại làm bài", và trong lúc hộp thoại mở, các sự kiện mất tiêu điểm vẫn tới – phải có cờ `paused` để bỏ qua chúng.

### 4.10. Lambda, tham chiếu phương thức, interface hàm

- `ServerClock(LocalDateTime serverNow)` gọi `this(serverNow, System::nanoTime)`: `System::nanoTime` là một
  `LongSupplier` (interface hàm trả `long`). Kiểm thử truyền nguồn giả `nanos::get`.
- `ExamProctor` nhận `Consumer<Instant> onLeave`; `ExamController` truyền `this::onLeave`.
- `AnswerSaver.Sink` là interface tự định nghĩa hai hàm; `ExamController.init` cài đặt nó bằng lớp vô danh gọi
  `client.exams.saveAnswer(...)` / `client.exams.setFlag(...)`.

### 4.11. Pattern matching `instanceof`, switch biểu thức

- `AnswerSaver.flush`: `if (error instanceof SessionRuleException locked)` – vừa kiểm tra kiểu vừa ép kiểu vào biến `locked`.
- `SessionListController.init`: `user instanceof Student s ? ...` để lấy mã sinh viên.
- `ExamController.show` dùng switch biểu thức trên `item.type()` để dựng ô trả lời theo loại câu; `ReviewItem.toText`,
  `JdbcStudentExamDao.loadReview` cũng vậy. Switch biểu thức trên enum bắt buộc phủ hết các giá trị, thêm loại câu mới
  mà quên chỗ này thì **không biên dịch được** – an toàn hơn chuỗi `if`.

### 4.12. JDBC: `PreparedStatement`, try-with-resources, khóa tự sinh

Mọi câu SQL dùng tham số `?` và nằm trong `try (...)` để tự đóng. Hai điểm riêng của phần 3:

- `Statement.RETURN_GENERATED_KEYS` + `getGeneratedKeys()` để lấy `id` vừa tự tăng của `exam_session` / `submission`.
- `ps.addBatch()` + `executeBatch()` trong `JdbcExamSessionDao.insert` để chèn nhiều dòng ghi danh bằng một lượt gửi.

---

## 5. Từng lớp chính

### 5.1. `SessionWindow` (record, gói `model`)

**Vai trò.** Khung giờ của một ca: `openTime`, `closeTime`. Là *nơi duy nhất* trả lời "ca đang mở không", "còn bao
nhiêu giây", dùng chung cho máy giảng viên (`ExamSession.getWindow()`) và máy thí sinh (`SessionInfo.window()`).

**Bất biến.** Giờ đóng sau giờ mở (compact constructor, mã lỗi `SESSION_TIME`). CSDL kiểm tra thêm lần nữa bằng
`CHECK (close_time > open_time)` trong bảng `exam_session`.

**Phương thức – mọi hàm nhận "bây giờ" từ ngoài vào:**

`oes/model/SessionWindow.java`
```java
    public boolean isUpcoming(LocalDateTime now) {
        return now.isBefore(openTime);
    }

    /** Mở từ đúng giờ mở (tính cả giây đó) tới trước giờ đóng. */
    public boolean isOpen(LocalDateTime now) {
        return !now.isBefore(openTime) && now.isBefore(closeTime);
    }

    public boolean isClosed(LocalDateTime now) {
        return !now.isBefore(closeTime);
    }

    /** Nhóm hiển thị trên danh sách ca thi của thí sinh (FR-20) và của giảng viên. */
    public SessionPhase phaseAt(LocalDateTime now) {
        if (isUpcoming(now)) {
            return SessionPhase.UPCOMING;
        }
        return isClosed(now) ? SessionPhase.CLOSED : SessionPhase.OPEN;
    }
```

- Khoảng mở là **nửa mở** `[openTime, closeTime)`: đúng giây mở là đã mở, đúng giây đóng là đã đóng
  (`SessionWindowTest.phânNhómTheoGiờMáyChủ` kiểm tra cả hai biên). Ba hàm `isUpcoming`, `isOpen`, `isClosed` vì thế
  luôn có **đúng một** hàm trả `true` – không có khe hở hay chồng lấn.
- `remainingSeconds(now)` = `Math.max(0, Duration.between(now, closeTime).getSeconds())`: thời gian còn lại tính tới
  **giờ đóng chung**, không phải "60 phút kể từ lúc thí sinh vào" → quyết định số 2, BR-03: vào muộn thì còn ít thời gian
  hơn (`SessionWindowTest.vàoMuộnThìCònÍtThờiGianHơn`). `Math.max(0, ...)` để đồng hồ không hiện số âm.
- `isAlmostOver(now)` = `isOpen(now) && remainingSeconds(now) <= ALMOST_OVER.getSeconds()`, với hằng
  `ALMOST_OVER = Duration.ofMinutes(5)` (FR-23). Có `isOpen` để ca đã đóng không bị coi là "sắp hết giờ".
- `formatRemaining(seconds)` (static): `"1:05:09"` khi còn ≥ 1 giờ, `"05:09"` khi dưới 1 giờ.
- `ensureOpen(now)` (BR-01, phần thời gian): ném `SessionRuleException` với **lý do cụ thể**:

```java
    public void ensureOpen(LocalDateTime now) {
        if (isUpcoming(now)) {
            throw new SessionRuleException(SessionRuleException.NOT_OPEN_YET,
                    "Ca thi chưa mở. Giờ mở: " + openTime.format(FMT) + ".");
        }
        if (isClosed(now)) {
            throw new SessionRuleException(SessionRuleException.CLOSED,
                    "Ca thi đã kết thúc lúc " + closeTime.format(FMT) + ".");
        }
    }
```

Mã lỗi thật mà nhật ký ghi là `SESSION_NOT_OPEN_YET` và `SESSION_CLOSED` (giá trị của hai hằng `NOT_OPEN_YET`, `CLOSED`).

**Vì sao thiết kế vậy.**
- *Vì sao không gọi `LocalDateTime.now()` bên trong?* Vì giờ chuẩn phải lấy từ máy chủ CSDL (quyết định số 6). Nếu
  `isOpen()` tự lấy giờ máy trạm thì thí sinh chỉnh đồng hồ Windows là vào được ca chưa mở. Nhận `now` từ ngoài còn giúp
  kiểm thử: truyền một thời điểm cố định là kiểm tra được mọi biên, không phải chờ đồng hồ thật.
- *Vì sao tách khỏi `ExamSession` (SRP)?* Máy thí sinh **không được có** `ExamSession` (nó chứa `Exam` có đáp án) nhưng
  vẫn cần tính giờ. Tách ra thì hai phía dùng chung đúng một đoạn mã – không thể hiểu "đang mở" theo hai cách.
- *Vì sao record?* Khung giờ là giá trị, không có danh tính, không đổi sau khi tạo.

### 5.2. `SessionPolicy` (record) và `SessionPhase` (enum)

`oes/model/SessionPolicy.java`
```java
/**
 * Các tùy chọn giảng viên chọn khi mở ca thi (FR-18).
 * Gom ba cờ boolean thành một đối tượng để hàm tạo ca thi không phải nhận một dãy true/false dễ truyền nhầm thứ tự.
 */
public record SessionPolicy(boolean shuffleQuestions, boolean shuffleOptions, boolean showAnswer) {
}
```

**Vì sao.** Mẫu *Parameter Object*: `createSession(actor, exam, window, policy, students)` thay vì
`createSession(actor, exam, open, close, true, false, true, students)` – ba `boolean` liền nhau rất dễ truyền nhầm thứ
tự mà trình biên dịch không phát hiện. `showAnswer` chỉ có tác dụng **sau khi ca đóng** (khung nhìn `v_review` lọc
`close_time <= NOW() AND show_answer`).

`SessionPhase` (`UPCOMING`, `OPEN`, `CLOSED`) là kết quả của `SessionWindow.phaseAt`. Có `displayName()` tiếng Việt
("Sắp diễn ra", "Đang mở", "Đã kết thúc") cho cột "Trạng thái" của `StaffSessionsController`; `toString()` giữ nguyên
tên hằng để nhật ký không đổi.

### 5.3. `PaperShuffler` (lớp tiện ích)

`oes/model/PaperShuffler.java`
```java
    public static long seedFor(int sessionId, int studentId) {
        return Objects.hash(sessionId, studentId);
    }

    /** Seed riêng cho phương án của từng câu, để các câu không bị đảo phương án giống hệt nhau. */
    public static long optionSeed(long paperSeed, int questionId) {
        return paperSeed * 31L + questionId;
    }
```

**Ý tưởng then chốt: trộn có seed (hạt giống).** `new Random(seed)` sinh ra **cùng một dãy số** mỗi lần được tạo với
cùng `seed` (thuật toán của `java.util.Random` được ghi cố định trong tài liệu Java). Vì vậy `Collections.shuffle(copy,
new Random(seed))` với cùng danh sách ban đầu và cùng seed luôn cho **cùng thứ tự**.

- Seed tính từ `(sessionId, studentId)` → cùng thí sinh, cùng ca thì luôn ra cùng thứ tự: **thoát ra vào lại, đổi sang máy
  khác vẫn thấy đề y như cũ** mà không cần lưu thứ tự vào CSDL.
- Trong một ca, `sessionId` cố định nên seed khác nhau theo `studentId` → hai bạn ngồi cạnh nhau thấy thứ tự khác
  (`PaperShufflerTest.khácThíSinhKhácThứTự`: 10 thí sinh, 20 câu, 10 thứ tự khác nhau).
- `Objects.hash(a, b)` = `31 * (31 + a) + b`, trả `int` rồi nới thành `long`. Hai cặp *khác ca* có thể trùng seed, nhưng
  không sao vì seed chỉ dùng để so trong cùng một ca.
- **Seed phương án** `optionSeed = paperSeed * 31L + questionId`: nếu mọi câu dùng chung seed của đề thì các câu có
  cùng số phương án sẽ bị đảo theo **cùng một kiểu** (đáp án ở vị trí 1 của mọi câu đều nhảy sang vị trí 3…). Cộng
  `questionId` cho mỗi câu một seed riêng.
- `shuffle` trả **bản sao**, không đụng danh sách gốc (`PaperShufflerTest.danhSáchGốcKhôngBịĐổi`) – `Exam.getItems()`
  dùng chung cho mọi thí sinh, trộn tại chỗ là hỏng đề của người khác.

**Điều kiện để hai phía ra cùng thứ tự:** danh sách *ban đầu* phải cùng thứ tự. Phía giảng viên: `Exam` sắp câu theo
`order`; phía thí sinh: `v_exam_paper` đọc `ORDER BY item_order, opt_order`. `PaperShufflerTest.haiPhíaThấyCùngThứTựCâuVàPhươngÁn`
chứng minh `ExamSession.buildPaperFor` / `optionsFor` (giảng viên) và `ExamPaper.shuffledFor` (thí sinh) ra cùng kết quả.

> Lưu ý trung thực: hiện tại màn hình giảng viên **không** hiển thị đề riêng của từng thí sinh – `buildPaperFor` và
> `optionsFor` chỉ được dùng trong kiểm thử. Việc chấm không phụ thuộc thứ tự hiển thị, vì câu trả lời luôn lưu theo
> **số thứ tự câu trong đề gốc** (`PaperItem.order`). Tính "hai phía cùng thứ tự" được giữ để sau này in đề riêng
> hoặc đối chiếu khiếu nại.

### 5.4. `ExamSession` (gói `model`, phía giảng viên)

**Vai trò.** Ghép `Exam` + `SessionWindow` + `SessionPolicy` + tập mã thí sinh ghi danh (`Set<Integer>
enrolledStudentIds`, `HashSet` để tra O(1)).

- Constructor `ExamSession(int sessionId, Exam exam, SessionWindow window, SessionPolicy policy)` kiểm tra khác `null`
  và gọi `exam.validateForUse()` – không tạo được ca thi từ một đề rỗng.
- `assignId(id)` chỉ gán **một lần** (đã có mã thì ném `IllegalStateException("Ca thi đã có mã " + ...)`), theo quy ước
  của `CLAUDE.md`: đối tượng tạo với mã `0`, DAO chèn xong mới gán mã thật.
- `enroll`, `enrollAll`, `enrollById`, `isEnrolled`, `getEnrolledStudentIds()` (trả `Collections.unmodifiableSet`).
- `ensureCanEnter(s, now)`: BR-01 đầy đủ (ghi danh + giờ). `buildPaperFor(s)`, `optionsFor(s, q)`: đề có đáp án theo
  thứ tự của thí sinh `s`.

**Vì sao chỉ "ghép".** Từ M5 (DESIGN-CHANGES Phase 1 mục 2), giờ nằm ở `SessionWindow`, tùy chọn ở `SessionPolicy`,
thuật toán trộn ở `PaperShuffler`, việc "đóng ca và nộp hết bài" thành một câu `UPDATE` trong `GradingDao.closeExpired`.
`ExamSession` còn lại rất mỏng – một lý do để thay đổi (SRP).

### 5.5. `ViolationPolicy` (gói `model`)

`oes/model/ViolationPolicy.java`
```java
    public static final int MAX_VIOLATIONS = 5;

    /**
     * Một lần Alt+Tab thường làm JavaFX phát liền hai, ba sự kiện (thoát toàn màn hình, mất tiêu điểm...).
     * Các sự kiện cách nhau dưới khoảng này được tính là một lần vi phạm.
     */
    public static final Duration MERGE_WINDOW = Duration.ofSeconds(3);
    // ...
    public boolean isMerged(Instant lastCounted, Instant event) {
        return lastCounted != null && Duration.between(lastCounted, event).compareTo(mergeWindow) < 0;
    }

    /** Đủ số lần thì kết thúc bài ngay. */
    public boolean isTerminal(int violationCount) {
        return violationCount >= maxViolations;
    }
```

- `isMerged`: so với **lần được tính gần nhất** (không phải sự kiện gần nhất); `< 0` nghĩa là *dưới* 3 giây – đúng 3
  giây là lần mới (`ViolationPolicyTest.sựKiệnTrongBaGiâyĐượcGộp`: 2999 ms gộp, 3 s không gộp, `null` không gộp).
- `isTerminal` dùng `>=` chứ không `==`: hai máy cùng tài khoản cùng báo có thể đẩy bộ đếm lên 6 – vẫn phải là kết thúc.
- `remaining(count)` = `Math.max(0, maxViolations - count)`.
- `rulesText()` sinh câu quy chế **từ chính hai con số đang áp dụng** – đổi quy chế thì màn hình xác nhận tự đổi theo,
  không có chỗ nào ghi cứng "5 lần" trong câu quy chế.
- Constructor `ViolationPolicy(int maxViolations, Duration mergeWindow)` từ chối `maxViolations <= 0` hoặc khoảng âm;
  `standard()` trả quy chế đã chốt (5, 3 giây).

**Vì sao lớp riêng.** Ở M4, `MAX_VIOLATIONS` nằm trong `Submission`, khoảng gộp nằm trong `ProctoringService`. Gom về
một lớp (SRP) thì quy chế đọc ở một chỗ, kiểm thử không cần CSDL, và `ProctoringService` nhận quy chế qua constructor
(DIP – xem mục 9).

### 5.6. Gói `client` – mô hình phía thí sinh **không có đáp án**

**Vì sao cần gói riêng (NFR-04).** Lớp `Question` có `isCorrect`, `AnswerOption` có `isCorrect`. Chỉ cần một đối tượng
`Question` tới được máy thí sinh là đáp án đã tới. Gói `client` định nghĩa lại những gì máy thí sinh cần, **bỏ hẳn
trường đáp án**, nên việc lộ đáp án *không viết ra được*: kiểu dữ liệu không có chỗ để chứa nó.

`oes/client/PaperOption.java`
```java
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
```

Ba lớp phòng thủ chồng lên nhau: (1) `ClientContext` không có `QuestionDao`/`ExamDao`; (2) kiểu `PaperOption` không có
trường đáp án; (3) tài khoản `oes_client` không có quyền `SELECT` trên `question`, `answer_option` – dù ai đó sửa mã
ứng dụng thi cũng không đọc được.

| Lớp | Loại | Giữ gì | Ghi chú quan trọng |
|---|---|---|---|
| `SessionInfo` | record | `sessionId`, `examId`, `examTitle`, `subjectCode`, `subjectName`, `window`, `policy`, `totalPoint`, `questionCount` | Đọc từ `v_session_for_student`; không có đề, không có đáp án |
| `SessionEntry` | record | `session`, `phase`, `attempt` (`null` nếu chưa vào) | `canEnter()`, `isResuming()`, `canViewResult()` – quy tắc bật nút đặt ở model, màn hình chỉ đọc |
| `PaperItem` | record | `order` (số câu **gốc**), `point`, `questionId`, `type`, `content`, `options` | `withOptions(...)` trả bản mới với phương án đã trộn |
| `PaperOption` | record | `optionId`, `content` | Không có cờ đúng/sai |
| `ExamPaper` | class | `items` (thứ tự hiển thị), `byOrder` (tra theo số câu gốc) | `shuffledFor(sessionId, studentId, policy)`, `get(order)` ném `ITEM_NOT_FOUND`, `indexOf`, `totalPoint` |
| `ExamAttempt` | record | `session`, `paper`, `state` | Gói ba thứ màn hình làm bài cần; `submissionId()`, `unansweredCount()` |
| `AttemptState` | class | `submissionId`, `sessionId`, `studentId`, `status`, `violationCount`, `endReason`, `totalScore`, `answers`, `flagged` | Bản sao những gì CSDL **đã nhận**; mọi hàm `synchronized` |
| `AnswerSheet` | class | `responses`, `flagged` | Những gì thí sinh **đang thấy**, kể cả chưa lưu kịp; chỉ dùng trên luồng giao diện |
| `AnswerFormat` | tiện ích | – | `joinOptionIds` → `"12,14"` (tăng dần, rỗng thì `null`), `parseOptionIds` |
| `ServerClock` | class | `serverBase`, `nanoBase`, nguồn `nanoTime` | Mục 5.7 |
| `ReviewItem` | record | `order`, `point`, `type`, `content`, `options`, `response`, `earnedPoint`, `correctAnswer` | `correctAnswer == null` khi chưa được xem đáp án; `responseText()` đổi mã phương án thành nội dung |

**`SessionEntry` – quy tắc nút trên danh sách ca:**

`oes/client/SessionEntry.java`
```java
    /** Ca đang mở và thí sinh chưa nộp bài (chưa vào, hoặc đang làm dở). */
    public boolean canEnter() {
        return phase == SessionPhase.OPEN && (attempt == null || !attempt.isLocked());
    }

    public boolean isResuming() {
        return attempt != null && !attempt.isLocked();
    }

    /** Đã nộp bài (kể cả khi ca chưa đóng). */
    public boolean canViewResult() {
        return attempt != null && attempt.isLocked();
    }
```

**`ExamPaper.shuffledFor` – cùng `PaperShuffler`, cùng seed với phía giảng viên:**

`oes/client/ExamPaper.java`
```java
    public ExamPaper shuffledFor(int sessionId, int studentId, SessionPolicy policy) {
        long seed = PaperShuffler.seedFor(sessionId, studentId);
        List<PaperItem> list = policy.shuffleQuestions() ? PaperShuffler.shuffle(items, seed) : items;
        if (policy.shuffleOptions()) {
            list = list.stream()
                    .map(it -> it.withOptions(PaperShuffler.shuffle(it.options(),
                            PaperShuffler.optionSeed(seed, it.questionId()))))
                    .toList();
        }
        return new ExamPaper(list);
    }
```

**`AttemptState` vs `AnswerSheet` – vì sao hai đối tượng.** `AttemptState` là "CSDL đã nhận" (service cập nhật trên luồng
nền sau mỗi lần ghi thành công). `AnswerSheet` là "thí sinh đang thấy". Nếu dùng chung một đối tượng: mạng chậm, thí
sinh chọn A rồi đổi sang B; lượt lưu A về muộn sẽ ghi đè B trên màn hình (DESIGN-CHANGES Phase 6 mục 1). Tách ra thì
màn hình luôn đúng ý thí sinh, còn việc đưa B xuống CSDL do `AnswerSaver` lo. `AnswerSheet` được khởi tạo từ
`AttemptState` đã lưu – nhờ vậy vào lại bài cũ thì các câu đã làm hiện lại đầy đủ.

**`AttemptState.isLocked()`** = `status != SubmissionStatus.IN_PROGRESS`: đã `SUBMITTED` hay `GRADED` đều là khóa.
`totalScore` là `null` khi chưa chấm (`JdbcStudentExamDao.findAttempt` chỉ đọc điểm khi `status == GRADED`, vì cột
`total_score` mặc định 0 – không phân biệt được "0 điểm" với "chưa chấm").

### 5.7. `ServerClock` – giờ máy chủ tính bằng `nanoTime`

`oes/client/ServerClock.java`
```java
    public ServerClock(LocalDateTime serverNow) {
        this(serverNow, System::nanoTime);
    }

    /** Cho kiểm thử: truyền nguồn nanoTime giả. */
    public ServerClock(LocalDateTime serverNow, LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
        resync(serverNow);
    }

    public synchronized LocalDateTime now() {
        return serverBase.plusNanos(nanoTime.getAsLong() - nanoBase);
    }

    public synchronized void resync(LocalDateTime serverNow) {
        this.serverBase = serverNow;
        this.nanoBase = nanoTime.getAsLong();
    }
```

**Cách tính.** Lấy giờ máy chủ một lần (`SELECT NOW()`), ghi kèm `System.nanoTime()` lúc đó. Về sau: giờ máy chủ = giờ
đã lấy + (nanoTime bây giờ − nanoTime lúc lấy).

**Vì sao không dùng đồng hồ máy trạm (`LocalDateTime.now()`)?**
1. Thí sinh có thể chỉnh đồng hồ Windows để "thêm giờ" hoặc vào ca chưa mở.
2. 60 máy trong phòng không đồng bộ giờ với nhau; chỉ máy chủ CSDL là một nguồn chung.
3. Máy chủ còn là nơi **thực thi** giờ đóng (trigger so `close_time > NOW()`), nên đồng hồ trên màn hình phải khớp với nó.

**Vì sao `nanoTime` thì không bị ảnh hưởng?** `System.nanoTime()` chỉ đo **khoảng thời gian** từ một mốc tùy ý, không
gắn với giờ hệ điều hành; chỉnh đồng hồ không làm nó nhảy. Vì vậy không phải hỏi máy chủ mỗi giây (60 máy × 1 truy
vấn/giây là tải vô ích).

**Đồng bộ lại.** `ExamController` gọi `clock.resync(...)` mỗi `RESYNC_EVERY_SECONDS = 60` giây để bù sai lệch tích lũy
(`ClientModelTest.đồngHồMáyChủKhôngPhụThuộcGiờMáyTrạm` giả lập "máy trạm chạy chậm 10 giây" rồi đồng bộ).

**Sai số chấp nhận được.** `NOW()` chính xác tới giây, và mốc `nanoBase` lấy *sau* khi kết quả về (chậm vài chục ms).
Đồng hồ trên màn hình có thể lệch dưới một giây – không sao, vì **CSDL mới là nơi quyết định**: sau `close_time`
trigger từ chối ghi, máy chấm đóng bài.

### 5.8. `ExamSessionService` (máy giảng viên)

`oes/service/ExamSessionService.java`
```java
    public ExamSession createSession(User actor, Exam exam, SessionWindow window, SessionPolicy policy,
                                     Collection<Student> students) {
        Permissions.requireManage(actor, exam.getSubject());
        ValidationException.require(!students.isEmpty(), "NO_STUDENTS", "Ca thi chưa có thí sinh nào. Hãy chọn ít nhất một thí sinh, hoặc chọn một lớp đã có thí sinh.");
        ValidationException.require(window.closeTime().isAfter(sessionDao.serverTime()), "SESSION_IN_PAST",
                "Giờ đóng ca thi đã ở trong quá khứ. Hãy chọn giờ đóng sau thời điểm hiện tại.");

        ExamSession s = new ExamSession(0, exam, window, policy);
        s.enrollAll(students);
        ExamSession saved = sessionDao.insert(s);

        // BR-09: từ lúc có ca thi, đề không được sửa nữa
        if (!exam.isLocked()) {
            exam.lock();
            examDao.lock(exam.getExamId());
        }
        AuditLog.record(actor, "CREATE_SESSION", saved.getSessionId() + " đề " + exam.getExamId()
                + ", " + students.size() + " thí sinh");
        return saved;
    }
```

Thứ tự kiểm tra: **quyền trước** (không lộ thông tin cho người không có quyền) → có thí sinh → giờ đóng phải sau giờ
máy chủ (lại là giờ máy chủ, không phải giờ máy giảng viên). Giờ mở ở quá khứ thì **được** – giảng viên hay mở ca ngay
lúc thi. Mã lỗi: `SUBJECT_FORBIDDEN`, `NO_STUDENTS`, `SESSION_IN_PAST` (cộng `SESSION_TIME` từ `SessionWindow`).

Các hàm khác: `createSessionForClass` (FR-19, lớp phải cùng môn với đề – mã `CLASS_WRONG_SUBJECT`), `studentsOfClass`,
`allStudents`, `listOfExam`, `serverTime`, `get` (mã `SESSION_NOT_FOUND`).

### 5.9. `StudentExamService` (máy thí sinh) – lớp trọng tâm

Thay cho `SubmissionService` của M4. Khác biệt chính: **không chấm bài**. Constructor chỉ nhận một `StudentExamDao`.

**`sessionsOf` và `overview`.** Cả hai lấy `dao.serverTime()` một lần rồi phân nhóm bằng `s.window().phaseAt(now)`.
`overview` (màn hình dùng) kèm bài làm của từng ca thành `SessionEntry`; `sessionsOf` trả `EnumMap` ba nhóm (hiện chỉ
kiểm thử dùng).

**`enter` – vào thi (UC-13):**

`oes/service/StudentExamService.java`
```java
    public ExamAttempt enter(User actor, int sessionId) {
        Student student = Permissions.requireStudent(actor);
        SessionInfo session = loadSession(sessionId);
        LocalDateTime now = dao.serverTime();

        Optional<AttemptState> existing = dao.findAttempt(sessionId, student.getUserId());
        if (existing.isPresent() && existing.get().isLocked()) {
            throw new SessionRuleException(SessionRuleException.ALREADY_SUBMITTED,
                    "Bạn đã nộp bài của ca thi này. Mỗi thí sinh chỉ được thi một lần.");
        }
        if (!dao.isEnrolled(sessionId, student.getUserId())) {
            throw SessionRuleException.notEnrolled();
        }
        session.window().ensureOpen(now);

        AttemptState state = existing.orElseGet(() -> create(student, sessionId, now));
        ExamPaper paper = dao.loadPaper(session.examId())
                .shuffledFor(sessionId, student.getUserId(), session.policy());
        return new ExamAttempt(session, paper, state);
    }
```

Ba khả năng: chưa có bài → tạo mới; có bài đang dở → **mở lại đúng bài đó** (câu trả lời, câu đánh dấu, số lần vi phạm
giữ nguyên); bài đã khóa → `ALREADY_SUBMITTED`. Thứ tự kiểm tra có chủ ý: "đã nộp" đứng trước "giờ", nên thí sinh đã
nộp mà quay lại sau giờ đóng vẫn nhận câu báo đúng nhất ("đã nộp") thay vì "ca đã kết thúc". Vào lại bài dở vẫn phải
qua `ensureOpen`: hết giờ thì không vào làm tiếp được.

**`create` – hai máy cùng bấm "Vào thi":**

```java
    private AttemptState create(Student student, int sessionId, LocalDateTime now) {
        try {
            AttemptState created = dao.createAttempt(sessionId, student.getUserId(), now);
            AuditLog.record(student, "START_EXAM", "ca " + sessionId);
            return created;
        } catch (DuplicateKeyException e) {
            // Hai máy cùng bấm "Vào thi" gần như một lúc: máy kia đã tạo bài trước.
            // Không báo lỗi, chỉ mở lại bài vừa được tạo.
            return dao.findAttempt(sessionId, student.getUserId()).orElseThrow(() -> e);
        }
    }
```

Giữa lúc `findAttempt` trả "chưa có" và lúc `INSERT`, máy kia có thể đã chèn. Kiểm tra trong Java không chặn được khe
hở này; **ràng buộc `UNIQUE (session_id, student_id)` trong CSDL** mới chặn được: chỉ một `INSERT` thành công, cái
kia nhận lỗi trùng khóa (MySQL mã 1062) → `JdbcErrors.translate` đổi thành `DuplicateKeyException` → ở đây đọc lại bài
máy kia vừa tạo. Kiểm chứng: `JdbcIntegrationTest.haiMáyCùngBấmVàoThiChỉTạoMộtBài` (hai luồng cùng gọi `enter`, ra
cùng `submissionId`).

**`saveAnswer`, `setFlag`, `writableState`:**

```java
    public void saveAnswer(User actor, ExamAttempt attempt, int order, String response) {
        AttemptState state = writableState(actor, attempt, order);
        dao.saveAnswer(state.getSubmissionId(), order, response);   // CSDL ném SessionRuleException nếu đã hết giờ
        state.recordAnswer(order, response);
    }
```

`writableState` kiểm tra: đúng chủ (`requireOwn`), bài chưa khóa (`SUBMISSION_LOCKED` "Bài làm đã nộp, không thể sửa
câu trả lời."), số câu có trong đề (`ITEM_NOT_FOUND`). Chú ý thứ tự: **ghi CSDL trước, cập nhật bản sao sau** – CSDL từ
chối thì bản sao không bị sai.

**Vì sao service không tự kiểm tra hết giờ khi lưu?** Vì kiểm tra trên máy trạm luôn có thể bị vượt (ứng dụng bị sửa,
đồng hồ lệch). Nơi đáng tin là trigger `trg_answer_client_insert` / `trg_answer_client_update` so với `NOW()` của máy
chủ. Comment đầu lớp nói rõ: "Mọi kiểm tra ở đây đều được CSDL kiểm tra lại bằng trigger, phòng khi ứng dụng thi bị sửa."

**`submit` – nộp bài (FR-25):**

```java
    public AttemptState submit(User actor, ExamAttempt attempt, EndReason reason) {
        AttemptState state = requireOwn(actor, attempt);
        if (state.isLocked()) {
            return state;
        }
        if (!dao.markSubmitted(state.getSubmissionId(), reason, dao.serverTime())) {
            return refresh(actor, state.getSessionId());
        }
        state.markSubmitted(reason);
        AuditLog.record(actor, "SUBMIT_" + reason, "ca " + state.getSessionId());
        return state;
    }
```

- Chỉ chuyển `IN_PROGRESS → SUBMITTED`, không chấm. `GradingWorker` ở máy giảng viên chấm sau (NFR-04).
- Nộp lần hai không lỗi, trả trạng thái lần đầu (`SubmissionFlowTest.nộpHaiLầnGiữKếtQuảLầnĐầu`).
- `markSubmitted` trả `false` khi câu `UPDATE ... WHERE id = ? AND status = 'IN_PROGRESS'` không sửa dòng nào: máy khác đã
  nộp, hoặc máy chấm đã đóng ca. Khi đó **đọc lại trạng thái thật** – kết quả của bên nộp trước được giữ
  (`SubmissionFlowTest.máyKhácĐãNộpThìĐọcLạiTrạngTháiThật`). Đây là kiểu "so sánh rồi mới ghi" (compare-and-set) bằng
  điều kiện `WHERE`.
- Nhật ký ghi `SUBMIT_MANUAL`, `SUBMIT_TIME_OUT` hoặc `SUBMIT_VIOLATION`.

**`refresh`, `review`, `requireOwn`:**
- `refresh(actor, sessionId)`: đọc lại bài từ CSDL; chưa có bài thì `SessionRuleException("NOT_STARTED", "Bạn chưa vào ca
  thi này. Hãy vào thi từ danh sách ca thi.")`. Dùng ở màn hình chờ chấm, đồng bộ 60 giây, và khi `markSubmitted` thất bại.
- `review(actor, sessionId)`: chưa nộp thì `NOT_SUBMITTED` "Bạn chưa nộp bài nên chưa xem được kết quả."; đã nộp thì
  `dao.loadReview(...)`. Có đáp án hay không là do **CSDL** quyết định qua `v_review`.
- `requireOwn` (phạm vi gói – `ProctoringService` cùng gói dùng được):

```java
    AttemptState requireOwn(User actor, ExamAttempt attempt) {
        Student student = Permissions.requireStudent(actor);
        AttemptState state = attempt.state();
        if (state.getStudentId() != student.getUserId()) {
            throw new AccessDeniedException("NOT_OWNER", "Đây không phải bài làm của bạn.");
        }
        return state;
    }
```

Mới ở M5: thí sinh chỉ thao tác trên bài của chính mình (`SubmissionFlowTest.khôngThaoTácĐượcTrênBàiCủaNgườiKhác`).

### 5.10. `ProctoringService` – ghi nhận vi phạm

`oes/service/ProctoringService.java`
```java
    public ViolationResult report(User actor, ExamAttempt attempt, Instant eventTime) {
        AttemptState state = exams.requireOwn(actor, attempt);
        if (state.isLocked()) {
            return new ViolationResult(state.getViolationCount(), 0, state.isTerminatedByViolation(), false);
        }

        int id = state.getSubmissionId();
        if (policy.isMerged(lastCounted.get(id), eventTime)) {
            int current = state.getViolationCount();
            return new ViolationResult(current, policy.remaining(current), false, true);
        }

        int count = dao.increaseViolation(id);
        // Chỉ nhớ thời điểm sau khi CSDL đã ghi: nếu lệnh trên lỗi (mất mạng), màn hình gửi lại đúng thời điểm này
        // và lần gửi lại phải được đếm, không bị coi là sự kiện trùng.
        lastCounted.put(id, eventTime);
        if (count == state.getViolationCount()) {
            // CSDL không tăng bộ đếm: bài đã bị khóa ở nơi khác (máy chấm đóng ca, máy khác nộp).
            // Không báo "vi phạm lần N" giả; đọc lại trạng thái thật để màn hình chuyển sang kết quả.
            AttemptState fresh = exams.refresh(actor, state.getSessionId());
            state.markSubmitted(fresh.getEndReason());
            return new ViolationResult(fresh.getViolationCount(), 0, fresh.isTerminatedByViolation(), false);
        }
        state.setViolationCount(count);
        if (policy.isTerminal(count)) {
            exams.submit(actor, attempt, EndReason.VIOLATION);
            lastCounted.remove(id);
            AuditLog.record(actor, "TERMINATED_BY_VIOLATION", "ca " + state.getSessionId() + ", bài " + id);
            return new ViolationResult(count, 0, true, false);
        }
        return new ViolationResult(count, policy.remaining(count), false, false);
    }
```

- **Dữ liệu:** `dao`, `exams` (dùng lại `requireOwn`, `refresh`, `submit`), `policy`, và `lastCounted` – map *mã bài →
  thời điểm lần vi phạm được tính gần nhất* (`ConcurrentHashMap`).
- **Kết quả** `ViolationResult(count, remaining, terminated, merged)` là record lồng; màn hình dựa vào đó để: không làm gì
  (gộp), hiện cảnh báo "lần N/5", hay chuyển sang kết quả.
- **Vì sao tăng bộ đếm *trong CSDL*** (`increaseViolation`) thay vì `count + 1` ở Java rồi ghi xuống? Cách đọc–cộng–ghi mất
  lượt khi hai sự kiện tới gần như cùng lúc (cả hai đọc 2, cả hai ghi 3). Câu `UPDATE ... SET violation_count =
  violation_count + 1` là nguyên tử (mục 5.11).
- **Phát hiện bài đã khóa ở nơi khác** (DESIGN-CHANGES Phase 1 mục 9): `increaseViolation` có điều kiện `status =
  'IN_PROGRESS'`; không tăng được thì giá trị trả về bằng giá trị cũ → service biết bài đã khóa, đọc lại trạng thái thật.
  Kiểm chứng: `SubmissionFlowTest.báoViPhạmKhiBàiĐãBịMáyChấmĐóngThìKhôngĐếmThêm`.
- Lần thứ 5: gọi `exams.submit(..., EndReason.VIOLATION)` – bài khóa ngay, vẫn được chấm phần đã làm (quyết định số 5).
- **Lưu ý:** việc *gửi tuần tự* và *thử lại khi mất mạng* **không** nằm ở `ProctoringService` mà ở
  `ExamController.onLeave` (mục 5.14). Service chỉ xử lý một sự kiện mỗi lần gọi.

### 5.11. `StudentExamDao` và `JdbcStudentExamDao`

Interface `StudentExamDao` gom **mọi** thao tác CSDL của máy thí sinh (ISP): `findSession`, `findSessionsOf`,
`isEnrolled`, `loadPaper`, `findAttempt`, `createAttempt`, `saveAnswer`, `setFlag`, `increaseViolation`,
`markSubmitted`, `loadReview`, `serverTime`. Không có hàm nào trả đề có đáp án hay ghi điểm.

`JdbcStudentExamDao` chạy bằng tài khoản `oes_client`. Các câu SQL đáng nhớ:

| Hàm | Câu SQL (trích) | Ghi chú |
|---|---|---|
| `findSession` / `findSessionsOf` | `SELECT v.session_id, ... FROM v_session_for_student v` (+ `JOIN session_student ss ON ss.session_id = v.session_id WHERE ss.student_id = ? ORDER BY v.open_time`) | Chỉ qua khung nhìn |
| `isEnrolled` | `SELECT 1 FROM session_student WHERE session_id = ? AND student_id = ?` | |
| `loadPaper` | `SELECT item_order, point, question_id, type, content, option_id, option_content FROM v_exam_paper WHERE exam_id = ? ORDER BY item_order, opt_order` | Mỗi phương án một dòng; gom theo `item_order` |
| `findAttempt` | `SELECT id, status, violation_count, end_reason, total_score FROM submission WHERE session_id = ? AND student_id = ?` rồi `SELECT item_order, response, flagged FROM answer WHERE submission_id = ?` | |
| `createAttempt` | `INSERT INTO submission(session_id, student_id, started_at, status) VALUES (?,?,?,'IN_PROGRESS')` | Trùng thì `DuplicateKeyException` |
| `saveAnswer` | `INSERT INTO answer(submission_id, item_order, response) VALUES (?,?,?) ON DUPLICATE KEY UPDATE response = ?` | Ghi đè nếu đã có (thí sinh đổi ý) |
| `setFlag` | `INSERT INTO answer(submission_id, item_order, flagged) VALUES (?,?,?) ON DUPLICATE KEY UPDATE flagged = ?` | |
| `increaseViolation` | `UPDATE submission SET violation_count = LAST_INSERT_ID(violation_count + 1) WHERE id = ? AND status = 'IN_PROGRESS'` rồi `SELECT LAST_INSERT_ID()` | Nguyên tử |
| `markSubmitted` | `UPDATE submission SET status = 'SUBMITTED', submitted_at = ?, end_reason = ? WHERE id = ? AND status = 'IN_PROGRESS'` | Trả `executeUpdate() == 1` |
| `serverTime` | `SELECT NOW()` | Giờ chuẩn |

**`loadPaper` – gom nhiều dòng thành một câu.** Khung nhìn trả mỗi phương án một dòng (câu 4 phương án = 4 dòng). Hàm
dùng hai `LinkedHashMap` (giữ thứ tự chèn = thứ tự `item_order`), gặp `item_order` mới thì tạo phần đầu câu; câu
đúng/sai và điền khuyết có `option_id` `NULL` nên kiểm tra `rs.wasNull()`:

`oes/dao/jdbc/JdbcStudentExamDao.java`
```java
                while (rs.next()) {
                    int order = rs.getInt("item_order");
                    if (!heads.containsKey(order)) {   // dòng đầu tiên của một câu
                        heads.put(order, new Head(rs.getDouble("point"), rs.getInt("question_id"),
                                QuestionType.valueOf(rs.getString("type")), rs.getString("content")));
                        options.put(order, new ArrayList<>());
                    }
                    int optionId = rs.getInt("option_id");
                    if (!rs.wasNull()) {   // câu đúng/sai, điền khuyết có option_id NULL
                        options.get(order).add(new PaperOption(optionId, rs.getString("option_content")));
                    }
                }
```

**`increaseViolation` – `LAST_INSERT_ID(expr)`:**

```java
    /**
     * Tăng bộ đếm ngay trong câu UPDATE thay vì đọc lên, cộng một, rồi ghi xuống.
     * Cách đọc-sửa-ghi sẽ mất lượt đếm khi hai sự kiện tới gần như cùng lúc.
     * LAST_INSERT_ID(expr) là cách của MySQL để lấy lại giá trị vừa ghi mà không cần truy vấn thêm;
     * giá trị này gắn với từng kết nối nên luồng khác không đọc nhầm.
     */
```

`LAST_INSERT_ID(x)` vừa trả `x` vừa ghi nhớ `x` cho **kết nối hiện tại**; câu `SELECT LAST_INSERT_ID()` tiếp theo trên
cùng kết nối (cùng lần `cm.execute`) đọc lại đúng giá trị mình vừa ghi, dù máy khác cũng đang tăng. Nếu `UPDATE` không
sửa dòng nào (bài đã khóa) thì đọc `SELECT violation_count FROM submission WHERE id = ?` – giá trị cũ. Kiểm chứng:
`JdbcIntegrationTest.đếmViPhạmSongSongKhôngMấtLượt` (4 luồng cùng tăng, nhận đúng 1, 2, 3, 4).

**`loadReview`** ghép ba nguồn: đề (`v_exam_paper`), câu trả lời + điểm từng câu (`answer`), đáp án đúng (`v_review`).
`v_review` chỉ có dòng khi ca đã đóng và cho xem đáp án, nên trước đó `correctAnswer` luôn `null`. Câu điền khuyết nhiều
đáp án chấp nhận nối bằng `" / "`, câu nhiều đáp án nối bằng `","`.

### 5.12. `ExamSessionDao`, `JdbcExamSessionDao`, hai DAO bộ nhớ

- `ExamSessionDao`: `insert`, `findById`, `findByExam` (ca mở sau đứng trước), `serverTime`. Đã bỏ `findOfStudent`
  (sang `StudentExamDao`) – ISP.
- `JdbcExamSessionDao.insert` chạy trong **một giao dịch** (`cm.inTransaction`): `INSERT INTO exam_session(exam_id,
  open_time, close_time, shuffle_questions, shuffle_options, show_answer) VALUES (?,?,?,?,?,?)`, lấy khóa tự sinh,
  `s.assignId(...)`, rồi `INSERT INTO session_student(session_id, student_id) VALUES (?, ?)` theo lô. Lỗi giữa chừng thì
  không có "ca thi không thí sinh" nào sót lại.
- `JdbcExamSessionDao.findById` đọc dòng vào một record riêng `SessionRow` trong lúc còn kết nối, **đóng kết nối rồi**
  mới gọi `examDao.findById` – tránh giữ hai kết nối lồng nhau.
- `MemorySessionDao`, `MemoryStudentExamDao` (phạm vi gói, chỉ tạo qua `InMemoryDatabase`): mọi hàm `synchronized (db)`.
  `MemoryStudentExamDao` **bắt chước đúng** khung nhìn và trigger: `loadPaper` trả đề rỗng nếu ca chưa tới giờ mở và
  chép sang `PaperItem` không có đáp án (`toPaperItem`); `writableRow` làm việc của `trg_answer_client_*` (ném
  `SUBMISSION_LOCKED` khi bài khóa hoặc ca đã đóng); `loadReview` chỉ trả đáp án khi `showAnswer()` và ca đã đóng. Nhờ vậy
  kiểm thử nghiệp vụ không cần MySQL vẫn kiểm tra được BR-04 và NFR-04 (DESIGN-CHANGES Phase 1 mục 8).

### 5.13. `AnswerSaver` (gói `ui`) – hàng đợi lưu bài

`oes/ui/AnswerSaver.java`
```java
    private static final Duration DEBOUNCE = Duration.millis(500);
    private static final Duration RETRY = Duration.seconds(3);

    private final Sink sink;
    private final Consumer<SessionRuleException> onLocked;
    private final Map<Integer, String> answers = new LinkedHashMap<>();
    private final Map<Integer, Boolean> flags = new LinkedHashMap<>();
    private final PauseTransition debounce = new PauseTransition(DEBOUNCE);
    private final PauseTransition retry = new PauseTransition(RETRY);
    private final SimpleBooleanProperty offline = new SimpleBooleanProperty(false);
```

Bốn tính chất (FR-24, NFR-06):

1. **Debounce 500 ms.** `answerChanged(order, response)` cho vào map rồi `debounce.playFromStart()`. Gõ "extends" ở câu
   điền khuyết là 7 lần đổi, nhưng chỉ sinh **một** lượt ghi.
2. **Mỗi câu chỉ giữ giá trị mới nhất.** Hàng đợi là `Map` theo số câu, không phải danh sách: đổi ý ba lần lúc mất mạng
   thì khi có mạng chỉ gửi lần cuối.
3. **Mất kết nối thì thử lại sau 3 giây**, bật cờ `offline` (dải vàng "Mất kết nối, câu trả lời sẽ được lưu khi kết nối
   lại. Bạn cứ tiếp tục làm bài."). Thí sinh không bị chặn.
4. **Bài đã khóa thì dừng hẳn**, gọi `onLocked` – không thử lại mãi một việc CSDL sẽ luôn từ chối.

```java
    public void flush() {
        if (sending || stopped || !hasPending()) {
            return;
        }
        sending = true;
        BackgroundTasks.run(() -> {
            drain();
            return Boolean.TRUE;
        }, ok -> {
            sending = false;
            offline.set(false);
            flush();   // có thể đã có thay đổi mới trong lúc gửi
        }, error -> {
            sending = false;
            if (error instanceof SessionRuleException locked) {
                stop();
                onLocked.accept(locked);
            } else {
                offline.set(true);
                retry.playFromStart();
            }
        });
    }
```

- Cờ `sending` bảo đảm **chỉ một lượt gửi** chạy một lúc (cờ này chỉ đọc/ghi trên luồng giao diện nên không cần đồng bộ).
- `drain()` lấy từng mục ra gửi qua `sink`, và chỉ xóa mục khỏi hàng đợi bằng `removeIfUnchanged`: nếu trong lúc đang gửi
  thí sinh đổi ý tiếp (map đã có giá trị khác), mục đó **được giữ lại** để lượt sau gửi giá trị mới. Nếu xóa thẳng thì
  giá trị mới bị mất.
- `first(map)` chép mục ra (`AbstractMap.SimpleImmutableEntry`) – không giữ tham chiếu vào map đang bị luồng khác sửa; giá
  trị có thể `null` (thí sinh bỏ chọn hết).
- Cờ đánh dấu (`flagChanged`) gửi **ngay**, không debounce – hiếm khi đổi.
- `drain()` được gọi trực tiếp (trên luồng nền) trước khi nộp bài, kể cả sau `stop()`: "đẩy hết hàng đợi trước, rồi mới nộp".
- `AnswerSaver` không biết `StudentExamService`: nó chỉ biết interface `Sink` (hai hàm `saveAnswer`, `saveFlag`). Đây là
  DIP – kiểm thử hay thay nơi ghi không phải sửa lớp này.

### 5.14. `ExamProctor` (gói `ui`) – toàn màn hình và phát hiện rời cửa sổ

`oes/ui/ExamProctor.java`
```java
    public void start() {
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);   // Esc không thoát được
        stage.setFullScreenExitHint("");
        previousCloseHandler = stage.getOnCloseRequest();
        stage.setOnCloseRequest(Event::consume);                          // chặn Alt+F4 trong lúc thi
        stage.setFullScreen(true);
        stage.fullScreenProperty().addListener(fullScreenListener);
        stage.focusedProperty().addListener(focusListener);
        stage.iconifiedProperty().addListener(iconifiedListener);
        watching = true;
    }
```

- **Esc:** `KeyCombination.NO_MATCH` – không phím nào thoát toàn màn hình. **Alt+F4:** sinh sự kiện "yêu cầu đóng cửa sổ";
  `Event::consume` nuốt sự kiện nên cửa sổ không đóng. `stop()` trả lại bộ xử lý cũ (`previousCloseHandler`).
- **Ba sự kiện rời cửa sổ** cùng gọi `leave()`: thoát toàn màn hình (`fullScreenListener` khi `isFull == false`), mất
  tiêu điểm – Alt+Tab (`focusListener` khi `focused == false`), thu nhỏ (`iconifiedListener` khi `iconified == true`).
  Một lần Alt+Tab thường phát 2–3 sự kiện; gộp chúng là việc của `ViolationPolicy`, không phải của lớp này.
- **Không chặn được** Alt+Tab hay phím Windows – hệ điều hành xử lý trước ứng dụng. Ứng dụng chỉ **phát hiện**.

**Hộp thoại không được tính là rời cửa sổ** (DESIGN-CHANGES Phase 6 mục 2). Chính hộp thoại cảnh báo/xác nhận làm cửa
sổ chính mất tiêu điểm. Mọi hộp thoại trong lúc thi mở qua `showDialog`:

```java
    public void showDialog(Runnable show) {
        paused = true;
        try {
            show.run();
        } finally {
            resumePending = true;
            if (watching && !stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
            tryResume();
            resumeFallback.playFromStart();
        }
    }
```

Ba cờ: `watching` (đang thi), `paused` (đang có hộp thoại – `leave()` bỏ qua mọi sự kiện), `resumePending` (hộp thoại
đã đóng, chờ quay lại). `tryResume()` chỉ bật nghe lại khi cửa sổ **đã toàn màn hình và có tiêu điểm**, không phải ngay
lúc hộp thoại đóng – vì trên đường quay lại toàn màn hình JavaFX còn phát vài sự kiện nữa. Hai listener gọi `tryResume()`
mỗi khi cửa sổ có lại toàn màn hình/tiêu điểm.

**Lưới an toàn 2 giây** (`RESUME_FALLBACK = Duration.seconds(2)`): nếu sau 2 giây cửa sổ vẫn chưa có lại tiêu điểm
(trình quản lý cửa sổ lạ, hoặc thí sinh Alt+Tab ngay khi đóng hộp thoại), `forceResume()` vẫn bật nghe lại, và nếu lúc
đó thí sinh đang ở ngoài thì tính luôn **một lần rời cửa sổ**. Không có lưới này, một thủ thuật đơn giản sẽ tắt việc giám
sát vĩnh viễn.

### 5.15. Các controller phía thí sinh

**`SessionListController`** (`session-list.fxml`): `init` → `onRefresh()` →
`BackgroundTasks.run(page, () -> client.exams.overview(user), this::render)`. `render` chia ba hộp `openBox`,
`upcomingBox`, `closedBox`; mỗi ca là một thẻ (`card`) với nút theo `SessionEntry`: "Vào thi" / "Tiếp tục làm bài"
(`canEnter`, `isResuming`), "Xem kết quả" (`canViewResult`), hoặc nhãn "Không dự thi" (ca đã đóng mà không có bài).
Controller không tự quyết định nút nào bật – chỉ đọc `SessionEntry`.

**`RulesController`** (`rules.fxml`): hiện tên đề, môn, số câu – tổng điểm, giờ đóng, thời gian còn lại (lấy giờ máy chủ
trên luồng nền), câu quy chế lấy từ `client.proctoring.getPolicy().rulesText()` cộng câu "Bài làm được lưu sau mỗi câu
trả lời; mất điện hay mất mạng thì đăng nhập lại để làm tiếp.". Vào lại bài dở thì hiện "Tiếp tục bài đang làm (đã vi
phạm N lần)." và đổi nút thành "Tiếp tục làm bài". `onStart`:

```java
    @FXML
    private void onStart() {
        int sid = session.sessionId();
        BackgroundTasks.run(form, () -> new Entered(client.exams.enter(user, sid), client.exams.serverTime()),
                e -> shell.navigator().show("exam.fxml",
                        (ExamController c) -> c.init(shell, client, user, e.attempt(), e.serverNow())));
    }
```

Lấy giờ máy chủ **ngay sau khi vào thi** trên cùng luồng nền, để `ServerClock` bắt đầu đúng.

**`ExamController`** (`exam.fxml`) – controller lớn nhất (khoảng 460 dòng) nhưng **không có quy tắc nghiệp vụ**: nó vẽ và
nối `AnswerSheet`, `AnswerSaver`, `ExamProctor`, `ServerClock`, `StudentExamService`, `ProctoringService`. Các cờ trạng thái:

| Trường | Ý nghĩa |
|---|---|
| `current` | vị trí câu đang xem trong đề đã trộn (0..n-1) |
| `populating` | đang điền sẵn câu trả lời cũ vào ô – listener không coi là thí sinh đổi ý |
| `warned` | đã cảnh báo 5 phút cuối |
| `timedOut` | đã tự nộp do hết giờ – các giây sau không gọi nộp lại |
| `submitting`, `finished` | đang nộp / đã rời màn hình |
| `secondsSinceSync` | đếm tới `RESYNC_EVERY_SECONDS` (60) |
| `reporting`, `queuedLeave` | đang gửi một lần vi phạm / sự kiện chờ gửi tiếp |

Những điểm hay bị hỏi:
- `init` gọi `Platform.runLater(proctor::start)`: bật toàn màn hình **sau khi** màn hình đã gắn vào cửa sổ.
- Lưới câu bên trái (`buildGrid`, `refreshGrid`) tô màu theo `AnswerSheet` (lớp CSS `q-answered`, `q-flagged`, `q-current`).
- `show(index)` dựng ô trả lời theo loại câu: `RadioButton` cho một đáp án và đúng/sai (`"true"`/`"false"`), `CheckBox`
  cho nhiều đáp án (`AnswerFormat.joinOptionIds`), `TextField` cho điền khuyết. Chữ "A. B. C." chỉ để đọc; giá trị lưu là
  **mã phương án** (`setUserData(...)`).
- `onTick()` mỗi giây: tính `remaining` từ `clock.now()`; cảnh báo 5 phút bằng **dải thông báo** (không dùng hộp thoại vì
  hộp thoại làm mất tiêu điểm); `remaining == 0` thì `submit(EndReason.TIME_OUT)` đúng một lần (`timedOut`).
- `resync()` mỗi 60 giây: đồng bộ giờ **và** đọc lại trạng thái bài; bài đã khóa (máy khác nộp, máy chấm đóng ca) thì
  `finish(...)`. Lỗi mạng thì bỏ qua, lần sau thử lại.
- `onLeave(when)`: chỉ gửi **một** sự kiện vi phạm tại một thời điểm (`reporting`); sự kiện tới giữa chừng giữ ở
  `queuedLeave` (giữ sự kiện mới nhất), gửi tiếp khi xong – để số lần trên màn hình không nhảy lùi 1/5 → 3/5 → 2/5 khi hai
  lượt về không theo thứ tự. Mất mạng: giữ **thời điểm sớm nhất** (`when.isBefore(queuedLeave)`), hiện "Mất kết nối. Lần
  rời cửa sổ vừa rồi sẽ được ghi nhận khi có kết nối lại.", 3 giây sau gửi lại (`retryQueuedLeave`).
- `handleViolation(r)`: kết thúc hoặc bài đã khóa → `finish`; gộp → không làm gì; còn lại → cập nhật nhãn
  "Vi phạm: N/5" và hộp thoại cảnh báo qua `proctor.showDialog(alert::showAndWait)`.
- `submit(reason)`: `saver.stop()`, `saver.drain()` trên luồng nền (bắt `SessionRuleException` – ca đã đóng thì bỏ câu
  chưa lưu kịp, vẫn nộp phần đã lưu), rồi `client.exams.submit(...)`; lỗi mạng thì 3 giây thử lại.
- `finish(state)`: dừng `ticker`, `saver`, `proctor`, chuyển sang `result.fxml` với `AttemptState` **mà `submit` trả về**
  (có thể là trạng thái máy chấm đã đặt), không phải bản sao cũ.

### 5.16. Các controller phía giảng viên

**`SessionFormController`** (`session-form.fxml`, mở từ `ExamListController.onOpenSession`):
- `init` lấy giờ máy chủ qua `sessionService.serverTime()` và điền sẵn: mở **ngay** (làm tròn phút), đóng sau **60 phút**
  (DESIGN-CHANGES Phase 8). Ba ô `shuffleQuestionsBox`, `shuffleOptionsBox`, `showAnswerBox` mặc định được tick (FXML
  `selected="true"`).
- Chọn lớp trong `classBox` → `loadStudents(cls)`: theo lớp thì đánh dấu sẵn cả lớp; chọn "Mọi thí sinh (chọn từng
  người)" thì để trống. Danh sách dùng `CheckBoxListCell` gắn với `Map<Student, BooleanProperty> chosen`; nhãn
  "Đã chọn n / m thí sinh" tự cập nhật qua listener.
- `onCreate`: ghép ngày + giờ bằng `Formats.combine` (mã `DATE_EMPTY`, `TIME_INVALID`), dựng `SessionWindow` (có thể ném
  `SESSION_TIME` ngay trên luồng giao diện – được bắt bởi bộ xử lý ngoại lệ chung của `OesApplication`), `SessionPolicy`,
  rồi gọi `createSession` trên luồng nền. Xong thì báo "Đã mở ca thi #… Đề "…" đã bị khóa, muốn sửa phải tạo phiên bản
  mới." và mở danh sách ca.
- Không có kiểm tra nghiệp vụ nào trong controller – đúng quy tắc ba tầng.

**`StaffSessionsController`** (`staff-sessions.fxml`, menu "Ca thi"): chọn môn → `onRefresh` lấy giờ máy chủ một lần,
duyệt các đề của môn (`examService.list`), các ca của mỗi đề (`sessionService.listOfExam`), tóm tắt bằng
`statistics.summarize(...)` thành `SessionSummary`; sắp ca mới lên đầu. Cột "Trạng thái" là `phase().displayName()`.
Bấm đúp một dòng hoặc nút kết quả → `onResults` mở `session-results.fxml` (phần 4).

### 5.17. `AppConfig` và `ServerSetupController`

**Tìm tệp cấu hình** (`AppConfig.locate`): (1) tham số `--config=đường/dẫn.properties`; (2) tệp cùng tên đặt cạnh tệp jar
(`defaultLocation`: thư mục chứa jar; chạy từ IntelliJ/Maven thì là thư mục đang chạy); (3) không có thì
`OesApplication.start` hiện màn hình cài đặt máy chủ. Tên tệp: `AppConfig.CLIENT_FILE = "db-client.properties"` (ứng
dụng thi), `AppConfig.STAFF_FILE = "db-staff.properties"`.

**`ServerSettings`** (record lồng) kiểm tra ô nhập trong compact constructor: `HOST_INVALID`, `PORT_INVALID` (1–65535),
`DB_INVALID` (chỉ chữ, số, gạch dưới), `USER_INVALID`. `jdbcUrl()` ghi sẵn `connectTimeout=5000&socketTimeout=30000`
để mất mạng thì báo lỗi sau vài giây, không treo.

**`ServerSetupController.onConnect`** chỉ đổi cổng thành số (sai thì `PORT_INVALID` "Cổng phải là số, MySQL thường dùng
3306."), dựng `ServerSettings` rồi gọi callback `onConnect` do `OesApplication.showServerSetup` truyền vào. Callback đó
**thử kết nối trước**, đúng thì mới `AppConfig.save` (không ghi lại một cấu hình sai); không ghi được tệp thì mã
`CONFIG_WRITE`. Tài khoản CSDL mặc định điền sẵn của ứng dụng thi là `oes_client` (`ExamClientApp.defaultDbUser`).

---

## 6. Luồng chạy đầu–cuối

Ký hiệu: **[GD]** = luồng giao diện, **[nền]** = luồng nền của `BackgroundTasks`. Câu SQL đầy đủ xem bảng ở mục 5.11.

### 6.1. Giảng viên mở ca thi (UC-10, UC-11)

1. Đề thi → chọn đề → "Mở ca thi…" → `ExamListController.onOpenSession` (phần 2) → `SessionFormController.init(ws, exam)`.
2. **[nền]** `ExamSessionService.serverTime()` → `JdbcExamSessionDao.serverTime()` → `SELECT NOW()` → điền giờ mở = bây giờ,
   giờ đóng = +60 phút. **[nền]** `subjectService.listClasses` → chọn lớp → `ExamSessionService.studentsOfClass` →
   `UserDao.findStudentsOfClass` (phần 1).
3. Bấm "Mở ca thi" → `SessionFormController.onCreate` → `new SessionWindow(...)`, `new SessionPolicy(...)` → **[nền]**
   `ExamSessionService.createSession`:
   - `Permissions.requireManage` → `NO_STUDENTS`? → `SESSION_IN_PAST`? (so với `SELECT NOW()`)
   - `new ExamSession(0, exam, window, policy)` → `exam.validateForUse()`; `enrollAll(students)`
   - `JdbcExamSessionDao.insert` trong giao dịch: `INSERT INTO exam_session(...)` + lô `INSERT INTO session_student(session_id, student_id) VALUES (?, ?)`;
     CSDL kiểm tra `CHECK (close_time > open_time)`, khóa chính `(session_id, student_id)`
   - `exam.lock()` + `ExamDao.lock(examId)` (BR-09); `AuditLog.record(actor, "CREATE_SESSION", ...)`
4. Thành công: hộp thoại "Đã mở ca thi #…", mở danh sách ca (`StaffSessionsController`).
5. Nhánh lỗi: giờ sai dạng → "Ô "Giờ mở" cần giờ dạng HH:mm, ví dụ 07:30."; giờ đóng ≤ giờ mở → "Giờ đóng ca thi phải sau
   giờ mở."; giờ đóng ở quá khứ → "Giờ đóng ca thi đã ở trong quá khứ. Hãy chọn giờ đóng sau thời điểm hiện tại."; không
   chọn ai → "Ca thi chưa có thí sinh nào. …". Tất cả hiện qua `ErrorDialogs` bằng `getUserMessage()`.

### 6.2. Đăng nhập → danh sách ca → quy chế → vào thi (UC-12, UC-13; NFR-07: ba thao tác)

1. Đăng nhập (phần 1: `LoginController` → `ExamClientApp.login` → `StudentAuthService.login`, khung nhìn `v_login`) →
   `ExamClientApp.showHome` → `SessionListController.init`.
2. **[nền]** `StudentExamService.overview(user)`:
   - `Permissions.requireStudent`; `dao.serverTime()` → `SELECT NOW()`
   - `dao.findSessionsOf(studentId)` → `SELECT ... FROM v_session_for_student v JOIN session_student ss ON ss.session_id = v.session_id WHERE ss.student_id = ? ORDER BY v.open_time`
   - mỗi ca: `dao.findAttempt(...)` → `SELECT id, status, violation_count, end_reason, total_score FROM submission WHERE session_id = ? AND student_id = ?`
     (+ đọc `answer`)
   - mỗi ca thành `SessionEntry(session, window.phaseAt(now), attempt)`.
3. **[GD]** `render` chia ba nhóm; ca đang mở có nút "Vào thi" (thao tác 1) → `RulesController.init`.
4. Màn hình quy chế: **[nền]** `serverTime()` để hiện thời gian còn lại. Tick "Tôi đã đọc và đồng ý với quy chế thi"
   (thao tác 2) bật nút nhờ binding. Bấm "Bắt đầu làm bài" (thao tác 3) → `RulesController.onStart`.
5. **[nền]** `StudentExamService.enter(user, sid)`:
   - `loadSession` → `findSession` → `v_session_for_student WHERE v.session_id = ?` (không thấy → `SESSION_NOT_FOUND`)
   - `serverTime()`; `findAttempt` → đã khóa → `ALREADY_SUBMITTED`
   - `isEnrolled` → `SELECT 1 FROM session_student WHERE session_id = ? AND student_id = ?` → không có → `NOT_ENROLLED`
   - `window.ensureOpen(now)` → `SESSION_NOT_OPEN_YET` / `SESSION_CLOSED`
   - chưa có bài → `create` → `INSERT INTO submission(session_id, student_id, started_at, status) VALUES (?,?,?,'IN_PROGRESS')`
     → trigger `trg_submission_client_insert` (máy thí sinh chỉ được tạo bài `IN_PROGRESS`); trùng → `DuplicateKeyException` → đọc lại bài
   - `loadPaper(examId)` → `v_exam_paper` (chỉ hiện đề của ca đã tới giờ mở, không có cột đáp án) → `shuffledFor(...)`
   - rồi `serverTime()` lần nữa cho đồng hồ.
6. **[GD]** `ExamController.init(...)`: dựng `AnswerSheet`, `ServerClock`, `AnswerSaver`, lưới câu, `Timeline`;
   `Platform.runLater(proctor::start)` → toàn màn hình.
7. Nhánh lỗi (hiện qua `ErrorDialogs`, vẫn ở màn hình quy chế): "Ca thi chưa mở. Giờ mở: 08:00 20/11/2026."; "Ca thi đã
   kết thúc lúc …"; "Bạn đã nộp bài của ca thi này. Mỗi thí sinh chỉ được thi một lần."; "Bạn không có tên trong danh sách
   dự thi của ca này. Hãy liên hệ giảng viên."; mất mạng → "Không kết nối được tới máy chủ dữ liệu. Vui lòng thử lại hoặc
   báo giám thị." (`DataAccessException.getUserMessage`).

### 6.3. Trả lời một câu → lưu (UC-14, FR-24)

1. **[GD]** Thí sinh chọn một `RadioButton` → listener của `ToggleGroup` trong `ExamController.radios` →
   `onAnswer(order, value)` (bỏ qua nếu `populating` hoặc `finished`).
   - Nhiều đáp án: listener của mỗi `CheckBox` → `AnswerFormat.joinOptionIds(ids)` → `"12,14"` (không chọn gì → `null`).
   - Điền khuyết: `textProperty` listener gọi `onAnswer` **mỗi phím gõ**.
2. **[GD]** `sheet.setResponse(...)` (lưới đổi màu ngay) → `saver.answerChanged(order, response)` → đưa vào map →
   `debounce.playFromStart()`.
3. Sau 500 ms không đổi gì → `AnswerSaver.flush()` → **[nền]** `drain()` → `sink.saveAnswer(order, response)` →
   `StudentExamService.saveAnswer(user, attempt, order, response)`:
   - `writableState`: `requireOwn` (`NOT_OWNER`), `isLocked` (`SUBMISSION_LOCKED`), `paper().contains(order)` (`ITEM_NOT_FOUND`)
   - `JdbcStudentExamDao.saveAnswer` → `INSERT INTO answer(submission_id, item_order, response) VALUES (?,?,?) ON DUPLICATE KEY UPDATE response = ?`
   - trigger `trg_answer_client_insert` **và** `trg_answer_client_update` (câu `ON DUPLICATE KEY UPDATE` đi qua cả hai):
     chỉ cho ghi khi bài `IN_PROGRESS` và `es.close_time > NOW()`
   - thành công → `state.recordAnswer(...)`.
4. **[GD]** Thành công: `offline = false`, `flush()` lần nữa nếu trong lúc gửi đã có thay đổi mới.
5. Nhánh lỗi:
   - Trigger từ chối (SQLSTATE `45000`, thông điệp bắt đầu bằng `SUBMISSION_LOCKED`) → `JdbcErrors.translate` →
     `SessionRuleException(SUBMISSION_LOCKED)` → `AnswerSaver.stop()` + `onLocked` → `ExamController.finishFromServer()`
     → `refresh` → màn hình kết quả. Thí sinh **không** thấy hộp thoại lỗi.
   - Mất mạng (`DataAccessException`) → `offline = true` → dải vàng; `retry` 3 giây gọi lại `flush()`.
6. Đánh dấu xem lại: nút "Đánh dấu xem lại" → `ExamController.onFlag` → `saver.flagChanged` → `flush()` ngay →
   `StudentExamService.setFlag` → `INSERT INTO answer(submission_id, item_order, flagged) VALUES (?,?,?) ON DUPLICATE KEY UPDATE flagged = ?`.

### 6.4. Rời cửa sổ → vi phạm → đủ 5 lần (UC-15, FR-28, FR-29)

1. Thí sinh Alt+Tab → JavaFX đổi `focusedProperty` thành `false` (có thể kèm `fullScreenProperty` → `false`) →
   `ExamProctor.focusListener` → `leave()` → nếu `watching && !paused` thì `onLeave.accept(startWall.plusNanos(System.nanoTime() - startNanos))`
   (thời điểm đo bằng `nanoTime`, xem mục 12 giới hạn 6).
2. **[GD]** `ExamController.onLeave(when)`: đang gửi (`reporting`) thì để vào `queuedLeave`; nếu không thì
   `reporting = true` và **[nền]** `client.proctoring.report(user, attempt, when)`:
   - `requireOwn`; bài đã khóa → trả kết quả "đã khóa"
   - `policy.isMerged(lastCounted, when)` → dưới 3 giây so với lần được tính gần nhất → `merged = true`, dừng
   - `dao.increaseViolation(id)` (thành công rồi mới `lastCounted.put(...)`) →
     `UPDATE submission SET violation_count = LAST_INSERT_ID(violation_count + 1) WHERE id = ? AND status = 'IN_PROGRESS'`
     + `SELECT LAST_INSERT_ID()`; trigger `trg_submission_client_update` (chỉ sửa bài đang làm, không giảm số lần)
   - không tăng được → bài đã khóa ở nơi khác → `refresh`
   - `isTerminal(count)` (≥ 5) → `exams.submit(..., EndReason.VIOLATION)` → `UPDATE submission SET status = 'SUBMITTED', submitted_at = ?, end_reason = ? WHERE id = ? AND status = 'IN_PROGRESS'`
     → nhật ký `TERMINATED_BY_VIOLATION`.
3. **[GD]** `handleViolation(result)`:
   - gộp → không làm gì
   - lần 1–4 → nhãn "Vi phạm: N/5" đổi màu; hộp thoại "Cảnh báo vi phạm quy chế": "Bạn đã rời cửa sổ làm bài (lần N/5).
     Còn M lần nữa bài thi sẽ bị kết thúc." với nút "Quay lại làm bài", mở qua `proctor.showDialog(...)` → không tự sinh
     vi phạm mới; đóng hộp thoại thì quay lại toàn màn hình.
   - lần 5 → `finish(state)` → màn hình kết quả: dải đỏ "Bài thi bị kết thúc do vi phạm quy chế (rời cửa sổ làm bài đủ số lần cho phép)." (phần 4), "Đã nộp bài.
     Đang chấm bài…". Bài vẫn được chấm phần đã làm (BR-06, phần 4).
4. Nhánh mất mạng: xem 6.6.

### 6.5. Nộp bài / hết giờ (UC-16, FR-25, FR-26)

**Nộp tay.**
1. Nút "Nộp bài" → `ExamController.onSubmit` → hộp thoại xác nhận (qua `proctor.showDialog`): "Còn N câu chưa trả lời, M câu
   đang đánh dấu xem lại. Nộp xong sẽ không sửa được nữa. Bạn chắc chắn nộp bài?" → "Nộp bài" / "Hủy".
2. `submit(EndReason.MANUAL)`: khóa nút, `saver.stop()`, dải "Đang nộp bài…", **[nền]**:
   - `saver.drain()` – đẩy hết câu còn trong hàng đợi (lỗi `SessionRuleException` thì bỏ qua: ca vừa đóng, BR-04)
   - `StudentExamService.submit` → `markSubmitted(id, MANUAL, serverTime())` → câu `UPDATE ... WHERE id = ? AND status = 'IN_PROGRESS'`
     → trigger `trg_submission_client_update` (không được đặt `GRADED`)
   - `false` (máy khác/máy chấm đã nộp) → `refresh` trả trạng thái thật.
3. **[GD]** `finish(state)` → `ResultController.init(...)` (phần 4): "Đã nộp bài. Đang chấm bài…", hỏi lại 2 giây/lần, tối đa
   30 lần (60 giây); hết lượt thì "Bài đã được nộp. Kết quả sẽ có khi giảng viên mở máy chấm."
4. Phía máy chấm (phần 4): `GradingWorker.runOnce` → `GradingDao.findSubmittedIds` → chấm → `GRADED`.
5. Lỗi mạng khi nộp: dải "Chưa nộp được do mất kết nối. Đang thử lại…", `PauseTransition` 3 giây → `submit(reason)` lại.

**Hết giờ – hai đường, kết quả như nhau.**
- *Máy thí sinh còn chạy:* `ExamController.onTick` → `remaining == 0` → cờ `timedOut` → `submit(EndReason.TIME_OUT)` một
  lần duy nhất. `drain()` lúc này thường bị trigger từ chối (`close_time > NOW()` sai) → bỏ phần chưa lưu, nộp phần đã lưu.
  Trigger `trg_submission_client_update` chỉ kiểm tra `OLD.status`, không kiểm tra giờ, nên máy thí sinh vẫn chuyển được
  bài sang `SUBMITTED` sau giờ đóng.
- *Máy thí sinh đã tắt / mất mạng:* máy chấm (phần 4) chạy `JdbcGradingDao.closeExpired(now)`:
  `UPDATE submission sb JOIN exam_session es ON es.id = sb.session_id SET sb.status = 'SUBMITTED', sb.submitted_at = es.close_time, sb.end_reason = 'TIME_OUT' WHERE sb.status = 'IN_PROGRESS' AND es.close_time <= ?`.
  Nếu sau đó máy thí sinh mới gọi `submit` thì `markSubmitted` trả `false` → `refresh` → thấy `TIME_OUT`.
- Trước đó, còn 5 phút: đồng hồ thêm lớp CSS `clock-warning` (đỏ) và dải "Còn dưới 5 phút. Hãy kiểm tra lại bài làm."

### 6.6. Mất kết nối, tắt máy rồi vào lại, hai máy cùng tài khoản (NFR-06)

| Tình huống | Đi qua đâu | Thí sinh thấy gì |
|---|---|---|
| Mất mạng khi đang lưu | `AnswerSaver.flush` nhánh lỗi → `offline = true`, `retry` 3 giây | Dải vàng; làm bài tiếp bình thường; có mạng lại thì dải tắt, câu được lưu (UiSmoke `reconnect`: 3 câu có trong CSDL) |
| Mất mạng khi rời cửa sổ | `ExamController.onLeave` nhánh lỗi: giữ thời điểm sớm nhất, 3 giây gửi lại | "Mất kết nối. Lần rời cửa sổ vừa rồi sẽ được ghi nhận khi có kết nối lại." (xem giới hạn ở mục 12) |
| Mất mạng lúc đồng bộ 60 giây | `ExamController.resync` nhánh lỗi: bỏ qua | Đồng hồ vẫn chạy theo lần đồng bộ trước |
| Mất mạng khi nộp | `ExamController.submit` nhánh lỗi, 3 giây thử lại | "Chưa nộp được do mất kết nối. Đang thử lại…" |
| Tắt máy / kill ứng dụng, mở lại | `overview` thấy bài `IN_PROGRESS` → `SessionEntry.isResuming()` → nút "Tiếp tục làm bài" → `enter` trả **bài cũ**; `shuffledFor` cùng seed → cùng thứ tự; `AnswerSheet` dựng từ `AttemptState` đã lưu | "Tiếp tục bài đang làm (đã vi phạm N lần)."; các câu đã lưu còn nguyên; giờ vẫn tính tới giờ đóng chung (UiSmoke `resume1` → `resume2`) |
| Hai máy cùng tài khoản cùng vào | `create` bắt `DuplicateKeyException` → cùng một bài | Hai máy làm chung một bài |
| Máy A nộp, máy B còn mở | Máy B: lần lưu kế tiếp bị trigger từ chối → `finishFromServer`; hoặc `resync` (≤ 60 giây) thấy `isLocked()` → `finish`; hoặc B bấm nộp → `markSubmitted` `false` → `refresh` | B tự chuyển sang màn hình kết quả, kết quả của A được giữ (UiSmoke `second` + `full`) |

Câu trả lời đã gửi được thì nằm trong CSDL ngay (lưu từng câu), nên tắt máy chỉ mất tối đa những gì thí sinh đổi trong
khoảng 500 ms cuối chưa kịp gửi.

---

## 7. CSDL của phần này

### 7.1. Bảng (trích `db/schema.sql`)

```sql
CREATE TABLE exam_session (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    exam_id            INT NOT NULL,
    open_time          DATETIME NOT NULL,
    close_time         DATETIME NOT NULL,
    shuffle_questions  BOOLEAN NOT NULL,
    shuffle_options    BOOLEAN NOT NULL,
    show_answer        BOOLEAN NOT NULL,
    CHECK (close_time > open_time),
    FOREIGN KEY (exam_id) REFERENCES exam(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE session_student (
    session_id  INT NOT NULL,
    student_id  INT NOT NULL,
    PRIMARY KEY (session_id, student_id),
    FOREIGN KEY (session_id) REFERENCES exam_session(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- Mỗi cột của `exam_session` ứng với một trường của `SessionWindow` / `SessionPolicy`.
- `session_student` là bảng nối nhiều–nhiều (một ca nhiều thí sinh, một thí sinh nhiều ca); khóa chính ghép chặn ghi danh
  trùng.

Bảng `submission` và `answer` thuộc phần 4, nhưng phần 3 là nơi **ghi** vào chúng từ máy thí sinh:

```sql
-- UNIQUE (session_id, student_id) là chốt chặn cuối cùng của quy tắc "mỗi thí sinh thi một lần":
-- dù hai máy cùng bấm "Vào thi" một lúc, CSDL chỉ cho một bản ghi đi qua.
CREATE TABLE submission (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    session_id       INT NOT NULL,
    student_id       INT NOT NULL,
    started_at       DATETIME NOT NULL,
    submitted_at     DATETIME,
    status           ENUM('IN_PROGRESS','SUBMITTED','GRADED') NOT NULL,
    end_reason       ENUM('MANUAL','TIME_OUT','VIOLATION'),
    total_score      DECIMAL(6,2) NOT NULL DEFAULT 0,
    violation_count  INT NOT NULL DEFAULT 0,
    UNIQUE (session_id, student_id),
    -- ...
```

`answer`: khóa chính `(submission_id, item_order)` – mỗi câu một dòng, nên `INSERT ... ON DUPLICATE KEY UPDATE` ghi đè
được khi thí sinh đổi ý; cột `flagged` (FR-22); `earned_point` chỉ máy chấm ghi. `item_order` là **số câu trong đề gốc**.

### 7.2. Khung nhìn (trích `db/security.sql`)

```sql
CREATE OR REPLACE VIEW v_session_for_student AS
SELECT es.id AS session_id, es.exam_id, es.open_time, es.close_time,
       es.shuffle_questions, es.shuffle_options, es.show_answer,
       e.title AS exam_title, s.code AS subject_code, s.name AS subject_name,
       (SELECT COALESCE(SUM(ei.point), 0) FROM exam_item ei WHERE ei.exam_id = e.id) AS total_point,
       (SELECT COUNT(*) FROM exam_item ei WHERE ei.exam_id = e.id) AS question_count
FROM exam_session es
JOIN exam e    ON e.id = es.exam_id
JOIN subject s ON s.id = e.subject_id;
```

```sql
CREATE OR REPLACE VIEW v_exam_paper AS
SELECT ei.exam_id, ei.item_order, ei.point,
       q.id AS question_id, q.type, q.content,
       ao.id AS option_id, ao.opt_order, ao.content AS option_content
FROM exam_item ei
JOIN question q ON q.id = ei.question_id
LEFT JOIN answer_option ao ON ao.question_id = q.id AND q.type IN ('SINGLE_CHOICE', 'MULTI_CHOICE')
WHERE EXISTS (SELECT 1 FROM exam_session es WHERE es.exam_id = ei.exam_id AND es.open_time <= NOW());
```

- `v_exam_paper` **không có** `is_correct`, `correct_value`, `case_sensitive`. Phương án chỉ lấy cho câu một/nhiều đáp án,
  vì bảng `answer_option` còn chứa *đáp án chấp nhận* của câu điền khuyết – lấy cho câu điền khuyết là lộ đáp án.
- `LEFT JOIN` để câu đúng/sai, điền khuyết vẫn có một dòng (với `option_id` `NULL`).
- `es.open_time <= NOW()`: thí sinh không đọc trước được đề của ca chưa mở (`JdbcSecurityTest.thíSinhKhôngĐọcTrướcĐượcĐềCủaCaChưaMở`).
- Khung nhìn chạy với quyền người tạo (`SQL SECURITY DEFINER` mặc định), nên `oes_client` đọc được dữ liệu đã lọc mà
  không cần quyền trên bảng gốc.
- `v_review` (phần 4) lọc `es.close_time <= NOW() AND es.show_answer = TRUE` – phần 3 chỉ đọc nó trong `loadReview`.

### 7.3. Quyền của `oes_client` (trích `db/security.sql`)

```sql
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

Đối chiếu với mã: mỗi câu `INSERT`/`UPDATE` trong `JdbcStudentExamDao` chỉ đụng đúng các cột được cấp. Không có quyền gì
trên `question`, `answer_option`, `exam_item`. (Thêm `SELECT ON v_login` và `EXECUTE` thủ tục đổi mật khẩu – phần 1.)

### 7.4. Trigger – phần 3 "đụng vào" ở đâu

| Trigger | Khi nào phần 3 kích hoạt | Chặn gì |
|---|---|---|
| `trg_submission_client_insert` | `createAttempt` | Máy thí sinh tạo bài với `status` khác `IN_PROGRESS` |
| `trg_submission_client_update` | `increaseViolation`, `markSubmitted` | Sửa bài không còn `IN_PROGRESS`; tự đặt `GRADED`; giảm `violation_count` |
| `trg_answer_client_insert`, `trg_answer_client_update` | `saveAnswer`, `setFlag` | Ghi khi bài đã khóa hoặc `close_time > NOW()` sai (BR-04 ở mức CSDL) |

Mọi trigger chỉ áp dụng khi `SUBSTRING_INDEX(USER(), '@', 1) = 'oes_client'`; báo lỗi bằng `SIGNAL SQLSTATE '45000'`
với thông điệp bắt đầu bằng `SUBMISSION_LOCKED`. Chi tiết vì sao dùng `USER()` chứ không `CURRENT_USER()` – phần 4.

---

## 8. Quy tắc nghiệp vụ

| Quy tắc | Dòng mã thực thi (lớp.phương thức) | Lớp chặn ở CSDL |
|---|---|---|
| **Quyết định 1 / BR-02**: mỗi thí sinh thi đúng một lần; vào lại thì mở bài cũ | `StudentExamService.enter` (`ALREADY_SUBMITTED`; `existing.orElseGet(...)`), `StudentExamService.create` (bắt `DuplicateKeyException`), `SessionEntry.canEnter` | `UNIQUE (session_id, student_id)` trên `submission` |
| **Quyết định 2 / BR-03**: giờ mở/đóng chung, vào muộn còn ít giờ | `SessionWindow.remainingSeconds` (tính tới `closeTime`), `ExamController.onTick` | `closeExpired` ghi `submitted_at = es.close_time` |
| **BR-01**: có tên và đang trong giờ | `StudentExamService.enter` (`isEnrolled`, `window.ensureOpen`), `ExamSession.ensureCanEnter` | `session_student`; `v_exam_paper` lọc `open_time <= NOW()` |
| **BR-04**: ca đóng thì khóa, từ chối ghi muộn | `StudentExamService.writableState`, `AnswerSaver` (dừng khi `SessionRuleException`), `ExamController.submit` (bỏ câu chưa lưu) | `trg_answer_client_insert` / `_update`; `closeExpired` (phần 4) |
| **Quyết định 5 / BR-05**: rời cửa sổ là vi phạm; 5 lần kết thúc; gộp dưới 3 giây | `ExamProctor` (3 listener), `ViolationPolicy.isMerged / isTerminal`, `ProctoringService.report`, `ExamController.handleViolation` | `trg_submission_client_update` cấm giảm `violation_count` |
| Toàn màn hình, chặn Esc / Alt+F4 (FR-27) | `ExamProctor.start` (`KeyCombination.NO_MATCH`, `Event::consume`) | – |
| Hộp thoại của ứng dụng không tính vi phạm | `ExamProctor.showDialog`, `tryResume`, `forceResume` | – |
| **Quyết định 6**: giờ chuẩn từ `SELECT NOW()` | `StudentExamDao.serverTime`, `ExamSessionDao.serverTime`, `ServerClock`, mọi hàm `SessionWindow` nhận `now` từ ngoài | Trigger và khung nhìn đều so với `NOW()` của máy chủ |
| Nộp bài chỉ đánh dấu `SUBMITTED`, không chấm (NFR-04) | `StudentExamService.submit`, `JdbcStudentExamDao.markSubmitted` | `oes_client` không có quyền ghi `total_score`, `earned_point`; trigger cấm tự đặt `GRADED` |
| Nộp hai lần giữ lần đầu | `StudentExamService.submit` (`isLocked` → trả luôn; `markSubmitted` false → `refresh`) | `WHERE ... AND status = 'IN_PROGRESS'` |
| Thí sinh chỉ thao tác bài của mình | `StudentExamService.requireOwn` (`NOT_OWNER`) | Không có (giới hạn: chung `oes_client`, mục 12) |
| Máy thí sinh không có đáp án (NFR-04) | gói `client` (`PaperOption` không có cờ đúng/sai), `ClientContext` không có `QuestionDao` | `v_exam_paper`, không cấp quyền trên `question`, `answer_option` |
| Đề đã mở ca thì khóa (BR-09, phối hợp phần 2) | `ExamSessionService.createSession` (`exam.lock()`, `examDao.lock`) | – |
| Tối đa 3 thao tác để bắt đầu (NFR-07) | "Vào thi" → tick đồng ý → "Bắt đầu làm bài" | – |

---

## 9. SOLID và mẫu thiết kế

**S – Single Responsibility (mỗi lớp một lý do để đổi).**
- `ExamSession` tách thành `SessionWindow` (giờ), `SessionPolicy` (tùy chọn), `PaperShuffler` (trộn). *Nếu không:* máy thí
  sinh muốn tính giờ phải có `ExamSession` – tức là có `Exam` có đáp án, vi phạm NFR-04.
- `ViolationPolicy` (quy chế) tách khỏi `ProctoringService` (điều phối) và `Submission` (dữ liệu). *Nếu không:* đổi "5 lần"
  thành "3 lần" phải sửa lớp đang lo cả CSDL lẫn dữ liệu bài làm.
- `ExamController` chỉ vẽ và nối; `AnswerSheet`, `AnswerFormat`, `ServerClock` (không JavaFX – kiểm thử JUnit được),
  `AnswerSaver`, `ExamProctor` mỗi lớp một việc. *Nếu không:* một controller 1000 dòng, không kiểm thử được phần nào.

**O – Open/Closed (mở để mở rộng, đóng để sửa).** `ProctoringService` nhận `ViolationPolicy` qua constructor; muốn quy
chế khác (ví dụ ca thi thử 10 lần) chỉ cần truyền `new ViolationPolicy(10, Duration.ofSeconds(3))` ở `ClientContext`,
không sửa `ProctoringService`. `PaperShuffler.shuffle` generic: trộn thêm kiểu dữ liệu mới không phải sửa nó.

**L – Liskov Substitution.** `JdbcStudentExamDao` và `MemoryStudentExamDao` thay thế nhau hoàn toàn ở chỗ cần
`StudentExamDao`: bản bộ nhớ cố ý làm đúng việc của khung nhìn và trigger (đề không có đáp án, không ghi sau giờ đóng,
`createAttempt` trùng thì ném `DuplicateKeyException`). *Nếu bản bộ nhớ "dễ dãi" hơn,* kiểm thử xanh nhưng chạy MySQL thật
lại hỏng – vi phạm LSP.

**I – Interface Segregation.** `StudentExamDao` chỉ có thao tác của máy thí sinh; `ExamSessionDao` chỉ của giảng viên;
`GradingDao` chỉ của máy chấm. `ClientContext` chỉ nhận `StudentExamDao`, `StudentLoginDao`, nên ứng dụng thi **không có
đường nào** gọi hàm đọc đáp án hay ghi điểm. Ở mức nhỏ: `AnswerSaver.Sink` chỉ có hai hàm `AnswerSaver` cần.

**D – Dependency Inversion.**
- `StudentExamService(StudentExamDao dao)`, `ExamSessionService(ExamSessionDao, ExamDao, UserDao)`,
  `ProctoringService(StudentExamDao, StudentExamService, ViolationPolicy)` – đều nhận trừu tượng qua constructor;
  `ClientContext` / `AppContext` lắp ráp.
- `AnswerSaver` phụ thuộc `Sink`, `ExamProctor` phụ thuộc `Consumer<Instant>`, `ServerClock` phụ thuộc `LongSupplier` –
  nhờ vậy `ClientModelTest` thay được nguồn `nanoTime` bằng số giả.

**Mẫu thiết kế và kỹ thuật**

| Mẫu | Ở đâu | Vì sao |
|---|---|---|
| Value Object (record bất biến) | `SessionWindow`, `SessionPolicy`, `PaperItem`, `PaperOption`, `SessionInfo`, `ReviewItem` | Kiểm tra một lần ở constructor, chia sẻ giữa các luồng không cần khóa |
| Parameter Object | `SessionPolicy`, `SessionWindow` trong `createSession` | Tránh dãy `boolean` dễ truyền nhầm thứ tự |
| Static factory | `ViolationPolicy.standard()`, `AttemptState.started(...)`, `SessionRuleException.notEnrolled()` | Tên nói rõ ý định; một câu báo lỗi dùng chung hai phía |
| Observer | Listener trên `fullScreenProperty`, `focusedProperty`, `iconifiedProperty`; binding `offlineProperty` | Phản ứng với thay đổi mà không phải hỏi liên tục |
| Adapter | Lớp vô danh `AnswerSaver.Sink` trong `ExamController.init` | Nối `AnswerSaver` (chỉ biết `Sink`) với `StudentExamService` (cần `user`, `attempt`) |
| Debounce / hàng đợi gộp | `AnswerSaver` (`PauseTransition` + `Map` theo số câu) | Ít lượt ghi, chỉ gửi giá trị cuối |
| Compare-and-set bằng `WHERE` | `markSubmitted`, `increaseViolation` (`AND status = 'IN_PROGRESS'`) | Hai máy cùng thao tác: chỉ một bên thắng, bên kia biết để đọc lại |
| Snapshot (bản chép) | `AttemptState.getAnswers()` → `Map.copyOf` | Luồng đọc không bị luồng ghi làm hỏng |
| Utility class | `PaperShuffler`, `AnswerFormat`, `AppConfig` | Hàm thuần, không trạng thái |

---

## 10. Khác với thiết kế M3/M4

Lấy từ `docs/DESIGN-CHANGES.md`; mỗi mục: *thay đổi – lý do – ảnh hưởng*.

| # | Thay đổi | Lý do | Ảnh hưởng |
|---|---|---|---|
| Phase 1 mục 2 | `ExamSession` tách ra `SessionWindow` (record), `SessionPolicy` (record), `PaperShuffler` (generic); enum `ExamSessionService.Phase` thành `model.SessionPhase`; bỏ `closeAndSubmitAll` | SRP; máy thí sinh không có `ExamSession` nhưng vẫn phải tính giờ và trộn đề y hệt máy giảng viên | Constructor `ExamSession(id, exam, SessionWindow, SessionPolicy)` thay 7 tham số; `createSession` nhận Parameter Object; "đóng ca" thành một câu `UPDATE` trong `GradingDao.closeExpired` |
| Phase 1 mục 3 | Gói `client` mới: `SessionInfo`, `PaperItem`, `PaperOption` (không cờ đúng/sai), `ExamPaper`, `AttemptState`, `ReviewItem`, `ExamAttempt` | NFR-04: `Question`/`AnswerOption` có `isCorrect`, tới máy thí sinh là lộ đáp án | Định dạng câu trả lời không đổi (`"12"`, `"12,14"`, `"true"`, văn bản); lưu theo số câu gốc |
| Phase 1 mục 4 | Interface DAO tách theo người dùng: `StudentExamDao` (mới), `GradingDao`, `StudentLoginDao`; `ExamSessionDao` bỏ `findOfStudent`, `findClosedWithOpenSubmissions` | ISP: ứng dụng thi không thể gọi hàm nó không được dùng | `ClientContext` chỉ lắp DAO phía thí sinh |
| Phase 1 mục 5 | `SubmissionService` (M4) xóa, thay bằng `StudentExamService` (máy thí sinh) + `GradingWorker` (máy giảng viên); `ProctoringService` nhận `ViolationPolicy` qua constructor | M4 chấm trên máy thí sinh → máy thí sinh đọc được đáp án | Nộp xong bài ở `SUBMITTED` tới khi máy chấm chạy; màn hình kết quả hiện "Đang chấm bài…"; thêm kiểm tra `NOT_OWNER`; `submitted_at` khi hết giờ = giờ đóng ca |
| Phase 1 mục 1 (liên quan) | `MAX_VIOLATIONS` rời `Submission` sang `ViolationPolicy` (5 lần, gộp 3 giây) | SRP – đổi quy chế chỉ sửa một lớp | Kiểm thử mới `ViolationPolicyTest` |
| Phase 1 mục 9 | `increaseViolation` không tăng được (bài khóa ở nơi khác) thì `ProctoringService.report` đọc lại trạng thái thật | Máy thí sinh chỉ giữ bản sao; CSDL quyết định | Không còn cảnh báo "vi phạm lần N" giả |
| Phase 6 mục 1 | Màn hình làm bài tách thành `AnswerSheet`, `AnswerFormat`, `ServerClock`, `SessionEntry`, `AnswerSaver`, `ExamProctor`; `ServerClock` dùng `nanoTime`; `AttemptState` an toàn đa luồng | SRP; chỉnh đồng hồ máy trạm không ảnh hưởng; tránh lượt lưu cũ ghi đè lựa chọn mới | `ExamController` chỉ vẽ và nối |
| Phase 6 mục 2 | Hộp thoại mở qua `ExamProctor.showDialog`; cảnh báo 5 phút và mất kết nối dùng dải màu; gửi vi phạm tuần tự | Hộp thoại làm mất tiêu điểm; tránh số lần nhảy lùi | – |
| Phase 6 mục 3 | Nộp tay và hết giờ đi cùng một đường (drain rồi `submit`); màn hình kết quả dùng `AttemptState` do `submit` trả về | Không mất câu đã trả lời; phản ánh đúng trạng thái máy chấm đặt | – |
| Phase 6 mục 4 | Thêm `SessionWindow.isAlmostOver`, `ALMOST_OVER`, `formatRemaining`, `ViolationPolicy.rulesText`, `ReviewItem.fullMarkCount`, `StudentExamService.overview` | Bổ sung nhỏ cho giao diện | – |
| Phase 7 | Đồng bộ 60 giây đọc lại cả trạng thái bài (máy thứ hai tự chuyển sang kết quả); hết giờ chỉ tự nộp một lần (cờ `timedOut`) | Lỗi tìm thấy khi rà soát | – |
| Phase 8 | Ca mới mặc định mở ngay, kéo dài 60 phút (trước: mở sau 5 phút) | Giảng viên thường mở ca ngay trước giờ thi; demo cần ca mở ngay | `SessionFormController.init` |
| Phase 1 mục 7 (phần 4 chủ trì) | `v_session_for_student`, `v_exam_paper` thay `v_exam_item_for_student` (M4); `v_exam_paper` chỉ hiện đề của ca đã tới giờ mở | NFR-04, chặt hơn kế hoạch | `JdbcStudentExamDao` chỉ đọc qua khung nhìn |

---

## 11. Kiểm thử

### 11.1. Các lớp kiểm thử của phần 3 (đều chạy không cần MySQL)

| Lớp | Số ca | Ca tiêu biểu | Chứng minh gì |
|---|---|---|---|
| `SessionWindowTest` | 4 | `phânNhómTheoGiờMáyChủ` | Biên `[open, close)`: đúng giờ mở là `OPEN`, đúng giờ đóng là `CLOSED` |
| | | `vàoMuộnThìCònÍtThờiGianHơn` | Thời gian còn lại tính tới giờ đóng chung, không âm |
| | | `báoLýDoCụThểKhiChưaMởHoặcĐãĐóng` | `ensureOpen` ném đúng mã `NOT_OPEN_YET` / `CLOSED` |
| | | `giờĐóngPhảiSauGiờMở` | Compact constructor chặn, mã `SESSION_TIME` |
| `PaperShufflerTest` | 5 | `cùngSeedCùngThứTự`, `khácThíSinhKhácThứTự`, `danhSáchGốcKhôngBịĐổi` | Ba tính chất của trộn có seed |
| | | `haiPhíaThấyCùngThứTựCâuVàPhươngÁn` | `ExamSession.buildPaperFor`/`optionsFor` và `ExamPaper.shuffledFor` ra cùng thứ tự câu **và** phương án |
| | | `khôngTrộnThìGiữThứTựGốc` | Tắt hai cờ trộn thì giữ nguyên |
| `ViolationPolicyTest` | 4 | `sựKiệnTrongBaGiâyĐượcGộp` | 2999 ms gộp, đúng 3 s không gộp, lần đầu không gộp |
| | | `đủNămLầnThìKếtThúc`, `sốLầnCònLạiKhôngÂm`, `quyChếChuẩnLàNămLầnBaGiây` | Ngưỡng `>=`, `remaining` không âm, quy chế chuẩn, tham số sai bị từ chối |
| `ClientModelTest` | 6 | `đồngHồMáyChủKhôngPhụThuộcGiờMáyTrạm` | `ServerClock` với nguồn `nanoTime` giả: 90 giây trôi, đồng bộ lại |
| | | `cảnhBáoNămPhútCuốiVàĐịnhDạngĐồngHồ` | `isAlmostOver` ở phút 54/55/60; `formatRemaining` |
| | | `câuTrảLờiNhiềuĐápÁnCùngĐịnhDạngVớiLúcChấm` | `"12,14"` tăng dần; rỗng là `null`; bỏ ký tự rác |
| | | `bảnNhápĐếmCâuĐãLàmVàCâuĐánhDấu` | `AnswerSheet` đếm đúng, chuỗi trắng là xóa |
| | | `nútTrênDanhSáchCaThiTheoTrạngThái` | Quy tắc nút của `SessionEntry` |
| | | `quyChếHiệnĐúngConSốĐangÁpDụng` | Câu `rulesText()` nguyên văn |
| `SubmissionFlowTest` | 20 | `làmĐúngHếtĐượcTrọnĐiểmSauKhiMáyChấmChạy` | Máy thí sinh nộp → `SUBMITTED`, chưa có điểm; máy chấm chạy → `GRADED`, 10 điểm |
| | | `mỗiThíSinhChỉThiMộtLần` | Vào lại sau khi nộp → `ALREADY_SUBMITTED` |
| | | `thoátRaVàoLạiThìMởĐúngBàiCũ` | Cùng `submissionId`; câu trả lời, câu đánh dấu, số vi phạm còn nguyên |
| | | `nộpHaiLầnGiữKếtQuảLầnĐầu`, `máyKhácĐãNộpThìĐọcLạiTrạngTháiThật` | Nộp lần hai / máy thứ hai không ghi đè lý do kết thúc |
| | | `ngoàiGiờHoặcKhôngCóTênThìKhôngVàoĐược` | `NOT_OPEN_YET`, `CLOSED`, `NOT_ENROLLED` |
| | | `hếtGiờThìKhôngGhiĐượcVàMáyChấmTựNộp` | Sau giờ đóng ghi bị từ chối (`SUBMISSION_LOCKED`); máy chấm nộp 2 bài `TIME_OUT`, chỉ câu đã lưu được tính |
| | | `đủNămLầnViPhạmThìKếtThúcBài` | 5 sự kiện cách 10 giây → kết thúc, khóa, vẫn chấm phần đã làm; máy chấm ghi nhật ký `GRADED_VIOLATION` |
| | | `sựKiệnSátNhauĐượcGộpLàmMột` | 0 ms → lần 1; 800 ms → gộp; 5 s → lần 2 |
| | | `mấtKếtNốiLúcBáoViPhạmThìGửiLạiVẫnĐượcTính` | CSDL lỗi lần đầu, gửi lại cùng thời điểm → vẫn tính lần 1, không bị gộp |
| | | `báoViPhạmKhiBàiĐãBịMáyChấmĐóngThìKhôngĐếmThêm` | Không có cảnh báo giả; bài biết mình đã khóa, lý do `TIME_OUT` |
| | | `thíSinhKhácNhauNhậnThứTựCâuKhácNhauNhưngỔnĐịnh`, `máyThíSinhThấyCùngThứTựCâuVớiMáyGiảngViên` | Trộn ổn định qua service thật |
| | | `đápÁnChỉHiệnSauKhiCaĐóng`, `chưaNộpBàiThìChưaXemĐượcKếtQuả` | `review`: đáp án chỉ sau khi đóng ca; `NOT_SUBMITTED` |
| | | `khôngThaoTácĐượcTrênBàiCủaNgườiKhác` | `NOT_OWNER` cho cả `saveAnswer` lẫn `report` |
| | | `tổngQuanCaThiKèmBàiLàmCủaThíSinh`, `danhSáchCaThiChiaTheoTrạngThái` | `overview`, `sessionsOf` |

`SubmissionFlowTest` dựng **hai "máy"** trên cùng một `InMemoryDatabase`: `AppContext.inMemory(db)` (giảng viên, chấm bài)
và `ClientContext.inMemory(db)` (thí sinh), cùng một `ManualClock` để "tua" thời gian (`clock.set`, `clock.advance`).

Ngoài ra (lớp của phần khác nhưng kiểm tra mã phần 3):
- `JdbcIntegrationTest.ràngBuộcDuyNhấtChặnBàiLàmThứHai`, `haiMáyCùngBấmVàoThiChỉTạoMộtBài`, `đếmViPhạmSongSongKhôngMấtLượt`
  – chạy với MySQL thật.
- `JdbcSecurityTest.ghiCâuTrảLờiSauGiờĐóngBịTriggerChặn`, `bàiĐãNộpThìMáyThíSinhKhôngSửaĐược`,
  `thíSinhKhôngĐọcTrướcĐượcĐềCủaCaChưaMở` – chạy bằng tài khoản `oes_client` thật.
- `ErrorCodeTest.lỗiCaThiVàBàiLàm` (`SESSION_IN_PAST`, `SESSION_NOT_FOUND`, `NOT_STARTED`);
  `StaffServicesTest.ghiDanhCảLớpHọcPhần` (`CLASS_WRONG_SUBJECT`), `lớpChưaCóThíSinhThìKhôngMởCaĐược` (`NO_STUDENTS`).

### 11.2. Cách chạy

```bash
./mvnw test                                    # toàn bộ; kiểm thử JDBC tự bỏ qua nếu chưa đặt biến môi trường
./mvnw test -Dtest=SubmissionFlowTest          # một lớp
./mvnw test -Dtest=ViolationPolicyTest,ClientModelTest,SessionWindowTest,PaperShufflerTest,SubmissionFlowTest
OES_TEST_DB=... OES_TEST_DB_CLIENT=... ./mvnw test   # thêm kiểm thử với MySQL thật (hai tài khoản)
```

### 11.3. `UiSmoke` – chạy thử giao diện tự động

`test/ui/UiSmoke.java` **không phải JUnit**: là một `main` chạy ứng dụng thật trong Xvfb (màn hình ảo), tự gõ, tự bấm,
chụp ảnh. Cú pháp ghi ở đầu tệp; chế độ `exam` nhận thêm tên tài khoản và kịch bản; `exam-seed` dựng đề 6 câu đủ 4 loại
và một ca cho `ui_sv1`, `ui_sv2`.

| Kịch bản | Làm gì | Đã chứng minh |
|---|---|---|
| `full` | Trả lời, đánh dấu, rời cửa sổ một lần, nộp, gọi máy chấm, xem kết quả | Hộp thoại "lần 1/5"; đóng hộp thoại thì quay lại toàn màn hình, không tự sinh vi phạm; có điểm |
| `violate5` | Rời cửa sổ 5 lần, mỗi lần cách 3,3 giây | Cảnh báo 1/5 … 4/5, lần 5 kết thúc; CSDL ghi `VIOLATION` |
| (nhánh `default`, gọi là `timeout`) | Trả lời một câu rồi chờ | Hết giờ tự nộp, lý do `TIME_OUT` (PROGRESS ghi: giờ nộp đúng giờ đóng ca) |
| `reconnect` | Dừng mysqld bằng `SIGSTOP`, trả lời thêm, rồi `SIGCONT` | Dải vàng hiện rồi tắt; đủ 3 câu trong CSDL |
| `resume1` → `resume2` | Trả lời 2 câu rồi `Runtime.getRuntime().halt(0)` (tắt đột ngột); mở lại | Nút "Tiếp tục làm bài", số câu đã làm còn nguyên |
| `second` | Máy thứ hai cùng tài khoản, không thao tác | Tự chuyển sang màn hình kết quả khi máy kia nộp |
| `deaddb` | CSDL không chạy khi mở ứng dụng | Báo lỗi sau vài giây, hiện màn hình nhập máy chủ, không treo |

**Giới hạn của `UiSmoke`:** Xvfb không có trình quản lý cửa sổ, nên "rời cửa sổ" được giả lập bằng `leaveWindow()` =
`mainStage().setFullScreen(false)`. Alt+Tab, phím Windows, thu nhỏ cửa sổ thật **chưa** chạy tự động – phải làm tay các
dòng **[Windows]** 35–39 và 51 trong `docs/manual-test-checklist.md`.

---

## 12. Giới hạn đã biết

Trả lời trung thực, kèm cách khắc phục nếu có.

1. **Alt+Tab, thu nhỏ, phím Windows thật chỉ được kiểm tra bằng giả lập** (mục 11.3). Đường mã đã được đọc kỹ và
   `UiSmoke` kiểm tra nhánh "thoát toàn màn hình"; nhóm phải chạy tay trên máy Windows trước buổi bảo vệ.
2. **Không chặn được Alt+Tab hay phím Windows**, chỉ phát hiện và tính vi phạm. Chặn ở mức hệ điều hành cần mã riêng cho
   từng hệ điều hành – trái NFR-11.
3. **UC-13 luồng 3a chưa làm**: nếu không vào được toàn màn hình thì không cho thi. Hiện `ExamProctor.start` gọi
   `setFullScreen(true)` nhưng không kiểm tra kết quả. JavaFX trên Windows/macOS/Linux đều hỗ trợ toàn màn hình nên gần
   như không xảy ra; nếu làm, kiểm tra `stage.isFullScreen()` sau khi bật và từ chối bắt đầu.
4. **Dùng chung tài khoản `oes_client`**: CSDL không phân biệt thí sinh nào gửi lệnh. `requireOwn` chặn ở tầng service,
   nhưng ứng dụng thi bị sửa vẫn ghi được vào bài người khác (không đọc được đáp án, không sửa được điểm). Chặn hẳn cần
   máy chủ ứng dụng ở giữa – ngoài công nghệ được phép (câu hỏi chung số 14).
5. **Đồng hồ máy trạm bị chỉnh**: chỉ có kiểm thử đơn vị (`ClientModelTest.đồngHồMáyChủKhôngPhụThuộcGiờMáyTrạm`), chưa
   chạy tay trên Windows (dòng 51 của danh sách kiểm thử tay).
6. **Đã sửa sau khi rà soát – mốc gộp vi phạm đo bằng đồng hồ máy trạm.** Bản đầu `ExamProctor.leave` gửi
   `Instant.now()`; nếu thí sinh chỉnh lùi đồng hồ giữa giờ thi, `Duration.between` ra số âm, nhỏ hơn 3 giây, nên mọi lần
   rời cửa sổ sau đó bị coi là "gộp" và không được tính. Nay `ExamProctor` ghi `startWall = Instant.now()` và
   `startNanos = System.nanoTime()` một lần khi tạo, mỗi lần rời cửa sổ gửi `startWall.plusNanos(System.nanoTime() - startNanos)`:
   khoảng cách giữa hai sự kiện luôn đúng dù đồng hồ hệ điều hành bị chỉnh – cùng ý tưởng với `ServerClock`. Lớp này
   dùng `Stage` của JavaFX nên không có kiểm thử JUnit; kiểm bằng tay theo dòng [Windows] trong
   `docs/manual-test-checklist.md`.
7. **Vi phạm xảy ra đúng lúc mất mạng – đã sửa sau khi rà soát.** Bản trước ghi `lastCounted.put(id, eventTime)`
   **trước** `dao.increaseViolation(id)`. Nếu lệnh CSDL lỗi, `ExamController.onLeave` gửi lại đúng thời điểm `when` sau
   3 giây, `isMerged(when, when)` trả `true` (cách nhau 0 giây) nên lần vi phạm bị coi là "gộp" và mất – thí sinh cố ý
   ngắt mạng trước mỗi lần Alt+Tab sẽ không bao giờ bị tính. Nay `lastCounted.put` chạy **sau** khi CSDL đã tăng bộ
   đếm; kiểm thử `SubmissionFlowTest.mấtKếtNốiLúcBáoViPhạmThìGửiLạiVẫnĐượcTính` tái hiện lỗi (đỏ trước khi sửa, xanh
   sau khi sửa). Nếu bị hỏi, đây là ví dụ tốt về "thứ tự câu lệnh cũng là thiết kế": chỉ ghi nhớ trạng thái khi việc
   chính đã thành công.
8. **Câu vừa đổi trong 500 ms cuối có thể không được lưu** khi: tắt máy đột ngột, hoặc bài bị kết thúc do vi phạm lần 5
   (`ProctoringService` gọi `submit` ngay, `ExamController.finish` dừng hàng đợi). Đúng tinh thần "khóa ngay" của BR-05,
   nhưng nên biết để trả lời.
9. **Chưa sửa / xóa / gia hạn được ca thi** sau khi mở: `ExamSessionDao` chỉ có `insert` và các hàm đọc.
10. **Tạo ca và khóa đề không cùng một giao dịch**: `sessionDao.insert` là một giao dịch, `examDao.lock` là lệnh sau.
    Nếu mất kết nối đúng giữa hai lệnh, ca đã có mà đề chưa khóa; lần mở ca sau sẽ khóa lại (`if (!exam.isLocked())`).
11. **Một vài hàm chỉ kiểm thử dùng**: `ExamSession.ensureCanEnter`, `buildPaperFor`, `optionsFor`,
    `StudentExamService.sessionsOf` – màn hình dùng `StudentExamService.enter` và `overview`. Giữ lại vì là đặc tả phía
    giảng viên và bảo đảm hai phía trộn giống nhau.
12. **Mốc gộp vi phạm chỉ nằm trong bộ nhớ** (`lastCounted`): tắt ứng dụng mở lại thì mất mốc – sự kiện đầu tiên sau khi
    mở lại luôn được tính. Không gây thiệt cho thí sinh trung thực.

---

## 13. Đoạn demo của mình

"Phần 3 · Nguyễn Hoàng Trung – Thí sinh làm bài (3 phút)" trong `docs/DEMO-SCRIPT.md`, thêm cột mã phía sau:

| Bấm | Nói | Phía sau là mã nào |
|---|---|---|
| Máy B: đăng nhập `b24dcce098` / `sv123456` | "Từ đăng nhập tới làm bài chỉ ba thao tác." | `StudentAuthService.login` (phần 1) → `ExamClientApp.showHome` → `SessionListController.onRefresh` → `StudentExamService.overview` → `v_session_for_student` |
| Vào thi (ca vừa mở) → đọc quy chế → tick đồng ý → Bắt đầu làm bài | "Cửa sổ chuyển toàn màn hình; Esc và Alt+F4 bị chặn." | `SessionEntry.canEnter` → `RulesController.init` (`ViolationPolicy.rulesText`) → `RulesController.onStart` → `StudentExamService.enter` → `ExamController.init` → `ExamProctor.start` (`KeyCombination.NO_MATCH`, `Event::consume`) |
| Trả lời vài câu, bấm số câu ở lưới, Đánh dấu xem lại | "Mỗi lần trả lời, nửa giây sau là lưu xuống CSDL. Rút mạng thì có dải vàng và tự lưu lại khi có mạng." | `ExamController.onAnswer` → `AnswerSaver.answerChanged` (`DEBOUNCE` 500 ms) → `StudentExamService.saveAnswer` → `INSERT ... ON DUPLICATE KEY UPDATE`; `offlineProperty`, `RETRY` 3 giây; `onFlag` → `setFlag` |
| Alt+Tab ra ngoài rồi quay lại | "Rời cửa sổ là một lần vi phạm; các sự kiện cách nhau dưới 3 giây gộp làm một. Đủ 5 lần thì kết thúc bài." | `ExamProctor.focusListener` → `ExamController.onLeave` → `ProctoringService.report` → `ViolationPolicy.isMerged` → `increaseViolation` (`LAST_INSERT_ID`) → `handleViolation` → `ExamProctor.showDialog` |
| (Máy C nếu có: Alt+Tab 5 lần) | "Bài này bị kết thúc ngay, vẫn được chấm phần đã làm." | `ViolationPolicy.isTerminal` → `StudentExamService.submit(..., EndReason.VIOLATION)` → `ExamController.finish` → `ResultController` (phần 4) |
| Nộp bài → hộp thoại báo số câu chưa trả lời → Nộp bài | "Máy thí sinh **không chấm bài** – nó không có đáp án. Màn hình chờ 'Đang chấm bài…'." | `ExamController.onSubmit` (`AnswerSheet.unansweredCount`, `flaggedCount`) → `submit` → `AnswerSaver.drain` → `StudentExamService.submit` → `markSubmitted` → `SUBMITTED`; `GradingWorker` (phần 4) chấm |

**Chuẩn bị riêng:** thử Alt+Tab trên đúng laptop demo (phải hiện "Bạn đã rời cửa sổ làm bài (lần 1/5)"); ca do phần 2 mở
trong demo có giờ đóng 15 phút sau – đủ cho 3 phút demo.

---

## 14. Câu hỏi vấn đáp

### 14.1. Cơ bản

**1. Ca thi gồm những gì? Lưu ở đâu?**
Một đề + một khung giờ (`SessionWindow`) + ba tùy chọn (`SessionPolicy`) + danh sách ghi danh – lớp `ExamSession`. Lưu ở
bảng `exam_session` (giờ, ba cờ) và `session_student` (ghi danh), chèn trong một giao dịch bởi `JdbcExamSessionDao.insert`.

**2. Vì sao `SessionWindow` là `record`? Có tạo được khung giờ đóng trước giờ mở không?**
Khung giờ là giá trị bất biến. Compact constructor gọi `ValidationException.require(closeTime.isAfter(openTime),
"SESSION_TIME", "Giờ đóng ca thi phải sau giờ mở.")`, nên không tạo được. CSDL còn có `CHECK (close_time > open_time)`.

**3. Thí sinh vào muộn 20 phút trong ca 60 phút thì còn bao nhiêu thời gian? Mã nào quyết định?**
Còn 40 phút. `SessionWindow.remainingSeconds(now)` tính tới `closeTime` chung (quyết định số 2, BR-03).
`ExamController.onTick` dùng nó mỗi giây. Kiểm thử `SessionWindowTest.vàoMuộnThìCònÍtThờiGianHơn`.

**4. Điều kiện vào thi là gì, kiểm ở đâu?**
`StudentExamService.enter`: chưa nộp bài (`ALREADY_SUBMITTED`), có tên trong `session_student` (`NOT_ENROLLED`), đang trong
giờ (`SessionWindow.ensureOpen` – `SESSION_NOT_OPEN_YET` / `SESSION_CLOSED`). Thêm `Permissions.requireStudent`.

**5. Thí sinh thoát ứng dụng rồi vào lại thì sao?**
`enter` thấy bài `IN_PROGRESS` qua `findAttempt` → dùng lại bài đó (`existing.orElseGet(...)`); câu trả lời, câu đánh dấu,
số lần vi phạm đọc từ CSDL. Đề vẫn đúng thứ tự cũ vì seed `PaperShuffler.seedFor(sessionId, studentId)` không đổi.
Kiểm thử `SubmissionFlowTest.thoátRaVàoLạiThìMởĐúngBàiCũ`.

**6. Máy thí sinh có đọc được đáp án không? Vì sao chắc chắn?**
Không. (1) `ClientContext` không có DAO đọc câu hỏi; (2) mô hình `client` không có trường đáp án – `PaperOption` chỉ có
`optionId`, `content`; (3) `oes_client` chỉ đọc `v_exam_paper`, không có quyền trên `question`, `answer_option`.

**7. Câu trả lời được lưu lúc nào?**
500 ms sau lần thay đổi cuối (`AnswerSaver.DEBOUNCE`). Gõ liền nhiều chữ thì chỉ lưu một lần. Đánh dấu xem lại thì lưu ngay.

**8. Ứng dụng biết thí sinh rời cửa sổ bằng cách nào?**
`ExamProctor` nghe ba thuộc tính của `Stage`: `fullScreenProperty` thành `false`, `focusedProperty` thành `false`
(Alt+Tab), `iconifiedProperty` thành `true` (thu nhỏ). Cả ba gọi `leave()` → `onLeave.accept(...)` với thời điểm đo bằng `System.nanoTime()` tính từ lúc tạo `ExamProctor`.

**9. Nộp bài xong có điểm ngay không?**
Không. `StudentExamService.submit` chỉ chuyển `IN_PROGRESS → SUBMITTED`. Máy giảng viên chạy `GradingWorker` mỗi 3 giây
(phần 4) mới chấm và đặt `GRADED`. Màn hình kết quả hỏi lại 2 giây/lần.

**10. Ba nhóm trên danh sách ca thi được chia thế nào?**
`StudentExamService.overview` lấy giờ máy chủ một lần, mỗi ca gọi `window.phaseAt(now)` → `UPCOMING` / `OPEN` / `CLOSED`.
`SessionListController.render` đưa vào ba hộp `upcomingBox`, `openBox`, `closedBox`.

### 14.2. Đào sâu

**11. Vì sao không dùng `LocalDateTime.now()` cho đồng hồ đếm ngược? Sai số bao nhiêu?**
Đồng hồ máy trạm chỉnh được và không đồng bộ giữa các máy (quyết định số 6). `ServerClock` lấy `SELECT NOW()` một lần rồi
cộng thời gian trôi đo bằng `System.nanoTime()` – không bị ảnh hưởng khi chỉnh giờ hệ điều hành; đồng bộ lại mỗi 60 giây
(`RESYNC_EVERY_SECONDS`). Sai số dưới 1 giây (`NOW()` tính tới giây, cộng độ trễ mạng); không quan trọng vì trigger dùng
`NOW()` của máy chủ để quyết định giờ đóng.

**12. Hai máy cùng tài khoản bấm "Vào thi" đúng cùng lúc – có tạo hai bài không?**
Không. `UNIQUE (session_id, student_id)` chỉ cho một `INSERT` thành công. Máy chậm hơn nhận lỗi 1062 → `JdbcErrors.translate`
→ `DuplicateKeyException` → `StudentExamService.create` bắt và đọc lại bài máy kia vừa tạo. Kiểm tra trong Java (`findAttempt`
trước khi chèn) không đủ, vì giữa hai bước máy kia có thể chen vào. Kiểm thử `JdbcIntegrationTest.haiMáyCùngBấmVàoThiChỉTạoMộtBài`.

**13. Vì sao `increaseViolation` không đọc số cũ, cộng 1, rồi ghi?**
Hai sự kiện gần như đồng thời sẽ cùng đọc N và cùng ghi N+1 – mất một lần. `UPDATE ... SET violation_count =
LAST_INSERT_ID(violation_count + 1)` cộng ngay trong CSDL (nguyên tử); `SELECT LAST_INSERT_ID()` trên cùng kết nối đọc lại
giá trị mình vừa ghi. Kiểm thử `JdbcIntegrationTest.đếmViPhạmSongSongKhôngMấtLượt` (4 luồng → 1, 2, 3, 4).

**14. Trong `AnswerSaver`, vì sao hàng đợi là `Map` chứ không là `List`? `removeIfUnchanged` để làm gì?**
`Map` theo số câu giữ **giá trị mới nhất** của mỗi câu: đổi ý nhiều lần lúc mất mạng chỉ gửi lần cuối. `removeIfUnchanged`
chỉ xóa mục khỏi hàng đợi nếu giá trị vẫn là giá trị vừa gửi; nếu trong lúc gửi thí sinh đổi ý tiếp, mục được giữ để
lượt sau gửi giá trị mới – không bị mất.

**15. Vì sao có cả `AttemptState` lẫn `AnswerSheet`?**
`AttemptState` = CSDL đã nhận (service cập nhật trên luồng nền); `AnswerSheet` = thí sinh đang thấy. Gộp làm một thì khi
mạng chậm, lượt lưu A về muộn ghi đè lựa chọn B mới hơn trên màn hình (DESIGN-CHANGES Phase 6).

**16. Vì sao mọi hàm của `AttemptState` là `synchronized`, còn `getAnswers` trả bản chép?**
Service ghi trên luồng nền trong khi màn hình đọc trên luồng giao diện. `synchronized` bảo đảm mỗi lần đọc/ghi trọn vẹn
và thấy giá trị mới nhất. `Map.copyOf` để nơi nhận duyệt map an toàn trong khi luồng khác ghi tiếp.

**17. Hộp thoại cảnh báo làm cửa sổ mất tiêu điểm – sao không bị tính thêm một vi phạm?**
Mọi hộp thoại trong lúc thi mở qua `ExamProctor.showDialog`: đặt `paused = true` nên `leave()` bỏ qua sự kiện. Khi hộp thoại
đóng, `resumePending = true`; chỉ khi cửa sổ đã toàn màn hình **và** có tiêu điểm thì `tryResume()` mới bỏ `paused`. Lưới an
toàn `RESUME_FALLBACK` 2 giây: quá 2 giây vẫn chưa quay lại thì `forceResume()` bật nghe lại và tính một lần rời cửa sổ nếu
thí sinh đang ở ngoài.

**18. Vì sao phải gộp sự kiện dưới 3 giây? So với cái gì?**
Một lần Alt+Tab, JavaFX phát liền 2–3 sự kiện (thoát toàn màn hình, mất tiêu điểm…). Không gộp thì một lần bị đếm thành 3.
`ViolationPolicy.isMerged(lastCounted, event)` so với **lần được tính gần nhất**, không phải sự kiện gần nhất – nên rời
cửa sổ liên tục cũng không thể "kéo dài" mãi khoảng gộp.

**19. Thí sinh sửa mã ứng dụng thi để ghi câu trả lời sau giờ đóng thì sao?**
Trigger `trg_answer_client_insert` / `trg_answer_client_update` kiểm tra `sb.status = 'IN_PROGRESS' AND es.close_time > NOW()`
với giờ máy chủ, báo `SIGNAL SQLSTATE '45000'` – CSDL từ chối. Kiểm thử `JdbcSecurityTest.ghiCâuTrảLờiSauGiờĐóngBịTriggerChặn`.
Ứng dụng thật nhận lỗi này dưới dạng `SessionRuleException(SUBMISSION_LOCKED)` và chuyển sang màn hình kết quả.

**20. Máy chấm đã đóng ca, thí sinh vẫn Alt+Tab – có hiện "lần N/5" không?**
Không. `increaseViolation` có `AND status = 'IN_PROGRESS'` nên không tăng; giá trị trả về bằng giá trị cũ →
`ProctoringService.report` gọi `refresh`, đánh dấu bản sao đã nộp, trả `count` thật – màn hình chuyển sang kết quả.
Kiểm thử `SubmissionFlowTest.báoViPhạmKhiBàiĐãBịMáyChấmĐóngThìKhôngĐếmThêm`.

**21. Vì sao seed trộn phương án khác seed trộn câu?**
Nếu mọi câu dùng chung một seed, các câu có cùng số phương án sẽ bị đảo theo cùng một kiểu – thí sinh đoán được quy luật.
`PaperShuffler.optionSeed(paperSeed, questionId)` = `paperSeed * 31L + questionId` cho mỗi câu một seed riêng.

**22. Trong `enter`, vì sao kiểm tra "đã nộp" trước "giờ"?**
Để câu báo đúng nhất: thí sinh đã nộp quay lại sau giờ đóng thấy "Bạn đã nộp bài của ca thi này. Mỗi thí sinh chỉ được thi
một lần." thay vì "Ca thi đã kết thúc…". Bài dở vào lại vẫn phải qua `ensureOpen` – hết giờ thì không làm tiếp được.

**23. Hết giờ mà máy thí sinh đã tắt thì bài có được nộp không? `submitted_at` là giờ nào?**
Có. Máy chấm chạy `JdbcGradingDao.closeExpired`: một câu `UPDATE` chuyển mọi bài `IN_PROGRESS` của ca quá giờ sang
`SUBMITTED`, `end_reason = 'TIME_OUT'`, `submitted_at = es.close_time` – đúng BR-03 dù máy chấm chạy trễ (FR-26, phần 4).
Nếu máy thí sinh còn chạy thì `ExamController.onTick` tự nộp trước (cờ `timedOut`).

**24. Vì sao cảnh báo 5 phút và mất kết nối dùng dải màu chứ không dùng hộp thoại?**
Hộp thoại lấy tiêu điểm khỏi cửa sổ chính. Cảnh báo tự bật (không do thí sinh bấm) mà dùng hộp thoại thì phải đi qua
`showDialog`, và nếu xuất hiện đúng lúc thí sinh đang thao tác sẽ gây rối; dải `noticeBanner` / `offlineBanner` nằm trong
màn hình, không ảnh hưởng tiêu điểm.

**25. Vì sao `ExamController.onLeave` chỉ gửi một sự kiện vi phạm tại một thời điểm?**
Hai lượt gửi song song có thể về không theo thứ tự (lượt "3" về trước lượt "2"), nhãn sẽ nhảy 1/5 → 3/5 → 2/5. Cờ
`reporting` + `queuedLeave` xếp hàng: xong lượt này mới gửi lượt sau.

**26. `ExamSessionService.createSession` khóa đề để làm gì? Vì sao kiểm tra quyền đầu tiên?**
BR-09: đề đã dùng cho ca thi không được sửa, nếu không bài đã làm sẽ chấm theo đề khác. Kiểm tra quyền
(`Permissions.requireManage`) trước mọi kiểm tra dữ liệu để người không có quyền không dò được thông tin qua câu báo lỗi.

**27. `submit` trả `AttemptState` – sao không trả `void`?**
Vì có thể máy khác hoặc máy chấm đã nộp trước: `markSubmitted` trả `false`, `submit` trả trạng thái thật từ `refresh`
(lý do kết thúc có thể là `TIME_OUT` chứ không phải `MANUAL`). `ExamController.finish` đưa đúng trạng thái này sang màn
hình kết quả.

### 14.3. Sửa trực tiếp

**28. "Đổi ngưỡng 5 lần thành 3 lần ngay tại chỗ."**
- Sửa **một hằng**: `oes/model/ViolationPolicy.java` → `public static final int MAX_VIOLATIONS = 3;`.
  `standard()`, `rulesText()` ("Rời khỏi cửa sổ làm bài 3 lần…"), nhãn "Vi phạm: N/3" và hộp thoại "(lần N/3)" trong
  `ExamController.updateViolations` / `handleViolation` đều đọc hằng này nên tự đổi.
- **Không** chỉ sửa `ClientContext` thành `new ViolationPolicy(3, ...)`: quy chế áp dụng sẽ là 3 nhưng `ExamController`
  vẫn hiện "/5" vì đọc thẳng `ViolationPolicy.MAX_VIOLATIONS`.
- Kiểm thử ghi cứng số 5 sẽ đỏ và phải sửa theo: `ViolationPolicyTest.đủNămLầnThìKếtThúc`, `sốLầnCònLạiKhôngÂm`,
  `quyChếChuẩnLàNămLầnBaGiây`; `ClientModelTest.quyChếHiệnĐúngConSốĐangÁpDụng`; `SubmissionFlowTest.đủNămLầnViPhạmThìKếtThúcBài`
  (vòng lặp 5 lần và `assertEquals(5, s.getViolationCount())`).
- Chạy: `./mvnw test -Dtest=ViolationPolicyTest,ClientModelTest,SubmissionFlowTest` rồi `./mvnw test`.
- Ghi vào `docs/DESIGN-CHANGES.md` (thay đổi quyết định nghiệp vụ số 5).

**29. "Đổi 3 giây thành 5 giây."** Hỏi lại thầy muốn đổi **cái nào**, vì có ba loại "3 giây":
- *Khoảng gộp vi phạm*: `ViolationPolicy.MERGE_WINDOW = Duration.ofSeconds(5)`. `rulesText()` tự đổi. Kiểm thử phải sửa:
  `ViolationPolicyTest.sựKiệnTrongBaGiâyĐượcGộp` (mốc 2999 ms và 3 s), `quyChếChuẩnLàNămLầnBaGiây`,
  `ClientModelTest.quyChếHiệnĐúngConSốĐangÁpDụng`. `SubmissionFlowTest.sựKiệnSátNhauĐượcGộpLàmMột` vẫn qua (đúng 5 giây là
  lần mới vì so `< 0`). `UiSmoke` kịch bản `violate5` chờ `Thread.sleep(3300)` giữa hai lần rời cửa sổ – phải tăng lên hơn
  5 giây, nếu không mọi lần sau lần đầu đều bị gộp và kịch bản không bao giờ kết thúc bài.
- *Thử lại lưu bài khi mất mạng*: `AnswerSaver.RETRY`.
- *Thử lại gửi vi phạm / nộp bài khi mất mạng*: hai chỗ `new PauseTransition(Duration.seconds(3))` trong
  `ExamController.onLeave` (nhánh lỗi) và `ExamController.submit` (nhánh lỗi) – đang là số viết thẳng, không phải hằng.
- (Máy chấm chạy 3 giây/lần là của phần 4 – `GradingScheduler`.)

**30. "Đổi debounce 500 ms thành 1 giây."**
`oes/ui/AnswerSaver.java` → `private static final Duration DEBOUNCE = Duration.millis(1000);`. Không có kiểm thử JUnit cho
`AnswerSaver` (cần bộ công cụ JavaFX đang chạy); kiểm tra bằng chạy ứng dụng (`./mvnw javafx:run@client`) hoặc `UiSmoke full`.
Nên sửa luôn câu "chờ 500 ms" trong chú thích đầu lớp và câu nói trong kịch bản demo.

**31. "Cảnh báo khi còn 10 phút thay vì 5 phút."**
`SessionWindow.ALMOST_OVER = Duration.ofMinutes(10)`. Câu "Còn dưới … phút" trong `ExamController.onTick` lấy
`ALMOST_OVER.toMinutes()` nên tự đổi. Sửa `ClientModelTest.cảnhBáoNămPhútCuốiVàĐịnhDạngĐồngHồ` (phút 54 khi đó đã là
"sắp hết"). Chạy `./mvnw test -Dtest=ClientModelTest`.

**32. "Đồng bộ giờ máy chủ 30 giây/lần."**
`ExamController.RESYNC_EVERY_SECONDS = 30`. Hệ quả: máy thứ hai của cùng tài khoản phát hiện bài đã nộp nhanh hơn, đổi lại
mỗi máy hỏi CSDL nhiều gấp đôi (60 máy × 2 truy vấn / 30 giây – vẫn rất nhẹ).

**33. "Ca mặc định kéo dài 90 phút, mặc định không trộn phương án."**
- 90 phút: `SessionFormController.init`, hai chỗ `open.plusMinutes(60)` → `plusMinutes(90)`.
- Không trộn phương án: `fxml/session-form.fxml`, ô `shuffleOptionsBox` bỏ `selected="true"`.
- Không có kiểm thử tự động; mở `./mvnw javafx:run@staff` → Đề thi → Mở ca thi… để xem.

**34. "Không cho vào thi mới nếu ca chỉ còn dưới 10 phút" (thêm quy tắc).**
Đặt ở **service**, không ở controller. Trong `StudentExamService.enter`, sau `session.window().ensureOpen(now);`, *mã đề
xuất – chưa có trong dự án*:
```java
        if (existing.isEmpty() && session.window().remainingSeconds(now) < 10 * 60) {
            throw new SessionRuleException("TOO_LATE_TO_START",
                    "Ca thi chỉ còn dưới 10 phút, không thể bắt đầu bài mới. Hãy liên hệ giám thị.");
        }
```
`existing.isEmpty()` để thí sinh đang làm dở vẫn vào lại được. Thêm một ca vào `SubmissionFlowTest` (tua `clock.set(T0.plusMinutes(55))`
rồi `assertThrows`) và vào `ErrorCodeTest.lỗiCaThiVàBàiLàm` (mỗi mã lỗi phải có kiểm thử). Ghi `DESIGN-CHANGES.md`.

**35. "Vì sao trong `ProctoringService.report`, `lastCounted.put` nằm sau `dao.increaseViolation`? Đổi lên trước thì sao?"**
Đổi lên trước là lỗi đã gặp và đã sửa (mục 12, giới hạn số 7). Nếu nhớ thời điểm trước rồi lệnh CSDL lỗi vì mất mạng,
màn hình gửi lại đúng thời điểm đó; `ViolationPolicy.isMerged` thấy hai thời điểm cách nhau 0 giây nên coi là gộp, lần
vi phạm không bao giờ được ghi. Thầy bảo thử: đổi hai dòng ngược lại rồi chạy
`./mvnw test -Dtest=SubmissionFlowTest` – ca `mấtKếtNốiLúcBáoViPhạmThìGửiLạiVẫnĐượcTính` sẽ đỏ. Kiểm thử này dùng
`java.lang.reflect.Proxy` tạo một `StudentExamDao` giả: chuyển mọi lời gọi sang DAO trong bộ nhớ, riêng
`increaseViolation` lần đầu ném `DataAccessException`.

---

## 15. Thuật ngữ

| Thuật ngữ | Nghĩa trong dự án |
|---|---|
| Ca thi (session) | Một lần tổ chức thi: đề + giờ mở/đóng + tùy chọn + danh sách thí sinh (`ExamSession`, `exam_session`) |
| Ghi danh (enroll) | Đưa thí sinh vào danh sách dự thi của ca (`session_student`) |
| Bài làm (attempt/submission) | Một dòng `submission`; trên máy thí sinh là `AttemptState` |
| `IN_PROGRESS` / `SUBMITTED` / `GRADED` | Đang làm / đã nộp, chờ chấm / đã chấm |
| Seed | Số khởi đầu cho bộ sinh số ngẫu nhiên; cùng seed → cùng dãy → cùng thứ tự trộn |
| Debounce | Chờ người dùng ngừng thao tác một khoảng rồi mới xử lý (500 ms trong `AnswerSaver`) |
| Gộp vi phạm (merge) | Các sự kiện rời cửa sổ cách lần được tính gần nhất dưới 3 giây chỉ tính một lần |
| Luồng giao diện (JavaFX Application Thread) | Luồng duy nhất được vẽ và sửa giao diện |
| Luồng nền | Luồng của `BackgroundTasks`, nơi gọi service/CSDL |
| Khung nhìn (view) | Câu `SELECT` được đặt tên, dùng như bảng; ở đây để lọc bỏ cột đáp án |
| Trigger | Đoạn mã chạy trong CSDL trước mỗi `INSERT`/`UPDATE`, ở đây để chặn máy thí sinh |
| Quyền theo cột | `GRANT INSERT (cột...)`: chỉ cho ghi đúng những cột liệt kê |
| Compare-and-set | Chỉ ghi khi giá trị hiện tại đúng như mong đợi (`WHERE status = 'IN_PROGRESS'`) |
| Nguyên tử (atomic) | Thao tác không bị chen ngang giữa chừng (`UPDATE ... violation_count + 1`) |
| Toàn màn hình (full screen) | `Stage.setFullScreen(true)`; thoát ra bị tính là rời cửa sổ |
| Giờ máy chủ | `SELECT NOW()` của MySQL – giờ chuẩn duy nhất |

---

## 16. Tự kiểm tra trước buổi vấn đáp

Phải tự nói được, **không nhìn tài liệu**:

- ☐ Phần 3 gồm những tệp nào; nói đoạn "1 phút" ở mục 0.
- ☐ Ba lớp tách từ `ExamSession` và vì sao tách (máy thí sinh không được có `Exam`).
- ☐ `SessionWindow`: khoảng `[openTime, closeTime)`, `phaseAt`, `remainingSeconds`, `isAlmostOver`, `ensureOpen` và hai mã lỗi.
- ☐ Vì sao mọi hàm thời gian nhận `now` từ ngoài; `now` lấy từ đâu (`SELECT NOW()`).
- ☐ `ServerClock`: công thức, vì sao `nanoTime`, đồng bộ 60 giây, sai số.
- ☐ Seed `Objects.hash(sessionId, studentId)`; vì sao vào lại vẫn đúng thứ tự; vì sao seed phương án riêng.
- ☐ Ba lớp phòng thủ NFR-04 của máy thí sinh (`ClientContext`, `PaperOption`, quyền `oes_client`).
- ☐ `StudentExamService.enter`: thứ tự kiểm tra, ba khả năng, xử lý `DuplicateKeyException`.
- ☐ `UNIQUE (session_id, student_id)` chặn gì mà mã Java không chặn được.
- ☐ `saveAnswer`: kiểm tra gì ở service, gì ở trigger; câu `INSERT ... ON DUPLICATE KEY UPDATE`.
- ☐ `AnswerSaver`: 500 ms, `Map` giữ giá trị mới nhất, `removeIfUnchanged`, 3 giây thử lại, dừng khi `SUBMISSION_LOCKED`, `drain` trước khi nộp.
- ☐ `AttemptState` vs `AnswerSheet`; vì sao `synchronized`.
- ☐ `ExamProctor`: chặn Esc (`KeyCombination.NO_MATCH`), Alt+F4 (`Event::consume`), ba sự kiện, `showDialog`, lưới an toàn 2 giây.
- ☐ `ViolationPolicy`: 5 lần (`>=`), gộp `< 3 giây` so với lần **được tính** gần nhất, `rulesText`.
- ☐ `ProctoringService.report`: các nhánh gộp / tăng / bài khóa ở nơi khác / kết thúc; vì sao tăng bộ đếm trong CSDL (`LAST_INSERT_ID`).
- ☐ Nộp tay và hết giờ: cùng `ExamController.submit`; cờ `timedOut`; `markSubmitted` trả `false` thì sao.
- ☐ Hết giờ khi máy thí sinh đã tắt: `closeExpired`, `submitted_at = close_time`.
- ☐ Mất mạng ở 4 chỗ (lưu, vi phạm, đồng bộ, nộp) xử lý ra sao; thí sinh thấy gì.
- ☐ Hai máy cùng tài khoản: vào cùng lúc, một máy nộp – máy kia biết bằng 3 đường nào.
- ☐ Khung nhìn `v_exam_paper` bỏ những cột nào, vì sao lọc `open_time <= NOW()`, vì sao chỉ lấy phương án của câu một/nhiều đáp án.
- ☐ Quyền theo cột của `oes_client` trên `submission`, `answer`; bốn trigger chặn gì (mức "phần 3 đụng vào").
- ☐ Đổi 5 lần → 3, đổi 3 giây, đổi 500 ms: sửa hằng nào, kiểm thử nào đỏ, chạy lệnh gì.
- ☐ Các giới hạn ở mục 12, đặc biệt: Alt+Tab chỉ giả lập, UC-13 3a chưa làm, dùng chung `oes_client`.
- ☐ Tự chạy được `./mvnw test -Dtest=SubmissionFlowTest` và `./mvnw javafx:run@client`; tự Alt+Tab thử trên máy Windows.
