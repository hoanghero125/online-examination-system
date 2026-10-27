-- Online Examination System – Nhóm 2
-- Phân quyền CSDL cho hai ứng dụng (NFR-04). Chạy SAU schema.sql, trên đúng CSDL cần cấp quyền:
--
--     mysql -u root -p oes < db/security.sql
--
-- Mọi tên bảng đều viết không kèm tên CSDL, nên cùng một tệp dùng được cho CSDL thật (oes)
-- và CSDL kiểm thử (oes_test). Chạy lại nhiều lần không lỗi.
--
-- Ý tưởng chính:
--   * Máy giảng viên dùng tài khoản oes_staff: toàn quyền, là nơi DUY NHẤT chấm bài.
--   * Máy thí sinh dùng tài khoản oes_client: chỉ đọc khung nhìn KHÔNG có đáp án, chỉ ghi được
--     câu trả lời và trạng thái nộp bài. Dù thí sinh sửa mã ứng dụng thi, tài khoản này cũng không
--     đọc được đáp án và không tự cho mình điểm được.
--   * Trigger là lưới an toàn cuối cùng cho những gì quyền theo cột không chặn được.
--
-- Chạy được trên MySQL 8 và MariaDB 10.6+.


-- =====================================================================================
-- 1. Khung nhìn cho máy thí sinh
--    Khung nhìn chạy với quyền của người tạo (SQL SECURITY DEFINER mặc định), nên oes_client
--    đọc được dữ liệu đã lọc mà không cần quyền trên bảng gốc question / answer_option.
-- =====================================================================================

-- Đăng nhập phía thí sinh. Chỉ trả mã băm mật khẩu của tài khoản thí sinh: tệp cấu hình chứa
-- mật khẩu oes_client nằm trên mọi máy trạm, không được để lộ mã băm của giảng viên, quản trị viên.
-- Vẫn trả dòng của các vai trò khác (mã băm rỗng) để ứng dụng thi báo "hãy dùng ứng dụng quản lý".
CREATE OR REPLACE VIEW v_login AS
SELECT id, username,
       CASE WHEN role = 'STUDENT' THEN password_hash ELSE '' END AS password_hash,
       full_name, email, role, active, must_change_password, student_code
FROM app_user;

-- Thông tin ca thi kèm tên đề, môn, tổng điểm, số câu: đủ cho màn hình danh sách ca và xác nhận quy chế.
CREATE OR REPLACE VIEW v_session_for_student AS
SELECT es.id AS session_id, es.exam_id, es.open_time, es.close_time,
       es.shuffle_questions, es.shuffle_options, es.show_answer,
       e.title AS exam_title, s.code AS subject_code, s.name AS subject_name,
       (SELECT COALESCE(SUM(ei.point), 0) FROM exam_item ei WHERE ei.exam_id = e.id) AS total_point,
       (SELECT COUNT(*) FROM exam_item ei WHERE ei.exam_id = e.id) AS question_count
FROM exam_session es
JOIN exam e    ON e.id = es.exam_id
JOIN subject s ON s.id = e.subject_id;

-- Đề thi cho màn hình làm bài: nội dung câu hỏi và phương án, KHÔNG có is_correct, correct_value,
-- case_sensitive. Phương án chỉ lấy cho câu một/nhiều đáp án, vì bảng answer_option còn chứa
-- đáp án chấp nhận của câu điền khuyết.
-- Chỉ hiện đề đã có ca thi tới giờ mở: thí sinh không đọc trước được đề của ca ngày mai.
CREATE OR REPLACE VIEW v_exam_paper AS
SELECT ei.exam_id, ei.item_order, ei.point,
       q.id AS question_id, q.type, q.content,
       ao.id AS option_id, ao.opt_order, ao.content AS option_content
