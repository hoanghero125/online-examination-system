# Kịch bản demo (10 phút)

Không có tệp phân công trong tài liệu được giao nên bốn phần dưới để tên **Người 1 → Người 4**; nhóm tự điền tên.
Mỗi bước ghi rõ bấm gì và nói gì. Thời lượng ghi trong ngoặc.

## Chạy thử trước buổi demo (làm trên đúng máy sẽ demo)

- [ ] Dựng CSDL và chạy SeedTool theo `docs/RUNBOOK.md` **không sớm hơn 1 tiếng rưỡi** trước giờ demo (ca mẫu mở 2 giờ).
- [ ] Mở thử cả hai ứng dụng một lần trên hai máy (hoặc hai cửa sổ trên một máy).
- [ ] Thử Alt+Tab trên đúng laptop demo: phải hiện "Bạn đã rời cửa sổ làm bài (lần 1/5)". (Trên máy phát triển chỉ
      giả lập được bằng cách tắt toàn màn hình – đây là việc bắt buộc phải tự thử.)
- [ ] Mở sẵn một cửa sổ dòng lệnh `mysql -h <ip> -u oes_client -p oes` cho phần 4.
- [ ] Chuẩn bị tệp `sample-questions.txt` (trong mã nguồn: `src/main/resources/`) để nhập thử.

Máy A: StaffApp (giảng viên). Máy B: ExamClientApp (thí sinh `b24dcce098`). Máy C (nếu có): thí sinh `b24dcce007`.

---

## Người 1 – Giới thiệu và quản trị (2 phút)

| Bấm | Nói |
|---|---|
| (slide) | "Đề tài 16 – hệ thống thi trực tuyến chạy trong phòng máy: một MySQL chung, hai ứng dụng desktop JavaFX." |
| Máy A: đăng nhập `admin` / `admin123` | "Một màn hình đăng nhập dùng chung cho hai ứng dụng; mỗi ứng dụng chỉ nhận đúng vai trò của mình." |
| Máy B: thử đăng nhập `admin` vào ứng dụng thi | "Tài khoản quản trị bị từ chối ở ứng dụng thi – kiểm tra này nằm ở tầng service, không phải ở giao diện." |
| Người dùng → gõ `b24dcce0` → lọc Thí sinh | "Tìm, lọc, khóa tài khoản, nhập danh sách sinh viên từ CSV." |
| Môn học & lớp → chọn INT1332 | "Môn, lớp học phần, phân công giảng viên. Giảng viên chỉ quản lý được môn mình phụ trách." |
| Đăng xuất | |

## Người 2 – Giảng viên: câu hỏi và đề thi (2 phút 30)

| Bấm | Nói |
|---|---|
| Máy A: đăng nhập `gv.viet` / `gv123456` → Ngân hàng câu hỏi | "60 câu, 4 loại, 3 chương, 3 mức độ. Lọc theo chương, mức độ, loại." |
| Thêm câu hỏi → loại Một đáp án → tick Đúng cho 2 phương án → Lưu | "Lỗi hiện ngay cạnh ô phương án. Quy tắc 'đúng một đáp án đúng' nằm trong lớp SingleChoiceQuestion, controller không kiểm tra gì." |
| Sửa thành 1 đáp án đúng → Lưu | "Bốn loại câu là bốn lớp con của Question; QuestionFactory chọn lớp con theo loại (mẫu Factory)." |
| Đề thi → Tạo đề mới → thẻ Theo ma trận → nhập chương 3 Khó = 10 → Kiểm tra ma trận | "Ngân hàng không đủ câu thì báo thiếu ở đâu, trước khi sinh đề." |
| Sửa thành 2 → Kiểm tra → đặt tên → Tạo đề | "Câu nhiều đáp án chấm toàn phần hay theo tỷ lệ là cấu hình của đề – mẫu Strategy." |
| Chọn đề vừa tạo → Mở ca thi… → lớp INT1332-01 → giữ giờ mở là bây giờ, sửa giờ đóng thành 15 phút sau → Mở ca thi | "Ghi danh cả lớp. Mở ca xong đề bị khóa; muốn sửa phải tạo phiên bản mới." |

