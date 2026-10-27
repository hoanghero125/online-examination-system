# Thay đổi so với thiết kế Milestone 3

Mỗi mục ghi theo mẫu *thay đổi – lý do – ảnh hưởng*, kèm nguyên lý thiết kế liên quan.
Nhóm dùng tệp này để trả lời vấn đáp: "vì sao mã khác sơ đồ lớp trong báo cáo M3?".

Những khác biệt đã có từ Milestone 4 (thêm `SubjectDao`, `DuplicateKeyException`, 12 bảng, bảng `app_user`)
được trình bày trong báo cáo M4, không nhắc lại ở đây.

## Phase 0 – Dựng repo

### `schema.sql` chuyển từ `src/main/resources/` sang `db/`
- **Lý do:** tệp SQL do quản trị CSDL chạy bằng tay, không phải tài nguyên chương trình đọc lúc chạy. Để ở `db/`
  cùng `security.sql` cho người dựng phòng máy dễ tìm.
- **Ảnh hưởng:** không có mã nào đọc tệp này; README và RUNBOOK trỏ tới đường dẫn mới.

## Phase 1 – Milestone 5: SOLID, refactor, NFR-04

Tóm tắt một câu: **máy thí sinh không còn chấm bài**. Ở M4, bài được chấm ngay trên máy thí sinh nên máy thí sinh
phải đọc được đáp án – trái với NFR-04 mà báo cáo M1 đã hứa. Từ M5, máy thí sinh chỉ ghi câu trả lời và đánh dấu
bài đã nộp; máy giảng viên chạy `GradingWorker` để chấm. Các thay đổi dưới đây đều xuất phát từ quyết định đó.

```
Máy thí sinh (ExamClientApp, tài khoản MySQL oes_client)
   chỉ đọc đề không có đáp án, ghi câu trả lời, đánh dấu bài SUBMITTED
                     │  (CSDL)
                     ▼
Máy giảng viên / máy chủ (StaffApp hoặc GradingDaemon, tài khoản oes_staff)
   GradingWorker mỗi 3 giây: (1) ca quá giờ đóng -> bài IN_PROGRESS thành SUBMITTED/TIME_OUT
                             (2) bài SUBMITTED -> chấm, ghi điểm, GRADED
```

### 1. `Submission` chỉ còn giữ dữ liệu và chuyển trạng thái – **SRP**
- **Thay đổi:** M4 `Submission` làm bốn việc: giữ dữ liệu, kiểm tra quyền ghi, đếm vi phạm, chấm điểm (`gradeAll`).
  Nay chỉ còn giữ dữ liệu và hai phép chuyển trạng thái hợp lệ: `markSubmitted(reason, now)` (trả `false` nếu đã nộp)
  và `markGraded(total)` (chỉ khi đang `SUBMITTED`). Bỏ `saveAnswer`, `addViolation`, `reachedViolationLimit`,
  `submit`, `gradeAll`, hằng `MAX_VIOLATIONS`.
  - Chấm điểm → lớp mới `grading.SubmissionGrader` (giữ nguyên logic `gradeAll`: chấm mọi câu kể cả câu bỏ trống,
    một câu lỗi thì đánh dấu và chấm tiếp, làm tròn hai chữ số).
  - Quy chế vi phạm → lớp mới `model.ViolationPolicy` (5 lần, gộp trong 3 giây; quyết định gộp và quyết định kết thúc).
  - Ghi câu trả lời, đếm vi phạm → phía thí sinh (`StudentExamService`, `ProctoringService`).
- **Lý do:** mỗi lớp chỉ có một lý do để thay đổi. Đổi quy chế (ví dụ 3 lần thay vì 5) chỉ sửa `ViolationPolicy`;
  đổi cách chấm chỉ sửa `SubmissionGrader`; hai việc này cũng phải chạy trên hai máy khác nhau.
- **Ảnh hưởng:** `Answer.markGradingError` thành `public` (lớp chấm nằm ở gói `grading`); `Answer.response` thành
  `final` vì không còn ai sửa câu trả lời sau khi dựng. Kiểm thử mới `SubmissionGraderTest`, `ViolationPolicyTest`.

