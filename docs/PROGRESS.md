# Tiến độ

## Các phase

| Phase | Nội dung | Trạng thái |
|---|---|---|
| 0 | Dựng repo, pom JavaFX, tài liệu | Xong |
| 1 | Refactor SOLID, NFR-04 (Milestone 5) | Xong |
| 2 | Service phía giảng viên và quản trị | Xong |
| 3 | Khung giao diện JavaFX | Xong |
| 4 | Màn hình quản trị viên | Chưa làm |
| 5 | Màn hình giảng viên | Chưa làm |
| 6 | Ứng dụng thi | Xong |
| 7 | Kiểm thử và gia cố | Chưa làm |
| 8 | Đóng gói, demo | Chưa làm |

## Nhật ký

### Phase 0
- Mã nguồn Milestone 4 đưa vào repo nguyên trạng (commit đầu tiên), 39/39 kiểm thử xanh trên MySQL 8.0.40.
- `pom.xml`: thêm JavaFX 21.0.5 (`javafx-controls`, `javafx-fxml`), `javafx-maven-plugin` với hai execution
  `staff` và `client`, bật `-Xlint:all`. Phiên bản 0.6.0.
- Thêm Maven Wrapper (`mvnw`, `mvnw.cmd`): máy chưa cài Maven vẫn build được, IntelliJ cũng nhận ra.
- Thêm cấu hình chạy dùng chung cho IntelliJ trong `.idea/runConfigurations/` (các tệp khác trong `.idea/` vẫn bị bỏ qua).
- `schema.sql` chuyển sang `db/`.

### Phase 1 – Milestone 5
- Máy thí sinh không còn chấm bài; `GradingWorker` trên máy giảng viên đóng ca quá giờ và chấm (chi tiết và nguyên lý
  SOLID: `docs/DESIGN-CHANGES.md`, mục Phase 1).
- `db/security.sql`: hai tài khoản `oes_staff`/`oes_client`, bốn khung nhìn, bốn trigger. Đã kiểm chứng bằng tay
  và bằng `JdbcSecurityTest` trên MySQL 8.0.40.
- Kiểm thử: 73 ca (58 chạy không cần CSDL; 15 ca JDBC gồm 8 `JdbcIntegrationTest` + 7 `JdbcSecurityTest`), tất cả xanh.
- `ConsoleDemo` cho ra cùng điểm như M4 (10 / 5 / 3), cả chế độ bộ nhớ lẫn MySQL hai tài khoản.
- Quyết định: trigger dùng `USER()` thay `CURRENT_USER()`; `v_login` không trả mã băm của giảng viên/quản trị;
  `v_exam_paper` chỉ hiện đề của ca đã tới giờ mở; `submitted_at` khi hết giờ ghi bằng giờ đóng ca.

### Phase 2
- FR-01, 04, 06, 07, 08, 19, 26, 30/34/35 có service và kiểm thử (`StaffServicesTest`, `GradingSchedulerTest`,
  thêm 4 ca vào `JdbcIntegrationTest`, 1 ca vào `JdbcSecurityTest`).
- `GradingDaemon` chạy từ dòng lệnh, in một dòng mỗi lượt có bài nộp/chấm, dừng bằng Ctrl+C.
- Kiểm thử: 102 ca, tất cả xanh trên MySQL 8.0.40 (20 ca JDBC: 12 tích hợp + 8 phân quyền).
- Quyết định: đổi mật khẩu phía thí sinh qua thủ tục lưu trữ; mật khẩu tối thiểu 6 ký tự kiểm tra trong `User`;
  thí sinh nhập từ CSV có tên đăng nhập = mật khẩu mặc định = mã sinh viên viết thường.

### Phase 3
- `StaffApp`, `ExamClientApp` (+ `StaffLauncher`, `ClientLauncher` cho jar), `OesApplication`, `AppShell`, `Navigator`,
  `UserSession`, `BackgroundTasks`, `ErrorDialogs`, `AppConfig`; màn hình đăng nhập, đổi mật khẩu (kể cả buộc đổi lần
  đầu), cài đặt máy chủ, chờ kết nối, trang chủ hai ứng dụng (khung, menu theo vai trò; nội dung ở Phase 4–6).
- StaffApp chạy `GradingScheduler` khi kết nối được, trạng thái hiện ở thanh dưới.
- Kiểm chứng: `src/test/java/.../ui/UiSmoke.java` (chương trình chạy thử, không phải JUnit) chạy trong Xvfb:
  đăng nhập / đăng xuất cả hai ứng dụng, thí sinh bị từ chối ở ứng dụng quản lý và ngược lại, buộc đổi mật khẩu,
  CSDL không chạy thì hiện hộp thoại lỗi rồi màn hình cài đặt máy chủ – không treo. `mvn javafx:run@staff`,
  `mvn javafx:run@client` khởi động được.

### Phase 6 (làm trước Phase 4, 5 theo thứ tự ưu tiên của kế hoạch)
- Màn hình: danh sách ca thi (3 nhóm, Làm mới), xác nhận quy chế, làm bài (lưới câu, 4 loại câu, đánh dấu, đồng hồ,
  dải mất kết nối, cảnh báo 5 phút), kết quả (chờ chấm 2 giây/lần tối đa 60 giây, điểm, số câu đúng, đáp án khi ca đóng).
