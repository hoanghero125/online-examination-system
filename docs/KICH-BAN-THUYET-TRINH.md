# Kịch bản thuyết trình sản phẩm (1 người demo, 1 người thuyết trình)

[README](../README.md) · [Phân công](PHAN-CONG.md) · [Tài liệu vấn đáp](vandap/) · [Chạy trên máy cá nhân](SETUP.md) · [Dựng phòng máy](RUNBOOK.md) · [Kịch bản demo](DEMO-SCRIPT.md)

Dùng cho buổi thuyết trình sản phẩm trước giảng viên: **một người chỉ thao tác máy (gọi là DM)** và **một người nói từ
đầu tới cuối (gọi là TT)**, cả buổi trên **một laptop** nối máy chiếu. Thời lượng **12 phút**, cộng thêm phần hỏi đáp.
Bỏ các dòng có ghi **[cắt]** thì còn khoảng 10 phút. Kịch bản demo chia theo bốn phần để vấn đáp nằm ở
[`DEMO-SCRIPT.md`](DEMO-SCRIPT.md).

Mục lục: [Sáu điểm sáng](#sáu-điểm-sáng--phải-thuộc) · [Phân vai](#phân-vai-và-quy-ước) · [Chuẩn bị](#chuẩn-bị) ·
[Dòng thời gian](#dòng-thời-gian) · [Kịch bản](#phần-1--mở-đầu-slide-12-1-phút) · [Slide](#slide-gợi-ý) ·
[Phần mở rộng](#phần-mở-rộng-nếu-còn-giờ-hoặc-giảng-viên-yêu-cầu) · [Sự cố](#sự-cố-và-cách-xử-lý) ·
[Hỏi đáp](#câu-hỏi-hay-gặp)

---

## Sáu điểm sáng – phải thuộc

Điều làm buổi thuyết trình khác "kể tính năng": **mỗi điểm sáng đều được chứng minh ngay trên màn hình**. Đúng lúc màn
hình hiện bằng chứng, TT nói nguyên văn **câu chốt**. Trong kịch bản, chỗ chứng minh được đánh dấu **[ĐIỂM SÁNG n]**.

| # | Điểm sáng | Chứng minh ở đâu | Câu chốt |
|:-:|---|---|---|
| 1 | **Máy thí sinh "mù đáp án"**: bảo mật nằm ở CSDL, không chỉ ở giao diện | Phần 6: gõ SQL bằng chính tài khoản của máy thí sinh, đọc đáp án và tự sửa điểm đều bị MySQL từ chối | "Kể cả sửa mã ứng dụng thi cũng không lấy được đáp án: người từ chối là MySQL, không phải giao diện." |
| 2 | **Hướng đối tượng thật**: luật nghiệp vụ nằm trong lớp, giao diện không kiểm tra gì | Phần 4: câu một đáp án đánh dấu 2 đáp án đúng bị lớp `SingleChoiceQuestion` từ chối; đề chọn cách chấm (mẫu Strategy) | "Controller không kiểm tra gì cả. Luật 'đúng một đáp án' sống trong lớp con của `Question`." |
| 3 | **Phân quyền ở tầng service**, không phải ẩn nút | Phần 3: tài khoản quản trị đăng nhập ứng dụng thi bị từ chối | "Ẩn nút chưa phải là phân quyền. Mọi kiểm tra quyền nằm ở service." |
| 4 | **Chống gian lận và giờ chuẩn từ máy chủ** | Phần 5: toàn màn hình, rời cửa sổ bị ghi vi phạm "lần 1/5"; đồng hồ đếm theo giờ MySQL | "Chỉnh đồng hồ máy thí sinh cũng không được thêm phút nào: giờ thi lấy từ máy chủ." |
| 5 | **Tách máy chấm khỏi máy thí sinh**, có điểm gần như ngay lập tức | Phần 5–6: nộp bài, vài giây sau có điểm; giảng viên thấy bảng điểm và câu khó nhất | "Máy thí sinh không chấm bài, vì nó không có đáp án để chấm." |
| 6 | **Kiểm chứng bằng số**: 135 kiểm thử, chạy trên MySQL và MariaDB, thử tải 60 thí sinh | Phần 7 (slide) | "9 kiểm thử đóng vai ứng dụng thi bị sửa, dùng chính tài khoản của máy thí sinh để tấn công CSDL." |

Hai ý TT nên lồng vào lời nói vì giảng viên môn OOP chấm đúng những chỗ này: **ba mẫu thiết kế** (Factory, Strategy,
Template Method) và **SOLID** (SRP, ISP, DIP). Cả hai được nhắc lúc demo và tóm lại ở slide 6.

---

## Phân vai và quy ước

- **DM** ngồi máy và điều khiển mọi thứ trên laptop, kể cả chuyển slide. Làm đúng thứ tự cột "DM bấm", không nói.
- **TT** đứng cạnh màn chiếu, nói theo cột "TT nói", chỉ tay vào màn hình khi tới cột "Màn hình phải hiện".
- **DM bấm trước, TT nói sau**: TT chỉ nói câu của bước nào khi màn hình đã hiện đúng như cột thứ ba. Máy chậm thì TT
  dùng **câu đệm** ghi ở đầu mỗi phần, không im lặng và không giục DM.
- **Câu hiệu chuyển phần**: TT nói "**Mời thầy và các bạn cùng xem**" thì DM chuyển từ slide sang ứng dụng. TT nói
  "**Quay lại slide**" thì DM về slide.
- **Trong lúc thi (phần 5), DM không chạm vào cửa sổ nào khác**, trừ đúng một lần Alt+Tab đã ghi trong kịch bản.

---

## Chuẩn bị

### Hôm trước (bắt buộc)

- [ ] Chạy trọn kịch bản **hai lần** trên đúng laptop và đúng máy chiếu, bấm giờ từng phần.
- [ ] **Thử Alt+Tab khi đang thi**: phải hiện hộp thoại "Cảnh báo vi phạm quy chế". Thử Esc và Alt+F4: không thoát được.
      Cơ chế này mới kiểm tra bằng giả lập (xem `PROGRESS.md`, "Câu hỏi mở"), nên **phải tự thử trên Windows**.
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
- [ ] Mở Notepad chứa sẵn hai lệnh SQL ở phần 6 và nội dung câu hỏi ở phần 4, để chép thay vì gõ trước khán giả.
- [ ] Sắp thứ tự cửa sổ: slide (trên cùng), StaffApp, ExamClientApp, dòng lệnh MySQL, Notepad.
- [ ] Tuyệt đối không dùng `b24dcce098` bấm "Vào thi" trước buổi: thí sinh này để dành cho phần 5.

| Vai trò | Tên đăng nhập | Mật khẩu | Dùng ở phần |
|---|---|---|---|
| Quản trị viên | `admin` | `admin123` | 3 |
| Giảng viên | `gv.viet` | `gv123456` | 4, 6 |
| Thí sinh | `b24dcce098` | `sv123456` | 5 |

---

## Dòng thời gian

| Phút | Phần | Màn hình | Điểm sáng |
|---|---|---|---|
| 0:00 | 1. Mở đầu | Slide 1–2 | |
| 1:00 | 2. Kiến trúc | Slide 3–5 | báo trước 1 |
| 2:30 | 3. Demo: phân quyền | StaffApp, ExamClientApp | 3 |
| 3:15 | 4. Demo: giảng viên | StaffApp | 2 |
| 5:45 | 5. Demo: thí sinh làm bài | ExamClientApp | 4, 5 |
| 8:30 | 6. Demo: kết quả và bảo mật | StaffApp, dòng lệnh MySQL | 5, **1** |
| 10:30 | 7. Chất lượng | Slide 6–7 | 2, 6 |
| 11:30 | 8. Kết luận | Slide 8 | |
| 12:00 | Hỏi đáp | | |

---

## Phần 1 · Mở đầu (slide 1–2, 1 phút)

| DM bấm | TT nói |
|---|---|
| Slide 1 | "Kính chào thầy và các bạn. Nhóm 2 xin trình bày đề tài 16: hệ thống thi trắc nghiệm trực tuyến cho phòng máy." |
| Slide 2 | "Thi trong phòng máy có ba nỗi lo: lộ đáp án, gian lận khi làm bài, và chấm điểm chậm. Hệ thống của chúng em xử lý cả ba: máy thí sinh không bao giờ giữ đáp án, bài làm toàn màn hình có giám sát, và điểm có ngay khi nộp." |
| | "Hôm nay chúng em sẽ không chỉ kể tính năng, mà chứng minh trực tiếp trên màn hình sáu điểm nhóm tự tin nhất." |

## Phần 2 · Kiến trúc (slide 3–5, 1 phút 30)

| DM bấm | TT nói |
|---|---|
| Slide 3 (sơ đồ triển khai) | "Một MySQL chung trong mạng LAN và hai ứng dụng desktop JavaFX. Ứng dụng quản lý dành cho quản trị viên và giảng viên, đồng thời là máy chấm bài. Ứng dụng thi cài trên máy thí sinh." |
| | "Điểm mấu chốt: hai ứng dụng đăng nhập MySQL bằng **hai tài khoản khác nhau**. Tài khoản của máy thí sinh chỉ đọc được các khung nhìn đã lọc bỏ đáp án. Lát nữa chúng em sẽ thử tấn công chính tài khoản đó." |
| Slide 4 (ba tầng) | "Mỗi ứng dụng chia ba tầng: giao diện, service, DAO. Controller giao diện không chứa nghiệp vụ. Service chỉ biết interface DAO, và mỗi interface có hai cài đặt: JDBC cho MySQL, bộ nhớ cho kiểm thử. Vì vậy cùng một nghiệp vụ chạy được cả khi không có MySQL – đó là nguyên lý đảo ngược phụ thuộc, chữ D trong SOLID." |
| Slide 5 (sáu điểm sáng) | "Giờ thi luôn lấy từ máy chủ MySQL, không bao giờ từ đồng hồ máy thí sinh. Đây là sáu điều chúng em sẽ chứng minh. **Mời thầy và các bạn cùng xem.**" |

## Phần 3 · Demo: phân quyền (45 giây) – [ĐIỂM SÁNG 3]

Câu đệm: "Trong lúc chờ, xin nói thêm: mỗi ứng dụng chỉ nhận đúng vai trò của mình."

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| StaffApp: đăng nhập `admin` / `admin123` | "Đây là ứng dụng quản lý, đăng nhập bằng tài khoản quản trị viên." | Màn hình chính, menu trái có "Người dùng", "Môn học & lớp" |
| Người dùng → gõ `b24dcce0` vào ô tìm → Tìm | "Quản trị viên tìm, khóa tài khoản và nhập cả lớp từ tệp CSV. Đây là 40 sinh viên lớp D24CQCE07-B, lớp của chính chúng em." | Danh sách các tài khoản `b24dcce0…` |
| **[cắt]** Môn học & lớp → chọn INT1332 | "Môn học, lớp học phần, phân công giảng viên. Giảng viên chỉ quản lý được môn mình được phân công." | Môn INT1332 và lớp INT1332-01 |
| Alt+Tab sang ExamClientApp → đăng nhập `admin` / `admin123` | "Giờ thử một việc: dùng chính tài khoản quản trị này đăng nhập vào **ứng dụng thi**." | Thông báo lỗi: "Tài khoản giảng viên và quản trị viên hãy dùng ứng dụng quản lý (StaffApp)." |
| (để yên) | "Bị từ chối. Kiểm tra này nằm ở tầng service: ai đó tự viết giao diện khác, gọi thẳng service cũng bị chặn. **Ẩn nút chưa phải là phân quyền. Mọi kiểm tra quyền nằm ở service.**" | |
| Alt+Tab về StaffApp → Đăng xuất | | Màn hình đăng nhập |

## Phần 4 · Demo: giảng viên (2 phút 30) – [ĐIỂM SÁNG 2]

Câu đệm: "Các thao tác này chạy nền, giao diện không bao giờ đứng hình khi chờ CSDL."

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| StaffApp: đăng nhập `gv.viet` / `gv123456` → Ngân hàng câu hỏi | "Giảng viên Việt phụ trách môn Lập trình hướng đối tượng. Ngân hàng có 60 câu, đủ bốn loại: một đáp án, nhiều đáp án, đúng/sai, điền khuyết, chia theo chương và mức độ." | Bảng 60 câu |
| Thêm câu hỏi → giữ loại "Một đáp án" → nội dung: `Lớp trừu tượng có tạo đối tượng trực tiếp được không?` → bốn phương án: `Được`, `Không`, `Chỉ khi có constructor`, `Chỉ trong lớp con` → tick **Đúng** ở cả `Được` và `Không` → Lưu câu hỏi | "Em cố tình nhập sai: câu một đáp án nhưng đánh dấu hai đáp án đúng." | Lỗi cạnh ô phương án: "Câu một đáp án phải có đúng 1 phương án đúng, hiện có 2." |
| (để yên) | "Thông báo này **không do giao diện kiểm tra**. Bốn loại câu là bốn lớp con của lớp trừu tượng `Question`, và luật 'đúng một đáp án' nằm trong constructor của `SingleChoiceQuestion`. Giao diện chỉ hiện lỗi mà lớp đó ném ra, nên không bao giờ có một câu hỏi sai nằm trong hệ thống. **Controller không kiểm tra gì cả. Luật sống trong lớp con của `Question`.**" | |
| Bỏ tick `Được` → Lưu câu hỏi | "Sửa lại cho đúng. Tạo đối tượng lớp con nào là việc của `QuestionFactory`, dựa theo loại câu – mẫu Factory." | Câu mới xuất hiện trong bảng |
| Đề thi → Tạo đề mới → Tên đề: `Demo thuyết trình` → ô "Câu nhiều đáp án" giữ "Theo tỷ lệ: đúng phần nào được phần đó" | "Tạo đề thi. Câu nhiều đáp án chấm toàn phần hay chấm theo tỷ lệ là lựa chọn của từng đề. Mỗi cách chấm là một lớp cài đặt interface `GradingStrategy` – mẫu Strategy. Thêm một cách chấm mới không phải sửa dòng nào của lớp cũ." | Màn hình "Tạo đề thi" |
| Thẻ "Theo ma trận" → nhập Chương 1 / Dễ = `3`, Chương 2 / Trung bình = `3`, Chương 3 / Khó = `10` → Kiểm tra ma trận | "Thay vì chọn từng câu, giảng viên nhập ma trận: chương nào, mức nào, bao nhiêu câu. Em cố tình xin 10 câu khó ở chương 3." | "Thiếu câu: chương 3 mức Khó: cần 10, có 7." |
| Sửa Chương 3 / Khó thành `2` → Kiểm tra ma trận → Tạo đề | "Ngân hàng chỉ có 7 câu khó ở chương 3, hệ thống báo thiếu **trước** khi sinh đề. Sửa thành 2 thì đủ; câu được bốc ngẫu nhiên." | "Đủ câu: ma trận lấy 8 câu.", rồi hộp thoại "Đã tạo đề…" |
| Chọn đề "Demo thuyết trình" → Mở ca thi với đề này… → ô Lớp chọn `INT1332-01` → giữ nguyên giờ → Mở ca thi | "Mở ca thi cho cả lớp. Giờ mở mặc định là ngay bây giờ **theo giờ máy chủ**, kéo dài 60 phút. Mọi thí sinh dùng chung giờ mở và giờ đóng: vào muộn thì còn ít thời gian hơn. Mở ca xong đề bị khóa, để không ai sửa được đề khi đang có người thi." | "Đã mở ca thi #… cho 40 thí sinh… Đề "Demo thuyết trình" đã bị khóa…" |
| Đóng hộp thoại. **Giữ StaffApp đăng nhập, không đóng** | "Ứng dụng quản lý cứ để mở: nó đang là máy chấm bài." | Danh sách ca thi |

## Phần 5 · Demo: thí sinh làm bài (2 phút 45) – [ĐIỂM SÁNG 4, 5]

Câu đệm: "Mọi câu trả lời đều được lưu xuống CSDL ngay, nên mất điện cũng không mất bài."

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| Alt+Tab sang ExamClientApp → đăng nhập `b24dcce098` / `sv123456` | "Bây giờ là phía thí sinh – mã sinh viên của bạn Hoàng trong nhóm." | "Ca thi của bạn", có ca "Demo thuyết trình" đang mở |
| Ở dòng ca "Demo thuyết trình" → Vào thi | "Từ đăng nhập tới làm bài chỉ hai thao tác." | "Xác nhận quy chế thi" |
| Tick "Tôi đã đọc và đồng ý với quy chế thi" → Bắt đầu làm bài | "Thí sinh đồng ý quy chế, và cửa sổ chiếm toàn màn hình." | Toàn màn hình, đồng hồ "Thời gian còn lại", lưới "Danh sách câu" |
| **[cắt]** Bấm Esc | "Esc không thoát được, Alt+F4 cũng vậy." | Vẫn toàn màn hình |
| Trả lời 3–4 câu (chọn phương án; nếu gặp câu điền khuyết thì gõ một từ), chuyển câu bằng "Câu sau ▶" hoặc bấm số câu ở lưới | "Mỗi câu trả lời được lưu xuống CSDL nửa giây sau khi chọn. Máy tắt đột ngột thì vào lại vẫn còn nguyên bài. Mỗi thí sinh chỉ có **một** bài trong một ca: vào lại là làm tiếp, không tạo bài mới." | Ô câu đã trả lời đổi màu |
| Bấm "Đánh dấu xem lại" ở một câu | "Câu chưa chắc thì đánh dấu để xem lại." | Ô câu đó đổi màu đánh dấu |
| **Alt+Tab ra ngoài, chờ một giây, Alt+Tab quay lại** | "Bây giờ em thử gian lận: rời cửa sổ làm bài để tra cứu." | Hộp thoại "Cảnh báo vi phạm quy chế": "Bạn đã rời cửa sổ làm bài (lần 1/5). Còn 4 lần nữa bài thi sẽ bị kết thúc." |
| (để hộp thoại trên màn hình) | "Mỗi lần rời cửa sổ là một lần vi phạm, ghi thẳng xuống CSDL. Đủ 5 lần thì bài bị kết thúc và khóa, vẫn chấm phần đã làm. Các lần rời cách nhau dưới 3 giây được gộp làm một, để không phạt oan khi máy giật. Đồng hồ đếm ngược thì tính theo giờ MySQL. **Chỉnh đồng hồ máy thí sinh cũng không được thêm phút nào: giờ thi lấy từ máy chủ.**" | |
| Quay lại làm bài → Nộp bài | "Nộp bài." | "Còn … câu chưa trả lời, 1 câu đang đánh dấu xem lại. Nộp xong sẽ không sửa được nữa. Bạn chắc chắn nộp bài?" |
| Xác nhận nộp | "Hệ thống nhắc số câu còn trống trước khi nộp." | "Đã nộp bài. Đang chấm bài…", vài giây sau hiện điểm "… / 8" |
| (chờ điểm hiện) | "**Máy thí sinh không chấm bài, vì nó không có đáp án để chấm.** Bài được chấm ở máy giảng viên, 3 giây một lượt, nên điểm về đây gần như ngay lập tức." | Điểm của bài |

## Phần 6 · Demo: kết quả và bảo mật (2 phút) – [ĐIỂM SÁNG 5, 1]

Câu đệm: "Bảng điểm này đọc trực tiếp từ CSDL, không phải từ máy thí sinh gửi lên."

| DM bấm | TT nói | Màn hình phải hiện |
|---|---|---|
| Alt+Tab sang StaffApp → (đang ở Ca thi) chọn ca "Demo thuyết trình" → Xem kết quả → Làm mới | "Sang phía giảng viên. Bài vừa nộp đã có điểm, kèm số lần vi phạm. Bài bị kết thúc vì vi phạm được tô đỏ riêng." | "Kết quả ca thi", dòng `b24dcce098` có điểm và 1 lần vi phạm |
| Chỉ vào bảng "Tỷ lệ trọn điểm từng câu (câu khó nhất ở trên)" | "Bên dưới là thống kê và tỷ lệ trọn điểm từng câu, câu khó nhất xếp trên cùng. Giảng viên biết ngay câu nào cả lớp chưa hiểu." | Bảng tỷ lệ theo câu |
| **[cắt]** Xuất CSV… → lưu ra Desktop → mở bằng Excel | "Xuất CSV mở thẳng bằng Excel, tiếng Việt không lỗi phông nhờ ghi kèm BOM UTF-8." | Excel hiện đúng tiếng Việt |
| Alt+Tab sang cửa sổ dòng lệnh MySQL (đã đăng nhập sẵn) | "**Đây là phần nhóm em tự tin nhất.** Cửa sổ này đăng nhập MySQL bằng đúng tài khoản mà máy thí sinh dùng. Giả sử có bạn sửa mã ứng dụng thi, hoặc lấy được mật khẩu CSDL trong máy." | Dấu nhắc `mysql>` |
| Dán `SELECT * FROM answer_option;` → Enter | "Thử đọc bảng đáp án…" | `ERROR 1142 (42000): SELECT command denied to user 'oes_client'@'…' for table 'answer_option'` |
| Dán `UPDATE submission SET total_score = 10 WHERE id = 1;` → Enter | "…và thử tự cho mình 10 điểm." | `ERROR 1143 (42000): UPDATE command denied to user 'oes_client'@'…' for column 'total_score' in table 'submission'` |
| (để yên) | "Cả hai đều bị **chính MySQL** từ chối. Tài khoản máy thí sinh chỉ đọc được các khung nhìn đã lọc bỏ đáp án, chỉ ghi được đúng các cột cần để làm bài, và trigger chặn mọi lần ghi khi bài đã nộp hoặc ca đã đóng. **Kể cả sửa mã ứng dụng thi cũng không lấy được đáp án: người từ chối là MySQL, không phải giao diện.** **Quay lại slide.**" | |

## Phần 7 · Chất lượng (slide 6–7, 1 phút) – [ĐIỂM SÁNG 2, 6]

| DM bấm | TT nói |
|---|---|
| Slide 6 (OOP) | "Về hướng đối tượng. **Trừu tượng**: `Question` và `User` là lớp trừu tượng, DAO là interface. **Kế thừa**: bốn loại câu hỏi, ba vai trò người dùng, hai ứng dụng cùng kế thừa `OesApplication`. **Đa hình**: mỗi loại câu tự chấm chính nó qua `isCorrect` và `correctRatio`. **Đóng gói**: dữ liệu được kiểm tra ngay trong constructor, collection trả ra là bản chỉ đọc. Ba mẫu thiết kế: Factory tạo câu hỏi, Strategy để chấm, Template Method cho khung hai ứng dụng. Về SOLID: tách `Submission` khỏi `ExamSession` theo SRP, tách DAO theo người dùng theo ISP, service chỉ biết interface theo DIP." |
| Slide 7 (kiểm thử) | "135 kiểm thử tự động JUnit 5, trong đó 23 kiểm thử chạy với MySQL thật. **9 kiểm thử đóng vai ứng dụng thi bị sửa, dùng chính tài khoản của máy thí sinh để tấn công CSDL** – đúng những gì thầy vừa thấy. Toàn bộ chạy được trên cả MySQL 8 và MariaDB 10.6. Thử tải 60 thí sinh làm bài cùng lúc: 95% số lần lưu câu trả lời chỉ mất khoảng 31 mili giây; chấm 60 bài mất 2,2 giây." |

## Phần 8 · Kết luận (slide 8, 30 giây)

| DM bấm | TT nói |
|---|---|
| Slide 8 | "Tóm lại: máy thí sinh không bao giờ giữ đáp án, giờ thi theo máy chủ, gian lận được ghi nhận, điểm có ngay khi nộp, và mỗi điều đó đều có kiểm thử tự động đi kèm." |
| | "Giới hạn chúng em biết rõ: mọi máy thí sinh dùng chung một tài khoản CSDL. Muốn CSDL phân biệt được từng thí sinh thì cần thêm một máy chủ ứng dụng ở giữa, nằm ngoài công nghệ môn học cho phép." |
| | "Nhóm 2 xin cảm ơn thầy và các bạn đã lắng nghe. Chúng em xin nhận câu hỏi." |

---

## Slide gợi ý

| Slide | Nội dung | Lấy từ |
|:-:|---|---|
| 1 | Tên đề tài, Nhóm 2, bốn thành viên, giảng viên | `README.md` |
| 2 | Ba nỗi lo khi thi trong phòng máy và cách hệ thống xử lý | Phần 1 ở trên |
| 3 | Sơ đồ triển khai: 1 MySQL, StaffApp (máy chấm), N máy ExamClientApp, hai tài khoản CSDL | `README.md`, mục Kiến trúc |
| 4 | Ba tầng view → service → DAO; DAO có hai cài đặt (JDBC, bộ nhớ); `AppContext`/`ClientContext` lắp ráp | `docs/DIAGRAMS.md` |
| 5 | Sáu điểm sáng (chỉ tên, mỗi dòng một câu) | Bảng đầu tài liệu này |
| 6 | OOP: sơ đồ lớp `Question` và 4 lớp con, `User` và 3 lớp con; ba mẫu thiết kế; SOLID | `docs/DIAGRAMS.md` (sơ đồ lớp) |
| 7 | Kiểm thử: 135 (112 không cần MySQL + 23 với MySQL thật, 9 bằng `oes_client`); MySQL 8 và MariaDB 10.6; tải 60 thí sinh: lưu câu p95 31 ms, chấm 60 bài 2,2 s | `docs/load-test.md`; chụp màn hình kết quả `scripts\test.bat --db` |
| 8 | Kết luận, giới hạn, cảm ơn | Phần 8 ở trên |
| Dự phòng | 6 ảnh giao diện | `docs/images/` |

Slide chỉ ghi ý ngắn; lời giải thích nằm trong lời TT. Không chép nguyên câu nói lên slide.

---

## Phần mở rộng (nếu còn giờ hoặc giảng viên yêu cầu)

Mỗi mục khoảng 40 giây, làm **sau** phần 6 để không làm hỏng luồng chính.

- **Vào lại làm tiếp bài cũ.** ExamClientApp: đăng xuất → đăng nhập `b24dcce007` → Vào thi ca "Demo thuyết trình" →
  trả lời 2 câu → Ctrl+Shift+Esc mở Task Manager (bị tính một lần vi phạm) → mở rộng các dòng Java ("Java(TM)
  Platform SE binary" hoặc "OpenJDK Platform binary"), chọn đúng dòng có cửa sổ "Hệ thống thi trực tuyến – Thi" →
  End task. **Không tắt nhầm dòng "– Quản lý":
  đó là máy chấm.** Chạy lại `scripts\start-client.bat` → đăng nhập `b24dcce007` → nút "Tiếp tục làm bài (đã vi phạm
  1 lần)" → hai câu đã trả lời vẫn còn.
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
| Ứng dụng báo không kết nối được CSDL | "Máy chủ CSDL đang khởi động lại; trong lúc chờ, em nói thêm về kiến trúc." | Mở dòng lệnh: `docker start oes-mysql`, chờ 20 giây, đăng nhập lại |
| Ứng dụng thi không thấy ca "Demo thuyết trình" | "Ca vừa mở, em làm mới danh sách." | Bấm "Làm mới" ở danh sách ca. Vẫn không có: kiểm tra phần 4 đã chọn lớp `INT1332-01` |
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
  service (bỏ qua giao diện) vẫn bị chặn. Phần 3 vừa chứng minh.
- **"Sao không dùng Spring, Hibernate?"** → Giảng viên yêu cầu JDBC thuần. Nhờ có interface DAO, đổi cách lưu trữ chỉ
  là thêm một cài đặt mới, service không phải sửa.
- **"Máy chủ dùng MariaDB thay MySQL có chạy không?"** → Có, từ MariaDB 10.6: đủ 135 kiểm thử đã chạy trên 10.6.28.
  XAMPP bản cũ đi kèm MariaDB 10.4, bản đó chưa thử.
- **"Mất điện giữa giờ?"** → Câu trả lời đã lưu từng câu. Đăng nhập lại, kể cả ở máy khác, là làm tiếp; giờ vẫn tính
  theo giờ đóng ca chung.
- **"Giới hạn còn lại?"** → Mọi máy thí sinh dùng chung tài khoản `oes_client`, nên ứng dụng thi bị sửa vẫn đọc/ghi
  được câu trả lời của thí sinh khác đang làm bài (không đọc được đáp án, không sửa được điểm). Chặn hẳn cần một máy
  chủ ứng dụng ở giữa, ngoài công nghệ được phép.
