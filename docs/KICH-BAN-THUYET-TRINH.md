# Kịch bản thuyết trình sản phẩm (1 người demo, 1 người thuyết trình)

[README](../README.md) · [Phân công](PHAN-CONG.md) · [Tài liệu vấn đáp](vandap/) · [Chạy trên máy cá nhân](SETUP.md) · [Dựng phòng máy](RUNBOOK.md) · [Kịch bản demo](DEMO-SCRIPT.md)

Dùng cho buổi thuyết trình sản phẩm trước giảng viên: **một người chỉ thao tác máy (gọi là DM)** và **một người nói từ
đầu tới cuối (gọi là TT)**, cả buổi trên **một laptop** nối máy chiếu. Trình tự: **thuyết trình bằng slide → demo →
chốt → hỏi đáp**. Thời lượng **12 phút**, chưa kể hỏi đáp. Bỏ các dòng có ghi **[cắt]** thì còn khoảng 10 phút.
Kịch bản demo chia theo bốn phần để vấn đáp nằm ở [`DEMO-SCRIPT.md`](DEMO-SCRIPT.md).

Mục lục: [Sáu điểm sáng](#sáu-điểm-sáng--phải-thuộc) · [Phân vai](#phân-vai-và-quy-ước) · [Chuẩn bị](#chuẩn-bị) ·
[Dòng thời gian](#dòng-thời-gian) · [A. Thuyết trình](#a-thuyết-trình-slide-18-4-phút-30) ·
[B. Demo](#b-demo-7-phút) · [C. Chốt](#c-chốt-và-mời-hỏi-đáp-slide-9-30-giây) · [Slide](#slide-gợi-ý) ·
[Phần mở rộng](#phần-mở-rộng-nếu-còn-giờ-hoặc-giảng-viên-yêu-cầu) · [Sự cố](#sự-cố-và-cách-xử-lý) ·
[Hỏi đáp](#câu-hỏi-hay-gặp)

---

## Sáu điểm sáng – phải thuộc

Phần slide **nêu** sáu điểm này, phần demo **chứng minh** chúng ngay trên màn hình. Đúng lúc màn hình hiện bằng chứng,
TT nói nguyên văn **câu chốt**. Trong kịch bản, chỗ chứng minh được đánh dấu **[ĐIỂM SÁNG n]**.

| # | Điểm sáng | Nêu ở | Chứng minh ở | Câu chốt |
|:-:|---|---|---|---|
| 1 | **Máy thí sinh "mù đáp án"**: bảo mật nằm ở CSDL, không chỉ ở giao diện | Slide 3, 6 | B4: gõ SQL bằng chính tài khoản của máy thí sinh, đọc đáp án và tự sửa điểm đều bị MySQL từ chối | "Kể cả sửa mã ứng dụng thi cũng không lấy được đáp án: người từ chối là MySQL, không phải giao diện." |
| 2 | **Hướng đối tượng thật**: luật nghiệp vụ nằm trong lớp, giao diện không kiểm tra gì | Slide 5 | B2: câu một đáp án đánh dấu 2 đáp án đúng bị lớp `SingleChoiceQuestion` từ chối | "Controller không kiểm tra gì cả. Luật 'đúng một đáp án' sống trong lớp con của `Question`." |
| 3 | **Phân quyền ở tầng service**, không phải ẩn nút | Slide 6 | B1: tài khoản quản trị đăng nhập ứng dụng thi bị từ chối | "Ẩn nút chưa phải là phân quyền. Mọi kiểm tra quyền nằm ở service." |
| 4 | **Chống gian lận và giờ chuẩn từ máy chủ** | Slide 6 | B3: toàn màn hình, rời cửa sổ bị ghi vi phạm "lần 1/5"; đồng hồ đếm theo giờ MySQL | "Chỉnh đồng hồ máy thí sinh cũng không được thêm phút nào: giờ thi lấy từ máy chủ." |
| 5 | **Tách máy chấm khỏi máy thí sinh**, có điểm gần như ngay lập tức | Slide 3 | B3–B4: nộp bài, vài giây sau có điểm; giảng viên thấy bảng điểm và câu khó nhất | "Máy thí sinh không chấm bài, vì nó không có đáp án để chấm." |
| 6 | **Kiểm chứng bằng số**: 135 kiểm thử, chạy trên MySQL và MariaDB, thử tải 60 thí sinh | Slide 7 | B4: hai lệnh SQL bị từ chối chính là điều 9 kiểm thử tự động làm mỗi lần chạy | "9 kiểm thử đóng vai ứng dụng thi bị sửa, dùng chính tài khoản của máy thí sinh để tấn công CSDL." |

Giảng viên môn OOP chấm đúng những chỗ sau, nên TT phải nói rõ ở slide 5 và nhắc lại trong demo: **bốn tính chất OOP**
gắn với lớp cụ thể, **ba mẫu thiết kế** (Factory, Strategy, Template Method) và **SOLID** (SRP, ISP, DIP).

---

## Phân vai và quy ước

- **DM** ngồi máy và điều khiển mọi thứ trên laptop, kể cả chuyển slide. Làm đúng thứ tự cột "DM bấm", không nói.
- **TT** đứng cạnh màn chiếu, nói theo cột "TT nói", chỉ tay vào màn hình khi tới cột "Màn hình phải hiện".
- **DM bấm trước, TT nói sau**: TT chỉ nói câu của bước nào khi màn hình đã hiện đúng như cột thứ ba. Máy chậm thì TT
  dùng **câu đệm** ghi ở đầu mỗi phần demo, không im lặng và không giục DM.
- **Câu hiệu chuyển phần**: TT nói "**Mời thầy và các bạn cùng xem**" thì DM rời slide sang ứng dụng. TT nói
  "**Quay lại slide**" thì DM về slide.
- **Mốc giờ**: hết phần slide mà đồng hồ đã quá **5 phút**, TT bỏ các dòng **[cắt]** trong demo. Demo là phần chứng minh,
  không được cắt mất phần B4.
- **Trong lúc thi (B3), DM không chạm vào cửa sổ nào khác**, trừ đúng một lần Alt+Tab đã ghi trong kịch bản.

---

## Chuẩn bị

### Hôm trước (bắt buộc)

- [ ] Chạy trọn kịch bản **hai lần** trên đúng laptop và đúng máy chiếu, bấm giờ từng phần.
- [ ] **Thử Alt+Tab khi đang thi**: phải hiện hộp thoại "Cảnh báo vi phạm quy chế". Thử Esc và Alt+F4: không thoát được.
      Cơ chế này mới kiểm tra bằng giả lập (xem `PROGRESS.md`, "Câu hỏi mở"), nên **phải tự thử trên Windows**.
- [ ] Chạy đủ kiểm thử và chụp màn hình dòng `Tests run: 135, Failures: 0, Errors: 0, Skipped: 0` cho slide 7. MySQL
      trong Docker: `scripts\test.bat --db "--port=<cổng container>" "--root-password=oes_root"` (cổng xem bằng
      `docker port oes-mysql`). MySQL cài trên Windows: `scripts\test.bat --db`, lệnh sẽ hỏi mật khẩu root. Gọi trần
      `--db` trên máy có cả hai thì lệnh nối vào cổng 3306, có thể nhầm sang MySQL cài sẵn.
- [ ] Làm slide theo [Slide gợi ý](#slide-gợi-ý). Thêm một slide dự phòng chứa 6 ảnh giao diện trong `docs/images/`.
      Xuất thêm một bản PDF.
- [ ] Tăng tỷ lệ hiển thị Windows lên 125–150% (Settings → Display → Scale) để cuối phòng đọc được chữ.

### 30 phút trước

- [ ] Bật Docker Desktop, chạy `scripts\setup-docker.bat --reset`. Lệnh này dựng lại CSDL sạch: tài khoản mẫu và 60 câu
      hỏi. Máy dùng MySQL cài sẵn thì chạy `scripts\setup.bat --reset`.
- [ ] Bật **Không làm phiền** (Settings → System → Notifications → Do not disturb). Thoát Zalo, Messenger, Teams,
      OneDrive. **Một thông báo bật lên giữa giờ thi được tính là một lần vi phạm.**
- [ ] Máy chiếu để chế độ **Duplicate** (Win+P → Duplicate). Ở chế độ Extend, cửa sổ thi toàn màn hình có thể nhảy
      sang màn hình kia.
- [ ] Cắm sạc, tắt chế độ ngủ, hoãn cập nhật Windows.
- [ ] Chạy `scripts\start-demo.bat` để mở hai ứng dụng, để cả hai ở màn hình đăng nhập.
- [ ] Mở một cửa sổ dòng lệnh, phóng chữ to (Ctrl + lăn chuột), **đăng nhập sẵn** MySQL bằng tài khoản máy thí sinh:
  - MySQL trong Docker: `docker exec -it oes-mysql mysql -uoes_client -p oes`, mật khẩu `doi_mat_khau_client`.
  - MySQL cài trên Windows: `"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u oes_client -p oes`.
- [ ] Mở Notepad chứa sẵn hai lệnh SQL ở B4 và nội dung câu hỏi ở B2, để chép thay vì gõ trước khán giả.
- [ ] Sắp thứ tự cửa sổ: slide (trên cùng, chế độ trình chiếu), StaffApp, ExamClientApp, dòng lệnh MySQL, Notepad.
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
| 0:45 | A2. Kiến trúc | Slide 3–4 | nêu 1, 5 |
| 1:45 | A3. Thiết kế hướng đối tượng | Slide 5 | nêu 2 |
| 3:00 | A4. Bảo mật, nghiệp vụ, kiểm thử | Slide 6–7 | nêu 1, 3, 4, 6 |
| 4:00 | A5. Chuyển sang demo | Slide 8 | |
| 4:30 | B1. Demo: phân quyền | StaffApp, ExamClientApp | **3** |
| 5:15 | B2. Demo: giảng viên | StaffApp | **2** |
| 7:30 | B3. Demo: thí sinh làm bài | ExamClientApp | **4**, **5** |
| 9:45 | B4. Demo: kết quả và bảo mật | StaffApp, dòng lệnh MySQL | **5**, **1**, **6** |
| 11:30 | C. Chốt | Slide 9 | |
| 12:00 | Hỏi đáp | Slide 9, ứng dụng vẫn mở | |

---

## A. Thuyết trình (slide 1–8, 4 phút 30)

### A1 · Mở đầu (slide 1–2, 45 giây)

| DM bấm | TT nói |
|---|---|
| Slide 1 | "Kính chào thầy và các bạn. Nhóm 2 xin trình bày đề tài 16: hệ thống thi trắc nghiệm trực tuyến cho phòng máy." |
| Slide 2 | "Thi trong phòng máy có ba nỗi lo: lộ đáp án, gian lận khi làm bài, và chấm điểm chậm. Hệ thống của chúng em xử lý cả ba: máy thí sinh không bao giờ giữ đáp án, bài làm toàn màn hình có giám sát, và điểm có ngay khi nộp." |
| | "Bài trình bày gồm hai phần: thiết kế của hệ thống, rồi demo trực tiếp để chứng minh từng điều chúng em nói." |

### A2 · Kiến trúc (slide 3–4, 1 phút)

| DM bấm | TT nói |
|---|---|
| Slide 3 (sơ đồ triển khai) | "Một MySQL chung trong mạng LAN và hai ứng dụng desktop JavaFX. Ứng dụng quản lý dành cho quản trị viên và giảng viên, đồng thời là **máy chấm bài**. Ứng dụng thi cài trên máy thí sinh, và nó **không chấm bài**, vì nó không có đáp án để chấm." |
| | "Điểm mấu chốt: hai ứng dụng đăng nhập MySQL bằng **hai tài khoản khác nhau**. Tài khoản của máy thí sinh chỉ đọc được các khung nhìn đã lọc bỏ đáp án. Cuối buổi chúng em sẽ thử tấn công chính tài khoản đó." |
| Slide 4 (ba tầng) | "Mỗi ứng dụng chia ba tầng: giao diện, service, DAO. Controller giao diện không chứa nghiệp vụ. Service chỉ biết interface DAO, và mỗi interface có hai cài đặt: JDBC cho MySQL, bộ nhớ cho kiểm thử. Cùng một nghiệp vụ chạy được cả khi không có MySQL – đó là nguyên lý đảo ngược phụ thuộc, chữ D trong SOLID." |

### A3 · Thiết kế hướng đối tượng (slide 5, 1 phút 15)

| DM bấm | TT nói |
|---|---|
| Slide 5 (sơ đồ lớp `Question`, `User`) | "**Trừu tượng**: `Question` và `User` là lớp trừu tượng, DAO là interface. **Kế thừa**: bốn loại câu hỏi là bốn lớp con của `Question`; ba vai trò là ba lớp con của `User`; hai ứng dụng cùng kế thừa `OesApplication`." |
| | "**Đa hình**: mỗi loại câu tự chấm chính nó qua `isCorrect` và `correctRatio`, nơi gọi không cần biết đó là loại nào. **Đóng gói**: dữ liệu được kiểm tra ngay trong constructor, nên không bao giờ tồn tại một câu hỏi sai; collection trả ra là bản chỉ đọc." |
| | "Ba mẫu thiết kế: **Factory** tạo đúng lớp con theo loại câu, **Strategy** để mỗi đề chọn cách chấm câu nhiều đáp án, **Template Method** cho phần khung chung của hai ứng dụng. Về SOLID: tách `Submission` khỏi `ExamSession` theo SRP, tách DAO theo người dùng theo ISP, service chỉ biết interface theo DIP." |
| | "Trong demo, thầy sẽ thấy luật nghiệp vụ nằm trong lớp, chứ không nằm trong giao diện." |

### A4 · Bảo mật, nghiệp vụ, kiểm thử (slide 6–7, 1 phút)

| DM bấm | TT nói |
|---|---|
| Slide 6 (ba lớp bảo vệ) | "Bảo mật có ba lớp. Một: tài khoản CSDL của máy thí sinh chỉ đọc khung nhìn không có đáp án, chỉ ghi đúng các cột cần để làm bài. Hai: trigger trong CSDL chặn ghi khi bài đã nộp hoặc ca đã đóng. Ba: mọi kiểm tra quyền nằm ở service, không dựa vào việc ẩn nút." |
| | "Nghiệp vụ đã chốt: mỗi thí sinh thi đúng một lần trong một ca, vào lại thì làm tiếp bài cũ. Giờ thi lấy từ máy chủ MySQL. Rời cửa sổ làm bài là một lần vi phạm, đủ năm lần thì khóa bài." |
| Slide 7 (kiểm thử) | "135 kiểm thử tự động JUnit 5, trong đó 23 chạy với MySQL thật, và **9 kiểm thử đóng vai ứng dụng thi bị sửa**, dùng chính tài khoản máy thí sinh để tấn công CSDL. Toàn bộ chạy được trên cả MySQL 8 và MariaDB 10.6. Thử tải 60 thí sinh làm bài cùng lúc: 95% số lần lưu câu trả lời chỉ mất khoảng 31 mili giây; chấm 60 bài mất 2,2 giây." |

### A5 · Chuyển sang demo (slide 8, 30 giây)

| DM bấm | TT nói |
|---|---|
| Slide 8 (sáu điều sẽ chứng minh) | "Đó là những gì chúng em thiết kế. Bây giờ xin chứng minh trực tiếp trên màn hình: phân quyền, hướng đối tượng, chống gian lận, chấm tức thì, và cuối cùng là thử tấn công CSDL bằng tài khoản của máy thí sinh – đúng việc mà các kiểm thử tự động vẫn làm." |
| | "Bạn ở máy sẽ thao tác, em xin dẫn. **Mời thầy và các bạn cùng xem.**" |

---

## B. Demo (7 phút)

### B1 · Phân quyền (45 giây) – [ĐIỂM SÁNG 3]

Câu đệm: "Trong lúc chờ, xin nói thêm: mỗi ứng dụng chỉ nhận đúng vai trò của mình."

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| StaffApp: đăng nhập `admin` / `admin123` | "Ứng dụng quản lý, đăng nhập bằng quản trị viên." | Màn hình chính, menu trái có "Người dùng", "Môn học & lớp" |
| Người dùng → gõ `b24dcce0` vào ô tìm → Tìm | "Quản trị viên tìm, khóa tài khoản và nhập cả lớp từ tệp CSV. Đây là 40 sinh viên lớp D24CQCE07-B, lớp của chính chúng em." | Danh sách các tài khoản `b24dcce0…` |
| **[cắt]** Môn học & lớp → chọn INT1332 | "Môn học, lớp học phần, phân công giảng viên." | Môn INT1332 và lớp INT1332-01 |
| Alt+Tab sang ExamClientApp → đăng nhập `admin` / `admin123` | "Giờ thử dùng chính tài khoản quản trị này đăng nhập vào **ứng dụng thi**." | Thông báo lỗi: "Tài khoản giảng viên và quản trị viên hãy dùng ứng dụng quản lý (StaffApp)." |
| (để yên) | "Bị từ chối, và kiểm tra này nằm ở tầng service: tự viết giao diện khác gọi thẳng service cũng bị chặn. **Ẩn nút chưa phải là phân quyền. Mọi kiểm tra quyền nằm ở service.**" | |
| Alt+Tab về StaffApp → Đăng xuất | | Màn hình đăng nhập |

### B2 · Giảng viên: câu hỏi, đề, ca thi (2 phút 15) – [ĐIỂM SÁNG 2]

Câu đệm: "Các thao tác này chạy nền, giao diện không bao giờ đứng hình khi chờ CSDL."

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| StaffApp: đăng nhập `gv.viet` / `gv123456` → Ngân hàng câu hỏi | "Giảng viên Việt phụ trách môn Lập trình hướng đối tượng. Ngân hàng 60 câu, đủ bốn loại, chia theo chương và mức độ." | Bảng 60 câu |
| Thêm câu hỏi → giữ loại "Một đáp án" → nội dung: `Lớp trừu tượng có tạo đối tượng trực tiếp được không?` → bốn phương án: `Được`, `Không`, `Chỉ khi có constructor`, `Chỉ trong lớp con` → tick **Đúng** ở cả `Được` và `Không` → Lưu câu hỏi | "Em cố tình nhập sai: câu một đáp án nhưng đánh dấu hai đáp án đúng." | Lỗi cạnh ô phương án: "Câu một đáp án phải có đúng 1 phương án đúng, hiện có 2." |
| (để yên) | "Đây là phần đóng gói ở slide 5. Thông báo này **không do giao diện kiểm tra**: luật nằm trong constructor của `SingleChoiceQuestion`, giao diện chỉ hiện lỗi mà lớp đó ném ra. **Controller không kiểm tra gì cả. Luật 'đúng một đáp án' sống trong lớp con của `Question`.**" | |
| Bỏ tick `Được` → Lưu câu hỏi | "Sửa lại cho đúng. Lớp con nào được tạo là việc của `QuestionFactory` – mẫu Factory." | Câu mới xuất hiện trong bảng |
| Đề thi → Tạo đề mới → Tên đề: `Demo thuyết trình` → ô "Câu nhiều đáp án" giữ "Theo tỷ lệ: đúng phần nào được phần đó" | "Tạo đề. Chấm toàn phần hay theo tỷ lệ là lựa chọn của từng đề – mẫu Strategy ở slide 5." | Màn hình "Tạo đề thi" |
| Thẻ "Theo ma trận" → nhập Chương 1 / Dễ = `3`, Chương 2 / Trung bình = `3`, Chương 3 / Khó = `10` → Kiểm tra ma trận | "Giảng viên nhập ma trận: chương nào, mức nào, bao nhiêu câu. Em cố tình xin 10 câu khó ở chương 3." | "Thiếu câu: chương 3 mức Khó: cần 10, có 7." |
| Sửa Chương 3 / Khó thành `2` → Kiểm tra ma trận → Tạo đề | "Ngân hàng chỉ có 7 câu, hệ thống báo thiếu **trước** khi sinh đề. Sửa thành 2 thì đủ; câu được bốc ngẫu nhiên." | "Đủ câu: ma trận lấy 8 câu.", rồi hộp thoại "Đã tạo đề…" |
| Chọn đề "Demo thuyết trình" → Mở ca thi với đề này… → ô Lớp chọn `INT1332-01` → giữ nguyên giờ → Mở ca thi | "Mở ca thi cho cả lớp. Giờ mở mặc định là ngay bây giờ **theo giờ máy chủ**, kéo dài 60 phút, chung cho mọi thí sinh. Mở ca xong đề bị khóa, để không ai sửa được đề khi đang có người thi." | "Đã mở ca thi #… cho 40 thí sinh… Đề "Demo thuyết trình" đã bị khóa…" |
| Đóng hộp thoại. **Giữ StaffApp đăng nhập, không đóng** | "Ứng dụng quản lý cứ để mở: nó đang là máy chấm bài." | Danh sách ca thi |

### B3 · Thí sinh làm bài (2 phút 15) – [ĐIỂM SÁNG 4, 5]

Câu đệm: "Mọi câu trả lời đều được lưu xuống CSDL ngay, nên mất điện cũng không mất bài."

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| Alt+Tab sang ExamClientApp → đăng nhập `b24dcce098` / `sv123456` | "Phía thí sinh – mã sinh viên của bạn Hoàng trong nhóm." | "Ca thi của bạn", có ca "INT1332 – Demo thuyết trình" |
| Ở dòng ca "Demo thuyết trình" → Vào thi → tick "Tôi đã đọc và đồng ý với quy chế thi" → Bắt đầu làm bài | "Từ đăng nhập tới làm bài chỉ hai thao tác. Đồng ý quy chế, và cửa sổ chiếm toàn màn hình." | Toàn màn hình, đồng hồ "Thời gian còn lại", lưới "Danh sách câu" |
| **[cắt]** Bấm Esc | "Esc không thoát được, Alt+F4 cũng vậy." | Vẫn toàn màn hình |
| Trả lời 3–4 câu (chọn phương án; gặp câu điền khuyết thì gõ một từ), chuyển câu bằng "Câu sau ▶" hoặc bấm số câu ở lưới | "Mỗi câu trả lời được lưu xuống CSDL nửa giây sau khi chọn. Máy tắt đột ngột thì vào lại vẫn còn nguyên bài, và vào lại là làm tiếp, không tạo bài mới." | Ô câu đã trả lời đổi màu |
| Bấm "Đánh dấu xem lại" ở một câu | "Câu chưa chắc thì đánh dấu để xem lại." | Ô câu đó đổi màu đánh dấu |
| **Alt+Tab ra ngoài, chờ một giây, Alt+Tab quay lại** | "Bây giờ em thử gian lận: rời cửa sổ làm bài để tra cứu." | Hộp thoại "Cảnh báo vi phạm quy chế": "Bạn đã rời cửa sổ làm bài (lần 1/5). Còn 4 lần nữa bài thi sẽ bị kết thúc." |
| (để hộp thoại trên màn hình) | "Mỗi lần rời cửa sổ là một lần vi phạm, ghi thẳng xuống CSDL; đủ 5 lần thì khóa bài, vẫn chấm phần đã làm. Các lần rời cách nhau dưới 3 giây được gộp làm một để không phạt oan khi máy giật. **Chỉnh đồng hồ máy thí sinh cũng không được thêm phút nào: giờ thi lấy từ máy chủ.**" | |
| Quay lại làm bài → Nộp bài → xác nhận nộp | "Hệ thống nhắc số câu còn trống trước khi nộp." | Hộp thoại "Còn … câu chưa trả lời, 1 câu đang đánh dấu xem lại…", rồi "Đã nộp bài. Đang chấm bài…" |
| (chờ điểm hiện, vài giây) | "**Máy thí sinh không chấm bài, vì nó không có đáp án để chấm.** Bài được chấm ở máy giảng viên, 3 giây một lượt, nên điểm về gần như ngay lập tức." | Điểm "… / 8" |

### B4 · Kết quả và bảo mật (1 phút 45) – [ĐIỂM SÁNG 5, 1, 6]

Câu đệm: "Bảng điểm này đọc trực tiếp từ CSDL, không phải từ máy thí sinh gửi lên."

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| Alt+Tab sang StaffApp → (đang ở Ca thi) chọn ca "Demo thuyết trình" → Xem kết quả → Làm mới | "Phía giảng viên: bài vừa nộp đã có điểm, kèm số lần vi phạm. Bài bị kết thúc vì vi phạm được tô đỏ riêng." | "Kết quả ca thi", dòng `b24dcce098` có điểm và 1 lần vi phạm |
| Chỉ vào bảng "Tỷ lệ trọn điểm từng câu (câu khó nhất ở trên)" | "Câu khó nhất xếp trên cùng: giảng viên biết ngay câu nào cả lớp chưa hiểu." | Bảng tỷ lệ theo câu |
| **[cắt]** Xuất CSV… → lưu ra Desktop → mở bằng Excel | "Xuất CSV mở thẳng bằng Excel, tiếng Việt không lỗi phông nhờ ghi kèm BOM UTF-8." | Excel hiện đúng tiếng Việt |
| Alt+Tab sang cửa sổ dòng lệnh MySQL (đã đăng nhập sẵn) | "**Phần cuối, và là phần nhóm em tự tin nhất.** Cửa sổ này đăng nhập MySQL bằng đúng tài khoản mà máy thí sinh dùng. Giả sử có bạn sửa mã ứng dụng thi, hoặc lấy được mật khẩu CSDL trong máy." | Dấu nhắc `mysql>` |
| Dán `SELECT * FROM answer_option;` → Enter | "Thử đọc bảng đáp án…" | `ERROR 1142 (42000): SELECT command denied to user 'oes_client'@'…' for table 'answer_option'` |
| Dán `UPDATE submission SET total_score = 10 WHERE id = 1;` → Enter | "…và thử tự cho mình 10 điểm." | `ERROR 1143 (42000): UPDATE command denied to user 'oes_client'@'…' for column 'total_score' in table 'submission'` |
| (để yên) | "Cả hai đều bị **chính MySQL** từ chối. **Kể cả sửa mã ứng dụng thi cũng không lấy được đáp án: người từ chối là MySQL, không phải giao diện.** Và đây không phải thử một lần cho demo: **9 kiểm thử tự động làm đúng việc này mỗi lần chạy.** **Quay lại slide.**" | |

---

## C. Chốt và mời hỏi đáp (slide 9, 30 giây)

| DM bấm | TT nói |
|---|---|
| Slide 9 (tổng kết, "Hỏi đáp") | "Thầy vừa thấy: phân quyền nằm ở service, luật nằm trong lớp, gian lận được ghi nhận theo giờ máy chủ, điểm có ngay khi nộp, và máy thí sinh không đọc được đáp án dù bị sửa mã." |
| | "Giới hạn chúng em biết rõ: mọi máy thí sinh dùng chung một tài khoản CSDL. Muốn CSDL phân biệt được từng thí sinh thì cần thêm một máy chủ ứng dụng ở giữa, nằm ngoài công nghệ môn học cho phép." |
| | "Nhóm 2 xin cảm ơn thầy và các bạn đã lắng nghe. Chúng em xin nhận câu hỏi." |

Trong lúc hỏi đáp: để slide 9 trên màn hình, **giữ nguyên các ứng dụng đang mở**. Giảng viên muốn xem gì thêm thì DM
làm theo [Phần mở rộng](#phần-mở-rộng-nếu-còn-giờ-hoặc-giảng-viên-yêu-cầu).

---

## Slide gợi ý

| Slide | Nội dung | Lấy từ |
|:-:|---|---|
| 1 | Tên đề tài, Nhóm 2, bốn thành viên, giảng viên | `README.md` |
| 2 | Ba nỗi lo khi thi trong phòng máy và cách hệ thống xử lý | A1 |
| 3 | Sơ đồ triển khai: 1 MySQL, StaffApp (máy chấm), N máy ExamClientApp, hai tài khoản CSDL | `README.md`, mục Kiến trúc |
| 4 | Ba tầng view → service → DAO; DAO có hai cài đặt (JDBC, bộ nhớ); `AppContext`/`ClientContext` lắp ráp | `docs/DIAGRAMS.md` |
| 5 | OOP: sơ đồ lớp `Question` và 4 lớp con, `User` và 3 lớp con; bốn tính chất gắn với lớp; ba mẫu thiết kế; SOLID | `docs/DIAGRAMS.md` (sơ đồ lớp) |
| 6 | Ba lớp bảo vệ (tài khoản CSDL, trigger, service) và các quy tắc nghiệp vụ đã chốt | `CLAUDE.md`, `docs/DIAGRAMS.md` (phân quyền) |
| 7 | Kiểm thử: 135 (112 không cần MySQL + 23 với MySQL thật, 9 bằng `oes_client`); MySQL 8 và MariaDB 10.6; tải 60 thí sinh: lưu câu p95 31 ms, chấm 60 bài 2,2 s | `docs/load-test.md`; ảnh chụp kết quả kiểm thử (mục Chuẩn bị) |
| 8 | "Demo": sáu điều sẽ chứng minh, mỗi dòng một câu | Bảng sáu điểm sáng |
| 9 | Tổng kết sáu điểm (đã chứng minh), giới hạn, "Cảm ơn – Hỏi đáp" | C |
| Dự phòng | 6 ảnh giao diện | `docs/images/` |

Slide chỉ ghi ý ngắn; lời giải thích nằm trong lời TT. Không chép nguyên câu nói lên slide.

---

## Phần mở rộng (nếu còn giờ hoặc giảng viên yêu cầu)

Mỗi mục khoảng 40 giây. Thường dùng trong lúc hỏi đáp, khi giảng viên hỏi "cho xem thử…".

- **Vào lại làm tiếp bài cũ.** ExamClientApp: đăng xuất → đăng nhập `b24dcce007` → Vào thi ca "Demo thuyết trình" →
  trả lời 2 câu → Ctrl+Shift+Esc mở Task Manager (bị tính một lần vi phạm) → mở rộng các dòng Java ("Java(TM)
  Platform SE binary" hoặc "OpenJDK Platform binary"), chọn đúng dòng có cửa sổ "Hệ thống thi trực tuyến – Thi" →
  End task. **Không tắt nhầm dòng "– Quản lý": đó là máy chấm.** Chạy lại `scripts\start-client.bat` → đăng nhập
  `b24dcce007` → nút "Tiếp tục làm bài (đã vi phạm 1 lần)" → hai câu đã trả lời vẫn còn.
  TT: "Mất điện, treo máy, đổi sang máy khác cũng vậy: bài vẫn còn, giờ vẫn tính theo giờ đóng ca chung."
- **Đủ 5 lần vi phạm.** Đăng nhập `b24dcce021` → Vào thi → Alt+Tab ra vào 5 lần, mỗi lần cách nhau hơn 3 giây → bài bị
  kết thúc. Sang StaffApp → Xem kết quả → Làm mới: dòng `b24dcce021` tô đỏ, vẫn có điểm phần đã làm.
- **Nghiệp vụ chạy không cần MySQL** (bằng chứng cho DIP). Cửa sổ dòng lệnh tại thư mục dự án: `mvnw.cmd -q compile
  exec:java`. ConsoleDemo chạy toàn bộ kịch bản tạo đề, thi, chấm trên DAO trong bộ nhớ.
  TT: "Cùng một service, chỉ thay cài đặt DAO. Service không hề biết dữ liệu nằm ở đâu."

---

## Sự cố và cách xử lý

TT không xin lỗi dài dòng. Nói một câu, rồi nói tiếp nội dung trong lúc DM xử lý.

| Sự cố | TT nói | DM làm |
|---|---|---|
| Phần slide bị lố giờ | (không nói gì, chỉ nói ngắn lại) | Bỏ mọi dòng **[cắt]** trong demo; giữ nguyên B4 |
| Ứng dụng báo không kết nối được CSDL | "Máy chủ CSDL đang khởi động lại; trong lúc chờ, em nói thêm về kiến trúc." | Mở dòng lệnh: `docker start oes-mysql`, chờ 20 giây, đăng nhập lại |
| Ứng dụng thi không thấy ca "Demo thuyết trình" | "Ca vừa mở, em làm mới danh sách." | Bấm "Làm mới" ở danh sách ca. Vẫn không có: kiểm tra B2 đã chọn lớp `INT1332-01` |
| Alt+Tab không hiện cảnh báo vi phạm | "Trên máy chiếu, Windows đôi khi không chuyển cửa sổ; logic gộp và đếm vi phạm có kiểm thử riêng." | Bỏ qua, làm tiếp bước Nộp bài |
| Nộp xong quá 10 giây chưa có điểm | "Máy chấm nằm ở ứng dụng quản lý; em kiểm tra nó đang chạy." | Alt+Tab sang StaffApp, xem thanh dưới có "Máy chấm đang chạy"; Ca thi → Xem kết quả → Làm mới |
| Cửa sổ thi treo, không thoát được | "Em khởi động lại ứng dụng thi; bài đã lưu từng câu nên không mất." | Ctrl+Shift+Esc → End task dòng "– Thi" (không phải "– Quản lý") |
| Hỏng hẳn, không cứu được trong 1 phút | "Chúng em chuyển sang ảnh chụp màn hình của đúng các bước này." | Mở slide dự phòng |

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
