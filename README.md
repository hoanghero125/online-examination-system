# Online Examination System – Nhóm 2 (Milestone 6, đang làm)

Phần lõi của ứng dụng thi trắc nghiệm: lớp nghiệp vụ, chấm điểm, truy cập MySQL qua JDBC,
xử lý ngoại lệ. Giao diện JavaFX làm ở Milestone 6.

## Chạy nhanh (không cần MySQL)

    ./mvnw compile exec:java

Chương trình `ConsoleDemo` chạy toàn bộ kịch bản: nhập câu hỏi, sinh đề, mở ca thi,
ba thí sinh làm bài (một người bị kết thúc do vi phạm), thống kê và xuất bảng điểm.

## Chạy với MySQL

1. Tạo CSDL: `CREATE DATABASE oes CHARACTER SET utf8mb4;`
2. Chạy `db/schema.sql` trên CSDL đó.
3. Sao chép `src/main/resources/db.properties.example` thành `db.properties`, sửa địa chỉ máy chủ.
4. `mvn compile exec:java -Dexec.args="db.properties"`

## Kiểm thử

    ./mvnw test

Kiểm thử với MySQL thật chỉ chạy khi có biến môi trường `OES_TEST_DB` trỏ tới tệp `db.properties`
của một CSDL thử nghiệm (sẽ bị ghi thêm dữ liệu):

    OES_TEST_DB=./db.properties mvn test

## Cấu trúc gói

    com.group2.oes
    ├── model         lớp nghiệp vụ: User, Exam, ExamSession, Submission...
    │   └── question  Question và 4 lớp con, AnswerOption
    ├── grading       GradingStrategy và 3 cách chấm (mẫu Strategy)
    ├── factory       QuestionFactory, QuestionData (mẫu Factory)
    ├── exception     ExamAppException và 5 lớp con
    ├── dao           interface DAO
    │   ├── jdbc      cài đặt bằng JDBC cho MySQL
    │   └── memory    cài đặt bằng Map, dùng cho kiểm thử
    ├── service       điều phối nghiệp vụ, kiểm tra quyền
    ├── util          băm mật khẩu, nhật ký thao tác
    └── app           AppContext (lắp ráp đối tượng), ConsoleDemo
