# Đề cương vấn đáp

[README](../README.md) · [Phân công](PHAN-CONG.md) · [Tài liệu vấn đáp](vandap/) · [Sơ đồ](DIAGRAMS.md) · [Kịch bản demo](DEMO-SCRIPT.md)

Bộ câu hỏi dự đoán, soạn theo góc nhìn giảng viên chấm bài tập lớn OOP: giảng viên có mã nguồn, báo cáo M1–M4 và đang
xem demo. Mỗi câu ghi người nên trả lời (P1–P4 theo [PHAN-CONG.md](PHAN-CONG.md), "Cả nhóm" nghĩa là ai cũng phải trả
lời được), mức độ, các ý cần có, và câu giảng viên dễ hỏi vặn tiếp. Gợi ý trả lời chi tiết hơn nằm trong tài liệu vấn đáp
của từng phần ([`vandap/`](vandap/)) và 14 câu hỏi chung trong [PHAN-CONG.md](PHAN-CONG.md#câu-hỏi-chung--ai-cũng-phải-trả-lời-được).

Cách luyện: một người đóng vai giảng viên, đọc câu hỏi; người trả lời mở mã nguồn và chỉ vào đúng tệp, đúng phương thức
khi nói. Trả lời bằng mã thật luôn thuyết phục hơn trả lời bằng định nghĩa trong sách.

Mức độ: **CB** cơ bản (ai học OOP cũng phải trả lời được), **K** khá (cần hiểu thiết kế của nhóm), **N** nâng cao
(hỏi vặn, phản biện, sửa tại chỗ).

## Mục lục

- [A. Mở đầu và tổng quan](#a-mở-đầu-và-tổng-quan)
- [B. Bốn tính chất OOP trên mã của nhóm](#b-bốn-tính-chất-oop-trên-mã-của-nhóm)
- [C. Kiến thức Java](#c-kiến-thức-java)
- [D. Thiết kế: SOLID, mẫu thiết kế, UML](#d-thiết-kế-solid-mẫu-thiết-kế-uml)
- [E. Phần 1 – Người dùng, phân quyền, khung ứng dụng](#e-phần-1--người-dùng-phân-quyền-khung-ứng-dụng)
- [F. Phần 2 – Ngân hàng câu hỏi và đề thi](#f-phần-2--ngân-hàng-câu-hỏi-và-đề-thi)
- [G. Phần 3 – Ca thi và làm bài](#g-phần-3--ca-thi-và-làm-bài)
- [H. Phần 4 – Chấm bài và kết quả](#h-phần-4--chấm-bài-và-kết-quả)
- [I. CSDL và bảo mật](#i-csdl-và-bảo-mật)
- [J. Đa luồng và JavaFX](#j-đa-luồng-và-javafx)
- [K. Kiểm thử và chất lượng mã](#k-kiểm-thử-và-chất-lượng-mã)
- [L. Sửa mã tại chỗ](#l-sửa-mã-tại-chỗ)
- [M. Phản biện và câu hỏi khó](#m-phản-biện-và-câu-hỏi-khó)
- [N. Quá trình làm việc của nhóm](#n-quá-trình-làm-việc-của-nhóm)
- [Bảng tra nhanh](#bảng-tra-nhanh)

---

## A. Mở đầu và tổng quan

**A1. Giới thiệu đề tài trong 1 phút: bài toán gì, ai dùng, chạy trên gì?** (Cả nhóm, CB)
- Thi trắc nghiệm trong phòng máy; 3 vai trò: quản trị viên, giảng viên, thí sinh (`Role`).
- Một MySQL chung trong LAN, hai ứng dụng JavaFX: StaffApp (quản trị, giảng viên, máy chấm) và ExamClientApp (thí sinh).
- 4 loại câu hỏi khách quan, chấm tự động; chống gian lận bằng toàn màn hình và đếm số lần rời cửa sổ.
- Hỏi vặn: "Sao không làm web?" → yêu cầu môn học là ứng dụng desktop Java/JavaFX; phòng máy không cần máy chủ web.

**A2. Phạm vi: những gì hệ thống không làm?** (Cả nhóm, CB)
- Không tự luận, không thi lại, không khiếu nại, không gửi email, không camera giám sát, không chạy qua Internet
  (quyết định đã chốt ở M1, ghi trong [`CLAUDE.md`](../CLAUDE.md)).
- Hỏi vặn: "Vậy câu điền khuyết chấm thế nào cho công bằng?" → so khớp chuỗi sau chuẩn hóa (bỏ khoảng trắng thừa,
  chuẩn hóa Unicode, tùy chọn phân biệt hoa thường), nhiều đáp án chấp nhận (P2, P4).

**A3. Vì sao tách hai ứng dụng mà không làm một ứng dụng có đăng nhập theo vai trò?** (P1, K)
- Máy thí sinh không được giữ thông tin truy cập có quyền đọc đáp án. Hai ứng dụng dùng hai tài khoản MySQL khác quyền
  (`oes_staff`, `oes_client`), nên dù thí sinh mở được tệp cấu hình cũng chỉ có quyền của `oes_client`.
- Dùng chung khung đăng nhập nhờ `OesApplication` và `AppShell`, nên không viết lặp.
- Hỏi vặn: "Thí sinh đăng nhập vào StaffApp thì sao?" → `AuthService` từ chối tài khoản thí sinh (mã `WRONG_APP`), kiểm
  tra ở service chứ không ở giao diện.

**A4. Chỉ trên sơ đồ: dữ liệu đi từ nút bấm tới CSDL qua những tầng nào?** (Cả nhóm, CB)
- View (FXML + controller) → service → interface DAO → cài đặt JDBC → MySQL. Model dùng chung cho mọi tầng.
- Mở [`DIAGRAMS.md`](DIAGRAMS.md#kiến-trúc-tầng-và-gói) để chỉ.

**A5. So với thiết kế M3, cái gì thay đổi nhiều nhất và vì sao?** (P4 chính, cả nhóm biết, K)
- Bản M4 chấm bài trên máy thí sinh, nghĩa là máy thí sinh phải đọc được đáp án: vi phạm NFR-04.
- M5: chuyển việc chấm sang máy giảng viên (`GradingWorker`), thêm mô hình riêng cho máy thí sinh không có đáp án (gói
  `client`), tách `Submission`, `ExamSession` theo SRP, tách DAO theo ISP. Chi tiết: [`DESIGN-CHANGES.md`](DESIGN-CHANGES.md).

---

## B. Bốn tính chất OOP trên mã của nhóm

Giảng viên thường không hỏi định nghĩa suông mà bảo "chỉ cho tôi trong mã của em". Mỗi câu dưới đây phải trả lời bằng
một lớp cụ thể.

**B1. Đóng gói thể hiện ở đâu?** (Cả nhóm, CB)
- Trường `private`, chỉ thay đổi qua phương thức có kiểm tra: `Submission.markSubmitted` chỉ đổi trạng thái hợp lệ;
  `assignId` chỉ gán mã một lần (`User`, `Exam`, `ExamSession`).
- Trả về bản chỉ đọc của collection: `Exam.getItems()` trả `Collections.unmodifiableList`, nên bên ngoài không thêm
  câu vào đề mà bỏ qua kiểm tra của `Exam.addItem`.
- Đóng gói ở mức gói: `Permissions` là lớp `final` không có `public`, chỉ các service cùng gói gọi được.
- Hỏi vặn: "Getter trả về List thì sao, người ta vẫn sửa được phần tử?" → phần tử là đối tượng bất biến hoặc chỉ sửa
  qua phương thức có kiểm tra; danh sách thì không thêm/xóa được.

**B2. Kế thừa: kể các cây kế thừa trong dự án.** (Cả nhóm, CB)
- `User` (trừu tượng) → `Administrator`, `Instructor`, `Student`.
- `Question` (trừu tượng) → `SingleChoiceQuestion`, `MultiChoiceQuestion`, `TrueFalseQuestion`, `FillInBlankQuestion`.
- `ExamAppException` (trừu tượng, kế thừa `RuntimeException`) → `ValidationException`, `AccessDeniedException`,
  `SessionRuleException`, `DataAccessException` → `DuplicateKeyException`.
- `OesApplication<C>` (trừu tượng, kế thừa `javafx.application.Application`) → `StaffApp`, `ExamClientApp`.
- Hỏi vặn: "Sao `Student` không có lớp con `Candidate`?" → không có hành vi khác nhau thì không tạo lớp con.

**B3. Đa hình: cho một ví dụ mà nơi gọi không biết đối tượng thuộc lớp nào.** (Cả nhóm, CB)
- `user.canManage(subject)`: `Administrator` luôn đúng, `Instructor` tra tập môn được phân công, `Student` luôn sai.
  `Permissions.requireManage` gọi mà không cần `instanceof`.
- `question.isCorrect(response)` và `correctRatio(response)`: mỗi loại câu tự biết cách so đáp án.
- `GradingStrategy.grade(...)`: `Exam` giữ `EnumMap<QuestionType, GradingStrategy>`, chọn chiến lược theo loại câu.
- Hỏi vặn: "Đa hình lúc biên dịch và lúc chạy khác nhau thế nào?" → nạp chồng (overloading, chọn lúc biên dịch, ví dụ
  hai constructor của `ServerClock`) và ghi đè (overriding, chọn lúc chạy, ví dụ `canManage`).

**B4. Trừu tượng: lớp trừu tượng và interface trong dự án.** (Cả nhóm, CB)
- Lớp trừu tượng: `User`, `Question`, `ExamAppException`, `OesApplication`.
- Interface: 9 DAO (`UserDao`, `QuestionDao`, `StudentExamDao`, `GradingDao`…), `GradingStrategy`, `AppShell`,
  `StaffWorkspace`.
- Hỏi vặn: "Khi nào chọn lớp trừu tượng, khi nào interface?" → có trạng thái và mã dùng chung (`User` giữ username,
  mật khẩu băm) thì lớp trừu tượng; chỉ là hợp đồng hành vi, có nhiều cài đặt thay thế (DAO JDBC và bộ nhớ) thì interface.

**B5. Vì sao `User` ghi đè `equals` và `hashCode`? So sánh theo gì?** (P1, K)
- So sánh theo `userId` (`o instanceof User other && userId == other.userId`); `Subject` cũng ghi đè vì nằm trong
  `Set` môn được phân công của `Instructor`.
- Quy tắc: ghi đè `equals` thì phải ghi đè `hashCode`, nếu không `HashSet`/`HashMap` hoạt động sai.
- Hỏi vặn: "Hai đối tượng chưa lưu, cùng mã 0, có bằng nhau không?" → có; chấp nhận được vì chỉ so sánh đối tượng đã
  lưu. Trả lời thật, không né.

**B6. Lớp nào bất biến (immutable)? Lợi ích?** (Cả nhóm, K)
- Các `record`: `SessionWindow`, `SessionPolicy`, `QuestionData`, `ResultRow`, `PaperItem`… Kiểm tra dữ liệu trong
  compact constructor, nên không tồn tại `SessionWindow` có giờ đóng trước giờ mở.
- Lợi ích: dùng chung giữa các luồng an toàn, không bị sửa ngầm.
- Hỏi vặn: "Record có kế thừa được không?" → không; record ngầm định `final` và kế thừa `java.lang.Record`.

**B7. Constructor `protected` của `User` để làm gì?** (P1, K)
- Lớp trừu tượng không `new` được; `protected` chỉ cho lớp con gọi `super(...)` để khởi tạo phần dữ liệu chung.

**B8. Liên kết, kết tập, hợp thành: chỉ ví dụ.** (Cả nhóm, K)
- Hợp thành: `Exam` – `ExamItem` (câu trong đề không tồn tại ngoài đề, điểm của câu thuộc về đề);
  `Question` – `AnswerOption`.
- Kết tập: `Instructor` – `Subject` (môn tồn tại độc lập với giảng viên).
- Liên kết: `ExamSession` dùng `Exam`, `Submission` thuộc một ca và một thí sinh.

---

## C. Kiến thức Java

**C1. Ngoại lệ checked và unchecked; nhóm chọn loại nào?** (P1, K)
- `ExamAppException` kế thừa `RuntimeException` (unchecked). Lý do: lỗi nghiệp vụ đi xuyên ba tầng và qua lambda
  (`BackgroundTasks.run`), checked exception sẽ buộc khai báo `throws` khắp nơi.
- Mỗi ngoại lệ có mã ổn định (ví dụ `SESSION_CLOSED`) và thông điệp tiếng Việt cho người dùng (`getUserMessage()`).
- `SQLException` (checked) bị chặn ở tầng DAO, `JdbcErrors.translate` dịch thành ngoại lệ của hệ thống.
- Hỏi vặn: "Bắt `Exception` chung chung có được không?" → chỉ ở ranh giới cuối (bộ xử lý lỗi của luồng giao diện, hiện
  "Đã có lỗi không mong muốn" và ghi log); bên trong thì bắt đúng loại.

**C2. `record` là gì, khác lớp thường thế nào?** (Cả nhóm, CB)
- Lớp dữ liệu bất biến, tự sinh constructor, getter, `equals`, `hashCode`, `toString`. Ví dụ `SessionWindow`.

**C3. `enum` trong Java khác hằng số `int` thế nào? Cho ví dụ enum có trường.** (P2, CB)
- Kiểu an toàn, có phương thức và trường: `Difficulty` có `displayName()` ("Dễ", "Trung bình", "Khó").
- `switch` biểu thức trên enum: `QuestionFactory.create` thiếu một nhánh thì trình biên dịch báo lỗi.
- `EnumMap` trong `Exam` cho chiến lược chấm.

**C4. Generic: chỉ một chỗ dùng generic do nhóm tự viết.** (P1, N)
- `OesApplication<C>`: `C` là `AppContext` (StaffApp) hoặc `ClientContext` (ExamClientApp); phương thức trừu tượng
  `connectTo(ConnectionManager cm)` trả về `C`.
- `ConnectionManager.execute(SqlWork<T>)`: một cách mở/đóng kết nối và dịch lỗi cho mọi truy vấn, kiểu trả về tùy truy vấn.

**C5. `final` dùng ở những đâu, ý nghĩa mỗi chỗ?** (Cả nhóm, K)
- Trường `final`: gán một lần (phụ thuộc DAO trong service). Lớp `final`: không cho kế thừa (lớp tiện ích như
  `PasswordHasher`, `AnswerSaver`). Phương thức `final`: `OesApplication.start` không cho lớp con ghi đè khung khởi động.

**C6. `static` dùng ở đâu? Vì sao `Permissions` toàn phương thức tĩnh?** (P1, K)
- Hàm kiểm tra không giữ trạng thái, không cần đối tượng. Hằng: `ViolationPolicy.MAX_VIOLATIONS`,
  `GradingScheduler.DEFAULT_PERIOD`.
- Hỏi vặn: "Static thì khó thay khi kiểm thử?" → `Permissions` thuần logic trên đối tượng `User`, không gọi ra ngoài,
  nên không cần thay.

**C7. `Optional` dùng ở đâu, vì sao không trả `null`?** (P2/P4, CB)
- DAO tìm theo khóa trả `Optional` (ví dụ `findById`, `findByUsername`): nơi gọi buộc phải xử lý trường hợp không có.

**C8. Pattern matching `instanceof` và `switch` biểu thức của Java 17.** (P2, K)
- `JdbcQuestionDao`: `if (q instanceof TrueFalseQuestion tf)` lấy ngay biến đã ép kiểu khi ghi cột riêng của loại câu.
- Hỏi vặn: "Dùng `instanceof` có trái đa hình không?" → ở tầng DAO cần biết cột nào để ghi, chấp nhận; ở nghiệp vụ thì
  dùng đa hình (`isCorrect`).

---

## D. Thiết kế: SOLID, mẫu thiết kế, UML

**D1. Năm nguyên lý SOLID, mỗi nguyên lý một ví dụ trong mã.** (Cả nhóm, K)
- S: `Submission` chỉ giữ dữ liệu và chuyển trạng thái; chấm điểm ở `SubmissionGrader`, quy chế vi phạm ở `ViolationPolicy`.
- O: thêm cách chấm mới = thêm một lớp `GradingStrategy`; thêm loại câu = thêm lớp con `Question`.
- L: mọi lớp con `Question` dùng được ở chỗ cần `Question`; mọi lớp con `User` trả lời `canManage`.
- I: máy thí sinh chỉ thấy `StudentExamDao`, `StudentLoginDao`, không thấy hàm chấm điểm hay đọc đáp án.
- D: service nhận interface DAO qua constructor; `AppContext`/`ClientContext` lắp ráp.
- Hỏi vặn: "Có chỗ nào vi phạm SOLID không?" → nói thật: `ExamController` vẫn khá lớn dù đã tách `AnswerSaver`,
  `ExamProctor`, `AnswerSheet`; `ExamController` đọc thẳng hằng `ViolationPolicy.MAX_VIOLATIONS` (P3 biết chỗ này).

**D2. Mẫu Factory: ở đâu, giải quyết vấn đề gì?** (P2, K)
- `QuestionFactory.create(QuestionType, QuestionData)`: nơi duy nhất quyết định dựng lớp con nào. Giao diện, nhập
  tệp và DAO đọc CSDL đều dùng chung, nên thêm loại câu chỉ sửa một chỗ.

**D3. Mẫu Strategy: ở đâu?** (P4, K)
- `GradingStrategy` + `ExactMatchGrading`, `PartialMatchGrading`, `TextMatchGrading`. Câu nhiều đáp án chấm toàn phần
  hay theo tỷ lệ là cấu hình của đề (`MultiChoicePolicy`), đổi chiến lược mà không sửa lớp câu hỏi.
- Hỏi vặn: "Strategy khác Factory chỗ nào?" → Factory tạo đối tượng; Strategy là thuật toán thay thế được lúc chạy.

**D4. Mẫu Template Method: ở đâu?** (P1, N)
- `OesApplication.start` là `final`, cố định trình tự khởi động (đọc cấu hình, nối CSDL, mở màn hình đăng nhập); lớp con
  chỉ điền `configFileName()`, `defaultDbUser()`, `connectTo(...)`.

**D5. `AppContext` là gì? Sao không để mỗi service tự `new` DAO?** (P1, K)
- Nơi lắp ráp (composition root). Service tự `new JdbcUserDao()` thì không thay được bằng DAO trong bộ nhớ để kiểm thử,
  và máy thí sinh có thể vô tình dựng DAO có quyền chấm điểm.

**D6. Sơ đồ lớp trong báo cáo M3 còn khớp với mã không?** (Cả nhóm, K)
- Không khớp hoàn toàn và nhóm ghi lại từng chỗ khác trong [`DESIGN-CHANGES.md`](DESIGN-CHANGES.md) (thay đổi, lý do,
  ảnh hưởng). Sơ đồ khớp mã hiện tại: [`DIAGRAMS.md`](DIAGRAMS.md#sơ-đồ-lớp).
- Hỏi vặn: "Vì sao không sửa báo cáo M3?" → báo cáo là mốc nộp theo tiến độ; thay đổi sau đó được ghi riêng để thấy lý do.

**D7. Vì sao tách DAO thành nhiều interface theo người dùng thay vì một `SubmissionDao` lớn?** (P4, K)
- ISP và bảo mật: máy thí sinh chỉ cần vào thi, lưu câu trả lời, nộp bài (`StudentExamDao`); máy chấm cần đọc bài và ghi
  điểm (`GradingDao`). Mỗi bên chỉ thấy đúng hàm mình được dùng, khớp với quyền MySQL của tài khoản tương ứng.

---

## E. Phần 1 – Người dùng, phân quyền, khung ứng dụng

**E1. Mật khẩu lưu thế nào? Vì sao có salt?** (P1, CB)
- `PasswordHasher`: SHA-256 với salt ngẫu nhiên 16 byte (`SecureRandom`), lưu dạng `salt$hash` (Base64).
- Salt để hai người cùng mật khẩu có chuỗi băm khác nhau và không tra được bằng bảng băm có sẵn.
- Hỏi vặn: "SHA-256 có đủ an toàn không?" → nói thật: hàm băm nhanh, chưa phải tốt nhất; nên dùng hàm băm chậm như PBKDF2
  (có sẵn trong JDK, `javax.crypto`) hoặc bcrypt. Đây là điểm cải tiến được.

**E2. Phân quyền kiểm tra ở đâu? Giao diện ẩn nút rồi sao còn kiểm tra ở service?** (P1, CB)
- Mọi kiểm tra ở service qua `Permissions` (`requireStaff`, `requireManage`, `requireStudent`).
- Giao diện có thể sai hoặc bị sửa; service là chốt cuối. Kiểm thử gọi thẳng service bằng tài khoản không đủ quyền và
  chờ `AccessDeniedException`.

**E3. Đổi mật khẩu của thí sinh đi qua đâu, khi `oes_client` không có quyền sửa bảng `app_user`?** (P1, N)
- Thủ tục lưu trữ `sp_student_change_password` khai báo `SQL SECURITY DEFINER`: chạy với quyền người tạo, chỉ cho đổi
  mật khẩu của đúng một thí sinh khi mật khẩu cũ khớp. `oes_client` chỉ có quyền `EXECUTE` thủ tục này.

**E4. Nhập danh sách sinh viên từ CSV: dòng lỗi thì sao?** (P1, K)
- `UserImportService` đọc từng dòng (`CsvLine.split` xử lý dấu phẩy trong ngoặc kép), dòng lỗi được ghi vào
  `ImportResult` kèm số dòng và lý do, các dòng đúng vẫn được nhập. Tài khoản nhập từ CSV bị buộc đổi mật khẩu lần đầu.

**E5. `BackgroundTasks` làm gì?** (P1, K) – xem [J1](#j-đa-luồng-và-javafx).

**E6. Nhật ký thao tác ghi ở đâu? Có ghi trên CSDL không?** (P1, K)
- `AuditLog` ghi tệp `oes-audit.log` trên từng máy; màn hình Nhật ký đọc tệp của máy đang chạy StaffApp.
- Nói thật giới hạn: nhật ký của thí sinh nằm trên máy thí sinh, không tập trung.

---

## F. Phần 2 – Ngân hàng câu hỏi và đề thi

**F1. Quy tắc "câu một đáp án có đúng một phương án đúng" kiểm tra ở đâu?** (P2, CB)
- Trong constructor của `SingleChoiceQuestion` (model), ném `ValidationException`. Controller chỉ bắt lỗi và hiện
  thông điệp, nên mọi đường tạo câu (giao diện, nhập tệp, đọc CSDL) đều bị kiểm tra như nhau.

**F2. Bốn loại câu lưu trong mấy bảng? Vì sao?** (P2, K)
- Một bảng `question` (cột riêng như `correct_value`, `case_sensitive` để trống với loại không dùng) và bảng
  `answer_option`. Đọc lên thì `QuestionFactory` dựng đúng lớp con. Đổi lại phải chấp nhận vài cột rỗng.
- Hỏi vặn: "Sao không mỗi loại một bảng?" → 4 loại khác nhau rất ít cột; một bảng thì truy vấn ngân hàng đơn giản.

**F3. Sinh đề theo ma trận hoạt động thế nào? Không đủ câu thì sao?** (P2, K)
- `ExamMatrix` gồm các dòng (chương, mức độ, số câu); `QuestionBank.findShortages` liệt kê ô thiếu trước khi sinh;
  `pick` trộn bằng `Random` và lấy đủ số câu. Ma trận rỗng bị chặn bằng `Exam.validateForUse` (`EXAM_EMPTY`).

**F4. Đề đã dùng cho ca thi thì sửa được không?** (P2, CB)
- Không. Mở ca thì đề bị khóa (`Exam.lock`); muốn sửa phải tạo phiên bản mới (`ExamService.newVersion`). Câu hỏi nằm
  trong đề đã khóa không sửa, không xóa được, vì bài làm cũ tham chiếu tới mã phương án.

**F5. Điểm của câu thuộc về câu hỏi hay về đề?** (P2, K)
- Về đề: `ExamItem` giữ điểm của câu trong đề đó. Cùng một câu có thể 1 điểm ở đề này, 2 điểm ở đề khác.

**F6. Xóa câu hỏi đang nằm trong đề chưa khóa thì sao?** (P2, N)
- Bị chặn với mã `QUESTION_IN_USE` (`QuestionDao.isUsedInAnyExam`). Đây là lỗi được phát hiện và sửa sau khi rà mã:
  trước đó MySQL từ chối vì khóa ngoại và người dùng thấy nhầm thành lỗi kết nối.

---

## G. Phần 3 – Ca thi và làm bài

**G1. Thí sinh vào thi hai lần, hoặc hai máy cùng bấm "Vào thi" thì sao?** (P3, CB)
- Mỗi thí sinh một bài trong một ca: ràng buộc `UNIQUE(session_id, student_id)` trong CSDL. Máy sau gặp lỗi trùng khóa
  thì đọc lại và mở đúng bài của máy trước. Kiểm thử `haiMáyCùngBấmVàoThiChỉTạoMộtBài` chạy hai luồng song song.

**G2. Giờ thi lấy từ đâu? Thí sinh chỉnh đồng hồ máy thì sao?** (P3, K)
- Lấy giờ máy chủ bằng `SELECT NOW()`; `ServerClock` cộng thêm thời gian trôi đo bằng `System.nanoTime()` (không đổi
  khi chỉnh giờ hệ điều hành), đồng bộ lại mỗi 60 giây. CSDL còn có trigger chặn ghi sau giờ đóng.
- Hỏi vặn: "Sao không gọi `SELECT NOW()` mỗi giây?" → 60 thí sinh × 1 truy vấn/giây là tải vô ích; đồng hồ đơn điệu đủ chính xác.

**G3. Trộn câu hỏi: thoát ra vào lại thì thứ tự có đổi không?** (P3, K)
- Không. `PaperShuffler` dùng seed `Objects.hash(sessionId, studentId)`: cùng thí sinh cùng ca thì cùng thứ tự; khác
  thí sinh thì khác thứ tự. Phương án trộn bằng seed riêng cho từng câu.

**G4. Đếm vi phạm thế nào? Một lần Alt+Tab có bị tính hai lần không?** (P3, CB)
- `ExamProctor` nghe 3 sự kiện: thoát toàn màn hình, mất tiêu điểm, thu nhỏ. Một lần Alt+Tab có thể phát 2–3 sự kiện,
  `ViolationPolicy` gộp các sự kiện cách nhau dưới 3 giây. Đủ 5 lần thì `ProctoringService` nộp bài với lý do
  `VIOLATION`, vẫn chấm phần đã làm.
- Hỏi vặn: "Hộp thoại cảnh báo của chính ứng dụng cũng làm mất tiêu điểm?" → mọi hộp thoại trong giờ thi mở qua
  `ExamProctor.showDialog`, tạm ngừng nghe trong lúc đó.

**G5. Câu trả lời được lưu khi nào? Mất mạng thì sao?** (P3, K)
- `AnswerSaver`: đổi câu trả lời thì chờ 500 ms rồi lưu (debounce); mỗi câu chỉ giữ giá trị mới nhất. Mất mạng thì hiện
  dải vàng, 3 giây thử lại một lần; bấm nộp thì gửi hết hàng đợi trước.

**G6. Mất kết nối đúng lúc thí sinh Alt+Tab thì lần vi phạm có bị mất không?** (P3, N)
- Không. Lỗi này từng có và đã sửa: `ProctoringService.report` chỉ ghi nhớ thời điểm vi phạm sau khi CSDL đã tăng bộ đếm;
  màn hình gửi lại đúng thời điểm cũ khi có mạng. Kiểm thử `mấtKếtNốiLúcBáoViPhạmThìGửiLạiVẫnĐượcTính`.

**G7. Hết giờ mà thí sinh chưa nộp thì ai nộp?** (P3, P4, K)
- Hai đường: màn hình thí sinh tự nộp khi đồng hồ về 0; máy chấm đóng mọi bài còn mở của ca đã hết giờ bằng một lệnh
  `UPDATE` (`GradingDao.closeExpired`). Điều kiện `WHERE status = 'IN_PROGRESS'` bảo đảm chỉ một đường thắng.

---

## H. Phần 4 – Chấm bài và kết quả

**H1. Chấm câu nhiều đáp án theo tỷ lệ: tính ví dụ.** (P4, CB)
- Câu 2 điểm, 2 phương án đúng, 2 sai. Chọn 1 đúng và 1 sai: tỷ lệ đúng 1/2, trừ phần chọn sai 1/2, còn 0 → 0 điểm.
  Chọn đúng 1 và không chọn sai: 1/2 × 2 = 1 điểm. Chặn dưới 0, không có điểm âm.
- Nên tự tính lại trên `PartialMatchGrading` trước buổi vấn đáp, đúng công thức trong mã.

**H2. Máy chấm chạy ở đâu, bao lâu một lượt? Không ai mở StaffApp thì sao?** (P4, CB)
- Trong StaffApp (`GradingScheduler`, mặc định 3 giây một lượt, `scheduleWithFixedDelay`) hoặc `GradingDaemon` không giao
  diện. Không có máy chấm thì thí sinh thấy "Đang chấm bài…" và bài được chấm khi máy chấm chạy lại.
- Hỏi vặn: "Hai StaffApp cùng chấm một bài?" → lệnh ghi điểm có `WHERE id = ? AND status = 'SUBMITTED'` trong một giao
  dịch (`JdbcGradingDao`); máy ghi sau không còn thấy bài ở trạng thái `SUBMITTED` nên không ghi đè.

**H3. Vì sao `scheduleWithFixedDelay` mà không `scheduleAtFixedRate`?** (P4, N)
- Fixed delay đếm 3 giây sau khi lượt trước xong; một lượt chậm (60 bài) không làm các lượt chồng lên nhau.

**H4. Một câu chấm lỗi (ngoại lệ) thì cả bài có hỏng không?** (P4, K)
- Không: `SubmissionGrader` bắt lỗi từng câu, câu đó 0 điểm và ghi log; bài vẫn được chấm. Nói thật giới hạn: cờ "câu
  chấm lỗi" không lưu vào CSDL.

**H5. Thống kê kết quả gồm gì? CSV mở bằng Excel bị lỗi font không?** (P4, CB)
- Cao nhất, thấp nhất, trung bình, số bài kết thúc do vi phạm, phổ điểm, tỷ lệ đúng từng câu (câu khó nhất lên đầu).
- CSV ghi kèm BOM UTF-8 để Excel hiện đúng tiếng Việt.

---

## I. CSDL và bảo mật

**I1. CSDL có bao nhiêu bảng? Kể các khóa ngoại quan trọng.** (P4, CB)
- 12 bảng ([ERD](DIAGRAMS.md#cơ-sở-dữ-liệu)); 4 khung nhìn, 4 trigger, 1 thủ tục lưu trữ, 2 tài khoản.

**I2. Làm sao chắc chắn máy thí sinh không đọc được đáp án?** (P4, CB)
- `oes_client` không có quyền trên bảng `answer_option`, `question`; chỉ đọc khung nhìn `v_exam_paper` (đề không có cột
  đúng/sai) và `v_review` (đáp án chỉ hiện sau khi ca đóng). Quyền ghi theo cột: không ghi được `total_score`.
- Chứng minh: `JdbcSecurityTest` (8 ca) đăng nhập bằng chính `oes_client`; trong demo gõ thẳng lệnh SQL bị từ chối.

**I3. Trigger dùng để làm gì khi đã có GRANT? Sao dùng `USER()` chứ không `CURRENT_USER()`?** (P4, N)
- GRANT theo cột không chặn được "sửa bài sau giờ đóng" hay "giảm số lần vi phạm"; trigger chặn và báo lỗi bằng
  `SIGNAL SQLSTATE '45000'` với tiền tố `SUBMISSION_LOCKED`.
- Trong trigger, `CURRENT_USER()` trả về người định nghĩa trigger; `USER()` mới là tài khoản đang kết nối.

**I4. Chống SQL injection thế nào?** (Cả nhóm, CB)
- Luôn `PreparedStatement` với tham số `?`. Chỗ duy nhất nối chuỗi là tên CSDL trong `DatabaseSetup` (tên không truyền
  qua `?` được) và tên đó bị kiểm tra chỉ gồm chữ, số, gạch dưới.

**I5. Giới hạn bảo mật còn lại?** (P4, N)
- Mọi máy thí sinh dùng chung `oes_client`: ứng dụng thi bị sửa vẫn đọc/ghi được câu trả lời của thí sinh khác (không đọc
  được đáp án, không sửa được điểm). Chặn hẳn cần máy chủ ứng dụng ở giữa, ngoài công nghệ được phép.

**I6. Giao dịch (transaction) dùng ở đâu?** (P4, K)
- Ghi đề và các câu trong đề, sửa câu hỏi (xóa phương án cũ, chèn mới), ghi điểm cả bài: `ConnectionManager.inTransaction`.
- Hỏi vặn: "Mở ca và khóa đề có cùng giao dịch không?" → không; nói thật đây là giới hạn nhỏ, lần mở ca sau vẫn khóa đề.

---

## J. Đa luồng và JavaFX

**J1. Vì sao không gọi CSDL trên luồng giao diện? Nhóm làm thế nào?** (P1, P3, CB)
- JavaFX vẽ trên một luồng duy nhất; truy vấn chậm làm cửa sổ đứng. `BackgroundTasks.run(việc, khiXong, khiLỗi)` chạy
  việc trên luồng nền, đưa kết quả về luồng giao diện.

**J2. Chỗ nào có nhiều luồng cùng đụng một dữ liệu? Xử lý ra sao?** (P3, N)
- `AttemptState` (`synchronized`), hàng đợi của `AnswerSaver` (khối `synchronized`), `ProctoringService.lastCounted`
  (`ConcurrentHashMap`), các DAO trong bộ nhớ khóa trên cả CSDL giả.

**J3. FXML và controller liên kết thế nào?** (P1, CB)
- `fx:controller` và `@FXML` gắn trường với thành phần giao diện; `Navigator` nạp FXML và truyền tham số vào controller.

**J4. Esc và Alt+F4 bị chặn bằng cách nào?** (P3, K)
- `setFullScreenExitKeyCombination(KeyCombination.NO_MATCH)` và `setOnCloseRequest(Event::consume)` trong `ExamProctor`.
- Hỏi vặn: "Ctrl+Alt+Del thì sao?" → không chặn được từ ứng dụng Java; lúc đó cửa sổ mất tiêu điểm và bị tính vi phạm.

---

## K. Kiểm thử và chất lượng mã

**K1. Có bao nhiêu kiểm thử, kiểm thử những gì?** (Cả nhóm, CB)
- 134 kiểm thử JUnit 5: 112 chạy với DAO trong bộ nhớ, 22 với MySQL thật (8 ca bằng tài khoản `oes_client`). Thêm kiểm
  thử tải 60 thí sinh và danh sách kiểm thử tay.

**K2. DAO trong bộ nhớ để làm gì? Có đáng tin bằng MySQL không?** (P4, K)
- Chạy kiểm thử nghiệp vụ nhanh, không cần MySQL. Không thay được MySQL hoàn toàn (không có GRANT, collation), nên có thêm
  kiểm thử JDBC. Ví dụ thật: lỗi "Illegal mix of collations" chỉ kiểm thử với MySQL mới bắt được.

**K3. Kiểm thử tải đo được gì?** (P4, K)
- 60 thí sinh lưu bài liên tục 3 phút: p95 31 ms, chậm nhất 114 ms, 0 lỗi; chấm 60 bài trong 2,2 giây. Đo trên localhost,
  cần đo lại trong LAN phòng máy (NFR-01 yêu cầu dưới 2 giây).

**K4. Nhóm phát hiện lỗi nào muộn, sửa thế nào?** (Cả nhóm, N)
- Năm lỗi phát hiện khi rà mã viết tài liệu, mỗi lỗi có kiểm thử tái hiện ([DESIGN-CHANGES.md](DESIGN-CHANGES.md)):
  vi phạm lúc mất mạng bị mất, sinh đề rỗng, xóa câu nằm trong đề báo sai lỗi, "mức HARD" trong thông báo, gộp vi phạm
  theo đồng hồ máy trạm. Giảng viên thường đánh giá cao câu trả lời thẳng thắn kiểu này.

---

## L. Sửa mã tại chỗ

Giảng viên hay yêu cầu sửa một chỗ nhỏ rồi chạy lại. Luyện trước: sửa, chạy kiểm thử liên quan, rồi `git checkout -- <tệp>`
để trả lại.

| # | Yêu cầu | Sửa ở đâu | Kiểm bằng | Ai |
|---|---|---|---|---|
| L1 | Đổi ngưỡng vi phạm từ 5 thành 3 | `ViolationPolicy.MAX_VIOLATIONS` | `ViolationPolicyTest`, `SubmissionFlowTest` | P3 |
| L2 | Đổi thời gian gộp sự kiện 3 giây thành 2 giây | `ViolationPolicy.MERGE_WINDOW` | `SubmissionFlowTest.sựKiệnSátNhauĐượcGộpLàmMột` | P3 |
| L3 | Mật khẩu tối thiểu 8 ký tự | `User.MIN_PASSWORD_LENGTH` | kiểm thử đổi mật khẩu trong `StaffServicesTest` | P1 |
| L4 | Máy chấm 5 giây một lượt | `GradingScheduler.DEFAULT_PERIOD` | `GradingSchedulerTest` | P4 |
| L5 | Thêm cách chấm "đúng hết mới có điểm, sai một mất nửa" | lớp mới cài `GradingStrategy`, đăng ký trong `Exam` | thêm ca vào `SubmissionGraderTest` | P4 |
| L6 | Thêm mức độ "Rất khó" | `Difficulty`, cột `difficulty` trong `db/schema.sql` | `SeedToolTest` sẽ báo thiếu câu mức mới | P2 |
| L7 | Thông báo thiếu câu đổi cách viết | `QuestionBank.findShortages` | `ExamAndBankTest.thiếuCâuThìBáoRõThiếuỞĐâu` | P2 |
| L8 | Ẩn số lần vi phạm trên màn hình làm bài | `ExamController`, `exam.fxml` | chạy thử giao diện | P3 |

Hỏi vặn sau khi sửa: "Sửa xong làm sao biết không hỏng chỗ khác?" → chạy `scripts/test.sh` (112 kiểm thử) và chỉ ra kiểm
thử nào đỏ, vì sao.

---

## M. Phản biện và câu hỏi khó

**M1. Thí sinh dùng điện thoại hoặc máy thứ hai tra cứu thì sao?** (P3, N)
- Ngoài phạm vi phần mềm (không có camera, theo M1); phải do giám thị. Phần mềm chỉ phát hiện rời cửa sổ trên chính máy thi.

**M2. Thí sinh chạy ứng dụng trong máy ảo rồi tra cứu ở máy thật?** (P3, N)
- Không phát hiện được: sự kiện mất tiêu điểm chỉ thấy trong máy ảo. Trả lời thật; biện pháp là quản lý phòng máy.

**M3. 1000 thí sinh cùng thi có chạy được không?** (P4, N)
- Mỗi máy giữ kết nối riêng tới MySQL, chưa có pool kết nối (không được thêm thư viện). Đã đo 60 thí sinh. Muốn lớn hơn cần
  pool kết nối và đo lại; không khẳng định khi chưa đo.

**M4. Sao không dùng Spring, Hibernate cho nhanh?** (Cả nhóm, CB)
- Ràng buộc của giảng viên: chỉ Java, JavaFX, JDBC, Maven, JUnit. Tự lắp ráp trong `AppContext` thay Spring, tự viết SQL
  thay Hibernate; nhờ đó hiểu rõ phần các framework vẫn làm thay.

**M5. Nếu chỉ còn một tuần, nhóm làm thêm gì?** (Cả nhóm, K)
- Hai luồng ngoại lệ M1 còn thiếu: UC-08 5a (đổi một câu trong bản xem trước đề), UC-13 3a (không vào được toàn màn hình
  thì không cho thi); băm mật khẩu bằng PBKDF2; chạy đủ danh sách kiểm thử tay trên Windows.

**M6. Nhóm có dùng công cụ AI (ChatGPT, Claude…) không? Dùng thế nào?** (Cả nhóm, K)
- Trả lời trung thực, đúng với quy định của môn học. Điều giảng viên cần kiểm chứng là nhóm hiểu mã: chứng minh bằng cách
  giải thích được từng quyết định thiết kế và sửa mã tại chỗ (mục L).

**M7. Chỉ ra một chỗ trong mã mà em thấy chưa tốt.** (Mỗi người với phần của mình, N)
- P1: SHA-256 thay vì hàm băm chậm; nhật ký không tập trung.
- P2: "Tạo phiên bản mới" hai lần trên cùng một đề ra hai bản cùng là v2; chưa có chức năng sửa/xóa đề.
- P3: `ExamController` còn lớn; Alt+Tab mới kiểm bằng giả lập.
- P4: dùng chung `oes_client`; chưa có pool kết nối; cờ câu chấm lỗi không lưu.

---

## N. Quá trình làm việc của nhóm

**N1. Phân công thế nào? Em làm phần nào?** (Mỗi người)
- Theo [PHAN-CONG.md](PHAN-CONG.md): 4 phần theo tính năng, mỗi phần xuyên ba tầng. Trả lời bằng đoạn "Nói trong 1 phút"
  ở mục 0 tài liệu vấn đáp của mình.

**N2. Quản lý mã nguồn thế nào?** (Cả nhóm, CB)
- Git, mỗi phase một commit `phase N: ...`, các commit sửa lỗi riêng. Repo trên GitHub.

**N3. Làm sao biết bản nộp chạy được trên máy giảng viên?** (Cả nhóm, CB)
- `scripts/setup` dựng CSDL và dữ liệu mẫu bằng một lệnh; `docs/SETUP.md` có cách làm tay; jar build lại trên đúng hệ
  điều hành theo [RUNBOOK.md](RUNBOOK.md).

---

## Bảng tra nhanh

| Nếu bị hỏi về | Mở tệp |
|---|---|
| Phân quyền | `service/Permissions.java`, `model/User.java` và 3 lớp con |
| Mật khẩu | `util/PasswordHasher.java`, thủ tục `sp_student_change_password` trong `db/security.sql` |
| Bốn loại câu hỏi | `model/question/`, `factory/QuestionFactory.java` |
| Sinh đề | `model/ExamMatrix.java`, `model/QuestionBank.java`, `service/ExamService.java` |
| Giờ thi | `model/SessionWindow.java`, `client/ServerClock.java` |
| Vi phạm | `model/ViolationPolicy.java`, `ui/ExamProctor.java`, `service/ProctoringService.java` |
| Lưu bài | `ui/AnswerSaver.java`, `service/StudentExamService.java` |
| Chấm điểm | `grading/`, `service/GradingWorker.java`, `service/GradingScheduler.java` |
| Bảo mật CSDL | `db/security.sql`, `test/.../JdbcSecurityTest.java` |
| Lắp ráp đối tượng | `app/AppContext.java`, `app/ClientContext.java` |
| Khung giao diện | `ui/OesApplication.java`, `ui/Navigator.java`, `ui/BackgroundTasks.java` |

Đường dẫn ở trên tính từ `src/main/java/com/group2/oes/`.
