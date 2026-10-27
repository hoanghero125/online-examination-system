# Kế hoạch triển khai – Online Examination System (Nhóm 2)

Tài liệu này dành cho Claude Code. Đọc hết một lượt trước khi viết dòng mã đầu tiên, và đọc lại mục 2 (quy ước bắt buộc) mỗi khi bắt đầu một phase mới.

---

## 0. Bối cảnh

Đây là bài tập lớn môn Lập trình hướng đối tượng, đề tài số 16, nhóm 4 sinh viên. Bốn milestone tài liệu đã nộp xong:

| Milestone | Nội dung | Trạng thái |
|---|---|---|
| M1 | Yêu cầu: 35 FR, 12 NFR, 11 BR, 18 use case | Đã nộp |
| M2 | Phân tích hướng đối tượng: 22 lớp, CRC, mô hình miền | Đã nộp |
| M3 | Thiết kế UML: sơ đồ lớp, Factory, Strategy, DAO, cây ngoại lệ | Đã nộp |
| M4 | Cài đặt phần lõi: model, DAO JDBC, service, 39 kiểm thử | Đã nộp, mã nguồn là điểm xuất phát |
| M5 | SOLID, refactor, mẫu thiết kế | **Làm trong plan này** |
| M6 | Sản phẩm cuối: giao diện JavaFX, kiểm thử, demo | **Làm trong plan này** |

Mã nguồn khởi điểm nằm trong `Group2-Milestone4-OnlineExaminationSystem-source.zip`.

Tài liệu này **tự chứa đủ ngữ cảnh** để làm việc: tóm tắt từng milestone, toàn bộ quy tắc nghiệp vụ, yêu cầu phi chức năng và danh sách use case nằm ở các phụ lục cuối tệp. Bốn tệp báo cáo `Group2-Milestone{1..4}-OnlineExaminationSystem.docx` đặt trong `docs/` chỉ để tra chi tiết khi cần (luồng sự kiện của từng use case, thẻ CRC, sơ đồ). Nếu máy có `pandoc` thì đọc bằng `pandoc -t plain`; nếu không có, cứ làm theo phụ lục, không cần cài thêm.

Toàn bộ mã do Claude viết. Bốn thành viên không code, nhưng sẽ bị giảng viên hỏi vấn đáp về từng phần. Vì vậy mã phải **dễ đọc và giải thích được**: chú thích tiếng Việt ở những chỗ có quyết định thiết kế, tên lớp và phương thức rõ nghĩa, không dùng mẹo khó hiểu khi có cách viết thẳng.

### 0.1. Hệ thống làm gì

Ứng dụng desktop cài trên các máy trạm của phòng máy, cùng nối tới một MySQL trong mạng LAN.

- **Quản trị viên**: quản lý tài khoản, môn học, lớp học phần, phân công giảng viên.
- **Giảng viên**: soạn ngân hàng câu hỏi, tạo đề (thủ công hoặc theo ma trận), mở ca thi, xem kết quả và thống kê, xuất bảng điểm.
- **Thí sinh**: xem danh sách ca thi, vào thi, làm bài ở chế độ toàn màn hình, nộp bài, xem kết quả.

### 0.2. Các quyết định nghiệp vụ đã chốt – KHÔNG được đổi

1. Mỗi thí sinh thi **đúng một lần** trong một ca thi. Thoát ra rồi vào lại thì mở tiếp bài cũ, không tạo bài mới.
2. Ca thi có **giờ mở và giờ đóng chung** cho mọi thí sinh. Vào muộn thì còn ít thời gian hơn. Không có thời lượng riêng cho từng người.
3. Chỉ có **4 loại câu hỏi khách quan**: một đáp án, nhiều đáp án, đúng/sai, điền khuyết. Không có tự luận. Mọi bài đều chấm tự động.
4. Câu nhiều đáp án chấm theo cấu hình của đề: `ALL_OR_NOTHING` hoặc `PARTIAL` (tỷ lệ đúng trừ phần chọn sai, chặn dưới 0).
5. Chống gian lận: cửa sổ làm bài ở chế độ toàn màn hình. Mỗi lần rời khỏi cửa sổ là một lần vi phạm. **Đủ 5 lần** thì lập tức kết thúc bài, khóa bài và chấm phần đã làm. Các sự kiện cách nhau dưới 3 giây được gộp làm một.
6. Giờ chuẩn lấy từ máy chủ MySQL (`SELECT NOW()`), không bao giờ dùng đồng hồ máy trạm để tính giờ thi.
7. Không có khiếu nại, không gửi email, không giám sát tiến độ từng thí sinh theo thời gian thực.

### 0.3. Công nghệ – ràng buộc của giảng viên

Giảng viên chỉ cho phép **Java và JavaFX**.

- Java 17 (cấp ngôn ngữ, `maven.compiler.release=17`). Được dùng record, switch biểu thức, pattern matching `instanceof`. Không dùng tính năng của Java 21 trở lên.
- JavaFX 21 (`javafx-controls`, `javafx-fxml`). Giao diện viết bằng FXML + CSS.
- JDBC thuần với driver `mysql-connector-j`. **Không** dùng Hibernate/JPA, Spring, hay bất kỳ framework nào.
- Maven để build. JUnit 5 cho kiểm thử.
- Ngoài danh sách trên, **không thêm thư viện nào**. Nếu thấy cần, dừng lại và ghi vào mục "Câu hỏi mở" ở cuối `docs/PROGRESS.md` thay vì tự thêm.
- MySQL 8 khi chạy thật. Môi trường dev có thể dùng MariaDB 10.6+; `schema.sql` phải chạy được trên cả hai.

---

## 1. Hiện trạng mã nguồn (cuối M4)

```
com.group2.oes
├── model            User/Administrator/Instructor/Student, Subject, CourseClass, Exam, ExamItem,
│                    ExamMatrix, ExamSession, Submission, Answer, QuestionBank, ResultStatistics,
│                    Criteria + 5 enum (Role, Difficulty, SubmissionStatus, EndReason, MultiChoicePolicy)
│   └── question     Question (abstract), Single/Multi/TrueFalse/FillInBlank, AnswerOption, QuestionType
├── grading          GradingStrategy + ExactMatch/PartialMatch/TextMatch
├── factory          QuestionFactory, QuestionData (record)
├── exception        ExamAppException (abstract, unchecked) + Validation/AccessDenied/SessionRule/
│                    DataAccess/DuplicateKey
├── dao              UserDao, SubjectDao, QuestionDao, ExamDao, ExamSessionDao, SubmissionDao
│   ├── jdbc         cài đặt JDBC, ConnectionManager (execute / inTransaction), JdbcErrors
│   └── memory       InMemoryDatabase (cài đặt bằng Map), ManualClock
├── service          Auth, QuestionBank, Exam, ExamSession, Submission, Proctoring, Statistics, Permissions
├── util             PasswordHasher (SHA-256 + salt), AuditLog (java.util.logging)
└── app              AppContext (lắp ráp đối tượng), ConsoleDemo
```

