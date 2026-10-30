# Phần 1 – Người dùng, phân quyền, quản trị, khung ứng dụng

[README](../../README.md) · [Phân công và câu hỏi chung](../PHAN-CONG.md) · [Đoạn demo của phần này](../DEMO-SCRIPT.md#phần-1--đỗ-phạm-bảo-hoàng--giới-thiệu-và-quản-trị-2-phút) · Phần: **1** · [2](phan-2-cau-hoi-de-thi.md) · [3](phan-3-ca-thi-lam-bai.md) · [4](phan-4-cham-bai-ket-qua-csdl.md)

> Người phụ trách: **Đỗ Phạm Bảo Hoàng** (B24DCCE098) · Đọc kèm: [`docs/PHAN-CONG.md`](../../docs/PHAN-CONG.md) (câu hỏi chung)
>
> Đường dẫn rút gọn trong tài liệu: `oes/` = [`src/main/java/com/group2/oes/`](../../src/main/java/com/group2/oes/), `fxml/` = [`src/main/resources/fxml/`](../../src/main/resources/fxml/),
> `test/` = [`src/test/java/com/group2/oes/`](../../src/test/java/com/group2/oes/).

---

## Mục lục

- [0. Nói trong 1 phút](#0-nói-trong-1-phút)
- [1. Phạm vi](#1-phạm-vi)
- [2. Ranh giới với các phần khác](#2-ranh-giới-với-các-phần-khác)
- [3. Thứ tự đọc mã](#3-thứ-tự-đọc-mã)
- [4. Kiến thức Java/OOP nền](#4-kiến-thức-javaoop-nền)
- [5. Từng lớp chính](#5-từng-lớp-chính)
- [6. Luồng chạy đầu–cuối](#6-luồng-chạy-đầucuối)
- [7. CSDL của phần này](#7-csdl-của-phần-này)
- [8. Quy tắc nghiệp vụ](#8-quy-tắc-nghiệp-vụ)
- [9. SOLID và mẫu thiết kế](#9-solid-và-mẫu-thiết-kế)
- [10. Khác với thiết kế M3/M4](#10-khác-với-thiết-kế-m3m4)
- [11. Kiểm thử](#11-kiểm-thử)
- [12. Giới hạn đã biết](#12-giới-hạn-đã-biết)
- [13. Đoạn demo của mình](#13-đoạn-demo-của-mình)
- [14. Câu hỏi vấn đáp](#14-câu-hỏi-vấn-đáp)
- [15. Thuật ngữ](#15-thuật-ngữ)
- [16. Tự kiểm tra trước buổi vấn đáp](#16-tự-kiểm-tra-trước-buổi-vấn-đáp)

## 0. Nói trong 1 phút

> "Phần em là **nền móng** của cả hệ thống: ai là người dùng, người đó được làm gì, và hai ứng dụng khởi động ra sao.
>
> Về mô hình, em dùng **kế thừa**: lớp trừu tượng `User` có ba lớp con `Administrator`, `Instructor`, `Student`.
> Mỗi lớp con tự trả lời câu hỏi `canManage(subject)` – quản trị viên luôn được, giảng viên chỉ được với môn mình
> phụ trách, thí sinh không bao giờ được. Lớp `Permissions` ở tầng service gọi `canManage` mà không cần biết đối tượng
> là lớp con nào – đó là **đa hình**.
>
> Về bảo mật, mật khẩu được băm **SHA-256 kèm salt ngẫu nhiên** bằng thư viện JDK (`PasswordHasher`). Hai ứng dụng
> đăng nhập theo hai đường khác nhau: ứng dụng quản lý đọc bảng `app_user`, còn ứng dụng thi chỉ đọc khung nhìn
> `v_login` – khung nhìn này không trả mã băm của giảng viên và quản trị viên. Thí sinh đổi mật khẩu qua thủ tục lưu
> trữ `sp_student_change_password`, vì tài khoản CSDL của máy thí sinh không có quyền sửa bảng người dùng.
>
> Về quản trị: tạo, sửa, khóa tài khoản, đặt lại mật khẩu tạm, nhập danh sách sinh viên từ CSV, quản lý môn học,
> lớp học phần và phân công giảng viên, xem nhật ký thao tác.
>
> Về khung ứng dụng: `AppContext` và `ClientContext` là hai nơi duy nhất `new` các đối tượng (DIP);
> `OesApplication` là mẫu Template Method cho quy trình khởi động chung; `BackgroundTasks` đưa mọi lệnh gọi CSDL ra
> luồng nền để giao diện không đứng hình; `ErrorDialogs` là chỗ duy nhất hiện lỗi cho người dùng."

---

[Về mục lục](#mục-lục)

## 1. Phạm vi

### Yêu cầu chức năng (FR) – số hiệu lấy từ [`docs/PLAN.md`](../../docs/PLAN.md) mục 4 và [`docs/PROGRESS.md`](../../docs/PROGRESS.md)

| FR | Nội dung | Lớp chính | Màn hình | Trạng thái |
|---|---|---|---|---|
| FR-01 | Tạo, sửa, khóa, tìm tài khoản | `AuthService`, `UserDao` | Người dùng | Xong |
| FR-02 | Gán vai trò | `User` và 3 lớp con | Người dùng | Xong (M4) |
| FR-03 | Đăng nhập, đăng xuất | `AuthService`, `StudentAuthService` | Đăng nhập | Xong |
| FR-04 | Đổi mật khẩu, hồ sơ | `AuthService`, `StudentAuthService` | Hồ sơ, Đổi mật khẩu | Xong |
| FR-05 | Chặn thao tác ngoài quyền | `Permissions` | – | Xong (M4) |
| FR-06 | Nhập người dùng từ CSV | `UserImportService` | Người dùng | Xong |
| FR-07 | Môn học, lớp học phần | `SubjectService` | Môn học & lớp | Xong |

### Yêu cầu phi chức năng (NFR) phần này chịu trách nhiệm

| NFR | Nội dung | Ở đâu |
|---|---|---|
| NFR-03 | Mật khẩu băm SHA-256 + salt bằng thư viện JDK | `PasswordHasher` |
| NFR-05 | Ghi nhật ký đăng nhập, tạo đề, kết thúc bài do vi phạm | `AuditLog` (ghi), `AuditLogService` + màn hình Nhật ký (xem) |
| NFR-08 | Thông báo lỗi rõ nguyên nhân, không lộ ngoại lệ kỹ thuật | Cây `ExamAppException`, `ErrorDialogs` |
| NFR-09 | Ba tầng, controller không có logic | Khung `ui` + quy ước (xem Câu hỏi chung 2, 3) |
| NFR-04 (một phần) | Máy thí sinh dùng tài khoản riêng | `ClientContext`, `StudentLoginDao`, `v_login` |

### Use case: UC-01 (quản lý tài khoản), UC-02 (phân quyền), UC-03 (đăng nhập/đăng xuất), UC-04 (hồ sơ, đổi mật khẩu), UC-05 (môn học, lớp học phần).

### Quy tắc nghiệp vụ: BR-10 ("chỉ giảng viên phụ trách môn hoặc quản trị viên mới xem được kết quả của môn đó").
Phần 1 sở hữu **cơ chế** (`User.canManage`, `Permissions.requireManage`); phần 4 **áp dụng** nó trong `StatisticsService`.

### Phần này sở hữu
- Model: `User`, `Administrator`, `Instructor`, `Student`, `Role`, `Subject`, `CourseClass`.
- Toàn bộ gói `exception`.
- Service: `Permissions`, `AuthService`, `NewAccount`, `StudentAuthService`, `UserImportService`, `ImportResult`,
  `SubjectService`, `AuditLogService`.
- DAO: `UserDao`, `SubjectDao`, `StudentLoginDao` và các cài đặt `Jdbc...`, `Memory...`.
- Tiện ích: `PasswordHasher`, `AuditLog`, `CsvLine`.
- Lắp ráp: `AppContext`, `ClientContext`, `ConsoleDemo`, `LoggingSetup`.
- Khung giao diện: `OesApplication`, `AppShell`, `StaffApp`, `ExamClientApp`, `StaffLauncher`, `ClientLauncher`,
  `Navigator`, `UserSession`, `BackgroundTasks`, `ErrorDialogs`, `StaffWorkspace`, `FormDialog`, `Tables`, `Formats`.
- Màn hình: đăng nhập, đổi mật khẩu, chờ kết nối, trang chủ quản lý, hồ sơ, người dùng, biểu mẫu người dùng, môn học &
  lớp, nhật ký; [`css/app.css`](../../src/main/resources/css/app.css).
- CSDL: bảng `app_user`, `subject`, `course_class`, `instructor_subject`; khung nhìn `v_login`;
  thủ tục `sp_student_change_password`.

---

[Về mục lục](#mục-lục)

## 2. Ranh giới với các phần khác

Phần 1 là phần "bị gọi" nhiều nhất. Bảng dưới ghi **lớp.phương thức ở cả hai phía**.

| Phía phần khác | Gọi sang phần 1 | Để làm gì |
|---|---|---|
| Phần 2: `QuestionBankService` (`addQuestion`, `loadBank`, `updateQuestion`, `deleteQuestion`, `importFile`), `ExamService` (`createManual`, `list`, `newVersion`, `generate`) | `Permissions.requireManage(actor, subject)` | Giảng viên chỉ thao tác trên môn mình phụ trách |
| Phần 3: `ExamSessionService.createSession`, `studentsOfClass`, `listOfExam` | `Permissions.requireManage` | Chỉ mở ca thi cho đề thuộc môn mình |
| Phần 3: `ExamSessionService.allStudents` | `Permissions.requireStaff`, `UserDao.search(null, Role.STUDENT)` | Chọn thí sinh ghi danh từng người |
| Phần 3: `ExamSessionService.studentsOfClass` | `UserDao.findStudentsOfClass(classId)` | Ghi danh cả lớp (FR-19) |
| Phần 3: `StudentExamService` (`enter`, `saveAnswer`, `submit`, `refresh`, `review`...) | `Permissions.requireStudent(actor)` | Chỉ thí sinh mới vào thi; trả về luôn đối tượng `Student` |
| Phần 3: `ExamClientApp.showHome` | mở `SessionListController` | Trang chủ của ứng dụng thi là danh sách ca thi |
| Phần 3: `OesApplication.start`, `showServerSetup` | dùng `AppConfig.locate`, `AppConfig.save`, `ServerSetupController` | Tìm/lưu tệp cấu hình CSDL |
| Phần 4: `StatisticsService` (`statsFor`, `listResults`, `exportCsv`) | `Permissions.requireManage` | BR-10 |
| Phần 4: `JdbcSubmissionDao` | `JdbcUserDao.USER_COLUMNS`, `JdbcUserDao.mapUser` | Dựng đối tượng `Student` khi đọc bảng điểm |
| Phần 4: `JdbcErrors.translate` | tạo `DuplicateKeyException`, `DataAccessException`, `SessionRuleException` | Đổi `SQLException` thành ngoại lệ của phần 1 |
| Phần 4: `StaffApp.onConnected` (phần 1) | khởi động `GradingScheduler` (phần 4) | StaffApp đồng thời là máy chấm |
| Phần 2, 3, 4: `ExamService`, `ExamSessionService`, `ProctoringService`, `GradingWorker`... | `AuditLog.record(...)` | Ghi nhật ký (`TERMINATED_BY_VIOLATION`, `CREATE_SESSION`, `CREATE_EXAM`...) |
| Phần 1: `UsersController.onImport` | gọi `QuestionBankController.showImportResult(r)` (phần 2) | Dùng chung hộp thoại kết quả nhập tệp |
| Phần 1: `StaffHomeController.init` | mở `QuestionBankController`, `ExamListController` (phần 2), `StaffSessionsController` (phần 3) | Menu của giảng viên |

Ghi nhớ: **phần 1 cung cấp `actor` (người đang thao tác) và cơ chế kiểm tra quyền; các phần khác chỉ gọi một dòng
`Permissions.requireXxx(...)` ở đầu mỗi hàm service.**

---

[Về mục lục](#mục-lục)

## 3. Thứ tự đọc mã

1. [`oes/model/Role.java`](../../src/main/java/com/group2/oes/model/Role.java) – ba vai trò, tên tiếng Việt `displayName()`.
2. [`oes/model/User.java`](../../src/main/java/com/group2/oes/model/User.java) – lớp trừu tượng, quy tắc mật khẩu, `assignId` một lần, `equals` theo mã.
3. [`oes/model/Administrator.java`](../../src/main/java/com/group2/oes/model/Administrator.java), `Instructor.java`, `Student.java` – ba cách trả lời `canManage`.
4. [`oes/model/Subject.java`](../../src/main/java/com/group2/oes/model/Subject.java), `CourseClass.java` – vì sao `Subject.equals` so theo mã môn.
5. [`oes/util/PasswordHasher.java`](../../src/main/java/com/group2/oes/util/PasswordHasher.java) – định dạng `salt$hash`, so sánh thời gian hằng.
6. `oes/exception/*.java` – gốc `ExamAppException`, mã lỗi, `getUserMessage()`.
7. [`oes/service/Permissions.java`](../../src/main/java/com/group2/oes/service/Permissions.java) – bốn hàm kiểm tra quyền.
8. [`oes/dao/UserDao.java`](../../src/main/java/com/group2/oes/dao/UserDao.java), `SubjectDao.java`, `StudentLoginDao.java` – ba interface.
9. [`oes/service/AuthService.java`](../../src/main/java/com/group2/oes/service/AuthService.java) – đăng nhập phía quản lý, tạo/sửa/khóa/đặt lại mật khẩu.
10. [`oes/service/StudentAuthService.java`](../../src/main/java/com/group2/oes/service/StudentAuthService.java) – đăng nhập phía thí sinh, đổi mật khẩu qua thủ tục.
11. [`db/security.sql`](../../db/security.sql) mục 1–3 – `v_login`, `sp_student_change_password`, quyền `oes_client`.
12. [`oes/dao/jdbc/JdbcUserDao.java`](../../src/main/java/com/group2/oes/dao/jdbc/JdbcUserDao.java), `JdbcStudentLoginDao.java` – SQL thật.
13. [`oes/util/CsvLine.java`](../../src/main/java/com/group2/oes/util/CsvLine.java), [`oes/service/UserImportService.java`](../../src/main/java/com/group2/oes/service/UserImportService.java), `ImportResult.java` – nhập CSV.
14. [`oes/service/SubjectService.java`](../../src/main/java/com/group2/oes/service/SubjectService.java), [`oes/dao/jdbc/JdbcSubjectDao.java`](../../src/main/java/com/group2/oes/dao/jdbc/JdbcSubjectDao.java) – môn, lớp, phân công.
15. [`oes/util/AuditLog.java`](../../src/main/java/com/group2/oes/util/AuditLog.java), [`oes/service/AuditLogService.java`](../../src/main/java/com/group2/oes/service/AuditLogService.java), [`src/main/resources/logging.properties`](../../src/main/resources/logging.properties) – nhật ký.
16. [`oes/app/AppContext.java`](../../src/main/java/com/group2/oes/app/AppContext.java), `ClientContext.java` – nơi lắp ráp.
17. [`oes/ui/OesApplication.java`](../../src/main/java/com/group2/oes/ui/OesApplication.java), `AppShell.java`, `StaffApp.java`, `ExamClientApp.java` – khởi động.
18. [`oes/ui/Navigator.java`](../../src/main/java/com/group2/oes/ui/Navigator.java), `BackgroundTasks.java`, `ErrorDialogs.java`, `UserSession.java` – hạ tầng giao diện.
19. [`oes/ui/controller/LoginController.java`](../../src/main/java/com/group2/oes/ui/controller/LoginController.java), `ChangePasswordController.java` + [`fxml/login.fxml`](../../src/main/resources/fxml/login.fxml) – màn hình dùng chung.
20. [`oes/ui/StaffWorkspace.java`](../../src/main/java/com/group2/oes/ui/StaffWorkspace.java), `controller/StaffHomeController.java` – trang chủ, menu theo vai trò.
21. `controller/UsersController.java`, `UserFormController.java`, `SubjectsController.java`, `AuditLogController.java`,
    `ProfileController.java` – các màn hình quản trị.
22. [`oes/app/ConsoleDemo.java`](../../src/main/java/com/group2/oes/app/ConsoleDemo.java) – chạy cả luồng nghiệp vụ không cần giao diện.
23. [`test/StaffServicesTest.java`](../../src/test/java/com/group2/oes/StaffServicesTest.java) (nửa đầu), `AdminAndResultsTest.java` (3 ca đầu), `ErrorCodeTest.java`, `CsvLineTest.java`.

---

[Về mục lục](#mục-lục)

## 4. Kiến thức Java/OOP nền

Chỉ những khái niệm dùng trong phần này, giải thích bằng mã của phần.

**Lớp trừu tượng và phương thức trừu tượng.** `User` khai báo `abstract`, không `new User(...)` được. Hai phương thức
`getRole()` và `canManage(Subject)` không có thân: mọi lớp con **bắt buộc** cài đặt, quên là biên dịch lỗi.

Mã nguồn: [oes/model/User.java](../../src/main/java/com/group2/oes/model/User.java)

```java
// oes/model/User.java
public abstract class User {
    // ...
    public abstract Role getRole();

    /** Người dùng này có được thao tác trên nội dung của môn học hay không. */
    public abstract boolean canManage(Subject subject);
```

**Constructor `protected`.** `User(...)` là `protected`: chỉ lớp con gọi được qua `super(...)`.

**Đa hình (polymorphism).** Biến kiểu `User` có thể trỏ tới `Instructor`; lời gọi `actor.canManage(subject)` chạy
phiên bản của lớp thật lúc chạy (dynamic dispatch). `Permissions.requireManage` dựa hoàn toàn vào điều này.

**`enum` có trường.** `Role` mang thêm `displayName` ("Quản trị viên"...). `Role.valueOf("STUDENT")` đổi chuỗi trong
cột `role` thành hằng enum (dùng trong `JdbcUserDao.mapUser`).

**Switch biểu thức (Java 14+) và `yield`.** `AuthService.createAccount` và `JdbcUserDao.mapUser` dùng
`switch (role) { case ADMIN -> ...; }`. Switch biểu thức trên enum phải **phủ hết** các hằng, nên thêm vai trò mới mà
quên một nhánh thì trình biên dịch báo lỗi. Nhánh nhiều dòng trả giá trị bằng `yield`.

**Pattern matching `instanceof` (Java 16+).** `if (u instanceof Instructor ins) { subjectDao.loadSubjectsOf(ins); }` –
vừa kiểm tra kiểu vừa ép kiểu, không phải viết `(Instructor) u`.

**`record`.** `NewAccount`, `ImportResult` là record: lớp dữ liệu bất biến, tự sinh constructor, getter (`a.role()`),
`equals`, `hashCode`, `toString`. `ImportResult` dùng *compact constructor* để chép danh sách lỗi thành bản chỉ đọc.
Ngoài ra có record nhỏ `Navigator.Loaded`, `StaffHomeController.MenuItem`, và record cục bộ `Details` trong
`SubjectsController.loadDetails`.

**Generic.** `<T extends User> T insert(T user)` trong `UserDao`: truyền `Student` vào thì nhận lại `Student`, không phải
ép kiểu. `OesApplication<C>`: `C` là `AppContext` hoặc `ClientContext`. `Navigator.show` có tham số kiểu `<C>`: kiểu controller.

**`Optional`.** `findByUsername` trả `Optional<User>` thay vì `null`, buộc nơi gọi xử lý trường hợp không có:
`.filter(x -> x.checkPassword(password)).orElseThrow(...)`.

**Lambda, method reference, interface hàm.** `Supplier<T>` (việc chạy nền), `Consumer<T>` (khi xong),
`StudentAuthService::invalidLogin`, `ConnectionManager.SqlWork` (giống `Function` nhưng được ném `SQLException`).

**Ngoại lệ không kiểm tra (unchecked).** `ExamAppException extends RuntimeException`: đi xuyên DAO → service → view mà
không phải khai báo `throws` ở mọi tầng. `serialVersionUID` có mặt vì `Throwable` cài đặt interface Serializable và `-Xlint:all`
sẽ cảnh báo nếu thiếu.

**Lớp tiện ích (utility class).** `PasswordHasher`, `AuditLog`, `CsvLine`, `Permissions`: `final`, constructor `private`,
toàn hàm `static` – không ai tạo được đối tượng hay kế thừa.

**Phạm vi truy cập package-private.** `Permissions` không có chữ `public`: chỉ các lớp trong gói `service` gọi được.
Controller ở gói `ui.controller` **không thể** gọi – buộc việc kiểm tra quyền phải nằm ở service.

**Phương thức `default` trong interface.** `AppShell.onLoggedIn`, `showLogin`, `logout` có thân sẵn; `StaffApp` và
`ExamClientApp` dùng chung mà không viết lại.

**`equals`/`hashCode`.** `Subject` bằng nhau khi cùng `code`; `User` bằng nhau khi cùng `userId`. Hai hàm phải đi cặp:
`HashSet`/`LinkedHashSet` dùng `hashCode` để tìm ngăn, rồi `equals` để so.

**Collection chỉ đọc.** `Instructor.getSubjects()` trả `Collections.unmodifiableSet(subjects)`; `ImportResult` dùng
`List.copyOf`. Bên ngoài không sửa được dữ liệu bên trong đối tượng (đóng gói – encapsulation).

**try-with-resources.** Mọi `PreparedStatement`, `ResultSet`, `CallableStatement` mở trong `try (...)` để tự đóng kể cả
khi lỗi.

**Đa luồng.** `BackgroundTasks` dùng `ExecutorService` 4 luồng daemon + `javafx.concurrent.Task`; `UserSession` có các
hàm `synchronized`.

---

[Về mục lục](#mục-lục)

## 5. Từng lớp chính

### 5.1. `User` ([`oes/model/User.java`](../../src/main/java/com/group2/oes/model/User.java))

**Vai trò:** lớp cha trừu tượng của mọi người dùng.
**Dữ liệu:** `userId`, `username` (`final`), `passwordHash`, `fullName`, `email`, `active`, `mustChangePassword`.
**Bất biến:**
- `username` và `passwordHash` không được `null` (`Objects.requireNonNull`).
- `assignId` chỉ gán được một lần – mã do CSDL sinh, gán hai lần là lỗi lập trình.
- Mật khẩu mới phải có ít nhất `MIN_PASSWORD_LENGTH` = 6 ký tự.

Mã nguồn: [oes/model/User.java](../../src/main/java/com/group2/oes/model/User.java)

```java
// oes/model/User.java
/** Chỉ gán được một lần, khi DAO vừa thêm người dùng vào CSDL. */
public void assignId(int id) {
    if (this.userId != 0) {
        throw new IllegalStateException("Người dùng đã có mã " + userId);
    }
    this.userId = id;
}

public static String hashNewPassword(String newRaw) {
    ValidationException.require(newRaw != null && newRaw.length() >= MIN_PASSWORD_LENGTH, "PASSWORD_TOO_SHORT",
            "Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.");
    return PasswordHasher.hash(newRaw);
}

/** Gán mã băm mới (do hashNewPassword sinh ra) và bỏ cờ buộc đổi mật khẩu. */
public void applyNewPasswordHash(String newHash) {
    this.passwordHash = Objects.requireNonNull(newHash);
    this.mustChangePassword = false;
}

/** Quản trị viên đặt lại mật khẩu: người dùng phải đổi mật khẩu ở lần đăng nhập sau. */
public void resetPassword(String temporaryRaw) {
    changePassword(temporaryRaw);
    this.mustChangePassword = true;
}
```

**Vì sao đổi mật khẩu tách hai bước (`hashNewPassword` + `applyNewPasswordHash`)?** Phía thí sinh, CSDL phải nhận mã
băm mới **trước** (qua thủ tục lưu trữ), rồi mới gán vào đối tượng. Nếu gán trước mà CSDL từ chối, đối tượng trong
bộ nhớ và CSDL lệch nhau. Phía quản lý dùng `changePassword(newRaw)` gộp hai bước vì `UserDao.update` ghi ngay sau đó.

**Đọc kỹ `resetPassword`:** `changePassword` gọi `applyNewPasswordHash`, hàm này đặt `mustChangePassword = false`;
dòng tiếp theo bật lại thành `true`. Thứ tự này đúng: mật khẩu tạm vẫn phải qua kiểm tra độ dài, và sau cùng cờ buộc đổi
phải bật.

**Vì sao `hashNewPassword` và `requireConfirmed` là `static`?** Chúng không cần dữ liệu của một người dùng cụ thể.
`StudentAuthService` gọi `User.hashNewPassword` trước khi có kết quả từ CSDL; `ChangePasswordController.onSave` gọi
`User.requireConfirmed(newPw, confirm)` – quy tắc "nhập lại phải khớp" nằm ở model, controller không tự so sánh.

**`equals` theo `userId`:** `AuthService.setActive` dùng `actor.equals(target)` để chặn tự khóa mình. Hai đối tượng
khác nhau (một đọc lúc đăng nhập, một đọc từ bảng người dùng) vẫn được coi là cùng một người.

### 5.2. Ba lớp con và `Role`

Mã nguồn: [oes/model/Administrator.java](../../src/main/java/com/group2/oes/model/Administrator.java)

```java
// oes/model/Administrator.java
/** Quản trị viên thao tác được trên mọi môn học. */
@Override
public boolean canManage(Subject subject) {
    return true;
}

// oes/model/Instructor.java
// Set vì một môn chỉ được phân công một lần; LinkedHashSet để giữ thứ tự hiển thị.
private final Set<Subject> subjects = new LinkedHashSet<>();
// ...
/** Giảng viên chỉ quản lý môn mình được phân công. */
@Override
public boolean canManage(Subject subject) {
    return subjects.contains(subject);
}

// oes/model/Student.java
/** Thí sinh không quản lý nội dung học thuật. */
@Override
public boolean canManage(Subject subject) {
    return false;
}
```

- `Student` có thêm `studentCode` (`final`, bắt buộc) và `courseClass` (có thể `null` = chưa xếp lớp).
- `Instructor` giữ tập môn được phân công. **Tập này nạp lúc đăng nhập** (`AuthService.login` gọi
  `subjectDao.loadSubjectsOf(ins)`), và được cập nhật khi `JdbcSubjectDao.assignInstructor`/`unassignInstructor`
  chạy trên chính đối tượng đó.
- **Vì sao dùng kế thừa thay cho một cột "vai trò" + `if`?** Quyết định từ M2: hành vi khác nhau theo vai trò
  (`canManage`) được đặt vào đúng lớp. Thêm vai trò mới là thêm một lớp con, không phải sửa chuỗi `if` rải khắp
  service. Hệ quả: **vai trò cố định sau khi tạo** (một đối tượng không đổi được lớp), nên biểu mẫu sửa người dùng
  khóa ô vai trò (`roleBox.setDisable(true)` trong `UserFormController.init`).

### 5.3. `Subject`, `CourseClass`

Mã nguồn: [oes/model/Subject.java](../../src/main/java/com/group2/oes/model/Subject.java)

```java
// oes/model/Subject.java
// Hai môn học bằng nhau khi trùng mã. Cần thiết để List.contains / Set hoạt động đúng.
@Override
public boolean equals(Object o) {
    return o instanceof Subject other && code.equals(other.code);
}

@Override
public int hashCode() {
    return code.hashCode();
}
```

**Vì sao quan trọng:** `Instructor.canManage` gọi `subjects.contains(subject)`. Đối tượng `Subject` trong tập của giảng
viên (nạp lúc đăng nhập) và đối tượng `Subject` gắn với câu hỏi/đề (đọc ở chỗ khác) là **hai đối tượng khác nhau**.
Nếu không ghi đè `equals`/`hashCode`, `contains` so theo địa chỉ bộ nhớ và luôn trả `false` – giảng viên mất quyền
trên chính môn của mình.

`Subject` và `CourseClass` đều **bất biến** (mọi trường `final`). Đổi tên môn hay sửa lớp là tạo đối tượng mới cùng mã
(`SubjectService.renameSubject`, `updateClass`).

### 5.4. `PasswordHasher` ([`oes/util/PasswordHasher.java`](../../src/main/java/com/group2/oes/util/PasswordHasher.java))

Mã nguồn: [oes/util/PasswordHasher.java](../../src/main/java/com/group2/oes/util/PasswordHasher.java)

```java
// oes/util/PasswordHasher.java
public static String hash(String raw) {
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);
    return encode(salt) + "$" + encode(digest(salt, raw));
}

public static boolean matches(String raw, String stored) {
    if (raw == null || stored == null) {
        return false;
    }
    int sep = stored.indexOf('$');
    if (sep < 0) {
        return false;
    }
    byte[] salt = Base64.getDecoder().decode(stored.substring(0, sep));
    byte[] expected = Base64.getDecoder().decode(stored.substring(sep + 1));
    // so sánh thời gian hằng, tránh đoán mật khẩu qua thời gian phản hồi
    return MessageDigest.isEqual(expected, digest(salt, raw));
}
```

Giải thích từng quyết định:
- **Băm (hash), không mã hóa (encrypt):** hàm băm một chiều, không giải ngược được. Lộ CSDL cũng không lộ mật khẩu gốc.
- **Salt 16 byte ngẫu nhiên từ `SecureRandom`:** hai người cùng mật khẩu `123456` có hai mã băm khác nhau, nên kẻ tấn
  công không dùng được bảng tra sẵn (rainbow table) và không nhận ra ai trùng mật khẩu với ai. `SecureRandom` thay vì
  `Random` vì `Random` đoán trước được.
- **Định dạng `salt$hash` (Base64):** salt phải lưu cùng mã băm để lúc kiểm tra băm lại đúng như cũ. Base64 không
  chứa ký tự `$` nên tách bằng `indexOf('$')` an toàn.
- **`MessageDigest.isEqual`:** so sánh hết mọi byte dù sai từ byte đầu (thời gian hằng). `Arrays.equals` dừng ở byte
  sai đầu tiên, về lý thuyết cho phép đoán dần qua thời gian phản hồi.
- **Chuỗi không có `$` thì `matches` trả `false`:** đây là lý do mã băm rỗng `''` mà `v_login` trả cho giảng viên không
  bao giờ khớp mật khẩu nào.
- **Chỉ dùng JDK** (`java.security`): ràng buộc "không thêm thư viện".

### 5.5. Hệ ngoại lệ ([`oes/exception/`](../../src/main/java/com/group2/oes/exception/))

```
RuntimeException
 └─ ExamAppException (abstract; getCode(), getUserMessage())
     ├─ ValidationException        – dữ liệu nhập sai; có hàm tiện ích require(điều kiện, mã, câu)
     ├─ AccessDeniedException      – ngoài quyền
     ├─ SessionRuleException       – quy tắc ca thi (phần 3 dùng nhiều nhất)
     └─ DataAccessException        – lỗi kỹ thuật CSDL, mã "DATA_ACCESS"
         └─ DuplicateKeyException  – trùng khóa duy nhất
```

Mã nguồn: [oes/exception/ExamAppException.java](../../src/main/java/com/group2/oes/exception/ExamAppException.java)

```java
// oes/exception/ExamAppException.java
public abstract class ExamAppException extends RuntimeException {
    // ...
    private final String code;

    /** Mã lỗi ổn định, dùng để ghi log và viết kiểm thử. */
    public String getCode() {
        return code;
    }

    /** Thông điệp hiển thị cho người dùng cuối. */
    public String getUserMessage() {
        return getMessage();
    }
}

// oes/exception/ValidationException.java
/** Ném ra nếu điều kiện sai. Giúp các hàm kiểm tra viết gọn trên một dòng. */
public static void require(boolean condition, String code, String message) {
    if (!condition) {
        throw new ValidationException(code, message);
    }
}
```

**Vì sao có mã lỗi riêng ngoài câu thông báo?** Câu tiếng Việt có thể sửa chữ bất cứ lúc nào; mã lỗi thì **ổn định**.
Kiểm thử so mã (`assertEquals("WRONG_APP", ...getCode())`) nên sửa câu chữ không làm hỏng kiểm thử. Biểu mẫu câu hỏi
(phần 2) dùng mã để biết hiện lỗi dưới ô nào.

**Vì sao `DataAccessException` ghi đè `getUserMessage()`?** `getMessage()` của nó chứa chi tiết SQL (để ghi log);
người dùng chỉ thấy "Không kết nối được tới máy chủ dữ liệu. Vui lòng thử lại hoặc báo giám thị."
`DuplicateKeyException` lại ghi đè lần nữa (Phase 7) vì kết nối vẫn tốt, chỉ là dữ liệu trùng. Lưu ý:
`DuplicateKeyException` **không có mã riêng**, nó kế thừa mã `DATA_ACCESS`; service thường bắt nó và ném lại một
`ValidationException` có mã nghiệp vụ (`USERNAME_TAKEN`, `SUBJECT_CODE_TAKEN`, `CLASS_CODE_TAKEN`).

**Bảng mã lỗi của phần 1** (câu báo trích nguyên văn; phần trong `<...>` là giá trị ghép vào lúc chạy):

| Mã | Lớp ngoại lệ | Nơi ném | Câu người dùng thấy |
|---|---|---|---|
| `INVALID_LOGIN` | Validation | `AuthService.login`, `StudentAuthService.invalidLogin` | Sai tên đăng nhập hoặc mật khẩu. Hãy kiểm tra lại (chú ý phím Caps Lock). |
| `ACCOUNT_LOCKED` | AccessDenied | `AuthService.login`, `StudentAuthService.login` | Tài khoản đã bị khóa. Hãy liên hệ quản trị viên. |
| `WRONG_APP` | AccessDenied | `AuthService.login` | Tài khoản thí sinh hãy dùng ứng dụng thi (ExamClientApp). |
| `WRONG_APP` | AccessDenied | `StudentAuthService.login` | Tài khoản giảng viên và quản trị viên hãy dùng ứng dụng quản lý (StaffApp). |
| `ROLE_REQUIRED` | AccessDenied | `Permissions.requireRole(actor, Role.ADMIN)` | Chức năng này chỉ dành cho quản trị viên. |
| `ROLE_REQUIRED` | AccessDenied | `Permissions.requireStaff` | Chức năng này dành cho giảng viên và quản trị viên. |
| `ROLE_REQUIRED` | AccessDenied | `Permissions.requireStudent` | Chức năng này chỉ dành cho thí sinh. |
| `SUBJECT_FORBIDDEN` | AccessDenied | `Permissions.requireManage` | Bạn không phụ trách môn `<mã môn>`. |
| `NOT_LOGGED_IN` | AccessDenied | `AuthService.requireLoggedIn` | Bạn chưa đăng nhập. |
| `ROLE_EMPTY` | Validation | `AuthService.createAccount` | Hãy chọn vai trò. |
| `USERNAME_INVALID` | Validation | `AuthService.createAccount` | Tên đăng nhập từ 3 tới 50 ký tự, chỉ gồm chữ không dấu, số và . _ - |
| `STUDENT_CODE_INVALID` | Validation | `AuthService.createAccount` | Thí sinh cần mã sinh viên (không có khoảng trắng). |
| `STUDENT_CODE_INVALID` | Validation | `UserImportService.parse` | mã sinh viên trống hoặc có khoảng trắng. |
| `USERNAME_TAKEN` | Validation | `AuthService.createUser` | Tên đăng nhập "`<tên>`" hoặc mã sinh viên đã có người dùng. |
| `PASSWORD_TOO_SHORT` | Validation | `User.hashNewPassword` | Mật khẩu mới phải có ít nhất 6 ký tự. |
| `PASSWORD_MISMATCH` | Validation | `User.requireConfirmed` | Mật khẩu nhập lại không khớp với mật khẩu mới. |
| `WRONG_PASSWORD` | Validation | `AuthService.changePassword`, `StudentAuthService.changePassword` | Mật khẩu cũ không đúng. |
| `PASSWORD_CHANGED_ELSEWHERE` | Validation | `StudentAuthService.changePassword` | Mật khẩu vừa được đổi ở nơi khác. Hãy đăng xuất rồi đăng nhập lại. |
| `SELF_LOCK` | Validation | `AuthService.setActive` | Không thể tự khóa tài khoản của chính mình. |
| `NAME_EMPTY` | Validation | `AuthService.validateProfile` | Họ tên không được để trống. |
| `NAME_EMPTY` | Validation | `SubjectService.requireName` | Tên môn không được để trống. |
| `EMAIL_INVALID` | Validation | `AuthService.validateProfile` | Email "`<email>`" không đúng dạng, ví dụ đúng: ten@ptit.edu.vn. |
| `CSV_COLUMNS` | Validation | `UserImportService.parse` | cần 4 cột (ma_sv,ho_ten,email,ma_lop), có `<n>`. |
| `CLASS_NOT_FOUND` | Validation | `UserImportService.parse` | không có lớp học phần `<mã lớp>`. |
| `CODE_INVALID` | Validation | `SubjectService.requireCode` | Hãy nhập mã môn (không có khoảng trắng, tối đa 20 ký tự). (hoặc "mã lớp") |
| `SUBJECT_CODE_TAKEN` | Validation | `SubjectService.createSubject` | Mã môn `<mã>` đã tồn tại. |
| `SUBJECT_IN_USE` | Validation | `SubjectService.deleteSubject` | Môn `<mã>` đã có câu hỏi, đề thi hoặc lớp học phần nên không xóa được. |
| `CLASS_CODE_TAKEN` | Validation | `SubjectService.createClass`, `updateClass` | Mã lớp `<mã>` đã tồn tại. |
| `DATA_ACCESS` | DataAccess | `ConnectionManager`, `JdbcErrors.translate` | Không kết nối được tới máy chủ dữ liệu. Vui lòng thử lại hoặc báo giám thị. |
| `CONFIG_WRITE` | Validation | `OesApplication.save` | Đã kết nối được nhưng không ghi được tệp cấu hình ... |

Ghi chú: ba lỗi nhập CSV (`STUDENT_CODE_INVALID` bản của `parse`, `CSV_COLUMNS`, `CLASS_NOT_FOUND`) viết chữ thường ở đầu vì chúng được
ghép sau "Dòng N: " trong danh sách lỗi.

### 5.6. `Permissions` ([`oes/service/Permissions.java`](../../src/main/java/com/group2/oes/service/Permissions.java))

Mã nguồn: [oes/service/Permissions.java](../../src/main/java/com/group2/oes/service/Permissions.java)

```java
// oes/service/Permissions.java
/** Các kiểm tra quyền dùng chung cho tầng service (FR-05, BR-10). */
final class Permissions {

    static void requireRole(User actor, Role role) {
        if (actor == null || actor.getRole() != role) {
            throw new AccessDeniedException("ROLE_REQUIRED",
                    "Chức năng này chỉ dành cho " + role.displayName().toLowerCase() + ".");
        }
    }

    /** Đa hình ở đây: không cần biết actor là lớp con nào, canManage tự trả lời đúng. */
    static void requireManage(User actor, Subject subject) {
        if (actor == null || !actor.canManage(subject)) {
            throw new AccessDeniedException("SUBJECT_FORBIDDEN",
                    "Bạn không phụ trách môn " + subject.getCode() + ".");
        }
    }
    // ... requireStaff, requireStudent
```

- **Bốn hàm, bốn loại kiểm tra:** đúng một vai trò (`requireRole`), quyền trên môn (`requireManage`), là nhân viên
  (`requireStaff`), là thí sinh (`requireStudent`, trả về luôn `Student` để nơi gọi khỏi ép kiểu).
- **`actor == null` cũng bị từ chối** – service không bao giờ chạy với "người vô danh".
- **Vì sao `requireManage` không viết `if (actor instanceof Administrator) ... else if (actor instanceof Instructor)`?**
  Vì như vậy mỗi khi thêm vai trò phải sửa `Permissions` (vi phạm OCP). Gọi `canManage` thì quyết định nằm trong
  lớp con.
- **Vì sao package-private?** Chỉ service được kiểm tra quyền. Giao diện có ẩn nút (ví dụ menu theo vai trò ở
  `StaffHomeController`) chỉ để gọn; quyền thật luôn được kiểm ở service, nên giao diện quên ẩn nút cũng không sao.

### 5.7. `AuthService` ([`oes/service/AuthService.java`](../../src/main/java/com/group2/oes/service/AuthService.java))

Phía ứng dụng quản lý: đăng nhập, quản lý người dùng (UC-01 → UC-04). Nhận `UserDao` và `SubjectDao` qua constructor.

Mã nguồn: [oes/service/AuthService.java](../../src/main/java/com/group2/oes/service/AuthService.java)

```java
// oes/service/AuthService.java
public User login(String username, String password) {
    User u = userDao.findByUsername(username == null ? "" : username.strip())
            .filter(x -> x.checkPassword(password))
            .orElseThrow(() -> new ValidationException("INVALID_LOGIN", "Sai tên đăng nhập hoặc mật khẩu. Hãy kiểm tra lại (chú ý phím Caps Lock)."));
    if (!u.isActive()) {
        throw new AccessDeniedException("ACCOUNT_LOCKED", "Tài khoản đã bị khóa. Hãy liên hệ quản trị viên.");
    }
    if (u.getRole() == Role.STUDENT) {
        throw new AccessDeniedException("WRONG_APP", "Tài khoản thí sinh hãy dùng ứng dụng thi (ExamClientApp).");
    }
    if (u instanceof Instructor ins) {
        subjectDao.loadSubjectsOf(ins);
    }
    AuditLog.record(u, "LOGIN", u.getUsername());
    return u;
}
```

**Thứ tự kiểm tra có chủ đích:**
1. Sai tên **hoặc** sai mật khẩu → **cùng một câu**. Người dò mật khẩu không biết tên đăng nhập nào có thật.
2. Chỉ sau khi đúng mật khẩu mới báo "đã khóa" hay "dùng nhầm ứng dụng". Người không biết mật khẩu không biết được
   tài khoản có tồn tại, có bị khóa, là vai trò gì (DESIGN-CHANGES Phase 2 mục 4).
3. Giảng viên: nạp danh sách môn phụ trách để `canManage` có dữ liệu.

**Tạo tài khoản theo vai trò:**

Mã nguồn: [oes/service/AuthService.java](../../src/main/java/com/group2/oes/service/AuthService.java)

```java
// oes/service/AuthService.java – createAccount
String username = a.username().strip().toLowerCase(Locale.ROOT);
String hash = User.hashNewPassword(a.password());
String email = blankToNull(a.email());
User u = switch (a.role()) {
    case ADMIN -> new Administrator(0, username, hash, a.fullName(), email, true);
    case INSTRUCTOR -> new Instructor(0, username, hash, a.fullName(), email, true);
    case STUDENT -> {
        ValidationException.require(a.studentCode() != null && a.studentCode().strip().matches("\\S+"),
                "STUDENT_CODE_INVALID", "Thí sinh cần mã sinh viên (không có khoảng trắng).");
        Student s = new Student(0, username, hash, a.fullName(), email, true,
                a.studentCode().strip().toUpperCase(Locale.ROOT));
        s.setCourseClass(a.courseClass());
        yield s;
    }
};
u.setMustChangePassword(true);   // mật khẩu do quản trị viên đặt: người dùng phải đổi ở lần đầu
return createUser(actor, u);
```

- **Vì sao chọn lớp con ở service, không ở controller?** Chọn lớp con theo vai trò là logic nghiệp vụ; controller chỉ
  gom ô nhập thành record `NewAccount` (DESIGN-CHANGES Phase 4–5 mục 3).
- **Tên đăng nhập viết thường, mã sinh viên viết hoa** (`Locale.ROOT` để không phụ thuộc ngôn ngữ máy – tránh lỗi kiểu
  chữ "I" của tiếng Thổ Nhĩ Kỳ).
- `createUser` kiểm tra quyền quản trị, kiểm tra họ tên/email, rồi gọi `userDao.insert`; bắt `DuplicateKeyException`
  và đổi thành `USERNAME_TAKEN` với câu dễ hiểu.

**Các hàm khác:**
- `searchUsers(actor, keyword, role)` – chỉ quản trị viên.
- `updateUser(actor, target, fullName, email, courseClass)` – sửa họ tên, email; với `Student` sửa cả lớp.
- `setActive(actor, target, active)` – **khóa thay vì xóa**: người dùng đã có bài thi (`submission.student_id` là khóa
  ngoại tới `app_user`) không thể xóa mà không mất lịch sử. Có kiểm tra `SELF_LOCK`.
- `resetPassword(actor, target)` – sinh mật khẩu tạm 8 ký tự từ `TEMP_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"`
  (bỏ `i`, `l`, `o`, `0`, `1` vì hay bị đọc nhầm khi đọc miệng hoặc chép tay), trả về cho quản trị viên đọc.
- `changePassword(actor, oldPassword, newPassword)` – phải đúng mật khẩu cũ (`WRONG_PASSWORD`).
- `updateProfile(actor, fullName, email)` – người dùng tự sửa họ tên, email.

### 5.8. `StudentAuthService` ([`oes/service/StudentAuthService.java`](../../src/main/java/com/group2/oes/service/StudentAuthService.java))

Đăng nhập trên **ứng dụng thi**, chỉ nhận thí sinh. Chỉ biết `StudentLoginDao`.

Mã nguồn: [oes/service/StudentAuthService.java](../../src/main/java/com/group2/oes/service/StudentAuthService.java)

```java
// oes/service/StudentAuthService.java
public User login(String username, String password) {
    User u = loginDao.findByUsername(username == null ? "" : username.strip())
            .orElseThrow(StudentAuthService::invalidLogin);
    if (u.getRole() != Role.STUDENT) {
        throw new AccessDeniedException("WRONG_APP",
                "Tài khoản giảng viên và quản trị viên hãy dùng ứng dụng quản lý (StaffApp).");
    }
    if (!u.checkPassword(password)) {
        throw invalidLogin();
    }
    if (!u.isActive()) {
        throw new AccessDeniedException("ACCOUNT_LOCKED", "Tài khoản đã bị khóa. Hãy liên hệ quản trị viên.");
    }
    AuditLog.record(u, "LOGIN", "ứng dụng thi");
    return u;
}
```

**Vì sao ở đây báo `WRONG_APP` *trước* khi kiểm tra mật khẩu (ngược với `AuthService`)?** `v_login` trả mã băm rỗng
cho giảng viên/quản trị viên, nên máy thí sinh **không thể** kiểm tra mật khẩu của họ – `PasswordHasher.matches` luôn
`false` với chuỗi không có `$`. Nếu kiểm mật khẩu trước, giảng viên gõ đúng mật khẩu vẫn nhận "sai mật khẩu" – khó hiểu.
Cái giá: ai cũng biết được một tên đăng nhập có phải giảng viên không (xem mục 12).

**Đổi mật khẩu thí sinh:**

Mã nguồn: [oes/service/StudentAuthService.java](../../src/main/java/com/group2/oes/service/StudentAuthService.java)

```java
// oes/service/StudentAuthService.java
public void changePassword(User actor, String oldPassword, String newPassword) {
    Permissions.requireStudent(actor);
    ValidationException.require(actor.checkPassword(oldPassword), "WRONG_PASSWORD", "Mật khẩu cũ không đúng.");
    String newHash = User.hashNewPassword(newPassword);
    if (!loginDao.changePassword(actor.getUserId(), actor.getPasswordHash(), newHash)) {
        throw new ValidationException("PASSWORD_CHANGED_ELSEWHERE",
                "Mật khẩu vừa được đổi ở nơi khác. Hãy đăng xuất rồi đăng nhập lại.");
    }
    actor.applyNewPasswordHash(newHash);
    AuditLog.record(actor, "CHANGE_PASSWORD", actor.getUsername());
}
```

Truyền mã băm **cũ** xuống để thủ tục "so sánh rồi mới ghi" (compare-and-set): nếu quản trị viên vừa đặt lại mật khẩu
trong lúc thí sinh đang đăng nhập, mã băm trong CSDL đã khác, thủ tục không ghi và trả 0.

### 5.9. Ba interface DAO

- `UserDao`: `findByUsername`, `findById`, `insert`, `update`, `findStudentsOfClass`, `findAll`, `search`.
- `SubjectDao`: môn (`insert`, `findByCode`, `findAll`, `update`, `delete`, `isInUse`), lớp (`insertClass`,
  `updateClass`, `findClassByCode`, `findClasses`), phân công (`assignInstructor`, `unassignInstructor`,
  `findInstructorsOf`, `loadSubjectsOf`).
- `StudentLoginDao`: **chỉ** `findByUsername` và `changePassword`.

**Vì sao tách `StudentLoginDao` khỏi `UserDao` (ISP)?** Ứng dụng thi chỉ cần hai việc đó. Nếu nó cầm `UserDao`, về mặt
kiểu dữ liệu nó gọi được `insert`, `update`, `findAll` – dù chỉ do lỗi lập trình. Với `StudentLoginDao`, những lời gọi
đó **không viết ra được**.

`JdbcStudentLoginDao` viết riêng, không dùng chung `JdbcUserDao` với tên bảng truyền vào: khung nhìn không có
`class_id`, và kiểu `StudentLoginDao` không có hàm ghi. Hai lớp vẫn dùng chung hàm dựng đối tượng
`JdbcUserDao.mapUser` (cùng tên cột).

### 5.10. `JdbcUserDao` ([`oes/dao/jdbc/JdbcUserDao.java`](../../src/main/java/com/group2/oes/dao/jdbc/JdbcUserDao.java))

Mã nguồn: [oes/dao/jdbc/JdbcUserDao.java](../../src/main/java/com/group2/oes/dao/jdbc/JdbcUserDao.java)

```java
// oes/dao/jdbc/JdbcUserDao.java
static User mapUser(ResultSet rs) throws SQLException {
    int id = rs.getInt("id");
    // ...
    Role role = Role.valueOf(rs.getString("role"));
    User u = switch (role) {
        case ADMIN -> new Administrator(id, username, hash, name, email, active);
        case INSTRUCTOR -> new Instructor(id, username, hash, name, email, active);
        case STUDENT -> new Student(id, username, hash, name, email, active, rs.getString("student_code"));
    };
    u.setMustChangePassword(rs.getBoolean("must_change_password"));
    return u;
}
```

Một bảng `app_user` cho cả ba vai trò (mẫu *single-table inheritance*): cột `role` quyết định lớp con khi đọc lên.

Mã nguồn: [oes/dao/jdbc/JdbcUserDao.java](../../src/main/java/com/group2/oes/dao/jdbc/JdbcUserDao.java)

```java
// oes/dao/jdbc/JdbcUserDao.java
/** Điều kiện lọc ghép bằng tham số "? IS NULL OR ...", nên chỉ cần một câu SQL cố định cho mọi tổ hợp lọc. */
@Override
public List<User> search(String keyword, Role role) {
    String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.strip() + "%";
    String roleName = role == null ? null : role.name();
    return cm.execute(c -> {
        try (PreparedStatement ps = c.prepareStatement(SELECT_WITH_CLASS
                + " WHERE (? IS NULL OR u.role = ?)"
                + " AND (? IS NULL OR u.username LIKE ? OR u.full_name LIKE ? OR u.student_code LIKE ?)"
                + " ORDER BY u.role, u.username")) {
            ps.setString(1, roleName);
            ps.setString(2, roleName);
            for (int i = 3; i <= 6; i++) {
                ps.setString(i, like);
            }
            // ...
```

**Vì sao `? IS NULL OR ...`?** Bốn tổ hợp lọc (có/không từ khóa × có/không vai trò) dùng **một** câu SQL cố định, không
phải nối chuỗi SQL theo điều kiện – vẫn 100% tham số `?`, không có chỗ cho SQL injection. Dấu `%` được ghép vào
**giá trị tham số**, không ghép vào câu SQL.

`update` không ghi `username`, `role`, `student_code`: ba thứ này không đổi sau khi tạo.

### 5.11. `CsvLine`, `UserImportService`, `ImportResult`

Mã nguồn: [oes/util/CsvLine.java](../../src/main/java/com/group2/oes/util/CsvLine.java)

```java
// oes/util/CsvLine.java – split
if (quoted) {
    if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
        cur.append('"');   // "" bên trong trường có nháy là một dấu nháy
        i++;
    } else if (ch == '"') {
        quoted = false;
    } else {
        cur.append(ch);
    }
} else if (ch == '"') {
    quoted = true;
} else if (ch == ',') {
    fields.add(cur.toString().strip());
    cur.setLength(0);
} else {
    cur.append(ch);
}
```

**Vì sao không dùng `line.split(",")`?** Họ tên có thể chứa dấu phẩy ("Trần, Thị Bình"); Excel bọc trường đó trong
nháy kép. `split(",")` sẽ cắt sai thành 5 cột. `CsvLine` là một máy trạng thái nhỏ với cờ `quoted`. Không xử lý trường
kéo dài nhiều dòng – đủ cho danh sách sinh viên.

Mã nguồn: [oes/service/UserImportService.java](../../src/main/java/com/group2/oes/service/UserImportService.java)

```java
// oes/service/UserImportService.java – importStudents
for (int i = 0; i < lines.size(); i++) {
    String line = lines.get(i);
    if (i == 0 && !line.isEmpty() && line.charAt(0) == BOM) {
        line = line.substring(1);
    }
    line = line.strip();
    if (line.isEmpty() || line.startsWith("#") || (i == 0 && line.toLowerCase(Locale.ROOT).startsWith(HEADER))) {
        continue;
    }
    int lineNo = i + 1;
    try {
        Student s = parse(CsvLine.split(line));
        userDao.insert(s);
        imported++;
    } catch (ValidationException e) {
        errors.add("Dòng " + lineNo + ": " + e.getUserMessage());
    } catch (DuplicateKeyException e) {
        errors.add("Dòng " + lineNo + ": mã sinh viên hoặc tên đăng nhập đã tồn tại.");
    }
}
```

- **BOM:** Excel lưu "CSV UTF-8" thêm ký tự `\uFEFF` ở đầu tệp; không bỏ thì cột đầu bắt đầu bằng ký tự lạ thay vì "ma_sv" và không nhận ra
  dòng tiêu đề.
- **Lỗi theo dòng, không dừng cả tệp:** một dòng sai không làm hỏng 59 dòng đúng. Số dòng tính cả dòng tiêu đề, để
  quản trị viên mở Excel là thấy đúng dòng.
- **Không gói cả tệp trong một giao dịch** – cố ý: dòng đúng được nhập, dòng sai được liệt kê để sửa rồi nhập lại.
- `parse`: tên đăng nhập = mật khẩu mặc định = mã sinh viên viết thường; `setMustChangePassword(true)`. Mã băm tạo
  thẳng bằng `PasswordHasher.hash(username)`, **không** qua `User.hashNewPassword` – nên mật khẩu mặc định không bị quy
  tắc 6 ký tự chặn (mã sinh viên thật đủ dài; bắt buộc đổi ở lần đầu nên mật khẩu yếu này chỉ sống tới lần đăng nhập đầu).
- `IOException` (không đọc được tệp) được ném ra ngoài: đó là lỗi người dùng tự xử lý (chọn tệp khác).
  `UsersController.onImport` bọc nó thành `UncheckedIOException`, `ErrorDialogs` hiện "Lỗi tệp".

Mã nguồn: [oes/service/ImportResult.java](../../src/main/java/com/group2/oes/service/ImportResult.java)

```java
// oes/service/ImportResult.java
public record ImportResult(int imported, List<String> errors) {

    public ImportResult {
        errors = List.copyOf(errors);
    }
}
```

Dùng chung cho nhập câu hỏi (FR-12, phần 2) và nhập người dùng (FR-06). `List.copyOf` làm danh sách bất biến: nơi nhận
không sửa được kết quả.

### 5.12. `SubjectService` ([`oes/service/SubjectService.java`](../../src/main/java/com/group2/oes/service/SubjectService.java))

Mã nguồn: [oes/service/SubjectService.java](../../src/main/java/com/group2/oes/service/SubjectService.java)

```java
// oes/service/SubjectService.java
/** Quản trị viên thấy mọi môn; giảng viên thấy môn được phân công. */
public List<Subject> listSubjects(User actor) {
    if (actor instanceof Instructor ins) {
        return new ArrayList<>(ins.getSubjects());
    }
    Permissions.requireRole(actor, Role.ADMIN);
    return subjectDao.findAll();
}

/** Không xóa môn đã có câu hỏi, đề thi hoặc lớp học phần: dữ liệu thi cũ phải còn nguyên. */
public void deleteSubject(User actor, Subject subject) {
    Permissions.requireRole(actor, Role.ADMIN);
    if (subjectDao.isInUse(subject.getSubjectId())) {
        throw new ValidationException("SUBJECT_IN_USE", "Môn " + subject.getCode()
                + " đã có câu hỏi, đề thi hoặc lớp học phần nên không xóa được.");
    }
    subjectDao.delete(subject.getSubjectId());
    AuditLog.record(actor, "DELETE_SUBJECT", subject.getCode());
}
```

- **Chỉ đổi tên môn, không đổi mã** (`renameSubject`): mã môn đã in trên đề và bảng điểm.
- **Lớp không đổi môn** (`updateClass` giữ `cls.getSubject()`): thí sinh và ca thi đã gắn với lớp theo môn đó.
- **`listClasses(actor, null)`** (mọi lớp) chỉ cho quản trị viên; với một môn cụ thể thì `requireManage` – giảng viên
  phụ trách xem được lớp của môn mình (dùng ở màn hình mở ca thi, phần 3).
- `requireCode`: mã không trống, không khoảng trắng, tối đa 20 ký tự (khớp `subject.code VARCHAR(20)`).
- `JdbcSubjectDao.delete` chạy trong `cm.inTransaction`: xóa dòng `instructor_subject` trước, rồi xóa `subject` –
  hai lệnh cùng thành công hoặc cùng hủy.
- `JdbcSubjectDao.assignInstructor` dùng `INSERT IGNORE`: phân công trùng không báo lỗi (khóa chính
  `(instructor_id, subject_id)` đã chặn trùng).

### 5.13. `AuditLog`, `AuditLogService`, `LoggingSetup`

Mã nguồn: [oes/util/AuditLog.java](../../src/main/java/com/group2/oes/util/AuditLog.java)

```java
// oes/util/AuditLog.java
public static void record(User actor, String action, String target) {
    String who = actor == null ? "-" : actor.getUsername() + "/" + actor.getRole();
    LOG.info(() -> String.format("%s | %s | %s", who, action, target));
}
```

- Ghi bằng `java.util.logging` (có sẵn JDK) vào logger `oes.audit`. Nơi ghi do `logging.properties` quyết định:
  tệp `oes-audit.log` trong thư mục chạy chương trình, mã hóa UTF-8. `LoggingSetup.configure()` nạp tệp này từ classpath.
- Lambda `() -> String.format(...)` (Supplier): chuỗi chỉ được ghép khi mức log thực sự được ghi.
- `actor == null` ghi là `-`: máy chấm (`GradingWorker`) ghi `AUTO_GRADE`, `AUTO_SUBMIT` không có người thao tác.
- `AuditLogService.recentLines(actor, keyword, limit)`: chỉ quản trị viên; đọc từ cuối tệp ngược lên, lấy tối đa
  `limit` dòng (màn hình dùng 500) có chứa từ khóa, không phân biệt hoa thường; chưa có tệp thì trả danh sách rỗng.
- `AppContext.AUDIT_LOG_FILE = "oes-audit.log"` phải trùng `java.util.logging.FileHandler.pattern`.

### 5.14. `AppContext`, `ClientContext` – hai nơi lắp ráp (Composition Root)

Mã nguồn: [oes/app/ClientContext.java](../../src/main/java/com/group2/oes/app/ClientContext.java)

```java
// oes/app/ClientContext.java
private ClientContext(StudentLoginDao logins, StudentExamDao studentExams) {
    this.logins = logins;
    this.studentExams = studentExams;

    this.auth = new StudentAuthService(logins);
    this.exams = new StudentExamService(studentExams);
    this.proctoring = new ProctoringService(studentExams, exams, ViolationPolicy.standard());
}

public static ClientContext inMemory(InMemoryDatabase db) {
    return new ClientContext(db.studentLoginDao(), db.studentExamDao());
}

/** Kết nối bằng tài khoản MySQL oes_client. */
public static ClientContext jdbc(ConnectionManager cm) {
    return new ClientContext(new JdbcStudentLoginDao(cm), new JdbcStudentExamDao(cm));
}
```

- Constructor `private` + hai hàm tạo `inMemory(db)` / `jdbc(cm)`: chỉ hai cách lắp, không có cách thứ ba.
- **Đổi MySQL sang bộ nhớ chỉ là đổi hàm tạo ở đây**; service không đổi một dòng. Kiểm thử và `ConsoleDemo` dùng
  `inMemory`; ứng dụng thật dùng `jdbc`.
- `ClientContext` **không có** `QuestionDao`, `ExamDao`, `GradingDao`: ứng dụng thi không có đường nào đọc đáp án,
  kể cả nếu ai đó cấu hình nó bằng tài khoản CSDL nhiều quyền hơn.
- `AppContext.inMemory(db)` và `ClientContext.inMemory(db)` dùng chung một `InMemoryDatabase` – như hai máy cùng nối
  một CSDL. Các trường `public final` vì đây là "hộp đựng" đối tượng đã lắp, không có logic.

### 5.15. `OesApplication` – mẫu Template Method

Mã nguồn: [oes/ui/OesApplication.java](../../src/main/java/com/group2/oes/ui/OesApplication.java)

```java
// oes/ui/OesApplication.java
public abstract class OesApplication<C> extends Application implements AppShell {
    // ...
    protected abstract String configFileName();
    protected abstract String defaultDbUser();
    protected abstract C connectTo(ConnectionManager cm);
    protected void onConnected() {
    }

    @Override
    public final void start(Stage stage) {
        Locale.setDefault(Locale.forLanguageTag("vi-VN"));
        LoggingSetup.configure();
        navigator = new Navigator(stage);
        ErrorDialogs.setOwner(stage);
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> ErrorDialogs.show(error));
        // ...
        Optional<Path> found = AppConfig.locate(getParameters().getNamed(), configFileName());
        configFile = found.orElse(AppConfig.defaultLocation(configFileName()));
        if (found.isPresent()) {
            connect(() -> connectTo(ConnectionManager.fromFile(configFile)));
        } else {
            showServerSetup();
        }
        stage.show();
    }
```

- `start` là **phương thức khuôn mẫu**, đánh dấu `final`: lớp con không đổi được thứ tự các bước.
- Các bước khác nhau là phương thức trừu tượng (`configFileName`, `defaultDbUser`, `connectTo`) và một "móc" (hook)
  có thân rỗng `onConnected` – `StaffApp` ghi đè để khởi động máy chấm, `ExamClientApp` không cần.
- `connect(...)` chạy trên luồng nền qua `BackgroundTasks.run`; lỗi thì `ErrorDialogs.show(error)` rồi quay về màn hình
  cài đặt máy chủ – mất mạng thì báo, không treo.
- `setUncaughtExceptionHandler`: lưới an toàn cuối – ngoại lệ lọt khỏi một nút bấm cũng hiện thành hộp thoại.
- `connectTo` của `StaffApp` gọi `app.sessions.serverTime()`, của `ExamClientApp` gọi `client.exams.serverTime()`:
  một truy vấn thử để chắc chắn kết nối và quyền đều đúng ngay lúc khởi động.

### 5.16. `AppShell`, `StaffApp`, `ExamClientApp`

Mã nguồn: [oes/ui/AppShell.java](../../src/main/java/com/group2/oes/ui/AppShell.java)

```java
// oes/ui/AppShell.java
/** Sau khi đăng nhập thành công: tài khoản bị buộc đổi mật khẩu thì đổi trước, rồi mới vào trang chủ. */
default void onLoggedIn(User user) {
    UserSession.start(user);
    if (user.isMustChangePassword()) {
        navigator().show("change-password.fxml", (ChangePasswordController c) -> c.init(this, user, true));
    } else {
        showHome(user);
    }
}

default void logout() {
    UserSession.end();
    showLogin();
}
```

- `AppShell` là "những gì màn hình dùng chung cần biết về ứng dụng đang chạy": `title`, `navigator`, `login`,
  `changePassword`, `showHome`, `showServerSetup`.
- `StaffApp.login` gọi `context.auth.login` (`AuthService`); `ExamClientApp.login` gọi `context.auth.login`
  (`StudentAuthService`). Cùng một dòng chữ, khác kiểu `context` → khác service. `LoginController` chỉ biết `AppShell`,
  **không biết** đang chạy ứng dụng nào – một `login.fxml` dùng cho cả hai.
- **Bắt buộc đổi mật khẩu lần đầu** nằm trong `onLoggedIn` (một chỗ, cả hai ứng dụng): cờ `isMustChangePassword()` →
  màn hình đổi mật khẩu với `forced = true`; nút "Hủy" đổi thành "Đăng xuất".
- `StaffLauncher`/`ClientLauncher`: lớp `main` thường gọi `Application.launch(...)`. Lớp chứa `main` không được kế thừa
  `Application`, nếu không jar chạy độc lập báo "JavaFX runtime components are missing".

### 5.17. `Navigator`, `UserSession`

Mã nguồn: [oes/ui/Navigator.java](../../src/main/java/com/group2/oes/ui/Navigator.java)

```java
// oes/ui/Navigator.java
/** Thay toàn bộ nội dung cửa sổ bằng màn hình mới. */
public <C> C show(String fxml, Consumer<C> init) {
    Loaded<C> loaded = load(fxml);
    init.accept(loaded.controller());
    Scene scene = stage.getScene();
    if (scene == null) {
        scene = new Scene(loaded.root(), 1100, 720);
        scene.getStylesheets().add(stylesheet());
        stage.setScene(scene);
    } else {
        scene.setRoot(loaded.root());   // giữ nguyên kích thước cửa sổ
    }
    return loaded.controller();
}
```

- Một cửa sổ (`Stage`) duy nhất; đổi màn hình là thay gốc của `Scene`.
- **Truyền dữ liệu qua `init`**: nơi mở màn hình gọi `c.init(...)` với đúng service và dữ liệu cần. Không dùng biến
  static toàn cục, không dùng controller factory dò constructor bằng reflection – **đọc chỗ mở màn hình là biết màn hình
  dùng gì** (DESIGN-CHANGES Phase 3 mục 3).
- `embed(...)`: nạp màn hình con vào vùng giữa trang chủ. `openDialog(...)`: cửa sổ con `WINDOW_MODAL` (biểu mẫu người dùng).
- `vietnamesePlaceholders`: bảng rỗng hiện "Chưa có dữ liệu." thay cho chữ tiếng Anh mặc định.
- `UserSession`: biến static duy nhất của giao diện, `start`/`end` package-private (chỉ `AppShell` gọi), các hàm
  `synchronized`. **Thực tế các màn hình không đọc `UserSession.get()`** – người dùng được truyền qua `init(...)` và
  `StaffWorkspace.user()`. `UserSession` hiện chỉ đánh dấu "đang có người đăng nhập" (xem mục 12).

### 5.18. `BackgroundTasks`, `ErrorDialogs`

Lý do chung (luồng giao diện, NFR-08): xem Câu hỏi chung 9 và 10 trong [`docs/PHAN-CONG.md`](../../docs/PHAN-CONG.md). Ở đây chỉ chi tiết cài đặt.

Mã nguồn: [oes/ui/BackgroundTasks.java](../../src/main/java/com/group2/oes/ui/BackgroundTasks.java)

```java
// oes/ui/BackgroundTasks.java
public static <T> void run(Node busy, Supplier<T> work, Consumer<T> onSuccess) {
    Scene scene = busy.getScene();
    busy.setDisable(true);
    if (scene != null) {
        scene.setCursor(Cursor.WAIT);
    }
    Runnable restore = () -> {
        busy.setDisable(false);
        if (scene != null) {
            scene.setCursor(Cursor.DEFAULT);
        }
    };
    run(work, result -> {
        restore.run();
        onSuccess.accept(result);
    }, error -> {
        restore.run();
        ErrorDialogs.show(error);
    });
}
```

- `run(work, onSuccess, onError)` (dạng đầy đủ) dựng một `javafx.concurrent.Task`, chạy `work.get()` trong `call()`
  trên luồng nền; `setOnSucceeded`/`setOnFailed` được JavaFX gọi **trên luồng giao diện**, nên cập nhật giao diện an toàn.
- Dạng `run(busy, work, onSuccess)`: vô hiệu hóa khung nhập trong lúc chạy – người dùng không bấm "Lưu" hai lần; luôn
  bật lại kể cả khi lỗi.
- Bể 4 luồng **daemon**: đóng cửa sổ thì chương trình thoát, không bị luồng nền giữ lại. Mỗi máy một người dùng nên 4
  luồng là đủ.

`ErrorDialogs.show(Throwable)`:
- Không ở luồng giao diện thì tự chuyển sang bằng `Platform.runLater`.
- `ExamAppException` → hộp thoại "Không thực hiện được" + `getUserMessage()`; ghi log mức `INFO`.
- `UncheckedIOException` → "Lỗi tệp" (người dùng tự xử lý được: chọn tệp khác, kiểm tra quyền ghi).
- Lỗi khác → "Đã có lỗi không mong muốn. Hãy thử lại; nếu vẫn lỗi, báo cho giám thị hoặc quản trị viên." + stack trace
  mức `SEVERE` vào nhật ký. Người dùng không bao giờ thấy chữ `NullPointerException`.

### 5.19. `StaffWorkspace`, `StaffHomeController`

Mã nguồn: [oes/ui/controller/StaffHomeController.java](../../src/main/java/com/group2/oes/ui/controller/StaffHomeController.java)

```java
// oes/ui/controller/StaffHomeController.java – init
List<MenuItem> items = new ArrayList<>();
if (user.getRole() == Role.ADMIN) {
    items.add(new MenuItem("Người dùng", () -> open("users.fxml", (UsersController c) -> c.init(this))));
    items.add(new MenuItem("Môn học & lớp", () -> open("subjects.fxml", (SubjectsController c) -> c.init(this))));
    items.add(new MenuItem("Nhật ký", () -> open("audit-log.fxml", (AuditLogController c) -> c.init(this))));
} else {
    items.add(new MenuItem("Ngân hàng câu hỏi", () -> open("question-bank.fxml", (QuestionBankController c) -> c.init(this))));
    items.add(new MenuItem("Đề thi", () -> open("exam-list.fxml", (ExamListController c) -> c.init(this))));
    items.add(new MenuItem("Ca thi", () -> open("staff-sessions.fxml", (StaffSessionsController c) -> c.init(this))));
}
items.add(new MenuItem("Hồ sơ", () -> open("profile.fxml", (ProfileController c) -> c.init(this, shell))));
```

- Menu theo vai trò **chỉ để gọn**; quyền thật ở service.
- `StaffHomeController` cài đặt `StaffWorkspace` (`app()`, `user()`, `navigator()`, `open(fxml, init)`, `gradeNow()`).
  Các màn hình chức năng nhận `StaffWorkspace`, **không** nhận lớp trang chủ cụ thể (ISP + DIP): kiểm thử hay thay trang
  chủ không ảnh hưởng chúng.
- `StaffHomeController.init` nhận `StaffApp` cụ thể (không phải `AppShell`) vì cần `gradeNow()` và thuộc tính
  `gradingStatus` – những thứ chỉ ứng dụng quản lý có. Nhãn trạng thái máy chấm được **gắn (bind)** vào thuộc tính:
  `gradingLabel.textProperty().bind(gradingStatus)`; máy chấm cập nhật thuộc tính qua `Platform.runLater`.

### 5.20. Các màn hình quản trị (controller)

Mọi controller theo cùng một khuôn: đọc ô nhập → `BackgroundTasks.run(...)` gọi service → cập nhật bảng/hiện thông báo.

- `UsersController`: bảng người dùng (cột "Trạng thái": "Đã khóa" / "Chờ đổi mật khẩu" / "Hoạt động"), ô tìm + ô lọc
  vai trò (đổi lựa chọn là tìm lại), nhấp đúp để sửa, `onToggleActive`, `onResetPassword` (hiện mật khẩu tạm),
  `onImport` (chọn tệp bằng `FileChooser`), `onImportHelp`.
- `UserFormController`: thêm/sửa. Khi sửa: vai trò, tên đăng nhập, mã sinh viên bị khóa; ô mật khẩu ẩn hẳn
  (`setVisible(false)` + `setManaged(false)` để không chiếm chỗ). Ô mã SV và lớp chỉ hiện khi vai trò là thí sinh.
- `SubjectsController`: bảng môn bên trái; chọn môn → nạp lớp + giảng viên phụ trách trong **một** lượt nền (record cục
  bộ `Details` gói hai danh sách). Các thao tác nhỏ dùng `FormDialog.ask(...)`.
- `AuditLogController`: `MAX_LINES = 500`, lọc theo từ khóa.
- `ProfileController`: sửa họ tên, email; nút đổi mật khẩu dùng màn hình chung.
- `Tables.column(...)`, `Tables.labels(...)`: rút gọn khai báo cột và ô chọn hiện tên tiếng Việt.

### 5.21. `ConsoleDemo`

Chạy toàn bộ luồng nghiệp vụ trên console, không cần JavaFX (`./mvnw compile exec:java`). Không có tham số → dữ liệu
trong bộ nhớ (`AppContext.inMemory` + `ClientContext.inMemory` chung một `InMemoryDatabase`); có tham số → MySQL
thật. Hàm `attempt(label, action)` chạy một thao tác **dự kiến bị từ chối** và in `getUserMessage()` kèm mã lỗi – đúng
cách màn hình JavaFX bắt lỗi. Bước 1 của demo là phần 1: tạo tài khoản, đăng nhập sai mật khẩu, giảng viên thử đăng
nhập ứng dụng thi.

---

[Về mục lục](#mục-lục)

## 6. Luồng chạy đầu–cuối

### Luồng A – Giảng viên đăng nhập StaffApp, bị buộc đổi mật khẩu

| Bước | Ở đâu | Chi tiết |
|---|---|---|
| 1 | `login.fxml`, nút "Đăng nhập" (`onAction="#onLogin"`) | |
| 2 | `LoginController.onLogin` | Đọc hai ô; `BackgroundTasks.run(form, () -> shell.login(username, password), user -> {...})` – khung nhập bị khóa, con trỏ chờ |
| 3 | `StaffApp.login` (luồng nền) | `context.auth.login(username, password)` |
| 4 | `AuthService.login` | `userDao.findByUsername(username.strip())` |
| 5 | `JdbcUserDao.findByUsername` → `findOne` | SQL bên dưới |
| 6 | `AuthService.login` | `checkPassword` → `isActive` → vai trò → `subjectDao.loadSubjectsOf(ins)` → `AuditLog.record(u, "LOGIN", ...)` |
| 7 | `JdbcSubjectDao.loadSubjectsOf` | `SELECT s.id, s.code, s.name FROM subject s JOIN instructor_subject i ON i.subject_id = s.id WHERE i.instructor_id = ? ORDER BY s.code` |
| 8 | Về luồng giao diện: `shell.onLoggedIn(user)` (`AppShell`) | `UserSession.start(user)`; `isMustChangePassword()` đúng → `change-password.fxml`, `forced = true` |
| 9 | `ChangePasswordController.onSave` | `User.requireConfirmed` → `shell.changePassword` → `AuthService.changePassword` → `User.changePassword` → `JdbcUserDao.update` |
| 10 | `ErrorDialogs.info("Đổi mật khẩu", "Đã đổi mật khẩu.")` → `shell.showHome(user)` | `StaffHomeController.init`, menu theo vai trò |

SQL ở bước 5 (ghép từ `SELECT_WITH_CLASS`):

Mã nguồn: [oes/dao/jdbc/JdbcUserDao.java](../../src/main/java/com/group2/oes/dao/jdbc/JdbcUserDao.java)

```java
// oes/dao/jdbc/JdbcUserDao.java
private static final String SELECT_WITH_CLASS =
        "SELECT " + USER_COLUMNS + ", c.id AS c_id, c.code AS c_code, c.semester AS c_semester,"
                + " s.id AS cs_id, s.code AS cs_code, s.name AS cs_name"
                + " FROM app_user u LEFT JOIN course_class c ON c.id = u.class_id"
                + " LEFT JOIN subject s ON s.id = c.subject_id";
// ...
return findOne(SELECT_WITH_CLASS + " WHERE u.username = ?", username);
```

SQL ở bước 9:

Mã nguồn: [oes/dao/jdbc/JdbcUserDao.java](../../src/main/java/com/group2/oes/dao/jdbc/JdbcUserDao.java)

```java
// oes/dao/jdbc/JdbcUserDao.java – update
try (PreparedStatement ps = c.prepareStatement(
        "UPDATE app_user SET password_hash=?, full_name=?, email=?, active=?, must_change_password=?,"
                + " class_id=? WHERE id=?")) {
```

**Nhánh lỗi** (đều đi `BackgroundTasks` → `ErrorDialogs.show` → hộp thoại "Không thực hiện được"):
- Sai tên hoặc mật khẩu: "Sai tên đăng nhập hoặc mật khẩu. Hãy kiểm tra lại (chú ý phím Caps Lock)."
- Tài khoản khóa: "Tài khoản đã bị khóa. Hãy liên hệ quản trị viên."
- Tài khoản thí sinh: "Tài khoản thí sinh hãy dùng ứng dụng thi (ExamClientApp)."
- Mất kết nối CSDL: `ConnectionManager.execute` → `JdbcErrors.translate` → `DataAccessException` → "Không kết nối được
  tới máy chủ dữ liệu. Vui lòng thử lại hoặc báo giám thị."
- Ở màn hình đổi mật khẩu: `PASSWORD_MISMATCH`, `WRONG_PASSWORD`, `PASSWORD_TOO_SHORT`. Bấm "Đăng xuất" →
  `ChangePasswordController.onCancel` → `shell.logout()` → `UserSession.end()` → màn hình đăng nhập.

### Luồng B – Quản trị viên thử đăng nhập ứng dụng thi (đoạn demo của mình)

| Bước | Ở đâu | Chi tiết |
|---|---|---|
| 1 | `login.fxml` của ExamClientApp | cùng tệp FXML, cùng `LoginController` |
| 2 | `LoginController.onLogin` → `ExamClientApp.login` | `context.auth.login(...)` – lần này `context` là `ClientContext`, `auth` là `StudentAuthService` |
| 3 | `StudentAuthService.login` → `JdbcStudentLoginDao.findByUsername` | `SELECT id, username, password_hash, full_name, email, role, active, must_change_password, student_code FROM v_login WHERE username = ?` (tài khoản `oes_client`) |
| 4 | Khung nhìn `v_login` | trả dòng `admin` với `password_hash = ''` |
| 5 | `StudentAuthService.login` | `u.getRole() != Role.STUDENT` → `AccessDeniedException("WRONG_APP", ...)` |
| 6 | `ErrorDialogs.show` | "Tài khoản giảng viên và quản trị viên hãy dùng ứng dụng quản lý (StaffApp)." |

Điểm cần nói: **từ chối nằm ở service** (`StudentAuthService`), không ở giao diện; và **dù sửa mã ứng dụng thi** để bỏ
dòng kiểm tra vai trò, mật khẩu admin vẫn không kiểm tra được vì `v_login` không trả mã băm.

### Luồng C – Nhập danh sách thí sinh từ CSV

| Bước | Ở đâu | Chi tiết |
|---|---|---|
| 1 | `users.fxml`, nút "Nhập thí sinh từ CSV…" | |
| 2 | `UsersController.onImport` | `FileChooser` (lọc `*.csv`, `*.txt`); hủy chọn thì dừng |
| 3 | `BackgroundTasks.run(page, ...)` | gọi `ws.app().userImport.importStudents(ws.user(), file.toPath())`; `IOException` → `UncheckedIOException` |
| 4 | `UserImportService.importStudents` | `Permissions.requireRole(actor, Role.ADMIN)`; `Files.readAllLines(csv, UTF_8)`; bỏ BOM, tiêu đề, dòng trống, dòng `#` |
| 5 | mỗi dòng: `CsvLine.split` → `parse` | kiểm 4 cột, mã SV, `AuthService.validateProfile`; mã lớp → `subjectDao.findClassByCode` |
| 6 | `JdbcSubjectDao.findClassByCode` | `SELECT c.id, c.code, c.semester, s.id AS s_id, s.code AS s_code, s.name AS s_name FROM course_class c JOIN subject s ON s.id = c.subject_id WHERE c.code = ?` |
| 7 | `JdbcUserDao.insert` | SQL bên dưới; trùng `username`/`student_code` → MySQL lỗi 1062 → `JdbcErrors.translate` → `DuplicateKeyException` → service ghi "Dòng N: mã sinh viên hoặc tên đăng nhập đã tồn tại." |
| 8 | `AuditLog.record(actor, "IMPORT_STUDENTS", ...)` → `new ImportResult(imported, errors)` | |
| 9 | Về luồng giao diện: `QuestionBankController.showImportResult(r)` rồi `onSearch()` | Hộp thoại "Kết quả nhập tệp": "Nhập được N dòng, M dòng lỗi." + từng dòng lỗi |

Mã nguồn: [oes/dao/jdbc/JdbcUserDao.java](../../src/main/java/com/group2/oes/dao/jdbc/JdbcUserDao.java)

```java
// oes/dao/jdbc/JdbcUserDao.java – insert
String sql = "INSERT INTO app_user(username, password_hash, full_name, email, role, active,"
        + " must_change_password, student_code, class_id) VALUES (?,?,?,?,?,?,?,?,?)";
try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    // ...
    ps.executeUpdate();
    try (ResultSet keys = ps.getGeneratedKeys()) {
        keys.next();
        user.assignId(keys.getInt(1));
    }
    return user;
}
```

`RETURN_GENERATED_KEYS` lấy mã `AUTO_INCREMENT` vừa sinh, rồi `assignId` gán (một lần) vào đối tượng.

**Nhánh lỗi:** giảng viên gọi được hàm này (không thể qua giao diện, nhưng giả sử) → "Chức năng này chỉ dành cho quản
trị viên."; tệp không đọc được → hộp thoại "Lỗi tệp".

### Luồng D – Thí sinh đổi mật khẩu lần đầu trên ứng dụng thi

| Bước | Ở đâu | Chi tiết |
|---|---|---|
| 1 | `StudentAuthService.login` | thí sinh nhập từ CSV: `must_change_password = TRUE` đọc từ `v_login` |
| 2 | `AppShell.onLoggedIn` | → `change-password.fxml` (`forced = true`) |
| 3 | `ChangePasswordController.onSave` → `ExamClientApp.changePassword` | → `StudentAuthService.changePassword` |
| 4 | `StudentAuthService.changePassword` | `requireStudent`, kiểm mật khẩu cũ, `User.hashNewPassword` |
| 5 | `JdbcStudentLoginDao.changePassword` | `{CALL sp_student_change_password(?, ?, ?, ?)}`, tham số 4 là `OUT` |
| 6 | Thủ tục `sp_student_change_password` (quyền người tạo) | `UPDATE app_user ... WHERE id = p_user_id AND role = 'STUDENT' AND password_hash = p_old_hash ...`; `SET p_changed = ROW_COUNT()` |
| 7 | `cs.getInt(4) == 1` ? | đúng → `actor.applyNewPasswordHash(newHash)` → `AuditLog.record(... "CHANGE_PASSWORD" ...)`; sai → `PASSWORD_CHANGED_ELSEWHERE` |
| 8 | `ExamClientApp.showHome` | `session-list.fxml` (phần 3) |

Mã nguồn: [oes/dao/jdbc/JdbcStudentLoginDao.java](../../src/main/java/com/group2/oes/dao/jdbc/JdbcStudentLoginDao.java)

```java
// oes/dao/jdbc/JdbcStudentLoginDao.java
/** oes_client không có quyền UPDATE bảng app_user; chỉ được gọi thủ tục trong security.sql. */
@Override
public boolean changePassword(int userId, String oldHash, String newHash) {
    return cm.execute(c -> {
        try (CallableStatement cs = c.prepareCall("{CALL sp_student_change_password(?, ?, ?, ?)}")) {
            cs.setInt(1, userId);
            cs.setString(2, oldHash);
            cs.setString(3, newHash);
            cs.registerOutParameter(4, Types.INTEGER);
            cs.execute();
            return cs.getInt(4) == 1;
        }
    });
}
```

---

[Về mục lục](#mục-lục)

## 7. CSDL của phần này

### Bảng ([`db/schema.sql`](../../db/schema.sql))

Mã nguồn: [db/schema.sql](../../db/schema.sql)

```sql
-- db/schema.sql
CREATE TABLE subject (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    code  VARCHAR(20)  NOT NULL UNIQUE,
    name  VARCHAR(200) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE course_class (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(30) NOT NULL UNIQUE,
    subject_id  INT NOT NULL,
    semester    VARCHAR(20),
    FOREIGN KEY (subject_id) REFERENCES subject(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ba vai trò dùng chung một bảng; cột role quyết định lớp con khi đọc lên.
-- Không đặt tên bảng là "user" để khỏi nhầm với bảng hệ thống mysql.user.
CREATE TABLE app_user (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(100),
    email          VARCHAR(100),
    role           ENUM('ADMIN','INSTRUCTOR','STUDENT') NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,   -- tài khoản nhập từ CSV phải đổi mật khẩu lần đầu
    student_code   VARCHAR(20) UNIQUE,
    class_id       INT,
    FOREIGN KEY (class_id) REFERENCES course_class(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE instructor_subject (
    instructor_id  INT NOT NULL,
    subject_id     INT NOT NULL,
    PRIMARY KEY (instructor_id, subject_id),
    FOREIGN KEY (instructor_id) REFERENCES app_user(id),
    FOREIGN KEY (subject_id)    REFERENCES subject(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Giải thích:
- **`app_user` một bảng cho ba vai trò** (single-table inheritance): cột chỉ có ở `Student` (`student_code`,
  `class_id`) để `NULL` với giảng viên/quản trị viên. Đơn giản hơn ba bảng + JOIN.
- **`student_code ... UNIQUE` nhưng cho phép `NULL`:** MySQL cho nhiều dòng `NULL` trong cột `UNIQUE`, nên mọi giảng
  viên (mã SV `NULL`) không đụng nhau; hai thí sinh cùng mã thì bị chặn. `MemoryUserDao.insert` mô phỏng đúng hai ràng
  buộc duy nhất này.
- **`password_hash VARCHAR(100)`:** salt 16 byte → 24 ký tự Base64, SHA-256 32 byte → 44 ký tự, cộng `$` = 69 ký tự.
- **`instructor_subject` khóa chính kép** `(instructor_id, subject_id)`: quan hệ nhiều–nhiều; một cặp chỉ một lần.
- **Khóa ngoại tới `subject`** từ `course_class`, `question`, `exam`, `instructor_subject`: xóa môn đang dùng sẽ bị
  CSDL chặn kể cả khi `SubjectService.isInUse` sót – lưới an toàn thứ hai.
- **Khóa ngoại tới `app_user`** từ `session_student.student_id` và `submission.student_id`: lý do "khóa tài khoản chứ
  không xóa".
- `utf8mb4`: lưu đủ tiếng Việt có dấu.

### Khung nhìn `v_login` ([`db/security.sql`](../../db/security.sql))

Mã nguồn: [db/security.sql](../../db/security.sql)

```sql
-- db/security.sql
-- Đăng nhập phía thí sinh. Chỉ trả mã băm mật khẩu của tài khoản thí sinh: tệp cấu hình chứa
-- mật khẩu oes_client nằm trên mọi máy trạm, không được để lộ mã băm của giảng viên, quản trị viên.
-- Vẫn trả dòng của các vai trò khác (mã băm rỗng) để ứng dụng thi báo "hãy dùng ứng dụng quản lý".
CREATE OR REPLACE VIEW v_login AS
SELECT id, username,
       CASE WHEN role = 'STUDENT' THEN password_hash ELSE '' END AS password_hash,
       full_name, email, role, active, must_change_password, student_code
FROM app_user;
```

Khung nhìn chạy với quyền người tạo (`SQL SECURITY DEFINER` mặc định), nên `oes_client` đọc được dữ liệu đã lọc mà
không cần quyền trên bảng `app_user`. Không có cột `class_id` – máy thí sinh không cần.

### Thủ tục `sp_student_change_password`

Mã nguồn: [db/security.sql](../../db/security.sql)

```sql
-- db/security.sql
CREATE PROCEDURE sp_student_change_password(IN p_user_id INT, IN p_old_hash VARCHAR(100),
                                            IN p_new_hash VARCHAR(100), OUT p_changed INT)
SQL SECURITY DEFINER
BEGIN
    UPDATE app_user
    SET password_hash = p_new_hash, must_change_password = FALSE
    WHERE id = p_user_id AND role = 'STUDENT' AND password_hash = p_old_hash
      AND p_new_hash IS NOT NULL AND LOCATE('$', p_new_hash) > 0;
    SET p_changed = ROW_COUNT();
END //
```

Từng điều kiện `WHERE` có lý do:
- `role = 'STUDENT'`: không bao giờ đổi được mật khẩu giảng viên/quản trị viên qua đường này.
- `password_hash = p_old_hash`: so sánh rồi mới ghi; máy khác (hoặc quản trị viên) vừa đổi thì không ghi.
- `LOCATE('$', p_new_hash) > 0`: mã băm mới phải đúng dạng `salt$hash` của `PasswordHasher` – chặn việc ghi một chuỗi
  rỗng hay chuỗi tùy ý khiến tài khoản không đăng nhập được.
- `SQL SECURITY DEFINER`: thủ tục chạy với quyền người tạo (root), nên sửa được `app_user` dù người gọi (`oes_client`)
  không có quyền `UPDATE` bảng đó.
- `ROW_COUNT()`: số dòng vừa sửa (0 hoặc 1) → `p_changed`.

### Quyền

Mã nguồn: [db/security.sql](../../db/security.sql)

```sql
-- db/security.sql
CREATE USER IF NOT EXISTS 'oes_staff'@'%'  IDENTIFIED BY 'doi_mat_khau_staff';
CREATE USER IF NOT EXISTS 'oes_client'@'%' IDENTIFIED BY 'doi_mat_khau_client';

-- "ON *" = mọi bảng của CSDL đang chọn.
GRANT ALL PRIVILEGES ON * TO 'oes_staff'@'%';

-- Máy thí sinh: chỉ đọc khung nhìn, cấu hình ca thi và danh sách ghi danh.
GRANT SELECT ON v_login               TO 'oes_client'@'%';
-- ...
-- Đổi mật khẩu: không cấp quyền UPDATE trên app_user (sẽ sửa được mật khẩu của cả quản trị viên),
-- chỉ cho gọi thủ tục sp_student_change_password ở mục 2.
GRANT EXECUTE ON PROCEDURE sp_student_change_password TO 'oes_client'@'%';
```

`oes_client` không có quyền gì trên `app_user`: `JdbcSecurityTest.thíSinhĐổiMậtKhẩuQuaThủTụcLưuTrữ` chạy thử
`UPDATE app_user SET password_hash = 'x' WHERE id = ?` bằng `oes_client` và chứng minh bị từ chối. Toàn bộ phân quyền
chi tiết (bảng bài làm, trigger) thuộc phần 4.

---

[Về mục lục](#mục-lục)

## 8. Quy tắc nghiệp vụ

| Quy tắc | Dòng mã thực thi |
|---|---|
| FR-05: mọi chức năng kiểm tra quyền ở service | `Permissions.requireRole/requireManage/requireStaff/requireStudent` ở dòng đầu mỗi hàm service |
| BR-10: chỉ giảng viên phụ trách môn hoặc quản trị viên xem kết quả của môn | `Instructor.canManage` (`subjects.contains`) + `Permissions.requireManage`, gọi trong `StatisticsService` |
| Ứng dụng quản lý từ chối thí sinh, ứng dụng thi từ chối giảng viên/quản trị | `AuthService.login` (`WRONG_APP`), `StudentAuthService.login` (`WRONG_APP`) |
| Sai tên hay sai mật khẩu báo cùng một câu | `AuthService.login` (`.filter(...).orElseThrow(...)`), `StudentAuthService.invalidLogin` |
| Mật khẩu ≥ 6 ký tự (FR-04) | `User.hashNewPassword`, hằng `User.MIN_PASSWORD_LENGTH` |
| Đổi mật khẩu phải đúng mật khẩu cũ | `AuthService.changePassword`, `StudentAuthService.changePassword` (`WRONG_PASSWORD`) |
| Tài khoản do quản trị viên tạo/đặt lại, hoặc nhập CSV, phải đổi mật khẩu lần đầu | `AuthService.createAccount` (`setMustChangePassword(true)`), `User.resetPassword`, `UserImportService.parse`; ép đổi ở `AppShell.onLoggedIn` |
| Không tự khóa chính mình | `AuthService.setActive` (`SELF_LOCK`) |
| Tài khoản bị khóa không đăng nhập được | `AuthService.login`, `StudentAuthService.login` (`ACCOUNT_LOCKED`) |
| Vai trò, tên đăng nhập, mã SV không đổi sau khi tạo | `JdbcUserDao.update` không có ba cột này; `UserFormController.init` khóa ô |
| CSV: tên đăng nhập = mật khẩu mặc định = mã SV viết thường | `UserImportService.parse` |
| Không xóa môn đã có câu hỏi/đề/lớp | `SubjectService.deleteSubject` + `JdbcSubjectDao.isInUse`; khóa ngoại trong `schema.sql` |
| Mã môn không đổi; lớp không đổi môn | `SubjectService.renameSubject`, `SubjectService.updateClass` |
| Giảng viên chỉ thấy môn mình phụ trách | `SubjectService.listSubjects` |
| Chỉ quản trị viên xem nhật ký (NFR-05) | `AuditLogService.recentLines` (`requireRole(actor, Role.ADMIN)`) |
| Thí sinh chỉ đổi được mật khẩu của tài khoản thí sinh | `sp_student_change_password` (`role = 'STUDENT'`) |

Quyết định đã chốt số 7 trong `CLAUDE.md` ("không gửi email") giải thích vì sao đặt lại mật khẩu **hiện mật khẩu tạm cho
quản trị viên đọc** thay vì gửi thư.

---

[Về mục lục](#mục-lục)

## 9. SOLID và mẫu thiết kế

| Nguyên lý / mẫu | Lớp cụ thể | Vì sao | Nếu không làm vậy |
|---|---|---|---|
| **S** – Đơn trách nhiệm | `PasswordHasher` (chỉ băm), `CsvLine` (chỉ tách dòng), `ErrorDialogs` (chỉ hiện lỗi), `AuthService` vs `StudentAuthService` vs `UserImportService` | Mỗi lớp một lý do để sửa | Đổi thuật toán băm phải lục khắp service; đổi cách hiện lỗi phải sửa mọi controller |
| **O** – Mở/đóng | `User` + 3 lớp con; `Permissions.requireManage` | Thêm vai trò = thêm lớp con, `Permissions` không sửa | Chuỗi `if (role == ...)` trong `Permissions` phải sửa mỗi lần thêm vai trò |
| **L** – Thay thế Liskov | Mọi lớp con `User` dùng được ở chỗ cần `User`; `canManage` không ném lỗi, chỉ trả `true/false` | Nơi gọi không cần biết lớp thật | Một lớp con ném `UnsupportedOperationException` trong `canManage` sẽ làm hỏng `requireManage` |
| **I** – Tách interface | `StudentLoginDao` (2 hàm) tách khỏi `UserDao` (7 hàm); `StaffWorkspace`; `AppShell` | Mỗi bên chỉ thấy hàm nó cần | Ứng dụng thi gọi được `UserDao.update` – về kiểu dữ liệu thì sửa được mật khẩu admin |
| **D** – Đảo phụ thuộc | `AuthService(UserDao, SubjectDao)`; `AppContext`, `ClientContext` | Service phụ thuộc interface, nơi lắp ráp chọn cài đặt | Không kiểm thử được service nếu không có MySQL |
| **Template Method** | `OesApplication.start` (`final`) + `configFileName`, `defaultDbUser`, `connectTo`, `onConnected` | Hai ứng dụng cùng quy trình khởi động | Viết hai lần, sửa một chỗ quên chỗ kia |
| **Composition Root** | `AppContext`, `ClientContext` | Mọi `new` ở một chỗ | `new JdbcUserDao(...)` rải trong controller, đổi sang bộ nhớ phải sửa nhiều nơi |
| **DAO** | `UserDao`/`JdbcUserDao`/`MemoryUserDao` | SQL chỉ ở một tầng | SQL lẫn trong service |
| **Simple Factory** (switch theo enum) | `AuthService.createAccount`, `JdbcUserDao.mapUser` | Chọn lớp con theo vai trò ở một chỗ, trình biên dịch kiểm đủ nhánh | `new Student(...)` trong controller (logic nghiệp vụ lọt lên view) |
| **Parameter Object** | record `NewAccount` (7 trường) | Truyền một đối tượng thay vì 7 tham số dễ nhầm thứ tự | `createAccount(actor, role, u, p, n, e, code, cls)` – đổi chỗ hai `String` không ai phát hiện |
| **Observer** (thuộc tính JavaFX) | `StaffApp.gradingStatus` + `gradingLabel.textProperty().bind(...)` | Máy chấm cập nhật, nhãn tự đổi theo | Máy chấm phải biết tới nhãn trên giao diện |
| **Đa hình qua interface** | `AppShell` (cài đặt bởi `StaffApp`, `ExamClientApp`) | Một `LoginController` cho hai ứng dụng | Hai bản màn hình đăng nhập, hoặc controller phải `if (đây là app nào)` |

---

[Về mục lục](#mục-lục)

## 10. Khác với thiết kế M3/M4

Trích từ [`docs/DESIGN-CHANGES.md`](../../docs/DESIGN-CHANGES.md), chỉ các mục thuộc phần 1.

| Mục | Thay đổi | Lý do | Ảnh hưởng |
|---|---|---|---|
| Phase 1, mục 4 | Thêm `StudentLoginDao` (chỉ `findByUsername` đọc `v_login`, sau thêm `changePassword`); `JdbcStudentLoginDao` viết riêng | ISP: ứng dụng thi không gọi nhầm `insert`/`update` | Dùng lại `JdbcUserDao.mapUser` |
| Phase 1, mục 5 | Thêm `StudentAuthService`; tài khoản giảng viên trên ứng dụng thi báo `WRONG_APP` | Ứng dụng thi chỉ nhận thí sinh | – |
| Phase 1, mục 6 | `AppContext` chỉ còn phía giảng viên; thêm `ClientContext` | DIP; mỗi ứng dụng chỉ lắp DAO nó được dùng | `inMemory(db)` dùng chung một `InMemoryDatabase` |
| Phase 1, mục 7 | `v_login` chỉ trả mã băm của thí sinh | Mật khẩu `oes_client` nằm trên mọi máy trạm | Phải báo `WRONG_APP` trước khi kiểm mật khẩu |
| Phase 2, mục 1 | Cột `must_change_password`; thủ tục `sp_student_change_password` (`SQL SECURITY DEFINER`); `oes_client` chỉ `EXECUTE` | `oes_client` không được có `UPDATE app_user` | `User` tách `hashNewPassword` + `applyNewPasswordHash`; giới hạn: ai có mật khẩu `oes_client` đọc được mã băm thí sinh |
| Phase 2, mục 2 | `ImportResult` thành lớp dùng chung (trước là record lồng trong `QuestionBankService`) | Nhập người dùng trả cùng kiểu kết quả | – |
| Phase 2, mục 4 | `AuthService.login` báo `WRONG_APP` cho thí sinh – chỉ sau khi đúng mật khẩu | Sai mật khẩu không lộ tài khoản có tồn tại | – |
| Phase 2, mục 5 | `UserDao.search`; `update` ghi lớp + cờ buộc đổi; `SubjectDao` thêm sửa/xóa môn, sửa lớp, tìm lớp theo mã, bỏ phân công | Đủ CRUD cho FR-01, FR-07 | `CourseClass` vẫn bất biến |
| Phase 3, mục 1 | `OesApplication<C>` – Template Method | Hai ứng dụng cùng quy trình khởi động | `StaffApp`, `ExamClientApp` chỉ điền phần khác |
| Phase 3, mục 2 | Interface `AppShell` | Một `login.fxml` cho hai ứng dụng | Từ chối sai ứng dụng vẫn ở service |
| Phase 3, mục 3 | `Navigator.show(fxml, controller -> controller.init(...))` | Không biến static ngoài `UserSession`, không reflection | Đọc chỗ mở màn hình là biết màn hình dùng gì |
| Phase 3, mục 5 | `BackgroundTasks`, `ErrorDialogs`, `Role.displayName()`, `User.requireConfirmed` | Luồng nền; một chỗ hiện lỗi; tên vai trò tiếng Việt dùng chung | Controller không tự so khớp mật khẩu |
| Phase 4–5, mục 1 | Interface `StaffWorkspace`, do `StaffHomeController` cài đặt | Màn hình con chỉ phụ thuộc thứ nó cần (ISP, DIP) | – |
| Phase 4–5, mục 3 | `AuthService.createAccount(actor, NewAccount)` (switch trên enum); `resetPassword(actor, target)` sinh mật khẩu tạm 8 ký tự | Chọn lớp con là nghiệp vụ, không phải việc của controller | Tài khoản tạo/đặt lại đều phải đổi mật khẩu lần đầu |
| Phase 4–5, mục 4 | `AuditLogService` (500 dòng mới nhất, lọc, chỉ quản trị viên); `OesApplication` đặt bộ xử lý ngoại lệ cho luồng giao diện; bảng rỗng hiện "Chưa có dữ liệu." | NFR-05, NFR-08 | – |
| Phase 7 | `DuplicateKeyException.getUserMessage()` riêng | Trước đó kế thừa câu "không kết nối được" – báo nhầm mất mạng | – |

---

[Về mục lục](#mục-lục)

## 11. Kiểm thử

**Cách chạy:**
```bash
./mvnw test                                   # tất cả; JDBC tự bỏ qua nếu chưa đặt biến môi trường
./mvnw test -Dtest=StaffServicesTest          # một lớp
./mvnw test -Dtest=AdminAndResultsTest,ErrorCodeTest,CsvLineTest
OES_TEST_DB=... OES_TEST_DB_CLIENT=... ./mvnw test -Dtest=JdbcSecurityTest,JdbcIntegrationTest
./mvnw compile exec:java                      # ConsoleDemo
```

Hầu hết kiểm thử phần 1 chạy với `AppContext.inMemory` + `ClientContext.inMemory` trên `InMemoryDatabase` với đồng hồ
giả `ManualClock` – không cần MySQL. Đây chính là lợi ích của DIP.

| Lớp test | Phương thức | Chứng minh gì |
|---|---|---|
| `StaffServicesTest` | `tìmNgườiDùngTheoTừKhóaVàVaiTrò` | Lọc vai trò, tìm theo mã SV không phân biệt hoa thường; giảng viên gọi `searchUsers` bị `AccessDeniedException` |
| | `quảnTrịViênSửaThôngTinVàLớpCủaThíSinh` | `updateUser` ghi lớp; `EMAIL_INVALID`, `NAME_EMPTY` |
| | `khóaTàiKhoảnThìKhôngĐăngNhậpĐượcVàKhôngTựKhóaMình` | `ACCOUNT_LOCKED`, `SELF_LOCK` |
| | `ứngDụngQuảnLýTừChốiTàiKhoảnThíSinh` | `WRONG_APP` khi đúng mật khẩu; `INVALID_LOGIN` khi sai – "sai mật khẩu thì không lộ vai trò" |
| | `đặtLạiMậtKhẩuThìPhảiĐổiỞLầnĐăngNhậpSau` | Cờ `isMustChangePassword` bật sau đặt lại, tắt sau khi đổi |
| | `đổiMậtKhẩuPhảiĐúngMậtKhẩuCũVàĐủSáuKýTự` | `WRONG_PASSWORD`, `PASSWORD_TOO_SHORT`; mật khẩu cũ hết hiệu lực |
| | `ngườiDùngTựSửaHồSơ` | `updateProfile` cắt khoảng trắng, ghi xuống DAO |
| | `thíSinhĐổiMậtKhẩuQuaỨngDụngThi` | `StudentAuthService.changePassword` qua `MemoryStudentLoginDao` |
| | `nhậpThíSinhTừCsvGhiLỗiTheoDòng` | BOM, tiêu đề, tên có dấu phẩy trong nháy, trùng mã, sai lớp, sai email, thiếu cột, dòng trống; 2 nhập được, 4 lỗi; "Dòng 4:" tính cả tiêu đề; giảng viên bị từ chối |
| | `quảnLýMônHọc` | `SUBJECT_CODE_TAKEN`, `CODE_INVALID`, đổi tên, xóa, `SUBJECT_IN_USE` |
| | `quảnLýLớpHọcPhần` | `CLASS_CODE_TAKEN`, sửa lớp tạo đối tượng mới nhưng giữ nguyên `classId`, giảng viên phụ trách xem được lớp |
| | `phânCôngGiảngViênQuyếtĐịnhQuyềnTrênMôn` | Phân công → có quyền; bỏ phân công → mất quyền; "đăng nhập lại cũng không còn quyền" |
| `AdminAndResultsTest` | `tạoTàiKhoảnTheoVaiTròDựngĐúngLớpCon` | `createAccount` trả `Student`/`Instructor` đúng lớp; tên thường, mã hoa; `USERNAME_INVALID`, `PASSWORD_TOO_SHORT`, `STUDENT_CODE_INVALID` |
| | `đặtLạiMậtKhẩuSinhMậtKhẩuTạmNgẫuNhiên` | 8 ký tự, hai lần khác nhau, không có ký tự dễ nhầm |
| | `nhậtKýLấyDòngMớiNhấtTrướcVàLọcTheoTừKhóa` | Dòng mới nhất đầu, bỏ dòng trống, lọc không phân biệt hoa thường, tệp chưa có → rỗng, giảng viên bị từ chối |
| `ErrorCodeTest` | `lỗiPhânQuyềnVàTàiKhoản` | `SUBJECT_FORBIDDEN`, `ROLE_REQUIRED`, `NOT_LOGGED_IN`, `USERNAME_TAKEN`, `ROLE_EMPTY`, `PASSWORD_MISMATCH` |
| | `lỗiNhậpTệpGhiTheoDòng` | "cần 4 cột" khi nhập CSV |
| `CsvLineTest` | `táchTrườngThườngVàTrườngRỗng`, `trườngTrongNgoặcKépGiữĐượcDấuPhẩyVàDấuNháy` | Trường rỗng, cắt khoảng trắng, `""` trong nháy là một dấu nháy |
| `JdbcSecurityTest` (MySQL thật) | `khungNhìnĐăngNhậpKhôngLộMãBămCủaGiảngViên` | Qua `oes_client`, mã băm giảng viên là `""`; `WRONG_APP`; thí sinh đăng nhập được |
| | `thíSinhĐổiMậtKhẩuQuaThủTụcLưuTrữ` | Thủ tục bỏ cờ buộc đổi; `PASSWORD_CHANGED_ELSEWHERE` khi admin vừa đặt lại; không đổi được mật khẩu giảng viên; `UPDATE app_user` bằng `oes_client` bị từ chối |
| `JdbcIntegrationTest` (MySQL thật) | `quảnLýNgườiDùngMônHọcVàLớpQuaCSDL` | SQL thật của `JdbcUserDao`, `JdbcSubjectDao`: lớp đọc lên kèm mã, tìm theo vai trò, xóa môn đã phân công |
| | `nhậpThíSinhTừCsvQuaCSDL` | Trùng mã → lỗi dòng qua `DuplicateKeyException` thật; đăng nhập ứng dụng thi bị buộc đổi mật khẩu |

---

[Về mục lục](#mục-lục)

## 12. Giới hạn đã biết

| Giới hạn | Câu trả lời trung thực |
|---|---|
| SHA-256 chỉ băm **một lần**, rất nhanh – kẻ có mã băm thử được hàng tỷ mật khẩu/giây | Đúng. NFR-03 ghi "SHA-256 + salt bằng thư viện JDK" nên làm đúng như vậy. Cải tiến: dùng PBKDF2 có sẵn trong JDK (lớp SecretKeyFactory, thuật toán PBKDF2WithHmacSHA256) với nhiều vòng lặp – vẫn không thêm thư viện; chỉ sửa `PasswordHasher`, định dạng lưu thêm số vòng. |
| Ai có mật khẩu `oes_client` (nằm trên mọi máy trạm) đọc được mã băm của **mọi thí sinh** qua `v_login` | Đúng, DESIGN-CHANGES Phase 2 mục 1 đã ghi. Kiểm tra "mã băm cũ" trong thủ tục chỉ chặn dùng nhầm, không chặn người cố ý. Chặn hẳn cần máy chủ ứng dụng ở giữa – ngoài công nghệ cho phép (Câu hỏi chung 14). |
| Ứng dụng thi tiết lộ một tên đăng nhập có phải giảng viên/quản trị viên không (báo `WRONG_APP` trước khi kiểm mật khẩu) | Đánh đổi có chủ ý: máy thí sinh không có mã băm của họ nên không kiểm được mật khẩu. Không lộ mật khẩu hay mã băm. |
| Quyền của giảng viên nạp **lúc đăng nhập** | Quản trị viên phân công/bỏ phân công trên máy khác thì giảng viên đang đăng nhập phải đăng xuất rồi vào lại mới có hiệu lực. |
| Mật khẩu mặc định khi nhập CSV = mã sinh viên (dễ đoán) | Bị buộc đổi ở lần đăng nhập đầu. Người biết mã SV của bạn có thể đăng nhập trước bạn – nên nhà trường cần cho sinh viên đổi mật khẩu sớm. |
| Nhật ký **mỗi máy một tệp** `oes-audit.log` | Màn hình Nhật ký chỉ đọc tệp của máy đang mở StaffApp. Sự kiện `LOGIN` của thí sinh nằm ở máy thí sinh. Tệp không xoay vòng, không chống sửa. Muốn tập trung thì ghi nhật ký vào một bảng CSDL. |
| `AuditLogService` đọc **cả tệp** vào bộ nhớ | Đủ cho quy mô phòng máy; tệp rất lớn thì nên đọc từ cuối tệp. |
| `UserSession.get()` không có nơi nào gọi | Kế hoạch yêu cầu lớp này; thực tế mọi màn hình nhận người dùng qua `init(...)`/`StaffWorkspace.user()` (rõ ràng hơn biến toàn cục). `UserSession` chỉ được `start`/`end` khi đăng nhập/đăng xuất. |
| `AuthService.updateProfile` không ghi nhật ký | Các thao tác khác của quản trị viên đều ghi; sửa hồ sơ của chính mình được coi là không nhạy cảm. Thêm một dòng `AuditLog.record` là xong (mục 14). |
| Quản trị viên khóa được quản trị viên khác | Chỉ chặn tự khóa mình (`SELF_LOCK`). |
| Nhập CSV không trong một giao dịch | Cố ý: dòng đúng vẫn được nhập, dòng sai liệt kê để sửa. |
| `deleteSubject` kiểm tra rồi mới xóa (hai bước, không khóa) | Nếu giữa hai bước có người thêm câu hỏi, khóa ngoại trong CSDL vẫn chặn lệnh `DELETE` → `DataAccessException`. |
| Không xóa được người dùng | Cố ý: bài thi tham chiếu `app_user`; khóa tài khoản thay cho xóa. |

---

[Về mục lục](#mục-lục)

## 13. Đoạn demo của mình

Chép từ [`docs/DEMO-SCRIPT.md`](../../docs/DEMO-SCRIPT.md) – "Phần 1 · Đỗ Phạm Bảo Hoàng – Giới thiệu và quản trị (2 phút)", thêm cột cuối.

| Bấm | Nói | Phía sau là mã nào |
|---|---|---|
| (slide) | "Đề tài 16 – hệ thống thi trực tuyến chạy trong phòng máy: một MySQL chung, hai ứng dụng desktop JavaFX." | `StaffApp` (tài khoản `oes_staff`, `AppContext.jdbc`), `ExamClientApp` (`oes_client`, `ClientContext.jdbc`) |
| Máy A: đăng nhập `admin` / `admin123` | "Một màn hình đăng nhập dùng chung cho hai ứng dụng; mỗi ứng dụng chỉ nhận đúng vai trò của mình." | `login.fxml` → `LoginController.onLogin` → `BackgroundTasks.run` → `StaffApp.login` → `AuthService.login` → `JdbcUserDao.findByUsername` → `AppShell.onLoggedIn` → `StaffHomeController.init` (menu quản trị) |
| Máy B: thử đăng nhập `admin` vào ứng dụng thi | "Tài khoản quản trị bị từ chối ở ứng dụng thi – kiểm tra này nằm ở tầng service, không phải ở giao diện." | Cùng `LoginController` → `ExamClientApp.login` → `StudentAuthService.login` → `JdbcStudentLoginDao.findByUsername` → `v_login` (mã băm rỗng) → `WRONG_APP` → `ErrorDialogs.show` |
| Người dùng → gõ `b24dcce0` → lọc Thí sinh | "Tìm, lọc, khóa tài khoản, nhập danh sách sinh viên từ CSV." | `UsersController.onSearch` → `AuthService.searchUsers` (`requireRole` ADMIN) → `JdbcUserDao.search` (`? IS NULL OR ...`, `LIKE`); nút khóa: `onToggleActive` → `AuthService.setActive`; CSV: `onImport` → `UserImportService.importStudents` |
| Môn học & lớp → chọn INT1332 | "Môn, lớp học phần, phân công giảng viên. Giảng viên chỉ quản lý được môn mình phụ trách." | `SubjectsController.loadDetails` → `SubjectService.listClasses` + `instructorsOf` → `JdbcSubjectDao.findClasses`, `findInstructorsOf`; quyền: `Instructor.canManage` + `Permissions.requireManage` |
| Đăng xuất | | `StaffHomeController.onLogout` → `ErrorDialogs.confirm` → `AppShell.logout` → `UserSession.end` → `Navigator.showLogin` |

Mẹo khi demo: nếu thầy hỏi "kiểm tra ở service thật không?", mở `StudentAuthService.login` chỉ dòng
`if (u.getRole() != Role.STUDENT)`, rồi mở `security.sql` chỉ dòng `CASE WHEN role = 'STUDENT' THEN password_hash ELSE ''`.

---

[Về mục lục](#mục-lục)

## 14. Câu hỏi vấn đáp

### Mức 1 – Cơ bản

**1. Vì sao `User` là lớp trừu tượng?**
Không có "người dùng chung chung": ai cũng là quản trị viên, giảng viên hoặc thí sinh. `abstract` cấm `new User(...)`
và buộc lớp con cài `getRole()`, `canManage(Subject)`.

**2. Đa hình nằm ở đâu trong phần của em?**
`Permissions.requireManage` gọi `actor.canManage(subject)` trên biến kiểu `User`; lúc chạy, Java gọi bản của
`Administrator` (luôn `true`), `Instructor` (`subjects.contains`) hay `Student` (luôn `false`). Thứ hai: `AppShell` –
`LoginController` gọi `shell.login(...)`, chạy bản của `StaffApp` hoặc `ExamClientApp`.

**3. Mật khẩu lưu thế nào?**
`PasswordHasher.hash`: salt 16 byte từ `SecureRandom` + SHA-256(salt + mật khẩu), lưu dạng Base64(salt) + "$" + Base64(hash)
trong cột `password_hash`. Kiểm tra bằng `PasswordHasher.matches`: tách salt, băm lại, so bằng `MessageDigest.isEqual`.

**4. Salt để làm gì?**
Cùng mật khẩu ra mã băm khác nhau → không dùng được bảng băm tra sẵn, không biết ai trùng mật khẩu với ai.

**5. Vì sao sai tên đăng nhập và sai mật khẩu báo cùng một câu?**
Nếu báo khác nhau, người dò thử biết tên nào có thật. Xem `AuthService.login`: `.filter(x -> x.checkPassword(password))`
gộp hai trường hợp vào một `orElseThrow`.

**6. Đăng xuất chạy những gì?**
`StaffHomeController.onLogout` → `ErrorDialogs.confirm` → `AppShell.logout` (`default`) → `UserSession.end()` →
`showLogin()` → `Navigator.showLogin` nạp lại `login.fxml`.

**7. Mã lỗi khác thông điệp chỗ nào?**
Mã (`WRONG_APP`) ổn định cho kiểm thử và log; thông điệp tiếng Việt cho người dùng, sửa chữ tự do. `ErrorDialogs` hiện
`getUserMessage()`; kiểm thử so `getCode()`.

**8. `AppContext` làm gì?**
Nơi duy nhất `new` DAO và service phía giảng viên. `AppContext.jdbc(cm)` lắp DAO MySQL; `AppContext.inMemory(db)` lắp DAO
bộ nhớ cho kiểm thử. Service không đổi gì (Câu hỏi chung 4).

**9. Vì sao quản trị viên không xóa được người dùng?**
`submission.student_id`, `session_student.student_id`, `instructor_subject.instructor_id` là khóa ngoại tới
`app_user`; xóa sẽ mất lịch sử bài thi. Thay bằng `AuthService.setActive(..., false)`.

**10. Tạo một thí sinh mới thì ghi những cột nào?**
`JdbcUserDao.insert`: `username, password_hash, full_name, email, role, active, must_change_password, student_code,
class_id`; mã `id` lấy bằng `getGeneratedKeys()` rồi `assignId`.

**11. Nhập CSV cần định dạng gì?**
`ma_sv,ho_ten,email,ma_lop`, UTF-8, dòng đầu là tiêu đề (bỏ qua); email và mã lớp để trống được.
Tên đăng nhập = mật khẩu ban đầu = mã SV viết thường; phải đổi ở lần đầu.

### Mức 2 – Đào sâu

**12. Vì sao `StudentAuthService.login` kiểm vai trò trước mật khẩu, còn `AuthService.login` thì ngược lại?**
`v_login` trả `password_hash = ''` cho giảng viên/quản trị viên, `PasswordHasher.matches` với chuỗi không có `$` luôn
`false`. Nếu kiểm mật khẩu trước, admin gõ đúng vẫn nhận "sai mật khẩu". Phía quản lý có đủ mã băm nên kiểm mật khẩu
trước để không lộ vai trò.

**13. `Subject` không ghi đè `equals` thì chuyện gì xảy ra?**
`Instructor.canManage` dùng `subjects.contains(subject)`; đối tượng môn nạp lúc đăng nhập và đối tượng môn của câu hỏi là
hai đối tượng khác nhau → `contains` so địa chỉ → `false` → giảng viên bị `SUBJECT_FORBIDDEN` trên chính môn của mình.
Ghi đè `equals` thì phải ghi đè `hashCode` (tập là `LinkedHashSet`).

**14. Vì sao `Permissions` không `public`?**
Chỉ gói `service` gọi được. Controller không thể "tự kiểm tra quyền rồi bỏ qua service". Quyền luôn kiểm ở service,
nên giao diện quên ẩn nút cũng không sao.

**15. Vì sao máy thí sinh đổi mật khẩu qua thủ tục lưu trữ, không `UPDATE` thẳng?**
`oes_client` không có quyền `UPDATE app_user` – nếu có, ai có mật khẩu `oes_client` sửa được mật khẩu admin.
`sp_student_change_password` chạy `SQL SECURITY DEFINER` (quyền người tạo), chỉ sửa dòng `role = 'STUDENT'` có mã băm cũ
khớp, mã băm mới phải chứa `$`. `oes_client` chỉ có `GRANT EXECUTE`.

**16. Tại sao truyền mã băm cũ vào thủ tục?**
So sánh rồi mới ghi. Ví dụ `JdbcSecurityTest.thíSinhĐổiMậtKhẩuQuaThủTụcLưuTrữ`: admin đặt lại mật khẩu trong lúc thí
sinh đang đăng nhập; mã băm thí sinh đang giữ đã cũ, thủ tục trả 0 → `PASSWORD_CHANGED_ELSEWHERE`.

**17. Vì sao `User` tách `hashNewPassword` và `applyNewPasswordHash`?**
Phía thí sinh phải chờ CSDL nhận mã băm mới rồi mới gán vào đối tượng; nếu gán trước mà thủ tục trả 0, đối tượng và
CSDL lệch nhau. `changePassword(newRaw)` gộp hai bước cho phía quản lý.

**18. `User.resetPassword` gọi `changePassword` – mà `changePassword` tắt cờ buộc đổi. Có lỗi không?**
Không: dòng sau `this.mustChangePassword = true;` bật lại. Gọi `changePassword` để mật khẩu tạm vẫn qua kiểm tra độ dài
và được băm.

**19. Giảng viên được phân công môn mới khi đang đăng nhập ở máy khác thì sao?**
Tập `Instructor.subjects` chỉ nạp ở `AuthService.login` (`subjectDao.loadSubjectsOf`). Phải đăng xuất, đăng nhập lại.
Cùng máy thì `JdbcSubjectDao.assignInstructor` cập nhật luôn đối tượng.

**20. `search` dùng `? IS NULL OR ...` có an toàn với SQL injection không? `%` ghép ở đâu?**
An toàn: câu SQL cố định, mọi giá trị qua `ps.setString`. `%` ghép vào **giá trị** (`"%" + keyword.strip() + "%"`),
không ghép vào câu SQL. Gõ `' OR 1=1 --` chỉ là một chuỗi đem đi `LIKE`.

**21. `BackgroundTasks.run` trả kết quả về luồng giao diện bằng cách nào?**
`javafx.concurrent.Task`: `call()` chạy trên luồng của `POOL`; JavaFX gọi `setOnSucceeded`/`setOnFailed` trên JavaFX
Application Thread. Nên `onSuccess` sửa bảng được ngay.

**22. Vì sao bể luồng là daemon?**
Luồng thường giữ JVM sống; đóng cửa sổ mà còn việc nền thì chương trình không thoát. Daemon thì JVM thoát khi không còn
luồng thường. `OesApplication.stop` còn gọi `BackgroundTasks.shutdown()`.

**23. Template Method trong `OesApplication` – đâu là khuôn, đâu là bước?**
Khuôn: `start(Stage)` (`final`): đặt ngôn ngữ, nạp cấu hình log, tạo `Navigator`, đặt bộ xử lý lỗi, tìm tệp cấu hình,
kết nối nền hoặc hiện màn hình máy chủ. Bước: `configFileName`, `defaultDbUser`, `connectTo` (trừu tượng), `onConnected`
(hook). `StaffApp.onConnected` khởi động `GradingScheduler`.

**24. Vì sao `ExamAppException` kế thừa `RuntimeException`?**
Đi xuyên ba tầng không cần `throws` ở mọi hàm; tầng view chỉ bắt một kiểu. Lỗi kỹ thuật `SQLException` (checked) bị đổi
thành `DataAccessException` ngay trong `ConnectionManager.execute`.

**25. Vì sao không có lớp `DuplicateKeyException` riêng mã lỗi?**
Nó là lỗi kỹ thuật (mã `DATA_ACCESS` kế thừa); service bắt và đổi thành lỗi nghiệp vụ có mã riêng: `AuthService.createUser`
→ `USERNAME_TAKEN`, `SubjectService.createSubject` → `SUBJECT_CODE_TAKEN`, `UserImportService` → câu lỗi theo dòng.

**26. Vì sao `ClientContext` an toàn ngay cả khi ai đó cấu hình nó bằng `oes_staff`?**
`ClientContext` không có `QuestionDao`, `ExamDao`, `GradingDao`; `StudentLoginDao` không có `insert`/`update`. Mã ứng dụng
thi không có lời gọi nào đọc đáp án hay sửa người dùng – lớp phòng thủ ở tầng kiểu dữ liệu, cộng với lớp ở CSDL.

**27. `UserSession` có cần không, khi không ai gọi `get()`?**
Trung thực: hiện chỉ có `start`/`end`. Mọi màn hình nhận người dùng qua tham số `init(...)` – rõ ràng hơn và kiểm thử dễ
hơn. Giữ lớp theo kế hoạch; có thể bỏ mà không ảnh hưởng chức năng.

### Mức 3 – Sửa trực tiếp ("thầy bảo đổi X ngay tại chỗ")

**28. "Mật khẩu tối thiểu 8 ký tự."**
Sửa `User.MIN_PASSWORD_LENGTH = 8`. Câu báo tự đổi theo (ghép từ hằng). Sửa thêm chữ tĩnh trong
[`fxml/change-password.fxml`](../../src/main/resources/fxml/change-password.fxml) (`noteLabel`: "Mật khẩu mới phải có ít nhất 6 ký tự.") và [`fxml/user-form.fxml`](../../src/main/resources/fxml/user-form.fxml)
(`promptText="ít nhất 6 ký tự"`). Kiểm: `./mvnw test -Dtest=StaffServicesTest,AdminAndResultsTest`. Sẽ đỏ
`đổiMậtKhẩuPhảiĐúngMậtKhẩuCũVàĐủSáuKýTự` (dùng `"123456"`) và `đặtLạiMậtKhẩuThìPhảiĐổiỞLầnĐăngNhậpSau` (dùng mật khẩu
tạm `"tam1234"` – vì `User.resetPassword` cũng qua kiểm tra độ dài) – sửa dữ liệu thử cho đủ 8 ký tự. Hai ca JDBC
`JdbcSecurityTest.thíSinhĐổiMậtKhẩuQuaThủTụcLưuTrữ`, `JdbcIntegrationTest.quảnLýNgườiDùngMônHọcVàLớpQuaCSDL` cũng dùng
`"tam1234"` – sửa luôn dù máy không có MySQL (chúng bị bỏ qua chứ không báo đỏ). Mật khẩu mặc định
khi nhập CSV không bị ảnh hưởng (đi thẳng `PasswordHasher.hash`).

**29. "Mật khẩu tạm 10 ký tự."**
`AuthService.TEMP_PASSWORD_LENGTH = 10`. Sửa `assertEquals(8, second.length())` trong
`AdminAndResultsTest.đặtLạiMậtKhẩuSinhMậtKhẩuTạmNgẫuNhiên`. Chạy `./mvnw test -Dtest=AdminAndResultsTest`.

**30. "Ghi nhật ký khi người dùng sửa hồ sơ."**
Trong `AuthService.updateProfile`, sau `userDao.update(actor);` thêm
`AuditLog.record(actor, "UPDATE_PROFILE", actor.getUsername());`. Chạy `./mvnw test -Dtest=StaffServicesTest`
(`ngườiDùngTựSửaHồSơ` vẫn xanh); mở StaffApp → Hồ sơ → Lưu → màn hình Nhật ký gõ `UPDATE_PROFILE`.

**31. "Cho giảng viên xem nhật ký."**
`AuditLogService.recentLines`: đổi `Permissions.requireRole(actor, Role.ADMIN)` thành `Permissions.requireStaff(actor)`.
`StaffHomeController.init`: thêm `items.add(new MenuItem("Nhật ký", ...))` vào nhánh `else`. Sửa dòng
`assertThrows(AccessDeniedException.class, () -> service.recentLines(teacher, "", 10))` trong
`nhậtKýLấyDòngMớiNhấtTrướcVàLọcTheoTừKhóa` (giờ phải không ném). Chạy `./mvnw test -Dtest=AdminAndResultsTest`.

**32. "Không cho khóa tài khoản quản trị viên."**
`AuthService.setActive`, sau dòng `SELF_LOCK` thêm
`ValidationException.require(active || target.getRole() != Role.ADMIN, "ADMIN_LOCK", "Không thể khóa tài khoản quản trị viên.");`
Viết thêm một dòng trong `StaffServicesTest.khóaTàiKhoảnThìKhôngĐăngNhậpĐượcVàKhôngTựKhóaMình` (tạo admin thứ hai, khóa
→ mong `ADMIN_LOCK`). Theo `ErrorCodeTest`, mã mới nên có ca kiểm thử.

**33. "Đăng nhập không phân biệt hoa thường."**
Tài khoản tạo qua biểu mẫu (`createAccount`) và qua CSV (`UserImportService.parse`) đều lưu tên đăng nhập viết thường. Trong `AuthService.login` và
`StudentAuthService.login`, đổi `username.strip()` thành `username.strip().toLowerCase(Locale.ROOT)` (thêm
`import java.util.Locale` cho `StudentAuthService`). Phải sửa **cả hai** service. Chạy `./mvnw test`.

**34. "Đổi câu báo sai mật khẩu."**
Hai chỗ: `AuthService.login` và `StudentAuthService.invalidLogin`. Kiểm thử so mã `INVALID_LOGIN` nên không cần sửa
kiểm thử. Chạy `./mvnw test`.

**35. "Thêm vai trò Giám thị."** (câu lớn, nói hướng làm)
Thêm hằng `Role` (+ tên tiếng Việt), lớp con `Proctor extends User` cài `getRole`, `canManage`. Trình biên dịch sẽ báo
thiếu nhánh ở hai switch biểu thức: `AuthService.createAccount` và `JdbcUserDao.mapUser` – đó là lợi ích của switch
biểu thức trên enum. Sửa `ENUM('ADMIN','INSTRUCTOR','STUDENT')` trong [`db/schema.sql`](../../db/schema.sql); thêm menu ở
`StaffHomeController.init`. `Permissions` **không phải sửa** (OCP). Viết kiểm thử tạo tài khoản vai trò mới.

---

[Về mục lục](#mục-lục)

## 15. Thuật ngữ

| Thuật ngữ | Nghĩa trong dự án |
|---|---|
| actor | Người đang thao tác, tham số đầu của mọi hàm service, dùng để kiểm tra quyền |
| salt | Chuỗi byte ngẫu nhiên trộn vào trước khi băm mật khẩu |
| hash (băm) | Hàm một chiều; ở đây SHA-256 |
| constant-time compare | So sánh mất thời gian như nhau dù sai ở byte nào (`MessageDigest.isEqual`) |
| view (khung nhìn) | Câu `SELECT` được lưu trong CSDL, dùng như bảng (`v_login`) |
| stored procedure (thủ tục lưu trữ) | Đoạn SQL lưu trong CSDL, gọi bằng `CALL` (`sp_student_change_password`) |
| `SQL SECURITY DEFINER` | Khung nhìn/thủ tục chạy với quyền người tạo, không phải người gọi |
| compare-and-set | Chỉ ghi khi giá trị hiện tại vẫn bằng giá trị đã biết |
| BOM | Ký tự `\uFEFF` Excel thêm vào đầu tệp UTF-8 |
| Composition Root | Nơi duy nhất tạo và nối các đối tượng (`AppContext`, `ClientContext`) |
| Template Method | Lớp cha cố định trình tự, lớp con điền bước (`OesApplication.start`) |
| hook | Bước tùy chọn có thân mặc định trong Template Method (`onConnected`) |
| JavaFX Application Thread | Luồng duy nhất được vẽ và sửa giao diện JavaFX |
| daemon thread | Luồng không giữ JVM sống khi chương trình đóng |
| package-private | Không ghi `public`/`private`: chỉ lớp cùng gói thấy (`Permissions`) |
| single-table inheritance | Nhiều lớp con lưu chung một bảng, phân biệt bằng một cột (`app_user.role`) |

---

[Về mục lục](#mục-lục)

## 16. Tự kiểm tra trước buổi vấn đáp

Phải tự nói được, không nhìn tài liệu:

- [ ] Vẽ cây `User` → `Administrator`/`Instructor`/`Student`, nói mỗi lớp trả lời `canManage` thế nào.
- [ ] Giải thích vì sao `Subject.equals` so theo `code` và chuyện gì xảy ra nếu bỏ.
- [ ] Bốn hàm của `Permissions`, vì sao lớp không `public`, vì sao không dùng `instanceof` trong `requireManage`.
- [ ] Định dạng `salt$hash`, vai trò của salt, vì sao `MessageDigest.isEqual`, vì sao chuỗi rỗng không bao giờ khớp.
- [ ] Thứ tự kiểm tra trong `AuthService.login` và trong `StudentAuthService.login`, và vì sao khác nhau.
- [ ] Nội dung `v_login` và vì sao nó trả `''` cho giảng viên.
- [ ] Bốn điều kiện `WHERE` của `sp_student_change_password` và ý nghĩa `SQL SECURITY DEFINER`.
- [ ] Luồng bắt buộc đổi mật khẩu: cờ lấy ở đâu, kiểm tra ở `AppShell.onLoggedIn`, nút Hủy thành Đăng xuất.
- [ ] Đặt lại mật khẩu: bộ ký tự, vì sao bỏ `0`, `1`, `i`, `l`, `o`; vì sao hiện cho quản trị viên chứ không gửi email.
- [ ] Nhập CSV: BOM, dòng tiêu đề, `CsvLine` xử lý nháy kép, lỗi theo dòng, mật khẩu mặc định.
- [ ] Vì sao khóa tài khoản chứ không xóa.
- [ ] `AppContext` vs `ClientContext`: mỗi cái lắp gì, vì sao `ClientContext` không có `QuestionDao`.
- [ ] Template Method trong `OesApplication`: khuôn là gì, bước là gì, hook là gì.
- [ ] `AppShell`: vì sao một `LoginController` dùng được cho hai ứng dụng.
- [ ] `BackgroundTasks`: vì sao luồng nền, kết quả về luồng giao diện thế nào, vì sao daemon.
- [ ] `ErrorDialogs`: ba nhánh xử lý lỗi và câu người dùng thấy ở mỗi nhánh.
- [ ] Kể ít nhất 10 mã lỗi của phần 1 và nơi ném.
- [ ] Lần được từ nút "Đăng nhập" tới câu `SELECT ... FROM app_user u LEFT JOIN course_class ...`.
- [ ] Chỉ ra một ví dụ cho mỗi chữ S, O, L, I, D trong phần của mình.
- [ ] Ba giới hạn trung thực: SHA-256 một vòng, `v_login` lộ mã băm thí sinh với ai có mật khẩu `oes_client`, nhật ký mỗi máy một tệp.
- [ ] Chạy được `./mvnw test -Dtest=StaffServicesTest` và nói một ca kiểm thử chứng minh điều gì.
- [ ] Làm thử tại chỗ câu 28 (mật khẩu 8 ký tự) và câu 30 (ghi nhật ký sửa hồ sơ), chạy lại kiểm thử.