FROM exam_item ei
JOIN question q ON q.id = ei.question_id
LEFT JOIN answer_option ao ON ao.question_id = q.id AND q.type IN ('SINGLE_CHOICE', 'MULTI_CHOICE')
WHERE EXISTS (SELECT 1 FROM exam_session es WHERE es.exam_id = ei.exam_id AND es.open_time <= NOW());

-- Đáp án đúng cho màn hình xem lại, CHỈ với ca đã đóng và cho xem đáp án.
-- Chặt hơn M1 ("xem sau khi nộp"): thí sinh nộp sớm không được lộ đáp án cho người còn đang thi.
--   SINGLE/MULTI: mỗi phương án đúng một dòng (correct_option_id)
--   TRUE_FALSE:   correct_value
--   FILL_IN_BLANK: mỗi đáp án chấp nhận một dòng (accepted_answer)
CREATE OR REPLACE VIEW v_review AS
SELECT es.id AS session_id, ei.item_order, q.type, q.correct_value,
       CASE WHEN q.type IN ('SINGLE_CHOICE', 'MULTI_CHOICE') THEN ao.id END      AS correct_option_id,
       CASE WHEN q.type = 'FILL_IN_BLANK' THEN ao.content END                     AS accepted_answer
FROM exam_session es
JOIN exam_item ei ON ei.exam_id = es.exam_id
JOIN question q   ON q.id = ei.question_id
LEFT JOIN answer_option ao ON ao.question_id = q.id AND ao.is_correct = TRUE
WHERE es.close_time <= NOW() AND es.show_answer = TRUE;


-- =====================================================================================
-- 2. Thủ tục đổi mật khẩu cho thí sinh
--    Thí sinh nhập từ CSV phải đổi mật khẩu ở lần đăng nhập đầu, mà thí sinh chỉ dùng ứng dụng thi.
--    Thủ tục chạy với quyền người tạo (SQL SECURITY DEFINER) và chỉ sửa được đúng một dòng:
--    tài khoản thí sinh, có mã băm cũ khớp (so sánh rồi mới ghi, như compare-and-set).
--    p_changed = 1 nếu đổi được, 0 nếu không (sai mã băm cũ, không phải thí sinh, hoặc máy khác vừa đổi).
-- =====================================================================================

DROP PROCEDURE IF EXISTS sp_student_change_password;

DELIMITER //

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

DELIMITER ;


-- =====================================================================================
-- 3. Tài khoản
--    Mật khẩu dưới đây chỉ là giá trị khởi tạo. Đặt lại ngay sau khi chạy script:
--        ALTER USER 'oes_staff'@'%'  IDENTIFIED BY '...';
--        ALTER USER 'oes_client'@'%' IDENTIFIED BY '...';
-- =====================================================================================

CREATE USER IF NOT EXISTS 'oes_staff'@'%'  IDENTIFIED BY 'doi_mat_khau_staff';
CREATE USER IF NOT EXISTS 'oes_client'@'%' IDENTIFIED BY 'doi_mat_khau_client';

-- "ON *" = mọi bảng của CSDL đang chọn.
GRANT ALL PRIVILEGES ON * TO 'oes_staff'@'%';

-- Máy thí sinh: chỉ đọc khung nhìn, cấu hình ca thi và danh sách ghi danh.
GRANT SELECT ON v_login               TO 'oes_client'@'%';
GRANT SELECT ON v_session_for_student TO 'oes_client'@'%';
GRANT SELECT ON v_exam_paper          TO 'oes_client'@'%';
GRANT SELECT ON v_review              TO 'oes_client'@'%';
GRANT SELECT ON exam_session          TO 'oes_client'@'%';
GRANT SELECT ON session_student       TO 'oes_client'@'%';

-- Bài làm: tạo bài, nộp bài, tăng số lần vi phạm. KHÔNG có quyền ghi total_score.
GRANT SELECT,
      INSERT (session_id, student_id, started_at, status),
      UPDATE (status, submitted_at, end_reason, violation_count)
      ON submission TO 'oes_client'@'%';

