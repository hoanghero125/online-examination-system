# Tiến độ

## Các phase

| Phase | Nội dung | Trạng thái |
|---|---|---|
| 0 | Dựng repo, pom JavaFX, tài liệu | Xong |
| 1 | Refactor SOLID, NFR-04 (Milestone 5) | Chưa làm |
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

## Bảng truy vết FR

| FR | Nội dung ngắn | Trạng thái |
|---|---|---|
| FR-01 | Tạo, sửa, khóa, tìm tài khoản | Một phần (M4: tạo, khóa) |
| FR-02 | Gán vai trò | Xong (M4) |
| FR-03 | Đăng nhập, đăng xuất | Một phần (M4: service) |
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
| FR-20 | Danh sách ca thi của thí sinh | Một phần (M4: service) |
| FR-21 | Điều kiện vào thi | Xong (M4) |
| FR-22 | Hiển thị đề, điều hướng, đánh dấu | Chưa |
| FR-23 | Đếm ngược, cảnh báo 5 phút | Chưa |
| FR-24 | Lưu bài | Một phần (M4: service) |
| FR-25 | Nộp bài có xác nhận | Một phần (M4: service) |
| FR-26 | Tự nộp khi ca đóng | Một phần (M4: chưa gọi định kỳ) |
| FR-27 | Xác nhận quy chế, toàn màn hình | Chưa |
| FR-28 | Ghi nhận vi phạm | Một phần (M4: service) |
| FR-29 | 5 lần thì kết thúc | Một phần (M4: service) |
| FR-30 | Giảng viên xem vi phạm | Một phần (M4: service) |
| FR-31 | Chấm tự động | Một phần (M4: chấm trên máy thí sinh) |
| FR-32 | Nhiều chiến lược chấm | Xong (M4) |
| FR-33 | Thí sinh xem kết quả, đáp án | Một phần (M4: điểm) |
| FR-34 | Thống kê | Xong (M4) |
| FR-35 | Xuất CSV | Xong (M4) |

## Câu hỏi mở

Những chỗ phải đoán ý, cần nhóm xác nhận:

- Giảng viên có cho phép dùng `maven-shade-plugin` để đóng gói không? Nếu không, thay bằng hướng dẫn chạy `mvn javafx:run`.
- Có cần màn hình giám sát danh sách thí sinh đang thi không? (Hiện đã loại khỏi phạm vi ở M1.)
- Maven Wrapper (`mvnw`) là công cụ build, không phải thư viện của chương trình – coi như không vi phạm ràng buộc "không thêm thư viện".