Tài nguyên: `schema.sql` (12 bảng + khung nhìn `v_exam_item_for_student`), `sample-questions.txt`, `logging.properties`, `db.properties.example`.

Kiểm thử: 39 ca, `JdbcIntegrationTest` chỉ chạy khi có biến môi trường `OES_TEST_DB` trỏ tới tệp properties của CSDL thử.

### 1.1. Những gì đã có và đúng – giữ nguyên hành vi

- Cây kế thừa User và Question, `canManage` đa hình, `QuestionFactory` dùng switch trên enum.
- `Exam` chọn chiến lược chấm bằng `EnumMap<QuestionType, GradingStrategy>`.
- `QuestionBank.pick(ExamMatrix, Random)` sinh đề, báo rõ thiếu câu ở đâu.
- `ExamSession.buildPaperFor(Student)` trộn câu với seed = hash(sessionId, studentId).
- Ràng buộc `UNIQUE(session_id, student_id)` + `DuplicateKeyException` → `SubmissionService.start` mở lại bài khi hai máy cùng bấm vào thi.
- `increaseViolation` tăng nguyên tử bằng `LAST_INSERT_ID(violation_count + 1)`.
- `saveResult` có điều kiện trạng thái trong câu UPDATE để không ghi đè bài đã nộp.
- `ConnectionManager.inTransaction` rollback cả khi gặp RuntimeException, dùng `addSuppressed`.
- Định dạng câu trả lời lưu trong CSDL (giữ nguyên ở mọi nơi):
  - một đáp án: mã phương án, `"12"`
  - nhiều đáp án: các mã cách nhau dấu phẩy, `"12,14"`
  - đúng/sai: `"true"` / `"false"`
  - điền khuyết: văn bản thí sinh gõ (so khớp sau chuẩn hóa NFC, gộp khoảng trắng, không phân biệt hoa thường)

### 1.2. Những gì còn thiếu hoặc sai so với yêu cầu

| Vấn đề | Chi tiết | Xử lý ở |
|---|---|---|
| NFR-04 chưa đạt | Bài được chấm trên máy thí sinh nên máy thí sinh đọc được đáp án | Phase 1 |
| `Submission` quá nhiều trách nhiệm | Giữ dữ liệu, kiểm tra ghi, đếm vi phạm, chấm điểm | Phase 1 |
| `ExamSession` vừa giữ cấu hình vừa trộn đề | Báo cáo M3 đã hứa tách | Phase 1 |
| Thiếu CRUD | Sửa câu hỏi (FR-08), sửa/khóa/tìm người dùng (FR-01), sửa hồ sơ (FR-04), CRUD môn & lớp (FR-07), ghi danh theo lớp (FR-19), nhập người dùng CSV (FR-06) | Phase 2 |
| Không có gì gọi định kỳ | `closeExpiredSessions` chưa có ai gọi | Phase 2 |
| Chưa có giao diện | Toàn bộ FR liên quan màn hình | Phase 3–6 |
| Đánh dấu câu xem lại (FR-22) | Chưa lưu | Phase 1 (cột `flagged`) |

---

## 2. Quy ước bắt buộc

Phase 0 sẽ chép mục này vào `CLAUDE.md` ở gốc repo.

### 2.1. Kiến trúc

- Ba tầng: **view (JavaFX) → service → DAO**. Lớp model dùng chung.
- **Không có logic nghiệp vụ trong controller JavaFX.** Controller chỉ: đọc ô nhập, gọi service, cập nhật giao diện, hiện lỗi. Kiểm tra điều kiện, tính điểm, kiểm tra quyền đều nằm ở service/model.
- Lớp model **không** import `javafx.*` và **không** chứa SQL.
- Service chỉ biết interface DAO. Mọi việc tạo đối tượng nằm trong `AppContext` (hoặc các lớp context tương ứng sau khi tách client/staff).
- Mọi kiểm tra quyền làm ở service qua `Permissions`, kể cả khi giao diện đã ẩn nút.

### 2.2. JavaFX

- **Không bao giờ gọi DAO/service trên luồng JavaFX Application Thread.** Dùng tiện ích `BackgroundTasks.run(supplier, onSuccess, onError)` (viết ở Phase 3), bên trong dùng `javafx.concurrent.Task` và một `ExecutorService` dùng chung. Cập nhật giao diện qua `Platform.runLater` hoặc callback của Task.
- Mỗi màn hình = một tệp FXML trong `src/main/resources/fxml/` + một controller trong `ui.controller`. Kiểu dáng đặt trong `src/main/resources/css/app.css`, không đặt style inline trong FXML trừ trường hợp đặc biệt.
- Toàn bộ chữ trên giao diện bằng tiếng Việt có dấu.
- Bắt lỗi thống nhất: `ExamAppException` → hộp thoại hiện `getUserMessage()`. Lỗi không lường trước (RuntimeException khác) → hộp thoại chung "Đã có lỗi không mong muốn" + ghi log kèm stack trace.

### 2.3. Mã nguồn

- Package gốc `com.group2.oes`. Tên lớp, phương thức, biến bằng tiếng Anh. Chú thích và thông điệp lỗi bằng tiếng Việt.
- Giữ phong cách hiện có: kiểm tra dữ liệu trong constructor, trả về bản chỉ đọc của collection, `assignId` chỉ gán một lần, ngoại lệ có mã lỗi ổn định.
- Chú thích giải thích **vì sao**, không lặp lại mã đang làm gì.
- JDBC: luôn dùng `PreparedStatement`, luôn try-with-resources, không bao giờ nối chuỗi giá trị người dùng nhập vào SQL.
- Không để cảnh báo biên dịch (`-Xlint:all` sạch).

### 2.4. Quy trình làm việc

- Làm lần lượt theo phase. Mỗi phase kết thúc khi đạt mọi tiêu chí "Xong khi" và `mvn test` xanh.
- Sau mỗi phase: commit với thông điệp `phase N: <tóm tắt>` và cập nhật `docs/PROGRESS.md` (phase đã xong, quyết định đã đưa ra, điều còn dang dở).
- Mọi thay đổi so với thiết kế M3 ghi vào `docs/DESIGN-CHANGES.md` theo mẫu: *thay đổi – lý do – ảnh hưởng*. Nhóm cần tệp này để trả lời vấn đáp.
- Thêm tính năng mới thì viết kiểm thử cho nó trong cùng phase.

---

## 3. Các phase

### Phase 0 – Dựng repo