### 2. `ExamSession` tách thành `SessionWindow`, `SessionPolicy`, `PaperShuffler` – **SRP**
- **Thay đổi:** báo cáo M3 đã hứa tách. `ExamSession` nay chỉ ghép đề + khung giờ + tùy chọn + danh sách ghi danh,
  giữ `ensureCanEnter`.
  - `SessionWindow` (record): giờ mở/đóng, `isOpen`, `isClosed`, `isUpcoming`, `phaseAt` (sắp diễn ra/đang mở/đã đóng),
    `remainingSeconds`, `ensureOpen`.
  - `SessionPolicy` (record): `shuffleQuestions`, `shuffleOptions`, `showAnswer`.
  - `PaperShuffler`: hàm generic `<T> List<T> shuffle(List<T>, long seed)` và công thức seed (giữ nguyên công thức M4).
- **Lý do:** máy thí sinh không có đối tượng `ExamSession` (vì `ExamSession` chứa đề có đáp án) nhưng vẫn phải tính
  "ca đang mở không", "còn bao nhiêu giây", và trộn đề y hệt máy giảng viên. Tách ra thì hai phía dùng chung đúng
  một chỗ tính giờ và một chỗ trộn, không thể lệch nhau. Viết generic để trộn được cả `ExamItem` (phía giảng viên)
  lẫn `PaperItem` (phía thí sinh) – kiểm thử `PaperShufflerTest` chứng minh hai bên ra cùng thứ tự.
- **Ảnh hưởng:** constructor `ExamSession(id, exam, SessionWindow, SessionPolicy)` thay cho 7 tham số;
  `ExamSessionService.createSession` nhận `SessionWindow` và `SessionPolicy` (mẫu *Parameter Object*: tránh truyền
  nhầm thứ tự ba giá trị boolean). Enum `ExamSessionService.Phase` chuyển thành `model.SessionPhase`.
  Bỏ `ExamSession.closeAndSubmitAll` (việc này nay là một câu UPDATE trong `GradingDao.closeExpired`).

### 3. Mô hình riêng cho máy thí sinh, không chứa đáp án – gói `client`
- **Thay đổi:** thêm gói `com.group2.oes.client`: `SessionInfo`, `PaperItem`, `PaperOption` (không có cờ đúng/sai),
  `ExamPaper`, `AttemptState` (câu trả lời, câu đánh dấu, số lần vi phạm, điểm khi đã chấm), `ReviewItem`
  (xem lại sau khi thi), `ExamAttempt` (gói ba thứ màn hình làm bài cần).
- **Lý do:** NFR-04. Lớp `Question` có `isCorrect`, `AnswerOption` có `isCorrect` – chỉ cần đối tượng tới được
  máy thí sinh là đáp án đã tới. Mô hình riêng làm cho việc lộ đáp án **không thể viết ra được**: kiểu dữ liệu không
  có trường đó.
- **Ảnh hưởng:** định dạng câu trả lời không đổi (`"12"`, `"12,14"`, `"true"`, văn bản); câu trả lời vẫn lưu theo số
  thứ tự câu trong đề gốc nên thứ tự hiển thị sau khi trộn không ảnh hưởng tới việc chấm.

### 4. Tách interface DAO theo người dùng – **ISP**
- **Thay đổi:**
  - `StudentExamDao` (mới): mọi thao tác của máy thí sinh – đọc ca, đọc đề, tạo bài, lưu câu trả lời, đánh dấu,
    tăng vi phạm, nộp bài, xem lại, lấy giờ máy chủ.
  - `GradingDao` (mới): `closeExpired(now)`, `findSubmittedIds()`, `load(id)`, `saveGrades(submission)`.
  - `StudentLoginDao` (mới): chỉ `findByUsername`, đọc khung nhìn `v_login`.
  - `SubmissionDao` thu gọn còn `findAllOfSession` (thống kê, bảng kết quả).
    `ExamSessionDao` bỏ `findOfStudent` và `findClosedWithOpenSubmissions` (đã chuyển sang hai interface trên).
- **Lý do:** "không ép bên dùng phụ thuộc vào hàm nó không dùng". Ứng dụng thi chỉ thấy `StudentExamDao`
  và `StudentLoginDao`, nên không có đường nào để gọi `QuestionDao` hay ghi điểm – kể cả do lỗi lập trình.