## Người 3 – Thí sinh làm bài (3 phút)

| Bấm | Nói |
|---|---|
| Máy B: đăng nhập `b24dcce098` / `sv123456` | "Từ đăng nhập tới làm bài chỉ ba thao tác." |
| Vào thi (ca vừa mở) → đọc quy chế → tick đồng ý → Bắt đầu làm bài | "Cửa sổ chuyển toàn màn hình; Esc và Alt+F4 bị chặn." |
| Trả lời vài câu, bấm số câu ở lưới, Đánh dấu xem lại | "Mỗi lần trả lời, nửa giây sau là lưu xuống CSDL. Rút mạng thì có dải vàng và tự lưu lại khi có mạng." |
| Alt+Tab ra ngoài rồi quay lại | "Rời cửa sổ là một lần vi phạm; các sự kiện cách nhau dưới 3 giây gộp làm một. Đủ 5 lần thì kết thúc bài." |
| (Máy C nếu có: Alt+Tab 5 lần) | "Bài này bị kết thúc ngay, vẫn được chấm phần đã làm." |
| Nộp bài → hộp thoại báo số câu chưa trả lời → Nộp bài | "Máy thí sinh **không chấm bài** – nó không có đáp án. Màn hình chờ 'Đang chấm bài…'." |

## Người 4 – Chấm bài, kết quả, bảo mật (2 phút 30)

| Bấm | Nói |
|---|---|
| Máy A: Ca thi → chọn ca → Xem kết quả → **Làm mới** | "Máy chấm chạy trên máy giảng viên 3 giây một lượt; Làm mới cho chấm ngay. Thí sinh bên kia đã thấy điểm." |
| Chỉ dòng tô đỏ | "Bài kết thúc do vi phạm được đánh dấu riêng (BR-06). Thống kê, phổ điểm, câu khó nhất lên đầu." |
| Xuất CSV… → mở bằng Excel | "Tiếng Việt hiển thị đúng nhờ ghi kèm BOM UTF-8." |
| Cửa sổ dòng lệnh `oes_client`: `SELECT * FROM answer_option;` | "Đây là tài khoản CSDL của máy thí sinh. Đọc bảng đáp án: **bị từ chối**." |
| `UPDATE submission SET total_score = 10 WHERE id = 1;` | "Tự sửa điểm: **bị từ chối**. Máy thí sinh chỉ đọc được các khung nhìn không có đáp án; trigger chặn ghi sau giờ đóng. Dù sửa mã ứng dụng thi cũng không lấy được đáp án (NFR-04)." |
| (slide) | "Milestone 5: tách Submission, ExamSession theo SRP; tách interface DAO theo người dùng (ISP); service chỉ biết interface (DIP). Chi tiết trong DESIGN-CHANGES." |

---

## Nếu bị hỏi

- "Sao không chấm trên máy thí sinh cho nhanh?" → máy thí sinh mà chấm được thì phải đọc được đáp án; đó là lỗi của bản
  M4, đã sửa ở M5 (`docs/DESIGN-CHANGES.md`, Phase 1).
- "Hai máy cùng bấm Vào thi?" → ràng buộc UNIQUE(session_id, student_id) trong CSDL; máy sau mở lại bài của máy trước.
- "Mất điện giữa giờ?" → câu trả lời đã lưu từng câu; đăng nhập lại (máy khác cũng được) là làm tiếp, giờ vẫn tính
  theo giờ đóng ca.
- "Giới hạn còn lại?" → mọi máy thí sinh dùng chung tài khoản `oes_client`, nên ứng dụng thi bị sửa vẫn đọc/ghi được
  câu trả lời của thí sinh khác (không đọc được đáp án, không sửa được điểm). Chặn hẳn cần máy chủ ứng dụng ở giữa –
  ngoài công nghệ được phép.
