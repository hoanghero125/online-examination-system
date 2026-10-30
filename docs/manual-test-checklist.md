# Danh sách kiểm thử tay (trước buổi bảo vệ)

[← README](../README.md) · [Phân công](PHAN-CONG.md) · [Tài liệu vấn đáp](vandap/) · [Chạy trên máy cá nhân](SETUP.md) · [Dựng phòng máy](RUNBOOK.md) · [Kịch bản demo](DEMO-SCRIPT.md)

Dùng dữ liệu của `SeedTool` (xem `docs/RUNBOOK.md`). Mỗi dòng: làm đúng các bước, so với kết quả mong đợi, đánh dấu ☑.
Ai chạy thì ghi tên và ngày ở cột cuối. Các dòng đánh dấu **[Windows]** là những việc **không chạy thử tự động được**
trên máy phát triển (không có trình quản lý cửa sổ thật) – đây là các dòng quan trọng nhất phải tự chạy trên máy phòng thi.

Tài khoản mẫu: `admin` / `admin123`, `gv.viet` / `gv123456`, `gv.lan` / `gv123456`, thí sinh `b24dcce001`… mật khẩu
`sv123456` (xem bảng SeedTool in ra).

## 1. Theo use case

| # | UC | Bước làm | Kết quả mong đợi | ☐ | Người chạy |
|---|---|---|---|---|---|
| 1 | UC-01 | StaffApp, đăng nhập `admin` → Người dùng → gõ `b24dcce00` vào ô tìm | Hiện các thí sinh có mã chứa chuỗi đó | ☐ | |
| 2 | UC-01 | Thêm → vai trò Thí sinh, nhập đủ → Lưu | Người dùng mới xuất hiện, trạng thái "Chờ đổi mật khẩu" | ☐ | |
| 3 | UC-01 | Chọn một thí sinh → Sửa → đổi họ tên, chọn lớp → Lưu | Bảng hiện họ tên và lớp mới | ☐ | |
| 4 | UC-01 | Chọn một thí sinh → Khóa / Mở khóa → Khóa; đăng nhập ExamClientApp bằng tài khoản đó | Báo "Tài khoản đã bị khóa…" | ☐ | |
| 5 | UC-01 | Chọn chính `admin` → Khóa | Báo "Không thể tự khóa tài khoản của chính mình." | ☐ | |
| 6 | UC-01 | Nhập thí sinh từ CSV với một tệp có 1 dòng sai mã lớp, 1 dòng trùng mã | Báo số dòng nhập được và từng dòng lỗi kèm số dòng | ☐ | |
| 7 | UC-01 | Đặt lại mật khẩu cho một thí sinh | Hiện mật khẩu tạm 8 ký tự; thí sinh đăng nhập bằng nó bị buộc đổi mật khẩu | ☐ | |
| 8 | UC-02 | Đăng nhập ExamClientApp bằng `gv.viet` | Báo "Tài khoản giảng viên và quản trị viên hãy dùng ứng dụng quản lý" | ☐ | |
| 9 | UC-02 | Đăng nhập StaffApp bằng một thí sinh | Báo "Tài khoản thí sinh hãy dùng ứng dụng thi" | ☐ | |
| 10 | UC-02 | Đăng nhập `gv.lan` (không phụ trách INT1332) → Ngân hàng câu hỏi | Ô Môn không có INT1332 | ☐ | |
| 11 | UC-03 | Đăng nhập sai mật khẩu | Báo "Sai tên đăng nhập hoặc mật khẩu…", không nói tên đăng nhập có tồn tại không | ☐ | |
| 12 | UC-03 | Đăng xuất ở cả hai ứng dụng | Hỏi xác nhận, về màn hình đăng nhập | ☐ | |
| 13 | UC-04 | Hồ sơ → sửa email sai dạng `abc` → Lưu | Báo email không đúng dạng | ☐ | |
| 14 | UC-04 | Đổi mật khẩu với mật khẩu mới 5 ký tự; rồi với nhập lại không khớp | Hai lỗi tương ứng; đổi đúng thì đăng nhập lại bằng mật khẩu mới được | ☐ | |
| 15 | UC-05 | Môn học & lớp → Thêm môn trùng mã INT1332 | Báo mã môn đã tồn tại | ☐ | |
| 16 | UC-05 | Xóa môn INT1332 | Báo không xóa được vì đã có câu hỏi/đề/lớp | ☐ | |
| 17 | UC-05 | Thêm lớp, sửa lớp, phân công rồi bỏ phân công giảng viên | Danh sách bên phải cập nhật; giảng viên đăng nhập lại thấy/không thấy môn | ☐ | |
| 18 | UC-06 | `gv.viet` → Ngân hàng câu hỏi → lọc chương 2, mức Trung bình, loại Nhiều đáp án | Chỉ còn các câu thỏa cả ba điều kiện, ô đếm "x / y câu" đúng | ☐ | |
| 19 | UC-06 | Thêm câu hỏi một đáp án, đánh dấu Đúng cho hai phương án → Lưu | Lỗi hiện ngay dưới ô phương án, không lưu | ☐ | |
| 20 | UC-06 | Thêm câu điền khuyết hợp lệ → Lưu; bấm đúp câu đó → sửa nội dung → Lưu | Câu mới xuất hiện; sửa xong thấy nội dung mới | ☐ | |
| 21 | UC-06 | Xóa một câu nằm trong đề đã mở ca thi | Báo câu đã dùng cho ca thi, không xóa được (FR-13) | ☐ | |
| 22 | UC-07 | Đăng nhập `gv.lan` → Ngân hàng câu hỏi, môn INT1313 (đang trống) → Nhập từ tệp `src/main/resources/sample-questions.txt` | Báo nhập được 18 câu, 2 dòng lỗi (dòng 22, 23); `gv.lan` không thấy môn INT1332 | ☐ | |
| 23 | UC-08 | Đề thi → Tạo đề mới → Chọn tay: thêm 3 câu, bấm đúp ô điểm sửa thành 2 | Tổng điểm cập nhật ngay; Tạo đề → đề mới trong danh sách | ☐ | |
| 24 | UC-08 | Tạo đề với tên trống hoặc chưa chọn câu nào | Báo lỗi, không tạo đề (BR-11) | ☐ | |
| 25 | UC-09 | Theo ma trận: chương 3 mức Khó nhập nhiều hơn số "có N" → Kiểm tra ma trận | Báo thiếu bao nhiêu câu ở chương, mức nào | ☐ | |
| 26 | UC-09 | Ma trận hợp lệ → Tạo đề | Đề có đúng số câu theo ma trận | ☐ | |
| 27 | UC-10 | Chọn đề → Mở ca thi với giờ đóng trước giờ mở | Báo "Giờ đóng ca thi phải sau giờ mở." | ☐ | |
| 28 | UC-10 | Mở ca thi hợp lệ, rồi quay lại danh sách đề | Đề ghi "Đã khóa"; Tạo phiên bản mới → có phiên bản 2 chưa khóa (BR-09) | ☐ | |
| 29 | UC-11 | Mở ca thi: chọn lớp → tất cả thí sinh lớp được đánh dấu sẵn; bỏ chọn 1 người | "Đã chọn n / m thí sinh" đúng; thí sinh bị bỏ không thấy ca thi | ☐ | |
| 30 | UC-12 | ExamClientApp, thí sinh có ca → danh sách ca thi | Ca chia 3 nhóm Đang mở / Sắp diễn ra / Đã kết thúc; Làm mới hoạt động | ☐ | |
| 31 | UC-13 | Vào thi → màn hình quy chế | Có tên đề, số câu, tổng điểm, giờ đóng, thời gian còn lại, câu quy chế; nút Bắt đầu chỉ bật khi tick đồng ý | ☐ | |
| 32 | UC-13 | Vào thi lại sau khi đã nộp | Không có nút Vào thi, chỉ có "Xem kết quả" | ☐ | |
| 33 | UC-14 | Làm bài: trả lời đủ 4 loại câu, đánh dấu xem lại, bấm số câu ở lưới bên trái | Lưới đổi màu đúng (đã trả lời / đánh dấu / đang xem), nhảy đúng câu | ☐ | |
| 34 | UC-14 | Kiểm tra trộn đề: hai thí sinh cùng ca | Thứ tự câu và phương án khác nhau | ☐ | |
| 35 | UC-15 | **[Windows]** Trong lúc thi bấm Alt+Tab sang cửa sổ khác | Hộp thoại "Bạn đã rời cửa sổ làm bài (lần 1/5)"; bấm Quay lại → toàn màn hình | ☐ | |
| 36 | UC-15 | **[Windows]** Alt+Tab hai lần thật nhanh (dưới 3 giây) | Chỉ tính 1 lần | ☐ | |
| 37 | UC-15 | **[Windows]** Bấm phím Windows / thu nhỏ cửa sổ | Tính 1 lần vi phạm | ☐ | |
| 38 | UC-15 | **[Windows]** Bấm Esc và Alt+F4 trong lúc thi | Không thoát toàn màn hình, không đóng được cửa sổ | ☐ | |
| 39 | UC-15 | **[Windows]** Rời cửa sổ 5 lần (cách nhau hơn 3 giây) | Lần thứ 5 kết thúc bài ngay, màn hình kết quả có dải đỏ "Bài thi bị kết thúc do vi phạm quy chế" | ☐ | |
| 40 | UC-16 | Bấm Nộp bài | Hộp thoại nêu số câu chưa trả lời và số câu đánh dấu; Hủy thì quay lại làm tiếp | ☐ | |
| 41 | UC-16 | Nộp bài khi StaffApp đang chạy | "Đang chấm bài…" rồi vài giây sau hiện điểm, số câu đúng | ☐ | |
| 42 | UC-16 | Để hết giờ (tạo ca 3 phút) | Đồng hồ đỏ khi còn dưới 5 phút; về 0 thì tự nộp, lý do "Hết giờ, tự động nộp" | ☐ | |
| 43 | UC-17 | StaffApp → Ca thi → chọn ca → Xem kết quả | Bảng điểm, dòng vi phạm tô đỏ, thống kê cao/thấp/trung bình, phổ điểm, tỷ lệ đúng từng câu | ☐ | |
| 44 | UC-17 | Thí sinh xem kết quả khi ca chưa đóng / sau khi ca đóng (ca cho xem đáp án) | Trước: "Đáp án sẽ hiện sau khi ca thi kết thúc"; sau: từng câu có đáp án đúng | ☐ | |
| 45 | UC-18 | Kết quả ca thi → Xuất CSV → mở bằng Excel | Tên tiếng Việt hiển thị đúng; cột kết thúc do vi phạm ghi "co"/"khong" | ☐ | |
| 46 | NFR-05 | Trên **máy chạy StaffApp**: Quản trị viên → Nhật ký → lọc `GRADED_VIOLATION` | Có dòng ghi mã sinh viên của bài bị kết thúc do vi phạm (dòng `TERMINATED_BY_VIOLATION` thì nằm trong `oes-audit.log` của máy thí sinh) | ☐ | |

