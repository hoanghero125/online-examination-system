# Chạy dự án trên máy cá nhân

Hướng dẫn cho thành viên mở dự án lần đầu trên laptop (Windows; Linux/macOS làm tương tự). Làm xong sẽ có MySQL
chạy trên chính máy mình, dữ liệu mẫu, và mở được cả hai ứng dụng. Dựng phòng máy cho buổi thi thật (nhiều máy,
mạng LAN, tệp jar) xem [`RUNBOOK.md`](RUNBOOK.md).

Mất khoảng 30 phút, phần lâu nhất là cài MySQL.

| Bước | Việc | Kiểm tra xong khi |
|---|---|---|
| 1 | Cài JDK 17, IntelliJ, MySQL 8 | `java -version` và `mysql --version` chạy được |
| 2 | Tạo CSDL `oes` | `SHOW TABLES` ra danh sách bảng |
| 3 | Mở dự án trong IntelliJ | chạy được ConsoleDemo |
| 4 | Tạo tệp cấu hình `db-staff.properties` | có tệp ở thư mục dự án |
| 5 | Tạo dữ liệu mẫu | SeedTool in ra bảng tài khoản |
| 6 | Mở StaffApp | đăng nhập `gv.viet` được |
| 7 | Mở ExamClientApp | đăng nhập `b24dcce001`, thấy ca thi |
| 8 | (Tùy chọn) Chạy kiểm thử với MySQL thật | 126 kiểm thử, không bỏ qua ca nào |

---

## 1. Cài phần mềm

