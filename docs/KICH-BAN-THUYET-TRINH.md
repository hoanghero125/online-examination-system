# Kịch bản thuyết trình sản phẩm (1 người demo, 1 người thuyết trình)

[README](../README.md) · [Phân công](PHAN-CONG.md) · [Tài liệu vấn đáp](vandap/) · [Chạy trên máy cá nhân](SETUP.md) · [Dựng phòng máy](RUNBOOK.md) · [Kịch bản demo](DEMO-SCRIPT.md)

Dùng cho buổi thuyết trình sản phẩm trước giảng viên: **một người chỉ thao tác máy (gọi là DM)** và **một người nói từ
đầu tới cuối (gọi là TT)**, cả buổi trên **một laptop** nối máy chiếu. Trình tự: **slide → demo → chốt → hỏi đáp**.

**Thời lượng: 10 phút cho cả thuyết trình và demo.** Kịch bản dự tính **9 phút 30**, chừa 30 giây cho máy chậm. Lố giờ
thì dùng [đường tắt](#mốc-giờ-và-đường-tắt) và bỏ các dòng **[cắt]**. Kịch bản demo chia theo bốn phần để vấn đáp nằm
ở [`DEMO-SCRIPT.md`](DEMO-SCRIPT.md).

Mục lục: [Sáu điểm sáng](#sáu-điểm-sáng--phải-thuộc) · [Phân vai](#phân-vai-và-quy-ước) ·
[Mốc giờ](#mốc-giờ-và-đường-tắt) · [Chuẩn bị](#chuẩn-bị) · [A. Thuyết trình](#a-thuyết-trình-slide-19-3-phút-20) ·
[B. Demo](#b-demo-5-phút-40) · [C. Chốt](#c-chốt-và-mời-hỏi-đáp-slide-10-30-giây) · [Slide](#bộ-slide) ·
[Khi hỏi đáp](#cho-xem-thêm-khi-hỏi-đáp) · [Sự cố](#sự-cố-và-cách-xử-lý) · [Câu hỏi hay gặp](#câu-hỏi-hay-gặp)

---

## Sáu điểm sáng – phải thuộc

Phần slide **nêu** sáu điểm này, phần demo **chứng minh** chúng trên màn hình. Đúng lúc màn hình hiện bằng chứng, TT
nói nguyên văn **câu chốt**. Trong kịch bản, chỗ chứng minh được đánh dấu **[ĐIỂM SÁNG n]**.

| # | Điểm sáng | Nêu ở | Chứng minh ở | Câu chốt |
|:-:|---|---|---|---|
| 1 | **Máy thí sinh "mù đáp án"**: bảo mật nằm ở CSDL, không chỉ ở giao diện | Slide 3, 7 | B4: gõ SQL bằng tài khoản của máy thí sinh, đọc đáp án và tự sửa điểm đều bị MySQL từ chối | "Kể cả sửa mã ứng dụng thi cũng không lấy được đáp án: người từ chối là MySQL, không phải giao diện." |
| 2 | **Hướng đối tượng thật**: luật nghiệp vụ nằm trong lớp, giao diện không kiểm tra gì | Slide 5–6 | B2: câu một đáp án đánh dấu 2 đáp án đúng bị lớp `SingleChoiceQuestion` từ chối | "Controller không kiểm tra gì cả. Luật sống trong lớp con của `Question`." |
| 3 | **Phân quyền ở tầng service**, không phải ẩn nút | Slide 7 | B1: tài khoản quản trị đăng nhập ứng dụng thi bị từ chối | "Ẩn nút chưa phải là phân quyền. Mọi kiểm tra quyền nằm ở service." |
| 4 | **Chống gian lận, giờ chuẩn từ máy chủ** | Slide 7 | B3: toàn màn hình, rời cửa sổ bị ghi vi phạm "lần 1/5" | "Chỉnh đồng hồ máy thí sinh cũng không được thêm phút nào: giờ thi lấy từ máy chủ." |
| 5 | **Tách máy chấm khỏi máy thí sinh**, có điểm gần như ngay | Slide 3 | B3: nộp bài, vài giây sau có điểm | "Máy thí sinh không chấm bài, vì nó không có đáp án để chấm." |
| 6 | **Kiểm chứng bằng số**: 135 kiểm thử, MySQL và MariaDB, tải 60 thí sinh | Slide 8 | B4: hai lệnh SQL bị từ chối chính là việc 9 kiểm thử tự động làm mỗi lần chạy | "9 kiểm thử dùng chính tài khoản máy thí sinh để tấn công CSDL." |

Giảng viên môn OOP chấm đúng những chỗ này, nên slide 5–6 phải nói rõ: **bốn tính chất OOP** gắn với lớp cụ thể, **ba mẫu
thiết kế** (Factory, Strategy, Template Method) và **SOLID** (SRP, ISP, DIP). Demo nhắc lại khi tới chỗ tương ứng.

---

## Phân vai và quy ước

- **DM** ngồi máy và điều khiển mọi thứ trên laptop, kể cả chuyển slide. Làm đúng thứ tự cột "DM bấm", không nói.
- **TT** đứng cạnh màn chiếu, nói theo cột "TT nói", chỉ tay vào màn hình khi tới cột "Màn hình phải hiện".
- **Nói trong lúc DM bấm**: 10 phút không có chỗ cho im lặng chờ máy. TT nói câu của bước tiếp theo ngay khi DM bắt
  đầu bấm, riêng **câu chốt** thì đợi màn hình hiện bằng chứng rồi mới nói.
- **Câu hiệu chuyển phần**: TT nói "**Mời thầy và các bạn cùng xem**" thì DM rời slide sang ứng dụng; "**Quay lại
  slide**" thì DM về slide.
- **"Alt+Tab sang X"** nghĩa là: giữ Alt, bấm Tab tới đúng cửa sổ X rồi mới thả. Alt+Tab nhảy theo thứ tự dùng gần
  nhất, bấm một lần có thể ra nhầm slide hoặc ứng dụng kia.
- **Trong lúc thi (B3), DM không chạm vào cửa sổ nào khác**, trừ đúng một lần Alt+Tab đã ghi trong kịch bản.

## Mốc giờ và đường tắt

DM để đồng hồ bấm giờ trên điện thoại cạnh laptop. Hai mốc:

| Mốc | Đúng tiến độ | Nếu lố |
|---|---|---|
| Hết slide 9 | **3:20** | Quá 3:40: dùng **đường tắt B2** |
| Bắt đầu B4 | **7:40** | Quá 8:00: bỏ mọi dòng **[cắt]** còn lại, đi thẳng tới hai lệnh SQL |

**Đường tắt B2** (tiết kiệm khoảng 1 phút): sau khi Lưu câu hỏi thì dừng phần giảng viên, **không tạo đề, không mở ca**.
Thí sinh vào ca mẫu "Kiểm tra giữa kỳ OOP" đã mở sẵn từ lúc dựng CSDL; B4 chọn đúng ca đó. TT nói thay: "Giảng viên còn
tạo đề theo ma trận chương và mức độ, và mở ca thi cho cả lớp; chúng em dùng ca đã mở sẵn."

**Không bao giờ cắt B4**: đó là phần chứng minh mạnh nhất.

---

## Chuẩn bị

### Hôm trước (bắt buộc)

- [ ] Chạy trọn kịch bản **ba lần** trên đúng laptop và máy chiếu, bấm giờ; lần cuối phải dưới 9 phút 30.
- [ ] **Thử Alt+Tab khi đang thi**: phải hiện hộp thoại "Cảnh báo vi phạm quy chế". Thử Esc và Alt+F4: không thoát được.
      Cơ chế này mới kiểm tra bằng giả lập (xem `PROGRESS.md`, "Câu hỏi mở"), nên **phải tự thử trên Windows**.
- [ ] Chạy lại đủ kiểm thử, phải ra đúng dòng in trên slide 8: `Tests run: 135, Failures: 0, Errors: 0, Skipped: 0`. MySQL
      trong Docker: `scripts\test.bat --db "--port=<cổng container>" "--root-password=oes_root"` (cổng xem bằng
      `docker port oes-mysql`). MySQL cài trên Windows: `scripts\test.bat --db`, lệnh sẽ hỏi mật khẩu root. Gọi trần
      `--db` trên máy có cả hai thì lệnh nối vào cổng 3306, có thể nhầm sang MySQL cài sẵn.
- [ ] Mở [bộ slide](#bộ-slide) trên đúng máy chiếu: chữ phải đọc được từ cuối phòng. Chép thêm bản PDF ra USB phòng
      khi laptop hỏng.
- [ ] Tăng tỷ lệ hiển thị Windows lên 125–150% (Settings → Display → Scale) để cuối phòng đọc được chữ.

### 30 phút trước

- [ ] Bật Docker Desktop, chạy `scripts\setup-docker.bat --reset`. Lệnh này dựng lại CSDL sạch: tài khoản mẫu, 60 câu
      hỏi và ca mẫu "Kiểm tra giữa kỳ OOP" mở 2 giờ (dùng cho đường tắt). Máy dùng MySQL cài sẵn thì chạy
      `scripts\setup.bat --reset`.
- [ ] Bật **Không làm phiền** (Settings → System → Notifications → Do not disturb). Thoát Zalo, Messenger, Teams,
      OneDrive. **Một thông báo bật lên giữa giờ thi được tính là một lần vi phạm.**
- [ ] Máy chiếu để chế độ **Duplicate** (Win+P → Duplicate). Ở chế độ Extend, cửa sổ thi toàn màn hình có thể nhảy
      sang màn hình kia.
- [ ] Cắm sạc, tắt chế độ ngủ, hoãn cập nhật Windows.
- [ ] Chạy `scripts\start-demo.bat` để mở hai ứng dụng, để cả hai ở màn hình đăng nhập.
- [ ] Mở một cửa sổ dòng lệnh, phóng chữ to (Ctrl + lăn chuột), **đăng nhập sẵn** MySQL bằng tài khoản máy thí sinh:
  - MySQL trong Docker: `docker exec -it oes-mysql mysql -uoes_client -p oes`, mật khẩu `doi_mat_khau_client`.
  - MySQL cài trên Windows: `"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u oes_client -p oes`.
- [ ] Mở Notepad chứa sẵn: nội dung câu hỏi và bốn phương án ở B2, tên đề `Demo thuyết trình`, hai lệnh SQL ở B4.
      Chép dán, không gõ trước khán giả.
- [ ] Sắp thứ tự cửa sổ: slide (trên cùng, chế độ trình chiếu), ExamClientApp, StaffApp, dòng lệnh MySQL, Notepad.
- [ ] Tuyệt đối không dùng `b24dcce098` bấm "Vào thi" trước buổi: thí sinh này để dành cho B3.

| Vai trò | Tên đăng nhập | Mật khẩu | Dùng ở |
|---|---|---|---|
| Quản trị viên | `admin` | `admin123` | B1 |
| Giảng viên | `gv.viet` | `gv123456` | B2, B4 |
| Thí sinh | `b24dcce098` | `sv123456` | B3 |

---

## Dòng thời gian

| Phút | Phần | Màn hình | Điểm sáng |
|---|---|---|---|
| 0:00 | A1. Mở đầu | Slide 1–2 | |
| 0:30 | A2. Kiến trúc | Slide 3–4 | nêu 1, 5 |
| 1:20 | A3. Thiết kế hướng đối tượng | Slide 5–6 | nêu 2 |
| 2:20 | A4. Bảo mật và kiểm thử | Slide 7–8 | nêu 3, 4, 6 |
| 3:10 | A5. Chuyển sang demo | Slide 9 | |
| 3:20 | B1. Phân quyền | ExamClientApp | **3** |
| 3:50 | B2. Giảng viên | StaffApp | **2** |
| 5:50 | B3. Thí sinh làm bài | ExamClientApp | **4**, **5** |
| 7:40 | B4. Kết quả và bảo mật | StaffApp, dòng lệnh MySQL | **1**, **6** |
| 9:00 | C. Chốt | Slide 10 | |
| 9:30 | (dự phòng 30 giây) | | |
| 10:00 | Hỏi đáp | Slide 10, ứng dụng vẫn mở | |

---

## A. Thuyết trình (slide 1–9, 3 phút 20)

### A1 · Mở đầu (slide 1–2, 30 giây)

| DM bấm | TT nói |
|---|---|
| Slide 1 | "Kính chào thầy và các bạn. Nhóm 2 xin trình bày đề tài 16: hệ thống thi trắc nghiệm trực tuyến cho phòng máy." |
| Slide 2 | "Thi trong phòng máy có ba nỗi lo: lộ đáp án, gian lận, và chấm điểm chậm. Hệ thống của chúng em giải cả ba, và phần demo cuối buổi sẽ chứng minh từng điều." |

### A2 · Kiến trúc (slide 3–4, 50 giây)

| DM bấm | TT nói |
|---|---|
| Slide 3 (sơ đồ triển khai) | "Một MySQL chung và hai ứng dụng JavaFX. Ứng dụng quản lý dành cho giảng viên, đồng thời là **máy chấm**. Ứng dụng thi trên máy thí sinh **không chấm bài**, vì nó không có đáp án. Mấu chốt: hai ứng dụng dùng **hai tài khoản CSDL khác nhau**; tài khoản của máy thí sinh chỉ đọc được dữ liệu đã lọc bỏ đáp án." |
| Slide 4 (ba tầng) | "Mỗi ứng dụng ba tầng: giao diện, service, DAO. Service chỉ biết interface DAO; mỗi interface có hai cài đặt, JDBC và bộ nhớ, nên nghiệp vụ chạy được cả khi không có MySQL – nguyên lý đảo ngược phụ thuộc." |

### A3 · Thiết kế hướng đối tượng (slide 5–6, 1 phút)

| DM bấm | TT nói |
|---|---|
| Slide 5 (sơ đồ lớp `Question`, `User`) | "Bốn tính chất OOP đều gắn với lớp cụ thể. **Trừu tượng**: `Question` và `User` là lớp trừu tượng. **Kế thừa**: bốn loại câu hỏi là bốn lớp con của `Question`, ba vai trò là ba lớp con của `User`. **Đa hình**: mỗi loại câu tự chấm chính nó qua `isCorrect`. **Đóng gói**: dữ liệu được kiểm tra ngay trong constructor, nên không bao giờ tồn tại một câu hỏi sai." |
| Slide 6 (mẫu thiết kế, SOLID) | "Ba mẫu thiết kế: **Factory** tạo câu hỏi, **Strategy** chọn cách chấm, **Template Method** cho khung hai ứng dụng. Và SOLID: tách lớp theo SRP, tách DAO theo ISP, service chỉ biết interface theo DIP." |

### A4 · Bảo mật và kiểm thử (slide 7–8, 50 giây)

| DM bấm | TT nói |
|---|---|
| Slide 7 (ba lớp bảo vệ) | "Bảo mật ba lớp: tài khoản CSDL giới hạn quyền, trigger chặn ghi khi bài đã nộp hoặc hết giờ, và kiểm tra quyền ở service. Giờ thi lấy từ máy chủ MySQL. Rời cửa sổ làm bài là vi phạm, đủ năm lần thì khóa bài." |
| Slide 8 (kiểm thử) | "135 kiểm thử tự động, trong đó **9 kiểm thử dùng chính tài khoản máy thí sinh để tấn công CSDL**, chạy được trên cả MySQL và MariaDB. Thử tải 60 thí sinh cùng lúc: lưu một câu trả lời chỉ khoảng 31 mili giây." |

### A5 · Chuyển sang demo (slide 9, 10 giây)

| DM bấm | TT nói |
|---|---|
| Slide 9 ("Demo") | "Bây giờ xin chứng minh trực tiếp. **Mời thầy và các bạn cùng xem.**" |

---

## B. Demo (5 phút 40)

### B1 · Phân quyền (30 giây) – [ĐIỂM SÁNG 3]

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| Alt+Tab sang ExamClientApp → đăng nhập `admin` / `admin123` | "Đây là ứng dụng thi. Thử đăng nhập bằng tài khoản **quản trị viên**." | "Tài khoản giảng viên và quản trị viên hãy dùng ứng dụng quản lý (StaffApp)." |
| (để yên) | "Bị từ chối, và kiểm tra này nằm ở service, nên tự viết giao diện khác cũng không qua được. **Ẩn nút chưa phải là phân quyền. Mọi kiểm tra quyền nằm ở service.**" | |

### B2 · Giảng viên (2 phút) – [ĐIỂM SÁNG 2]

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| Alt+Tab sang StaffApp → đăng nhập `gv.viet` / `gv123456` → Ngân hàng câu hỏi | "Ứng dụng quản lý, giảng viên môn OOP. Ngân hàng 60 câu, đủ bốn loại." | Bảng 60 câu |
| Thêm câu hỏi → giữ loại "Một đáp án" → dán nội dung `Lớp trừu tượng có tạo đối tượng trực tiếp được không?` và bốn phương án `Được`, `Không`, `Chỉ khi có constructor`, `Chỉ trong lớp con` → tick **Đúng** ở cả `Được` và `Không` → Lưu câu hỏi | "Em cố tình nhập sai: câu một đáp án nhưng có hai đáp án đúng." | "Câu một đáp án phải có đúng 1 phương án đúng, hiện có 2." |
| (để yên) | "Lỗi này **không do giao diện kiểm tra**: luật nằm trong constructor của `SingleChoiceQuestion`, giao diện chỉ hiện lỗi mà lớp đó ném ra. **Controller không kiểm tra gì cả. Luật sống trong lớp con của `Question`.**" | |
| Bỏ tick `Được` → Lưu câu hỏi | "Sửa lại thì lưu được; tạo lớp con nào là việc của `QuestionFactory`." | Câu mới xuất hiện trong bảng |
| *(Đường tắt: dừng ở đây, sang B3)* | | |
| Đề thi → Tạo đề mới → dán tên `Demo thuyết trình` → thẻ "Theo ma trận" → Chương 1 / Dễ = `3`, Chương 2 / Trung bình = `3`, Chương 3 / Khó = `2` → Tạo đề | "Tạo đề theo ma trận chương và mức độ; câu bốc ngẫu nhiên. Câu nhiều đáp án chấm toàn phần hay theo tỷ lệ là lựa chọn của đề – mẫu Strategy." | Hộp thoại "Đã tạo đề…" |
| **[cắt]** (trước khi Tạo đề) đổi Chương 3 / Khó thành `10` → Kiểm tra ma trận, rồi đổi lại `2` | "Xin quá số câu có trong ngân hàng thì hệ thống báo thiếu trước khi sinh đề." | "Thiếu câu: chương 3 mức Khó: cần 10, có 7." |
| Chọn đề "Demo thuyết trình" → Mở ca thi với đề này… → ô Lớp chọn `INT1332-01` → Mở ca thi → đóng hộp thoại. **Giữ StaffApp mở** | "Mở ca cho cả lớp, giờ mở theo **giờ máy chủ**, chung cho mọi thí sinh. Mở ca xong đề bị khóa. Ứng dụng này cứ để mở: nó đang là máy chấm." | "Đã mở ca thi #… cho 40 thí sinh…" |

### B3 · Thí sinh làm bài (1 phút 50) – [ĐIỂM SÁNG 4, 5]

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| Alt+Tab sang ExamClientApp → đăng nhập `b24dcce098` / `sv123456` → ở ca "Demo thuyết trình" (đường tắt: "Kiểm tra giữa kỳ OOP") → Vào thi → tick "Tôi đã đọc và đồng ý với quy chế thi" → Bắt đầu làm bài | "Phía thí sinh. Hai thao tác là vào thi, cửa sổ chiếm toàn màn hình, Esc và Alt+F4 bị chặn." | Toàn màn hình, đồng hồ "Thời gian còn lại" |
| Trả lời 3 câu, chuyển câu bằng "Câu sau ▶" | "Mỗi câu trả lời lưu xuống CSDL nửa giây sau khi chọn: mất điện vào lại vẫn còn nguyên bài, và là làm tiếp chứ không tạo bài mới." | Ô câu đã trả lời đổi màu |
| **[cắt]** Bấm "Đánh dấu xem lại" ở một câu | "Câu chưa chắc thì đánh dấu để xem lại." | Ô câu đổi màu đánh dấu |
| **Alt+Tab ra ngoài, chờ một giây, Alt+Tab quay lại** | "Giờ em thử gian lận: rời cửa sổ để tra cứu." | "Cảnh báo vi phạm quy chế": "Bạn đã rời cửa sổ làm bài (lần 1/5). Còn 4 lần nữa bài thi sẽ bị kết thúc." |
| (để hộp thoại trên màn hình) | "Ghi ngay một lần vi phạm; đủ năm lần thì khóa bài. **Chỉnh đồng hồ máy thí sinh cũng không được thêm phút nào: giờ thi lấy từ máy chủ.**" | |
| Quay lại làm bài → Nộp bài → xác nhận nộp | "Nộp bài, hệ thống nhắc số câu còn trống." | "Đã nộp bài. Đang chấm bài…" |
| (chờ điểm hiện, vài giây) | "**Máy thí sinh không chấm bài, vì nó không có đáp án để chấm.** Máy giảng viên chấm 3 giây một lượt, điểm về gần như ngay." | Điểm "… / 8" (đường tắt: "… / 10") |

### B4 · Kết quả và bảo mật (1 phút 20) – [ĐIỂM SÁNG 1, 6]

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| Alt+Tab sang StaffApp → Ca thi → chọn ca vừa thi → Xem kết quả → Làm mới | "Giảng viên thấy ngay điểm, số lần vi phạm, và câu khó nhất của cả lớp." | Dòng `b24dcce098` có điểm và 1 lần vi phạm |
| Alt+Tab sang cửa sổ dòng lệnh MySQL (đã đăng nhập sẵn) | "**Phần nhóm em tự tin nhất.** Cửa sổ này đăng nhập MySQL bằng đúng tài khoản của máy thí sinh, như thể có người sửa mã ứng dụng thi." | Dấu nhắc `mysql>` |
| Dán `SELECT * FROM answer_option;` → Enter | "Đọc bảng đáp án…" | `ERROR 1142 (42000): SELECT command denied to user 'oes_client'@'…' for table 'answer_option'` |
| Dán `UPDATE submission SET total_score = 10 WHERE id = 1;` → Enter | "…tự cho mình 10 điểm." | `ERROR 1143 (42000): UPDATE command denied to user 'oes_client'@'…' for column 'total_score' in table 'submission'` |
| (để yên) | "Cả hai bị **chính MySQL** từ chối. **Kể cả sửa mã ứng dụng thi cũng không lấy được đáp án: người từ chối là MySQL, không phải giao diện.** Và 9 kiểm thử tự động làm đúng việc này mỗi lần chạy. **Quay lại slide.**" | |

---

## C. Chốt và mời hỏi đáp (slide 10, 30 giây)

| DM bấm | TT nói |
|---|---|
| Slide 10 (tổng kết, "Hỏi đáp") | "Thầy vừa thấy: phân quyền ở service, luật nằm trong lớp, gian lận được ghi nhận theo giờ máy chủ, điểm có ngay, và máy thí sinh không đọc được đáp án dù bị sửa mã. Giới hạn chúng em biết rõ: mọi máy thí sinh dùng chung một tài khoản CSDL; tách riêng cần thêm máy chủ ứng dụng, ngoài công nghệ môn học cho phép." |
| | "Nhóm 2 xin cảm ơn thầy và các bạn. Chúng em xin nhận câu hỏi." |

Trong lúc hỏi đáp: để slide 10 trên màn hình, **giữ nguyên các ứng dụng đang mở**. Giảng viên muốn xem gì thêm thì DM
làm theo mục [Cho xem thêm khi hỏi đáp](#cho-xem-thêm-khi-hỏi-đáp).

---

## Bộ slide

Có sẵn: [`slides/thuyet-trinh.pdf`](slides/thuyet-trinh.pdf), 13 trang (10 slide chính và 3 slide dự phòng), nguồn
LaTeX [`slides/thuyet-trinh.tex`](slides/thuyet-trinh.tex). Sửa nội dung thì sửa tệp `.tex` rồi chạy
`xelatex thuyet-trinh.tex` trong thư mục `docs/slides` (MiKTeX hoặc TeX Live; cần XeLaTeX để có font tiếng Việt).

| Slide | Nội dung | Phần |
|:-:|---|---|
| 1 | Tên đề tài, Nhóm 2, bốn thành viên, giảng viên | A1 |
| 2 | Ba nỗi lo → ba lời giải | A1 |
| 3 | Sơ đồ triển khai: MySQL, StaffApp (máy chấm), N máy ExamClientApp, hai tài khoản CSDL | A2 |
| 4 | Ba tầng giao diện → service → DAO; DAO có hai cài đặt (JDBC, bộ nhớ); DIP | A2 |
| 5 | Cây lớp `Question` (4 lớp con) và `User` (3 lớp con); bốn tính chất OOP | A3 |
| 6 | Ba mẫu thiết kế (Factory, Strategy, Template Method) và SOLID (SRP, ISP, DIP) | A3 |
| 7 | Bảo mật ba lớp (tài khoản CSDL, trigger, service); bốn quy tắc đã chốt | A4 |
| 8 | Bốn con số: 135 kiểm thử, 9 ca tấn công, 2 hệ CSDL, 31 ms; dòng kết quả `Tests run: 135…` | A4 |
| 9 | "Demo": năm điều sẽ chứng minh | A5 |
| 10 | Năm điều đã chứng minh, giới hạn, "Cảm ơn – Hỏi đáp" | C |
| 11–13 | Dự phòng: 6 ảnh giao diện theo đúng thứ tự demo | Khi demo hỏng |

Trình chiếu: mở PDF ở chế độ toàn màn hình. Slide chỉ có từ khóa, sơ đồ và con số; lời giải thích nằm trong lời TT,
không đọc chữ trên màn hình.

---

## Cho xem thêm khi hỏi đáp

Những phần bị bỏ khỏi 10 phút, dùng khi giảng viên hỏi "cho xem thử…". Mỗi mục khoảng 40 giây.

- **Quản trị viên.** StaffApp: Đăng xuất → đăng nhập `admin` / `admin123` → Người dùng (tìm `b24dcce0`, khóa, đặt lại
  mật khẩu, nhập CSV) → Môn học & lớp (phân công giảng viên).
- **Kết quả chi tiết.** Xem kết quả → bảng "Tỷ lệ trọn điểm từng câu (câu khó nhất ở trên)" → Xuất CSV… → mở bằng
  Excel: tiếng Việt đúng nhờ BOM UTF-8.
- **Vào lại làm tiếp bài cũ.** ExamClientApp: đăng nhập `b24dcce007` → Vào thi → trả lời 2 câu → Ctrl+Shift+Esc mở
  Task Manager (bị tính một lần vi phạm) → mở rộng các dòng Java ("Java(TM) Platform SE binary" hoặc "OpenJDK Platform
  binary"), chọn dòng có cửa sổ "Hệ thống thi trực tuyến – Thi" → End task. **Không tắt nhầm dòng "– Quản lý": đó là
  máy chấm.** Chạy lại `scripts\start-client.bat` → đăng nhập `b24dcce007` → "Tiếp tục làm bài (đã vi phạm 1 lần)" →
  hai câu đã trả lời vẫn còn.
- **Đủ 5 lần vi phạm.** Đăng nhập `b24dcce021` → Vào thi → Alt+Tab ra vào 5 lần, mỗi lần cách nhau hơn 3 giây → bài bị
  kết thúc. StaffApp → Xem kết quả → Làm mới: dòng `b24dcce021` tô đỏ, vẫn có điểm phần đã làm.
- **Nghiệp vụ chạy không cần MySQL** (bằng chứng cho DIP). Cửa sổ dòng lệnh tại thư mục dự án: `mvnw.cmd -q compile
  exec:java`. ConsoleDemo chạy toàn bộ kịch bản tạo đề, thi, chấm trên DAO trong bộ nhớ.

---

## Sự cố và cách xử lý

TT không xin lỗi dài dòng. Nói một câu, rồi nói tiếp nội dung trong lúc DM xử lý.

| Sự cố | TT nói | DM làm |
|---|---|---|
| Ứng dụng báo không kết nối được CSDL | "Máy chủ CSDL đang khởi động lại, trong lúc chờ em nói tiếp." | `docker start oes-mysql`, chờ 20 giây, đăng nhập lại |
| Ứng dụng thi không thấy ca "Demo thuyết trình" | "Em làm mới danh sách." | Bấm "Làm mới" ở danh sách ca. Vẫn không có: vào ca mẫu "Kiểm tra giữa kỳ OOP" |
| Alt+Tab không hiện cảnh báo vi phạm | "Logic đếm và gộp vi phạm có kiểm thử riêng." | Bỏ qua, làm tiếp bước Nộp bài |
| Nộp xong quá 10 giây chưa có điểm | "Máy chấm nằm ở ứng dụng quản lý." | Sang StaffApp → Ca thi → Xem kết quả → Làm mới; điểm hiện ở đó |
| Cửa sổ thi treo, không thoát được | "Bài đã lưu từng câu nên không mất." | Ctrl+Shift+Esc → End task dòng "– Thi" (không phải "– Quản lý") |
| Hỏng hẳn, không cứu được trong 30 giây | "Chúng em chuyển sang ảnh chụp của đúng các bước này." | Mở slide dự phòng (11–13) |

---

## Câu hỏi hay gặp

- **"Sao không chấm trên máy thí sinh cho nhanh?"** → Muốn chấm được thì máy thí sinh phải đọc được đáp án. Đó là lỗi
  của bản thiết kế M4, đã sửa ở M5 (`DESIGN-CHANGES.md`, Phase 1). Chấm ở máy giảng viên vẫn chỉ mất vài giây.
- **"Hai máy cùng bấm Vào thi một lúc?"** → Ràng buộc `UNIQUE(session_id, student_id)` trong CSDL: máy sau mở lại đúng
  bài của máy trước.
- **"Thí sinh chỉnh giờ máy thì sao?"** → Giờ chuẩn là `SELECT NOW()` của MySQL; ứng dụng chỉ đo khoảng trôi bằng
  `System.nanoTime()`, không phụ thuộc giờ hệ điều hành. Hạn chót còn được trigger kiểm tra lần nữa ngay trong CSDL.
- **"Ẩn nút trên giao diện chưa đủ à?"** → Không. Mọi kiểm tra quyền nằm ở service qua `Permissions`, nên gọi thẳng
  service (bỏ qua giao diện) vẫn bị chặn. B1 vừa chứng minh.
- **"Sao không dùng Spring, Hibernate?"** → Giảng viên yêu cầu JDBC thuần. Nhờ có interface DAO, đổi cách lưu trữ chỉ
  là thêm một cài đặt mới, service không phải sửa.
- **"Máy chủ dùng MariaDB thay MySQL có chạy không?"** → Có, từ MariaDB 10.6: đủ 135 kiểm thử đã chạy trên 10.6.28.
  XAMPP bản cũ đi kèm MariaDB 10.4, bản đó chưa thử.
- **"Mất điện giữa giờ?"** → Câu trả lời đã lưu từng câu. Đăng nhập lại, kể cả ở máy khác, là làm tiếp; giờ vẫn tính
  theo giờ đóng ca chung.
- **"Giới hạn còn lại?"** → Mọi máy thí sinh dùng chung tài khoản `oes_client`, nên ứng dụng thi bị sửa vẫn đọc/ghi
  được câu trả lời của thí sinh khác đang làm bài (không đọc được đáp án, không sửa được điểm). Chặn hẳn cần một máy
  chủ ứng dụng ở giữa, ngoài công nghệ được phép.
- Câu hỏi sâu về từng phần: [`DE-CUONG-VAN-DAP.md`](DE-CUONG-VAN-DAP.md).
