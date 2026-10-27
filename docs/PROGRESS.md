# Tiến độ

## Các phase

| Phase | Nội dung | Trạng thái |
|---|---|---|
| 0 | Dựng repo, pom JavaFX, tài liệu | Xong |
| 1 | Refactor SOLID, NFR-04 (Milestone 5) | Xong |
| 2 | Service phía giảng viên và quản trị | Chưa làm |
| 3 | Khung giao diện JavaFX | Chưa làm |
| 4 | Màn hình quản trị viên | Chưa làm |
| 5 | Màn hình giảng viên | Chưa làm |
| 6 | Ứng dụng thi | Chưa làm |
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
| FR-01 | Tạo, sửa, khóa, tìm tài khoản | Một phần (M4: tạo, khóa) |
| FR-02 | Gán vai trò | Xong (M4) |
| FR-03 | Đăng nhập, đăng xuất | Một phần (service hai ứng dụng) |
| FR-04 | Đổi mật khẩu, hồ sơ | Chưa |
| FR-05 | Chặn ngoài quyền | Xong (M4) |
| FR-06 | Nhập người dùng CSV | Chưa |
| FR-07 | Môn học, lớp học phần | Một phần (M4: thêm) |
| FR-08 | Thêm, sửa, xóa câu hỏi | Một phần (M4: thêm, xóa) |
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
| FR-19 | Ghi danh | Một phần (M4: chọn từng người) |
| FR-20 | Danh sách ca thi của thí sinh | Một phần (StudentExamService) |
| FR-21 | Điều kiện vào thi | Xong (SessionWindow, StudentExamService) |
| FR-22 | Hiển thị đề, điều hướng, đánh dấu | Một phần (ExamPaper, cờ flagged) |
| FR-23 | Đếm ngược, cảnh báo 5 phút | Chưa |
| FR-24 | Lưu bài | Một phần (StudentExamDao) |
| FR-25 | Nộp bài có xác nhận | Một phần (StudentExamService.submit) |
| FR-26 | Tự nộp khi ca đóng | Xong (GradingWorker; chạy định kỳ ở Phase 2) |
| FR-27 | Xác nhận quy chế, toàn màn hình | Chưa |
| FR-28 | Ghi nhận vi phạm | Một phần (ProctoringService, ViolationPolicy) |
| FR-29 | 5 lần thì kết thúc | Một phần (ProctoringService, ViolationPolicy) |
| FR-30 | Giảng viên xem vi phạm | Một phần (M4: service) |
| FR-31 | Chấm tự động | Xong (SubmissionGrader, GradingWorker) |
| FR-32 | Nhiều chiến lược chấm | Xong (M4) |
| FR-33 | Thí sinh xem kết quả, đáp án | Một phần (StudentExamService.review, v_review) |
| FR-34 | Thống kê | Xong (M4) |
| FR-35 | Xuất CSV | Xong (M4) |

## Câu hỏi mở

Những chỗ phải đoán ý, cần nhóm xác nhận:

- Giảng viên có cho phép dùng `maven-shade-plugin` để đóng gói không? Nếu không, thay bằng hướng dẫn chạy `mvn javafx:run`.
- Có cần màn hình giám sát danh sách thí sinh đang thi không? (Hiện đã loại khỏi phạm vi ở M1.)
- Mọi máy thí sinh dùng chung tài khoản `oes_client`, nên ứng dụng thi bị sửa vẫn đọc/ghi được câu trả lời của thí sinh khác (không đọc được đáp án, không sửa được điểm). Chặn hẳn cần máy chủ ứng dụng ở giữa – ngoài phạm vi công nghệ được phép. Nhóm có chấp nhận giới hạn này không?
- Maven Wrapper (`mvnw`) là công cụ build, không phải thư viện của chương trình – coi như không vi phạm ràng buộc "không thêm thư viện".