1. Giải nén mã M4 vào repo, `git init`, commit `phase 0: mã nguồn milestone 4`.
2. Tạo `CLAUDE.md` từ mục 0.2, 0.3 và 2 của tài liệu này.
3. Tạo `docs/PROGRESS.md`, `docs/DESIGN-CHANGES.md` (rỗng, có tiêu đề), chép 4 tệp báo cáo vào `docs/`.
4. Cập nhật `pom.xml`:
   - thêm `org.openjfx:javafx-controls` và `javafx-fxml` bản 21.x,
   - thêm `org.openjfx:javafx-maven-plugin`, cấu hình hai execution: `staff` (main `com.group2.oes.ui.StaffApp`) và `client` (main `com.group2.oes.ui.ExamClientApp`),
   - giữ `exec-maven-plugin` cho `ConsoleDemo`.
5. Tạo `db/` ở gốc repo, chuyển `schema.sql` vào đó (giữ bản trong resources nếu kiểm thử đang dùng, hoặc sửa kiểm thử đọc từ `db/`).

**Xong khi:** `mvn test` xanh như cuối M4; `mvn compile exec:java` chạy `ConsoleDemo` ra kết quả như M4.

---

### Phase 1 – Refactor theo SOLID và đạt NFR-04 (nội dung Milestone 5)

Đây là phase quan trọng nhất về mặt điểm OOP. Mỗi thay đổi phải gọi tên được nguyên lý nó phục vụ, và ghi vào `DESIGN-CHANGES.md`.

#### 1.1. Mô hình chấm bài mới

Hiện tại máy thí sinh tự chấm nên phải đọc được đáp án. Chuyển sang mô hình:

```
Máy thí sinh (ExamClientApp, tài khoản MySQL oes_client)
   chỉ đọc đề không có đáp án, ghi câu trả lời, đánh dấu bài SUBMITTED
                     │
                     ▼  (CSDL)
Máy giảng viên / máy chủ (StaffApp hoặc GradingDaemon, tài khoản oes_staff)
   GradingWorker chạy mỗi 3 giây:
     1. ca nào quá giờ đóng: chuyển bài IN_PROGRESS -> SUBMITTED, end_reason = TIME_OUT
     2. bài nào SUBMITTED: chấm, ghi điểm, chuyển GRADED
```

Hệ quả: sau khi nộp, máy thí sinh hiện "Đang chấm bài…" và hỏi lại trạng thái 2 giây một lần cho tới khi thấy GRADED (tối đa 60 giây, quá thì báo "Kết quả sẽ có khi giảng viên mở máy chấm").

#### 1.2. Tách vai trò của `Submission` (SRP)

| Lớp mới / sửa | Trách nhiệm |
|---|---|
| `Submission` (giữ) | Dữ liệu bài làm và chuyển trạng thái hợp lệ: `markSubmitted(reason, now)` trả boolean, `markGraded(total)` |
| `SubmissionGrader` (mới, `grading`) | Nhận `Submission` + `Exam`, chấm từng câu bằng chiến lược của đề, cô lập lỗi từng câu (giữ logic `gradeAll` hiện tại) |
| `ViolationPolicy` (mới, `model`) | Hằng `MAX_VIOLATIONS = 5`, `MERGE_WINDOW = 3s`, quyết định gộp sự kiện và quyết định kết thúc bài. `ProctoringService` dùng lớp này thay vì tự giữ hằng |

`Submission.submit` hiện gọi `gradeAll` ngay; bỏ việc đó. Chỉ `GradingWorker` mới chấm.

#### 1.3. Tách vai trò của `ExamSession` (SRP)

| Lớp | Trách nhiệm |
|---|---|
| `SessionWindow` (record) | `openTime`, `closeTime`, `isOpen(now)`, `isClosed(now)`, `remainingSeconds(now)` |
| `SessionPolicy` (record) | `shuffleQuestions`, `shuffleOptions`, `showAnswer` |
| `PaperShuffler` | Trộn danh sách mục đề và phương án theo seed (sessionId, studentId). Dùng được cho cả `ExamItem` (phía giảng viên) và `PaperItem` (phía thí sinh) – viết generic `List<T> shuffle(List<T>, long seed)` |
| `ExamSession` | Ghép các phần trên + đề + danh sách ghi danh, giữ `ensureCanEnter` |

#### 1.4. Phía thí sinh: mô hình không chứa đáp án

Tạo package `com.group2.oes.client` (dùng chung model enum, exception):

| Lớp | Nội dung |
|---|---|
| `SessionInfo` (record) | sessionId, examTitle, subjectCode, `SessionWindow`, `SessionPolicy`, totalPoint, questionCount |
| `PaperItem` (record) | order, point, questionId, `QuestionType`, content, `List<PaperOption>` |
| `PaperOption` (record) | optionId, content – **không có cờ đúng/sai** |
| `ExamPaper` | danh sách `PaperItem` đã trộn cho thí sinh, tra theo order |
| `AttemptState` | submissionId, status, violationCount, endReason, totalScore (null khi chưa chấm), `Map<Integer, String>` câu trả lời, `Set<Integer>` câu đánh dấu |
| `ReviewItem` (record) | dùng cho màn hình xem lại sau khi thi: câu trả lời của thí sinh, điểm câu, phương án đúng (chỉ có khi ca đã đóng và `showAnswer`) |

#### 1.5. Tách interface DAO (ISP)

Thay vì một `SubmissionDao` to cho mọi bên, tách theo ai dùng:

- `StudentExamDao` (phía thí sinh, cài đặt JDBC đọc qua khung nhìn):
  - `Optional<SessionInfo> findSession(int sessionId)`
  - `List<SessionInfo> findSessionsOf(int studentId)`
  - `boolean isEnrolled(int sessionId, int studentId)`
  - `ExamPaper loadPaper(int examId)` (chưa trộn)
  - `Optional<AttemptState> findAttempt(int sessionId, int studentId)`
  - `AttemptState createAttempt(int sessionId, int studentId, LocalDateTime now)` (ném `DuplicateKeyException`)
  - `void saveAnswer(int submissionId, int order, String response)`
  - `void setFlag(int submissionId, int order, boolean flagged)`
  - `int increaseViolation(int submissionId)`
  - `boolean markSubmitted(int submissionId, EndReason reason, LocalDateTime now)`
  - `List<ReviewItem> loadReview(int submissionId)`
  - `LocalDateTime serverTime()`
- `GradingDao` (phía giảng viên): `List<Integer> findSubmittedIds()`, `int closeExpired(LocalDateTime now)` (một câu UPDATE), `Submission load(int submissionId)`, `boolean saveGrades(Submission)` (điều kiện `status = 'SUBMITTED'`).
- `SubmissionDao` hiện có giữ cho thống kê và xem kết quả phía giảng viên.