- **`JdbcStudentLoginDao` viết riêng** thay vì cho `JdbcUserDao` nhận tên bảng/khung nhìn qua constructor:
  khung nhìn chỉ có đủ cột cho đăng nhập (không có `class_id`), và quan trọng hơn, kiểu `StudentLoginDao` không có
  `insert`/`update`, nên ứng dụng thi không thể gọi nhầm. Dùng lại hàm `JdbcUserDao.mapUser` để dựng đúng lớp con.
- **`GradingDao` giữ đúng chữ ký trong kế hoạch.** `load(id)` mỗi lần đọc lại ca thi và đề; với 60 bài nộp cùng lúc là
  vài trăm truy vấn nhỏ trong LAN, chưa cần tối ưu.

### 5. `SubmissionService` thay bằng `StudentExamService` + `GradingWorker`
- **Thay đổi:** `SubmissionService` (M4) bị xóa.
  - `StudentExamService` (máy thí sinh): `sessionsOf`, `enter`, `saveAnswer`, `setFlag`, `submit`, `refresh`, `review`.
    Giữ nguyên các luồng UC-13/UC-16 của M4: vào lại thì mở bài cũ; hai máy cùng bấm "Vào thi" thì máy sau mở bài của
    máy trước (`DuplicateKeyException`); nộp hai lần giữ lần đầu; máy khác đã nộp thì đọc lại trạng thái thật.
    Mới: kiểm tra thí sinh chỉ thao tác trên bài của chính mình (`NOT_OWNER`).
  - `GradingWorker` (máy giảng viên): `runOnce()` đóng các ca quá giờ rồi chấm mọi bài `SUBMITTED`. Thay cho
    `closeExpiredSessions` mà M4 tự nhận là "chưa có ai gọi". Lỗi kết nối thì dừng cả lượt; lỗi riêng một bài thì
    bỏ qua bài đó, chấm tiếp.
  - `ProctoringService` nhận `ViolationPolicy` qua constructor (**DIP**: phụ thuộc vào quy chế được truyền vào,
    kiểm thử được với quy chế khác).
  - `StudentAuthService` (mới): đăng nhập trên ứng dụng thi, chỉ nhận tài khoản thí sinh; tài khoản giảng viên được
    báo rõ "hãy dùng ứng dụng quản lý" (mã `WRONG_APP`).
- **Hệ quả nhìn thấy được:** nộp bài xong, bài ở trạng thái `SUBMITTED` cho tới khi máy chấm chạy. Màn hình kết quả
  (Phase 6) hiện "Đang chấm bài…" và hỏi lại định kỳ.
- **Thời điểm nộp khi hết giờ:** `submitted_at` ghi bằng **giờ đóng ca**, không phải lúc máy chấm chạy, để bảng điểm
  phản ánh đúng BR-03 dù máy chấm chạy trễ.

### 6. Hai nơi lắp ráp: `AppContext` (giảng viên) và `ClientContext` (thí sinh) – **DIP**
- **Thay đổi:** `AppContext` chỉ còn phía giảng viên (thêm `grading`, `gradingWorker`); `ClientContext` mới lắp ráp
  phía thí sinh. Bản bộ nhớ: `AppContext.inMemory(db)` và `ClientContext.inMemory(db)` dùng chung một
  `InMemoryDatabase` – như hai máy cùng nối một CSDL.
- **Lý do:** service chỉ biết interface DAO; mỗi ứng dụng chỉ được lắp những DAO nó được phép dùng.

