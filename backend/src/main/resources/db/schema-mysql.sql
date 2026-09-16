-- 408真题训练系统 MySQL 数据库初始化脚本（MySQL 8.0）
-- 与 db/schema.sql (H2) 保持相同列顺序，用于从 H2 迁移数据
-- 由 spring.sql.init 在 mysql profile 下执行；CREATE TABLE IF NOT EXISTS 幂等

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50),
    `email` VARCHAR(100),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 题库表
CREATE TABLE IF NOT EXISTS `question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_year` INT COMMENT '年份(AI生成题为NULL)',
    `subject` VARCHAR(20) NOT NULL COMMENT '科目: 数据结构/计算机组成原理/操作系统/计算机网络',
    `type` VARCHAR(20) NOT NULL COMMENT '题型: 单选/综合应用',
    `question_number` INT COMMENT '题号',
    `content` TEXT NOT NULL COMMENT '题目内容',
    `options` TEXT COMMENT '选项JSON, 如[{"key":"A","text":"..."}]',
    `answer` TEXT NOT NULL COMMENT '正确答案',
    `analysis` TEXT COMMENT '解析',
    `knowledge_tag` VARCHAR(100) COMMENT '知识点标签',
    `knowledge_tags` VARCHAR(500) COMMENT '细分知识点标签，逗号分隔',
    `source` VARCHAR(10) DEFAULT 'real' COMMENT '来源: real=真题 / ai=AI生成',
    `verified` TINYINT(1) DEFAULT 1 COMMENT 'AI题是否通过自检',
    INDEX `idx_exam_year` (`exam_year`),
    INDEX `idx_subject` (`subject`),
    INDEX `idx_type` (`type`),
    INDEX `idx_knowledge_tag` (`knowledge_tag`),
    INDEX `idx_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 考试/刷题记录表
CREATE TABLE IF NOT EXISTS `exam_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `exam_year` INT COMMENT '年份(年份套题模式)',
    `subject` VARCHAR(20) COMMENT '科目(单科模式)',
    `mode` VARCHAR(20) NOT NULL COMMENT '刷题模式: year/subject/random/wrong',
    `total_questions` INT DEFAULT 0,
    `correct_count` INT DEFAULT 0,
    `score` DECIMAL(5,1) DEFAULT 0,
    `duration_seconds` INT DEFAULT 0 COMMENT '用时(秒)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户答题明细表
CREATE TABLE IF NOT EXISTS `user_answer` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `exam_record_id` BIGINT,
    `user_answer` VARCHAR(500) COMMENT '用户答案',
    `is_correct` TINYINT(1) COMMENT '是否正确',
    `is_self_scored` TINYINT(1) DEFAULT 0 COMMENT '大题是否手动评分',
    `score` INT DEFAULT 0 COMMENT '该题得分(应用题AI评分)',
    `ai_feedback` TEXT COMMENT '应用题AI评分反馈',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_question` (`user_id`, `question_id`),
    INDEX `idx_exam_record` (`exam_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 错题本表
CREATE TABLE IF NOT EXISTS `wrong_question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `wrong_count` INT DEFAULT 1 COMMENT '错误次数',
    `last_wrong_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_reviewed` TINYINT(1) DEFAULT 0 COMMENT '是否已复习',
    `review_stage` INT DEFAULT 0 COMMENT '间隔重复阶段',
    `next_review_at` DATETIME COMMENT '下次复习时间(空=待复习)',
    `reason` VARCHAR(20) COMMENT '错题原因: 概念不清/粗心/计算错/审题不清/其他',
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
    INDEX `idx_user_reviewed` (`user_id`, `is_reviewed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI对话会话表
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT,
    `title` VARCHAR(200),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_ai_session_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI对话消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL,
    `role` VARCHAR(10) NOT NULL COMMENT 'user/assistant',
    `content` TEXT,
    `prompt_tokens` INT DEFAULT 0,
    `completion_tokens` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_ai_msg_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI用量日志表
CREATE TABLE IF NOT EXISTS `ai_usage_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `feature` VARCHAR(30) COMMENT '功能: explain/chat/assistant/diagnosis/variant',
    `prompt_tokens` INT DEFAULT 0,
    `completion_tokens` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_ai_usage_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;