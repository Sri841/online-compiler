-- ============================================================
-- Online Compiler — MySQL Schema
-- Run this once before starting the backend
-- ============================================================

CREATE DATABASE IF NOT EXISTS compiler_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE compiler_db;

CREATE TABLE IF NOT EXISTS submissions (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    language    VARCHAR(20) NOT NULL,
    source_code TEXT        NOT NULL,
    input_data  TEXT,
    output_data TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_language   (language),
    INDEX idx_status     (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample rows so the history panel shows something
INSERT INTO submissions (language, source_code, input_data, output_data, status) VALUES
('java',
 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello World!");\n    }\n}',
 '', 'Hello World!\n', 'SUCCESS'),
('cpp',
 '#include<iostream>\nusing namespace std;\nint main(){ cout<<"Hello C++!"<<endl; return 0; }',
 '', 'Hello C++!\n', 'SUCCESS'),
('c',
 '#include<stdio.h>\nint main(){ printf("Hello C!\\n"); return 0; }',
 '', 'Hello C!\n', 'SUCCESS');

SELECT 'Database ready!' AS result;
SELECT id, language, status FROM submissions;
