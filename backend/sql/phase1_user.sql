-- Phase 1：用户中心数据表，适用于本地 MySQL 8。
-- 数据库尚不存在时创建数据库，字符集统一使用 utf8mb4。
CREATE DATABASE IF NOT EXISTS `campus`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 明确选择项目数据库，避免在其他数据库中创建用户表。
USE `campus`;

-- 仅在表不存在时创建，不删除或覆盖已有用户数据。
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `real_name` VARCHAR(50),
    `student_id` VARCHAR(20),
    `college` VARCHAR(100),
    `phone` VARCHAR(20),
    `avatar` VARCHAR(255),
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '0-普通用户 1-管理员 2-超管',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
