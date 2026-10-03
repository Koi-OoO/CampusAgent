-- 为已有数据库补充学号唯一约束。
-- 脚本可以重复执行：索引已存在时跳过。
-- 如果已有重复学号，脚本会停止，不会自动删除或覆盖用户数据。
USE `campus`;

DROP PROCEDURE IF EXISTS `ensure_user_student_id_unique`;

DELIMITER //

CREATE PROCEDURE `ensure_user_student_id_unique`()
BEGIN
    DECLARE duplicate_count INT DEFAULT 0;
    DECLARE unique_index_count INT DEFAULT 0;

    SELECT COUNT(*)
      INTO duplicate_count
      FROM (
          SELECT `student_id`
           FROM `user`
           WHERE `student_id` IS NOT NULL
           GROUP BY `student_id`
          HAVING COUNT(*) > 1
      ) duplicates;

    IF duplicate_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'user.student_id 存在重复值，请先处理重复学号后再执行迁移';
    END IF;

    SELECT COUNT(*)
      INTO unique_index_count
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'user'
       AND column_name = 'student_id'
       AND non_unique = 0;

    IF unique_index_count = 0 THEN
        ALTER TABLE `user`
            ADD UNIQUE KEY `uk_user_student_id` (`student_id`);
    END IF;
END//

DELIMITER ;

CALL `ensure_user_student_id_unique`();
DROP PROCEDURE `ensure_user_student_id_unique`;
