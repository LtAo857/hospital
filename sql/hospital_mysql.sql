/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : localhost:3306
 Source Schema         : hospital_mysql

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 20/04/2026 09:47:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for doctor
-- ----------------------------
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pid` char(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `birthday` date NULL DEFAULT NULL,
  `school` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `degree` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tel` char(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `job` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `hiredate` date NULL DEFAULT NULL,
  `tag` json NULL,
  `recommended` tinyint(1) NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_doctor_uuid`(`uuid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor
-- ----------------------------
INSERT INTO `doctor` VALUES (1, '程淳美', '360201198609151112', '2F0EB81AF9094277A958A41B59139DE1', '女', 'doctor/doctor-1.jpg', '1986-09-15', '重庆医科大学', '博士', '13593812535', '北京市西城区', 'cheng@hospital.com', '主任医师', '专家', '女，1977年1月出生，毕业于山东大学临床医学专业，副主任医师。2005年至省人民医院普外科学习一年，2007年取得普外科职称。诊治范围：普外科大多数疾病，如甲状腺、乳腺、疝气、胃肠道疾病及胆道疾病。业务专长：腹腔镜胆囊切除术，腹股沟疝无张力修补术，甲状腺次全切除术。学术方面先后在省市级杂志上发表论文。', '2025-05-24', '[\"专家\", \"主任医师\"]', 1, 1, '2025-04-12 21:47:55');
INSERT INTO `doctor` VALUES (4, '王五', '110120198505050055', '4F59E9D8459B48B2B31DBC4B632978ED', '女', 'doctor/doctor-4.jpg', '2025-04-10', '国防科技大学', '研究生', '15111111111', '阿斯顿', '15111111111@qq.com', '主任医师', '专家', '男，1982年10月出生，毕业于石河子大学医学院临床医学专业，副主任医师。从事普外科临床工作10年，积累了丰富的临床工作经验，熟练常握了普外科常见病、多发病的诊治。擅长普外科疾病如胆囊结石、胆道结石症、胃十二指肠溃疡穿孔、胃肠道间质瘤、腹外疝、肠梗阻、阑尾炎、甲状腺、乳腺等常见病的诊治。', '2025-04-02', '[]', 1, 1, '2025-04-13 18:25:05');
INSERT INTO `doctor` VALUES (6, '赵六', '110120199909090099', 'E3DA595C39FE4841A9BC11FB38088906', '男', 'doctor/doctor-6.jpg', '2026-03-31', '医科大', '博士', '15111111111', '1', '1231@qq.com', '副主任医师', '专家', '男，2009年毕业于东南大学临床医学院，主治医师。从事普外科工作十年，积累丰富临床经验。擅长普外科常见病诊断治疗：乳腺、甲状腺、腹股沟疝、胆囊结石及其他普外科常见疾病诊治。', '2026-03-31', '[]', 1, 1, '2026-03-31 11:07:52');

-- ----------------------------
-- Table structure for doctor_consult
-- ----------------------------
DROP TABLE IF EXISTS `doctor_consult`;
CREATE TABLE `doctor_consult`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_card_id` int NULL DEFAULT NULL,
  `sub_dept_id` int NULL DEFAULT NULL,
  `doctor_id` int NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  `out_trade_no` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `amount` decimal(10, 2) NULL DEFAULT NULL,
  `prepay_id` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `transaction_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `payment_status` tinyint NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `files` json NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_consult
-- ----------------------------

-- ----------------------------
-- Table structure for doctor_evaluation
-- ----------------------------
DROP TABLE IF EXISTS `doctor_evaluation`;
CREATE TABLE `doctor_evaluation`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NOT NULL,
  `patient_card_id` int NOT NULL,
  `registration_id` int NULL DEFAULT NULL COMMENT '挂号ID',
  `video_diagnose_id` int NULL DEFAULT NULL COMMENT '视频问诊ID',
  `score` tinyint NOT NULL COMMENT '1-5星',
  `comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_doctor_id`(`doctor_id`) USING BTREE,
  INDEX `idx_registration_id`(`registration_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_evaluation
-- ----------------------------
INSERT INTO `doctor_evaluation` VALUES (1, 4, 2, 2, NULL, 4, '好', '2026-03-14 17:00:50');
INSERT INTO `doctor_evaluation` VALUES (2, 1, 2, 3, NULL, 4, '很好的医生', '2026-04-01 11:11:14');

-- ----------------------------
-- Table structure for doctor_prescription
-- ----------------------------
DROP TABLE IF EXISTS `doctor_prescription`;
CREATE TABLE `doctor_prescription`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `patient_card_id` int NULL DEFAULT NULL,
  `diagnosis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `sub_dept_id` int NULL DEFAULT NULL,
  `doctor_id` int NULL DEFAULT NULL,
  `registration_id` int NULL DEFAULT NULL,
  `rp` json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_registration_id_unique`(`registration_id`) USING BTREE,
  INDEX `idx_doctor_prescription_uuid`(`uuid`) USING BTREE,
  INDEX `idx_doctor_prescription_patient_card_id`(`patient_card_id`) USING BTREE,
  INDEX `idx_doctor_prescription_sub_dept_id`(`sub_dept_id`) USING BTREE,
  INDEX `idx_doctor_prescription_doctor_id`(`doctor_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of doctor_prescription
-- ----------------------------
INSERT INTO `doctor_prescription` VALUES (1, '1638AE1E0A73489E8C41C923DABEAB4F', 2, '腰间盘突出', 3, 4, 7, '[{\"num\": 4, \"name\": \"健胃消食片\", \"spec\": \"1\", \"method\": \"一日三次\"}]');

-- ----------------------------
-- Table structure for doctor_price
-- ----------------------------
DROP TABLE IF EXISTS `doctor_price`;
CREATE TABLE `doctor_price`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NULL DEFAULT NULL,
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `price_1` decimal(10, 2) NULL DEFAULT NULL,
  `price_2` decimal(10, 2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_price
-- ----------------------------
INSERT INTO `doctor_price` VALUES (1, 1, '主任医师', 80.00, 200.00);
INSERT INTO `doctor_price` VALUES (2, 4, '科室主任', 30.00, 50.00);
INSERT INTO `doctor_price` VALUES (3, 6, '科室主任', 20.00, 20.00);

-- ----------------------------
-- Table structure for doctor_work_plan
-- ----------------------------
DROP TABLE IF EXISTS `doctor_work_plan`;
CREATE TABLE `doctor_work_plan`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int NULL DEFAULT NULL,
  `dept_sub_id` int NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `maximum` smallint NULL DEFAULT NULL,
  `num` smallint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_work_plan
-- ----------------------------
INSERT INTO `doctor_work_plan` VALUES (1, 1, 1, '2025-04-19', 50, 0);
INSERT INTO `doctor_work_plan` VALUES (2, 1, 1, '2025-04-13', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (3, 4, 1, '2025-04-14', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (4, 1, 1, '2025-06-14', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (5, 1, 1, '2026-03-14', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (6, 1, 1, '2026-03-30', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (7, 6, 1, '2026-03-31', 30, 0);
INSERT INTO `doctor_work_plan` VALUES (8, 1, 1, '2026-04-01', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (9, 4, 3, '2026-04-01', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (10, 6, 1, '2026-04-01', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (11, 1, 1, '2026-04-02', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (12, 4, 3, '2026-04-09', 150, 1);
INSERT INTO `doctor_work_plan` VALUES (13, 4, 3, '2026-04-10', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (14, 6, 1, '2026-04-09', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (15, 1, 1, '2026-04-10', 150, 1);
INSERT INTO `doctor_work_plan` VALUES (16, 1, 1, '2026-04-12', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (17, 6, 1, '2026-04-12', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (18, 4, 3, '2026-04-12', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (19, 4, 3, '2026-04-20', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (20, 1, 1, '2026-04-20', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (21, 4, 3, '2026-04-21', 45, 1);
INSERT INTO `doctor_work_plan` VALUES (22, 1, 1, '2026-04-21', 45, 1);

-- ----------------------------
-- Table structure for doctor_work_plan_schedule
-- ----------------------------
DROP TABLE IF EXISTS `doctor_work_plan_schedule`;
CREATE TABLE `doctor_work_plan_schedule`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `work_plan_id` int NULL DEFAULT NULL,
  `slot` tinyint NULL DEFAULT NULL,
  `maximum` smallint NULL DEFAULT NULL,
  `num` smallint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 317 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_work_plan_schedule
-- ----------------------------
INSERT INTO `doctor_work_plan_schedule` VALUES (1, 1, 1, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (2, 2, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (3, 2, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (4, 2, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (5, 2, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (6, 2, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (7, 2, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (8, 2, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (9, 2, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (10, 2, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (11, 2, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (12, 2, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (13, 2, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (14, 2, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (15, 2, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (16, 2, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (17, 3, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (18, 3, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (19, 3, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (20, 3, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (21, 3, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (22, 3, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (23, 3, 7, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (24, 3, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (25, 3, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (26, 3, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (27, 3, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (28, 3, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (29, 3, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (30, 3, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (31, 3, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (32, 4, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (33, 4, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (34, 4, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (35, 4, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (36, 4, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (37, 4, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (38, 4, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (39, 4, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (40, 4, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (41, 4, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (42, 4, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (43, 4, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (44, 4, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (45, 4, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (46, 4, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (47, 5, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (48, 5, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (49, 5, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (50, 5, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (51, 5, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (52, 5, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (53, 5, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (54, 5, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (55, 5, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (56, 5, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (57, 5, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (58, 5, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (59, 5, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (60, 5, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (61, 5, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (62, 6, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (63, 6, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (64, 6, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (65, 6, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (66, 6, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (67, 6, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (68, 6, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (69, 6, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (70, 6, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (71, 6, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (72, 6, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (73, 6, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (74, 6, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (75, 6, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (76, 6, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (77, 7, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (78, 7, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (79, 7, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (80, 7, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (81, 7, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (82, 7, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (83, 7, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (84, 7, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (85, 7, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (86, 7, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (92, 8, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (93, 8, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (94, 8, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (95, 8, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (96, 8, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (97, 8, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (98, 8, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (99, 8, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (100, 8, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (101, 8, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (102, 8, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (103, 8, 12, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (104, 8, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (105, 8, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (106, 8, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (107, 9, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (108, 9, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (109, 9, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (110, 9, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (111, 9, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (112, 9, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (113, 9, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (114, 9, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (115, 9, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (116, 9, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (117, 9, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (118, 9, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (119, 9, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (120, 9, 14, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (121, 9, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (122, 10, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (123, 10, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (124, 10, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (125, 10, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (126, 10, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (127, 10, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (128, 10, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (129, 10, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (130, 10, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (131, 10, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (132, 10, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (133, 10, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (134, 10, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (135, 10, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (136, 10, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (137, 11, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (138, 11, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (139, 11, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (140, 11, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (141, 11, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (142, 11, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (143, 11, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (144, 11, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (145, 11, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (146, 11, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (147, 11, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (148, 11, 12, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (149, 11, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (150, 11, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (151, 11, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (152, 12, 1, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (153, 12, 2, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (154, 12, 3, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (155, 12, 4, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (156, 12, 5, 10, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (157, 12, 6, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (158, 12, 7, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (159, 12, 8, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (160, 12, 9, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (161, 12, 10, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (162, 12, 11, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (163, 12, 12, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (164, 12, 13, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (165, 12, 14, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (166, 12, 15, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (167, 13, 1, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (168, 13, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (169, 13, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (170, 13, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (171, 13, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (172, 13, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (173, 13, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (174, 13, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (175, 13, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (176, 13, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (177, 13, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (178, 13, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (179, 13, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (180, 13, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (181, 13, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (182, 14, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (183, 14, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (184, 14, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (185, 14, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (186, 14, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (187, 14, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (188, 14, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (189, 14, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (190, 14, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (191, 14, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (192, 14, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (193, 14, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (194, 14, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (195, 14, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (196, 14, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (197, 15, 1, 10, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (198, 15, 2, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (199, 15, 3, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (200, 15, 4, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (201, 15, 5, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (202, 15, 6, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (203, 15, 7, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (204, 15, 8, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (205, 15, 9, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (206, 15, 10, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (207, 15, 11, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (208, 15, 12, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (209, 15, 13, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (210, 15, 14, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (211, 15, 15, 10, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (212, 16, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (213, 16, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (214, 16, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (215, 16, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (216, 16, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (217, 16, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (218, 16, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (219, 16, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (220, 16, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (221, 16, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (222, 16, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (223, 16, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (224, 16, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (225, 16, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (226, 16, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (227, 17, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (228, 17, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (229, 17, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (230, 17, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (231, 17, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (232, 17, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (233, 17, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (234, 17, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (235, 17, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (236, 17, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (237, 17, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (238, 17, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (239, 17, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (240, 17, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (241, 17, 15, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (242, 18, 1, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (243, 18, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (244, 18, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (245, 18, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (246, 18, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (247, 18, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (248, 18, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (249, 18, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (250, 18, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (251, 18, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (252, 18, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (253, 18, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (254, 18, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (255, 18, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (256, 18, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (257, 19, 1, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (258, 19, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (259, 19, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (260, 19, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (261, 19, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (262, 19, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (263, 19, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (264, 19, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (265, 19, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (266, 19, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (267, 19, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (268, 19, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (269, 19, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (270, 19, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (271, 19, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (272, 20, 1, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (273, 20, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (274, 20, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (275, 20, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (276, 20, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (277, 20, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (278, 20, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (279, 20, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (280, 20, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (281, 20, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (282, 20, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (283, 20, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (284, 20, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (285, 20, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (286, 20, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (287, 21, 1, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (288, 21, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (289, 21, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (290, 21, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (291, 21, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (292, 21, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (293, 21, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (294, 21, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (295, 21, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (296, 21, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (297, 21, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (298, 21, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (299, 21, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (300, 21, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (301, 21, 15, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (302, 22, 1, 3, 1);
INSERT INTO `doctor_work_plan_schedule` VALUES (303, 22, 2, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (304, 22, 3, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (305, 22, 4, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (306, 22, 5, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (307, 22, 6, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (308, 22, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (309, 22, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (310, 22, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (311, 22, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (312, 22, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (313, 22, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (314, 22, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (315, 22, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (316, 22, 15, 3, 0);

-- ----------------------------
-- Table structure for illness
-- ----------------------------
DROP TABLE IF EXISTS `illness`;
CREATE TABLE `illness`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `cause` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `symptom` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `method` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of illness
-- ----------------------------
INSERT INTO `illness` VALUES (1, '白内障', '各种原因如老化、遗传、局部营养障碍、免疫与代谢异常、外伤、中毒、辐射等，都能引起晶状体代谢紊乱，导致晶状体蛋白质变性而发生混浊。\n诱发因素\n增加白内障风险的因素包括：\n年龄增加；\n青光眼患者；\n高度近视者；\n糖尿病、半乳糖代谢障碍、钙代谢障碍等与代谢性白内障有关；\n强光刺激、过度日光照射；\n吸烟、酗酒；\n肥胖、营养不良；\n有眼部外伤史、炎症史或手术史；\n长期使用糖皮质激素、缩瞳剂等药物。', '白内障的早期症状一般不明显，仅为轻度的视物模糊，患者可能误以为是老花眼或眼疲劳所致，极易漏诊。中期以后患者的晶状体混浊逐渐加重，视物模糊的程度也随之加重，并可能出现复斜视、近视、眩光等异常感觉。随病情发展，患者最终可完全失明。\n视物混浊、模糊，总是感到眼前朦胧感、雾蒙蒙感，这是最重要也最明显的症状。视力下降\n晶状体周边的混浊可以不影响视力，而在中央部的混浊，即使范围很小也会严重影响视力。\n患者在强光下时，由于瞳孔缩小，进入眼内的光线更少，视力反而不如在弱光下好。\n当晶状体严重混浊时，视力可降至仅有光感甚至失明。\n对比敏感度下降，视物模糊\n在日常生活中，人眼需要分辨边界清晰的物体，也需要分辨边界模糊的物体，后一种分辨能力则称为对比敏感度。部分白内障患者有可能视力下降不明显，但是对比敏感度显著下降，即视功能减退。\n屈光改变\n核性白内障患者的晶状体屈光能力增强，出现“核性近视”，对于老年人反而出现原有的老视减轻；\n发生白内障后，晶状体各部混浊程度不一，对光线的折射能力不同，可导致“晶状体性散光”。\n色觉改变\n混浊晶状体对蓝光吸收增强，使患眼对这些颜色的敏感度下降；此外，晶状体核颜色的改变也会影响色觉。\n其他症状\n单眼复视或多视；\n视野缺损；\n眩光，即对太阳光和灯光等亮光出现不适应，甚至面对强亮光时丧失视力。', '白内障的治疗主要有药物和手术两种途径。\n药物治疗仅适用于少部分症状轻微、尚未达到手术标准的患者，或是因某些原因（例如严重的心脑血管疾病、麻醉药物过敏等）无法接受手术治疗的患者。\n手术是白内障的主要治疗方式，目的是切除已经混浊的晶状体，并植入人工晶体。目前的白内障手术治疗术式成熟、疗效较好、开展广泛，可作为患者的首选治疗方案。\n药物治疗\n目前临床上有多种抗白内障药物，但效果均不明确。\n辅助营养类药物：口服药物，包括维生素C、维生素E等，用于改善晶状体的营养障碍。\n抗氧化损伤药物：谷胱甘肽滴眼液，可用于初期的老年性白内障。\n其他：吡诺克辛滴眼液、苄达赖氨酸滴眼液等。\n手术治疗\n手术是治疗各型白内障的主要方式。\n手术适应证\n患者视力下降影响正常生活，术前检查无严重眼底疾病。\n术前准备\n完善前述的所有眼部相关检查。\n冲洗结膜囊和泪道，术前散瞳。', '白内障是一种眼球中晶状体部位由于各种原因发生混浊导致视觉障碍的疾病。其常见类型包括老年性白内障、并发性白内障、外伤性白内障、代谢性白内障等，老年性白内障尤为常见。原因多种多样，如老化、遗传、局部营养障碍、免疫与代谢异常等。此外，暴露在强光下、吸烟酗酒、营养不良、长期使用糖皮质激素等都会增加白内障的风险。白内障是全球第一位致盲性眼病，特别是在老年人中较为常见。\n白内障的初期症状通常不明显，但随病情发展，会出现视物模糊、复视、近视、眩光等症状，并可能导致患者完全失明。提醒需要注意的是，白内障不具有传染性。\n白内障的主要治疗方法是手术，包括切除混浊的晶状体并植入人工晶体。目前白内障手术已经成熟，疗效良好。药物治疗仅适用于症状轻微、尚未达到手术标准的患者。在恢复期，患者需注意保持眼部清洁，预防感染，并按照医生的指导进行复查。术后视力的提高程度取决于眼底和角膜的状况。');

-- ----------------------------
-- Table structure for medical_dept
-- ----------------------------
DROP TABLE IF EXISTS `medical_dept`;
CREATE TABLE `medical_dept`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `outpatient` tinyint(1) NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `recommended` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_dept
-- ----------------------------
INSERT INTO `medical_dept` VALUES (1, '口腔科', 1, '口腔科诊疗中心', 1);
INSERT INTO `medical_dept` VALUES (5, '骨科', 1, '骨科', 1);

-- ----------------------------
-- Table structure for medical_dept_sub
-- ----------------------------
DROP TABLE IF EXISTS `medical_dept_sub`;
CREATE TABLE `medical_dept_sub`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `dept_id` int NULL DEFAULT NULL,
  `location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_dept_sub
-- ----------------------------
INSERT INTO `medical_dept_sub` VALUES (1, '口腔颌面外科', 1, '1号楼2层A区');
INSERT INTO `medical_dept_sub` VALUES (3, '骨科三诊', 5, '骨科三诊1号楼');

-- ----------------------------
-- Table structure for medical_dept_sub_and_doctor
-- ----------------------------
DROP TABLE IF EXISTS `medical_dept_sub_and_doctor`;
CREATE TABLE `medical_dept_sub_and_doctor`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `dept_sub_id` int NULL DEFAULT NULL,
  `doctor_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_dept_sub_and_doctor
-- ----------------------------
INSERT INTO `medical_dept_sub_and_doctor` VALUES (1, 1, 1);
INSERT INTO `medical_dept_sub_and_doctor` VALUES (2, 3, 4);
INSERT INTO `medical_dept_sub_and_doctor` VALUES (3, 3, 5);
INSERT INTO `medical_dept_sub_and_doctor` VALUES (4, 1, 5);
INSERT INTO `medical_dept_sub_and_doctor` VALUES (5, 1, 6);

-- ----------------------------
-- Table structure for medical_registration
-- ----------------------------
DROP TABLE IF EXISTS `medical_registration`;
CREATE TABLE `medical_registration`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_card_id` int NULL DEFAULT NULL,
  `work_plan_id` int NULL DEFAULT NULL,
  `doctor_schedule_id` int NULL DEFAULT NULL,
  `doctor_id` int NULL DEFAULT NULL,
  `dept_sub_id` int NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `slot` tinyint NULL DEFAULT NULL,
  `amount` decimal(10, 2) NULL DEFAULT NULL,
  `out_trade_no` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `prepay_id` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `transaction_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `payment_status` tinyint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_out_trade_no`(`out_trade_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_registration
-- ----------------------------
INSERT INTO `medical_registration` VALUES (1, 1, 1, NULL, 1, 1, '2025-04-19', 1, 80.00, NULL, NULL, NULL, 2, '2025-04-12 21:47:55');
INSERT INTO `medical_registration` VALUES (2, 2, 3, 23, 4, 1, '2025-04-14', 7, 11.00, 'DC5DE2E7844449B49A2BD8A3DCD715B1', '1', NULL, 2, '2025-04-14 09:55:05');
INSERT INTO `medical_registration` VALUES (3, 2, 8, 103, 1, 1, '2026-04-01', 12, 80.00, 'D202FAB3CF4D473EB5196C644F8F64DF', '1', NULL, 2, '2026-04-01 11:10:40');
INSERT INTO `medical_registration` VALUES (4, 2, 9, 120, 4, 3, '2026-04-01', 14, 30.00, 'EA4A18DF12924AFAB76AC2D3A1CC7FB2', '1', NULL, 2, '2026-04-01 11:13:26');
INSERT INTO `medical_registration` VALUES (5, 2, 11, 148, 1, 1, '2026-04-02', 12, 80.00, '2DA1D2F2279B4A80BCE784F0909830FF', '1', NULL, 2, '2026-04-01 11:15:52');
INSERT INTO `medical_registration` VALUES (6, 2, 12, 156, 4, 3, '2026-04-09', 5, 30.00, '33FB36BD26C847529113A4D832505C47', '1', NULL, 2, '2026-04-09 09:40:48');
INSERT INTO `medical_registration` VALUES (7, 2, 13, 167, 4, 3, '2026-04-10', 1, 30.00, '6F7536E048674FDC89CF5216259867D9', '1', NULL, 2, '2026-04-09 15:22:02');
INSERT INTO `medical_registration` VALUES (8, 2, 15, 197, 1, 1, '2026-04-10', 1, 80.00, '503B8530514C45C5B0C6E3DDA3D343DB', '1', NULL, 2, '2026-04-09 18:24:21');
INSERT INTO `medical_registration` VALUES (9, 2, 18, 242, 4, 3, '2026-04-12', 1, 30.00, '87E9A7E7830940CDBF185D4103162E64', '1', NULL, 2, '2026-04-11 17:47:54');
INSERT INTO `medical_registration` VALUES (10, 2, 17, 241, 6, 1, '2026-04-12', 15, 20.00, '828384524FD642CC8BD4DB4F94200A18', '1', NULL, 2, '2026-04-11 19:29:06');
INSERT INTO `medical_registration` VALUES (11, 2, 19, 257, 4, 3, '2026-04-20', 1, 30.00, 'F5470908A4114B5EB5A71734060334EA', '1', NULL, 2, '2026-04-19 14:29:36');
INSERT INTO `medical_registration` VALUES (12, 2, 21, 287, 4, 3, '2026-04-21', 1, 30.00, 'F2302AE1BB324A2C85F7A42F62E0B0EB', '1', NULL, 2, '2026-04-19 14:48:09');
INSERT INTO `medical_registration` VALUES (13, 2, 22, 302, 1, 1, '2026-04-21', 1, 80.00, 'A433F5BD77334A1BB3A1EEC1C4051483', '1', NULL, 2, '2026-04-19 18:40:27');

-- ----------------------------
-- Table structure for mis_action
-- ----------------------------
DROP TABLE IF EXISTS `mis_action`;
CREATE TABLE `mis_action`  (
  `id` smallint NOT NULL,
  `action_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `action_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_action
-- ----------------------------
INSERT INTO `mis_action` VALUES (1, 'INSERT', '添加');
INSERT INTO `mis_action` VALUES (2, 'DELETE', '删除');
INSERT INTO `mis_action` VALUES (3, 'UPDATE', '修改');
INSERT INTO `mis_action` VALUES (4, 'SELECT', '查询');
INSERT INTO `mis_action` VALUES (5, 'APPROVAL', '审批');
INSERT INTO `mis_action` VALUES (6, 'IMPORT', '导入');
INSERT INTO `mis_action` VALUES (7, 'EXPORT', '导出');
INSERT INTO `mis_action` VALUES (8, 'BACKUP', '备份');
INSERT INTO `mis_action` VALUES (9, 'ARCHIVE', '归档');
INSERT INTO `mis_action` VALUES (10, 'DIAGNOSE', '诊断');

-- ----------------------------
-- Table structure for mis_module
-- ----------------------------
DROP TABLE IF EXISTS `mis_module`;
CREATE TABLE `mis_module`  (
  `id` smallint NOT NULL,
  `module_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `module_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_module
-- ----------------------------
INSERT INTO `mis_module` VALUES (1, 'MIS_USER', 'MIS端用户管理');
INSERT INTO `mis_module` VALUES (2, 'PATIENT_USER', '患者端用户管理');
INSERT INTO `mis_module` VALUES (3, 'WORKER_USER', '医护端用户管理');
INSERT INTO `mis_module` VALUES (4, 'DEPT', '部门管理');
INSERT INTO `mis_module` VALUES (5, 'MEDICAL_DEPT', '医疗科室管理');
INSERT INTO `mis_module` VALUES (6, 'MEDICAL_DEPT_SUB', '医疗诊室管理');
INSERT INTO `mis_module` VALUES (7, 'SCHEDULE', '出诊管理');
INSERT INTO `mis_module` VALUES (8, 'REGISTRATION', '挂号管理');
INSERT INTO `mis_module` VALUES (9, 'VIDEO_DIAGNOSE', '视频问诊管理');
INSERT INTO `mis_module` VALUES (10, 'DOCTOR', '医生管理');
INSERT INTO `mis_module` VALUES (11, 'NURSE', '护士管理');
INSERT INTO `mis_module` VALUES (12, 'NURSING_ASSISTANT', '护工管理');
INSERT INTO `mis_module` VALUES (13, 'DOCTOR_PRICE', '诊费管理');
INSERT INTO `mis_module` VALUES (14, 'SYSTEM', '系统管理');
INSERT INTO `mis_module` VALUES (15, 'EVALUATION', '评价管理');
INSERT INTO `mis_module` VALUES (17, 'FAVORITE', '收藏管理');
INSERT INTO `mis_module` VALUES (18, 'PRESCRIPTION', '电子处方管理');

-- ----------------------------
-- Table structure for mis_permission
-- ----------------------------
DROP TABLE IF EXISTS `mis_permission`;
CREATE TABLE `mis_permission`  (
  `id` smallint NOT NULL,
  `permission_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `module_id` smallint NULL DEFAULT NULL,
  `action_id` smallint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_permission
-- ----------------------------
INSERT INTO `mis_permission` VALUES (0, 'ROOT', 0, 0);
INSERT INTO `mis_permission` VALUES (1, 'MIS_USER:INSERT', 1, 1);
INSERT INTO `mis_permission` VALUES (2, 'MIS_USER:DELETE', 1, 2);
INSERT INTO `mis_permission` VALUES (3, 'MIS_USER:UPDATE', 1, 3);
INSERT INTO `mis_permission` VALUES (4, 'MIS_USER:SELECT', 1, 4);
INSERT INTO `mis_permission` VALUES (5, 'PATIENT_USER:INSERT', 2, 1);
INSERT INTO `mis_permission` VALUES (6, 'PATIENT_USER:DELETE', 2, 2);
INSERT INTO `mis_permission` VALUES (7, 'PATIENT_USER:UPDATE', 2, 3);
INSERT INTO `mis_permission` VALUES (8, 'PATIENT_USER:SELECT', 2, 4);
INSERT INTO `mis_permission` VALUES (9, 'WORKER_USER:INSERT', 3, 1);
INSERT INTO `mis_permission` VALUES (10, 'WORKER_USER:DELETE', 3, 2);
INSERT INTO `mis_permission` VALUES (11, 'WORKER_USER:UPDATE', 3, 3);
INSERT INTO `mis_permission` VALUES (12, 'WORKER_USER:SELECT', 3, 4);
INSERT INTO `mis_permission` VALUES (13, 'DEPT:INSERT', 4, 1);
INSERT INTO `mis_permission` VALUES (14, 'DEPT:DELETE', 4, 2);
INSERT INTO `mis_permission` VALUES (15, 'DEPT:UPDATE', 4, 3);
INSERT INTO `mis_permission` VALUES (16, 'DEPT:SELECT', 4, 4);
INSERT INTO `mis_permission` VALUES (17, 'MEDICAL_DEPT:INSERT', 5, 1);
INSERT INTO `mis_permission` VALUES (18, 'MEDICAL_DEPT:DELETE', 5, 2);
INSERT INTO `mis_permission` VALUES (19, 'MEDICAL_DEPT:UPDATE', 5, 3);
INSERT INTO `mis_permission` VALUES (20, 'MEDICAL_DEPT:SELECT', 5, 4);
INSERT INTO `mis_permission` VALUES (21, 'MEDICAL_DEPT_SUB:INSERT', 6, 1);
INSERT INTO `mis_permission` VALUES (22, 'MEDICAL_DEPT_SUB:DELETE', 6, 2);
INSERT INTO `mis_permission` VALUES (23, 'MEDICAL_DEPT_SUB:UPDATE', 6, 3);
INSERT INTO `mis_permission` VALUES (24, 'MEDICAL_DEPT_SUB:SELECT', 6, 4);
INSERT INTO `mis_permission` VALUES (25, 'SCHEDULE:INSERT', 7, 1);
INSERT INTO `mis_permission` VALUES (26, 'SCHEDULE:DELETE', 7, 2);
INSERT INTO `mis_permission` VALUES (27, 'SCHEDULE:UPDATE', 7, 3);
INSERT INTO `mis_permission` VALUES (28, 'SCHEDULE:SELECT', 7, 4);
INSERT INTO `mis_permission` VALUES (29, 'REGISTRATION:INSERT', 8, 1);
INSERT INTO `mis_permission` VALUES (30, 'REGISTRATION:DELETE', 8, 2);
INSERT INTO `mis_permission` VALUES (31, 'REGISTRATION:UPDATE', 8, 3);
INSERT INTO `mis_permission` VALUES (32, 'REGISTRATION:SELECT', 8, 4);
INSERT INTO `mis_permission` VALUES (33, 'VIDEO_DIAGNOSE:INSERT', 9, 1);
INSERT INTO `mis_permission` VALUES (34, 'VIDEO_DIAGNOSE:DELETE', 9, 2);
INSERT INTO `mis_permission` VALUES (35, 'VIDEO_DIAGNOSE:UPDATE', 9, 3);
INSERT INTO `mis_permission` VALUES (36, 'VIDEO_DIAGNOSE:SELECT', 9, 4);
INSERT INTO `mis_permission` VALUES (37, 'VIDEO_DIAGNOSE:DIAGNOSE', 9, 5);
INSERT INTO `mis_permission` VALUES (38, 'DOCTOR:INSERT', 10, 1);
INSERT INTO `mis_permission` VALUES (39, 'DOCTOR:DELETE', 10, 2);
INSERT INTO `mis_permission` VALUES (40, 'DOCTOR:UPDATE', 10, 3);
INSERT INTO `mis_permission` VALUES (41, 'DOCTOR:SELECT', 10, 4);
INSERT INTO `mis_permission` VALUES (42, 'NURSE:INSERT', 11, 1);
INSERT INTO `mis_permission` VALUES (43, 'NURSE:DELETE', 11, 2);
INSERT INTO `mis_permission` VALUES (44, 'NURSE:UPDATE', 11, 3);
INSERT INTO `mis_permission` VALUES (45, 'NURSE:SELECT', 11, 4);
INSERT INTO `mis_permission` VALUES (46, 'NURSING_ASSISTANT:INSERT', 12, 1);
INSERT INTO `mis_permission` VALUES (47, 'NURSING_ASSISTANT:DELETE', 12, 2);
INSERT INTO `mis_permission` VALUES (48, 'NURSING_ASSISTANT:UPDATE', 12, 3);
INSERT INTO `mis_permission` VALUES (49, 'NURSING_ASSISTANT:SELECT', 12, 4);
INSERT INTO `mis_permission` VALUES (50, 'DOCTOR_PRICE:INSERT', 13, 1);
INSERT INTO `mis_permission` VALUES (51, 'DOCTOR_PRICE:DELETE', 14, 2);
INSERT INTO `mis_permission` VALUES (52, 'DOCTOR_PRICE:UPDATE', 15, 3);
INSERT INTO `mis_permission` VALUES (53, 'DOCTOR_PRICE:SELECT', 16, 4);
INSERT INTO `mis_permission` VALUES (54, 'SYSTEM:UPDATE', 16, 3);
INSERT INTO `mis_permission` VALUES (55, 'SYSTEM:SELECT', 16, 4);
INSERT INTO `mis_permission` VALUES (56, 'EVALUATION:SELECT', 15, 4);
INSERT INTO `mis_permission` VALUES (57, 'FAVORITE:SELECT', 17, 4);
INSERT INTO `mis_permission` VALUES (58, 'PRESCRIPTION:INSERT', 18, 1);
INSERT INTO `mis_permission` VALUES (59, 'PRESCRIPTION:UPDATE', 18, 3);
INSERT INTO `mis_permission` VALUES (60, 'PRESCRIPTION:SELECT', 18, 4);

-- ----------------------------
-- Table structure for mis_role
-- ----------------------------
DROP TABLE IF EXISTS `mis_role`;
CREATE TABLE `mis_role`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `remark` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_role
-- ----------------------------
INSERT INTO `mis_role` VALUES (0, '超级管理员', '超级管理员');
INSERT INTO `mis_role` VALUES (1, '医生', '医生角色');
INSERT INTO `mis_role` VALUES (2, '视频问诊医生', '可以视频问诊的医生');

-- ----------------------------
-- Table structure for mis_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `mis_role_permission`;
CREATE TABLE `mis_role_permission`  (
  `id` int NOT NULL,
  `role_id` int NULL DEFAULT NULL,
  `permission_id` smallint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_role_permission
-- ----------------------------
INSERT INTO `mis_role_permission` VALUES (0, 0, 0);
INSERT INTO `mis_role_permission` VALUES (1, 1, 4);
INSERT INTO `mis_role_permission` VALUES (2, 1, 16);
INSERT INTO `mis_role_permission` VALUES (3, 1, 20);
INSERT INTO `mis_role_permission` VALUES (4, 1, 24);
INSERT INTO `mis_role_permission` VALUES (5, 1, 28);
INSERT INTO `mis_role_permission` VALUES (6, 1, 32);
INSERT INTO `mis_role_permission` VALUES (7, 2, 36);
INSERT INTO `mis_role_permission` VALUES (8, 2, 37);
INSERT INTO `mis_role_permission` VALUES (9, 0, 56);
INSERT INTO `mis_role_permission` VALUES (10, 0, 57);
INSERT INTO `mis_role_permission` VALUES (11, 0, 58);
INSERT INTO `mis_role_permission` VALUES (12, 0, 59);
INSERT INTO `mis_role_permission` VALUES (13, 0, 60);
INSERT INTO `mis_role_permission` VALUES (14, 1, 58);
INSERT INTO `mis_role_permission` VALUES (15, 1, 59);
INSERT INTO `mis_role_permission` VALUES (16, 1, 60);

-- ----------------------------
-- Table structure for mis_user
-- ----------------------------
DROP TABLE IF EXISTS `mis_user`;
CREATE TABLE `mis_user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tel` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `dept_id` int NULL DEFAULT NULL,
  `job` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ref_id` int NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL COMMENT '1有效，2离职，3禁用',
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `mis_user_idx_1`(`username`) USING BTREE,
  INDEX `mis_user_idx_2`(`dept_id`) USING BTREE,
  INDEX `mis_user_idx_3`(`job`) USING BTREE,
  INDEX `mis_user_idx_5`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_user
-- ----------------------------
INSERT INTO `mis_user` VALUES (1, 'admin', '061575f43e456772015c0032c0531edf', '超级管理员', '男', NULL, NULL, NULL, NULL, NULL, 1, '2025-04-12 22:17:41');
INSERT INTO `mis_user` VALUES (7, 'wangwu', 'ba48c0b523a92b120c26cf1363dfcd31', '王五', '女', '15111111111', '15111111111@qq.com', NULL, '医生', 4, 1, '2026-03-29 16:35:02');
INSERT INTO `mis_user` VALUES (8, 'zhaoliu', '319b638bf4f4cc6a99f8532773e1a90a', '赵六', '男', '15111111111', '1231@qq.com', NULL, '医生', 6, 1, '2026-03-31 11:08:36');
INSERT INTO `mis_user` VALUES (9, 'chengchunmei', '4ca7221fae13063708f2e07e80e96e3e', '程淳美', '女', '13593812535', 'cheng@hospital.com', NULL, '医生', 1, 1, '2026-03-31 11:09:11');

-- ----------------------------
-- Table structure for mis_user_role
-- ----------------------------
DROP TABLE IF EXISTS `mis_user_role`;
CREATE TABLE `mis_user_role`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL DEFAULT NULL,
  `role_id` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `mis_user_role_idx_1`(`user_id`) USING BTREE,
  INDEX `mis_user_role_idx_2`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_user_role
-- ----------------------------
INSERT INTO `mis_user_role` VALUES (1, 1, 0);
INSERT INTO `mis_user_role` VALUES (2, 1, 1);
INSERT INTO `mis_user_role` VALUES (3, 1, 2);
INSERT INTO `mis_user_role` VALUES (4, 7, 1);
INSERT INTO `mis_user_role` VALUES (5, 7, 2);
INSERT INTO `mis_user_role` VALUES (6, 8, 1);
INSERT INTO `mis_user_role` VALUES (7, 8, 2);
INSERT INTO `mis_user_role` VALUES (8, 9, 1);
INSERT INTO `mis_user_role` VALUES (9, 9, 2);

-- ----------------------------
-- Table structure for multi_agent_registration_audit
-- ----------------------------
DROP TABLE IF EXISTS `multi_agent_registration_audit`;
CREATE TABLE `multi_agent_registration_audit`  (
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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of multi_agent_registration_audit
-- ----------------------------
INSERT INTO `multi_agent_registration_audit` VALUES (1, '1776595215502', 'E90B183799E237A7A454613EB2682285', 2, 22, 302, 1, 1, '2026-04-21', 1, 80.00, 'A433F5BD77334A1BB3A1EEC1C4051483', 'SUCCESS', NULL, NULL, 13, '{\"date\": \"2026-04-21\", \"slot\": 1, \"amount\": \"80.00\", \"userId\": 2, \"doctorId\": 1, \"deptSubId\": 1, \"requestId\": \"E90B183799E237A7A454613EB2682285\", \"sessionId\": \"1776595215502\", \"scheduleId\": 302, \"workPlanId\": 22}', '2026-04-19 18:40:27', '2026-04-19 18:40:27');

-- ----------------------------
-- Table structure for patient_doctor_favorite
-- ----------------------------
DROP TABLE IF EXISTS `patient_doctor_favorite`;
CREATE TABLE `patient_doctor_favorite`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_card_id` int NOT NULL,
  `doctor_id` int NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_patient_doctor_favorite`(`patient_card_id`, `doctor_id`) USING BTREE,
  INDEX `idx_patient_doctor_favorite_doctor_id`(`doctor_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of patient_doctor_favorite
-- ----------------------------
INSERT INTO `patient_doctor_favorite` VALUES (1, 2, 1, '2026-04-01 10:24:46');
INSERT INTO `patient_doctor_favorite` VALUES (2, 2, 6, '2026-04-01 10:25:01');
INSERT INTO `patient_doctor_favorite` VALUES (3, 2, 4, '2026-04-01 10:25:03');

-- ----------------------------
-- Table structure for patient_face_auth
-- ----------------------------
DROP TABLE IF EXISTS `patient_face_auth`;
CREATE TABLE `patient_face_auth`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_card_id` int NULL DEFAULT NULL,
  `date` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of patient_face_auth
-- ----------------------------
INSERT INTO `patient_face_auth` VALUES (1, 1, '2025-04-12 21:47:55');
INSERT INTO `patient_face_auth` VALUES (2, 2, '2025-04-14 00:00:00');

-- ----------------------------
-- Table structure for patient_message
-- ----------------------------
DROP TABLE IF EXISTS `patient_message`;
CREATE TABLE `patient_message`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '患者用户ID',
  `type` tinyint NOT NULL COMMENT '1挂号成功 2就诊提醒 3问诊结束 4收到评价回复',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `ref_id` int NULL DEFAULT NULL COMMENT '关联业务ID(挂号ID/问诊ID)',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '0未读 1已读',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of patient_message
-- ----------------------------
INSERT INTO `patient_message` VALUES (1, 2, 4, '评价成功', '您的评价已提交成功，感谢您的反馈', 1, 1, '2026-03-14 17:00:50');
INSERT INTO `patient_message` VALUES (2, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-01', 3, 1, '2026-04-01 11:10:40');
INSERT INTO `patient_message` VALUES (3, 2, 4, '评价成功', '您的评价已提交成功，感谢您的反馈', 2, 1, '2026-04-01 11:11:14');
INSERT INTO `patient_message` VALUES (4, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-01', 4, 1, '2026-04-01 11:13:26');
INSERT INTO `patient_message` VALUES (5, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-02', 5, 1, '2026-04-01 11:15:52');
INSERT INTO `patient_message` VALUES (6, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-09', 6, 1, '2026-04-09 09:40:48');
INSERT INTO `patient_message` VALUES (7, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-10', 7, 1, '2026-04-09 15:22:02');
INSERT INTO `patient_message` VALUES (8, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-10', 8, 1, '2026-04-09 18:24:21');
INSERT INTO `patient_message` VALUES (9, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-12', 9, 1, '2026-04-11 17:47:54');
INSERT INTO `patient_message` VALUES (10, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-12', 10, 1, '2026-04-11 19:29:06');
INSERT INTO `patient_message` VALUES (11, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-20', 11, 0, '2026-04-19 14:29:36');
INSERT INTO `patient_message` VALUES (12, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-21', 12, 0, '2026-04-19 14:48:09');
INSERT INTO `patient_message` VALUES (13, 2, 1, '挂号成功', '您已成功挂号，就诊日期：2026-04-21', 13, 0, '2026-04-19 18:40:27');
INSERT INTO `patient_message` VALUES (14, 2, 2, '就诊提醒', '您今天在骨科三诊有预约（王五医生），请按时就诊', 11, 0, '2026-04-20 08:00:00');

-- ----------------------------
-- Table structure for patient_user
-- ----------------------------
DROP TABLE IF EXISTS `patient_user`;
CREATE TABLE `patient_user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `open_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_openid`(`open_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of patient_user
-- ----------------------------
INSERT INTO `patient_user` VALUES (1, 'oM7a25TrLxH2', '患者昵称', '/user/photo.jpg', '男', 1, '2025-04-12 21:47:55');
INSERT INTO `patient_user` VALUES (2, 'oYhEv4_-pu7QtCyS_x1pQ8WAiSoY', '微信用户', 'https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132', '男', 1, '2025-04-13 20:05:32');

-- ----------------------------
-- Table structure for patient_user_info_card
-- ----------------------------
DROP TABLE IF EXISTS `patient_user_info_card`;
CREATE TABLE `patient_user_info_card`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NULL DEFAULT NULL,
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pid` char(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tel` char(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `birthday` date NULL DEFAULT NULL,
  `medical_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `insurance_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `exist_face_model` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of patient_user_info_card
-- ----------------------------
INSERT INTO `patient_user_info_card` VALUES (1, 1, '550e8400e29b41d4a716446655440000', '张三', '男', '110101199003077832', '13800138001', '1990-03-07', '[\"无\"]', '城镇医保', 1);
INSERT INTO `patient_user_info_card` VALUES (2, 2, 'da42e00492a5444a8322d6c4933cdf33', '测试', '男', '110101199003077832', '15111111111', '1900-01-01', '[\"脑中风\",\"白血病\",\"癫痫\",\"脑梗\",\"心脏病\"]', '其他', 1);

-- ----------------------------
-- Table structure for video_diagnose
-- ----------------------------
DROP TABLE IF EXISTS `video_diagnose`;
CREATE TABLE `video_diagnose`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_card_id` int NULL DEFAULT NULL,
  `doctor_id` int NULL DEFAULT NULL,
  `out_trade_no` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `amount` decimal(10, 2) NULL DEFAULT NULL,
  `payment_status` tinyint NULL DEFAULT NULL,
  `prepay_id` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `transaction_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `expect_start` datetime NULL DEFAULT NULL,
  `expect_end` datetime NULL DEFAULT NULL,
  `real_start` datetime NULL DEFAULT NULL,
  `real_end` datetime NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `video_diagnose_idx_1`(`patient_card_id`) USING BTREE,
  INDEX `video_diagnose_idx_2`(`doctor_id`) USING BTREE,
  INDEX `video_diagnose_idx_3`(`out_trade_no`) USING BTREE,
  INDEX `video_diagnose_idx_4`(`payment_status`) USING BTREE,
  INDEX `video_diagnose_idx_5`(`prepay_id`) USING BTREE,
  INDEX `video_diagnose_idx_6`(`transaction_id`) USING BTREE,
  INDEX `video_diagnose_idx_7`(`expect_start`) USING BTREE,
  INDEX `video_diagnose_idx_8`(`expect_end`) USING BTREE,
  INDEX `video_diagnose_idx_9`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of video_diagnose
-- ----------------------------

-- ----------------------------
-- Table structure for video_diagnose_file
-- ----------------------------
DROP TABLE IF EXISTS `video_diagnose_file`;
CREATE TABLE `video_diagnose_file`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `video_diagnose_id` int NULL DEFAULT NULL,
  `filename` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `path` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `video_diagnose_file_idx_1`(`video_diagnose_id`) USING BTREE,
  INDEX `video_diagnose_file_idx_2`(`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of video_diagnose_file
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
