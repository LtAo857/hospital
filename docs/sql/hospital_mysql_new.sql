-- MySQL dump 10.13  Distrib 8.0.26, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: hospital_mysql
-- ------------------------------------------------------
-- Server version	8.0.26

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `doctor`
--

DROP TABLE IF EXISTS `doctor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `pid` char(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `school` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `degree` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `tel` char(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `email` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `job` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `remark` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `hiredate` date DEFAULT NULL,
  `tag` json DEFAULT NULL,
  `recommended` tinyint(1) DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_doctor_uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor`
--

LOCK TABLES `doctor` WRITE;
/*!40000 ALTER TABLE `doctor` DISABLE KEYS */;
INSERT INTO `doctor` VALUES (1,'程淳美','360201198609151112','2F0EB81AF9094277A958A41B59139DE1','女','/doctor/doctor-1.jpg','1986-09-15','重庆医科大学','博士','13593812535','北京市西城区','cheng@hospital.com','主任医师','专家','心脏外科专家','2025-05-24','[\"专家\", \"主任医师\"]',1,1,'2025-04-12 21:47:55'),(2,'阿斯顿','140227200106190519','68C1C15CB66D42498485DD389130D876','女',NULL,'2025-04-10','sd ','研究生','15110790629','阿斯顿','1asdas@qq.com','主任医师','123','123','2025-04-02','[]',1,1,'2025-04-13 18:22:57'),(3,'阿斯顿','140227200106190519','42B2243D4B634685AEC676E6A4284810','女',NULL,'2025-04-10','sd ','研究生','15110790629','阿斯顿','1asdas@qq.com','主任医师','123','123','2025-04-02','[]',1,1,'2025-04-13 18:23:08'),(4,'王五','110120198505050055','4F59E9D8459B48B2B31DBC4B632978ED','女','/doctor/doctor-4.jpg','2025-04-10','国防科技大学','研究生','15111111111','阿斯顿','15111111111@qq.com','主任医师','无','无','2025-04-02','[]',1,1,'2025-04-13 18:25:05'),(5,'阿斯顿','140211195505050055','CAC57020EF0B47C7BE7D6C74F1C5B542','男',NULL,'2025-04-03','阿斯顿','博士','15115115111','q','111@qq.com','主任医师','1','1','2025-04-02','[]',1,4,'2025-04-15 15:37:52');
/*!40000 ALTER TABLE `doctor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `doctor_consult`
--

DROP TABLE IF EXISTS `doctor_consult`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor_consult` (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_card_id` int DEFAULT NULL,
  `sub_dept_id` int DEFAULT NULL,
  `doctor_id` int DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `out_trade_no` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `amount` decimal(10,2) DEFAULT NULL,
  `prepay_id` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `transaction_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `payment_status` tinyint DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `files` json DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor_consult`
--

LOCK TABLES `doctor_consult` WRITE;
/*!40000 ALTER TABLE `doctor_consult` DISABLE KEYS */;
INSERT INTO `doctor_consult` VALUES (1,1,1,1,'2025-04-12 22:47:55','2025-04-12 23:47:55',NULL,150.00,NULL,NULL,2,1,NULL,'2025-04-12 21:47:55');
/*!40000 ALTER TABLE `doctor_consult` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `doctor_price`
--

DROP TABLE IF EXISTS `doctor_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor_price` (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int DEFAULT NULL,
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `price_1` decimal(10,2) DEFAULT NULL,
  `price_2` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor_price`
--

LOCK TABLES `doctor_price` WRITE;
/*!40000 ALTER TABLE `doctor_price` DISABLE KEYS */;
INSERT INTO `doctor_price` VALUES (1,1,'主任医师',80.00,200.00),(2,4,'科室主任',30.00,50.00);
/*!40000 ALTER TABLE `doctor_price` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `doctor_work_plan`
--

DROP TABLE IF EXISTS `doctor_work_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor_work_plan` (
  `id` int NOT NULL AUTO_INCREMENT,
  `doctor_id` int DEFAULT NULL,
  `dept_sub_id` int DEFAULT NULL,
  `date` date DEFAULT NULL,
  `maximum` smallint DEFAULT NULL,
  `num` smallint DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor_work_plan`
--

LOCK TABLES `doctor_work_plan` WRITE;
/*!40000 ALTER TABLE `doctor_work_plan` DISABLE KEYS */;
INSERT INTO `doctor_work_plan` VALUES (1,1,1,'2025-04-19',50,0),(2,1,1,'2025-04-13',45,0),(3,4,1,'2025-04-14',45,1),(4,1,1,'2025-06-14',45,0);
/*!40000 ALTER TABLE `doctor_work_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `doctor_work_plan_schedule`
--

DROP TABLE IF EXISTS `doctor_work_plan_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor_work_plan_schedule` (
  `id` int NOT NULL AUTO_INCREMENT,
  `work_plan_id` int DEFAULT NULL,
  `slot` tinyint DEFAULT NULL,
  `maximum` smallint DEFAULT NULL,
  `num` smallint DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor_work_plan_schedule`
--

LOCK TABLES `doctor_work_plan_schedule` WRITE;
/*!40000 ALTER TABLE `doctor_work_plan_schedule` DISABLE KEYS */;
INSERT INTO `doctor_work_plan_schedule` VALUES (1,1,1,10,0),(2,2,1,3,0),(3,2,2,3,0),(4,2,3,3,0),(5,2,4,3,0),(6,2,5,3,0),(7,2,6,3,0),(8,2,7,3,0),(9,2,8,3,0),(10,2,9,3,0),(11,2,10,3,0),(12,2,11,3,0),(13,2,12,3,0),(14,2,13,3,0),(15,2,14,3,0),(16,2,15,3,0),(17,3,1,3,0),(18,3,2,3,0),(19,3,3,3,0),(20,3,4,3,0),(21,3,5,3,0),(22,3,6,3,0),(23,3,7,3,1),(24,3,8,3,0),(25,3,9,3,0),(26,3,10,3,0),(27,3,11,3,0),(28,3,12,3,0),(29,3,13,3,0),(30,3,14,3,0),(31,3,15,3,0),(32,4,1,3,0),(33,4,2,3,0),(34,4,3,3,0),(35,4,4,3,0),(36,4,5,3,0),(37,4,6,3,0),(38,4,7,3,0),(39,4,8,3,0),(40,4,9,3,0),(41,4,10,3,0),(42,4,11,3,0),(43,4,12,3,0),(44,4,13,3,0),(45,4,14,3,0),(46,4,15,3,0);
/*!40000 ALTER TABLE `doctor_work_plan_schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `illness`
--

DROP TABLE IF EXISTS `illness`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `illness` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `cause` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `symptom` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `method` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `illness`
--

LOCK TABLES `illness` WRITE;
/*!40000 ALTER TABLE `illness` DISABLE KEYS */;
INSERT INTO `illness` VALUES (1,'白内障','各种原因如老化、遗传、局部营养障碍、免疫与代谢异常、外伤、中毒、辐射等，都能引起晶状体代谢紊乱，导致晶状体蛋白质变性而发生混浊。\n诱发因素\n增加白内障风险的因素包括：\n年龄增加；\n青光眼患者；\n高度近视者；\n糖尿病、半乳糖代谢障碍、钙代谢障碍等与代谢性白内障有关；\n强光刺激、过度日光照射；\n吸烟、酗酒；\n肥胖、营养不良；\n有眼部外伤史、炎症史或手术史；\n长期使用糖皮质激素、缩瞳剂等药物。','白内障的早期症状一般不明显，仅为轻度的视物模糊，患者可能误以为是老花眼或眼疲劳所致，极易漏诊。中期以后患者的晶状体混浊逐渐加重，视物模糊的程度也随之加重，并可能出现复斜视、近视、眩光等异常感觉。随病情发展，患者最终可完全失明。\n视物混浊、模糊，总是感到眼前朦胧感、雾蒙蒙感，这是最重要也最明显的症状。视力下降\n晶状体周边的混浊可以不影响视力，而在中央部的混浊，即使范围很小也会严重影响视力。\n患者在强光下时，由于瞳孔缩小，进入眼内的光线更少，视力反而不如在弱光下好。\n当晶状体严重混浊时，视力可降至仅有光感甚至失明。\n对比敏感度下降，视物模糊\n在日常生活中，人眼需要分辨边界清晰的物体，也需要分辨边界模糊的物体，后一种分辨能力则称为对比敏感度。部分白内障患者有可能视力下降不明显，但是对比敏感度显著下降，即视功能减退。\n屈光改变\n核性白内障患者的晶状体屈光能力增强，出现“核性近视”，对于老年人反而出现原有的老视减轻；\n发生白内障后，晶状体各部混浊程度不一，对光线的折射能力不同，可导致“晶状体性散光”。\n色觉改变\n混浊晶状体对蓝光吸收增强，使患眼对这些颜色的敏感度下降；此外，晶状体核颜色的改变也会影响色觉。\n其他症状\n单眼复视或多视；\n视野缺损；\n眩光，即对太阳光和灯光等亮光出现不适应，甚至面对强亮光时丧失视力。','白内障的治疗主要有药物和手术两种途径。\n药物治疗仅适用于少部分症状轻微、尚未达到手术标准的患者，或是因某些原因（例如严重的心脑血管疾病、麻醉药物过敏等）无法接受手术治疗的患者。\n手术是白内障的主要治疗方式，目的是切除已经混浊的晶状体，并植入人工晶体。目前的白内障手术治疗术式成熟、疗效较好、开展广泛，可作为患者的首选治疗方案。\n药物治疗\n目前临床上有多种抗白内障药物，但效果均不明确。\n辅助营养类药物：口服药物，包括维生素C、维生素E等，用于改善晶状体的营养障碍。\n抗氧化损伤药物：谷胱甘肽滴眼液，可用于初期的老年性白内障。\n其他：吡诺克辛滴眼液、苄达赖氨酸滴眼液等。\n手术治疗\n手术是治疗各型白内障的主要方式。\n手术适应证\n患者视力下降影响正常生活，术前检查无严重眼底疾病。\n术前准备\n完善前述的所有眼部相关检查。\n冲洗结膜囊和泪道，术前散瞳。','白内障是一种眼球中晶状体部位由于各种原因发生混浊导致视觉障碍的疾病。其常见类型包括老年性白内障、并发性白内障、外伤性白内障、代谢性白内障等，老年性白内障尤为常见。原因多种多样，如老化、遗传、局部营养障碍、免疫与代谢异常等。此外，暴露在强光下、吸烟酗酒、营养不良、长期使用糖皮质激素等都会增加白内障的风险。白内障是全球第一位致盲性眼病，特别是在老年人中较为常见。\n白内障的初期症状通常不明显，但随病情发展，会出现视物模糊、复视、近视、眩光等症状，并可能导致患者完全失明。提醒需要注意的是，白内障不具有传染性。\n白内障的主要治疗方法是手术，包括切除混浊的晶状体并植入人工晶体。目前白内障手术已经成熟，疗效良好。药物治疗仅适用于症状轻微、尚未达到手术标准的患者。在恢复期，患者需注意保持眼部清洁，预防感染，并按照医生的指导进行复查。术后视力的提高程度取决于眼底和角膜的状况。');
/*!40000 ALTER TABLE `illness` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `medical_dept`
--

DROP TABLE IF EXISTS `medical_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medical_dept` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `outpatient` tinyint(1) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `recommended` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medical_dept`
--

LOCK TABLES `medical_dept` WRITE;
/*!40000 ALTER TABLE `medical_dept` DISABLE KEYS */;
INSERT INTO `medical_dept` VALUES (1,'口腔科',1,'口腔科诊疗中心',1),(2,'侧睡',1,'1',1),(3,'阿斯顿',0,'阿斯顿',1),(4,'阿斯顿',1,'11',1),(5,'骨科',1,'骨科',1),(6,'测试1',1,'测试1',1);
/*!40000 ALTER TABLE `medical_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `medical_dept_sub`
--

DROP TABLE IF EXISTS `medical_dept_sub`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medical_dept_sub` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `dept_id` int DEFAULT NULL,
  `location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medical_dept_sub`
--

LOCK TABLES `medical_dept_sub` WRITE;
/*!40000 ALTER TABLE `medical_dept_sub` DISABLE KEYS */;
INSERT INTO `medical_dept_sub` VALUES (1,'口腔颌面外科',1,'1号楼2层A区'),(3,'骨科三诊',5,'骨科三诊1号楼');
/*!40000 ALTER TABLE `medical_dept_sub` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `medical_dept_sub_and_doctor`
--

DROP TABLE IF EXISTS `medical_dept_sub_and_doctor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medical_dept_sub_and_doctor` (
  `id` int NOT NULL AUTO_INCREMENT,
  `dept_sub_id` int DEFAULT NULL,
  `doctor_id` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medical_dept_sub_and_doctor`
--

LOCK TABLES `medical_dept_sub_and_doctor` WRITE;
/*!40000 ALTER TABLE `medical_dept_sub_and_doctor` DISABLE KEYS */;
INSERT INTO `medical_dept_sub_and_doctor` VALUES (1,1,1),(2,3,4),(3,3,5);
/*!40000 ALTER TABLE `medical_dept_sub_and_doctor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `medical_registration`
--

DROP TABLE IF EXISTS `medical_registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `medical_registration` (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_card_id` int DEFAULT NULL,
  `work_plan_id` int DEFAULT NULL,
  `doctor_schedule_id` int DEFAULT NULL,
  `doctor_id` int DEFAULT NULL,
  `dept_sub_id` int DEFAULT NULL,
  `date` date DEFAULT NULL,
  `slot` tinyint DEFAULT NULL,
  `amount` decimal(10,2) DEFAULT NULL,
  `out_trade_no` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `prepay_id` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `transaction_id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `payment_status` tinyint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_out_trade_no` (`out_trade_no`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medical_registration`
--

LOCK TABLES `medical_registration` WRITE;
/*!40000 ALTER TABLE `medical_registration` DISABLE KEYS */;
INSERT INTO `medical_registration` VALUES (1,1,1,NULL,1,1,'2025-04-19',1,80.00,NULL,NULL,NULL,1,'2025-04-12 21:47:55'),(2,2,3,23,4,1,'2025-04-14',7,11.00,'DC5DE2E7844449B49A2BD8A3DCD715B1','1',NULL,1,'2025-04-14 09:55:05');
/*!40000 ALTER TABLE `medical_registration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mis_action`
--

DROP TABLE IF EXISTS `mis_action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mis_action` (
  `id` smallint NOT NULL,
  `action_code` varchar(50) NOT NULL,
  `action_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mis_action`
--

LOCK TABLES `mis_action` WRITE;
/*!40000 ALTER TABLE `mis_action` DISABLE KEYS */;
INSERT INTO `mis_action` VALUES (1,'INSERT','添加'),(2,'DELETE','删除'),(3,'UPDATE','修改'),(4,'SELECT','查询'),(5,'APPROVAL','审批'),(6,'IMPORT','导入'),(7,'EXPORT','导出'),(8,'BACKUP','备份'),(9,'ARCHIVE','归档'),(10,'DIAGNOSE','诊断');
/*!40000 ALTER TABLE `mis_action` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mis_module`
--

DROP TABLE IF EXISTS `mis_module`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mis_module` (
  `id` smallint NOT NULL,
  `module_code` varchar(50) NOT NULL,
  `module_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mis_module`
--

LOCK TABLES `mis_module` WRITE;
/*!40000 ALTER TABLE `mis_module` DISABLE KEYS */;
INSERT INTO `mis_module` VALUES (1,'MIS_USER','MIS端用户管理'),(2,'PATIENT_USER','患者端用户管理'),(3,'WORKER_USER','医护端用户管理'),(4,'DEPT','部门管理'),(5,'MEDICAL_DEPT','医疗科室管理'),(6,'MEDICAL_DEPT_SUB','医疗诊室管理'),(7,'SCHEDULE','出诊管理'),(8,'REGISTRATION','挂号管理'),(9,'VIDEO_DIAGNOSE','视频问诊管理'),(10,'DOCTOR','医生管理'),(11,'NURSE','护士管理'),(12,'NURSING_ASSISTANT','护工管理'),(13,'DOCTOR_PRICE','诊费管理'),(14,'SYSTEM','系统管理');
/*!40000 ALTER TABLE `mis_module` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mis_permission`
--

DROP TABLE IF EXISTS `mis_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mis_permission` (
  `id` smallint NOT NULL,
  `permission_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `module_id` smallint DEFAULT NULL,
  `action_id` smallint DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mis_permission`
--

LOCK TABLES `mis_permission` WRITE;
/*!40000 ALTER TABLE `mis_permission` DISABLE KEYS */;
INSERT INTO `mis_permission` VALUES (0,'ROOT',0,0),(1,'MIS_USER:INSERT',1,1),(2,'MIS_USER:DELETE',1,2),(3,'MIS_USER:UPDATE',1,3),(4,'MIS_USER:SELECT',1,4),(5,'PATIENT_USER:INSERT',2,1),(6,'PATIENT_USER:DELETE',2,2),(7,'PATIENT_USER:UPDATE',2,3),(8,'PATIENT_USER:SELECT',2,4),(9,'WORKER_USER:INSERT',3,1),(10,'WORKER_USER:DELETE',3,2),(11,'WORKER_USER:UPDATE',3,3),(12,'WORKER_USER:SELECT',3,4),(13,'DEPT:INSERT',4,1),(14,'DEPT:DELETE',4,2),(15,'DEPT:UPDATE',4,3),(16,'DEPT:SELECT',4,4),(17,'MEDICAL_DEPT:INSERT',5,1),(18,'MEDICAL_DEPT:DELETE',5,2),(19,'MEDICAL_DEPT:UPDATE',5,3),(20,'MEDICAL_DEPT:SELECT',5,4),(21,'MEDICAL_DEPT_SUB:INSERT',6,1),(22,'MEDICAL_DEPT_SUB:DELETE',6,2),(23,'MEDICAL_DEPT_SUB:UPDATE',6,3),(24,'MEDICAL_DEPT_SUB:SELECT',6,4),(25,'SCHEDULE:INSERT',7,1),(26,'SCHEDULE:DELETE',7,2),(27,'SCHEDULE:UPDATE',7,3),(28,'SCHEDULE:SELECT',7,4),(29,'REGISTRATION:INSERT',8,1),(30,'REGISTRATION:DELETE',8,2),(31,'REGISTRATION:UPDATE',8,3),(32,'REGISTRATION:SELECT',8,4),(33,'VIDEO_DIAGNOSE:INSERT',9,1),(34,'VIDEO_DIAGNOSE:DELETE',9,2),(35,'VIDEO_DIAGNOSE:UPDATE',9,3),(36,'VIDEO_DIAGNOSE:SELECT',9,4),(37,'VIDEO_DIAGNOSE:DIAGNOSE',9,5),(38,'DOCTOR:INSERT',10,1),(39,'DOCTOR:DELETE',10,2),(40,'DOCTOR:UPDATE',10,3),(41,'DOCTOR:SELECT',10,4),(42,'NURSE:INSERT',11,1),(43,'NURSE:DELETE',11,2),(44,'NURSE:UPDATE',11,3),(45,'NURSE:SELECT',11,4),(46,'NURSING_ASSISTANT:INSERT',12,1),(47,'NURSING_ASSISTANT:DELETE',12,2),(48,'NURSING_ASSISTANT:UPDATE',12,3),(49,'NURSING_ASSISTANT:SELECT',12,4),(50,'DOCTOR_PRICE:INSERT',13,1),(51,'DOCTOR_PRICE:DELETE',14,2),(52,'DOCTOR_PRICE:UPDATE',15,3),(53,'DOCTOR_PRICE:SELECT',16,4),(54,'SYSTEM:UPDATE',16,3),(55,'SYSTEM:SELECT',16,4);
/*!40000 ALTER TABLE `mis_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mis_role`
--

DROP TABLE IF EXISTS `mis_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mis_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL,
  `remark` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mis_role`
--

LOCK TABLES `mis_role` WRITE;
/*!40000 ALTER TABLE `mis_role` DISABLE KEYS */;
INSERT INTO `mis_role` VALUES (0,'超级管理员','超级管理员'),(1,'医生','医生角色'),(2,'视频问诊医生','可以视频问诊的医生');
/*!40000 ALTER TABLE `mis_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mis_role_permission`
--

DROP TABLE IF EXISTS `mis_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mis_role_permission` (
  `id` int NOT NULL,
  `role_id` int DEFAULT NULL,
  `permission_id` smallint DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mis_role_permission`
--

LOCK TABLES `mis_role_permission` WRITE;
/*!40000 ALTER TABLE `mis_role_permission` DISABLE KEYS */;
INSERT INTO `mis_role_permission` VALUES (0,0,0),(1,1,4),(2,1,16),(3,1,20),(4,1,24),(5,1,28),(6,1,32),(7,2,36),(8,2,37);
/*!40000 ALTER TABLE `mis_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mis_user`
--

DROP TABLE IF EXISTS `mis_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mis_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `tel` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `dept_id` int DEFAULT NULL,
  `job` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `ref_id` int DEFAULT NULL,
  `status` tinyint DEFAULT NULL COMMENT '1有效，2离职，3禁用',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `mis_user_idx_1` (`username`) USING BTREE,
  KEY `mis_user_idx_2` (`dept_id`) USING BTREE,
  KEY `mis_user_idx_3` (`job`) USING BTREE,
  KEY `mis_user_idx_5` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mis_user`
--

LOCK TABLES `mis_user` WRITE;
/*!40000 ALTER TABLE `mis_user` DISABLE KEYS */;
INSERT INTO `mis_user` VALUES (1,'admin','061575f43e456772015c0032c0531edf','超级管理员','男',NULL,NULL,NULL,NULL,NULL,1,'2025-04-12 22:17:41'),(2,NULL,'061575f43e456772015c0032c0531edf',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(4,'asd','061575f43e456772015c0032c0531edf','阿斯顿','女','15110790629','110@qq.com',1,'医生',1,1,'2025-04-15 16:55:45'),(5,'gcm','123456','程淳美','女','13593812535','110@qq.com',1,'医生',1,1,'2025-04-15 18:26:21');
/*!40000 ALTER TABLE `mis_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mis_user_role`
--

DROP TABLE IF EXISTS `mis_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mis_user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `role_id` int DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `mis_user_role_idx_1` (`user_id`) USING BTREE,
  KEY `mis_user_role_idx_2` (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mis_user_role`
--

LOCK TABLES `mis_user_role` WRITE;
/*!40000 ALTER TABLE `mis_user_role` DISABLE KEYS */;
INSERT INTO `mis_user_role` VALUES (1,1,0),(2,1,1),(3,1,2);
/*!40000 ALTER TABLE `mis_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_face_auth`
--

DROP TABLE IF EXISTS `patient_face_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patient_face_auth` (
  `id` int NOT NULL AUTO_INCREMENT,
  `patient_card_id` int DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_face_auth`
--

LOCK TABLES `patient_face_auth` WRITE;
/*!40000 ALTER TABLE `patient_face_auth` DISABLE KEYS */;
INSERT INTO `patient_face_auth` VALUES (1,1,'2025-04-12 21:47:55'),(2,2,'2025-04-14 00:00:00');
/*!40000 ALTER TABLE `patient_face_auth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_user`
--

DROP TABLE IF EXISTS `patient_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patient_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `open_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `photo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_openid` (`open_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_user`
--

LOCK TABLES `patient_user` WRITE;
/*!40000 ALTER TABLE `patient_user` DISABLE KEYS */;
INSERT INTO `patient_user` VALUES (1,'oM7a25TrLxH2','患者昵称','/user/photo.jpg','男',1,'2025-04-12 21:47:55'),(2,'oYhEv4_-pu7QtCyS_x1pQ8WAiSoY','微信用户','https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132','男',1,'2025-04-13 20:05:32');
/*!40000 ALTER TABLE `patient_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_user_info_card`
--

DROP TABLE IF EXISTS `patient_user_info_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patient_user_info_card` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `sex` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `pid` char(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `tel` char(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `medical_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `insurance_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `exist_face_model` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_user_info_card`
--

LOCK TABLES `patient_user_info_card` WRITE;
/*!40000 ALTER TABLE `patient_user_info_card` DISABLE KEYS */;
INSERT INTO `patient_user_info_card` VALUES (1,1,'550e8400e29b41d4a716446655440000','张三','男','110101199003077832','13800138001','1990-03-07','[\"无\"]','城镇医保',1),(2,2,'da42e00492a5444a8322d6c4933cdf33','测试','男','110101199003077832','15110790629','1900-01-01','[\"无\"]','无',1);
/*!40000 ALTER TABLE `patient_user_info_card` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-06-13 11:52:34
