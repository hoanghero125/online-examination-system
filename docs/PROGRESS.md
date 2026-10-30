# Tiến độ

[README](../README.md) · [Phân công](PHAN-CONG.md) · [Tài liệu vấn đáp](vandap/) · [Chạy trên máy cá nhân](SETUP.md) · [Dựng phòng máy](RUNBOOK.md) · [Kịch bản demo](DEMO-SCRIPT.md)

## Các phase

| Phase | Nội dung | Trạng thái |
|---|---|---|
| 0 | Dựng repo, pom JavaFX, tài liệu | Xong |
| 1 | Refactor SOLID, NFR-04 (Milestone 5) | Xong |
| 2 | Service phía giảng viên và quản trị | Xong |
| 3 | Khung giao diện JavaFX | Xong |
| 4 | Màn hình quản trị viên | Xong |
| 5 | Màn hình giảng viên | Xong |
| 6 | Ứng dụng thi | Xong |
| 7 | Kiểm thử và gia cố | Xong |
| 8 | Đóng gói, demo | Xong |

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

### Phase 5, 4
- Giảng viên: ngân hàng câu hỏi (lọc, thêm, sửa, xóa, nhập tệp), biểu mẫu câu hỏi (đổi theo loại, lỗi cạnh ô nhập),
  danh sách đề (phiên bản mới cho đề đã khóa), tạo đề (chọn tay có sửa điểm từng câu / theo ma trận có nút kiểm tra),
  mở ca thi (ghi danh theo lớp hoặc từng người), danh sách ca thi, kết quả ca thi (bảng điểm tô màu bài vi phạm, thống kê,
  phổ điểm, tỷ lệ đúng từng câu, xuất CSV, "Làm mới" chấm ngay), hồ sơ.
- Quản trị viên: người dùng (tìm, lọc, thêm, sửa, khóa, đặt lại mật khẩu, nhập CSV), môn học & lớp & phân công, nhật ký.
- `UiSmoke instructor` mở kết quả của ca chạy thử ở Phase 6: đúng 2 bài, 1 bài "Kết thúc do vi phạm quy chế" (tô đỏ),
  1 bài "Hết giờ, tự động nộp" – hoàn tất tiêu chí "StaffApp thấy đúng hai bài với đúng lý do kết thúc".
- Kiểm thử: thêm `AdminAndResultsTest` (5 ca), tổng 115 ca xanh trên MySQL.

### Phase 7
- Mỗi mã lỗi có ít nhất một ca kiểm thử (`ErrorCodeTest`, đối chiếu bằng grep mã lỗi trong `src/main` và `src/test`).
  Chưa kiểm thử: `CONFIG_WRITE` (cần thư mục không có quyền ghi), `EXAM_ZERO_POINT` (kiểm tra phòng hờ, không xảy ra
  được vì mỗi câu đều có điểm > 0). Tổng 121 ca xanh trên MySQL.
- Kiểm thử tải 60 thí sinh × 3 phút (`LoadSimulator`, tài khoản oes_client): lưu câu trả lời p95 = 31 ms, max = 114 ms,
  0 lỗi; chấm 60 bài trong 2,2 giây. Chi tiết và lệnh chạy lại trong phòng máy: `docs/load-test.md`.
- Tình huống hỏng hóc chạy tự động bằng `UiSmoke`: mất kết nối giữa giờ (dừng MySQL bằng SIGSTOP), tắt hẳn ứng dụng
  rồi mở lại, hai máy cùng tài khoản, CSDL không chạy khi mở ứng dụng. Đồng hồ máy trạm sai: chỉ kiểm thử đơn vị
  (`ClientModelTest`) – cần chạy tay trên Windows.
- Sửa sau khi rà soát: `DuplicateKeyException` có câu báo riêng (trước đây báo nhầm "không kết nối được");
  hết giờ chỉ gọi tự nộp một lần; máy thứ hai của cùng tài khoản tự phát hiện bài đã nộp (đồng bộ 60 giây/lần).
- Rà lại thông điệp lỗi: các câu chỉ nêu lỗi được thêm cách xử lý (đề trống, ca chưa có thí sinh, giờ đóng ở quá khứ,
  chưa vào ca thi, sai mật khẩu, chưa được phân công môn).
- `docs/manual-test-checklist.md`: 46 dòng theo UC-01 → UC-18 + 6 tình huống hỏng hóc; các dòng **[Windows]** là
  những việc không chạy thử tự động được.

### Phase 8
- `mvn package` tạo `dist/oes-staff.jar` và `dist/oes-client.jar` (maven-shade-plugin, `StaffLauncher` / `ClientLauncher`
  làm lớp main, gộp `META-INF/services` để driver MySQL tự đăng ký). Đã chạy thử `java -jar` cả hai trong Xvfb:
  StaffApp kết nối MySQL tới màn hình đăng nhập; ứng dụng thi chưa có cấu hình thì hiện màn hình nhập máy chủ.
