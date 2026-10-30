# Phân công vấn đáp

[README](../README.md) · [Phân công](PHAN-CONG.md) · [Tài liệu vấn đáp](vandap/) · [Chạy trên máy cá nhân](SETUP.md) · [Dựng phòng máy](RUNBOOK.md) · [Kịch bản demo](DEMO-SCRIPT.md)

Dự án chia thành **4 phần theo tính năng**. Mỗi người nắm trọn một phần **xuyên suốt ba tầng**: giao diện (FXML +
controller) → service → DAO → bảng/khung nhìn trong CSDL. Khi giảng viên chỉ vào một nút bấm và hỏi "bấm vào thì chuyện
gì xảy ra", người phụ trách phải lần được tới câu SQL cuối cùng.

> Tên gán theo thứ tự trên trang bìa báo cáo. Muốn đổi người, chỉ cần sửa bảng dưới đây và tên trong
> `docs/DEMO-SCRIPT.md`; nội dung các tài liệu không phụ thuộc vào tên.

| Phần | Người phụ trách | Nội dung | Tài liệu cần đọc kỹ | Đoạn demo |
|---|---|---|---|---|
| 1 | **Đỗ Phạm Bảo Hoàng** (B24DCCE098) | Người dùng, phân quyền, quản trị, khung ứng dụng | [`vandap/phan-1-nguoi-dung-quan-tri.md`](vandap/phan-1-nguoi-dung-quan-tri.md) | Giới thiệu và quản trị (2 phút) |
| 2 | **Đỗ Thị Vân Anh** (B24DCCE007) | Ngân hàng câu hỏi và đề thi | [`vandap/phan-2-cau-hoi-de-thi.md`](vandap/phan-2-cau-hoi-de-thi.md) | Giảng viên: câu hỏi và đề thi (2 phút 30) |
| 3 | **Nguyễn Hoàng Trung** (B24DCCE273) | Ca thi và làm bài (máy thí sinh) | [`vandap/phan-3-ca-thi-lam-bai.md`](vandap/phan-3-ca-thi-lam-bai.md) | Thí sinh làm bài (3 phút) |
| 4 | **Phan Tùng Dương** (B24DCCE070) | Chấm bài, kết quả, CSDL và bảo mật | [`vandap/phan-4-cham-bai-ket-qua-csdl.md`](vandap/phan-4-cham-bai-ket-qua-csdl.md) | Chấm bài, kết quả, bảo mật (2 phút 30) |

**Ai cũng phải đọc:** tệp này (phần "Câu hỏi chung" bên dưới), `README.md`, mục "Quyết định nghiệp vụ đã chốt" trong
`CLAUDE.md`, và lướt `docs/DESIGN-CHANGES.md`. Giảng viên có thể hỏi chéo sang phần khác ở mức "phần đó làm gì,
nối với phần mình ở đâu" – mục *Ranh giới với các phần khác* trong mỗi tài liệu trả lời câu đó.
Sơ đồ ERD, lớp, tuần tự của từng phần: [`DIAGRAMS.md`](DIAGRAMS.md) (mục lục ghi rõ sơ đồ nào thuộc phần nào).

---

## Tệp thuộc phần nào

Đường dẫn rút gọn: `oes/` = `src/main/java/com/group2/oes/`, `fxml/` = `src/main/resources/fxml/`,
`test/` = `src/test/java/com/group2/oes/`.

### Phần 1 – Người dùng, phân quyền, quản trị, khung ứng dụng (Đỗ Phạm Bảo Hoàng)

| Tầng | Tệp |
|---|---|
| Model | `oes/model/User`, `Administrator`, `Instructor`, `Student`, `Role`, `Subject`, `CourseClass` |
| Ngoại lệ | `oes/exception/ExamAppException`, `ValidationException`, `AccessDeniedException`, `DataAccessException`, `DuplicateKeyException`, `SessionRuleException` |
| Service | `oes/service/Permissions`, `AuthService`, `NewAccount`, `StudentAuthService`, `UserImportService`, `ImportResult`, `SubjectService`, `AuditLogService` |
| DAO | `oes/dao/UserDao`, `SubjectDao`, `StudentLoginDao` + `dao/jdbc/JdbcUserDao`, `JdbcSubjectDao`, `JdbcStudentLoginDao` + `dao/memory/MemoryUserDao`, `MemorySubjectDao`, `MemoryStudentLoginDao` |
| Tiện ích | `oes/util/PasswordHasher`, `AuditLog`, `CsvLine` |
| Lắp ráp | `oes/app/AppContext`, `ClientContext`, `ConsoleDemo`, `LoggingSetup` |
| Khung giao diện | `oes/ui/OesApplication`, `AppShell`, `StaffApp`, `ExamClientApp`, `StaffLauncher`, `ClientLauncher`, `Navigator`, `UserSession`, `BackgroundTasks`, `ErrorDialogs`, `StaffWorkspace`, `FormDialog`, `Tables`, `Formats` |
| Màn hình | `LoginController`, `ChangePasswordController`, `LoadingController`, `StaffHomeController`, `ProfileController`, `UsersController`, `UserFormController`, `SubjectsController`, `AuditLogController` + FXML cùng tên; `css/app.css` |
| CSDL | bảng `app_user`, `subject`, `course_class`, `instructor_subject`; khung nhìn `v_login`; thủ tục `sp_student_change_password` |
| Kiểm thử | `test/StaffServicesTest` (phần tài khoản), `AdminAndResultsTest` (phần quản trị), `ErrorCodeTest`, `CsvLineTest` |