`UserDao.findByUsername` phía thí sinh đọc từ khung nhìn `v_login`. Viết `JdbcStudentLoginDao` riêng hoặc cho `JdbcUserDao` nhận tên bảng/khung nhìn qua constructor – chọn cách nào thì ghi lý do.

#### 1.6. CSDL: hai tài khoản, khung nhìn, trigger

Tạo `db/schema.sql` (bảng) và `db/security.sql` (người dùng, khung nhìn, quyền, trigger):

- Bảng `answer` thêm cột `flagged BOOLEAN NOT NULL DEFAULT FALSE`.
- Khung nhìn:
  - `v_login`: id, username, password_hash, full_name, role, active, student_code
  - `v_session_for_student`: thông tin ca + tên đề + mã môn + tổng điểm + số câu
  - `v_exam_paper`: exam_item + question + answer_option, **không có** `is_correct`, `correct_value`, `case_sensitive`
  - `v_review`: phương án đúng và đáp án chấp nhận, **chỉ** cho các ca có `close_time <= NOW()` và `show_answer = TRUE`
- Tài khoản:
  - `oes_staff`: toàn quyền trên CSDL `oes`
  - `oes_client`: SELECT trên các khung nhìn trên + `exam_session`, `session_student`; SELECT, INSERT(session_id, student_id, started_at, status), UPDATE(status, submitted_at, end_reason, violation_count) trên `submission`; SELECT, INSERT(submission_id, item_order, response, flagged), UPDATE(response, flagged) trên `answer`. **Không** có quyền đọc `question`, `answer_option`, không có quyền ghi `total_score`, `earned_point`.
- Trigger (lưới an toàn cuối cùng, đề phòng ứng dụng thí sinh bị sửa):
  - `BEFORE UPDATE ON submission`: nếu `CURRENT_USER()` là `oes_client` thì cấm đặt `status = 'GRADED'` và cấm sửa bài có `OLD.status <> 'IN_PROGRESS'`.
  - `BEFORE INSERT/UPDATE ON answer`: nếu người dùng là `oes_client` thì cấm ghi khi bài không còn `IN_PROGRESS` hoặc ca đã quá `close_time` (BR-04 ở mức CSDL).
  - Dùng `SIGNAL SQLSTATE '45000'`; `JdbcErrors.translate` đổi lỗi này thành `SessionRuleException(SUBMISSION_LOCKED)`.
- Tệp cấu hình tách đôi: `db-staff.properties.example`, `db-client.properties.example`.

#### 1.7. Kiểm thử phase 1

- Sửa các kiểm thử cũ theo mô hình mới (nộp xong là SUBMITTED, chạy `GradingWorker.runOnce()` rồi mới GRADED).
- Kiểm thử mới:
  - `SubmissionGraderTest`: một câu ném lỗi thì các câu còn lại vẫn được chấm.
  - `ViolationPolicyTest`: gộp sự kiện trong 3 giây, đủ 5 lần thì kết thúc.
  - `PaperShufflerTest`: cùng seed cùng thứ tự, khác thí sinh khác thứ tự.
  - `JdbcSecurityTest` (chạy khi có `OES_TEST_DB_CLIENT`): đăng nhập bằng `oes_client` thì `SELECT * FROM answer_option` bị từ chối; `UPDATE submission SET total_score = 10` bị từ chối; ghi câu trả lời sau giờ đóng bị trigger chặn.
  - Bản `memory` của các DAO mới để kiểm thử nghiệp vụ không cần MySQL.

**Xong khi:** mọi kiểm thử xanh; `DESIGN-CHANGES.md` có đủ các mục trên kèm tên nguyên lý SOLID; `ConsoleDemo` vẫn chạy được (sửa để gọi `GradingWorker.runOnce()` sau khi nộp).

---

### Phase 2 – Hoàn thiện tầng service phía giảng viên và quản trị

| FR | Việc cần làm |
|---|---|
| FR-01 | `UserDao.search(keyword, role)`, `update`; `AuthService.updateUser`, `setActive` (đã có), không tự khóa mình |
| FR-04 | `AuthService.changePassword(actor, oldPw, newPw)` kiểm tra mật khẩu cũ, mật khẩu mới ≥ 6 ký tự; `updateProfile` (họ tên, email) |
| FR-06 | `UserImportService.importStudents(actor, Path csv)`: cột `ma_sv,ho_ten,email,ma_lop`; mật khẩu mặc định = mã sinh viên viết thường, bắt đổi ở lần đăng nhập đầu (thêm cột `must_change_password`); lỗi theo dòng như `importFile` câu hỏi |
| FR-07 | `SubjectService`: thêm/sửa/xóa môn (cấm xóa khi đã có câu hỏi), thêm/sửa lớp học phần, phân công/bỏ phân công giảng viên |
| FR-08 | `QuestionDao.update(Question)` trong giao dịch (xóa phương án cũ, chèn lại); cấm sửa câu đã nằm trong đề đã khóa (cùng điều kiện với xóa) |
| FR-11 | `QuestionBank.findBy(Criteria)` đã có; thêm phân trang nếu ngân hàng > 500 câu không cần thiết, bỏ qua |
| FR-19 | `ExamSessionService.createSession` nhận thêm lựa chọn ghi danh theo lớp học phần (`UserDao.findStudentsOfClass`) |
| FR-26 | `GradingWorker` chạy bằng `ScheduledExecutorService` 3 giây/lần trong `StaffApp`; thêm `GradingDaemon` (main không giao diện) để chạy trên máy chủ |
| FR-30, 34, 35 | Đã có trong `StatisticsService`; thêm `listResults(actor, session)` trả danh sách dòng cho bảng kết quả |

Thêm `ExamService.list(actor, subject)` và `ExamSessionService.listOfExam(actor, exam)` cho các màn hình danh sách.

**Xong khi:** mỗi dòng trong bảng trên có ít nhất một kiểm thử; `GradingDaemon` chạy được từ dòng lệnh và in ra số bài đã chấm mỗi lượt có bài.

---

### Phase 3 – Khung giao diện JavaFX

1. Hai lớp khởi động:
   - `ui.StaffApp` (quản trị viên, giảng viên) – đọc `db-staff.properties`, khởi động `GradingWorker`.
   - `ui.ExamClientApp` (thí sinh) – đọc `db-client.properties`.
   - Mỗi app có lớp `Launcher` với `main` thường gọi `Application.launch` (cần cho jar chạy độc lập).
   - Vị trí tệp cấu hình: tham số dòng lệnh `--config=...`, nếu không có thì tìm cạnh tệp jar, nếu không có nữa thì hiện màn hình nhập địa chỉ máy chủ và lưu lại.