- Script `dist/run-staff`, `run-client`, `run-grading-daemon`, `run-seed` (`.bat` và `.sh`), tự chuyển về thư mục
  chứa jar để tìm tệp cấu hình và ghi nhật ký đúng chỗ.
- `tools.SeedTool` + `seed-questions.txt` (60 câu OOP, đủ 4 loại × 3 chương × 3 mức độ, nạp qua chức năng nhập tệp):
  1 quản trị, 2 giảng viên, 2 môn, 2 lớp, 60 thí sinh + 4 thành viên nhóm (tên và mã lấy từ trang bìa báo cáo),
  đề 20 câu sinh theo ma trận, ca thi mở 2 giờ. Đã chạy qua `dist/run-seed.sh` trên CSDL mới; chạy lần hai thì từ chối.
  `SeedToolTest` kiểm tra tệp câu hỏi không có dòng lỗi và thí sinh vào thi được ngay.
- `docs/RUNBOOK.md` (dựng phòng máy, bảng sự cố), `docs/DEMO-SCRIPT.md` (10 phút, 4 người – nhóm tự điền tên vì không
  có tệp phân công), README viết lại.

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
| FR-01 | Tạo, sửa, khóa, tìm tài khoản | Xong |
| FR-02 | Gán vai trò | Xong (M4) |
| FR-03 | Đăng nhập, đăng xuất | Xong |
| FR-04 | Đổi mật khẩu, hồ sơ | Xong |
| FR-05 | Chặn ngoài quyền | Xong (M4) |
| FR-06 | Nhập người dùng CSV | Xong |
| FR-07 | Môn học, lớp học phần | Xong |
| FR-08 | Thêm, sửa, xóa câu hỏi | Xong |
| FR-09 | 4 loại câu hỏi | Xong (M4) |
| FR-10 | Thuộc tính câu hỏi | Xong (M4) |
| FR-11 | Lọc, tìm câu hỏi | Xong |
| FR-12 | Nhập câu hỏi từ tệp | Xong |
| FR-13 | Cấm xóa câu đã dùng | Xong |
| FR-14 | Tạo đề thủ công | Xong |
| FR-15 | Sinh đề theo ma trận | Xong |
| FR-16 | Điểm từng câu, tổng điểm | Xong |
| FR-17 | Ca thi giờ mở/đóng | Xong |
| FR-18 | Cấu hình trộn, xem đáp án | Xong |
| FR-19 | Ghi danh | Xong |
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
| FR-30 | Giảng viên xem vi phạm | Xong |
| FR-31 | Chấm tự động | Xong (SubmissionGrader, GradingWorker) |
| FR-32 | Nhiều chiến lược chấm | Xong (M4) |
| FR-33 | Thí sinh xem kết quả, đáp án | Xong |
| FR-34 | Thống kê | Xong |
| FR-35 | Xuất CSV | Xong |

## Câu hỏi mở

Những chỗ phải đoán ý hoặc chưa kiểm chứng được, cần nhóm xác nhận:

- **"Format của một project IntelliJ"** được hiểu là: dự án Maven chuẩn (IntelliJ mở trực tiếp), thêm `.editorconfig`
  và các cấu hình chạy dùng chung trong `.idea/runConfigurations/`; phần còn lại của `.idea/` không đưa lên Git.
  Nếu ý là dạng dự án IntelliJ không dùng Maven (`.iml`, thư mục `out/`) thì cần làm lại cách build.
- **Alt+Tab, thu nhỏ cửa sổ, chỉnh đồng hồ máy trạm** chỉ được kiểm tra bằng giả lập (máy phát triển không có trình
  quản lý cửa sổ). Phải tự chạy các dòng **[Windows]** trong `docs/manual-test-checklist.md` trên máy phòng thi.
- Giảng viên có cho phép dùng `maven-shade-plugin` để đóng gói không? Nếu không, thay bằng hướng dẫn chạy `mvn javafx:run`.
- Có cần màn hình giám sát danh sách thí sinh đang thi không? (Hiện đã loại khỏi phạm vi ở M1.)
- Mọi máy thí sinh dùng chung tài khoản `oes_client`, nên ứng dụng thi bị sửa vẫn đọc/ghi được câu trả lời của thí sinh khác (không đọc được đáp án, không sửa được điểm). Chặn hẳn cần máy chủ ứng dụng ở giữa – ngoài phạm vi công nghệ được phép. Nhóm có chấp nhận giới hạn này không?
- `db/schema.sql` và `db/security.sql` mới chạy thử trên MySQL 8.0.40, **chưa chạy trên MariaDB** (kế hoạch yêu cầu cả hai). Nhóm cần chạy thử trên MariaDB trước buổi dựng phòng máy nếu máy chủ dùng MariaDB.
- Hai luồng ngoại lệ trong đặc tả M1 chưa làm: UC-08 5a (đổi một câu trong bản xem trước) và UC-13 3a (không vào được toàn màn hình thì không cho thi). Xem cuối `docs/manual-test-checklist.md`.
- Maven Wrapper (`mvnw`) là công cụ build, không phải thư viện của chương trình – coi như không vi phạm ràng buộc "không thêm thư viện".
