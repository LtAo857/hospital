/*
 Navicat Premium Data Transfer

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : localhost:3306
 Source Schema         : hospital_mysql

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 13/04/2025 20:12:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for doctor
-- ----------------------------
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
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
  `status` tinyint(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_doctor_uuid`(`uuid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor
-- ----------------------------
INSERT INTO `doctor` VALUES (1, '程淳美', '360201198609151112', '2F0EB81AF9094277A958A41B59139DE1', '女', '/doctor/doctor-1.jpg', '1986-09-15', '重庆医科大学', '博士', '13593812535', '北京市西城区', 'cheng@hospital.com', '主任医师', '专家', '心脏外科专家', '2010-05-01', '[\"专家\", \"主任医师\"]', 1, 1, '2025-04-12 21:47:55');
INSERT INTO `doctor` VALUES (2, '阿斯顿', '110110110110110111', '68C1C15CB66D42498485DD389130D876', '女', NULL, '2025-04-10', 'sd ', '研究生', '15110790629', '阿斯顿', '1asdas@qq.com', '主任医师', '123', '123', '2025-04-02', '[]', 1, 1, '2025-04-13 18:22:57');
INSERT INTO `doctor` VALUES (3, '阿斯顿', '110110110110110111', '42B2243D4B634685AEC676E6A4284810', '女', NULL, '2025-04-10', 'sd ', '研究生', '15110790629', '阿斯顿', '1asdas@qq.com', '主任医师', '123', '123', '2025-04-02', '[]', 1, 1, '2025-04-13 18:23:08');
INSERT INTO `doctor` VALUES (4, '阿斯顿', '110110110110110111', '4F59E9D8459B48B2B31DBC4B632978ED', '女', NULL, '2025-04-10', 'sd ', '研究生', '15110790629', '阿斯顿', '1asdas@qq.com', '主任医师', '123', '123', '2025-04-02', '[]', 1, 1, '2025-04-13 18:25:05');

-- ----------------------------
-- Table structure for doctor_consult
-- ----------------------------
DROP TABLE IF EXISTS `doctor_consult`;
CREATE TABLE `doctor_consult`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `patient_card_id` int(0) NULL DEFAULT NULL,
  `sub_dept_id` int(0) NULL DEFAULT NULL,
  `doctor_id` int(0) NULL DEFAULT NULL,
  `start_time` datetime(0) NULL DEFAULT NULL,
  `end_time` datetime(0) NULL DEFAULT NULL,
  `out_trade_no` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `amount` decimal(10, 2) NULL DEFAULT NULL,
  `prepay_id` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `transaction_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `payment_status` tinyint(0) NULL DEFAULT NULL,
  `status` tinyint(0) NULL DEFAULT NULL,
  `files` json NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_consult
-- ----------------------------
INSERT INTO `doctor_consult` VALUES (1, 1, 1, 1, '2025-04-12 22:47:55', '2025-04-12 23:47:55', NULL, 150.00, NULL, NULL, 2, 1, NULL, '2025-04-12 21:47:55');

-- ----------------------------
-- Table structure for doctor_price
-- ----------------------------
DROP TABLE IF EXISTS `doctor_price`;
CREATE TABLE `doctor_price`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `doctor_id` int(0) NULL DEFAULT NULL,
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `price_1` decimal(10, 2) NULL DEFAULT NULL,
  `price_2` decimal(10, 2) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_price
-- ----------------------------
INSERT INTO `doctor_price` VALUES (1, 1, '主任医师', 80.00, 200.00);
INSERT INTO `doctor_price` VALUES (2, 4, '科室主任', 11.00, 11.00);

-- ----------------------------
-- Table structure for doctor_work_plan
-- ----------------------------
DROP TABLE IF EXISTS `doctor_work_plan`;
CREATE TABLE `doctor_work_plan`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `doctor_id` int(0) NULL DEFAULT NULL,
  `dept_sub_id` int(0) NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `maximum` smallint(0) NULL DEFAULT NULL,
  `num` smallint(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doctor_work_plan
-- ----------------------------
INSERT INTO `doctor_work_plan` VALUES (1, 1, 1, '2025-04-19', 50, 0);
INSERT INTO `doctor_work_plan` VALUES (2, 1, 1, '2025-04-13', 45, 0);
INSERT INTO `doctor_work_plan` VALUES (3, 4, 1, '2025-04-14', 45, 0);

-- ----------------------------
-- Table structure for doctor_work_plan_schedule
-- ----------------------------
DROP TABLE IF EXISTS `doctor_work_plan_schedule`;
CREATE TABLE `doctor_work_plan_schedule`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `work_plan_id` int(0) NULL DEFAULT NULL,
  `slot` tinyint(0) NULL DEFAULT NULL,
  `maximum` smallint(0) NULL DEFAULT NULL,
  `num` smallint(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

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
INSERT INTO `doctor_work_plan_schedule` VALUES (23, 3, 7, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (24, 3, 8, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (25, 3, 9, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (26, 3, 10, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (27, 3, 11, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (28, 3, 12, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (29, 3, 13, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (30, 3, 14, 3, 0);
INSERT INTO `doctor_work_plan_schedule` VALUES (31, 3, 15, 3, 0);

-- ----------------------------
-- Table structure for illness
-- ----------------------------
DROP TABLE IF EXISTS `illness`;
CREATE TABLE `illness`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `cause` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `symptom` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `method` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of illness
-- ----------------------------
INSERT INTO `illness` VALUES (1, '白内障', '原因', '症状', '方法', '白内障的描述');
INSERT INTO `illness` VALUES (2, '阿斯顿', '阿斯顿', '啊', '啊', '啊');

-- ----------------------------
-- Table structure for medical_dept
-- ----------------------------
DROP TABLE IF EXISTS `medical_dept`;
CREATE TABLE `medical_dept`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `outpatient` tinyint(1) NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `recommended` tinyint(1) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_dept
-- ----------------------------
INSERT INTO `medical_dept` VALUES (1, '口腔科', 1, '口腔科诊疗中心', 1);
INSERT INTO `medical_dept` VALUES (2, '侧睡', 1, '1', 1);
INSERT INTO `medical_dept` VALUES (3, '阿斯顿', 0, '阿斯顿', 1);
INSERT INTO `medical_dept` VALUES (4, '阿斯顿', 1, '11', 1);
INSERT INTO `medical_dept` VALUES (5, '测试', 1, '333', 0);

-- ----------------------------
-- Table structure for medical_dept_sub
-- ----------------------------
DROP TABLE IF EXISTS `medical_dept_sub`;
CREATE TABLE `medical_dept_sub`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `dept_id` int(0) NULL DEFAULT NULL,
  `location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_dept_sub
-- ----------------------------
INSERT INTO `medical_dept_sub` VALUES (1, '口腔颌面外科', 1, '1号楼2层A区');
INSERT INTO `medical_dept_sub` VALUES (3, '测试', 5, '测试');

-- ----------------------------
-- Table structure for medical_dept_sub_and_doctor
-- ----------------------------
DROP TABLE IF EXISTS `medical_dept_sub_and_doctor`;
CREATE TABLE `medical_dept_sub_and_doctor`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `dept_sub_id` int(0) NULL DEFAULT NULL,
  `doctor_id` int(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_dept_sub_and_doctor
-- ----------------------------
INSERT INTO `medical_dept_sub_and_doctor` VALUES (1, 1, 1);
INSERT INTO `medical_dept_sub_and_doctor` VALUES (2, 1, 4);

-- ----------------------------
-- Table structure for medical_registration
-- ----------------------------
DROP TABLE IF EXISTS `medical_registration`;
CREATE TABLE `medical_registration`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `patient_card_id` int(0) NULL DEFAULT NULL,
  `work_plan_id` int(0) NULL DEFAULT NULL,
  `doctor_schedule_id` int(0) NULL DEFAULT NULL,
  `doctor_id` int(0) NULL DEFAULT NULL,
  `dept_sub_id` int(0) NULL DEFAULT NULL,
  `date` date NULL DEFAULT NULL,
  `slot` tinyint(0) NULL DEFAULT NULL,
  `amount` decimal(10, 2) NULL DEFAULT NULL,
  `out_trade_no` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `prepay_id` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `transaction_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `payment_status` tinyint(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_out_trade_no`(`out_trade_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medical_registration
-- ----------------------------
INSERT INTO `medical_registration` VALUES (1, 1, 1, NULL, 1, 1, '2025-04-19', 1, 80.00, NULL, NULL, NULL, 1, '2025-04-12 21:47:55');

-- ----------------------------
-- Table structure for mis_action
-- ----------------------------
DROP TABLE IF EXISTS `mis_action`;
CREATE TABLE `mis_action`  (
  `id` smallint(0) NOT NULL,
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
  `id` smallint(0) NOT NULL,
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

-- ----------------------------
-- Table structure for mis_permission
-- ----------------------------
DROP TABLE IF EXISTS `mis_permission`;
CREATE TABLE `mis_permission`  (
  `id` smallint(0) NOT NULL,
  `permission_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `module_id` smallint(0) NULL DEFAULT NULL,
  `action_id` smallint(0) NULL DEFAULT NULL,
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

-- ----------------------------
-- Table structure for mis_role
-- ----------------------------
DROP TABLE IF EXISTS `mis_role`;
CREATE TABLE `mis_role`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `remark` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

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
  `id` int(0) NOT NULL,
  `role_id` int(0) NULL DEFAULT NULL,
  `permission_id` smallint(0) NULL DEFAULT NULL,
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

-- ----------------------------
-- Table structure for mis_user
-- ----------------------------
DROP TABLE IF EXISTS `mis_user`;
CREATE TABLE `mis_user`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `tel` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `dept_id` int(0) NULL DEFAULT NULL,
  `job` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ref_id` int(0) NULL DEFAULT NULL,
  `status` tinyint(0) NULL DEFAULT NULL COMMENT '1有效，2离职，3禁用',
  `create_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `mis_user_idx_1`(`username`) USING BTREE,
  INDEX `mis_user_idx_2`(`dept_id`) USING BTREE,
  INDEX `mis_user_idx_3`(`job`) USING BTREE,
  INDEX `mis_user_idx_5`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_user
-- ----------------------------
INSERT INTO `mis_user` VALUES (1, 'admin', '061575f43e456772015c0032c0531edf', '超级管理员', '男', NULL, NULL, NULL, NULL, NULL, 1, '2025-04-12 22:17:41');
INSERT INTO `mis_user` VALUES (2, NULL, '061575f43e456772015c0032c0531edf', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for mis_user_role
-- ----------------------------
DROP TABLE IF EXISTS `mis_user_role`;
CREATE TABLE `mis_user_role`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `user_id` int(0) NULL DEFAULT NULL,
  `role_id` int(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `mis_user_role_idx_1`(`user_id`) USING BTREE,
  INDEX `mis_user_role_idx_2`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mis_user_role
-- ----------------------------
INSERT INTO `mis_user_role` VALUES (1, 1, 0);
INSERT INTO `mis_user_role` VALUES (2, 1, 1);
INSERT INTO `mis_user_role` VALUES (3, 1, 2);

-- ----------------------------
-- Table structure for patient_face_auth
-- ----------------------------
DROP TABLE IF EXISTS `patient_face_auth`;
CREATE TABLE `patient_face_auth`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `patient_card_id` int(0) NULL DEFAULT NULL,
  `date` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of patient_face_auth
-- ----------------------------
INSERT INTO `patient_face_auth` VALUES (1, 1, '2025-04-12 21:47:55');

-- ----------------------------
-- Table structure for patient_user
-- ----------------------------
DROP TABLE IF EXISTS `patient_user`;
CREATE TABLE `patient_user`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `open_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` tinyint(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_openid`(`open_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

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
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `user_id` int(0) NULL DEFAULT NULL,
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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of patient_user_info_card
-- ----------------------------
INSERT INTO `patient_user_info_card` VALUES (1, 1, '550e8400e29b41d4a716446655440000', '张三', '男', '110101199003077832', '13800138000', '1990-03-07', '无重大疾病史', '城镇医保', 1);
INSERT INTO `patient_user_info_card` VALUES (2, 2, 'da42e00492a5444a8322d6c4933cdf33', '测试', '男', '140227200106190518', '15110790629', '1900-01-01', '[\"无\"]', '无', NULL);

SET FOREIGN_KEY_CHECKS = 1;