2. `ui.Navigator`: đổi màn hình trong một `Stage`, truyền dữ liệu giữa màn hình qua tham số, không dùng biến static toàn cục ngoài `UserSession`.
3. `ui.UserSession`: người dùng đang đăng nhập; đăng xuất thì xóa.
4. `ui.BackgroundTasks`: chạy công việc nền, hiện con trỏ chờ / vô hiệu hóa nút trong lúc chạy, chuyển lỗi về `ErrorDialogs`.
5. `ui.ErrorDialogs`: một chỗ duy nhất hiển thị lỗi (mục 2.2).
6. `css/app.css`: một bộ màu trung tính, chữ ≥ 14px, nút chính màu xanh đậm. Giao diện gọn, không cầu kỳ.
7. Màn hình đăng nhập dùng chung (FXML một tệp, controller nhận vai trò được phép). StaffApp từ chối tài khoản thí sinh và ngược lại, báo rõ "Tài khoản thí sinh hãy dùng ứng dụng thi".
8. Đăng nhập lần đầu với `must_change_password` thì bắt đổi mật khẩu trước khi vào.

**Xong khi:** chạy `mvn javafx:run@staff` và `mvn javafx:run@client` đăng nhập được, đăng xuất được, mất kết nối CSDL thì hiện hộp thoại thông báo chứ không treo.

---

### Phase 4 – Màn hình quản trị viên (StaffApp)

| Màn hình | Nội dung | FR |
|---|---|---|
| Trang chủ quản trị | Menu bên trái: Người dùng, Môn học & lớp, Nhật ký | – |
| Người dùng | Bảng (TableView) có ô tìm kiếm, lọc theo vai trò; nút Thêm, Sửa, Khóa/Mở khóa, Đặt lại mật khẩu, Nhập từ CSV (hiện kết quả nhập + danh sách dòng lỗi) | FR-01, 02, 06 |
| Môn học & lớp | Hai bảng cạnh nhau; phân công giảng viên cho môn | FR-07 |
| Nhật ký | Đọc tệp `oes-audit.log`, hiện 500 dòng mới nhất, lọc theo từ khóa | NFR-05 |

---

### Phase 5 – Màn hình giảng viên (StaffApp)

| Màn hình | Nội dung | FR |
|---|---|---|
| Ngân hàng câu hỏi | Chọn môn; bảng câu hỏi với bộ lọc chương / mức độ / loại / từ khóa; Thêm, Sửa, Xóa, Nhập từ tệp | FR-08, 11, 12, 13 |
| Biểu mẫu câu hỏi | Chọn loại → phần nhập thay đổi theo loại: danh sách phương án có ô đánh dấu đúng (thêm/bớt dòng), hai nút Đúng/Sai, danh sách đáp án chấp nhận + ô phân biệt hoa thường. Lỗi hiện ngay cạnh ô nhập | FR-09, 10 |
| Tạo đề | Hai thẻ: "Chọn tay" (bảng ngân hàng + danh sách đã chọn, nhập điểm từng câu, tổng điểm cập nhật liền) và "Theo ma trận" (bảng chương × mức độ, nút "Kiểm tra" báo thiếu câu trước khi sinh). Chọn cách chấm câu nhiều đáp án | FR-14, 15, 16 |
| Danh sách đề | Đề theo môn, cột đã khóa / phiên bản; nút "Tạo phiên bản mới" cho đề đã khóa | BR-09 |
| Mở ca thi | Chọn đề, giờ mở/đóng (DatePicker + ô giờ), ba ô tùy chọn trộn câu / trộn phương án / cho xem đáp án; ghi danh theo lớp hoặc chọn từng người (có "Chọn tất cả") | FR-17, 18, 19 |
| Danh sách ca thi | Trạng thái sắp diễn ra / đang mở / đã đóng, số thí sinh, số bài đã nộp (làm mới khi bấm nút, không cần thời gian thực) | – |
| Kết quả ca thi | Bảng: mã SV, họ tên, điểm, điểm hệ 10, số câu trả lời, số lần vi phạm, cột "Kết thúc do vi phạm" tô màu; thống kê cao/thấp/trung bình; biểu đồ phổ điểm (`BarChart` của JavaFX); bảng tỷ lệ đúng từng câu, câu khó nhất lên đầu; nút Xuất CSV (FileChooser) | FR-30, 34, 35 |
| Hồ sơ | Đổi họ tên, email, mật khẩu | FR-04 |

---

### Phase 6 – Ứng dụng thi (ExamClientApp)

Đây là phần sẽ được demo nhiều nhất. Làm kỹ.

#### 6.1. Danh sách ca thi (FR-20)

Ba nhóm: sắp diễn ra (hiện giờ mở), đang mở (nút "Vào thi"), đã kết thúc (nút "Xem kết quả" nếu đã có bài). Nút "Làm mới".

#### 6.2. Xác nhận quy chế (FR-27)

Hiện tên đề, số câu, tổng điểm, giờ đóng, thời gian còn lại, và quy chế: "Rời khỏi cửa sổ làm bài 5 lần sẽ bị kết thúc bài thi ngay lập tức. Các lần rời cửa sổ sát nhau trong 3 giây được tính là một lần." Ô "Tôi đã đọc và đồng ý" + nút "Bắt đầu làm bài". Nếu đã có bài đang dở thì hiện "Tiếp tục bài đang làm (đã vi phạm N lần)".

#### 6.3. Màn hình làm bài (FR-21 → 29)

Bố cục: thanh trên (tên đề, đồng hồ đếm ngược, số lần vi phạm), cột trái (lưới số câu: đã trả lời / chưa / đánh dấu xem lại, bấm để nhảy câu), vùng giữa (nội dung câu và phần trả lời), thanh dưới (Câu trước, Đánh dấu xem lại, Câu sau, Nộp bài).

Phần trả lời theo loại câu: RadioButton (một đáp án, đúng/sai), CheckBox (nhiều đáp án), TextField (điền khuyết).

**Toàn màn hình và vi phạm:**

```java
stage.setFullScreen(true);
stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // Esc không thoát được
stage.setFullScreenExitHint("");
stage.setOnCloseRequest(Event::consume);                         // chặn Alt+F4 trong lúc thi
```

Lắng nghe ba sự kiện, mỗi sự kiện gọi cùng một hàm `onLeave(Instant.now())`:
- `stage.fullScreenProperty()` chuyển sang `false`
- `stage.focusedProperty()` chuyển sang `false`
- `stage.iconifiedProperty()` chuyển sang `true`

`onLeave` gọi `ProctoringService.report` ở luồng nền. Kết quả:
- gộp → không làm gì
- vi phạm mới → hiện hộp thoại modal "Bạn đã rời cửa sổ làm bài (lần N/5)". Khi thí sinh bấm "Quay lại làm bài" thì gọi lại `setFullScreen(true)`
- kết thúc → đóng màn hình làm bài, chuyển sang màn hình kết quả với dòng "Bài thi bị kết thúc do vi phạm quy chế"

