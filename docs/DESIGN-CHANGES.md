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

### 9. Báo vi phạm khi bài đã bị khóa ở nơi khác
- **Thay đổi:** nếu `increaseViolation` không tăng bộ đếm (máy chấm vừa đóng ca, hoặc máy khác vừa nộp),
  `ProctoringService.report` đọc lại trạng thái thật và trả kết quả "bài đã khóa", không báo "vi phạm lần N" giả.
- **Lý do:** máy thí sinh chỉ giữ bản sao trạng thái; CSDL mới là nơi quyết định.

### Giới hạn còn lại (ghi để trả lời vấn đáp trung thực)
- Mọi máy thí sinh dùng chung một tài khoản `oes_client`, nên CSDL không phân biệt được *thí sinh nào* đang gửi lệnh.
  Người sửa ứng dụng thi vẫn có thể đọc hoặc ghi câu trả lời của **thí sinh khác** trong bảng `answer`. Muốn chặn
  hẳn cần một tầng máy chủ ứng dụng ở giữa (ngoài phạm vi: giảng viên chỉ cho dùng Java + JavaFX + JDBC).
  Điều NFR-04 yêu cầu – không đọc được đáp án, không tự sửa điểm – thì đã được CSDL bảo đảm.

## Phase 2 – Hoàn thiện service phía giảng viên và quản trị

### 1. Đổi mật khẩu phía thí sinh qua thủ tục lưu trữ `sp_student_change_password`
- **Thay đổi:** cột `app_user.must_change_password` (thí sinh nhập từ CSV phải đổi mật khẩu lần đầu);
  `v_login` có thêm cột này; thủ tục `sp_student_change_password(id, mã_băm_cũ, mã_băm_mới, OUT đổi_được)` chạy với
  quyền người tạo (`SQL SECURITY DEFINER`); `oes_client` chỉ được `EXECUTE` thủ tục này.
- **Lý do:** thí sinh chỉ dùng ứng dụng thi, mà `oes_client` không được có quyền `UPDATE app_user` (sẽ sửa được mật khẩu
  của cả quản trị viên). Thủ tục chỉ sửa đúng một dòng: tài khoản **thí sinh** có mã băm cũ khớp (so sánh rồi mới ghi).
- **Giới hạn:** ai có mật khẩu `oes_client` cũng đọc được mã băm của mọi thí sinh qua `v_login`, nên kiểm tra "mã băm cũ"
  chỉ chặn được dùng nhầm, không chặn được người cố ý đổi mật khẩu của bạn khác. Cùng giới hạn với mục
  "Giới hạn còn lại" ở Phase 1 (một tài khoản CSDL dùng chung cho mọi máy thí sinh).
- **Ảnh hưởng:** `User` tách đổi mật khẩu làm hai bước: `hashNewPassword` (kiểm tra ≥ 6 ký tự, băm) và
  `applyNewPasswordHash` (gán sau khi CSDL đã nhận). `JdbcErrors` chỉ đổi SQLSTATE 45000 thành `SUBMISSION_LOCKED`
  khi thông điệp bắt đầu bằng mã đó, để lỗi khác không bị báo nhầm là "bài đã nộp".

### 2. `ImportResult` thành lớp dùng chung
- **Thay đổi:** `QuestionBankService.ImportResult` (record lồng) chuyển thành `service.ImportResult`.
- **Lý do:** nhập người dùng (FR-06) trả về đúng kiểu kết quả như nhập câu hỏi (FR-12): số dòng nhập được + lỗi theo dòng.

### 3. `GradingScheduler` tách khỏi `GradingWorker` – **SRP**
- **Thay đổi:** `GradingWorker` chấm **một lượt**; `GradingScheduler` lặp lại lượt đó 3 giây/lần bằng
  `ScheduledExecutorService`. `GradingDaemon` (main không giao diện) và `StaffApp` (Phase 3) đều dùng `GradingScheduler`.
- **Lý do:** kiểm thử gọi thẳng `runOnce()` không phải chờ đồng hồ; việc hẹn giờ có cái bẫy riêng (một lượt ném ngoại lệ
  thì `ScheduledExecutorService` dừng hẳn) nên cần một chỗ riêng để xử lý.
- **Nhật ký không bị ngập:** bài lỗi được thử lại mỗi lượt nhưng chỉ ghi stack trace lần đầu; mất kết nối CSDL chỉ ghi
  một lần và một dòng khi kết nối lại. Nếu không, một bài hỏng trong ca thi 2 giờ sinh ra khoảng 2.400 stack trace
  trong `oes-audit.log` – đúng tệp màn hình Nhật ký (Phase 4) hiển thị.

