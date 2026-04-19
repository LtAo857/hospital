SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `multi_agent_registration_audit` (
  `id` int NOT NULL AUTO_INCREMENT,
  `session_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `request_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `user_id` int NULL DEFAULT NULL,
  `work_plan_id` int NULL DEFAULT NULL,
  `schedule_id` int NULL DEFAULT NULL,
  `doctor_id` int NULL DEFAULT NULL,
  `dept_sub_id` int NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `slot` tinyint NULL DEFAULT NULL,
  `amount` decimal(10, 2) NULL DEFAULT NULL,
  `out_trade_no` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `registration_id` int NULL DEFAULT NULL,
  `trace_json` json NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_request_id`(`request_id`) USING BTREE,
  INDEX `idx_status_updated_at`(`status`, `updated_at`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_schedule_id`(`schedule_id`) USING BTREE,
  INDEX `idx_out_trade_no`(`out_trade_no`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

UPDATE `medical_registration`
SET `payment_status` = 2
WHERE `payment_status` = 1;

SET FOREIGN_KEY_CHECKS = 1;
