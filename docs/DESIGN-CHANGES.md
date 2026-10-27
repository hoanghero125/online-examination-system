# Thay đổi so với thiết kế Milestone 3

Mỗi mục ghi theo mẫu *thay đổi – lý do – ảnh hưởng*, kèm nguyên lý thiết kế liên quan.
Nhóm dùng tệp này để trả lời vấn đáp: "vì sao mã khác sơ đồ lớp trong báo cáo M3?".

Những khác biệt đã có từ Milestone 4 (thêm `SubjectDao`, `DuplicateKeyException`, 12 bảng, bảng `app_user`)
được trình bày trong báo cáo M4, không nhắc lại ở đây.

## Phase 0 – Dựng repo

### `schema.sql` chuyển từ `src/main/resources/` sang `db/`
- **Lý do:** tệp SQL do quản trị CSDL chạy bằng tay, không phải tài nguyên chương trình đọc lúc chạy. Để ở `db/`
  cùng `security.sql` cho người dựng phòng máy dễ tìm.
- **Ảnh hưởng:** không có mã nào đọc tệp này; README và RUNBOOK trỏ tới đường dẫn mới.