## 2. Tình huống hỏng hóc (Phase 7)

| # | Tình huống | Bước làm | Kết quả mong đợi | Tự động? | ☐ |
|---|---|---|---|---|---|
| 47 | Rút dây mạng giữa giờ thi | Đang làm bài, rút cáp mạng (hoặc tắt Wi-Fi), trả lời thêm 2 câu, cắm lại | Dải vàng "Mất kết nối…", vẫn làm bài được; có mạng lại thì dải vàng tắt, 2 câu đã có trong CSDL | Có (`UiSmoke … reconnect`, giả lập bằng dừng MySQL) | ☐ |
| 48 | Tắt hẳn ứng dụng rồi mở lại | Đang làm bài, tắt máy / kill tiến trình, mở lại, đăng nhập | Nút "Tiếp tục làm bài", màn hình quy chế ghi số lần đã vi phạm, các câu đã trả lời còn nguyên | Có (`resume1`, `resume2`) | ☐ |
| 49 | Hai máy cùng một tài khoản thí sinh | Máy A và B cùng vào thi; A nộp bài | B tự chuyển sang màn hình kết quả trong vòng 60 giây (hoặc ngay khi B lưu câu trả lời) | Có (`second` + `full`) | ☐ |
| 50 | StaffApp không chạy khi thí sinh nộp bài | Tắt StaffApp và GradingDaemon; thí sinh nộp | "Đang chấm bài…" quá 60 giây thì báo "Kết quả sẽ có khi giảng viên mở máy chấm"; mở StaffApp → bấm Kiểm tra lại → có điểm | Một phần | ☐ |
| 51 | **[Windows]** Đồng hồ máy thí sinh bị chỉnh sai | Chỉnh giờ Windows sai 1 tiếng trước khi mở ứng dụng thi, và đổi tiếp trong lúc thi | Đồng hồ đếm ngược vẫn đúng theo giờ máy chủ; không vào được ca chưa mở | Không (cần quyền quản trị máy) | ☐ |
| 52 | Máy chủ CSDL không chạy khi mở ứng dụng | Tắt MySQL rồi mở ExamClientApp | Sau vài giây báo "Không kết nối được tới máy chủ dữ liệu…", hiện màn hình nhập địa chỉ máy chủ, không treo | Có (`deaddb`) | ☐ |

## 3. Chưa làm (ghi để trả lời nếu bị hỏi)

- UC-08 luồng 5a "đổi một câu trong bản xem trước sang câu khác cùng chương, cùng mức độ": chưa có nút riêng.
  Cách thay thế: ở thẻ Chọn tay, bỏ câu đó rồi thêm câu khác.
- UC-13 luồng 3a "không vào được chế độ toàn màn hình thì không cho bắt đầu": chưa kiểm tra riêng; JavaFX trên
  Windows/macOS/Linux đều hỗ trợ toàn màn hình nên trường hợp này gần như không xảy ra.