### Phần 2 – Ngân hàng câu hỏi và đề thi (Đỗ Thị Vân Anh)

| Tầng | Tệp |
|---|---|
| Model | `oes/model/question/Question`, `SingleChoiceQuestion`, `MultiChoiceQuestion`, `TrueFalseQuestion`, `FillInBlankQuestion`, `AnswerOption`, `QuestionType`; `oes/model/QuestionBank`, `Exam`, `ExamItem`, `ExamMatrix`, `Criteria`, `Difficulty`, `MultiChoicePolicy` |
| Factory | `oes/factory/QuestionFactory`, `QuestionData` |
| Service | `oes/service/QuestionBankService`, `ExamService` |
| DAO | `oes/dao/QuestionDao`, `ExamDao` + `JdbcQuestionDao`, `JdbcExamDao` + `MemoryQuestionDao`, `MemoryExamDao` |
| Công cụ | `oes/tools/SeedTool`; `src/main/resources/seed-questions.txt`, `sample-questions.txt`, `seed-students.csv` (danh sách lớp thật) |
| Màn hình | `QuestionBankController`, `QuestionFormController`, `ExamListController`, `ExamBuilderController` + FXML cùng tên |
| CSDL | bảng `question`, `answer_option`, `exam`, `exam_item` |
| Kiểm thử | `test/QuestionTest`, `ExamAndBankTest`, `SeedToolTest`, `StaffServicesTest` (phần câu hỏi, đề) |

### Phần 3 – Ca thi và làm bài (Nguyễn Hoàng Trung)

| Tầng | Tệp |
|---|---|
| Model | `oes/model/ExamSession`, `SessionWindow`, `SessionPolicy`, `SessionPhase`, `PaperShuffler`, `ViolationPolicy` |
| Mô hình máy thí sinh | toàn bộ `oes/client/`: `SessionInfo`, `SessionEntry`, `ExamPaper`, `PaperItem`, `PaperOption`, `ExamAttempt`, `AttemptState`, `AnswerSheet`, `AnswerFormat`, `ServerClock`, `ReviewItem` |
| Service | `oes/service/ExamSessionService`, `StudentExamService`, `ProctoringService` |
| DAO | `oes/dao/ExamSessionDao`, `StudentExamDao` + `JdbcExamSessionDao`, `JdbcStudentExamDao` + `MemorySessionDao`, `MemoryStudentExamDao` |
| Giao diện | `oes/ui/ExamProctor`, `AnswerSaver`, `AppConfig` |
| Màn hình | `SessionFormController`, `StaffSessionsController`, `SessionListController`, `RulesController`, `ExamController`, `ServerSetupController` + FXML cùng tên |
| CSDL | bảng `exam_session`, `session_student`; khung nhìn `v_session_for_student`, `v_exam_paper`; quyền ghi của `oes_client` lên `submission`, `answer` |
| Kiểm thử | `test/SessionWindowTest`, `PaperShufflerTest`, `ViolationPolicyTest`, `ClientModelTest`, `SubmissionFlowTest`; `test/ui/UiSmoke` |

### Phần 4 – Chấm bài, kết quả, CSDL và bảo mật (Phan Tùng Dương)

