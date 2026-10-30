# Phần 2 – Ngân hàng câu hỏi và đề thi

[← README](../../README.md) · [Phân công và câu hỏi chung](../PHAN-CONG.md) · [Đoạn demo của phần này](../DEMO-SCRIPT.md#phần-2--đỗ-thị-vân-anh--giảng-viên-câu-hỏi-và-đề-thi-2-phút-30) · Phần: [1](phan-1-nguoi-dung-quan-tri.md) · **2** · [3](phan-3-ca-thi-lam-bai.md) · [4](phan-4-cham-bai-ket-qua-csdl.md)

> Người phụ trách: **Đỗ Thị Vân Anh** (B24DCCE007) · Đọc kèm: [`docs/PHAN-CONG.md`](../../docs/PHAN-CONG.md) (câu hỏi chung)

Quy ước đường dẫn trong tài liệu này (giống `PHAN-CONG.md`): `oes/` = [`src/main/java/com/group2/oes/`](../../src/main/java/com/group2/oes/),
`fxml/` = [`src/main/resources/fxml/`](../../src/main/resources/fxml/), `test/` = [`src/test/java/com/group2/oes/`](../../src/test/java/com/group2/oes/).
Những câu đã có trong mục "Câu hỏi chung" của `PHAN-CONG.md` (ba tầng, DIP, `BackgroundTasks`, lỗi, SQL injection...)
chỉ được nhắc lại ngắn và dẫn chiếu.

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

"Em phụ trách phần **ngân hàng câu hỏi và đề thi** – mọi thứ giảng viên làm trước khi mở ca thi.

Ở tầng model, câu hỏi là lớp trừu tượng `Question` với bốn lớp con: `SingleChoiceQuestion`, `MultiChoiceQuestion`,
`TrueFalseQuestion`, `FillInBlankQuestion`. Mỗi lớp con **tự kiểm tra dữ liệu trong constructor** (ví dụ câu một đáp án
phải có đúng một phương án đúng) và **tự biết cách so khớp câu trả lời** qua `isCorrect` và `correctRatio`. Vì loại câu
hỏi chỉ biết lúc chạy (từ biểu mẫu, từ tệp, từ CSDL), việc chọn lớp con nằm ở một chỗ duy nhất là `QuestionFactory` –
mẫu Factory.

Đề thi là `Exam` gồm các `ExamItem` (thứ tự + điểm của câu trong đề). Đề có cấu hình `MultiChoicePolicy` – câu nhiều đáp
án chấm toàn phần hay theo tỷ lệ – và từ cấu hình đó chọn chiến lược chấm cho phần 4. Đề tạo được bằng hai cách: chọn tay
hoặc sinh ngẫu nhiên theo ma trận chương × mức độ (`ExamMatrix`); trước khi sinh, `QuestionBank.findShortages` báo thiếu
câu ở ô nào. Khi đề được dùng mở ca thi thì bị khóa; muốn đổi phải tạo phiên bản mới.

Phía service có `QuestionBankService` (thêm, sửa, xóa, nhập câu hỏi từ tệp văn bản, báo lỗi theo dòng) và `ExamService`
(tạo đề, sinh đề, danh sách đề, phiên bản mới). Mọi hàm đều kiểm tra giảng viên có phụ trách môn đó không. Tầng DAO lưu
câu hỏi kèm phương án trong **một giao dịch**. Cuối cùng là `SeedTool` tạo dữ liệu mẫu 60 câu hỏi và một đề 20 câu để demo."

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 1. Phạm vi

### Yêu cầu thuộc phần này (mã số lấy từ [`docs/PLAN.md`](../../docs/PLAN.md), trạng thái từ [`docs/PROGRESS.md`](../../docs/PROGRESS.md))

| Mã | Nội dung | Lớp chính | Màn hình | Trạng thái |
|---|---|---|---|---|
| FR-08 | Thêm, sửa, xóa câu hỏi | `QuestionBankService` | Ngân hàng câu hỏi | Xong |
| FR-09 | 4 loại câu hỏi | `Question` + lớp con, `QuestionFactory` | Biểu mẫu câu hỏi | Xong (M4) |
| FR-10 | Thuộc tính câu hỏi (chương, mức độ, điểm) | `Question` | Biểu mẫu câu hỏi | Xong (M4) |
| FR-11 | Lọc, tìm câu hỏi | `QuestionBank`, `Criteria` | Ngân hàng câu hỏi | Xong |
| FR-12 | Nhập câu hỏi từ tệp | `QuestionBankService.importFile` | Ngân hàng câu hỏi | Xong |
| FR-13 | Cấm xóa câu đã dùng | `QuestionBankService` | Ngân hàng câu hỏi | Xong |
| FR-14 | Tạo đề thủ công | `ExamService.createManual` | Tạo đề | Xong |
| FR-15 | Sinh đề theo ma trận | `QuestionBank.pick`, `ExamService.generate` | Tạo đề | Xong |
| FR-16 | Điểm từng câu, tổng điểm | `Exam` | Tạo đề | Xong |

Quy tắc nghiệp vụ (BR) và yêu cầu phi chức năng liên quan:

| Mã | Nội dung | Vai trò của phần 2 |
|---|---|---|
| BR-08 | Câu nhiều đáp án chấm theo cấu hình của đề: toàn phần hoặc theo tỷ lệ | Sở hữu cấu hình (`MultiChoicePolicy`, `Exam.strategyFor`); phần 4 sở hữu thuật toán chấm |
| BR-09 | Đề đã dùng cho ca thi thì không sửa được; muốn đổi phải tạo phiên bản mới | Sở hữu `Exam.lock`, `ensureEditable`, `newVersion`, `ExamDao.lock` |
| BR-11 | Tổng điểm đề bằng tổng điểm các câu và phải lớn hơn 0 | `Exam.getTotalPoint`, `Exam.validateForUse` |
| NFR-10 | Thêm loại câu hỏi hoặc cách chấm không sửa lớp cũ | Factory + lớp con `Question` |
| NFR-12 | Có script tạo bảng và dữ liệu mẫu | `SeedTool` + `seed-questions.txt` |

Use case: **UC-06** (quản lý ngân hàng câu hỏi, FR-08–11, 13), **UC-07** (nhập câu hỏi từ tệp, FR-12),
**UC-08** (tạo đề thi, FR-14, 16), **UC-09** (sinh đề theo ma trận, FR-15). UC-06 và UC-08 có đặc tả chi tiết trong báo cáo M1.

### Phần này sở hữu (theo bảng "Phần 2" trong `PHAN-CONG.md`)

- Model: [`oes/model/question/`](../../src/main/java/com/group2/oes/model/question/) (`Question`, 4 lớp con, `AnswerOption`, `QuestionType`); [`oes/model/QuestionBank`](../../src/main/java/com/group2/oes/model/QuestionBank.java),
  `Exam`, `ExamItem`, `ExamMatrix`, `Criteria`, `Difficulty`, `MultiChoicePolicy`.
- Factory: [`oes/factory/QuestionFactory`](../../src/main/java/com/group2/oes/factory/QuestionFactory.java), `QuestionData`.
- Service: `QuestionBankService`, `ExamService`.
- DAO: `QuestionDao`, `ExamDao`, `JdbcQuestionDao`, `JdbcExamDao`, `MemoryQuestionDao`, `MemoryExamDao`.
- Công cụ: [`oes/tools/SeedTool`](../../src/main/java/com/group2/oes/tools/SeedTool.java); `seed-questions.txt`, `sample-questions.txt`.
- Màn hình: `QuestionBankController`, `QuestionFormController`, `ExamListController`, `ExamBuilderController` +
  `question-bank.fxml`, `question-form.fxml`, `exam-list.fxml`, `exam-builder.fxml`.
- CSDL: bảng `question`, `answer_option`, `exam`, `exam_item`.
- Kiểm thử: `QuestionTest`, `ExamAndBankTest`, `SeedToolTest`, phần câu hỏi/đề trong `StaffServicesTest`.

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 2. Ranh giới với các phần khác

| Chỗ nối | Phía phần 2 | Phía phần kia | Ý nghĩa |
|---|---|---|---|
| Phân quyền (phần 1) | `QuestionBankService.*`, `ExamService.*` gọi `Permissions.requireManage(actor, subject)` | `Permissions.requireManage` → `User.canManage` (`Instructor.canManage` = `subjects.contains(subject)`, `Administrator` luôn `true`, `Student` luôn `false`) | Giảng viên chỉ thao tác môn mình phụ trách |
| Danh sách môn trong ô chọn (phần 1) | `QuestionBankController.init`, `ExamListController.init` | `SubjectService.listSubjects(actor)` – giảng viên nhận môn được phân công, quản trị viên nhận mọi môn | Ô "Môn" chỉ có môn được phép |
| So sánh môn (phần 1) | `QuestionBank.add`, `Exam.addItem` dùng `Subject.equals` | `Subject.equals` so theo `code` | Câu hỏi khác môn bị từ chối (`WRONG_SUBJECT`) |
| Lắp ráp (phần 1) | Service nhận interface DAO qua constructor | `AppContext`: `new JdbcQuestionDao(cm)`, `new JdbcExamDao(cm, q)`, `new QuestionBankService(questions)`, `new ExamService(exams)` | DIP |
| Nhật ký (phần 1) | `AuditLog.record(actor, "UPDATE_QUESTION", ...)` v.v. | `AuditLog.record` ghi vào nhật ký | NFR-05 |
| Mở ca thi (phần 3) | `ExamListController.onOpenSession` mở `session-form.fxml`; `Exam.lock`, `Exam.validateForUse`; `ExamDao.lock` | `ExamSessionService.createSession` gọi `exam.lock()` rồi `examDao.lock(exam.getExamId())`; constructor `ExamSession` gọi `exam.validateForUse()` | BR-09, BR-11 |
| Đọc đề cho ca thi (phần 3) | `ExamDao.findById` | `JdbcExamSessionDao` dùng `examDao.findById(...)` để dựng `ExamSession` | Ca thi giữ đề có đáp án – chỉ phía giảng viên |
| Đề cho máy thí sinh (phần 3) | Bảng `exam_item`, `question`, `answer_option` | Khung nhìn `v_exam_paper` (bỏ `is_correct`, `correct_value`, `case_sensitive`), đọc bằng `StudentExamDao.loadPaper` | NFR-04: `Question` không bao giờ tới máy thí sinh |
| Chấm bài (phần 4) | `Exam.strategyFor(question)`, `ExamItem.grade(response, strategy)`, `Question.isCorrect`, `Question.correctRatio` | `SubmissionGrader.grade` gọi `answer.grade(exam.strategyFor(item.getQuestion()))`; `ExactMatchGrading`, `PartialMatchGrading`, `TextMatchGrading` gọi `isCorrect`/`correctRatio` | Mẫu Strategy: phần 2 chọn chiến lược, phần 4 cài đặt chiến lược |
| Xem lại đáp án (phần 4) | Đáp án chấp nhận của câu điền khuyết lưu trong `answer_option` với `is_correct = TRUE` | Khung nhìn `v_review` lấy ra thành `accepted_answer` | |
| Lỗi CSDL (phần 4) | `JdbcQuestionDao`, `JdbcExamDao` dùng `cm.execute`, `cm.inTransaction` | `ConnectionManager.inTransaction` (commit/rollback), `JdbcErrors.translate` | DAO không để lọt `SQLException` |

Câu trả lời ngắn khi bị hỏi chéo: *"Phần em tạo ra `Exam`. Phần 3 lấy đề đó mở ca thi và khóa nó; phần 4 hỏi đề
`strategyFor(câu)` để biết chấm câu đó bằng chiến lược nào. Máy thí sinh không bao giờ thấy lớp `Question` của em –
nó đọc khung nhìn `v_exam_paper` không có cột đáp án."*

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 3. Thứ tự đọc mã

1. [`oes/model/question/QuestionType.java`](../../src/main/java/com/group2/oes/model/question/QuestionType.java) – 4 loại câu, tên hiển thị tiếng Việt.
2. [`oes/model/question/AnswerOption.java`](../../src/main/java/com/group2/oes/model/question/AnswerOption.java) – phương án; `assignId` gán một lần.
3. [`oes/model/question/Question.java`](../../src/main/java/com/group2/oes/model/question/Question.java) – phần chung, kiểm tra chung trong constructor, 3 phương thức trừu tượng.
4. [`oes/model/question/SingleChoiceQuestion.java`](../../src/main/java/com/group2/oes/model/question/SingleChoiceQuestion.java) – kiểm tra "đúng 1 phương án đúng", so khớp mã phương án.
5. [`oes/model/question/MultiChoiceQuestion.java`](../../src/main/java/com/group2/oes/model/question/MultiChoiceQuestion.java) – `correctRatio` có trừ phần chọn sai.
6. [`oes/model/question/TrueFalseQuestion.java`](../../src/main/java/com/group2/oes/model/question/TrueFalseQuestion.java) – không có phương án, vì sao không dùng thẳng `Boolean.parseBoolean`.
7. [`oes/model/question/FillInBlankQuestion.java`](../../src/main/java/com/group2/oes/model/question/FillInBlankQuestion.java) – chuẩn hóa chuỗi (NFC, khoảng trắng, hoa thường).
8. [`oes/factory/QuestionData.java`](../../src/main/java/com/group2/oes/factory/QuestionData.java) – gói dữ liệu thô, các hàm `with...`, `from(Question)`.
9. [`oes/factory/QuestionFactory.java`](../../src/main/java/com/group2/oes/factory/QuestionFactory.java) – switch trên enum chọn lớp con.
10. [`oes/model/Difficulty.java`](../../src/main/java/com/group2/oes/model/Difficulty.java), `MultiChoicePolicy.java`, `Criteria.java` – enum và record nhỏ.
11. [`oes/model/QuestionBank.java`](../../src/main/java/com/group2/oes/model/QuestionBank.java) – lọc, `findShortages`, `pick`.
12. [`oes/model/ExamMatrix.java`](../../src/main/java/com/group2/oes/model/ExamMatrix.java) – ma trận, record lồng `Entry`.
13. [`oes/model/ExamItem.java`](../../src/main/java/com/group2/oes/model/ExamItem.java), [`oes/model/Exam.java`](../../src/main/java/com/group2/oes/model/Exam.java) – điểm từng câu, khóa đề, phiên bản, chọn chiến lược chấm.
14. [`oes/service/QuestionBankService.java`](../../src/main/java/com/group2/oes/service/QuestionBankService.java) – thêm/sửa/xóa/nhập tệp.
15. [`oes/service/ExamService.java`](../../src/main/java/com/group2/oes/service/ExamService.java) – tạo, sinh, liệt kê, phiên bản mới.
16. [`oes/dao/QuestionDao.java`](../../src/main/java/com/group2/oes/dao/QuestionDao.java), `ExamDao.java` – hợp đồng (interface).
17. [`oes/dao/jdbc/JdbcQuestionDao.java`](../../src/main/java/com/group2/oes/dao/jdbc/JdbcQuestionDao.java) – giao dịch, đọc bằng hai truy vấn, dựng lại qua Factory.
18. [`oes/dao/jdbc/JdbcExamDao.java`](../../src/main/java/com/group2/oes/dao/jdbc/JdbcExamDao.java) – lưu đề + mục đề, `restoreItem`.
19. [`oes/dao/memory/MemoryQuestionDao.java`](../../src/main/java/com/group2/oes/dao/memory/MemoryQuestionDao.java), `MemoryExamDao.java` – bản trong bộ nhớ cho kiểm thử.
20. [`fxml/question-bank.fxml`](../../src/main/resources/fxml/question-bank.fxml) + `QuestionBankController.java` – bảng, bộ lọc, nhập tệp.
21. [`fxml/question-form.fxml`](../../src/main/resources/fxml/question-form.fxml) + `QuestionFormController.java` – biểu mẫu đổi theo loại, lỗi hiện cạnh ô.
22. [`fxml/exam-list.fxml`](../../src/main/resources/fxml/exam-list.fxml) + `ExamListController.java` – danh sách đề, phiên bản mới.
23. [`fxml/exam-builder.fxml`](../../src/main/resources/fxml/exam-builder.fxml) + `ExamBuilderController.java` – chọn tay, ma trận.
24. [`oes/tools/SeedTool.java`](../../src/main/java/com/group2/oes/tools/SeedTool.java) + `seed-questions.txt` – dữ liệu mẫu đi qua service.
25. [`db/schema.sql`](../../db/schema.sql) (bảng `question` → `exam_item`) và đoạn `v_exam_paper` trong [`db/security.sql`](../../db/security.sql).
26. [`test/QuestionTest.java`](../../src/test/java/com/group2/oes/QuestionTest.java), [`test/ExamAndBankTest.java`](../../src/test/java/com/group2/oes/ExamAndBankTest.java), [`test/SeedToolTest.java`](../../src/test/java/com/group2/oes/SeedToolTest.java).

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 4. Kiến thức Java/OOP nền

Chỉ những khái niệm dùng trong phần này, minh họa bằng chính mã của phần.

**Lớp trừu tượng và phương thức trừu tượng.** `Question` khai báo `public abstract class Question` với ba phương thức
`abstract`: `getType()`, `isCorrect(String)`, `correctRatio(String)`. Không `new Question(...)` được; lớp con bắt buộc
cài đặt ba phương thức này, nếu không trình biên dịch báo lỗi. Chọn lớp trừu tượng chứ không phải interface vì `Question`
có **trạng thái chung** (mã, môn, nội dung, chương, mức độ, điểm) và **kiểm tra chung trong constructor** – interface
không có trường đối tượng và không có constructor.

**Kế thừa và `super(...)`.** Constructor mỗi lớp con gọi `super(questionId, subject, content, chapter, difficulty, defaultPoint)`
ở dòng đầu, nên kiểm tra chung của lớp cha luôn chạy trước kiểm tra riêng. Constructor lớp cha là `protected`: chỉ lớp
con gọi được.

**Đa hình (ghi đè – override).** Nơi chấm bài chỉ gọi `question.isCorrect(response)`; Java tự gọi bản của lớp con thật
lúc chạy. `getOptions()` có bản mặc định ở lớp cha (trả `List.of()`), hai lớp trắc nghiệm ghi đè. `TrueFalseQuestion` ghi
đè `getDisplayOptions()` để trả `List.of("Đúng", "Sai")`.

**Đóng gói và bất biến.** Trường đều `private final` (trừ mã, gán sau khi lưu). Danh sách được chép thành bản chỉ đọc:
`this.options = List.copyOf(options);` (nơi gọi sửa danh sách gốc cũng không ảnh hưởng câu hỏi), `Exam.getItems()` trả
`Collections.unmodifiableList(items)`. Mã chỉ gán một lần bằng `assignId`.

**Enum có trường.** `QuestionType`, `Difficulty`, `MultiChoicePolicy` mỗi hằng mang một `displayName` tiếng Việt.
`name()` (ví dụ `"SINGLE_CHOICE"`) dùng để lưu CSDL và đọc tệp; `displayName()` dùng trên giao diện.

**Record (Java 16+, được phép trong Java 17).** `Criteria`, `QuestionData`, `ExamMatrix.Entry`, `QuestionBank.PoolKey`,
`JdbcQuestionDao.QuestionRow`. Record tự sinh constructor, getter (`chapter()`, không có `get`), `equals`, `hashCode`.
`PoolKey` dùng làm **khóa của `Map`** – nhờ `equals`/`hashCode` tự sinh mà hai `new PoolKey(1, EASY)` là cùng một khóa.
`ExamMatrix.Entry` có *compact constructor* để kiểm tra `count > 0`.

**Switch biểu thức và `yield`.** `QuestionFactory.create` dùng `return switch (type) { case ... -> ...; }`. Với enum,
switch biểu thức phải phủ hết các hằng, thiếu một nhánh là lỗi biên dịch. Nhánh nhiều dòng dùng khối `{ ... yield ...; }`.

**Pattern matching `instanceof` (Java 16+).** `if (q instanceof TrueFalseQuestion tf) { ... tf.getCorrectValue() ... }`
vừa kiểm tra kiểu vừa ép kiểu – dùng trong `QuestionData.from` và `JdbcQuestionDao.setQuestionColumns`.

**Phương thức tạo tĩnh (static factory method).** `AnswerOption.of`, `Exam.draft`, `Criteria.any`, `QuestionData.basic`:
tên nói rõ ý nghĩa hơn constructor nhiều tham số (`Exam.draft` = mã 0, phiên bản 1, chưa khóa).

**Collection.** `LinkedHashMap` (giữ thứ tự thêm, tra theo khóa O(1)) trong `QuestionBank`; `HashSet` chặn trùng
trong `Exam.questionIds`; `EnumMap` trong `Exam.strategies`; `Collectors.groupingBy` gom câu theo (chương, mức độ);
`Collections.shuffle(list, random)` trộn.

**Ngoại lệ không kiểm tra (unchecked).** `ValidationException` kế thừa `ExamAppException` → `RuntimeException`, mang
**mã lỗi ổn định** (`getCode()`) và thông điệp tiếng Việt. `ValidationException.require` (điều kiện, mã, thông điệp) viết
gọn một dòng kiểm tra. Lỗi do lập trình (gán mã hai lần) thì ném `IllegalStateException`, không phải `ValidationException`.

**`Optional`.** `QuestionDao.findById` trả `Optional<Question>`; service dùng `.orElseThrow(...)` để biến "không có"
thành lỗi nghiệp vụ `QUESTION_NOT_FOUND`.

**Interface và nhiều cài đặt.** `QuestionDao` có `JdbcQuestionDao` (MySQL) và `MemoryQuestionDao` (bộ nhớ, cho kiểm thử).
Service chỉ biết `QuestionDao`.

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 5. Từng lớp chính

### 5.1. `QuestionType` (enum)

`SINGLE_CHOICE("Một đáp án")`, `MULTI_CHOICE("Nhiều đáp án")`, `TRUE_FALSE("Đúng/Sai")`, `FILL_IN_BLANK("Điền khuyết")`.

- Tên hằng trùng đúng giá trị cột `question.type` (`ENUM('SINGLE_CHOICE','MULTI_CHOICE','TRUE_FALSE','FILL_IN_BLANK')`)
  và chữ đầu mỗi dòng tệp nhập, nên đọc ngược bằng `QuestionType.valueOf(...)`.
- **Vì sao vẫn cần enum khi đã có 4 lớp con?** Lớp con là *hành vi*, enum là *nhãn dữ liệu*: dùng làm khóa `EnumMap`
  chiến lược chấm trong `Exam`, làm tiêu chí lọc trong `Criteria`, làm giá trị ô chọn trên biểu mẫu, và làm đầu vào cho
  `QuestionFactory`. Mỗi lớp con trả đúng một hằng qua `getType()`.
- `displayName()` thêm ở Phase 4–5; không đổi `toString()` để thông điệp và nhật ký cũ không đổi theo (`DESIGN-CHANGES.md`).

### 5.2. `AnswerOption`

Dữ liệu: `optionId` (0 khi chưa lưu), `content`, `correct`.

<sub>📄 [oes/model/question/AnswerOption.java](../../src/main/java/com/group2/oes/model/question/AnswerOption.java)</sub>

```java
// oes/model/question/AnswerOption.java
public AnswerOption(int optionId, String content, boolean correct) {
    ValidationException.require(content != null && !content.isBlank(),
            "OPTION_EMPTY", "Nội dung phương án không được để trống.");
    this.optionId = optionId;
    this.content = content.strip();
    this.correct = correct;
}

public static AnswerOption of(String content, boolean correct) {
    return new AnswerOption(0, content, correct);
}

/** Chỉ gán được một lần, khi DAO vừa lưu phương án xuống CSDL. */
public void assignId(int id) {
    if (this.optionId != 0) {
        throw new IllegalStateException("Phương án đã có mã " + optionId);
    }
    this.optionId = id;
}
```

- **Vì sao `optionId` quan trọng:** câu trả lời của thí sinh cho câu trắc nghiệm là **mã phương án** (`"12"`, `"12,14"`),
  không phải thứ tự A/B/C. Nhờ vậy trộn phương án trên máy thí sinh (phần 3) không ảnh hưởng chấm điểm.
- **Vì sao `assignId` ném `IllegalStateException`:** gán mã hai lần là lỗi lập trình (DAO gọi nhầm), không phải lỗi người
  dùng nhập sai, nên không dùng `ValidationException`. Kiểm thử `giaoDịchLỗiGiữaChừngThìKhôngGhiGìCả` lợi dụng chính
  quy tắc này để chứng minh giao dịch rollback (xem mục 11).
- `strip()` bỏ khoảng trắng hai đầu để " A " và "A" không thành hai phương án khác nhau.

### 5.3. `Question` (lớp trừu tượng)

Dữ liệu chung: `questionId`, `subject`, `content`, `chapter`, `difficulty`, `defaultPoint`.

<sub>📄 [oes/model/question/Question.java](../../src/main/java/com/group2/oes/model/question/Question.java)</sub>

```java
// oes/model/question/Question.java
protected Question(int questionId, Subject subject, String content,
                   int chapter, Difficulty difficulty, double defaultPoint) {
    ValidationException.require(content != null && !content.isBlank(),
            "QUESTION_EMPTY", "Nội dung câu hỏi không được để trống.");
    ValidationException.require(chapter > 0,
            "CHAPTER_INVALID", "Số chương phải lớn hơn 0.");
    ValidationException.require(defaultPoint > 0,
            "POINT_INVALID", "Điểm mặc định của câu hỏi phải lớn hơn 0.");
    this.questionId = questionId;
    this.subject = Objects.requireNonNull(subject, "subject");
    this.content = content.strip();
    this.chapter = chapter;
    this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
    this.defaultPoint = defaultPoint;
}

public abstract QuestionType getType();

/** Câu trả lời có hoàn toàn đúng hay không. */
public abstract boolean isCorrect(String response);

/** Mức đúng trong khoảng [0, 1], dùng cho chiến lược chấm theo tỷ lệ. */
public abstract double correctRatio(String response);
```

**Bất biến (invariant) – điều luôn đúng với mọi đối tượng `Question` tồn tại:** nội dung không rỗng, chương > 0, điểm
mặc định > 0, có môn, có mức độ.

**Vì sao kiểm tra trong constructor của model, không phải ở controller?** Đây là câu chắc chắn bị hỏi.
1. **Mọi đường tạo câu hỏi đều đi qua constructor:** biểu mẫu (`QuestionFormController`), nhập tệp
   (`QuestionBankService.importFile`), đọc từ CSDL (`JdbcQuestionDao.load`), `SeedTool`, kiểm thử. Kiểm tra ở controller
   thì đường nhập tệp và đường đọc CSDL bỏ qua kiểm tra.
2. **Không thể tồn tại một đối tượng sai.** Đã có đối tượng `SingleChoiceQuestion` trong tay thì chắc chắn nó có đúng một
   phương án đúng – mọi nơi dùng không phải kiểm tra lại.
3. **Kiểm thử được không cần giao diện:** `QuestionTest` gọi thẳng constructor.
4. Kiểm tra chung đặt ở lớp cha nên **bốn lớp con được hưởng mà không chép lại** – kiểm thử
   `dữLiệuChungKhôngHợpLệBịChặnỞLớpCha` chứng minh.

**Định dạng câu trả lời** (ghi trong javadoc của `Question`): một đáp án `"12"`; nhiều đáp án `"12,14"`; đúng/sai
`"true"`/`"false"`; điền khuyết là văn bản. Mọi loại đều là `String` để bảng `answer.response` chỉ cần một cột `TEXT`.

`isBlank(response)` là hàm `protected static` dùng chung cho lớp con: câu bỏ trống luôn sai, không ném lỗi.

### 5.4. `SingleChoiceQuestion`

<sub>📄 [oes/model/question/SingleChoiceQuestion.java](../../src/main/java/com/group2/oes/model/question/SingleChoiceQuestion.java)</sub>

```java
// oes/model/question/SingleChoiceQuestion.java
super(questionId, subject, content, chapter, difficulty, defaultPoint);
ValidationException.require(options != null && options.size() >= 2,
        "OPTIONS_TOO_FEW", "Câu trắc nghiệm cần ít nhất 2 phương án.");
long correct = options.stream().filter(AnswerOption::isCorrect).count();
ValidationException.require(correct == 1,
        "SINGLE_NEEDS_ONE_CORRECT", "Câu một đáp án phải có đúng 1 phương án đúng, hiện có " + correct + ".");
this.options = List.copyOf(options);
```

```java
@Override
public boolean isCorrect(String response) {
    if (isBlank(response)) {
        return false;
    }
    try {
        int chosen = Integer.parseInt(response.strip());
        return options.stream().anyMatch(o -> o.isCorrect() && o.getOptionId() == chosen);
    } catch (NumberFormatException e) {
        // dữ liệu hỏng thì coi như trả lời sai, không làm sập cả lượt chấm
        return false;
    }
}
```

- `correctRatio` = 1.0 nếu đúng, 0.0 nếu sai (BR-07: không có điểm âm).
- **Vì sao bắt `NumberFormatException`:** máy chấm (phần 4) chấm hàng loạt; một câu trả lời hỏng (ví dụ `"abc"`) không
  được làm hỏng cả bài. Kiểm thử `singleChoiceChỉĐúngKhiChọnĐúngPhươngÁn` gọi `isCorrect("abc")`.
- Thông điệp có số đếm thật ("hiện có 2") để giảng viên biết mình đang tick mấy ô.

### 5.5. `MultiChoiceQuestion`

Kiểm tra: ít nhất 2 phương án (`OPTIONS_TOO_FEW`), ít nhất 1 phương án đúng (`MULTI_NEEDS_CORRECT`,
"Câu nhiều đáp án phải có ít nhất 1 phương án đúng.").

`isCorrect`: tập mã đã chọn **bằng đúng** tập mã đúng (`parse(response).equals(correctIds())`) – thứ tự chọn không quan
trọng vì so sánh hai `Set`.

<sub>📄 [oes/model/question/MultiChoiceQuestion.java](../../src/main/java/com/group2/oes/model/question/MultiChoiceQuestion.java)</sub>

```java
// oes/model/question/MultiChoiceQuestion.java
/**
 * Tỷ lệ đúng = (số phương án đúng đã chọn - số phương án sai đã chọn) / tổng số phương án đúng,
 * chặn dưới ở 0. Trừ điểm phần chọn sai để thí sinh không thể chọn hết mọi phương án mà vẫn có điểm.
 */
@Override
public double correctRatio(String response) {
    Set<Integer> chosen = parse(response);
    Set<Integer> correct = correctIds();

    Set<Integer> hit = new HashSet<>(chosen);
    hit.retainAll(correct);              // giao: chọn đúng
    Set<Integer> wrong = new HashSet<>(chosen);
    wrong.removeAll(correct);            // hiệu: chọn sai

    double ratio = (double) (hit.size() - wrong.size()) / correct.size();
    return Math.max(0.0, ratio);
}
```

Ví dụ (đúng là 21, 22; sai là 23, 24 – lấy từ `Fixtures.multi`): chọn `"21,22"` → 1.0; `"21"` → 0.5; `"21,23"` → 0.0;
`"21,22,23,24"` → (2 − 2)/2 = 0.0. Đây là quyết định nghiệp vụ số 4 đã chốt trong `CLAUDE.md`.

- **Vì sao trừ phần chọn sai:** nếu chỉ tính số đúng đã chọn, thí sinh chọn hết mọi phương án sẽ luôn được trọn điểm.
- **Vì sao chia cho `correct.size()` không lo chia 0:** constructor đã bảo đảm có ít nhất 1 phương án đúng – lợi ích trực
  tiếp của kiểm tra trong constructor.
- `parse` gặp chuỗi hỏng thì trả tập rỗng (0 điểm), không ném lỗi.

### 5.6. `TrueFalseQuestion`

Chỉ có thêm `correctValue` (boolean). Không có kiểm tra riêng (mọi giá trị boolean đều hợp lệ); kiểm tra "chưa chọn
Đúng/Sai" nằm ở `QuestionFactory` (xem 5.9) vì một `boolean` nguyên thủy không thể "chưa chọn".

<sub>📄 [oes/model/question/TrueFalseQuestion.java](../../src/main/java/com/group2/oes/model/question/TrueFalseQuestion.java)</sub>

```java
// oes/model/question/TrueFalseQuestion.java
String r = response.strip();
// Boolean.parseBoolean trả false cho mọi chuỗi lạ, nên phải kiểm tra rõ ràng
if (!r.equalsIgnoreCase("true") && !r.equalsIgnoreCase("false")) {
    return false;
}
return Boolean.parseBoolean(r) == correctValue;
```

**Vì sao:** với câu có đáp án đúng là `false`, nếu dùng thẳng `Boolean.parseBoolean("có")` thì được `false` → chấm
**đúng** một câu trả lời rác. Kiểm thử `trueFalseKhôngNhậnChuỗiLạ` giữ lỗi này không quay lại.

### 5.7. `FillInBlankQuestion`

Dữ liệu: `accepted` (danh sách đáp án chấp nhận, đã chuẩn hóa), `caseSensitive`. Kiểm tra: ít nhất một đáp án không rỗng
(`FILL_NEEDS_ANSWER`, "Câu điền khuyết cần ít nhất 1 đáp án chấp nhận.").

<sub>📄 [oes/model/question/FillInBlankQuestion.java](../../src/main/java/com/group2/oes/model/question/FillInBlankQuestion.java)</sub>

```java
// oes/model/question/FillInBlankQuestion.java
this.caseSensitive = caseSensitive;
// chuẩn hóa sẵn một lần lúc tạo, lúc chấm chỉ còn phải chuẩn hóa câu trả lời
this.accepted = accepted.stream()
        .filter(s -> s != null && !s.isBlank())
        .map(a -> normalize(a, caseSensitive))
        .distinct()
        .toList();
// ...
private static String normalize(String s, boolean caseSensitive) {
    String r = Normalizer.normalize(s, Normalizer.Form.NFC).strip().replaceAll("\\s+", " ");
    return caseSensitive ? r : r.toLowerCase(Locale.ROOT);
}
```

- **NFC (Unicode Normalization Form C):** bộ gõ tiếng Việt có thể sinh "ế" thành một ký tự, hoặc "e" + dấu rời. Nhìn giống
  hệt nhau nhưng `equals` trả `false`. Chuẩn hóa NFC đưa về một dạng. Kiểm thử
  `điềnKhuyếtChuẩnHóaKhoảngTrắngHoaThườngVàDấuTiếngViệt` tạo chuỗi NFD và chứng minh vẫn chấm đúng.
- Gộp khoảng trắng: `"Kế   Thừa "` khớp `"kế thừa"`. Không bỏ dấu: `"ke thua"` vẫn sai.
- `Locale.ROOT` để `toLowerCase` không phụ thuộc ngôn ngữ máy (ví dụ máy cài tiếng Thổ Nhĩ Kỳ đổi "I" thành "ı").
- **Hệ quả:** khi sửa câu, ô đáp án hiện dạng đã chuẩn hóa (chữ thường) – biểu mẫu có dòng ghi chú về điều này.
- `getOptions()` không ghi đè → trả `List.of()`; câu điền khuyết không có phương án để chọn.

### 5.8. `QuestionData` (record)

Gói **dữ liệu thô** để dựng một câu hỏi khi chưa biết lớp con: 6 trường chung + `options` (trắc nghiệm),
`correctValue` (đúng/sai), `acceptedAnswers` + `caseSensitive` (điền khuyết). Trường không dùng thì `null`.

<sub>📄 [oes/factory/QuestionData.java](../../src/main/java/com/group2/oes/factory/QuestionData.java)</sub>

```java
// oes/factory/QuestionData.java
public static QuestionData basic(Subject subject, String content, int chapter,
                                 Difficulty difficulty, double point) {
    return new QuestionData(0, subject, content, chapter, difficulty, point,
            null, null, null, false);
}

public QuestionData withOptions(List<AnswerOption> opts) {
    return new QuestionData(questionId, subject, content, chapter, difficulty, defaultPoint,
            opts, correctValue, acceptedAnswers, caseSensitive);
}
```

- **Vì sao record + các hàm `with...`:** record bất biến, nên "thêm phương án" là tạo bản mới. Cách viết đọc như câu:
  `QuestionData.basic(...).withOptions(...).withId(id)`.
- **Vì sao `correctValue` kiểu `Boolean` (lớp bọc) chứ không phải `boolean`:** để biểu diễn được "chưa chọn" (`null`).
  `QuestionFactory` dựa vào `null` để báo `TF_NEEDS_VALUE`.
- `QuestionData.from(Question q)` – điền sẵn biểu mẫu sửa (FR-08). Phương án được chép thành **phương án mới chưa có mã**
  (`AnswerOption.of(o.getContent(), o.isCorrect())`) vì khi lưu, DAO xóa toàn bộ phương án cũ và chèn lại; nếu giữ đối
  tượng cũ (đã có mã) thì `assignId` sẽ ném lỗi.

### 5.9. `QuestionFactory` – mẫu Factory

<sub>📄 [oes/factory/QuestionFactory.java](../../src/main/java/com/group2/oes/factory/QuestionFactory.java)</sub>

```java
// oes/factory/QuestionFactory.java
public static Question create(QuestionType type, QuestionData d) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(d, "data");

    // switch trên enum: thiếu một nhánh thì trình biên dịch báo lỗi, không bị bỏ sót
    return switch (type) {
        case SINGLE_CHOICE -> new SingleChoiceQuestion(d.questionId(), d.subject(), d.content(),
                d.chapter(), d.difficulty(), d.defaultPoint(), d.options());

        case MULTI_CHOICE -> new MultiChoiceQuestion(d.questionId(), d.subject(), d.content(),
                d.chapter(), d.difficulty(), d.defaultPoint(), d.options());

        case TRUE_FALSE -> {
            ValidationException.require(d.correctValue() != null,
                    "TF_NEEDS_VALUE", "Câu đúng/sai chưa chọn đáp án đúng.");
            yield new TrueFalseQuestion(d.questionId(), d.subject(), d.content(),
                    d.chapter(), d.difficulty(), d.defaultPoint(), d.correctValue());
        }

        case FILL_IN_BLANK -> new FillInBlankQuestion(d.questionId(), d.subject(), d.content(),
                d.chapter(), d.difficulty(), d.defaultPoint(),
                d.acceptedAnswers(), d.caseSensitive());
    };
}
```

- **Vấn đề Factory giải quyết:** `new` đòi biết lớp cụ thể lúc viết mã, nhưng loại câu hỏi chỉ biết **lúc chạy** – từ ô
  chọn trên biểu mẫu, chữ đầu dòng trong tệp, cột `type` trong CSDL.
- **Được gọi từ:** `QuestionBankService.addQuestion`, `QuestionBankService.updateQuestion` (tầng service) và
  `JdbcQuestionDao.load` (tầng DAO, khi đọc CSDL). Javadoc của lớp ghi: "Nơi duy nhất trong chương trình biết loại câu
  hỏi nào ứng với lớp con nào."
- **Nếu không có Factory:** mỗi nơi trên tự viết một chuỗi `if (type == ...) new ...`. Thêm loại câu hỏi mới phải đi tìm
  sửa từng chỗ; quên một chỗ thì, ví dụ, lưu được nhưng đọc lên bị lỗi.
- Lớp `final` + constructor `private`: chỉ có hàm tĩnh, không ai tạo đối tượng hay kế thừa.
- **Nói cho đúng tên:** đây là *Simple Factory* (một hàm tĩnh chọn lớp con). Factory Method của GoF thì để lớp con ghi đè
  hàm tạo đối tượng – dự án không dùng dạng đó. `PHAN-CONG.md` gọi chung là "Factory".

### 5.10. `Difficulty`, chương, `MultiChoicePolicy`, `Criteria`

- `Difficulty`: `EASY("Dễ")`, `MEDIUM("Trung bình")`, `HARD("Khó")`. Trùng cột `difficulty ENUM('EASY','MEDIUM','HARD')`.
- **Chương** chỉ là một số nguyên `chapter` (> 0) trong `Question`, không có lớp `Chapter` riêng: chương không có thuộc
  tính nào khác ngoài số thứ tự, tạo lớp riêng là trừu tượng thừa. Danh sách chương trên giao diện lấy từ chính các câu
  hỏi đang có (`QuestionBankController.load`, `ExamBuilderController.buildMatrix`).
- `MultiChoicePolicy`: `ALL_OR_NOTHING("Toàn phần: đúng hết mới có điểm")`, `PARTIAL("Theo tỷ lệ: đúng phần nào được phần đó")`.
  Javadoc: "Cách chấm câu nhiều đáp án, cấu hình theo từng đề thi." Là thuộc tính của `Exam`, lưu ở cột `exam.multi_policy`.
- `Criteria` – record tiêu chí lọc; **trường nào `null` thì không lọc theo trường đó**:

<sub>📄 [oes/model/Criteria.java](../../src/main/java/com/group2/oes/model/Criteria.java)</sub>

```java
// oes/model/Criteria.java
public record Criteria(Integer chapter, Difficulty difficulty, QuestionType type, String keyword) {

    public static Criteria any() {
        return new Criteria(null, null, null, null);
    }

    public boolean matches(Question q) {
        return (chapter == null || q.getChapter() == chapter)
                && (difficulty == null || q.getDifficulty() == difficulty)
                && (type == null || q.getType() == type)
                && (keyword == null || keyword.isBlank()
                    || q.getContent().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)));
    }
}
```

`chapter` kiểu `Integer` (lớp bọc) chứ không phải `int` – để có giá trị `null` = "mọi chương". Ô chọn trên giao diện có
một mục `null` hiện là "Mọi chương" / "Mọi mức độ" / "Mọi loại".

### 5.11. `QuestionBank`

Ngân hàng câu hỏi **của một môn**, nằm trong bộ nhớ. Dữ liệu: `subject`, `questions` (`LinkedHashMap<Integer, Question>`).

- `add(q)`: chặn câu khác môn (`WRONG_SUBJECT`) và câu chưa lưu (`QUESTION_NOT_SAVED`, "Câu hỏi phải được lưu trước khi
  đưa vào ngân hàng.") – vì ngân hàng tra theo mã, mã 0 sẽ đè lên nhau.
- `get(id)`: không có thì `QUESTION_NOT_FOUND`.
- `findBy(Criteria c)`: `questions.values().stream().filter(c::matches).toList()` – FR-11.
- `findShortages(matrix)` và `pick(matrix, random)` – FR-15:

<sub>📄 [oes/model/QuestionBank.java](../../src/main/java/com/group2/oes/model/QuestionBank.java)</sub>

```java
// oes/model/QuestionBank.java
/** Liệt kê các dòng của ma trận mà ngân hàng không đủ câu. Danh sách rỗng nghĩa là đủ. */
public List<String> findShortages(ExamMatrix matrix) {
    Map<PoolKey, List<Question>> pool = groupByChapterAndLevel();
    List<String> shortages = new ArrayList<>();
    for (ExamMatrix.Entry e : matrix.getEntries()) {
        int have = pool.getOrDefault(new PoolKey(e.chapter(), e.difficulty()), List.of()).size();
        if (have < e.count()) {
            shortages.add("chương " + e.chapter() + " mức " + e.difficulty().displayName()
                    + ": cần " + e.count() + ", có " + have);
        }
    }
    return shortages;
}

public List<Question> pick(ExamMatrix matrix, Random random) {
    List<String> shortages = findShortages(matrix);
    if (!shortages.isEmpty()) {
        throw new ValidationException("BANK_SHORTAGE",
                "Ngân hàng không đủ câu hỏi: " + String.join("; ", shortages) + ".");
    }
    Map<PoolKey, List<Question>> pool = groupByChapterAndLevel();
    List<Question> picked = new ArrayList<>();
    for (ExamMatrix.Entry e : matrix.getEntries()) {
        // sao chép rồi mới trộn, không làm xáo trộn dữ liệu gốc của ngân hàng
        List<Question> candidates = new ArrayList<>(pool.get(new PoolKey(e.chapter(), e.difficulty())));
        Collections.shuffle(candidates, random);
        picked.addAll(candidates.subList(0, e.count()));
    }
    return picked;
}
```

- **Kiểm tra đủ câu trước khi sinh, báo thiếu ở đâu:** `findShortages` duyệt **mọi** dòng ma trận và gom **mọi** chỗ thiếu,
  không dừng ở chỗ đầu tiên – giảng viên sửa một lần là xong. Màn hình gọi `findShortages` khi bấm "Kiểm tra ma trận";
  `pick` gọi lại lần nữa để service không bao giờ sinh đề thiếu câu dù giao diện quên kiểm tra.
- **Không lấy trùng câu:** mỗi câu chỉ thuộc đúng một ô (chương, mức độ), và trong một ô ta lấy `subList(0, count)` của
  danh sách đã trộn – các phần tử khác nhau.
- **Vì sao truyền `Random` từ ngoài:** kiểm thử truyền `new Random(7)` hai lần và chứng minh ra cùng một bộ đề
  (`cùngSeedThìCùngBộĐề`); `SeedTool` dùng `new Random(2026)` để dữ liệu demo lần nào cũng như nhau; giao diện dùng
  `new Random()`.
- **Vì sao lọc trong bộ nhớ chứ không viết SQL cho mỗi lần lọc:** ngân hàng một môn cỡ vài trăm câu, nạp một lần khi mở
  màn hình; lọc theo từng phím gõ không phải gọi CSDL. Cùng một dữ liệu dùng được cho lọc, ma trận và chọn tay.
- `PoolKey` là record `private` lồng trong lớp – chỉ `QuestionBank` cần nó.
- Thông điệp dùng `e.difficulty().displayName()` → "Khó", không phải tên hằng `HARD` (bản đầu in tên hằng – mục 12, giới hạn 4 – đã sửa).

### 5.12. `ExamMatrix`

"Ma trận đề: cần lấy bao nhiêu câu ở mỗi cặp (chương, mức độ khó)."

<sub>📄 [oes/model/ExamMatrix.java](../../src/main/java/com/group2/oes/model/ExamMatrix.java)</sub>

```java
// oes/model/ExamMatrix.java
public record Entry(int chapter, Difficulty difficulty, int count) {
    public Entry {
        ValidationException.require(count > 0, "MATRIX_COUNT", "Số câu trong ma trận phải lớn hơn 0.");
        Objects.requireNonNull(difficulty);
    }
}

private final List<Entry> entries = new ArrayList<>();

public ExamMatrix add(int chapter, Difficulty difficulty, int count) {
    boolean duplicate = entries.stream()
            .anyMatch(e -> e.chapter() == chapter && e.difficulty() == difficulty);
    ValidationException.require(!duplicate, "MATRIX_DUPLICATE",
            "Ma trận đã có dòng cho chương " + chapter + ", mức " + difficulty.displayName() + ".");
    entries.add(new Entry(chapter, difficulty, count));
    return this;
}
```

- `add` trả `this` để viết nối chuỗi: `new ExamMatrix().add(1, Difficulty.EASY, 3).add(...)` (xem `SeedTool.seed`).
- **Chặn trùng dòng:** hai dòng cùng (chương 1, EASY) sẽ khiến `pick` lấy hai lần từ cùng một nhóm → có thể trùng câu.
- `ExamMatrix` là lớp thường (không phải record) vì được dựng dần từng dòng; `getEntries()` trả bản chỉ đọc.
- `totalRequired()` tổng số câu; `check(bank)` = `bank.findShortages(this).isEmpty()`.
- Màn hình chỉ thêm dòng cho ô có số > 0 (`ExamBuilderController.readMatrix`), nên `MATRIX_COUNT` không xảy ra từ giao diện.

### 5.13. `ExamItem`

"Một câu hỏi khi đã nằm trong một đề cụ thể. Thứ tự và điểm thuộc về đề, không thuộc về câu hỏi gốc, nên cùng một
Question có thể mang điểm khác nhau ở hai đề khác nhau."

- Dữ liệu: `order` (≥ 1, `ITEM_ORDER`), `point` (> 0, `ITEM_POINT`, "Điểm của câu hỏi trong đề phải lớn hơn 0."), `question`.
- `grade(response, strategy)` = `strategy.grade(question, response, point)` – điểm tối đa lấy **từ đề**, không phải
  `defaultPoint`. `defaultPoint` của câu hỏi chỉ là gợi ý khi thêm câu vào đề.
- **Vì sao tách khỏi `Question`:** nếu đặt `point` trong `Question` thì cùng một câu không thể 1 điểm ở đề giữa kỳ và 2
  điểm ở đề cuối kỳ. Đây là quan hệ **hợp thành (composition)**: `Exam` chứa các `ExamItem`, mỗi `ExamItem` tham chiếu một
  `Question`.

### 5.14. `Exam`

Dữ liệu: `examId`, `title`, `subject`, `version`, `multiChoicePolicy`, `items`, `questionIds` (chặn trùng),
`strategies` (`EnumMap<QuestionType, GradingStrategy>`), `locked`.

**Chọn chiến lược chấm theo cấu hình đề (nối sang phần 4):**

<sub>📄 [oes/model/Exam.java](../../src/main/java/com/group2/oes/model/Exam.java)</sub>

```java
// oes/model/Exam.java
// Các chiến lược không giữ trạng thái nên dùng chung một thể hiện cho cả chương trình.
private static final GradingStrategy EXACT = new ExactMatchGrading();
private static final GradingStrategy PARTIAL = new PartialMatchGrading();
private static final GradingStrategy TEXT = new TextMatchGrading();
// ... trong constructor:
// Mỗi loại câu hỏi ứng với một chiến lược. Riêng câu nhiều đáp án tùy cấu hình của đề.
strategies.put(QuestionType.SINGLE_CHOICE, EXACT);
strategies.put(QuestionType.TRUE_FALSE, EXACT);
strategies.put(QuestionType.FILL_IN_BLANK, TEXT);
strategies.put(QuestionType.MULTI_CHOICE,
        multiChoicePolicy == MultiChoicePolicy.PARTIAL ? PARTIAL : EXACT);
// ...
public GradingStrategy strategyFor(Question q) {
    return strategies.get(q.getType());
}
```

`ALL_OR_NOTHING` → `ExactMatchGrading` (dùng `isCorrect`: đúng hết mới có điểm). `PARTIAL` → `PartialMatchGrading` (dùng
`correctRatio`, làm tròn 2 chữ số). Phần 4 (`SubmissionGrader`) chỉ hỏi `exam.strategyFor(...)`, không biết cấu hình.

**Thêm câu, chặn trùng, khóa đề:**

```java
public ExamItem addItem(Question q, double point) {
    ensureEditable();
    ValidationException.require(q.getSubject().equals(subject), "WRONG_SUBJECT",
            "Câu hỏi #" + q.getQuestionId() + " không thuộc môn " + subject.getCode() + ".");
    ValidationException.require(questionIds.add(q.getQuestionId()), "DUPLICATE_QUESTION",
            "Câu hỏi #" + q.getQuestionId() + " đã có trong đề.");
    ExamItem item = new ExamItem(items.size() + 1, point, q);
    items.add(item);
    return item;
}

/** Dùng khi DAO dựng lại đề từ CSDL: giữ nguyên thứ tự đã lưu, bỏ qua kiểm tra khóa. */
public void restoreItem(ExamItem item) {
    questionIds.add(item.getQuestion().getQuestionId());
    items.add(item);
    items.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
}

/** BR-11: đề phải có câu hỏi và tổng điểm lớn hơn 0 mới được dùng cho ca thi. */
public void validateForUse() {
    ValidationException.require(!items.isEmpty(), "EXAM_EMPTY", "Đề thi chưa có câu hỏi nào. Hãy thêm câu hỏi vào đề rồi mới mở ca thi.");
    ValidationException.require(getTotalPoint() > 0, "EXAM_ZERO_POINT", "Tổng điểm của đề phải lớn hơn 0.");
}

/** BR-09: đề đã dùng cho ca thi thì khóa lại, muốn sửa phải tạo phiên bản mới. */
public void lock() {
    validateForUse();
    this.locked = true;
}

public Exam newVersion() {
    Exam copy = new Exam(0, title, subject, version + 1, multiChoicePolicy, false);
    for (ExamItem it : items) {
        copy.addItem(it.getQuestion(), it.getPoint());
    }
    return copy;
}
```

- `questionIds.add(...)` trả `false` nếu mã đã có – vừa kiểm tra vừa ghi nhận trong một lệnh, O(1).
- `ensureEditable()` ném `EXAM_LOCKED`: "Đề \"<tên>\" đã được dùng cho ca thi nên không sửa được. Hãy tạo phiên bản mới."
- **Vì sao có `restoreItem` riêng:** DAO dựng lại một đề **đã khóa** từ CSDL; nếu dùng `addItem` thì `ensureEditable`
  ném `EXAM_LOCKED` ngay. `restoreItem` cũng giữ đúng `order` đã lưu thay vì đánh số lại.
- `getTotalPoint()` = tổng điểm các `ExamItem` – BR-11 "tổng điểm đề bằng tổng điểm các câu" đúng theo cách tính, không
  có trường tổng điểm riêng có thể lệch.
- **`newVersion()`** tạo bản chép **chưa khóa**, mã 0 (chưa lưu), phiên bản +1, cùng tên, cùng cấu hình, cùng câu và điểm.
  Bản cũ giữ nguyên (`đềĐãKhóaPhảiTạoPhiênBảnMới`: v2 tổng 6.0, bản cũ vẫn 5.0) – các ca thi cũ và bài làm cũ vẫn trỏ
  đúng đề cũ.
- **Vì sao khóa:** bài làm lưu câu trả lời theo số thứ tự câu và mã phương án. Sửa đề sau khi thi làm điểm cũ đổi theo.
  `PLAN.md` ghi rõ BR-09: "Cài đặt hiện tại khóa đề ngay khi mở ca, chặt hơn M1 – giữ nguyên."

### 5.15. `QuestionBankService`

Nhận `QuestionDao` qua constructor. Mọi hàm bắt đầu bằng `Permissions.requireManage(...)`.

<sub>📄 [oes/service/QuestionBankService.java](../../src/main/java/com/group2/oes/service/QuestionBankService.java)</sub>

```java
// oes/service/QuestionBankService.java
public Question addQuestion(User actor, QuestionType type, QuestionData data) {
    Permissions.requireManage(actor, data.subject());
    Question q = QuestionFactory.create(type, data);   // ném ValidationException nếu dữ liệu sai
    return questionDao.insert(q);
}
```

**Sửa câu hỏi (FR-08):**

```java
public Question updateQuestion(User actor, QuestionType type, QuestionData data) {
    Question original = questionDao.findById(data.questionId())
            .orElseThrow(() -> new ValidationException("QUESTION_NOT_FOUND",
                    "Không tìm thấy câu hỏi #" + data.questionId() + "."));
    Permissions.requireManage(actor, original.getSubject());
    ValidationException.require(original.getSubject().equals(data.subject()), "SUBJECT_CHANGED",
            "Không chuyển câu hỏi sang môn khác được. Hãy tạo câu hỏi mới ở môn kia.");
    if (questionDao.isUsedInLockedExam(original.getQuestionId())) {
        throw new ValidationException("QUESTION_IN_USE", "Câu hỏi #" + original.getQuestionId()
                + " đã có trong đề được dùng cho ca thi, không sửa được. Hãy tạo câu hỏi mới.");
    }
    Question updated = QuestionFactory.create(type, data);   // ném ValidationException nếu dữ liệu sai
    questionDao.update(updated);
    AuditLog.record(actor, "UPDATE_QUESTION", String.valueOf(updated.getQuestionId()));
    return updated;
}
```

- **Quyền kiểm tra theo môn của câu gốc trong CSDL**, không theo `data.subject()` do giao diện gửi lên – giao diện có thể
  gửi sai, CSDL thì không.
- **Được đổi cả loại câu hỏi** (một đáp án → đúng/sai): vì `update` dựng lại đối tượng qua Factory và thay toàn bộ phương án.
  Kiểm thử `sửaCâuHỏiKểCảĐổiLoại`.
- **Vì sao cấm sửa câu trong đề đã khóa** (cùng điều kiện với xóa, FR-13): bài làm đã lưu tham chiếu tới mã phương án cũ;
  sửa thì kết quả thi cũ bị thay đổi về sau.

**Xóa (FR-13):** `deleteQuestion` kiểm tra quyền, rồi hai lớp kiểm tra cùng mã `QUESTION_IN_USE`:
`isUsedInLockedExam` ("... đã có trong đề được dùng cho ca thi, không thể xóa.") và `isUsedInAnyExam` ("... đang nằm trong
một đề thi, không thể xóa."), rồi `questionDao.delete`, ghi `DELETE_QUESTION`. Vì sao chặn cả đề chưa khóa: xóa thì đề âm
thầm mất một câu, và trên MySQL khóa ngoại `exam_item.question_id` cũng từ chối – trước khi có kiểm tra này người dùng
thấy nhầm thành lỗi kết nối (xem mục 12, giới hạn 3 – đã sửa).

**Nhập từ tệp (FR-12):** định dạng trong javadoc của `importFile`:

```text
LOAI | chuong | muc_do | diem | noi_dung | dap_an
SINGLE_CHOICE|1|EASY|1|Java chạy trên gì?|*JVM;CLR;V8
MULTI_CHOICE|2|MEDIUM|2|Chọn kiểu nguyên thủy|*int;String;*boolean
TRUE_FALSE|1|EASY|1|Java hỗ trợ đa kế thừa lớp|false
FILL_IN_BLANK|3|HARD|1|Từ khóa để kế thừa lớp là ...|extends
```

Phương án đúng có `*` ở đầu, các phương án/đáp án cách nhau bằng `;`. Dòng trống và dòng bắt đầu bằng `#` được bỏ qua.

```java
for (int i = 0; i < lines.size(); i++) {
    String line = lines.get(i).strip();
    if (line.isEmpty() || line.startsWith("#")) {
        continue;
    }
    int lineNo = i + 1;
    try {
        String[] f = line.split("\\|", -1);
        if (f.length != 6) {
            throw new ValidationException("CSV_COLUMNS", "cần 6 trường, có " + f.length);
        }
        QuestionType type = QuestionType.valueOf(f[0].strip());
        QuestionData data = QuestionData.basic(subject, f[4],
                Integer.parseInt(f[1].strip()), Difficulty.valueOf(f[2].strip()),
                Double.parseDouble(f[3].strip()));
        data = withAnswers(type, data, f[5]);
        addQuestion(actor, type, data);
        imported++;
    } catch (ValidationException e) {
        errors.add("Dòng " + lineNo + ": " + e.getUserMessage());
    } catch (IllegalArgumentException e) {
        // NumberFormatException là lớp con của IllegalArgumentException, nên một nhánh bắt được cả hai:
        // số chương/điểm không phải số, hoặc tên loại câu hỏi / mức độ gõ sai
        errors.add("Dòng " + lineNo + ": giá trị không hợp lệ (" + e.getMessage() + ")");
    }
}
```

- **Báo lỗi theo dòng, không dừng cả tệp:** một dòng sai không làm mất 99 dòng đúng. Số dòng `i + 1` là số dòng thật trong
  tệp (tính cả dòng chú thích), nên giảng viên mở tệp ra là tìm thấy.
- **`split("\\|", -1)`:** `|` là ký tự đặc biệt của biểu thức chính quy nên phải thoát; tham số `-1` giữ cả trường rỗng
  cuối dòng, để dòng thiếu đáp án báo đúng "cần 6 trường".
- **Thứ tự hai khối `catch`:** `ValidationException` là `RuntimeException` nhưng **không** là lớp con của
  `IllegalArgumentException`, nên hai nhánh không chồng nhau. `QuestionType.valueOf` gặp tên loại gõ sai và `Integer.parseInt`
  gặp chữ đều ném `IllegalArgumentException` (hoặc lớp con).
- **Đi qua `addQuestion`:** dòng tệp được kiểm tra **y hệt** biểu mẫu (quyền + constructor). Mỗi dòng đúng được lưu
  ngay trong giao dịch riêng của nó – cố ý không gói cả tệp vào một giao dịch.
- Lỗi đọc tệp (không có tệp) là `IOException` ném ra ngoài; controller bọc thành `UncheckedIOException`, `ErrorDialogs`
  hiện hộp thoại "Lỗi tệp".
- `withAnswers` (hàm `private static`): `TRUE_FALSE` chỉ nhận `true`/`false` (mã `TF_VALUE`, "câu đúng/sai cần đáp án true
  hoặc false"); `FILL_IN_BLANK` luôn **không phân biệt hoa thường** (`data.withAccepted(parts, false)`).
- Ghi nhật ký `IMPORT_QUESTIONS` kèm số câu và số lỗi. Kết quả là record `ImportResult(imported, errors)` – dùng chung với
  nhập người dùng của phần 1 (`DESIGN-CHANGES.md` Phase 2 mục 2).

### 5.16. `ExamService`

<sub>📄 [oes/service/ExamService.java](../../src/main/java/com/group2/oes/service/ExamService.java)</sub>

```java
// oes/service/ExamService.java
public Exam createManual(User actor, QuestionBank bank, String title, MultiChoicePolicy policy,
                         Map<Integer, Double> picks) {
    Permissions.requireManage(actor, bank.getSubject());
    ValidationException.require(!picks.isEmpty(), "EXAM_EMPTY", "Chưa chọn câu hỏi nào cho đề. Hãy thêm ít nhất một câu từ bảng ngân hàng.");
    Exam exam = Exam.draft(title, bank.getSubject(), policy);
    picks.forEach((questionId, point) -> exam.addItem(bank.get(questionId), point));
    exam.validateForUse();
    Exam saved = examDao.insert(exam);
    AuditLog.record(actor, "CREATE_EXAM", saved.getExamId() + " " + title);
    return saved;
}

/** Sinh đề tự động theo ma trận, mọi câu cùng một mức điểm. */
public Exam generate(User actor, QuestionBank bank, String title, MultiChoicePolicy policy,
                     ExamMatrix matrix, double pointEach, Random random) {
    Permissions.requireManage(actor, bank.getSubject());
    List<Question> picked = bank.pick(matrix, random);   // ném ValidationException nếu thiếu câu
    Exam exam = Exam.draft(title, bank.getSubject(), policy);
    picked.forEach(q -> exam.addItem(q, pointEach));
    exam.validateForUse();   // ma trận rỗng thì không lưu đề 0 câu (BR-11), giống createManual
    Exam saved = examDao.insert(exam);
    AuditLog.record(actor, "GENERATE_EXAM", saved.getExamId() + " " + title + " (" + picked.size() + " câu)");
    return saved;
}
```

- `picks` là `Map<mã câu, điểm>`; javadoc nhắc phía gọi dùng `LinkedHashMap` để giữ thứ tự câu trong đề
  (`ExamBuilderController.onCreate` làm đúng vậy).
- Toàn bộ đề được dựng và kiểm tra **trong bộ nhớ trước**, chỉ khi hợp lệ mới gọi `examDao.insert` – không có đề dở dang
  trong CSDL.
- `list(actor, subject)` → `examDao.findBySubject`; `newVersion(actor, exam)` → `examDao.insert(exam.newVersion())`, ghi
  `NEW_EXAM_VERSION`.
- Cả `createManual` và `generate` đều gọi `exam.validateForUse()` trước khi lưu (bản đầu `generate` thiếu dòng này –
  mục 12, giới hạn 2 – đã sửa).

### 5.17. DAO: `QuestionDao`, `ExamDao` và hai cài đặt

`QuestionDao`: `insert`, `findById`, `findByIds`, `findBySubject`, `update`, `delete`, `isUsedInLockedExam`,
`isUsedInAnyExam`.
`ExamDao`: `insert`, `findById`, `findBySubject`, `lock`.

**`JdbcQuestionDao.insert` – giao dịch:**

<sub>📄 [oes/dao/jdbc/JdbcQuestionDao.java](../../src/main/java/com/group2/oes/dao/jdbc/JdbcQuestionDao.java)</sub>

```java
// oes/dao/jdbc/JdbcQuestionDao.java
/** Câu hỏi và phương án phải được ghi cùng nhau, nên dùng giao dịch. */
@Override
public Question insert(Question q) {
    return cm.inTransaction(c -> {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO question(subject_id, type, content, chapter, difficulty, default_point,"
                        + " correct_value, case_sensitive) VALUES (?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            setQuestionColumns(ps, q);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                q.assignId(k.getInt(1));
            }
        }
        insertOptions(c, q);
        return q;
    });
}
```

- **Giao dịch (transaction):** chèn câu hỏi xong mà chèn phương án lỗi thì không được để lại "câu hỏi không có phương án".
  `ConnectionManager.inTransaction` tắt auto-commit, `commit` khi xong, `rollback` khi có bất kỳ ngoại lệ nào.
- `RETURN_GENERATED_KEYS` lấy mã `AUTO_INCREMENT` vừa sinh để `assignId`.
- `insertOptions`: câu trắc nghiệm chèn **từng phương án một** để lấy lại mã sinh tự động (chú thích: "từng phương án cần
  lấy lại mã sinh tự động, nên chèn lần lượt thay vì chạy batch"); câu điền khuyết chèn đáp án chấp nhận vào
  `answer_option` với `is_correct = true` bằng batch (không cần mã).
- `setQuestionColumns` dùng chung cho `INSERT` và `UPDATE`; cột `correct_value`/`case_sensitive` để `NULL` với loại không
  dùng (`ps.setNull(7, Types.BOOLEAN)`).

**`JdbcQuestionDao.update` – sửa trong giao dịch (FR-08):** ba câu lệnh trong một `cm.inTransaction`:
`UPDATE question SET ... WHERE id = ?` → `DELETE FROM answer_option WHERE question_id = ?` → `insertOptions` chèn lại.
**Vì sao xóa rồi chèn lại thay vì so khớp từng phương án:** đơn giản, đúng với mọi trường hợp (thêm/bớt phương án, đổi
loại câu). Nhược điểm – phương án mới có mã mới – không gây hại vì câu đã dùng trong đề khóa thì không cho sửa.

**`JdbcQuestionDao.load` – đọc bằng hai truy vấn, dựng lại qua Factory:**

```java
/**
 * Đọc câu hỏi bằng hai truy vấn: một cho bảng question, một cho toàn bộ phương án liên quan.
 * Tránh kiểu "mỗi câu hỏi một truy vấn phương án", với 200 câu sẽ thành 201 lượt gọi CSDL.
 */
// ...
QuestionData data = QuestionData.basic(r.subject, r.content, r.chapter, r.difficulty, r.point)
        .withId(r.id);
data = switch (r.type) {
    case SINGLE_CHOICE, MULTI_CHOICE -> data.withOptions(opts);
    case TRUE_FALSE -> data.withCorrectValue(r.correctValue);
    case FILL_IN_BLANK -> data.withAccepted(
            opts.stream().map(AnswerOption::getContent).toList(), r.caseSensitive);
};
result.put(r.id, QuestionFactory.create(r.type, data));
```

- Tránh lỗi "N+1 truy vấn": phương án của mọi câu lấy bằng **một** câu `... WHERE question_id IN (?,?,...)`.
- `findByIds` tạo chuỗi `?,?,?` theo số phần tử – vẫn là tham số của `PreparedStatement`, **không** nối giá trị vào SQL.
- Dữ liệu đọc từ CSDL cũng đi qua constructor (qua Factory) nên cũng được kiểm tra.
- Record `QuestionRow`: "Một dòng của bảng question, đọc ra trước khi biết sẽ dựng lớp con nào." `rs.getBoolean` đọc
  `NULL` thành `false` – chỉ dùng khi đúng loại.

**`isUsedInLockedExam`:** `SELECT 1 FROM exam_item ei JOIN exam e ON e.id = ei.exam_id WHERE ei.question_id = ? AND e.locked = TRUE LIMIT 1`.
**`isUsedInAnyExam`:** `SELECT 1 FROM exam_item WHERE question_id = ? LIMIT 1`.

**`JdbcExamDao`:** `insert` trong giao dịch (một dòng `exam` + batch `exam_item`); `findById` đọc đề và mục đề trong một
kết nối rồi nạp mọi câu hỏi bằng **một** lần `questionDao.findByIds(...)`, ghép lại bằng `restoreItem`. Câu hỏi bị
thiếu thì ném `DataAccessException` – dữ liệu hỏng, không phải lỗi người dùng. `JdbcExamDao` nhận `QuestionDao` qua
constructor (DIP: dùng lại cách dựng câu hỏi, không viết lại SQL đọc câu hỏi).

**`MemoryQuestionDao`, `MemoryExamDao`:** lớp *package-private* trong `dao.memory`, giữ dữ liệu trong `InMemoryDatabase`,
mọi hàm `synchronized (db)`. Chúng làm đúng hợp đồng (gán mã qua `db.nextId()`, `isUsedInLockedExam` duyệt các đề
`isLocked`) để kiểm thử nghiệp vụ chạy không cần MySQL.

### 5.18. Bốn màn hình

Nguyên tắc chung (xem câu chung 3, 9 trong `PHAN-CONG.md`): controller đọc ô nhập → gọi service qua `BackgroundTasks` →
hiện kết quả/lỗi. Mở từ trang chủ: `StaffHomeController` có mục "Ngân hàng câu hỏi" (`question-bank.fxml`) và "Đề thi"
(`exam-list.fxml`).

**`QuestionBankController` (`question-bank.fxml`)** – FR-08, 11, 12, 13.
- `init`: dựng cột bảng, ô lọc chương/mức độ/loại có mục `null` ("Mọi chương"...), nạp danh sách môn
  (`subjectService.listSubjects`). Đổi môn → `load()` gọi `bankService.loadBank` trên luồng nền.
- `applyFilter()` chạy **trên luồng giao diện** vì chỉ lọc `QuestionBank` đã nạp, không gọi CSDL:
  `bank.findBy(new Criteria(chapterBox.getValue(), difficultyBox.getValue(), typeBox.getValue(), keywordField.getText()))`.
  Nhãn đếm "x / y câu".
- Nút "Thêm câu hỏi"/"Sửa" (hoặc bấm đúp dòng) → `openForm` mở hộp thoại `question-form.fxml`, truyền `this::load` để nạp
  lại sau khi lưu. "Xóa" → `ErrorDialogs.confirm` rồi `bankService.deleteQuestion`. "Nhập từ tệp…" → `FileChooser` →
  `bankService.importFile` → `showImportResult` hiện tối đa `MAX_ERRORS_SHOWN` (15) dòng lỗi. Nút "?" → `onImportHelp`
  hiện định dạng tệp.

**`QuestionFormController` (`question-form.fxml`)** – FR-09, 10.
- Chọn loại → `showPaneFor(type)` hiện một trong ba vùng: `optionsPane` (dòng phương án + ô "Đúng" + nút ✕),
  `trueFalsePane` (hai `RadioButton` cùng `ToggleGroup`), `fillPane` (mỗi dòng một đáp án + ô phân biệt hoa thường).
- Sửa: `fill(QuestionData.from(existing), existing.getType())` điền sẵn.
- `collect()` – "Đọc các ô nhập thành QuestionData. Chỉ đổi kiểu dữ liệu (chữ → số), không kiểm tra nghiệp vụ." Ô điểm
  không phải số → `POINT_INVALID` "Điểm phải là số, ví dụ 1 hoặc 0.5." (đây là đổi kiểu, quy tắc `> 0` vẫn ở `Question`).
- `onSave()` dùng dạng `BackgroundTasks.run(work, onSuccess, onError)` để tự xử lý lỗi: `ValidationException` →
  `showError` tìm ô theo **mã lỗi**:

<sub>📄 [oes/ui/controller/QuestionFormController.java](../../src/main/java/com/group2/oes/ui/controller/QuestionFormController.java)</sub>

```java
// oes/ui/controller/QuestionFormController.java
Map<String, Label> byCode = Map.ofEntries(
        Map.entry("QUESTION_EMPTY", contentError),
        Map.entry("CHAPTER_INVALID", chapterError),
        Map.entry("POINT_INVALID", pointError),
        Map.entry("OPTIONS_TOO_FEW", optionsError),
        Map.entry("OPTION_EMPTY", optionsError),
        Map.entry("SINGLE_NEEDS_ONE_CORRECT", optionsError),
        Map.entry("MULTI_NEEDS_CORRECT", optionsError),
        Map.entry("TF_NEEDS_VALUE", trueFalseError),
        Map.entry("FILL_NEEDS_ANSWER", acceptedError));
Label target = byCode.get(e.getCode());
if (target == null) {
    ErrorDialogs.show(e);
    return;
}
```

  **Vì sao dựa vào mã lỗi chứ không dò nội dung thông điệp:** sửa câu chữ thông báo không làm hỏng việc định vị ô.
  Mã không có trong bảng (ví dụ `SUBJECT_FORBIDDEN`, `QUESTION_IN_USE`) thì hiện hộp thoại.
- Biểu mẫu chỉ hiện **một lỗi mỗi lần**: constructor dừng ở điều kiện sai đầu tiên.

**`ExamListController` (`exam-list.fxml`)** – BR-09. Bảng cột Mã, Tên đề, Phiên bản, Số câu, Tổng điểm, Câu nhiều đáp án
(`displayName()` của chính sách), Trạng thái ("Đã khóa (đã mở ca)" / "Chưa dùng"). Nút "Tạo đề mới" → `exam-builder.fxml`;
"Tạo phiên bản mới" → nếu đề chưa khóa thì báo "Đề này chưa dùng cho ca thi nào nên vẫn dùng được, không cần tạo phiên bản
mới.", ngược lại gọi `examService.newVersion`; "Mở ca thi với đề này…" → `session-form.fxml` (phần 3).

**`ExamBuilderController` (`exam-builder.fxml`)** – FR-14, 15, 16. Ô tên đề, ô chọn chính sách (mặc định `PARTIAL`),
hai thẻ:
- *Chọn tay*: bảng ngân hàng (lọc theo từ khóa) → "Thêm ▶" tạo `Pick` (câu + điểm `SimpleObjectProperty<Double>`); ô
  "Điểm khi thêm" để trống thì lấy `defaultPoint`. Bảng câu đã chọn sửa điểm tại chỗ (`TextFieldTableCell` +
  `PointConverter`; gõ sai hoặc ≤ 0 thì giữ điểm cũ). `updateTotal()` cập nhật "Đã chọn N câu, tổng X điểm" ngay.
- *Theo ma trận*: `buildMatrix()` dựng lưới chương × mức độ, mỗi ô một `Spinner` (0–99) kèm nhãn "/ có N".
  "Kiểm tra ma trận" → `onCheckMatrix()`:

```java
/** FR-15: báo thiếu câu ở ô nào trước khi sinh đề. */
@FXML
private void onCheckMatrix() {
    ExamMatrix m = readMatrix();
    List<String> shortages = bank.findShortages(m);
    matrixResult.setText(m.totalRequired() == 0 ? "Hãy nhập số câu cho ít nhất một ô."
            : shortages.isEmpty() ? "Đủ câu: ma trận lấy " + m.totalRequired() + " câu."
            : "Thiếu câu: " + String.join("; ", shortages) + ".");
}
```

  Gọi thẳng model trên luồng giao diện là hợp lệ: `findShortages` chỉ tính trên `QuestionBank` đã nạp, không gọi CSDL,
  và **quy tắc nằm ở model**, controller chỉ ghép câu hiển thị.
- "Tạo đề" → `onCreate()`: thẻ đang chọn là *Chọn tay* thì gọi `examService.createManual` với `LinkedHashMap`, ngược lại
  gọi `examService.generate(..., new Random())`. Xong → `created(exam)` báo số câu, tổng điểm rồi về danh sách đề.

### 5.19. `SeedTool`

Tạo dữ liệu demo trên CSDL vừa dựng: 1 quản trị viên, 2 giảng viên, 2 môn, 2 lớp, 40 thí sinh là danh sách lớp thật D24CQCE07-B
(`seed-students.csv`, gồm cả 4 thành viên nhóm), 60 câu
hỏi môn INT1332, 1 đề 20 câu sinh theo ma trận, 1 ca thi mở ngay trong 2 giờ.

<sub>📄 [oes/tools/SeedTool.java](../../src/main/java/com/group2/oes/tools/SeedTool.java)</sub>

```java
// oes/tools/SeedTool.java
// 20 câu, mỗi câu 0,5 điểm = 10 điểm. Mỗi ô của ma trận đều nhỏ hơn số câu ngân hàng có.
QuestionBank bank = app.bankService.loadBank(viet, oop);
ExamMatrix matrix = new ExamMatrix()
        .add(1, Difficulty.EASY, 3).add(1, Difficulty.MEDIUM, 2).add(1, Difficulty.HARD, 2)
        .add(2, Difficulty.EASY, 3).add(2, Difficulty.MEDIUM, 2).add(2, Difficulty.HARD, 2)
        .add(3, Difficulty.EASY, 2).add(3, Difficulty.MEDIUM, 2).add(3, Difficulty.HARD, 2);
Exam exam = app.examService.generate(viet, bank, "Kiểm tra giữa kỳ OOP", MultiChoicePolicy.PARTIAL, matrix, 0.5,
        new Random(2026));
```

- **Mọi thao tác đi qua service** như người dùng thật ("nên dữ liệu mẫu cũng kiểm tra luôn nghiệp vụ"). Câu hỏi được nạp
  bằng chính `importFile` từ `seed-questions.txt`; có dòng lỗi thì dừng (`IllegalStateException`).
- **Vì sao chép tài nguyên ra tệp tạm:** `importFile` nhận `Path`, mà tệp nằm trong jar thì không có đường dẫn thật – đọc
  bằng `getResourceAsStream` rồi chép ra `Files.createTempFile`.
- `seed(app)` tách khỏi `main` để `SeedToolTest` chạy được với CSDL bộ nhớ.
- `main` từ chối chạy lần hai: đã có môn `INT1332` thì in hướng dẫn và thoát mã 2.
- Phân bố 60 câu trong `seed-questions.txt`: chương 1 (dễ 8, trung bình 7, khó 5), chương 2 (7, 7, 6), chương 3 (6, 7, 7);
  theo loại: 25 một đáp án, 9 nhiều đáp án, 16 đúng/sai, 10 điền khuyết.
- `sample-questions.txt` (20 dòng câu hỏi, dùng cho `ConsoleDemo` và để demo "Nhập từ tệp"): hai dòng cuối **cố ý sai** –
  một câu một đáp án có hai `*`, một câu có chương là `abc`.

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 6. Luồng chạy đầu–cuối

Ký hiệu: **[GD]** = chạy trên luồng giao diện; **[nền]** = chạy trong `BackgroundTasks` (luồng nền).
Câu SQL trích trong mục này là chuỗi trong mã Java (thường viết nối nhiều đoạn bằng `+`) đã ghép lại thành một dòng.
Lỗi đi qua `BackgroundTasks.run(page, ...)` đều tới `ErrorDialogs.show`: `ExamAppException` → hộp thoại tiêu đề
"Không thực hiện được" với `getUserMessage()`.

### Luồng A – Thêm câu hỏi một đáp án (và nhánh sửa)

0. Mở màn hình: trang chủ → "Ngân hàng câu hỏi" → `QuestionBankController.init` → [nền] `SubjectService.listSubjects` →
   chọn môn đầu tiên → `load()` → [nền] `QuestionBankService.loadBank(actor, subject)` →
   `Permissions.requireManage` → `JdbcQuestionDao.findBySubject` → hàm `load` chạy hai câu SQL (trích ở 5.17):
   `SELECT_QUESTION + " WHERE q.subject_id = ? ORDER BY q.id"`, rồi một truy vấn lấy mọi phương án `WHERE question_id IN (...)`.
   Mỗi dòng → `QuestionData` → `QuestionFactory.create` → đối tượng lớp con. Kết quả là một `QuestionBank`; bảng hiện
   qua `applyFilter()` [GD].
1. Bấm **"Thêm câu hỏi"** → `onAdd()` → `openForm(null)` → `navigator().openDialog("question-form.fxml", ...)` →
   `QuestionFormController.init(ws, subject, null, this::load)`: loại mặc định `SINGLE_CHOICE`, mức `EASY`, điểm `1`,
   4 dòng phương án trống.
2. Nhập nội dung, 4 phương án, tick "Đúng" cho **2** phương án → **"Lưu câu hỏi"** → `onSave()` [GD] → `clearErrors()` →
   `collect()`: đổi ô điểm thành số, mỗi dòng thành `AnswerOption.of(text, checkbox)`, gói thành `QuestionData`.
   - Nhánh lỗi sớm: còn một dòng phương án trống → constructor `AnswerOption` ném `OPTION_EMPTY` ngay trong `collect()` →
     hiện "Nội dung phương án không được để trống." dưới ô phương án. (Phải xóa dòng thừa bằng nút ✕.)
3. [nền] `QuestionBankService.addQuestion(actor, SINGLE_CHOICE, data)` → `Permissions.requireManage(actor, data.subject())`
   → `QuestionFactory.create` → `new SingleChoiceQuestion(...)` → `super(...)` kiểm tra chung → kiểm tra riêng ném
   `ValidationException("SINGLE_NEEDS_ONE_CORRECT", ...)`.
4. Lỗi quay về [GD] → nhánh `onError` của `onSave` → `showError(v)` → mã `SINGLE_NEEDS_ONE_CORRECT` ứng với
   `optionsError` → hiện **"Câu một đáp án phải có đúng 1 phương án đúng, hiện có 2."** ngay dưới danh sách phương án.
   Không có gì được ghi xuống CSDL.
5. Bỏ tick một ô → Lưu lại → constructor qua được → `JdbcQuestionDao.insert` trong `cm.inTransaction`:
   ```sql
   INSERT INTO question(subject_id, type, content, chapter, difficulty, default_point, correct_value, case_sensitive) VALUES (?,?,?,?,?,?,?,?)
   -- rồi với từng phương án:
   INSERT INTO answer_option(question_id, opt_order, content, is_correct) VALUES (?,?,?,?)
   ```
   Mã sinh tự động được gán vào câu hỏi và từng phương án (`assignId`). Commit.
6. Thành công → `close()` đóng hộp thoại → `onSaved.run()` = `QuestionBankController.load()` nạp lại ngân hàng.
   - Nhánh lỗi quyền (không phụ trách môn): `AccessDeniedException("SUBJECT_FORBIDDEN")` không có trong bảng mã của
     `showError` → hộp thoại "Bạn không phụ trách môn INT1313." (mã môn tùy trường hợp).
   - Nhánh mất kết nối: `DataAccessException` → hộp thoại "Không kết nối được tới máy chủ dữ liệu. Vui lòng thử lại hoặc
     báo giám thị."

**Nhánh sửa:** bấm đúp một dòng hoặc "Sửa" → `onEdit()` → `openForm(q)` → `fill(QuestionData.from(existing), ...)`.
Lưu → [nền] `QuestionBankService.updateQuestion`:
1. `questionDao.findById(id)` – `SELECT_QUESTION + " WHERE q.id IN (?)"` + truy vấn phương án; không có →
   `QUESTION_NOT_FOUND`.
2. `Permissions.requireManage(actor, original.getSubject())`; khác môn → `SUBJECT_CHANGED`.
3. `questionDao.isUsedInLockedExam(id)`:
   ```sql
   SELECT 1 FROM exam_item ei JOIN exam e ON e.id = ei.exam_id WHERE ei.question_id = ? AND e.locked = TRUE LIMIT 1
   ```
   Có dòng → `QUESTION_IN_USE` → không có trong bảng mã của biểu mẫu → hộp thoại "Câu hỏi #N đã có trong đề được dùng cho
   ca thi, không sửa được. Hãy tạo câu hỏi mới."
4. `QuestionFactory.create(type, data)` (được đổi loại) → `JdbcQuestionDao.update` – **một giao dịch** gồm
   `UPDATE question SET ... WHERE id = ?`, `DELETE FROM answer_option WHERE question_id = ?`, rồi chèn lại từng phương án
   (`INSERT INTO answer_option ...` như bước 5 ở trên; giải thích ở 5.17).
5. `AuditLog.record(actor, "UPDATE_QUESTION", ...)`.

### Luồng B – Nhập câu hỏi từ tệp (FR-12)

1. Bấm **"Nhập từ tệp…"** → `QuestionBankController.onImport()` [GD] → `FileChooser` (lọc `*.txt`, `*.csv`) → chọn
   `sample-questions.txt`.
2. [nền] `QuestionBankService.importFile(actor, subject, path)` → `Permissions.requireManage` →
   `Files.readAllLines(file, StandardCharsets.UTF_8)` (tiếng Việt có dấu đọc đúng).
3. Mỗi dòng không trống, không bắt đầu bằng `#`: tách 6 trường → `QuestionType.valueOf`, `Integer.parseInt`,
   `Difficulty.valueOf`, `Double.parseDouble` → `withAnswers` → `addQuestion` (Luồng A bước 3, 5: Factory + `INSERT` trong
   giao dịch riêng của dòng đó).
4. Với `sample-questions.txt`: 18 dòng nhập được; dòng 22 (`*A;*B;C` cho câu một đáp án) →
   "Dòng 22: Câu một đáp án phải có đúng 1 phương án đúng, hiện có 2."; dòng 23 (chương `abc`) → nhánh
   `IllegalArgumentException` → "Dòng 23: giá trị không hợp lệ (For input string: "abc")".
   (Danh sách kiểm thử tay [`docs/manual-test-checklist.md`](../../docs/manual-test-checklist.md) mục 22 ghi đúng kết quả "18 câu, 2 dòng lỗi (dòng 22, 23)".)
5. `AuditLog.record(actor, "IMPORT_QUESTIONS", "<tên tệp>: 18 câu, 2 lỗi")` → trả `ImportResult`.
6. [GD] `showImportResult(r)` → hộp thoại "Kết quả nhập tệp": "Nhập được 18 dòng, 2 dòng lỗi." + từng lỗi (tối đa 15) →
   `load()` nạp lại bảng.
   - Nhánh không đọc được tệp: `IOException` → bọc `UncheckedIOException` → `ErrorDialogs.show` hiện "Lỗi tệp".

### Luồng C – Tạo đề theo ma trận (FR-15) với kiểm tra thiếu câu

1. Trang chủ → "Đề thi" → `ExamListController.init` → [nền] `ExamService.list(actor, subject)` →
   `JdbcExamDao.findBySubject`: `SELECT id FROM exam WHERE subject_id = ? ORDER BY title, version`, rồi `findById` từng đề.
2. **"Tạo đề mới"** → `onCreate()` → `ws.open("exam-builder.fxml", c -> c.init(ws, s))` →
   `ExamBuilderController.init` → [nền] `bankService.loadBank` → `filterBank()` + `buildMatrix()` (mỗi ô có nhãn
   "/ có N", N tính bằng `bank.findBy(new Criteria(ch, level, null, null)).size()`).
3. Thẻ **"Theo ma trận"**, nhập chương 3 – Khó = 10 → **"Kiểm tra ma trận"** → `onCheckMatrix()` [GD] → `readMatrix()`
   (chỉ ô > 0 thành dòng `ExamMatrix.add`) → `bank.findShortages(m)` → nhãn hiện
   **"Thiếu câu: chương 3 mức Khó: cần 10, có 7."** (dữ liệu mẫu có 7 câu khó chương 3). Không gọi CSDL.
4. Sửa thành 2 → kiểm tra lại → "Đủ câu: ma trận lấy 2 câu." → nhập tên đề, chọn chính sách → **"Tạo đề"** → `onCreate()`
   → `parsePoint(pointEachField, 1)` → [nền] `ExamService.generate(actor, bank, title, policy, m, each, new Random())`:
   - `Permissions.requireManage`;
   - `bank.pick(matrix, random)` – kiểm tra thiếu câu **lần nữa** (`BANK_SHORTAGE` nếu thiếu), trộn, lấy đủ số câu;
   - `Exam.draft(title, subject, policy)` – tên trống → `EXAM_TITLE` "Tên đề thi không được để trống.";
   - `exam.addItem(q, pointEach)` cho từng câu – điểm ≤ 0 → `ITEM_POINT`;
   - `exam.validateForUse()` – đề 0 câu → `EXAM_EMPTY`;
   - `JdbcExamDao.insert` trong một giao dịch:
     ```sql
     INSERT INTO exam(title, subject_id, version, multi_policy, locked) VALUES (?,?,?,?,?)
     INSERT INTO exam_item(exam_id, item_order, question_id, point) VALUES (?,?,?,?)   -- batch, mỗi câu một dòng
     ```
   - `AuditLog.record(actor, "GENERATE_EXAM", ...)`.
5. [GD] `created(exam)` → "Đã tạo đề "<tên>": 2 câu, 2 điểm." → quay về `exam-list.fxml`, đề mới ở trạng thái "Chưa dùng".

*Biến thể "Chọn tay":* thêm câu vào bảng bên phải, sửa điểm tại chỗ → "Tạo đề" → `ExamService.createManual` với
`LinkedHashMap<mã câu, điểm>`: chưa chọn câu → `EXAM_EMPTY` "Chưa chọn câu hỏi nào cho đề. Hãy thêm ít nhất một câu từ
bảng ngân hàng."; `bank.get(id)`, `addItem`, `validateForUse()` rồi mới `insert`, ghi `CREATE_EXAM`.

### Luồng D – Mở ca thi khóa đề; tạo phiên bản mới (BR-09)

1. Ở danh sách đề, chọn đề → **"Mở ca thi với đề này…"** → `ExamListController.onOpenSession()` →
   `session-form.fxml` (`SessionFormController`, **phần 3**) → [nền] `ExamSessionService.createSession(...)`.
2. Trong phần 3: `new ExamSession(0, exam, window, policy)` gọi `exam.validateForUse()` (BR-11: đề rỗng → `EXAM_EMPTY`);
   lưu ca thi; rồi:
   <sub>📄 [oes/service/ExamSessionService.java](../../src/main/java/com/group2/oes/service/ExamSessionService.java)</sub>

   ```java
   // oes/service/ExamSessionService.java (phần 3)
   // BR-09: từ lúc có ca thi, đề không được sửa nữa
   if (!exam.isLocked()) {
       exam.lock();
       examDao.lock(exam.getExamId());
   }
   ```
   `Exam.lock()` (phần 2) gọi `validateForUse()` rồi đặt `locked = true`; `JdbcExamDao.lock` (phần 2):
   ```sql
   UPDATE exam SET locked = TRUE WHERE id = ?
   ```
3. Từ đây: `Exam.addItem` ném `EXAM_LOCKED`; `isUsedInLockedExam` trả `true` cho mọi câu của đề → không sửa/xóa được
   các câu đó (`QUESTION_IN_USE`). Danh sách đề hiện "Đã khóa (đã mở ca)".
4. Muốn đổi đề: chọn đề đã khóa → **"Tạo phiên bản mới"** → `ExamListController.onNewVersion()` → (đề chưa khóa thì chỉ
   báo, không gọi service) → [nền] `ExamService.newVersion(actor, exam)` → `Permissions.requireManage` →
   `exam.newVersion()` (bản chép chưa khóa, phiên bản + 1) → `JdbcExamDao.insert` (hai câu `INSERT` như Luồng C) → ghi
   `NEW_EXAM_VERSION` → hộp thoại "Đã tạo "<tên>" phiên bản 2." → nạp lại bảng.

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 7. CSDL của phần này

Trích [`db/schema.sql`](../../db/schema.sql). Chú thích trong tệp: "Bốn loại câu hỏi dùng chung một bảng (single-table). Cột nào không thuộc
loại câu hỏi thì để NULL."

```sql
CREATE TABLE question (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    subject_id     INT NOT NULL,
    type           ENUM('SINGLE_CHOICE','MULTI_CHOICE','TRUE_FALSE','FILL_IN_BLANK') NOT NULL,
    content        TEXT NOT NULL,
    chapter        INT NOT NULL,
    difficulty     ENUM('EASY','MEDIUM','HARD') NOT NULL,
    default_point  DECIMAL(5,2) NOT NULL,
    correct_value  BOOLEAN,      -- chỉ dùng cho TRUE_FALSE
    case_sensitive BOOLEAN,      -- chỉ dùng cho FILL_IN_BLANK
    FOREIGN KEY (subject_id) REFERENCES subject(id),
    INDEX idx_question_pick (subject_id, chapter, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE answer_option (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    question_id  INT NOT NULL,
    opt_order    INT NOT NULL,
    content      TEXT NOT NULL,
    is_correct   BOOLEAN NOT NULL,
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    subject_id    INT NOT NULL,
    version       INT NOT NULL DEFAULT 1,
    multi_policy  ENUM('ALL_OR_NOTHING','PARTIAL') NOT NULL,
    locked        BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (subject_id) REFERENCES subject(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE exam_item (
    exam_id      INT NOT NULL,
    item_order   INT NOT NULL,
    question_id  INT NOT NULL,
    point        DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (exam_id, item_order),
    UNIQUE (exam_id, question_id),
    FOREIGN KEY (exam_id)     REFERENCES exam(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

| Điểm thiết kế | Vì sao |
|---|---|
| **Một bảng cho 4 loại câu** (single-table inheritance) | Chỉ 2 cột riêng (`correct_value`, `case_sensitive`), không đáng tách 4 bảng; đọc ngân hàng không phải `JOIN` nhiều bảng. Cột `type` cho DAO biết dựng lớp con nào (qua Factory). |
| Đáp án chấp nhận của câu điền khuyết nằm trong `answer_option`, `is_correct = TRUE` | Dùng lại bảng sẵn có, không thêm bảng. Khung nhìn `v_exam_paper` chỉ nối `answer_option` khi `q.type IN ('SINGLE_CHOICE', 'MULTI_CHOICE')`, nên đáp án điền khuyết không lộ ra máy thí sinh. |
| `answer_option ... ON DELETE CASCADE` | Xóa câu hỏi thì phương án tự xóa theo; `JdbcQuestionDao.delete` chỉ cần một câu `DELETE FROM question WHERE id = ?`. |
| `exam_item` khóa chính `(exam_id, item_order)` | Mỗi đề không có hai câu cùng số thứ tự. Câu trả lời thí sinh lưu theo `item_order`. |
| `UNIQUE (exam_id, question_id)` | Chốt chặn ở CSDL cho quy tắc "một câu không xuất hiện hai lần trong đề" – trùng với `Exam.questionIds` ở model. |
| `exam_item.question_id` **không** cascade | Không cho xóa câu hỏi đang nằm trong một đề – CSDL từ chối (xem mục 12 về thông báo). |
| `ENUM` cho `type`, `difficulty`, `multi_policy` | Giá trị khớp `name()` của enum Java; CSDL tự chặn giá trị lạ. |
| `DECIMAL(5,2)` cho điểm | CSDL lưu đúng hai chữ số thập phân (0.5, 0.25), và `SUM(ei.point)` trong `v_session_for_student` cộng chính xác. Phía Java tính bằng `double` (`Exam.getTotalPoint`) nên các chỗ ra điểm cuối đều làm tròn 2 chữ số (`PartialMatchGrading`, `SubmissionGrader`). |
| `INDEX idx_question_pick (subject_id, chapter, difficulty)` | Thiết kế cho việc chọn câu theo (môn, chương, mức độ). Mã hiện tại nạp cả ngân hàng một môn rồi gom nhóm trong Java (`groupByChapterAndLevel`), nên chỉ mục đang phục vụ `WHERE q.subject_id = ?` (cột đầu của chỉ mục). |
| `exam.version` + `locked` | BR-09: đề cũ khóa, phiên bản mới là một dòng `exam` khác. |

**Khung nhìn và quyền liên quan ([`db/security.sql`](../../db/security.sql), thuộc phần 4 quản lý, phần 2 cần hiểu):**
- `oes_staff`: `GRANT ALL PRIVILEGES` – ứng dụng giảng viên đọc/ghi bốn bảng trên.
- `oes_client`: *"Không cấp gì trên question, answer_option, exam_item, app_user: SELECT trực tiếp sẽ bị từ chối."* Máy thí
  sinh chỉ đọc `v_exam_paper` (cột `exam_id, item_order, point, question_id, type, content, option_id, opt_order,
  option_content` – không có `is_correct`), `v_session_for_student` (tính `total_point` = `SUM(ei.point)` và
  `question_count` từ `exam_item`), `v_review` (đáp án, chỉ khi ca đã đóng và cho xem đáp án).
- Không có trigger nào trên bốn bảng của phần 2.

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 8. Quy tắc nghiệp vụ

| Quy tắc | Nơi thực thi trong mã |
|---|---|
| Chỉ 4 loại câu hỏi khách quan (quyết định chốt số 3) | `QuestionType` 4 hằng; `QuestionFactory.create` switch phủ 4 nhánh; cột `question.type` là `ENUM` 4 giá trị |
| Câu nhiều đáp án chấm theo cấu hình của đề: `ALL_OR_NOTHING` hoặc `PARTIAL` (chốt số 4, BR-08) | `Exam` constructor: `strategies.put(QuestionType.MULTI_CHOICE, multiChoicePolicy == MultiChoicePolicy.PARTIAL ? PARTIAL : EXACT)` |
| `PARTIAL` = tỷ lệ đúng trừ phần chọn sai, chặn dưới 0 (chốt số 4) | `MultiChoiceQuestion.correctRatio`: `(hit.size() - wrong.size()) / correct.size()`, `Math.max(0.0, ratio)` |
| Câu một đáp án có đúng 1 phương án đúng, không điểm âm (BR-07) | `SingleChoiceQuestion` constructor (`SINGLE_NEEDS_ONE_CORRECT`); `correctRatio` chỉ 0 hoặc 1 |
| Câu trắc nghiệm ≥ 2 phương án; câu nhiều đáp án ≥ 1 đúng | `OPTIONS_TOO_FEW`, `MULTI_NEEDS_CORRECT` trong constructor hai lớp |
| Câu đúng/sai phải chọn đáp án | `QuestionFactory.create`, nhánh `TRUE_FALSE`: `TF_NEEDS_VALUE` |
| Câu điền khuyết ≥ 1 đáp án chấp nhận | `FillInBlankQuestion` constructor: `FILL_NEEDS_ANSWER` |
| Nội dung không rỗng, chương > 0, điểm > 0 (FR-10) | `Question` constructor: `QUESTION_EMPTY`, `CHAPTER_INVALID`, `POINT_INVALID` |
| Không xóa câu đã dùng cho ca thi (FR-13); không sửa (FR-08) | `QuestionBankService.deleteQuestion` (+ `isUsedInAnyExam`: không xóa cả câu trong đề chưa khóa), `updateQuestion` + `QuestionDao.isUsedInLockedExam` → `QUESTION_IN_USE` |
| Không chuyển câu hỏi sang môn khác khi sửa | `QuestionBankService.updateQuestion`: `SUBJECT_CHANGED` |
| Giảng viên chỉ thao tác môn mình phụ trách (FR-05) | Mọi hàm public của `QuestionBankService`, `ExamService` gọi `Permissions.requireManage` → `SUBJECT_FORBIDDEN` |
| Câu trong đề phải cùng môn với đề, không trùng | `Exam.addItem`: `WRONG_SUBJECT`, `DUPLICATE_QUESTION`; CSDL: `UNIQUE (exam_id, question_id)` |
| Tổng điểm đề = tổng điểm các câu và > 0 (BR-11) | `Exam.getTotalPoint` (tính từ `items`); `Exam.validateForUse`: `EXAM_EMPTY`, `EXAM_ZERO_POINT`; `ExamItem`: `ITEM_POINT` |
| Đề dùng cho ca thi thì khóa, muốn đổi tạo phiên bản mới (BR-09) | `Exam.lock`, `Exam.ensureEditable` (`EXAM_LOCKED`), `Exam.newVersion`; `ExamDao.lock`; gọi từ `ExamSessionService.createSession` (phần 3) |
| Sinh đề: ngân hàng phải đủ câu cho mọi ô ma trận (FR-15) | `QuestionBank.findShortages`; `QuestionBank.pick` ném `BANK_SHORTAGE` |
| Ma trận không có hai dòng trùng (chương, mức), số câu > 0 | `ExamMatrix.add` (`MATRIX_DUPLICATE`), `ExamMatrix.Entry` (`MATRIX_COUNT`) |
| Nhập tệp: dòng lỗi được ghi lại, dòng đúng vẫn nhập (FR-12) | `QuestionBankService.importFile`: `try/catch` trong vòng lặp, lỗi dạng "Dòng N: ..." |

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 9. SOLID và mẫu thiết kế

| Nguyên lý / mẫu | Ở đâu | Vì sao | Nếu không làm vậy |
|---|---|---|---|
| **S** – Đơn trách nhiệm | `Question` chỉ biết dữ liệu câu và cách so khớp; `ExamItem` giữ điểm trong đề; `GradingStrategy` (phần 4) quy đổi ra điểm; `QuestionBank` lo lọc/chọn; `ExamMatrix` chỉ mô tả ma trận | Đổi công thức chấm theo tỷ lệ không đụng tới câu hỏi; đổi cách chọn câu không đụng tới đề | `Question` phình to, mỗi lần đổi chấm điểm lại sửa lớp câu hỏi |
| **O** – Mở để mở rộng, đóng để sửa đổi | Lớp con của `Question` | Thêm loại câu mới = thêm một lớp con với `isCorrect`/`correctRatio` riêng; bốn lớp cũ không sửa | Một lớp `Question` với `switch(type)` trong `isCorrect` – thêm loại là sửa lớp đang chạy đúng |
| **L** – Thay thế Liskov | Mọi chỗ nhận `Question` (`ExamItem`, `GradingStrategy.grade`, `QuestionBank`) làm việc đúng với cả 4 lớp con. `getOptions()` mặc định trả danh sách rỗng chứ không `null` | Nơi gọi không phải `instanceof` để tránh `NullPointerException` | Lớp con trả `null` hoặc ném lỗi ở `getOptions()` → nơi gọi phải biết lớp cụ thể |
| **I** – Tách interface | `QuestionDao`, `ExamDao` chỉ có ở phía giảng viên; máy thí sinh chỉ thấy `StudentExamDao` | Ứng dụng thi không có đường nào gọi `QuestionDao` để đọc đáp án | Một DAO chung → ứng dụng thi có sẵn hàm đọc câu hỏi kèm đáp án |
| **D** – Đảo phụ thuộc | `QuestionBankService(QuestionDao)`, `ExamService(ExamDao)`, `JdbcExamDao(cm, QuestionDao)` | Kiểm thử chạy với `MemoryQuestionDao`; `AppContext` chọn cài đặt | Service `new JdbcQuestionDao` → không kiểm thử được khi không có MySQL |
| **Factory** (Simple Factory) | `QuestionFactory.create` | Loại câu chỉ biết lúc chạy; một chỗ duy nhất ánh xạ loại → lớp con | Ba nơi (thêm, sửa, đọc CSDL) tự viết `if/else`, dễ lệch nhau |
| **Strategy** (phía chọn) | `Exam.strategies`, `Exam.strategyFor` | Chính sách chấm là cấu hình của đề; cùng một câu nhiều đáp án, hai đề chấm hai kiểu | `if (policy == PARTIAL)` rải trong mã chấm |
| **Value Object / record** | `Criteria`, `QuestionData`, `ExamMatrix.Entry` | Bất biến, so sánh theo giá trị, kiểm tra trong compact constructor | Lớp có setter → đối tượng đổi giữa chừng khi đang dùng |
| **Composition** | `Exam` ◆— `ExamItem` —→ `Question` | Điểm thuộc về đề; một câu dùng trong nhiều đề | Điểm nằm trong `Question` → không đổi được điểm theo đề |

**Ghi chú trung thực về `instanceof`:** `QuestionData.from` và `JdbcQuestionDao.setQuestionColumns` dùng
`instanceof TrueFalseQuestion`/`FillInBlankQuestion`. Đây là chỗ đổi đối tượng ↔ dạng lưu trữ, cần trường riêng của
từng lớp con; đưa việc này vào `Question` (ví dụ thêm một hàm "chuyển thành dòng CSDL") sẽ làm model biết về CSDL – trái quy ước "model không chứa
SQL". Đánh đổi được chấp nhận.

**Ghi chú về NFR-10 ("thêm loại câu hỏi không sửa lớp cũ"):** đúng ở tầng model (lớp con mới, không sửa 4 lớp cũ). Nhưng
các tầng khác vẫn có `switch` theo loại phải thêm nhánh (xem câu Sửa trực tiếp 1 ở mục 14). Nhờ `switch` biểu thức trên
enum không có `default`, **trình biên dịch chỉ ra mọi chỗ còn thiếu**.

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 10. Khác với thiết kế M3/M4

Lấy từ [`docs/DESIGN-CHANGES.md`](../../docs/DESIGN-CHANGES.md) (*thay đổi – lý do – ảnh hưởng*):

| Thay đổi | Lý do | Ảnh hưởng tới phần 2 |
|---|---|---|
| **Phase 1 mục 3** – Máy thí sinh có mô hình riêng (`client`: `PaperItem`, `PaperOption` không có cờ đúng/sai) | NFR-04: "Lớp `Question` có `isCorrect`, `AnswerOption` có `isCorrect` – chỉ cần đối tượng tới được máy thí sinh là đáp án đã tới." | `Question`, `AnswerOption`, `Exam` chỉ còn tồn tại phía giảng viên. Câu trả lời vẫn là mã phương án theo số thứ tự câu trong đề gốc. |
| **Phase 1 mục 4** – Tách DAO theo người dùng (ISP) | "Ứng dụng thi chỉ thấy `StudentExamDao` và `StudentLoginDao`, nên không có đường nào để gọi `QuestionDao`..." | `QuestionDao`, `ExamDao` chỉ lắp trong `AppContext`. |
| **Phase 2 mục 2** – `ImportResult` thành lớp dùng chung | Nhập người dùng (FR-06) trả cùng kiểu kết quả như nhập câu hỏi (FR-12) | Trước là record lồng `QuestionBankService.ImportResult`; nay là `service.ImportResult`. |
| **Phase 2 mục 5** – `QuestionDao.update` | FR-08 (M4 chưa có sửa câu hỏi): "ghi lại câu hỏi và thay toàn bộ phương án trong một giao dịch (FR-08); cấm sửa câu đã nằm trong đề đã khóa, cùng điều kiện với xóa" | Thêm `QuestionBankService.updateQuestion`, kiểm thử `sửaCâuHỏiThayToànBộPhươngÁnTrongGiaoDịch`. |
| **Phase 2 mục 5** – `ExamService.list`, `ExamService.newVersion` (BR-09) | Màn hình danh sách đề và nút "Tạo phiên bản mới" | Thêm `ExamDao.findBySubject`. |
| **Phase 4–5 mục 2** – Biểu mẫu câu hỏi không tự kiểm tra nghiệp vụ | "Controller chỉ gom ô nhập thành `QuestionData` (thêm `QuestionData.from(Question)` để điền sẵn khi sửa)... Lỗi hiện ngay dưới ô liên quan nhờ mã lỗi ổn định của `ValidationException`" | `QuestionFormController.showError` ánh xạ mã → ô. Đáp án điền khuyết hiện dạng đã chuẩn hóa khi sửa. |
| **Phase 4–5 mục 3** – `AuthService.createAccount` dựng lớp con theo vai trò "(switch trên enum, như `QuestionFactory`)" | Controller không được tự `new` lớp con | Phần 1 dùng lại đúng cách làm của `QuestionFactory`. |
| **Phase 4–5 mục 4** – `Difficulty`, `QuestionType`, `MultiChoicePolicy` có `displayName()` | Giao diện tiếng Việt | "không đổi `toString()` để các thông điệp và nhật ký cũ không đổi theo" – vì vậy thông điệp thiếu câu vẫn in `HARD`. |
| **Phase 8** – `SeedTool` trong [`src/main`](../../src/main) (gói `tools`) | "để có sẵn trong `oes-staff.jar`; chỉ chạy trên CSDL chưa có dữ liệu mẫu" | `SeedToolTest` kiểm tra tệp câu hỏi mẫu không có dòng lỗi. |
| **BR-09 trong `PLAN.md`** – khóa đề ngay khi **mở ca** | "chặt hơn M1 – giữ nguyên" (M1: đề đã dùng cho ca thi) | `ExamSessionService.createSession` khóa đề ngay khi tạo ca, kể cả ca chưa tới giờ mở. |

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 11. Kiểm thử

Chạy: `./mvnw test` (toàn bộ) hoặc từng lớp, ví dụ `./mvnw test -Dtest=QuestionTest`,
`./mvnw test -Dtest=ExamAndBankTest`, `./mvnw test -Dtest=SeedToolTest`, `./mvnw test -Dtest=StaffServicesTest`.
Các kiểm thử JDBC bị bỏ qua nếu chưa đặt `OES_TEST_DB` (lệnh trong `CLAUDE.md`).

**Tệp của phần 2:**

| Lớp test | Phương thức | Chứng minh |
|---|---|---|
| `QuestionTest` | `singleChoiceChỉĐúngKhiChọnĐúngPhươngÁn` | Chọn đúng mã → đúng; khoảng trắng, chuỗi rỗng, `null`, `"abc"` không làm sập |
| | `multiChoiceTínhTỷLệCóTrừPhầnChọnSai` | 1.0 / 0.5 / 0.0; chọn hết được 0; thứ tự chọn không quan trọng |
| | `trueFalseKhôngNhậnChuỗiLạ` | `"FALSE"` đúng, `"có"` không bị chấm đúng |
| | `điềnKhuyếtChuẩnHóaKhoảngTrắngHoaThườngVàDấuTiếngViệt` | Chuẩn hóa NFC, khoảng trắng, hoa thường; không bỏ dấu |
| | `câuMộtĐápÁnCóHaiPhươngÁnĐúngBịTừChối` | `SINGLE_NEEDS_ONE_CORRECT` |
| | `dữLiệuChungKhôngHợpLệBịChặnỞLớpCha` | `POINT_INVALID`, `QUESTION_EMPTY` từ constructor lớp cha |
| `ExamAndBankTest` | `sinhĐềĐúngSốCâuTheoMaTrận` | Đúng số câu mỗi ô, không trùng câu |
| | `cùngSeedThìCùngBộĐề` | Cùng `Random(7)` → cùng bộ đề (truyền `Random` từ ngoài) |
| | `thiếuCâuThìBáoRõThiếuỞĐâu` | Thông điệp chứa "chương 2 mức Khó: cần 10, có 3" và "chương 3 mức Dễ: cần 1, có 0" |
| | `maTrậnKhôngChoKhaiBáoTrùngDòng` | `MATRIX_DUPLICATE` |
| | `đềKhôngNhậnCâuTrùngHoặcKhácMôn` | `DUPLICATE_QUESTION`, `WRONG_SUBJECT` |
| | `đềĐãKhóaPhảiTạoPhiênBảnMới` | `EXAM_LOCKED`; v2 = phiên bản 2, tổng 6.0; bản cũ giữ 5.0 |
| | `đềRỗngKhôngKhóaĐược` | `EXAM_EMPTY` khi `lock()` |
| `SeedToolTest` | `dữLiệuMẫuTạoĐủVàThíSinhVàoThiĐượcNgay` | 60 câu, đủ 4 loại và 3 mức; đề 20 câu, 10 điểm; thí sinh vào thi được ngay |
| `StaffServicesTest` | `phânCôngGiảngViênQuyếtĐịnhQuyềnTrênMôn` | Chưa phân công → `AccessDeniedException` khi `loadBank`; phân công → được; bỏ phân công → mất quyền |
| | `sửaCâuHỏiKểCảĐổiLoại` | Sửa câu một đáp án thành đúng/sai, giữ nguyên mã |
| | `khôngSửaCâuĐãDùngChoCaThiHoặcĐổiMôn` | `QUESTION_IN_USE` sau khi mở ca; `SUBJECT_CHANGED` |
| | `danhSáchĐềVàTạoPhiênBảnMớiChoĐềĐãKhóa` | `newVersion` → phiên bản 2, chưa khóa, tổng 10.0; `list` trả 2 đề |

**Kiểm thử nằm ở tệp của phần khác nhưng kiểm mã phần 2:**

| Lớp test | Phương thức | Chứng minh |
|---|---|---|
| `ErrorCodeTest` | `lỗiDữLiệuCâuHỏi` | `CHAPTER_INVALID`, `OPTION_EMPTY`, `OPTIONS_TOO_FEW`, `MULTI_NEEDS_CORRECT`, `FILL_NEEDS_ANSWER`, `QUESTION_NOT_SAVED`, `QUESTION_NOT_FOUND` |
| | `lỗiDữLiệuĐềVàMaTrận` | `EXAM_TITLE`, `ITEM_ORDER`, `ITEM_POINT`, `ITEM_NOT_FOUND`, `MATRIX_COUNT`, `MATRIX_DUPLICATE`, `BANK_SHORTAGE` |
| | `lỗiNhậpTệpGhiTheoDòng` | Dòng `có lẽ` cho câu đúng/sai → lỗi chứa "true hoặc false"; dòng thiếu cột → "cần 6 trường" |
| `GradingTest` | `đềChọnChiếnLượcTheoLoạiCâuVàCấuHình` | `PARTIAL` → `PartialMatchGrading`, `ALL_OR_NOTHING` → `ExactMatchGrading`; mọi đề dùng chung một thể hiện (`assertSame`) |
| | `factoryDựngĐúngLớpConTheoLoại` | Factory dựng đúng lớp con; thiếu đáp án đúng/sai → `TF_NEEDS_VALUE` |
| `JdbcIntegrationTest` (cần MySQL) | `đọcCâuHỏiLênĐúngLớpConQuaFactory` | Đọc từ CSDL ra đúng `MultiChoiceQuestion`, `FillInBlankQuestion`; tiếng Việt qua CSDL vẫn so khớp |
| | `giaoDịchLỗiGiữaChừngThìKhôngGhiGìCả` | Phương án mang sẵn mã 999 → `assignId` ném lỗi giữa giao dịch → dòng `question` đã chèn bị rollback |
| | `sửaCâuHỏiThayToànBộPhươngÁnTrongGiaoDịch` | Sửa đổi loại sang nhiều đáp án, 3 phương án mới mã mới; câu trong đề đã mở ca → `QUESTION_IN_USE` |

Kiểm thử tay: [`docs/manual-test-checklist.md`](../../docs/manual-test-checklist.md) mục 18–26 (UC-06 → UC-09).

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 12. Giới hạn đã biết

Trả lời trung thực, nêu chỗ sửa nếu được hỏi.

1. **UC-08 luồng 5a – đổi một câu trong bản xem trước đề sang câu khác cùng chương, cùng mức độ: chưa làm.**
   ([`docs/manual-test-checklist.md`](../../docs/manual-test-checklist.md): "chưa có nút riêng".) Đề sinh theo ma trận được lưu ngay, không có bước xem trước.
   *Trả lời:* "Hiện giảng viên muốn đổi câu thì dùng thẻ Chọn tay để tự dựng đề. Muốn làm 5a: tách `generate` thành hai
   bước – `bank.pick` trả danh sách xem trước cho giao diện, thêm hàm đổi một câu lấy ngẫu nhiên câu khác cùng `PoolKey`
   chưa có trong danh sách, rồi mới gọi `createManual` để lưu."
2. **Đã sửa sau khi rà soát – sinh đề rỗng từ ma trận.** Bản đầu `createManual` gọi `exam.validateForUse()` nhưng
   `generate` thì không: ma trận rỗng → `pick` trả danh sách rỗng → đề 0 câu vẫn được lưu (lỗ hổng BR-11; đề đó không mở ca
   được nên không gây sai điểm). Nay `generate` cũng gọi `validateForUse()` trước `examDao.insert` → `EXAM_EMPTY`.
   Kiểm thử: `StaffServicesTest.sinhĐềTheoMaTrậnKhôngLưuĐềRỗng`.
3. **Đã sửa sau khi rà soát – xóa câu hỏi đang nằm trong một đề chưa khóa.** Bản đầu service chỉ kiểm tra đề đã khóa;
   trên MySQL khóa ngoại `exam_item.question_id` (không cascade) từ chối → `JdbcErrors.translate` → `DataAccessException`
   → người dùng thấy "Không kết nối được tới máy chủ dữ liệu..." – sai nguyên nhân; bản bộ nhớ lại xóa được, hai bản chạy
   khác nhau. Nay `QuestionDao.isUsedInAnyExam` (cài ở cả `JdbcQuestionDao` và `MemoryQuestionDao`) được kiểm tra trong
   `deleteQuestion` → `QUESTION_IN_USE` "Câu hỏi #N đang nằm trong một đề thi, không thể xóa.". Kiểm thử:
   `StaffServicesTest.khôngXóaCâuĐangNằmTrongĐềChưaKhóa` (bộ nhớ) và
   `JdbcIntegrationTest.xóaCâuNằmTrongĐềBáoLỗiNghiệpVụKhôngPhảiLỗiKếtNối` (MySQL thật).
4. **Đã sửa sau khi rà soát – thông điệp thiếu câu in tên hằng enum** ("mức HARD"). Nay `findShortages` và thông điệp
   `MATRIX_DUPLICATE` của `ExamMatrix.add` dùng `difficulty.displayName()` → "mức Khó". Vẫn giữ `toString()` của enum
   là tên hằng (Phase 4–5 mục 4). Kiểm thử: `ExamAndBankTest.thiếuCâuThìBáoRõThiếuỞĐâu`.
5. **Không sửa/xóa được đề.** `ExamDao` chỉ có `insert`, `findById`, `findBySubject`, `lock`. "Phiên bản mới" chép y nguyên
   đề cũ và cũng không có màn hình sửa – muốn đề khác thì tạo đề mới. Bấm "Tạo phiên bản mới" hai lần trên cùng v1 tạo hai
   bản cùng là v2 (`newVersion` lấy `version + 1` của đề được chọn, CSDL không có ràng buộc duy nhất cho (tên, phiên bản)).
6. **Mở ca và khóa đề không cùng một giao dịch** (phần 3: `sessionDao.insert` rồi mới `examDao.lock`). Nếu mất kết nối
   đúng giữa hai lệnh, ca thi tồn tại mà đề chưa khóa. Xác suất rất nhỏ; `ExamSessionService.createSession` lần sau vẫn
   khóa vì kiểm tra `!exam.isLocked()`.
7. **`JdbcExamDao.findBySubject` là N+1:** lấy danh sách mã đề rồi `findById` từng đề (mỗi đề vài truy vấn). Môn có vài
   chục đề thì chấp nhận được.
8. **Đọc ngân hàng gặp dữ liệu sai thì hỏng cả lần nạp:** dữ liệu từ CSDL cũng qua constructor; nếu ai đó sửa tay CSDL tạo
   câu một đáp án có hai phương án đúng, `loadBank` ném `ValidationException` cho cả môn. Đánh đổi có chủ ý: không để dữ
   liệu sai lọt vào đề thi.
9. **Biểu mẫu báo từng lỗi một** (constructor dừng ở điều kiện sai đầu tiên). Dòng phương án trống phải xóa bằng ✕.
10. Nhỏ: trong `Exam.addItem`, mã câu được thêm vào `questionIds` trước khi `new ExamItem` kiểm tra điểm; nếu điểm ≤ 0 thì
    câu đó bị coi là "đã có trong đề" ở lần thêm sau. Không ảnh hưởng thực tế vì `createManual`/`generate` bỏ cả đề khi lỗi.
11. Tìm theo từ khóa (`Criteria.matches`) không chuẩn hóa NFC như câu điền khuyết; gõ tiếng Việt khác bộ gõ có thể không
    khớp.

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 13. Đoạn demo của mình

Chép từ [`docs/DEMO-SCRIPT.md`](../../docs/DEMO-SCRIPT.md), mục "Phần 2 · Đỗ Thị Vân Anh – Giảng viên: câu hỏi và đề thi (2 phút 30)", thêm cột mã.

| Bấm | Nói | Phía sau là mã nào |
|---|---|---|
| Máy A: đăng nhập `gv.viet` / `gv123456` → Ngân hàng câu hỏi | "60 câu, 4 loại, 3 chương, 3 mức độ. Lọc theo chương, mức độ, loại." | `QuestionBankController.init` → [nền] `QuestionBankService.loadBank` → `Permissions.requireManage` → `JdbcQuestionDao.findBySubject` (2 truy vấn) → `QuestionFactory.create` từng dòng. Lọc: `applyFilter()` → `QuestionBank.findBy(new Criteria(...))`, không gọi CSDL. 60 câu do `SeedTool` nạp từ `seed-questions.txt`. |
| Thêm câu hỏi → loại Một đáp án → tick Đúng cho 2 phương án → Lưu | "Lỗi hiện ngay cạnh ô phương án. Quy tắc 'đúng một đáp án đúng' nằm trong lớp SingleChoiceQuestion, controller không kiểm tra gì." | `QuestionFormController.onSave` → `collect()` → [nền] `QuestionBankService.addQuestion` → `QuestionFactory.create` → constructor `SingleChoiceQuestion` ném `SINGLE_NEEDS_ONE_CORRECT` → `showError` ánh xạ mã → `optionsError`: "Câu một đáp án phải có đúng 1 phương án đúng, hiện có 2." |
| Sửa thành 1 đáp án đúng → Lưu | "Bốn loại câu là bốn lớp con của Question; QuestionFactory chọn lớp con theo loại (mẫu Factory)." | Constructor qua được → `JdbcQuestionDao.insert` trong `cm.inTransaction` (`INSERT INTO question` + `INSERT INTO answer_option` từng phương án) → `close()` → `QuestionBankController.load()` |
| Đề thi → Tạo đề mới → thẻ Theo ma trận → nhập chương 3 Khó = 10 → Kiểm tra ma trận | "Ngân hàng không đủ câu thì báo thiếu ở đâu, trước khi sinh đề." | `ExamListController.onCreate` → `ExamBuilderController.init`/`buildMatrix` (ô hiện "/ có 7") → `onCheckMatrix` → `readMatrix` → `QuestionBank.findShortages` → "Thiếu câu: chương 3 mức HARD: cần 10, có 7." |
| Sửa thành 2 → Kiểm tra → đặt tên → Tạo đề | "Câu nhiều đáp án chấm toàn phần hay theo tỷ lệ là cấu hình của đề – mẫu Strategy." | `onCreate` → [nền] `ExamService.generate` → `QuestionBank.pick` → `Exam.draft(title, subject, policy)` (constructor điền `strategies`) → `addItem` → `JdbcExamDao.insert` (`INSERT INTO exam` + batch `INSERT INTO exam_item`) → `created` |
| Chọn đề vừa tạo → Mở ca thi… → lớp INT1332-01 → giữ giờ mở là bây giờ, sửa giờ đóng thành 15 phút sau → Mở ca thi | "Ghi danh cả lớp. Mở ca xong đề bị khóa; muốn sửa phải tạo phiên bản mới." | `ExamListController.onOpenSession` → `session-form.fxml` (phần 3) → `ExamSessionService.createSession` → `exam.lock()` + `JdbcExamDao.lock` (`UPDATE exam SET locked = TRUE WHERE id = ?`). Nếu được hỏi tiếp: nút "Tạo phiên bản mới" → `ExamService.newVersion` |

**Mẹo khi demo:**
- Bước "tick Đúng cho 2 phương án": nhập **nội dung câu hỏi** và **đủ 4 dòng phương án** (hoặc bấm ✕ xóa dòng thừa). Còn
  dòng trống thì lỗi hiện ra sẽ là "Nội dung phương án không được để trống." (`OPTION_EMPTY`) chứ không phải lỗi hai
  đáp án đúng.
- Ma trận chương 3 Khó = 2, điểm mỗi câu mặc định 1 → đề 2 câu, 2 điểm. Ô chính sách mặc định là `PARTIAL` ("Theo tỷ lệ: đúng phần nào được phần đó").

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 14. Câu hỏi vấn đáp

### Mức 1 – Cơ bản

**1. Có mấy loại câu hỏi, mỗi loại là lớp nào?**
Bốn: `SingleChoiceQuestion`, `MultiChoiceQuestion`, `TrueFalseQuestion`, `FillInBlankQuestion`, đều kế thừa lớp trừu
tượng `Question`; nhãn tương ứng trong enum `QuestionType`. Chỉ có câu khách quan vì mọi bài phải chấm tự động
(quyết định chốt số 3).

**2. Vì sao `Question` là lớp trừu tượng mà không phải interface?**
Vì có trạng thái chung (mã, môn, nội dung, chương, mức độ, điểm) và kiểm tra chung trong constructor `protected`. Interface
không có trường đối tượng, không có constructor. Ba hành vi khác nhau theo loại (`getType`, `isCorrect`, `correctRatio`)
là `abstract`.

**3. Quy tắc "câu một đáp án có đúng một phương án đúng" nằm ở đâu? Vì sao không để controller kiểm tra?**
Trong constructor `SingleChoiceQuestion` (mã `SINGLE_NEEDS_ONE_CORRECT`). Mọi đường tạo câu hỏi – biểu mẫu, nhập tệp, đọc
CSDL, `SeedTool`, kiểm thử – đều qua constructor, nên không thể tồn tại câu sai. Đặt ở controller thì nhập tệp bỏ qua kiểm
tra và không kiểm thử được nếu không mở giao diện. Controller chỉ đọc mã lỗi để hiện lỗi đúng ô (`showError`).

**4. `QuestionFactory` làm gì, ai gọi nó?**
Nhận `QuestionType` + `QuestionData`, trả về đúng lớp con (switch trên enum). Gọi từ `QuestionBankService.addQuestion`,
`QuestionBankService.updateQuestion` và `JdbcQuestionDao.load`. Cần vì loại câu chỉ biết lúc chạy; gom vào một chỗ để
thêm loại mới chỉ sửa một nơi.

**5. Câu trả lời của thí sinh lưu dạng gì? Vì sao câu trắc nghiệm lưu mã phương án?**
Luôn là chuỗi: `"12"`, `"12,14"`, `"true"`/`"false"`, văn bản. Lưu `optionId` (mã trong bảng `answer_option`) chứ không
lưu A/B/C vì máy thí sinh có thể trộn phương án; mã thì không đổi khi trộn.

**6. `ExamItem` khác `Question` thế nào?**
`ExamItem` là "câu hỏi khi đã nằm trong một đề": thêm `order` và `point`. Cùng một câu có thể 1 điểm ở đề này, 2 điểm ở đề
khác. `defaultPoint` của câu chỉ là gợi ý khi thêm vào đề. Chấm điểm dùng `ExamItem.getPoint()` làm điểm tối đa.

**7. `ALL_OR_NOTHING` và `PARTIAL` khác nhau thế nào? Đặt ở đâu?**
Là `MultiChoicePolicy`, thuộc tính của **đề** (cột `exam.multi_policy`), chọn ở ô "Câu nhiều đáp án" màn hình Tạo đề.
Constructor `Exam` ánh xạ: `PARTIAL` → `PartialMatchGrading` (dùng `correctRatio`), ngược lại → `ExactMatchGrading`
(dùng `isCorrect`, đúng hết mới có điểm). Phần 4 chỉ gọi `exam.strategyFor(question)`.

**8. Lọc câu hỏi chạy thế nào, có gọi CSDL mỗi lần gõ không?**
Không. Đổi môn thì nạp cả ngân hàng một lần (`loadBank`). Mỗi lần đổi bộ lọc, `applyFilter()` tạo `Criteria` rồi gọi
`QuestionBank.findBy` trên bộ nhớ. Trường nào `null` thì không lọc theo trường đó; từ khóa không phân biệt hoa thường.

**9. Định dạng tệp nhập câu hỏi? Dòng sai thì sao?**
Mỗi dòng 6 trường cách nhau `|`: `LOAI|chuong|muc_do|diem|noi_dung|dap_an`; phương án cách nhau `;`, phương án đúng có `*`.
Dòng sai được ghi "Dòng N: lý do" và nhập tiếp dòng sau; cuối cùng trả `ImportResult(imported, errors)`.

**10. Giảng viên A có thêm/sửa câu hỏi môn của giảng viên B được không? Kiểm tra ở đâu?**
Không. Mọi hàm của `QuestionBankService`/`ExamService` gọi `Permissions.requireManage(actor, subject)` →
`User.canManage` (đa hình: `Instructor` kiểm tra môn được phân công). Sai → `AccessDeniedException("SUBJECT_FORBIDDEN")`
"Bạn không phụ trách môn ...". Kiểm tra ở service nên dù giao diện có nút cũng bị chặn. `Permissions` là lớp
package-private trong `service` – controller không gọi được, chỉ service gọi.

**11. Đề bị khóa khi nào? Khóa rồi muốn đổi thì làm sao?**
Khi mở ca thi đầu tiên với đề đó: `ExamSessionService.createSession` gọi `exam.lock()` và `examDao.lock(...)`. Sau đó
`addItem` ném `EXAM_LOCKED`, câu hỏi trong đề không sửa/xóa được. Muốn đổi: "Tạo phiên bản mới" → `ExamService.newVersion`
→ bản chép chưa khóa, phiên bản + 1; đề cũ giữ nguyên cho các ca đã thi.

### Mức 2 – Đào sâu

**12. Vì sao `QuestionData.correctValue` là `Boolean` chứ không phải `boolean`?**
Để có giá trị `null` = "chưa chọn Đúng/Sai". `QuestionFactory` dựa vào đó ném `TF_NEEDS_VALUE`. Kiểu `boolean` luôn có
giá trị (mặc định `false`) nên không phân biệt được "chọn Sai" với "quên chọn".

**13. Câu có 2 phương án đúng, thí sinh chọn 2 đúng và 1 sai, chế độ `PARTIAL` được mấy phần?**
`correctRatio` = (2 − 1) / 2 = 0.5. `PartialMatchGrading` nhân với điểm câu và làm tròn 2 chữ số. Trừ phần chọn sai để chọn
hết mọi phương án không được điểm; chặn dưới 0 để không có điểm âm. Ở chế độ `ALL_OR_NOTHING` cùng câu trả lời được 0.

**14. Vì sao `TrueFalseQuestion.isCorrect` không gọi thẳng `Boolean.parseBoolean`?**
Vì `parseBoolean` trả `false` cho mọi chuỗi lạ. Với câu có đáp án `false`, chuỗi rác như `"có"` sẽ bị chấm đúng. Mã kiểm
tra chuỗi phải là `true`/`false` (không phân biệt hoa thường) trước. Kiểm thử `trueFalseKhôngNhậnChuỗiLạ`.

**15. Điền khuyết so khớp thế nào? NFC và `Locale.ROOT` để làm gì?**
`normalize`: NFC → `strip()` → gộp khoảng trắng → chữ thường (nếu không phân biệt hoa thường). NFC vì bộ gõ có thể sinh
chữ có dấu theo hai cách mã hóa trông giống nhau. `Locale.ROOT` để `toLowerCase` không đổi theo ngôn ngữ máy. Đáp án chấp
nhận chuẩn hóa sẵn trong constructor, lúc chấm chỉ chuẩn hóa câu trả lời. Không bỏ dấu: "ke thua" ≠ "kế thừa".

**16. Sửa câu hỏi: vì sao trong giao dịch, vì sao xóa phương án rồi chèn lại?**
`JdbcQuestionDao.update` chạy `UPDATE question`, `DELETE FROM answer_option`, rồi chèn lại – trong một
`cm.inTransaction`: lỗi giữa chừng thì không để câu hỏi mất hết phương án. Xóa-chèn lại đúng với mọi trường hợp (thêm/bớt
phương án, đổi loại câu). Vì thế `QuestionData.from` chép phương án thành đối tượng mới chưa có mã (`AnswerOption.of`),
nếu không `assignId` sẽ ném lỗi vì phương án cũ đã có mã.

**17. Vì sao cấm sửa câu trong đề đã khóa nhưng vẫn cho sửa câu trong đề chưa khóa?**
Đề đã khóa nghĩa là đã có ca thi, bài làm lưu tham chiếu mã phương án; sửa thì điểm cũ đổi theo. Đề chưa khóa chưa ai thi,
sửa câu không ảnh hưởng kết quả nào. Kiểm tra bằng `isUsedInLockedExam` (`exam_item JOIN exam ... e.locked = TRUE`).

**18. Đọc ngân hàng 200 câu tốn mấy truy vấn? Chuỗi `IN (?,?,?)` có bị SQL injection không?**
Hai: một lấy câu hỏi, một lấy mọi phương án `WHERE question_id IN (...)` rồi gom theo `question_id` (tránh N+1 = 201
truy vấn). `IN (...)` được dựng bằng cách nối **dấu `?`** theo số phần tử, giá trị vẫn gán bằng `ps.setInt` – không nối giá
trị nào vào SQL nên không bị injection.

**19. Sinh đề theo ma trận: làm sao không trùng câu, không làm xáo trộn ngân hàng, và kiểm thử được?**
Gom câu theo `PoolKey(chương, mức)`; mỗi câu thuộc đúng một nhóm; mỗi ô ma trận lấy `subList(0, count)` của bản sao đã trộn
– phần tử khác nhau. Trộn trên `new ArrayList<>(...)` nên ngân hàng gốc không đổi thứ tự. `Random` truyền từ ngoài: kiểm
thử cố định seed để ra cùng bộ đề. `MATRIX_DUPLICATE` chặn hai dòng cùng ô (sẽ lấy trùng).

**20. Kiểm tra thiếu câu ở cả màn hình (`onCheckMatrix`) và ở `pick` – có thừa không?**
Không thừa. Màn hình gọi `findShortages` để báo trước, giảng viên sửa ma trận. `pick` gọi lại vì service không được tin
giao diện (giảng viên có thể bấm "Tạo đề" mà không kiểm tra). Quy tắc chỉ viết một lần trong `QuestionBank.findShortages`,
hai nơi cùng dùng.

**21. `Exam.restoreItem` để làm gì, sao không dùng `addItem`?**
DAO dựng lại đề từ CSDL, kể cả đề đã khóa. `addItem` gọi `ensureEditable` → ném `EXAM_LOCKED`, và đánh số lại thứ tự.
`restoreItem` giữ `order` đã lưu và bỏ qua kiểm tra khóa. Chỉ DAO được dùng hàm này (ghi rõ trong javadoc).

**22. Vì sao `Exam` dùng chung ba đối tượng chiến lược `static final`? Có an toàn khi nhiều luồng chấm không?**
Ba lớp chiến lược không có trường nào (stateless), kết quả chỉ phụ thuộc tham số, nên một thể hiện dùng chung cho mọi đề
và mọi luồng là an toàn. Kiểm thử `đềChọnChiếnLượcTheoLoạiCâuVàCấuHình` dùng `assertSame`.

**23. Vì sao một bảng `question` cho 4 loại? Đáp án điền khuyết lưu ở đâu, có lộ ra máy thí sinh không?**
Chỉ khác 2 cột (`correct_value`, `case_sensitive`) nên dùng một bảng (single-table), cột `type` cho biết lớp con.
Đáp án điền khuyết lưu trong `answer_option` với `is_correct = TRUE`. Không lộ: `oes_client` không có quyền trên
`question`, `answer_option`; khung nhìn `v_exam_paper` chỉ nối `answer_option` cho `SINGLE_CHOICE`/`MULTI_CHOICE` và không
có cột `is_correct`.

**24. `QuestionFactory` có phải Factory Method của GoF không?**
Không hẳn. Đây là Simple Factory: một hàm tĩnh chọn lớp con theo tham số. Factory Method của GoF là một phương thức tạo đối
tượng mà lớp con ghi đè. Ý chung giống nhau: nơi dùng không `new` lớp cụ thể.

**25. Máy thí sinh có đối tượng `Question` không?**
Không. `Question` có `isCorrect`, `AnswerOption` có `isCorrect`; có đối tượng là có đáp án. Máy thí sinh dùng mô hình riêng
trong gói `client` (`PaperItem`, `PaperOption` – không có cờ đúng/sai), đọc từ `v_exam_paper` (phần 3). Chỉ máy giảng viên
dựng `Question` và chấm bài.

**26. Trong `importFile`, `catch (IllegalArgumentException e)` có bắt nhầm `ValidationException` không?**
Không. `ValidationException` kế thừa `ExamAppException` → `RuntimeException`, không phải lớp con của
`IllegalArgumentException`. Hai nhánh tách bạch: lỗi nghiệp vụ hiện `getUserMessage()`; lỗi đổi kiểu
(`NumberFormatException` là lớp con của `IllegalArgumentException`, và `valueOf` của enum gõ sai) hiện "giá trị không hợp lệ".

**27. Vì sao `updateQuestion` kiểm tra quyền theo môn của câu gốc trong CSDL?**
Vì `data.subject()` do giao diện gửi lên – không đáng tin. Kiểm tra theo `original.getSubject()` (đọc từ CSDL) rồi mới bắt
buộc `data.subject()` trùng môn đó (`SUBJECT_CHANGED`). Nếu chỉ kiểm tra `data.subject()`, giảng viên có thể gửi môn của
mình kèm mã câu hỏi của môn khác.

### Mức 3 – Sửa trực tiếp

**S1. "Thêm loại câu hỏi trả lời bằng số (ví dụ NUMERIC) ngay tại chỗ."**
1. `QuestionType`: thêm hằng mới kèm tên hiển thị.
2. Tạo lớp con mới trong `model/question/` kế thừa `Question`: trường đáp án, kiểm tra trong constructor, cài
   `getType`, `isCorrect`, `correctRatio`.
3. Biên dịch (`./mvnw compile`): trình biên dịch báo **mọi `switch` biểu thức trên `QuestionType` thiếu nhánh** – thêm nhánh ở
   `QuestionFactory.create`, `QuestionBankService.withAnswers`, `JdbcQuestionDao.load`, `QuestionFormController.collect`,
   và các chỗ của phần 3/4: `ExamController.show`, `ReviewItem.toText`, `MemoryStudentExamDao.toPaperItem`,
   `MemoryStudentExamDao.correctAnswerOf`, `JdbcStudentExamDao.loadReview`, `ConsoleDemo.correctResponse`.
4. Những chỗ trình biên dịch **không** báo, phải tự nhớ: `Exam` constructor `strategies.put(...)` (thiếu thì `strategyFor`
   trả `null`, chấm lỗi); `JdbcQuestionDao.setQuestionColumns`/`insertOptions` (lưu đáp án vào đâu);
   `QuestionData.from` (rơi vào nhánh cuối `withOptions`); `QuestionFormController.showPaneFor` + vùng nhập trong
   `question-form.fxml`; cột `question.type ENUM(...)` trong [`db/schema.sql`](../../db/schema.sql) (cần `ALTER TABLE`).
5. Kiểm: thêm kiểm thử vào `QuestionTest` (đúng/sai/chuỗi hỏng), chạy `./mvnw test -Dtest=QuestionTest,GradingTest`, rồi
   `./mvnw test`.

**S2. "Đổi quy tắc: câu trắc nghiệm phải có ít nhất 3 phương án."**
Sửa điều kiện `options.size() >= 2` thành `>= 3` và câu thông báo `OPTIONS_TOO_FEW` trong constructor
`SingleChoiceQuestion` **và** `MultiChoiceQuestion`. Biểu mẫu tự hiện lỗi đúng ô (mã không đổi). Chạy `./mvnw test` và
chờ **một số kiểm thử cũ đỏ** vì đang dựng câu chỉ 2 phương án: `QuestionTest.câuMộtĐápÁnCóHaiPhươngÁnĐúngBịTừChối` (sẽ
nhận `OPTIONS_TOO_FEW` thay vì `SINGLE_NEEDS_ONE_CORRECT`), `ErrorCodeTest.lỗiDữLiệuCâuHỏi` (dòng `MULTI_NEEDS_CORRECT`),
hàm `addSingle` trong `StaffServicesTest`, và hai ca trong `JdbcIntegrationTest` – thêm phương án thứ ba vào các chỗ đó.
`SeedToolTest` vẫn xanh: cả 34 câu trắc nghiệm trong `seed-questions.txt` đều có 4 phương án.

**S3. "Sinh đề theo ma trận mà ma trận rỗng thì sao? Chỉ cho tôi chỗ chặn."**
`ExamService.generate` gọi `exam.validateForUse()` trước `examDao.insert(exam)`, giống `createManual` → `EXAM_EMPTY`,
hộp thoại hiện thông điệp. Đây là lỗi đã gặp và đã sửa (mục 12, giới hạn 2). Thầy bảo thử: xóa dòng đó rồi chạy
`./mvnw test -Dtest=StaffServicesTest` – `sinhĐềTheoMaTrậnKhôngLưuĐềRỗng` đỏ; trả lại dòng thì xanh.

**S4. "Đổi thông báo thiếu câu thành 'Chương 3, mức Khó: thiếu 3 câu'."**
Sửa chuỗi trong `QuestionBank.findShortages` (đã dùng `e.difficulty().displayName()`, chỉ đổi cách ghép chuỗi:
`"Chương " + e.chapter() + ", mức " + e.difficulty().displayName() + ": thiếu " + (e.count() - have) + " câu"`). Sửa hai
dòng `assertTrue` trong `ExamAndBankTest.thiếuCâuThìBáoRõThiếuỞĐâu` theo chuỗi mới. Chạy
`./mvnw test -Dtest=ExamAndBankTest`.

**S5. "Thêm mức độ 'Rất khó'."**
Thêm hằng vào `Difficulty` (ví dụ sau `HARD`). Giao diện tự có: ô lọc dùng `Difficulty.values()`, lưới ma trận
(`buildMatrix`) tự thêm một cột. Phải sửa CSDL: cột `difficulty ENUM('EASY','MEDIUM','HARD')` của bảng `question` trong
[`db/schema.sql`](../../db/schema.sql) (và `ALTER TABLE` trên CSDL đang chạy), nếu không MySQL từ chối giá trị mới. Tệp nhập dùng tên hằng mới.
Chạy `./mvnw test` (`SeedToolTest` kiểm tra đủ mọi mức độ có trong `Difficulty.values()` – sẽ **thất bại** vì dữ liệu mẫu
chưa có câu "Rất khó"; phải thêm câu vào `seed-questions.txt` hoặc sửa kiểm thử).

**S6. "Cho phép xóa câu nằm trong đề chưa khóa, tự bỏ câu đó khỏi đề."**
Hiện `QuestionBankService.deleteQuestion` chặn bằng `isUsedInAnyExam` (mục 12, giới hạn 3 – đã sửa). Muốn đổi quy tắc:
bỏ khối `isUsedInAnyExam` trong `deleteQuestion`; trong `JdbcQuestionDao.delete` xóa trước các dòng
`DELETE FROM exam_item WHERE question_id = ?` rồi mới xóa câu, hai lệnh trong cùng một giao dịch (`cm.inTransaction`); bản `MemoryQuestionDao` phải bỏ câu khỏi các đề tương ứng để hai bản chạy giống nhau.
Sửa `StaffServicesTest.khôngXóaCâuĐangNằmTrongĐềChưaKhóa` và `JdbcIntegrationTest.xóaCâuNằmTrongĐềBáoLỗiNghiệpVụKhôngPhảiLỗiKếtNối`
theo quy tắc mới. Nên trả lời kèm lý do vì sao nhóm chọn chặn: giảng viên dễ vô tình làm hỏng đề đang soạn.

**S7. "Điểm của một câu trong đề không được vượt quá 10."**
Thêm một `ValidationException.require(point <= 10, ...)` vào constructor `ExamItem` (cạnh `ITEM_POINT`). Nằm ở model nên
cả chọn tay, sinh ma trận, `newVersion` và `restoreItem` (qua `new ExamItem` trong DAO) đều tuân theo. Lưu ý đề cũ trong
CSDL có câu > 10 điểm sẽ không đọc lên được. Thêm dòng vào `ErrorCodeTest.lỗiDữLiệuĐềVàMaTrận`; chạy `./mvnw test`.

**S8. "Bảng ngân hàng ở thẻ Chọn tay thêm cột Loại."**
Trong `ExamBuilderController.init`, thêm một dòng giống các cột đã có:
`bankTable.getColumns().add(Tables.column("Loại", 110, q -> q.getType().displayName()));`. Chỉ là giao diện, không có
logic. Kiểm tra bằng `./mvnw javafx:run@staff` → Đề thi → Tạo đề mới.

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 15. Thuật ngữ

| Thuật ngữ | Nghĩa trong dự án |
|---|---|
| Ngân hàng câu hỏi (`QuestionBank`) | Toàn bộ câu hỏi của một môn, nạp vào bộ nhớ để lọc và chọn |
| Phương án (`AnswerOption`) | Một lựa chọn của câu trắc nghiệm; có mã, nội dung, cờ đúng |
| Đáp án chấp nhận | Các chuỗi được coi là đúng của câu điền khuyết |
| Chương / mức độ | `chapter` (số nguyên > 0) / `Difficulty` (Dễ, Trung bình, Khó) |
| Ma trận đề (`ExamMatrix`) | Bảng (chương, mức độ) → số câu cần lấy |
| Mục đề (`ExamItem`) | Một câu khi nằm trong một đề: thứ tự + điểm |
| Chính sách câu nhiều đáp án (`MultiChoicePolicy`) | Toàn phần (`ALL_OR_NOTHING`) hoặc theo tỷ lệ (`PARTIAL`) |
| Khóa đề / phiên bản | Đề đã dùng mở ca thì `locked`; muốn đổi tạo bản `version + 1` |
| Factory | Một chỗ duy nhất quyết định tạo lớp con nào từ dữ liệu |
| Strategy | Thuật toán chấm thay đổi được; đề chọn chiến lược cho từng loại câu |
| Bất biến (invariant) | Điều luôn đúng với mọi đối tượng hợp lệ, bảo đảm bằng constructor |
| Compact constructor | Constructor rút gọn của record, dùng để kiểm tra dữ liệu |
| Giao dịch (transaction) | Nhóm câu lệnh SQL: hoặc tất cả được ghi, hoặc không câu nào (`inTransaction`) |
| N+1 truy vấn | Lỗi hiệu năng: 1 truy vấn danh sách + N truy vấn cho từng phần tử |
| Single-table inheritance | Nhiều lớp con lưu chung một bảng, cột `type` phân biệt |
| NFC | Dạng chuẩn hóa Unicode gộp chữ và dấu thành một ký tự |
| Mã lỗi ổn định | `getCode()` của `ExamAppException`, không đổi khi sửa câu thông báo |

---

<sub>[↑ Về mục lục](#mục-lục)</sub>

## 16. Tự kiểm tra trước buổi vấn đáp

Phải tự nói được, không nhìn tài liệu:

- ☐ Nói trôi chảy đoạn "Nói trong 1 phút" (mục 0).
- ☐ Kể 4 lớp con của `Question`, mỗi lớp kiểm tra gì trong constructor và so khớp câu trả lời thế nào.
- ☐ Giải thích vì sao kiểm tra dữ liệu nằm trong constructor model, không ở controller (ít nhất 3 lý do).
- ☐ Giải thích vì sao `Question` là lớp trừu tượng, không phải interface.
- ☐ Giải thích `QuestionFactory`: vấn đề nó giải quyết, 3 nơi gọi, nếu bỏ đi thì sao; vì sao `switch` không có `default`.
- ☐ Giải thích vì sao `QuestionData.correctValue` là `Boolean`, và vì sao `QuestionData.from` tạo phương án không mã.
- ☐ Tính tay `correctRatio` cho vài cách chọn; nói khác biệt `ALL_OR_NOTHING` / `PARTIAL` và chỗ `Exam` chọn chiến lược.
- ☐ Giải thích vì sao `TrueFalseQuestion` không dùng thẳng `Boolean.parseBoolean`; vì sao điền khuyết dùng NFC.
- ☐ Giải thích `ExamItem` (điểm theo đề), `Exam.getTotalPoint`, `validateForUse` (BR-11).
- ☐ Vẽ được luồng sinh đề: `onCheckMatrix` → `findShortages` → `generate` → `pick` → `JdbcExamDao.insert`, nói được câu
  thông báo thiếu câu thật.
- ☐ Giải thích khóa đề: ai gọi `lock` (phần 3), `ensureEditable`, `restoreItem`, `newVersion`.
- ☐ Đọc thuộc định dạng tệp nhập; giải thích báo lỗi theo dòng, `split("\\|", -1)`, hai khối `catch`.
- ☐ Kể ba câu SQL của `JdbcQuestionDao.update` và vì sao trong một giao dịch; vì sao đọc câu hỏi chỉ 2 truy vấn.
- ☐ Giải thích kiểm tra quyền: `Permissions.requireManage` → `canManage` đa hình; vì sao kiểm tra ở service.
- ☐ Nói được 4 bảng, khóa chính/khóa ngoại/`UNIQUE` của `exam_item`, vì sao một bảng cho 4 loại câu.
- ☐ Giải thích vì sao máy thí sinh không bao giờ có đối tượng `Question` (NFR-04, `v_exam_paper`).
- ☐ Nói trung thực các giới hạn ở mục 12, nhất là UC-08 5a và ba lỗi đã sửa sau rà soát (đề rỗng từ ma trận, xóa câu trong đề chưa khóa, "mức HARD").
- ☐ Làm thử tại máy ít nhất S3 và S4 ở mục 14, chạy `./mvnw test` thấy xanh, rồi trả lại mã bằng `git checkout -- <tệp vừa sửa>` (không dùng `git checkout .` – sẽ xóa cả thay đổi khác chưa commit).
- ☐ Chạy thử đoạn demo mục 13 trên đúng máy demo sau khi chạy `SeedTool`.
- ☐ Kể tên ít nhất 5 kiểm thử của phần mình và mỗi cái chứng minh gì.