- **"Xong khi" đã đạt** (chạy bằng `UiSmoke` trong Xvfb, hai tiến trình cùng một ca): `ui_sv1` rời cửa sổ 5 lần
  (cảnh báo 1/5 … 4/5, lần 5 bị kết thúc), `ui_sv2` để hết giờ tự nộp; CSDL có đúng hai bài, lý do `VIOLATION`
  (5 lần vi phạm) và `TIME_OUT` (giờ nộp đúng giờ đóng ca); máy chấm chấm cả hai. Bảng kết quả trên StaffApp kiểm tra
  lại ở Phase 5.
- **Chưa kiểm chứng được ở đây:** Xvfb không có trình quản lý cửa sổ nên không Alt+Tab được thật; `UiSmoke` giả lập
  bằng cách tắt toàn màn hình. Đường Alt+Tab / thu nhỏ cửa sổ thật mới được kiểm tra qua đọc mã – **nhóm cần chạy thử
  trên Windows** theo `docs/manual-test-checklist.md` (Phase 7).
- Kiểm thử: thêm `ClientModelTest` (6 ca) và 1 ca `overview`; tổng 110 ca xanh trên MySQL.

## Cách chạy kiểm thử với MySQL

```bash
mysql -u root -p -e "CREATE DATABASE oes_test CHARACTER SET utf8mb4"
mysql -u root -p oes_test < db/schema.sql
mysql -u root -p oes_test < db/security.sql
# tạo hai tệp properties trỏ tới oes_test: một bằng oes_staff, một bằng oes_client
OES_TEST_DB=test-staff.properties OES_TEST_DB_CLIENT=test-client.properties ./mvnw test
```

## Bảng truy vết FR

| FR | Nội dung ngắn | Trạng thái |
|---|---|---|
| FR-01 | Tạo, sửa, khóa, tìm tài khoản | Xong (service) |
| FR-02 | Gán vai trò | Xong (M4) |
| FR-03 | Đăng nhập, đăng xuất | Xong |
| FR-04 | Đổi mật khẩu, hồ sơ | Xong (service) |
| FR-05 | Chặn ngoài quyền | Xong (M4) |
| FR-06 | Nhập người dùng CSV | Xong (UserImportService) |
| FR-07 | Môn học, lớp học phần | Xong (SubjectService) |
| FR-08 | Thêm, sửa, xóa câu hỏi | Xong (service) |
| FR-09 | 4 loại câu hỏi | Xong (M4) |
| FR-10 | Thuộc tính câu hỏi | Xong (M4) |
| FR-11 | Lọc, tìm câu hỏi | Xong (M4) |
| FR-12 | Nhập câu hỏi từ tệp | Xong (M4) |
| FR-13 | Cấm xóa câu đã dùng | Xong (M4) |
| FR-14 | Tạo đề thủ công | Xong (M4) |
| FR-15 | Sinh đề theo ma trận | Xong (M4) |
| FR-16 | Điểm từng câu, tổng điểm | Xong (M4) |
| FR-17 | Ca thi giờ mở/đóng | Xong (M4) |
| FR-18 | Cấu hình trộn, xem đáp án | Xong (M4) |
| FR-19 | Ghi danh | Xong (theo lớp hoặc từng người) |
| FR-20 | Danh sách ca thi của thí sinh | Xong |
| FR-21 | Điều kiện vào thi | Xong (SessionWindow, StudentExamService) |
| FR-22 | Hiển thị đề, điều hướng, đánh dấu | Xong |
| FR-23 | Đếm ngược, cảnh báo 5 phút | Xong |
| FR-24 | Lưu bài | Xong (hàng đợi + thử lại) |
| FR-25 | Nộp bài có xác nhận | Xong |
| FR-26 | Tự nộp khi ca đóng | Xong (GradingWorker + GradingScheduler, GradingDaemon) |
| FR-27 | Xác nhận quy chế, toàn màn hình | Xong |
| FR-28 | Ghi nhận vi phạm | Xong |
| FR-29 | 5 lần thì kết thúc | Xong |
| FR-30 | Giảng viên xem vi phạm | Xong (service: listResults) |
| FR-31 | Chấm tự động | Xong (SubmissionGrader, GradingWorker) |
| FR-32 | Nhiều chiến lược chấm | Xong (M4) |
| FR-33 | Thí sinh xem kết quả, đáp án | Xong |
| FR-34 | Thống kê | Xong (M4) |
| FR-35 | Xuất CSV | Xong (M4) |

## Câu hỏi mở

Những chỗ phải đoán ý, cần nhóm xác nhận:

- Giảng viên có cho phép dùng `maven-shade-plugin` để đóng gói không? Nếu không, thay bằng hướng dẫn chạy `mvn javafx:run`.
- Có cần màn hình giám sát danh sách thí sinh đang thi không? (Hiện đã loại khỏi phạm vi ở M1.)
- Mọi máy thí sinh dùng chung tài khoản `oes_client`, nên ứng dụng thi bị sửa vẫn đọc/ghi được câu trả lời của thí sinh khác (không đọc được đáp án, không sửa được điểm). Chặn hẳn cần máy chủ ứng dụng ở giữa – ngoài phạm vi công nghệ được phép. Nhóm có chấp nhận giới hạn này không?
- `db/schema.sql` và `db/security.sql` mới chạy thử trên MySQL 8.0.40, **chưa chạy trên MariaDB** (kế hoạch yêu cầu cả hai). Nhóm cần chạy thử trên MariaDB trước buổi dựng phòng máy nếu máy chủ dùng MariaDB.
- Maven Wrapper (`mvnw`) là công cụ build, không phải thư viện của chương trình – coi như không vi phạm ràng buộc "không thêm thư viện".
