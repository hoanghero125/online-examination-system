-- Online Examination System – Nhóm 2
-- Chạy được trên MySQL 8 và MariaDB 10.6 trở lên.
-- Thứ tự DROP ngược thứ tự CREATE để không vướng khóa ngoại.

DROP VIEW  IF EXISTS v_exam_item_for_student;
DROP TABLE IF EXISTS answer;
DROP TABLE IF EXISTS submission;
DROP TABLE IF EXISTS session_student;
DROP TABLE IF EXISTS exam_session;
DROP TABLE IF EXISTS exam_item;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS answer_option;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS instructor_subject;
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS course_class;
DROP TABLE IF EXISTS subject;

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

-- Bốn loại câu hỏi dùng chung một bảng (single-table). Cột nào không thuộc loại câu hỏi thì để NULL.
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

-- Phương án trắc nghiệm, và cả các đáp án chấp nhận của câu điền khuyết (is_correct = TRUE).
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

CREATE TABLE exam_session (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    exam_id            INT NOT NULL,
    open_time          DATETIME NOT NULL,
    close_time         DATETIME NOT NULL,
    shuffle_questions  BOOLEAN NOT NULL,
    shuffle_options    BOOLEAN NOT NULL,
    show_answer        BOOLEAN NOT NULL,
    CHECK (close_time > open_time),
    FOREIGN KEY (exam_id) REFERENCES exam(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE session_student (
    session_id  INT NOT NULL,
    student_id  INT NOT NULL,
    PRIMARY KEY (session_id, student_id),
    FOREIGN KEY (session_id) REFERENCES exam_session(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- UNIQUE (session_id, student_id) là chốt chặn cuối cùng của quy tắc "mỗi thí sinh thi một lần":
-- dù hai máy cùng bấm "Vào thi" một lúc, CSDL chỉ cho một bản ghi đi qua.
CREATE TABLE submission (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    session_id       INT NOT NULL,
    student_id       INT NOT NULL,
    started_at       DATETIME NOT NULL,
    submitted_at     DATETIME,
    status           ENUM('IN_PROGRESS','SUBMITTED','GRADED') NOT NULL,
    end_reason       ENUM('MANUAL','TIME_OUT','VIOLATION'),
    total_score      DECIMAL(6,2) NOT NULL DEFAULT 0,
    violation_count  INT NOT NULL DEFAULT 0,
    UNIQUE (session_id, student_id),
    FOREIGN KEY (session_id) REFERENCES exam_session(id),
    FOREIGN KEY (student_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE answer (
    submission_id  INT NOT NULL,
    item_order     INT NOT NULL,
    response       TEXT,
    earned_point   DECIMAL(5,2),
    PRIMARY KEY (submission_id, item_order),
    FOREIGN KEY (submission_id) REFERENCES submission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Khung nhìn cho màn hình làm bài: có nội dung câu hỏi và phương án,
-- nhưng KHÔNG có cột is_correct, correct_value. Tài khoản MySQL của máy thí sinh
-- chỉ được cấp quyền đọc khung nhìn này (NFR-04).
CREATE VIEW v_exam_item_for_student AS
SELECT ei.exam_id, ei.item_order, ei.point,
       q.id AS question_id, q.type, q.content,
       ao.id AS option_id, ao.opt_order, ao.content AS option_content
FROM exam_item ei
JOIN question q ON q.id = ei.question_id
LEFT JOIN answer_option ao ON ao.question_id = q.id AND q.type IN ('SINGLE_CHOICE','MULTI_CHOICE');

-- Dự kiến cho Milestone 5 (xem mục "Những điểm chưa làm được" trong báo cáo Milestone 4):
-- tách tài khoản MySQL của máy thí sinh, chỉ cho đọc khung nhìn trên.
-- CREATE USER 'oes_client'@'%' IDENTIFIED BY '...';
-- GRANT SELECT ON oes.v_exam_item_for_student TO 'oes_client'@'%';
-- GRANT SELECT, INSERT, UPDATE ON oes.submission TO 'oes_client'@'%';
-- GRANT SELECT, INSERT, UPDATE ON oes.answer     TO 'oes_client'@'%';