Chú ý: hộp thoại cảnh báo tự nó làm cửa sổ chính mất tiêu điểm. Phải tắt việc lắng nghe trong lúc hộp thoại đang mở, nếu không sẽ tự sinh vi phạm.

**Đồng hồ:** lấy giờ máy chủ một lần khi vào thi, tính độ lệch so với `System.nanoTime()`, dùng `Timeline` 1 giây/lần để hiện thời gian còn lại; cứ 60 giây đồng bộ lại với máy chủ. Còn 5 phút: đồng hồ đổi màu đỏ và hiện thông báo một lần. Về 0: tự nộp với `TIME_OUT`.

**Lưu bài (FR-24, NFR-06):**
- Mỗi lần đổi câu trả lời: chờ 500 ms (`PauseTransition`) rồi lưu, để gõ chữ ở câu điền khuyết không sinh hàng chục lượt ghi.
- Giữ một hàng đợi các câu chưa lưu được. Mất kết nối thì hiện dải cảnh báo vàng "Mất kết nối, câu trả lời sẽ được lưu khi kết nối lại", thử lại sau 3 giây, không chặn thí sinh làm tiếp.
- Khi bấm Nộp: đẩy hết hàng đợi trước, rồi mới `markSubmitted`.

**Nộp bài (FR-25):** hộp thoại xác nhận nêu số câu chưa trả lời và số câu đang đánh dấu xem lại.

#### 6.4. Kết quả (FR-33)

Sau khi nộp: "Đang chấm bài…" với vòng xoay, hỏi lại 2 giây một lần. Có điểm thì hiện điểm, số câu đúng, lý do kết thúc. Nếu ca đã đóng và cho xem đáp án: danh sách từng câu, câu trả lời của thí sinh, đáp án đúng, điểm câu. Ca chưa đóng thì ghi "Đáp án sẽ hiện sau khi ca thi kết thúc".

**Xong khi:** chạy thử trên hai máy (hoặc hai tiến trình) cùng một ca: cả hai làm bài, một bên Alt+Tab 5 lần bị kết thúc, một bên để hết giờ tự nộp, StaffApp thấy đúng hai bài với đúng lý do kết thúc.

---

### Phase 7 – Kiểm thử và gia cố (Milestone 6)

1. Kiểm thử đơn vị và tích hợp: giữ mọi kiểm thử xanh; mục tiêu mỗi service có kiểm thử cho luồng chính và mỗi mã lỗi.
2. `tools/LoadSimulator` (main riêng, nằm trong `src/test/java` hoặc module `tools`): giả lập 60 thí sinh, mỗi người vào thi, ghi một câu trả lời mỗi 2 giây trong 3 phút, rồi nộp. In ra p50 / p95 / max thời gian của `saveAnswer`. Đây là cách kiểm chứng NFR-01 đã viết trong báo cáo M1 (< 2 giây). Lưu kết quả vào `docs/load-test.md`.
3. `docs/manual-test-checklist.md`: danh sách kiểm thử tay theo từng use case UC-01 → UC-18, mỗi dòng có bước làm, kết quả mong đợi, ô đánh dấu. Bốn thành viên dùng tệp này để tự chạy thử trước buổi bảo vệ.
4. Tình huống hỏng hóc phải xử lý được:
   - rút dây mạng giữa giờ thi rồi cắm lại
   - tắt hẳn ứng dụng thi rồi mở lại, đăng nhập lại
   - hai máy cùng đăng nhập một tài khoản thí sinh
   - StaffApp không chạy khi thí sinh nộp bài (bài ở trạng thái SUBMITTED, chấm sau khi StaffApp mở)
   - đồng hồ máy thí sinh bị chỉnh sai
5. Rà toàn bộ thông điệp lỗi người dùng nhìn thấy: tiếng Việt, nói rõ phải làm gì.

---

### Phase 8 – Đóng gói và chuẩn bị demo

1. `mvn package` tạo jar chạy được cho từng app (dùng `maven-shade-plugin`, lớp `Launcher` làm main). Ghi rõ trong README: jar build trên hệ điều hành nào thì chạy trên hệ điều hành đó (JavaFX có phần native).
2. Script `run-staff.bat/.sh`, `run-client.bat/.sh`.
3. `tools/SeedTool` (main): tạo dữ liệu demo – 1 quản trị viên, 2 giảng viên, 2 môn, 2 lớp học phần, 60 thí sinh mã `B24DCCE001`…`B24DCCE060` (thêm 4 tài khoản đúng tên và mã của 4 thành viên nhóm), 60 câu hỏi môn OOP mở rộng từ `sample-questions.txt` (đủ 4 loại, 3 chương, 3 mức độ), 1 đề sinh theo ma trận, 1 ca thi mở trong 2 giờ kể từ lúc chạy. In ra bảng tài khoản và mật khẩu.
4. `docs/RUNBOOK.md` – hướng dẫn dựng phòng máy:
   - cài MySQL trên một máy, mở cổng 3306 trong LAN, chạy `db/schema.sql` rồi `db/security.sql`, đặt mật khẩu hai tài khoản
   - kiểm tra các máy trạm nối được tới máy chủ (`telnet <ip> 3306`), lưu ý mạng trường có thể chặn kết nối giữa các máy; phương án dự phòng là phát Wi-Fi từ điện thoại
   - chạy `SeedTool`, mở StaffApp trên máy chủ, mở ExamClientApp trên các máy còn lại
5. `docs/DEMO-SCRIPT.md` – kịch bản demo 10 phút, chia lời cho 4 người (xem tệp phân công), mỗi bước ghi rõ bấm gì và nói gì.
6. README cập nhật đầy đủ.

---

## 4. Bảng truy vết FR → nơi cài đặt

Claude Code cập nhật cột "Trạng thái" trong `docs/PROGRESS.md` khi làm xong.