- **JDK 17 trở lên** – ví dụ Eclipse Temurin 17 (https://adoptium.net). Lúc cài, chọn *Set JAVA_HOME* và
  *Add to PATH*. Mở cửa sổ dòng lệnh mới, gõ `java -version`, phải thấy `17` (hoặc 21).
- **IntelliJ IDEA** (bản Community là đủ).
- **MySQL 8.0** – trên Windows dùng *MySQL Installer*, chọn *Server only* hoặc *Developer Default* (có kèm MySQL
  Workbench). Khi cài:
  - *Authentication Method*: giữ mặc định (*Use Strong Password Encryption*);
  - đặt mật khẩu cho `root` và **ghi lại**;
  - giữ cổng **3306**.

  Thêm `C:\Program Files\MySQL\MySQL Server 8.0\bin` vào biến PATH để gõ được lệnh `mysql` ở bất kỳ đâu
  (Start → *Edit the system environment variables* → *Environment Variables…* → *Path* → *New*). Mở cửa sổ dòng lệnh
  mới, gõ `mysql --version` để kiểm tra.

Không cần cài Maven: dự án có sẵn `mvnw` / `mvnw.cmd`, và IntelliJ có Maven đi kèm.

## 2. Tạo CSDL

Mở **Command Prompt** (hoặc PowerShell) tại **thư mục dự án** (thư mục có tệp `pom.xml`), chạy lần lượt:

```bat
mysql -u root -p -e "CREATE DATABASE oes CHARACTER SET utf8mb4"
mysql -u root -p --default-character-set=utf8mb4 oes -e "source db/schema.sql"
mysql -u root -p --default-character-set=utf8mb4 oes -e "source db/security.sql"
```

Mỗi lệnh hỏi mật khẩu `root`. **Đúng thứ tự**: `schema.sql` tạo bảng, `security.sql` tạo khung nhìn, trigger, hai
tài khoản `oes_staff` / `oes_client` và phân quyền – thiếu tệp thứ hai thì ứng dụng thi không đăng nhập được.
`--default-character-set=utf8mb4` để trình `mysql` trên Windows đọc tệp theo UTF-8 (tệp có chú thích tiếng Việt)
thay vì bảng mã của Command Prompt.

Kiểm tra:

```bat
mysql -u oes_staff -pdoi_mat_khau_staff oes -e "SHOW TABLES"
```

Phải ra 16 dòng: 12 bảng và 4 khung nhìn `v_...`.

> Dùng MySQL Workbench thay dòng lệnh cũng được: tạo schema `oes` (charset `utf8mb4`), chọn nó làm mặc định
> (nháy đúp), rồi *File → Open SQL Script…* mở `db/schema.sql`, bấm nút tia sét để chạy; làm lại với
> `db/security.sql`.

Hai tài khoản được tạo với mật khẩu mẫu `doi_mat_khau_staff` và `doi_mat_khau_client`. Trên máy cá nhân giữ nguyên
cho tiện; **trên máy chủ phòng thi phải đổi** (xem RUNBOOK bước 1.3).

Muốn **làm lại CSDL từ đầu** (ví dụ chạy lại SeedTool): `mysql -u root -p -e "DROP DATABASE oes"` rồi chạy lại cả
bước 2.

## 3. Mở dự án trong IntelliJ

1. *File → Open…* → chọn thư mục dự án → *Trust Project*. IntelliJ tự nhận dự án Maven và tải thư viện (lần đầu mất
   vài phút, cần mạng).
2. *File → Project Structure… → Project → SDK*: chọn JDK 17 (hoặc mới hơn) đã cài ở bước 1.
3. Góc trên bên phải có sẵn các cấu hình chạy. Chọn **ConsoleDemo** → nút ▶. Chương trình chạy toàn bộ kịch bản
   nghiệp vụ trên console, không cần MySQL – chạy được tức là JDK và thư viện đã ổn.

## 4. Tệp cấu hình kết nối

Chép `config/db-staff.properties.example` thành **`db-staff.properties` ở thư mục dự án** (cạnh `pom.xml`), sửa
dòng `db.url` thành `localhost`:

```properties
db.url=jdbc:mysql://localhost:3306/oes?useUnicode=true&characterEncoding=utf8
db.user=oes_staff
db.password=doi_mat_khau_staff
```

Tệp này đã có trong `.gitignore`, không bị đưa lên Git.

Ứng dụng thi **không cần** chép tệp: lần đầu mở sẽ hiện màn hình nhập máy chủ (bước 7) và tự lưu
`db-client.properties` ở thư mục dự án.

## 5. Dữ liệu mẫu

Chọn cấu hình **SeedTool (dữ liệu mẫu)** → ▶. Console in ra:

```
Đã tạo dữ liệu mẫu:
  - 60 câu hỏi môn INT1332
  - đề "Kiểm tra giữa kỳ OOP": 20 câu, 10.0 điểm (sinh theo ma trận)
  - ca thi #1 mở từ 19:55 28/10/2026 đến 21:55 28/10/2026, 43 thí sinh lớp INT1332-01

TÀI KHOẢN
  Vai trò        Tên đăng nhập     Mật khẩu    Ghi chú
  Quản trị viên  admin             admin123
  Giảng viên     gv.viet           gv123456    phụ trách INT1332
  Giảng viên     gv.lan            gv123456    phụ trách INT1313
  Thí sinh       b24dcce001…060    sv123456    001–040 lớp INT1332-01, 041–060 lớp INT1313-01
  Thành viên     b24dcce007        sv123456    Đỗ Thị Vân Anh
  ...
```

Lưu ý:

- **Ca thi mẫu mở 2 giờ kể từ lúc chạy SeedTool.** Quá 2 giờ thì thí sinh không còn ca để thi: đăng nhập StaffApp
  bằng `gv.viet` → *Ca thi* → mở ca mới cho lớp INT1332-01.
- SeedTool chỉ chạy trên CSDL trống. Chạy lần hai sẽ báo "CSDL đã có dữ liệu mẫu" – muốn tạo lại thì làm lại bước 2.

## 6. Ứng dụng quản lý (StaffApp)

Chọn **StaffApp (quản lý)** → ▶, đăng nhập:

- `gv.viet` / `gv123456` – giảng viên: ngân hàng câu hỏi, đề thi, ca thi, kết quả;
- `admin` / `admin123` – quản trị viên: người dùng, môn học, lớp, nhật ký.

Thanh dưới cùng hiện "Máy chấm đang chạy": **StaffApp đang mở thì bài thi nộp lên mới được chấm**. Khi thử ứng dụng
thi, để StaffApp chạy song song.

## 7. Ứng dụng thi (ExamClientApp)

Chọn **ExamClientApp (thi)** → ▶. Lần đầu hiện màn hình **Kết nối máy chủ**:

| Ô | Nhập |
|---|---|
| Địa chỉ máy chủ | `localhost` |
| Cổng | `3306` |
| Tên CSDL | `oes` |
| Tài khoản CSDL | `oes_client` |
| Mật khẩu CSDL | `doi_mat_khau_client` |

Bấm **Kết nối và lưu**, rồi đăng nhập `b24dcce001` / `sv123456` → thấy ca thi mẫu → *Vào thi*.

Màn hình làm bài **chiếm toàn màn hình và không có nút đóng** – đó là tính năng chống gian lận. Muốn thoát giữa
chừng: bấm *Nộp bài*. Nếu cần dừng hẳn chương trình: Alt+Tab sang IntelliJ (bị tính một lần vi phạm) → nút ■ đỏ;
cửa sổ thi còn mở thì tắt tiến trình `java` trong Task Manager. Mở lại và đăng nhập cùng tài khoản thì làm tiếp
đúng bài cũ.

Muốn thử với thí sinh khác: đăng xuất, đăng nhập `b24dcce002`… (mỗi thí sinh chỉ thi một lần trong một ca).

## 8. (Tùy chọn) Kiểm thử với MySQL thật

`./mvnw test` (Windows: `mvnw.cmd test`) hoặc cấu hình **mvn test** chạy 104 kiểm thử không cần MySQL; 22 kiểm thử
JDBC được bỏ qua. Để chạy cả 22 ca đó, dùng một **CSDL riêng** (kiểm thử ghi thêm dữ liệu, không nên dùng `oes`):

```bat
mysql -u root -p -e "CREATE DATABASE oes_test CHARACTER SET utf8mb4"
mysql -u root -p --default-character-set=utf8mb4 oes_test -e "source db/schema.sql"
mysql -u root -p --default-character-set=utf8mb4 oes_test -e "source db/security.sql"
```

Tạo hai tệp, ví dụ `test-staff.properties` và `test-client.properties` ở thư mục dự án (giống bước 4 nhưng
CSDL là `oes_test`; tệp thứ hai dùng `oes_client` / `doi_mat_khau_client`), rồi:

```bat
set OES_TEST_DB=test-staff.properties
set OES_TEST_DB_CLIENT=test-client.properties
mvnw.cmd test
```

(PowerShell: `$env:OES_TEST_DB="test-staff.properties"`; Linux/macOS: `OES_TEST_DB=... OES_TEST_DB_CLIENT=... ./mvnw test`.)
Kết quả đúng: 126 kiểm thử, 0 lỗi, 0 bỏ qua.

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `'mysql' is not recognized as an internal or external command` | Chưa thêm thư mục `bin` của MySQL vào PATH | Bước 1; hoặc dùng MySQL Workbench |
| `release version 17 not supported` khi build | IntelliJ/Maven đang dùng JDK cũ hơn 17 hoặc chỉ có JRE | Bước 3.2; với `mvnw`, đặt `JAVA_HOME` trỏ tới JDK 17 |
| SeedTool báo "Không tìm thấy …db-staff.properties" | Tệp cấu hình sai chỗ hoặc sai tên (Windows hay ẩn đuôi, tệp thành `db-staff.properties.txt`) | Bước 4; bật *View → File name extensions* trong Explorer |
| "Không kết nối được tới máy chủ dữ liệu" | Dịch vụ MySQL chưa chạy, sai cổng | Mở *Services* (services.msc) → *MySQL80* → *Start*; kiểm tra cổng trong `db.url` |
| `Access denied for user 'oes_staff'` / `'oes_client'` | Chưa chạy `security.sql`, hoặc sai mật khẩu trong tệp cấu hình | Bước 2; mật khẩu mẫu ở bước 2 |
| `SELECT command denied ... 'v_login'` | Chạy `security.sql` trên CSDL khác `oes` | Chạy lại lệnh `source db/security.sql` với đúng tên CSDL |
| `Public Key Retrieval is not allowed` | MySQL tắt TLS | Thêm `&allowPublicKeyRetrieval=true` vào cuối `db.url` |
| Ứng dụng thi không thấy ca thi nào | Ca mẫu đã quá 2 giờ, hoặc thí sinh không thuộc lớp của ca | Bước 5 – mở ca mới trên StaffApp |
| Thí sinh nộp xong đứng mãi ở "Đang chấm bài…" | StaffApp không chạy | Mở StaffApp (bước 6), thí sinh bấm "Kiểm tra lại" |
| Muốn đổi máy chủ cho ứng dụng thi | `db-client.properties` đã lưu địa chỉ cũ | Xóa tệp `db-client.properties` ở thư mục dự án, mở lại ứng dụng |