-- Câu trả lời: ghi câu trả lời và cờ đánh dấu. KHÔNG có quyền ghi earned_point.
GRANT SELECT,
      INSERT (submission_id, item_order, response, flagged),
      UPDATE (response, flagged)
      ON answer TO 'oes_client'@'%';

-- Đổi mật khẩu: không cấp quyền UPDATE trên app_user (sẽ sửa được mật khẩu của cả quản trị viên),
-- chỉ cho gọi thủ tục sp_student_change_password ở mục 2.
GRANT EXECUTE ON PROCEDURE sp_student_change_password TO 'oes_client'@'%';

-- Không cấp gì trên question, answer_option, exam_item, app_user: SELECT trực tiếp sẽ bị từ chối.


-- =====================================================================================
-- 4. Trigger – lưới an toàn khi ứng dụng thi bị sửa
--    Trong trigger, CURRENT_USER() trả về NGƯỜI TẠO trigger (root), không phải người đang kết nối,
--    nên phải dùng USER() ("oes_client@ip-máy-trạm") để biết câu lệnh đến từ máy thí sinh.
--    Các lệnh của oes_staff và root không bị trigger chặn.
--    SQLSTATE '45000' với thông điệp bắt đầu bằng "SUBMISSION_LOCKED" được JdbcErrors đổi thành
--    SessionRuleException(SUBMISSION_LOCKED).
-- =====================================================================================

DROP TRIGGER IF EXISTS trg_submission_client_insert;
DROP TRIGGER IF EXISTS trg_submission_client_update;
DROP TRIGGER IF EXISTS trg_answer_client_insert;
DROP TRIGGER IF EXISTS trg_answer_client_update;

DELIMITER //

-- Bài làm mới do máy thí sinh tạo phải ở trạng thái đang làm.
CREATE TRIGGER trg_submission_client_insert BEFORE INSERT ON submission
FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) = 'oes_client' AND NEW.status <> 'IN_PROGRESS' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: new submission must be IN_PROGRESS';
    END IF;
END //

-- Máy thí sinh chỉ được sửa bài đang làm, chỉ được chuyển sang SUBMITTED (không tự đặt GRADED),
-- và không được giảm số lần vi phạm.
CREATE TRIGGER trg_submission_client_update BEFORE UPDATE ON submission
FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) = 'oes_client' THEN
        IF OLD.status <> 'IN_PROGRESS' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: submission is no longer in progress';
        END IF;
        IF NEW.status = 'GRADED' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: client cannot grade';
        END IF;
        IF NEW.violation_count < OLD.violation_count THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: violation count cannot decrease';
        END IF;
    END IF;
END //

-- BR-04 ở mức CSDL: không ghi câu trả lời khi bài đã nộp hoặc ca đã quá giờ đóng (giờ máy chủ).
CREATE TRIGGER trg_answer_client_insert BEFORE INSERT ON answer
FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) = 'oes_client' AND NOT EXISTS (
            SELECT 1 FROM submission sb JOIN exam_session es ON es.id = sb.session_id
            WHERE sb.id = NEW.submission_id AND sb.status = 'IN_PROGRESS' AND es.close_time > NOW()) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: submission locked or session closed';
    END IF;
END //

-- INSERT ... ON DUPLICATE KEY UPDATE đi qua cả trigger trên lẫn trigger này.
CREATE TRIGGER trg_answer_client_update BEFORE UPDATE ON answer
FOR EACH ROW
BEGIN
    IF SUBSTRING_INDEX(USER(), '@', 1) = 'oes_client' AND NOT EXISTS (
            SELECT 1 FROM submission sb JOIN exam_session es ON es.id = sb.session_id
            WHERE sb.id = NEW.submission_id AND sb.status = 'IN_PROGRESS' AND es.close_time > NOW()) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SUBMISSION_LOCKED: submission locked or session closed';
    END IF;
END //

DELIMITER ;
