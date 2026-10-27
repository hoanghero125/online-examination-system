# Online Examination System – Nhóm 2

Bài tập lớn Lập trình hướng đối tượng, đề tài 16 (lớp D24CQCE07-B, giảng viên Phạm Hoàng Việt).
Hệ thống thi trắc nghiệm cho phòng máy: một MySQL chung trong mạng LAN, hai ứng dụng desktop JavaFX.

- **Ứng dụng quản lý (StaffApp)** – quản trị viên: người dùng, môn học, lớp, nhật ký; giảng viên: ngân hàng câu hỏi,
  đề thi, ca thi, kết quả và thống kê. Khi mở, ứng dụng này đồng thời là **máy chấm bài**.
- **Ứng dụng thi (ExamClientApp)** – thí sinh: danh sách ca thi, làm bài toàn màn hình có giám sát rời cửa sổ, kết quả.
  Máy thí sinh dùng tài khoản MySQL riêng, **không đọc được đáp án** và không chấm bài (NFR-04).

Thành viên: Đỗ Phạm Bảo Hoàng (B24DCCE098), Đỗ Thị Vân Anh (B24DCCE007), Nguyễn Hoàng Trung (B24DCCE273),
Phan Tùng Dương (B24DCCE070).

Công nghệ: Java 17, JavaFX 21 (FXML + CSS), JDBC thuần với MySQL Connector/J, Maven, JUnit 5. Không dùng framework nào khác.

## Mở trong IntelliJ IDEA

`File → Open…` chọn thư mục này (IntelliJ tự nhận dự án Maven). Các cấu hình chạy có sẵn ở góc trên bên phải:

| Cấu hình | Việc |
|---|---|
| StaffApp (quản lý) | `mvn javafx:run@staff` |
| ExamClientApp (thi) | `mvn javafx:run@client` |
| ConsoleDemo | Chạy toàn bộ kịch bản nghiệp vụ trên console, không cần MySQL |
| GradingDaemon (máy chấm) | Máy chấm không giao diện, đọc `db-staff.properties` |
| SeedTool (dữ liệu mẫu) | Tạo dữ liệu demo, đọc `db-staff.properties` |
| mvn test / Tất cả kiểm thử | Chạy kiểm thử |

Không cần cài Maven: dùng `./mvnw` (Linux/macOS) hoặc `mvnw.cmd` (Windows) đi kèm.

## Chạy nhanh

```bash
./mvnw compile exec:java          # ConsoleDemo, dữ liệu trong bộ nhớ, không cần MySQL
./mvnw javafx:run@staff           # ứng dụng quản lý
./mvnw javafx:run@client          # ứng dụng thi
```

Khi chạy từ IntelliJ / Maven, tệp cấu hình `db-staff.properties`, `db-client.properties` đặt ở **thư mục dự án**
(mẫu trong `config/`). Chưa có tệp thì ứng dụng hiện màn hình nhập địa chỉ máy chủ và tự lưu.

## Đóng gói và dựng phòng máy

```bash
./mvnw package                    # tạo dist/oes-staff.jar, dist/oes-client.jar
```

Thư mục `dist/` có sẵn script `run-staff`, `run-client`, `run-grading-daemon`, `run-seed` (`.bat` cho Windows, `.sh`
cho Linux/macOS); tệp cấu hình đặt cạnh jar. **Jar build trên hệ điều hành nào thì chạy trên hệ điều hành đó**
(JavaFX có phần native) – hãy build trên máy Windows sẽ dùng. Cách dựng MySQL, phân quyền, kiểm tra mạng, dữ liệu mẫu:
[`docs/RUNBOOK.md`](docs/RUNBOOK.md).

## Kiểm thử

```bash
./mvnw test
```

Kiểm thử với MySQL thật (thêm 21 ca JDBC, gồm kiểm chứng phân quyền tài khoản `oes_client`) chạy khi có hai biến môi
trường trỏ tới tệp cấu hình của một CSDL thử đã chạy `db/schema.sql` rồi `db/security.sql`:

