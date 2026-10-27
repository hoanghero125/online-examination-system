# CLAUDE.md – Online Examination System (Nhóm 2)

Bài tập lớn môn Lập trình hướng đối tượng. Kế hoạch đầy đủ: `docs/PLAN.md`. Tiến độ: `docs/PROGRESS.md`.
Mọi thay đổi so với thiết kế Milestone 3 ghi vào `docs/DESIGN-CHANGES.md` (*thay đổi – lý do – ảnh hưởng*).

Toàn bộ mã do Claude viết; bốn thành viên sẽ bị hỏi vấn đáp về từng phần. Mã phải **dễ đọc và giải thích được**:
chú thích tiếng Việt ở chỗ có quyết định thiết kế, tên rõ nghĩa, không dùng mẹo khó hiểu.

## Lệnh thường dùng

```bash
./mvnw test                         # kiểm thử (JDBC bỏ qua nếu chưa đặt biến môi trường)
./mvnw compile exec:java            # ConsoleDemo, dữ liệu trong bộ nhớ
./mvnw javafx:run@staff             # ứng dụng quản lý
./mvnw javafx:run@client            # ứng dụng thi
OES_TEST_DB=... OES_TEST_DB_CLIENT=... ./mvnw test   # thêm kiểm thử với MySQL thật
```

## Quyết định nghiệp vụ đã chốt – KHÔNG được đổi

1. Mỗi thí sinh thi **đúng một lần** trong một ca thi. Thoát ra rồi vào lại thì mở tiếp bài cũ, không tạo bài mới.
2. Ca thi có **giờ mở và giờ đóng chung** cho mọi thí sinh. Vào muộn thì còn ít thời gian hơn.
3. Chỉ có **4 loại câu hỏi khách quan**: một đáp án, nhiều đáp án, đúng/sai, điền khuyết. Mọi bài đều chấm tự động.
4. Câu nhiều đáp án chấm theo cấu hình của đề: `ALL_OR_NOTHING` hoặc `PARTIAL` (tỷ lệ đúng trừ phần chọn sai, chặn dưới 0).
5. Chống gian lận: làm bài toàn màn hình; mỗi lần rời cửa sổ là một lần vi phạm; **đủ 5 lần** thì kết thúc và khóa bài,
   chấm phần đã làm. Các sự kiện cách nhau dưới 3 giây được gộp làm một.
6. Giờ chuẩn lấy từ máy chủ MySQL (`SELECT NOW()`), không bao giờ dùng đồng hồ máy trạm để tính giờ thi.
7. Không có khiếu nại, không gửi email, không giám sát tiến độ từng thí sinh theo thời gian thực.

## Công nghệ – ràng buộc của giảng viên

- Java 17 (`maven.compiler.release=17`): được dùng record, switch biểu thức, pattern matching `instanceof`; không dùng tính năng Java 21+.
- JavaFX 21 (`javafx-controls`, `javafx-fxml`), giao diện FXML + CSS.
- JDBC thuần với `mysql-connector-j`. **Không** Hibernate/JPA, Spring hay framework nào khác.
- Maven, JUnit 5. **Không thêm thư viện nào khác**; nếu thấy cần, ghi vào "Câu hỏi mở" trong `docs/PROGRESS.md`.
- MySQL 8 khi chạy thật; `db/*.sql` phải chạy được trên cả MariaDB 10.6+.

## Kiến trúc

- Ba tầng: **view (JavaFX) → service → DAO**. Lớp model dùng chung.
- **Không có logic nghiệp vụ trong controller JavaFX.** Controller chỉ đọc ô nhập, gọi service, cập nhật giao diện, hiện lỗi.
- Model **không** import `javafx.*` và **không** chứa SQL.
- Service chỉ biết interface DAO. Mọi việc tạo đối tượng nằm trong `AppContext` (phía giảng viên) / `ClientContext` (phía thí sinh).
- Mọi kiểm tra quyền làm ở service qua `Permissions`, kể cả khi giao diện đã ẩn nút.
- Máy thí sinh dùng tài khoản MySQL `oes_client`, chỉ đọc khung nhìn không có đáp án (NFR-04). Chỉ phía giảng viên chấm bài.

## JavaFX

- **Không bao giờ gọi DAO/service trên JavaFX Application Thread.** Dùng `BackgroundTasks.run(supplier, onSuccess, onError)`.
- Mỗi màn hình = một FXML trong `src/main/resources/fxml/` + một controller trong `ui.controller`. Kiểu dáng ở `css/app.css`.
- Toàn bộ chữ trên giao diện bằng tiếng Việt có dấu.
- Lỗi: `ExamAppException` → hộp thoại hiện `getUserMessage()`; lỗi khác → "Đã có lỗi không mong muốn" + log kèm stack trace.

## Mã nguồn

- Package gốc `com.group2.oes`. Tên lớp/phương thức/biến tiếng Anh; chú thích và thông điệp lỗi tiếng Việt.
- Kiểm tra dữ liệu trong constructor, trả về bản chỉ đọc của collection, `assignId` chỉ gán một lần, ngoại lệ có mã lỗi ổn định.
- Chú thích giải thích **vì sao**, không lặp lại mã đang làm gì.
- JDBC: luôn `PreparedStatement`, luôn try-with-resources, không nối chuỗi giá trị vào SQL.
- `-Xlint:all` sạch (không cảnh báo biên dịch).
- Tên phương thức kiểm thử viết tiếng Việt mô tả hành vi, như các kiểm thử có sẵn.

## Quy trình

- Làm lần lượt theo phase trong `docs/PLAN.md`; mỗi phase xong khi đạt tiêu chí "Xong khi" và `mvn test` xanh.
- Sau mỗi phase: commit `phase N: <tóm tắt>`, cập nhật `docs/PROGRESS.md`.
- Thêm tính năng mới thì viết kiểm thử cho nó trong cùng phase.