| FR | Nội dung ngắn | Service / lớp chính | Màn hình |
|---|---|---|---|
| FR-01 | Tạo, sửa, khóa, tìm tài khoản | AuthService, UserDao | Người dùng |
| FR-02 | Gán vai trò | User và 3 lớp con | Người dùng |
| FR-03 | Đăng nhập, đăng xuất | AuthService | Đăng nhập |
| FR-04 | Đổi mật khẩu, hồ sơ | AuthService | Hồ sơ |
| FR-05 | Chặn ngoài quyền | Permissions | – |
| FR-06 | Nhập người dùng CSV | UserImportService | Người dùng |
| FR-07 | Môn học, lớp học phần | SubjectService | Môn học & lớp |
| FR-08 | Thêm, sửa, xóa câu hỏi | QuestionBankService | Ngân hàng câu hỏi |
| FR-09 | 4 loại câu hỏi | Question + lớp con, QuestionFactory | Biểu mẫu câu hỏi |
| FR-10 | Thuộc tính câu hỏi | Question | Biểu mẫu câu hỏi |
| FR-11 | Lọc, tìm câu hỏi | QuestionBank, Criteria | Ngân hàng câu hỏi |
| FR-12 | Nhập câu hỏi từ tệp | QuestionBankService.importFile | Ngân hàng câu hỏi |
| FR-13 | Cấm xóa câu đã dùng | QuestionBankService | Ngân hàng câu hỏi |
| FR-14 | Tạo đề thủ công | ExamService.createManual | Tạo đề |
| FR-15 | Sinh đề theo ma trận | QuestionBank.pick, ExamService.generate | Tạo đề |
| FR-16 | Điểm từng câu, tổng điểm | Exam | Tạo đề |
| FR-17 | Ca thi giờ mở/đóng | ExamSession, SessionWindow | Mở ca thi |
| FR-18 | Cấu hình trộn, xem đáp án | SessionPolicy | Mở ca thi |
| FR-19 | Ghi danh | ExamSessionService | Mở ca thi |
| FR-20 | Danh sách ca thi của thí sinh | StudentExamService | Danh sách ca thi |
| FR-21 | Điều kiện vào thi | ExamSession.ensureCanEnter / StudentExamService | Xác nhận quy chế |
| FR-22 | Hiển thị đề, điều hướng, đánh dấu | PaperShuffler, ExamPaper | Làm bài |
| FR-23 | Đếm ngược, cảnh báo 5 phút | SessionWindow + Timeline | Làm bài |
| FR-24 | Lưu bài | StudentExamDao.saveAnswer + hàng đợi | Làm bài |
| FR-25 | Nộp bài có xác nhận | StudentExamService.submit | Làm bài |
| FR-26 | Tự nộp khi ca đóng | GradingWorker | – |
| FR-27 | Xác nhận quy chế, toàn màn hình | – | Xác nhận quy chế, Làm bài |
| FR-28 | Ghi nhận vi phạm | ProctoringService, ViolationPolicy | Làm bài |
| FR-29 | 5 lần thì kết thúc | ProctoringService, ViolationPolicy | Làm bài |
| FR-30 | Giảng viên xem vi phạm | StatisticsService | Kết quả ca thi |
| FR-31 | Chấm tự động | SubmissionGrader, GradingWorker | – |
| FR-32 | Nhiều chiến lược chấm | GradingStrategy + 3 lớp | Tạo đề (chọn cách chấm) |
| FR-33 | Thí sinh xem kết quả, đáp án | StudentExamDao.loadReview | Kết quả |
| FR-34 | Thống kê | ResultStatistics | Kết quả ca thi |
| FR-35 | Xuất CSV | StatisticsService.exportCsv | Kết quả ca thi |

---

## 5. Thứ tự ưu tiên khi thiếu thời gian

Nếu phải cắt, cắt theo thứ tự từ dưới lên:

1. (giữ bằng mọi giá) Phase 0, 1, 3, 6 – lõi OOP và luồng thi là thứ được chấm và được demo
2. Phase 5 phần ngân hàng câu hỏi, tạo đề, mở ca, kết quả
3. Phase 7 mục 1, 3, 4
4. Phase 8 mục 1–5
5. Phase 2 phần FR-06, FR-07 đầy đủ; Phase 4 (có thể thay bằng dữ liệu từ SeedTool)
6. Màn hình Nhật ký, biểu đồ phổ điểm, LoadSimulator

---

## 6. Câu hỏi mở

Claude Code ghi thêm vào đây (trong `docs/PROGRESS.md`) mọi chỗ phải đoán ý, để nhóm xác nhận:

- Giảng viên có cho phép dùng `maven-shade-plugin` để đóng gói không? Nếu không, thay bằng hướng dẫn chạy `mvn javafx:run`.
- Có cần màn hình giám sát danh sách thí sinh đang thi không? (Hiện đã loại khỏi phạm vi ở M1.)

---

## Phụ lục A – Tóm tắt từng milestone

**M1 – Phân tích bài toán.** Chốt phạm vi: ứng dụng desktop cho phòng máy, 3 vai trò, 35 FR chia 6 nhóm (người dùng, ngân hàng câu hỏi, đề và ca thi, làm bài, giám sát quy chế, chấm điểm và kết quả), 12 NFR, 11 BR, 18 use case. Ngoài phạm vi: camera giám sát, tự luận, thi lại, khiếu nại, email, chạy qua Internet, tích hợp hệ thống đào tạo (chỉ nhập/xuất CSV).

**M2 – Phân tích hướng đối tượng.** Rút 22 lớp từ phân tích danh từ. Bốn quyết định quan trọng: tách `ExamItem` khỏi `Question` (điểm và thứ tự thuộc về đề); tách `GradingStrategy` khỏi `Question` (cách chấm đổi theo đề); không tạo lớp `Role` (dùng kế thừa vì 3 vai trò cố định); không tạo lớp lưu từng lần vi phạm (chỉ cần đếm). Sơ đồ trạng thái bài làm: IN_PROGRESS → SUBMITTED → GRADED.

**M3 – Thiết kế UML.** Kiến trúc 4 gói (view, service, model + grading + factory, dao). Hai cây kế thừa User và Question. Factory cho câu hỏi, Strategy cho chấm điểm. 5 interface DAO, ánh xạ một bảng chung cho 4 loại câu hỏi (cột `type`). Cây ngoại lệ gốc `ExamAppException`. Hứa hẹn: dùng hai tài khoản MySQL để máy thí sinh không đọc được đáp án.

**M4 – Cài đặt lõi.** Viết toàn bộ model, DAO JDBC + DAO bộ nhớ, 7 service, `ConsoleDemo`, 39 kiểm thử. Khác với M3: thêm `SubjectDao`, `DuplicateKeyException`, 12 bảng thay vì 9, bảng người dùng tên `app_user`. Hạn chế tự nhận trong báo cáo (plan này phải xử lý): chưa đạt NFR-04 vì chấm trên máy thí sinh; `closeExpiredSessions` chưa được gọi định kỳ; mỗi thao tác mở một kết nối; `Submission` quá nhiều trách nhiệm.

**M5 – Cải tiến thiết kế** (giảng viên không giám sát, nhóm tự làm): SOLID, refactor, mẫu thiết kế. Tương ứng Phase 1 của plan này.

**M6 – Sản phẩm cuối:** kiểm thử, demo, thuyết trình. Tương ứng Phase 2 → 8. Slide thuyết trình và báo cáo cuối kỳ **không** thuộc phạm vi Claude Code; nhóm làm riêng, Claude Code chỉ cần để lại `docs/` đầy đủ để nhóm lấy nội dung.

