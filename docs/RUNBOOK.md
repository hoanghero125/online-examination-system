# Hướng dẫn dựng phòng máy

Mô hình: **một máy chủ** chạy MySQL (và nên chạy StaffApp hoặc GradingDaemon để chấm bài), **các máy trạm** chạy
ứng dụng thi, tất cả trong cùng mạng LAN.

```
 Máy giảng viên / máy chủ                     Máy thí sinh (×N)
 ┌────────────────────────────┐               ┌──────────────────────┐
 │ MySQL 8  (cổng 3306)        │◀── LAN ──────│ run-client  (oes_client)│
 │ run-staff / run-grading-…   │               └──────────────────────┘
 │  (tài khoản oes_staff)      │
 └────────────────────────────┘
```

## 0. Chuẩn bị (làm trước buổi thi một ngày)

- Mọi máy cài **Java 17 trở lên** (`java -version`). Bản JRE là đủ.
- Đặt múi giờ Việt Nam (UTC+7) cho mọi máy. Đồng hồ đếm ngược luôn tính theo giờ máy chủ nên không bị ảnh hưởng,
  nhưng giờ mở/đóng in trên màn hình đổi theo múi giờ của máy trạm – sai múi giờ sẽ hiện giờ lạ, dễ gây hoang mang.
- **Build jar trên đúng hệ điều hành sẽ chạy nó.** Jar chứa JavaFX, mà JavaFX có phần native theo hệ điều hành:
  jar build trên Linux không chạy trên Windows. Trên máy Windows có mã nguồn:

  ```bat
  mvnw.cmd package
  ```

  Thư mục `dist\` có `oes-staff.jar`, `oes-client.jar` và các script `run-*.bat`. Chép **cả thư mục `dist`** sang
  máy cần chạy. Nếu không build được jar: mở mã nguồn trong IntelliJ và chạy cấu hình "StaffApp (quản lý)" /
  "ExamClientApp (thi)", hoặc `mvnw.cmd javafx:run@staff` / `mvnw.cmd javafx:run@client`.

## 1. Máy chủ: MySQL

1. Cài MySQL 8. Mở cho máy khác trong LAN nối vào – **mặc định MySQL 8 chỉ nghe 127.0.0.1**:
   - trong `my.ini` (Windows: `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`) hoặc `my.cnf`, mục `[mysqld]`, đặt
     `bind-address = 0.0.0.0`, rồi khởi động lại dịch vụ MySQL;
   - mở cổng **TCP 3306** chiều vào trên tường lửa của máy chủ (Windows Defender Firewall → Inbound Rules → New Rule → Port → 3306).
2. Tạo CSDL và bảng, rồi phân quyền (chạy **đúng thứ tự**, từ thư mục mã nguồn):

   ```bash
   mysql -u root -p -e "CREATE DATABASE oes CHARACTER SET utf8mb4"
   mysql -u root -p oes < db/schema.sql
   mysql -u root -p oes < db/security.sql
   ```

3. **Đổi ngay mật khẩu hai tài khoản** (một số bản MySQL có chính sách mật khẩu mạnh, sẽ từ chối mật khẩu mẫu trong
   `security.sql`; chọn mật khẩu dài, có chữ hoa, chữ thường, số, ký tự đặc biệt):

   ```sql
   ALTER USER 'oes_staff'@'%'  IDENTIFIED BY 'MatKhauStaff#2026';
   ALTER USER 'oes_client'@'%' IDENTIFIED BY 'MatKhauClient#2026';
   ```

4. Kiểm tra phân quyền máy thí sinh đúng như thiết kế (NFR-04) – đăng nhập bằng `oes_client`:

   ```sql
   -- mysql -h <ip-máy-chủ> -u oes_client -p oes
   SELECT * FROM answer_option;                          -- phải bị từ chối: SELECT command denied
   SELECT COUNT(*) FROM v_exam_paper;                    -- được phép
   UPDATE submission SET total_score = 10 WHERE id = 1;  -- phải bị từ chối: UPDATE command denied ... 'total_score'
   ```

   Nếu máy chủ dùng **MariaDB** thay vì MySQL: `security.sql` mới chạy thử trên MySQL 8.0.40. Chạy hai tệp SQL như
   trên rồi chạy các lệnh kiểm tra này; lệnh nào cho kết quả khác thì báo lại.

## 2. Kiểm tra mạng

Trên **từng máy trạm**:

```bat
telnet <ip-máy-chủ> 3306
```

(Windows chưa có telnet: Control Panel → Programs → Turn Windows features on or off → Telnet Client;
hoặc PowerShell: `Test-NetConnection <ip-máy-chủ> -Port 3306`.) Màn hình đen hoặc `TcpTestSucceeded : True` là được.

Không nối được: kiểm tra lại `bind-address` và tường lửa ở bước 1. **Mạng của trường có thể chặn kết nối giữa các
máy.** Phương án dự phòng: phát Wi-Fi từ một điện thoại, cho máy chủ và các máy trạm cùng nối vào đó (dùng IP mới của
máy chủ trong mạng điện thoại).

## 3. Cấu hình hai ứng dụng

Tệp cấu hình đặt **cạnh tệp jar** (trong thư mục `dist`), hoặc chỉ định bằng `--config=đường/dẫn`.

- Máy chủ / máy giảng viên: chép `config/db-staff.properties.example` thành `dist/db-staff.properties`, sửa `db.url`
  (IP máy chủ) và `db.password` (mật khẩu `oes_staff` đã đặt ở bước 1.3).
- Máy thí sinh: không cần chép tệp – lần đầu mở ứng dụng thi sẽ hiện màn hình **Kết nối máy chủ**: nhập IP, mật khẩu
  `oes_client`, bấm "Kết nối và lưu"; ứng dụng thử kết nối, đúng thì tự lưu `db-client.properties` cạnh jar.
  Có thể làm trên một máy rồi chép tệp `db-client.properties` sang các máy khác.

## 4. Dữ liệu mẫu

Trên máy chủ (đã có `dist/db-staff.properties`):

```bat
dist\run-seed.bat
```

In ra bảng tài khoản (quản trị `admin`/`admin123`, giảng viên `gv.viet`, `gv.lan` / `gv123456`, thí sinh
`b24dcce001`…`060` và 4 thành viên nhóm / `sv123456`). **Ca thi mẫu mở đúng 2 giờ kể từ lúc chạy SeedTool**:
chạy không sớm hơn khoảng 1 tiếng rưỡi trước giờ demo, hoặc để giảng viên mở ca mới trên StaffApp (đó cũng là một
bước demo). SeedTool chỉ chạy được trên CSDL trống; muốn làm lại từ đầu thì chạy lại bước 1.2.

## 5. Ngày thi

1. Máy chủ: mở `dist\run-staff.bat`, đăng nhập giảng viên. Thanh dưới cùng hiện "Máy chấm đang chạy" – StaffApp mở
   là bài nộp được chấm sau vài giây. Không muốn mở giao diện: chạy `dist\run-grading-daemon.bat` thay thế (để cửa sổ
   dòng lệnh mở suốt giờ thi).
2. Máy thí sinh: mở `dist\run-client.bat`, thí sinh đăng nhập, Vào thi → đọc quy chế → Bắt đầu làm bài.
3. Sau giờ thi: StaffApp → Ca thi → chọn ca → Xem kết quả → Xuất CSV.

Nhật ký thao tác của mỗi máy nằm ở `dist/oes-audit.log` (StaffApp → Nhật ký để xem trên máy đang chạy StaffApp).

## 6. Sự cố thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| "Không kết nối được tới máy chủ dữ liệu" sau khoảng 5 giây | Sai IP, MySQL chưa mở cho LAN, tường lửa chặn | Làm lại bước 1.1 và bước 2 |
| `Public Key Retrieval is not allowed` trong `oes-audit.log` | MySQL tắt TLS, tài khoản dùng `caching_sha2_password` | Thêm `&allowPublicKeyRetrieval=true` vào cuối `db.url` trong tệp cấu hình |
| `Access denied for user 'oes_client'` | Sai mật khẩu, hoặc chưa chạy `security.sql` trên đúng CSDL | Kiểm tra bước 1.2, 1.3; mật khẩu trong tệp cấu hình phải trùng |
| `SELECT command denied ... 'v_exam_paper'` | Chạy `security.sql` trên CSDL khác với CSDL trong `db.url` | Chạy lại `mysql -u root -p oes < db/security.sql` |
| Thí sinh nộp xong đứng ở "Đang chấm bài…" | Không có StaffApp / GradingDaemon nào chạy | Mở StaffApp hoặc `run-grading-daemon`; thí sinh bấm "Kiểm tra lại" |
| Jar báo `UnsatisfiedLinkError` / không mở cửa sổ | Jar build trên hệ điều hành khác | Build lại trên đúng hệ điều hành (bước 0) |
| Màn hình làm bài không vào toàn màn hình | Driver đồ họa cũ | Thử chạy với `java -Dprism.order=sw -jar oes-client.jar` |
