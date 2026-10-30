# Kiểm thử tải – NFR-01, NFR-02

[← README](../README.md) · [Phân công](PHAN-CONG.md) · [Tài liệu vấn đáp](vandap/) · [Chạy trên máy cá nhân](SETUP.md) · [Dựng phòng máy](RUNBOOK.md) · [Kịch bản demo](DEMO-SCRIPT.md)

> NFR-01: 60 máy trạm cùng nối một CSDL; chuyển câu, lưu câu trả lời phản hồi dưới 2 giây trong mạng LAN.
> NFR-02: chấm một bài 50 câu dưới 3 giây.

## Cách đo

`src/test/java/com/group2/oes/tools/LoadSimulator.java` giả lập 60 thí sinh chạy song song. Mỗi thí sinh:

1. đăng nhập bằng ứng dụng thi (tài khoản MySQL **oes_client**, đi qua khung nhìn, quyền theo cột và trigger thật);
2. vào thi (tạo bài làm, đọc đề qua `v_exam_paper`, trộn đề);
3. cứ 2 giây lưu một câu trả lời ngẫu nhiên, trong 3 phút;
4. nộp bài.

Sau đó máy chấm (tài khoản **oes_staff**) chấm toàn bộ 60 bài trong một lượt. Thời gian mỗi thao tác đo bằng
`System.nanoTime()` quanh đúng lời gọi service, tức là gồm cả mở kết nối, TLS, câu SQL và trigger.

## Kết quả lần chạy ngày 28/10/2026

Môi trường: **MySQL 8.0.40 trên cùng máy** với chương trình giả lập (localhost, kết nối TLS mặc định của Connector/J),
máy 30 nhân, 62 GB RAM, Java 17. **Không phải mạng LAN của phòng máy** – xem mục "Chạy lại trong phòng máy".

| Thao tác | Số lần | p50 (ms) | p95 (ms) | p99 (ms) | max (ms) |
|---|---|---|---|---|---|
| Vào thi (`enter`) | 60 | 59.4 | 70.9 | 80.5 | 80.5 |
| Lưu câu trả lời (`saveAnswer`) | 5 357 | 18.2 | 31.3 | 39.0 | 113.8 |
| Nộp bài (`submit`) | 60 | 24.6 | 36.2 | 43.6 | 43.6 |

- Lỗi: **0**.
- Máy chấm: chấm **60 bài trong 2 162 ms** (khoảng 36 ms/bài, đề 20 câu; gồm cả đọc bài và đề từ CSDL).
- NFR-02 còn được kiểm tra riêng bằng `SubmissionGraderTest.chấmBàiNămMươiCâuDướiBaGiây` (chấm trong bộ nhớ, dưới 1 ms).

### Nhận xét

- Thao tác chậm nhất (lưu câu trả lời, max 114 ms) vẫn thấp hơn ngưỡng 2 giây khoảng 17 lần.
- Phần lớn thời gian là **mở kết nối mới cho mỗi thao tác** (bắt tay TCP + TLS + xác thực), như báo cáo M4 đã ghi.
  Trong LAN mỗi vòng đi về thêm khoảng 1 ms nên con số thật sẽ cao hơn một chút nhưng vẫn rất xa ngưỡng.
  Vì vậy **chưa cần connection pool** (và không được thêm thư viện); nếu đo trong phòng máy mà p95 vượt 500 ms
  thì đó là nơi đầu tiên cần xem.
- Màn hình làm bài còn chờ 500 ms sau mỗi lần đổi câu trả lời rồi mới lưu (gõ chữ không sinh nhiều lượt ghi),
  nên tải thật lên CSDL còn thấp hơn mức giả lập 1 lần / 2 giây / thí sinh.

## Chạy lại trong phòng máy

Con số có ý nghĩa cho NFR-01 là con số đo trong mạng LAN thật. Trên một máy trạm (không phải máy chủ CSDL):

```bash
./mvnw -q test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/test-classes:target/classes:$(cat target/cp.txt)" com.group2.oes.tools.LoadSimulator \
     db-staff.properties db-client.properties 60 180 2000
```

Trên Windows thay dấu `:` trong `-cp` bằng `;` và `$(cat target/cp.txt)` bằng nội dung tệp `target/cp.txt`.
Tham số: tệp cấu hình giảng viên, tệp cấu hình thí sinh, số thí sinh, số giây, khoảng cách giữa hai lần lưu (ms).
Chương trình tự tạo môn `LOAD`, 60 tài khoản `load_sv001`… và một ca thi mới mỗi lần chạy.