### 7. CSDL: hai tài khoản, khung nhìn, trigger – `db/security.sql`
- **Thay đổi:** tệp mới `db/security.sql`, chạy sau `schema.sql`:
  - Khung nhìn `v_login`, `v_session_for_student`, `v_exam_paper`, `v_review`
    (thay cho `v_exam_item_for_student` của M4).
  - Tài khoản `oes_staff` (toàn quyền) và `oes_client` (chỉ đọc khung nhìn; ghi `submission`, `answer` theo cột,
    **không** có quyền ghi `total_score`, `earned_point`, không đọc được `question`, `answer_option`).
  - Bốn trigger chặn máy thí sinh: tạo bài với trạng thái khác `IN_PROGRESS`; sửa bài đã nộp; tự đặt `GRADED`;
    giảm số lần vi phạm; ghi câu trả lời khi bài đã khóa hoặc ca đã quá giờ đóng (BR-04 ở mức CSDL).
  - `schema.sql`: bảng `answer` thêm cột `flagged` (FR-22); bảng `submission` thêm chỉ mục `status`.
  - `JdbcErrors.translate` đổi `SQLSTATE 45000` (trigger từ chối) thành `SessionRuleException(SUBMISSION_LOCKED)`;
    kiểu trả về nới từ `DataAccessException` thành `ExamAppException`.
  - Tệp cấu hình tách đôi: `config/db-staff.properties.example`, `config/db-client.properties.example`.
- **Khác với chữ trong kế hoạch: trigger dùng `USER()` chứ không dùng `CURRENT_USER()`.** Bên trong trigger,
  `CURRENT_USER()` trả về *người tạo trigger* (root), không phải người đang kết nối, nên điều kiện sẽ không bao giờ
  đúng. `USER()` trả về tên đăng nhập thật kèm máy (`oes_client@192.168.1.23`). Đã kiểm chứng trên MySQL 8.0.40.
- **Chặt hơn kế hoạch ở ba chỗ:**
  - `v_login` chỉ trả mã băm mật khẩu của tài khoản thí sinh (mã băm rỗng với giảng viên, quản trị viên): mật khẩu
    `oes_client` nằm trong tệp cấu hình trên mọi máy trạm, không được để lộ mã băm của tài khoản quản trị.
  - `v_exam_paper` chỉ hiện đề đã có ca thi tới giờ mở: thí sinh không đọc trước được đề của ca ngày mai.
  - Trigger còn chặn giảm `violation_count` và chặn tạo bài ở trạng thái khác `IN_PROGRESS` – những lỗ quyền theo cột
    không bịt được.
- **Đáp án chỉ xem được sau khi ca đóng** (`v_review` lọc `close_time <= NOW() AND show_answer`): chặt hơn M1
  ("xem sau khi nộp"), để thí sinh nộp sớm không lộ đáp án cho người còn đang thi.
- **Kiểm chứng:** `JdbcSecurityTest` (7 ca) chạy bằng tài khoản `oes_client` thật: `SELECT` bảng đáp án bị từ chối,
  `UPDATE total_score` bị từ chối, ghi câu trả lời sau giờ đóng bị trigger chặn, đáp án chỉ hiện sau khi đóng ca.

### 8. `InMemoryDatabase` lưu theo dòng, tách mỗi DAO một lớp
- **Thay đổi:** bài làm lưu dạng `SubmissionRow` (giống một dòng `submission` kèm các dòng `answer`) thay vì lưu thẳng
  đối tượng `Submission` kèm các bảng phụ "trạng thái đã lưu". Mỗi DAO một lớp `Memory...Dao` cùng gói, dùng chung
  `InMemoryDatabase` làm khóa đồng bộ.
- **Lý do:** phía thí sinh và phía giảng viên dựng hai kiểu đối tượng khác nhau từ cùng dữ liệu, giống như đọc MySQL.
  Bản bộ nhớ cũng làm đúng việc của khung nhìn và trigger (không có cờ đúng/sai trong đề, không ghi sau giờ đóng,
  đáp án chỉ hiện sau khi đóng ca), nên kiểm thử nghiệp vụ không cần MySQL vẫn kiểm tra được BR-04 và NFR-04.

### Giới hạn còn lại (ghi để trả lời vấn đáp trung thực)
- Mọi máy thí sinh dùng chung một tài khoản `oes_client`, nên CSDL không phân biệt được *thí sinh nào* đang gửi lệnh.
  Người sửa ứng dụng thi vẫn có thể đọc hoặc ghi câu trả lời của **thí sinh khác** trong bảng `answer`. Muốn chặn
  hẳn cần một tầng máy chủ ứng dụng ở giữa (ngoài phạm vi: giảng viên chỉ cho dùng Java + JavaFX + JDBC).
  Điều NFR-04 yêu cầu – không đọc được đáp án, không tự sửa điểm – thì đã được CSDL bảo đảm.