| Tầng | Tệp |
|---|---|
| Model | `oes/model/Submission`, `SubmissionStatus`, `Answer`, `EndReason`, `ResultRow`, `ResultStatistics`, `SessionSummary` |
| Chấm điểm | toàn bộ `oes/grading/`: `GradingStrategy`, `ExactMatchGrading`, `PartialMatchGrading`, `TextMatchGrading`, `SubmissionGrader` |
| Service | `oes/service/GradingWorker`, `GradingScheduler`, `StatisticsService` |
| DAO | `oes/dao/GradingDao`, `SubmissionDao` + `JdbcGradingDao`, `JdbcSubmissionDao` + `MemoryGradingDao`, `MemorySubmissionDao`, `SubmissionRow` |
| Tầng dữ liệu chung | `oes/dao/jdbc/ConnectionManager`, `JdbcErrors`; `oes/dao/memory/InMemoryDatabase`, `ManualClock` |
| Máy chấm | `oes/app/GradingDaemon` |
| Màn hình | `SessionResultsController`, `ResultController` + FXML cùng tên |
| CSDL | **toàn bộ** `db/schema.sql` và `db/security.sql`: bảng `submission`, `answer`; hai tài khoản MySQL, khung nhìn `v_review`, phân quyền, 4 trigger |
| Dựng CSDL | `oes/tools/DatabaseSetup`, `SqlScript`; thư mục `scripts/` (`setup`, `setup-docker`, `start-*`, `test`) |
| Kiểm thử | `test/SqlScriptTest`, `test/tools/DatabaseSetupTest`, `test/SubmissionGraderTest`, `GradingTest`, `GradingSchedulerTest`, `JdbcIntegrationTest`, `JdbcSecurityTest`, `AdminAndResultsTest` (phần kết quả); `test/tools/LoadSimulator`, `docs/load-test.md` |

---

## Câu hỏi chung – ai cũng phải trả lời được

**1. Hệ thống gồm những gì, chạy thế nào?**
Một MySQL chung trong mạng LAN phòng máy và hai ứng dụng desktop JavaFX. *StaffApp* cho quản trị viên và giảng viên,
khi mở thì đồng thời là máy chấm bài. *ExamClientApp* cho thí sinh. Không có máy chủ ứng dụng ở giữa: cả hai ứng dụng
nối thẳng vào MySQL bằng JDBC, nhưng bằng **hai tài khoản MySQL khác quyền** (`oes_staff` toàn quyền, `oes_client`
chỉ đọc các khung nhìn không có đáp án).

**2. Kiến trúc ba tầng là gì, tầng nào làm gì?**
- *View* (`ui`, `ui.controller`, FXML): đọc ô nhập, gọi service, hiện kết quả hoặc lỗi. Không có logic nghiệp vụ.
- *Service* (`service`): quy tắc nghiệp vụ và kiểm tra quyền (`Permissions`).
- *DAO* (`dao`, `dao.jdbc`, `dao.memory`): đọc ghi dữ liệu, chỉ chứa SQL.
- *Model* (`model`, `client`, `grading`) dùng chung cho cả ba tầng: không import `javafx`, không chứa SQL.

Lợi ích: đổi giao diện không phải sửa nghiệp vụ; nghiệp vụ được kiểm thử bằng DAO trong bộ nhớ mà không cần MySQL
(`ConsoleDemo`, phần lớn kiểm thử JUnit chạy như vậy).

**3. Vì sao controller không được chứa logic nghiệp vụ?**
Controller JavaFX chỉ chạy được khi có giao diện, nên logic đặt ở đó không kiểm thử tự động được và không dùng lại được.
Ví dụ: quy tắc "câu một đáp án phải có đúng một phương án đúng" nằm trong `SingleChoiceQuestion`, controller chỉ
bắt `ValidationException` rồi hiện thông báo. Kiểm tra quyền cũng ở service, nên dù giao diện quên ẩn một nút, gọi
service vẫn bị từ chối.

**4. Service biết DAO nào? Đối tượng được tạo ở đâu?**
Service chỉ biết **interface** DAO, nhận qua constructor. Việc chọn cài đặt (JDBC hay bộ nhớ) và `new` các đối tượng
chỉ nằm ở hai nơi lắp ráp: `AppContext` (phía giảng viên) và `ClientContext` (phía thí sinh). Đây là nguyên lý
**DIP**: tầng trên phụ thuộc vào trừu tượng, không phụ thuộc vào lớp cụ thể.

**5. Năm nguyên lý SOLID, mỗi cái một ví dụ trong dự án?**
- **S** – `Submission` chỉ giữ dữ liệu và chuyển trạng thái; chấm điểm sang `SubmissionGrader`, đếm vi phạm sang
  `ViolationPolicy` (phần 3, 4).
- **O** – thêm cách chấm mới thì viết thêm một lớp `GradingStrategy`, không sửa lớp cũ (phần 4); thêm loại câu hỏi thì
  thêm lớp con `Question` (phần 2).