### 4. Ứng dụng quản lý từ chối tài khoản thí sinh
- **Thay đổi:** `AuthService.login` báo `WRONG_APP` "Tài khoản thí sinh hãy dùng ứng dụng thi" – chỉ sau khi đã đúng
  mật khẩu, để sai mật khẩu thì vẫn không lộ tài khoản có tồn tại hay không.

### 5. Các hàm mới khác (không đổi thiết kế, chỉ bổ sung)
- `UserDao.search(keyword, role)`; `update` ghi thêm lớp học phần và cờ buộc đổi mật khẩu; người dùng đọc lên kèm lớp.
- `SubjectDao`: sửa tên môn, xóa môn (cấm khi đã có câu hỏi/đề/lớp), sửa lớp, tìm lớp theo mã, bỏ phân công.
  `CourseClass` vẫn bất biến: sửa lớp là tạo đối tượng mới cùng mã.
- `QuestionDao.update`: ghi lại câu hỏi và thay toàn bộ phương án trong một giao dịch (FR-08); cấm sửa câu đã nằm
  trong đề đã khóa, cùng điều kiện với xóa.
- `ExamSessionService.createSessionForClass` (FR-19), `ExamService.list`, `ExamService.newVersion` (BR-09),
  `ExamSessionService.listOfExam`, `StatisticsService.listResults` trả `ResultRow`.

## Phase 3 – Khung giao diện JavaFX

### 1. `OesApplication` – phần khởi động chung (mẫu **Template Method**)
- **Thay đổi:** lớp trừu tượng `ui.OesApplication<C>` giữ cố định các bước khởi động: tìm tệp cấu hình → kết nối CSDL
  trên luồng nền → màn hình đăng nhập; lỗi thì quay về màn hình nhập địa chỉ máy chủ. `StaffApp` và `ExamClientApp`
  chỉ điền các bước khác nhau (tên tệp cấu hình, tài khoản CSDL mặc định, dựng `AppContext` hay `ClientContext`).
- **Lý do:** hai ứng dụng có cùng quy trình khởi động; viết hai lần thì sửa một chỗ quên chỗ kia.

### 2. `AppShell` – một màn hình đăng nhập cho hai ứng dụng (đa hình, **DIP**)
- **Thay đổi:** interface `AppShell` (tên ứng dụng, `login`, `changePassword`, `showHome`, `showServerSetup`).
  `LoginController` và `ChangePasswordController` chỉ biết `AppShell`, không biết đang chạy ứng dụng nào.
- **Lý do:** kế hoạch yêu cầu một tệp `login.fxml` dùng chung, controller không chứa logic vai trò. Việc từ chối
  tài khoản sai ứng dụng đã nằm ở `AuthService` / `StudentAuthService` (Phase 1–2).

### 3. Truyền dữ liệu giữa màn hình qua tham số
- `Navigator.show(fxml, controller -> controller.init(...))`: nơi mở màn hình truyền thẳng service và dữ liệu cần thiết
  cho controller. Không dùng biến static nào ngoài `UserSession`; không dùng controller factory dò constructor bằng
  reflection – đọc chỗ mở màn hình là biết màn hình dùng gì.

### 4. Thời gian chờ kết nối mặc định trong `ConnectionManager`
- **Thay đổi:** `open()` đặt `connectTimeout = 5 s`, `socketTimeout = 30 s` nếu URL chưa ghi.
- **Lý do:** mặc định của driver là chờ vô hạn; máy chủ bật nhưng MySQL không nghe cổng thì ứng dụng treo khoảng
  2 phút. Kiểm thử `máyChủKhôngTrảLờiThìBáoLỗiSauVàiGiâyKhôngTreo` kiểm tra điều này.

### 5. Các lớp hạ tầng giao diện
- `BackgroundTasks`: mọi lệnh gọi service chạy trên luồng nền (`javafx.concurrent.Task` + 4 luồng daemon dùng chung);
  trong lúc chạy, khung nhập bị vô hiệu hóa và hiện con trỏ chờ; xong (kể cả lỗi) thì luôn bật lại.
- `ErrorDialogs`: nơi duy nhất hiện lỗi (NFR-08). `ExamAppException` → `getUserMessage()`; lỗi khác → câu chung
  "Đã có lỗi không mong muốn" + ghi stack trace vào nhật ký.
- `AppConfig`: tìm tệp cấu hình theo `--config=...` → cạnh tệp jar → màn hình nhập địa chỉ máy chủ (thử kết nối được
  mới lưu tệp).
- `Role.displayName()`: tên vai trò tiếng Việt đặt trong enum để mọi màn hình (và `Permissions`) dùng chung.
- `User.requireConfirmed`: kiểm tra "nhập lại mật khẩu" nằm ở model, controller chỉ đọc ô nhập.