```bash
OES_TEST_DB=test-staff.properties OES_TEST_DB_CLIENT=test-client.properties ./mvnw test
```

Ngoài ra: `docs/load-test.md` (60 thí sinh đồng thời), `docs/manual-test-checklist.md` (kiểm thử tay theo use case),
`src/test/java/com/group2/oes/ui/UiSmoke.java` (chạy thử giao diện tự động trong Xvfb).

## Cấu trúc mã nguồn

```
com.group2.oes
├── model         lớp nghiệp vụ: User và 3 lớp con, Exam, ExamSession, SessionWindow, SessionPolicy,
│   │             Submission, ViolationPolicy, PaperShuffler, QuestionBank, ResultStatistics...
│   └── question  Question và 4 lớp con, AnswerOption
├── client        mô hình phía máy thí sinh, KHÔNG chứa đáp án: SessionInfo, ExamPaper, PaperItem,
│                 AttemptState, AnswerSheet, ServerClock, ReviewItem...
├── grading       GradingStrategy + 3 cách chấm (mẫu Strategy), SubmissionGrader
├── factory       QuestionFactory, QuestionData (mẫu Factory)
├── exception     ExamAppException và các lớp con, mỗi lỗi một mã ổn định
├── dao           interface DAO, tách theo người dùng (ISP): StudentExamDao, GradingDao, StudentLoginDao...
│   ├── jdbc      cài đặt JDBC cho MySQL
│   └── memory    cài đặt bằng Map, dùng cho kiểm thử và ConsoleDemo
├── service       nghiệp vụ và kiểm tra quyền: AuthService, StudentExamService, GradingWorker, ProctoringService...
├── app           AppContext (phía giảng viên), ClientContext (phía thí sinh), ConsoleDemo, GradingDaemon
├── tools         SeedTool
├── ui            hai ứng dụng JavaFX: OesApplication (Template Method), AppShell, Navigator, BackgroundTasks,
│   │             ErrorDialogs, ExamProctor, AnswerSaver...
│   └── controller một controller cho mỗi màn hình (src/main/resources/fxml/*.fxml)
└── util          PasswordHasher (SHA-256 + salt), AuditLog, CsvLine

db/               schema.sql (bảng), security.sql (tài khoản, khung nhìn, quyền, trigger)
config/           mẫu tệp cấu hình hai ứng dụng
dist/             script chạy; jar tạo bởi mvn package
docs/             tiến độ, các thay đổi thiết kế, hướng dẫn dựng phòng máy, kịch bản demo, báo cáo M1–M4
```

## Tài liệu cho báo cáo và vấn đáp

| Tệp | Nội dung |
|---|---|
| [`docs/DESIGN-CHANGES.md`](docs/DESIGN-CHANGES.md) | Mọi thay đổi so với thiết kế M3: thay đổi – lý do – ảnh hưởng, kèm nguyên lý SOLID và mẫu thiết kế |
| [`docs/PROGRESS.md`](docs/PROGRESS.md) | Tiến độ từng phase, bảng truy vết FR, câu hỏi mở cần nhóm xác nhận |
| [`docs/RUNBOOK.md`](docs/RUNBOOK.md) | Dựng phòng máy |
| [`docs/DEMO-SCRIPT.md`](docs/DEMO-SCRIPT.md) | Kịch bản demo 10 phút, câu hỏi thường gặp |
| [`docs/manual-test-checklist.md`](docs/manual-test-checklist.md) | Danh sách kiểm thử tay trước buổi bảo vệ |
| [`docs/load-test.md`](docs/load-test.md) | Kết quả kiểm thử tải (NFR-01, NFR-02) |
| [`docs/PLAN.md`](docs/PLAN.md) | Kế hoạch triển khai gốc |