- **L** – mọi lớp con `Question` dùng được ở chỗ cần `Question`; mọi lớp con `User` trả lời `canManage` theo cách
  riêng mà nơi gọi không cần biết là lớp nào (phần 1, 2).
- **I** – DAO tách theo người dùng: máy thí sinh chỉ thấy `StudentExamDao`, `StudentLoginDao`, không thấy hàm chấm
  điểm hay đọc đáp án (phần 3, 4).
- **D** – service nhận interface DAO qua constructor, `AppContext`/`ClientContext` lắp ráp (phần 1).

**6. Những mẫu thiết kế nào được dùng?**
Factory (`QuestionFactory`, phần 2), Strategy (`GradingStrategy`, phần 4), Template Method (`OesApplication`, phần 1),
Composition Root (`AppContext`, `ClientContext`, phần 1). Ngoài ra: record bất biến cho đối tượng giá trị
(`SessionWindow`, `SessionPolicy`, ...).

**7. Vì sao dùng `record`?** Java 17 cho phép. Record là lớp dữ liệu bất biến: tự có constructor, `equals`,
`hashCode`, `toString`. Dùng cho đối tượng giá trị như `SessionWindow` (giờ mở – giờ đóng); kiểm tra dữ liệu đặt trong
*compact constructor*, nên không thể tạo một `SessionWindow` có giờ đóng trước giờ mở.

**8. Vì sao không dùng Spring, Hibernate?** Ràng buộc của giảng viên: chỉ Java 17, JavaFX, JDBC thuần, Maven, JUnit.
Vì vậy tự lắp ráp đối tượng trong `AppContext` thay cho Spring, tự viết SQL với `PreparedStatement` thay cho Hibernate.

**9. Vì sao không bao giờ gọi CSDL trên luồng giao diện?** JavaFX vẽ màn hình trên một luồng duy nhất (JavaFX
Application Thread). Gọi CSDL trên luồng đó thì mạng chậm vài giây là cửa sổ đứng hình. Mọi lời gọi service đi qua
`BackgroundTasks.run(việc, khiXong, khiLỗi)`: việc chạy trên luồng nền, kết quả quay về luồng giao diện để cập nhật.

**10. Lỗi được xử lý thế nào?** Mọi lỗi nghiệp vụ là lớp con của `ExamAppException`, có **mã lỗi ổn định** (ví dụ
`SESSION_CLOSED`) và thông điệp tiếng Việt cho người dùng. `ErrorDialogs` hiện `getUserMessage()`; lỗi không lường
trước thì hiện "Đã có lỗi không mong muốn" và ghi stack trace vào log. Lỗi SQL được `JdbcErrors` dịch thành ngoại lệ
của hệ thống, nên tầng trên không bao giờ thấy `SQLException`.

**11. Chống SQL injection thế nào?** Luôn dùng `PreparedStatement` với tham số `?`, không bao giờ nối chuỗi giá trị
vào câu SQL.

**12. Kiểm thử thế nào?** 134 kiểm thử JUnit 5: 112 chạy với DAO trong bộ nhớ (không cần MySQL), 22 chạy với MySQL thật,
trong đó 8 ca dùng chính tài khoản `oes_client` để chứng minh máy thí sinh không đọc được đáp án. Ngoài ra có kiểm thử
tải 60 thí sinh (`docs/load-test.md`) và danh sách kiểm thử tay (`docs/manual-test-checklist.md`).

**13. Khác gì so với thiết kế Milestone 3 / mã Milestone 4?** Xem `docs/DESIGN-CHANGES.md`. Ý chính: bản M4 chấm bài
trên máy thí sinh nên máy thí sinh phải đọc được đáp án – vi phạm NFR-04. M5 chuyển việc chấm sang máy giảng viên,
tách `Submission`/`ExamSession` theo SRP, tách DAO theo ISP, thêm hai tài khoản MySQL, khung nhìn và trigger.

**14. Giới hạn còn lại?** Mọi máy thí sinh dùng chung tài khoản `oes_client`, nên ứng dụng thi bị sửa vẫn đọc/ghi được
câu trả lời của thí sinh khác (không đọc được đáp án, không sửa được điểm). Chặn hẳn cần máy chủ ứng dụng ở giữa –
ngoài công nghệ được phép. Hai luồng ngoại lệ trong M1 chưa làm: UC-08 5a (đổi một câu trong bản xem trước đề) và
UC-13 3a (không vào được toàn màn hình thì không cho thi).