## Phase 6 – Ứng dụng thi (làm trước Phase 4, 5 theo thứ tự ưu tiên ở mục 5 của kế hoạch)

### 1. Màn hình làm bài tách thành nhiều lớp nhỏ – **SRP**
`ExamController` chỉ vẽ giao diện và nối các phần; mỗi việc khó có lớp riêng, kiểm thử được riêng:

| Lớp | Gói | Việc |
|---|---|---|
| `AnswerSheet` | client | Câu trả lời đang hiện trên màn hình (kể cả chưa lưu kịp), đếm câu đã làm / chưa làm / đánh dấu |
| `AnswerFormat` | client | Đổi ô đánh dấu ↔ chuỗi `"12,14"`, cùng định dạng với lúc chấm |
| `ServerClock` | client | Giờ máy chủ = giờ lấy lúc vào thi + `System.nanoTime()` đã trôi; đồng bộ lại 60 giây/lần |
| `SessionEntry` | client | Quy tắc bật nút "Vào thi" / "Tiếp tục làm bài" / "Xem kết quả" |
| `AnswerSaver` | ui | Hàng đợi lưu: chờ 500 ms, mỗi câu giữ giá trị mới nhất, mất mạng thì dải vàng + thử lại 3 giây |
| `ExamProctor` | ui | Toàn màn hình, chặn Esc / Alt+F4, nghe 3 sự kiện rời cửa sổ |

- **`ServerClock` dùng `nanoTime` chứ không dùng `LocalDateTime.now()`:** thí sinh chỉnh đồng hồ máy trạm thì
  `LocalDateTime.now()` đổi theo, còn `nanoTime` chỉ đo khoảng thời gian nên không bị ảnh hưởng (quyết định số 6).
- **`AnswerSheet` tách khỏi `AttemptState`:** `AttemptState` là những gì CSDL đã nhận (service cập nhật trên luồng nền);
  `AnswerSheet` là những gì thí sinh đang thấy. Nếu dùng chung một đối tượng, khi mạng chậm thí sinh đổi ý A → B, lượt
  lưu A về muộn sẽ ghi đè B trên màn hình.
- **`AttemptState` thành an toàn đa luồng** (các hàm `synchronized`, `getAnswers` trả bản chép) vì service sửa nó trên
  luồng nền trong lúc giao diện đọc.

### 2. Hộp thoại không được tính là rời cửa sổ
- Chính hộp thoại của ứng dụng (cảnh báo vi phạm, xác nhận nộp bài) làm cửa sổ chính mất tiêu điểm. Mọi hộp thoại trong
  lúc thi mở qua `ExamProctor.showDialog(...)`: tạm ngừng nghe, chỉ nghe lại khi cửa sổ **đã trở lại toàn màn hình và
  có tiêu điểm** (không phải ngay lúc hộp thoại đóng – trên đường quay lại toàn màn hình JavaFX còn phát thêm sự kiện).
- Thông báo "còn 5 phút" và "mất kết nối" hiện bằng dải màu trong màn hình, không dùng hộp thoại, vì cùng lý do trên.
- Mỗi lần chỉ gửi một sự kiện vi phạm lên CSDL; sự kiện tới giữa chừng được gửi tiếp sau, để số lần trên màn hình không
  nhảy lùi (1/5 → 3/5 → 2/5) khi hai lượt gửi về không theo thứ tự.

### 3. Nộp bài
- Nút "Nộp bài" và hết giờ đi cùng một đường: gửi hết hàng đợi lưu bài trên luồng nền rồi mới `submit`. Mất mạng thì
  3 giây thử lại; CSDL báo ca đã đóng (trigger) thì bỏ các câu chưa lưu kịp (BR-04) nhưng vẫn nộp phần đã lưu.
- Màn hình kết quả dùng `AttemptState` mà `submit` trả về (có thể máy chấm đã đóng bài trước), không dùng bản sao cũ.

### 4. Bổ sung nhỏ ở model
- `EndReason.displayName()`, `SessionWindow.isAlmostOver(now)` với hằng `ALMOST_OVER = 5 phút`,
  `SessionWindow.formatRemaining`, `ViolationPolicy.rulesText()` (câu quy chế lấy đúng con số đang áp dụng),
  `ReviewItem.fullMarkCount`, `StudentExamService.overview` (danh sách ca kèm bài làm).

## Phase 4, 5 – Màn hình quản trị viên và giảng viên

### 1. `StaffWorkspace` – các màn hình chức năng mở nhau qua trang chủ
- **Thay đổi:** interface `ui.StaffWorkspace` (service, người dùng, `open(fxml, init)`, `gradeNow()`), do
  `StaffHomeController` cài đặt. Màn hình "Đề thi" mở "Tạo đề", "Mở ca thi"... vào vùng giữa thông qua interface này.