## Phụ lục B – Quy tắc nghiệp vụ (BR)

| Mã | Quy tắc |
|---|---|
| BR-01 | Thí sinh chỉ được vào ca thi khi có tên trong danh sách dự thi và thời điểm hiện tại nằm trong khung giờ mở của ca thi. |
| BR-02 | Mỗi thí sinh có đúng một bài làm trong một ca thi; đã nộp thì không được vào lại ca đó. |
| BR-03 | Thời gian làm bài của mọi thí sinh kết thúc tại giờ đóng ca thi, không phụ thuộc thời điểm bắt đầu. |
| BR-04 | Khi ca thi đóng, bài làm bị khóa và tự động nộp; mọi câu trả lời gửi lên sau thời điểm đó bị từ chối. |
| BR-05 | Mỗi lần rời khỏi cửa sổ làm bài là một lần vi phạm. Đạt 5 lần thì bài bị kết thúc và khóa ngay, không mở lại được. |
| BR-06 | Bài bị kết thúc do vi phạm vẫn được chấm như bài nộp bình thường nhưng phải được đánh dấu riêng trong bảng điểm. |
| BR-07 | Câu một đáp án chỉ có điểm khi chọn trùng đáp án đúng; không có điểm âm. |
| BR-08 | Câu nhiều đáp án chấm theo cấu hình của đề: toàn phần hoặc theo tỷ lệ. |
| BR-09 | Đề đã dùng cho ca thi thì không sửa được; muốn đổi phải tạo phiên bản mới. (Cài đặt hiện tại khóa đề ngay khi mở ca, chặt hơn M1 – giữ nguyên.) |
| BR-10 | Chỉ giảng viên phụ trách môn hoặc quản trị viên mới xem được kết quả thí sinh của môn đó. |
| BR-11 | Tổng điểm đề bằng tổng điểm các câu thành phần và phải lớn hơn 0. |

## Phụ lục C – Yêu cầu phi chức năng (NFR)

| Mã | Yêu cầu | Ghi chú cho Claude Code |
|---|---|---|
| NFR-01 | 60 máy trạm cùng nối một CSDL; chuyển câu, lưu câu trả lời phản hồi < 2 giây trong LAN | Kiểm chứng bằng `LoadSimulator` (Phase 7) |
| NFR-02 | Chấm một bài 50 câu < 3 giây | Thêm một kiểm thử đo thời gian `SubmissionGrader` với đề 50 câu |
| NFR-03 | Mật khẩu băm SHA-256 + salt bằng thư viện JDK | Đã có `PasswordHasher` |
| NFR-04 | Máy thí sinh dùng tài khoản riêng, chỉ đọc khung nhìn không có đáp án | Phase 1. M1 ghi "đáp án đọc được sau khi nộp và ca cho phép"; plan chọn chặt hơn: chỉ sau khi **ca đã đóng**, để thí sinh nộp sớm không lộ đáp án cho người còn đang thi. Ghi vào `DESIGN-CHANGES.md` |
| NFR-05 | Ghi nhật ký đăng nhập, tạo đề, kết thúc bài do vi phạm | `AuditLog`; màn hình Nhật ký ở Phase 4 |
| NFR-06 | Tắt máy đột ngột không mất bài; đăng nhập máy khác làm tiếp với thời gian tính theo giờ đóng | Lưu theo từng thay đổi + hàng đợi khi mất mạng (Phase 6) |
| NFR-07 | Giao diện tiếng Việt; tối đa 3 thao tác để bắt đầu làm bài sau khi đăng nhập | Danh sách ca → Vào thi → Bắt đầu |
| NFR-08 | Thông báo lỗi rõ nguyên nhân và cách xử lý, không lộ ngoại lệ kỹ thuật | `ErrorDialogs` |
| NFR-09 | Ba tầng, controller không có logic | Mục 2.1 |
| NFR-10 | Thêm loại câu hỏi hoặc cách chấm không sửa lớp cũ | Giữ Factory + Strategy |
| NFR-11 | Java 17, JavaFX 21, MySQL 8; chạy trên Windows, macOS, Linux | Không dùng API riêng của một hệ điều hành |
| NFR-12 | Dữ liệu tập trung trên MySQL trong LAN, có script tạo bảng và dữ liệu mẫu | `db/*.sql`, `SeedTool` |

## Phụ lục D – Use case

| Mã | Tên | Tác nhân | FR |
|---|---|---|---|
| UC-01 | Quản lý tài khoản người dùng | Quản trị viên | FR-01, 06 |
| UC-02 | Phân quyền người dùng | Quản trị viên | FR-02, 05 |
| UC-03 | Đăng nhập / Đăng xuất | Mọi người dùng | FR-03 |
| UC-04 | Cập nhật hồ sơ, đổi mật khẩu | Mọi người dùng | FR-04 |
| UC-05 | Quản lý môn học và lớp học phần | Quản trị viên, Giảng viên | FR-07 |
| UC-06 | Quản lý ngân hàng câu hỏi | Giảng viên | FR-08–11, 13 |
| UC-07 | Nhập câu hỏi từ tệp | Giảng viên | FR-12 |
| UC-08 | Tạo đề thi | Giảng viên | FR-14, 16 |
| UC-09 | Sinh đề theo ma trận | Giảng viên | FR-15 |
| UC-10 | Lập lịch và cấu hình ca thi | Giảng viên | FR-17, 18 |
| UC-11 | Ghi danh thí sinh | Giảng viên | FR-19 |
| UC-12 | Xem danh sách ca thi | Thí sinh | FR-20 |
| UC-13 | Vào ca thi | Thí sinh | FR-21, 27 |
| UC-14 | Làm bài thi | Thí sinh | FR-22–24 |
| UC-15 | Ghi nhận vi phạm quy chế | Thí sinh | FR-28, 29 |
| UC-16 | Nộp bài và chấm tự động | Thí sinh, Đồng hồ hệ thống | FR-25, 26, 31, 32 |
| UC-17 | Xem kết quả và thống kê | Thí sinh, Giảng viên | FR-30, 33, 34 |
| UC-18 | Xuất bảng điểm CSV | Giảng viên | FR-35 |

Năm use case có đặc tả chi tiết (luồng chính, luồng thay thế, ngoại lệ) trong báo cáo M1: UC-06, UC-08, UC-13, UC-15, UC-16. Những luồng ngoại lệ quan trọng đã được đưa vào plan: UC-13 vào lại bài đang dở; UC-15 gộp sự kiện sát nhau; UC-16 nộp hai lần giữ kết quả lần đầu, một câu chấm lỗi không làm hỏng cả bài.