- **Lý do:** màn hình con chỉ phụ thuộc vào những gì nó cần (**ISP**, **DIP**), không phụ thuộc vào lớp trang chủ cụ thể.

### 2. Biểu mẫu câu hỏi không tự kiểm tra nghiệp vụ
- Controller chỉ gom ô nhập thành `QuestionData` (thêm `QuestionData.from(Question)` để điền sẵn khi sửa) rồi gọi service.
  Các quy tắc (ít nhất 2 phương án, đúng một đáp án đúng, có đáp án chấp nhận...) vẫn nằm trong constructor các lớp
  câu hỏi như từ M4. Lỗi hiện ngay dưới ô liên quan nhờ **mã lỗi ổn định** của `ValidationException`
  (`OPTIONS_TOO_FEW` → ô phương án...), không phải dò nội dung câu báo lỗi.
- Đáp án chấp nhận của câu điền khuyết hiện ở dạng đã chuẩn hóa (chữ thường) khi sửa – biểu mẫu ghi chú điều này.

### 3. Tạo tài khoản theo vai trò nằm ở service
- **Thay đổi:** `AuthService.createAccount(actor, NewAccount)` dựng đúng lớp con theo vai trò (switch trên enum, như
  `QuestionFactory`); `AuthService.resetPassword(actor, target)` sinh mật khẩu tạm 8 ký tự bỏ các ký tự dễ nhầm.
  Tài khoản do quản trị viên tạo hoặc đặt lại mật khẩu đều phải đổi mật khẩu ở lần đăng nhập đầu.
- **Lý do:** controller không được tự `new Student(...)` – việc chọn lớp con theo vai trò là logic nghiệp vụ.

### 4. Lớp hỗ trợ mới
- `SessionSummary` + `StatisticsService.summarize`: số thí sinh / đã vào thi / đã nộp cho danh sách ca thi.
- `AuditLogService`: đọc 500 dòng nhật ký mới nhất, lọc theo từ khóa, chỉ quản trị viên (NFR-05).
- `Difficulty`, `QuestionType`, `MultiChoicePolicy`, `SessionPhase` có `displayName()` tiếng Việt (giống `Role` ở Phase 3);
  không đổi `toString()` để các thông điệp và nhật ký cũ không đổi theo.
- `OesApplication` đặt bộ xử lý ngoại lệ cho luồng giao diện: lỗi lọt khỏi một nút bấm cũng hiện hộp thoại và ghi nhật ký.
- Bảng và danh sách rỗng hiện "Chưa có dữ liệu." (mặc định của JavaFX là tiếng Anh); nút hộp thoại là "Đóng".

## Phase 7 – Kiểm thử và gia cố

- `DuplicateKeyException.getUserMessage()` riêng: trước đây kế thừa câu "Không kết nối được tới máy chủ dữ liệu" của
  `DataAccessException`, nên một bản ghi trùng chưa được service bắt sẽ báo nhầm là mất mạng.
- Màn hình làm bài: đồng bộ 60 giây/lần giờ đây đọc lại cả trạng thái bài; cùng tài khoản đã nộp ở máy khác (hoặc máy
  chấm đã đóng ca) thì máy này tự chuyển sang màn hình kết quả.
- Hết giờ: chỉ gọi tự nộp một lần (cờ `timedOut`); mất mạng thì việc thử lại 3 giây/lần do `submit` lo, không nộp
  chồng mỗi giây.
- `LoadSimulator`, `UiSmoke` nằm trong `src/test` (không đóng gói vào sản phẩm), chạy bằng `java -cp` như mô tả trong
  `docs/load-test.md` và đầu tệp `UiSmoke.java`.

## Phase 8 – Đóng gói và demo

- **Đóng gói bằng `maven-shade-plugin`** (hai execution, hai jar). Đây là plugin build, không phải thư viện của chương
  trình, nhưng kế hoạch ghi rõ cần hỏi giảng viên – xem "Câu hỏi mở" trong `PROGRESS.md`. Nếu không được dùng:
  bỏ khối plugin trong `pom.xml`, chạy bằng `mvnw javafx:run@staff|client` hoặc cấu hình chạy của IntelliJ.
- **Ca thi mới mặc định mở ngay** (theo giờ máy chủ) và kéo dài 60 phút; trước đây mặc định mở sau 5 phút.
  Thực tế giảng viên thường mở ca ngay trước giờ thi, và buổi demo cần ca mở ngay.
- `SeedTool` nằm trong `src/main` (gói `tools`) để có sẵn trong `oes-staff.jar`; chỉ chạy trên CSDL chưa có dữ liệu mẫu.
