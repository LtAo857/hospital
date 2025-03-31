CREATE SCHEMA hospital;
USE
hospital;
-- 疾病百科表 --------------------------------------------------
DROP TABLE IF EXISTS hospital.illness;
CREATE TABLE hospital.illness
(
    "id" INTEGER NOT NULL PRIMARY KEY,
"name"   VARCHAR(50),
"cause" VARCHAR(65535),
"symptom" VARCHAR(65535),
"method" VARCHAR(65535),
"description" VARCHAR(65535)
);

DROP SEQUENCE IF EXISTS hospital.illness_sequence;
CREATE SEQUENCE hospital.illness_sequence START WITH 1 increment BY 1;

UPSERT INTO hospital.illness("id","name","cause","symptom","method","description") VALUES(NEXT VALUE FOR hospital.illness_sequence,'白内障','原因', '症状', '方法','白内障的描述');

-- 患者端用户表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.patient_user;
CREATE TABLE hospital.patient_user
(
    "id"          INTEGER NOT NULL PRIMARY KEY,
    "open_id"     VARCHAR,
    "nickname"    VARCHAR(50),
    "photo"       VARCHAR,
    "sex"         VARCHAR(1),
--   状态：1代表正常，2代表禁用
    "status"      TINYINT,
    "create_time" DATE
);
DROP SEQUENCE IF EXISTS hospital.patient_user_sequence;
CREATE SEQUENCE hospital.patient_user_sequence START WITH 1 increment BY 1;
CREATE INDEX patient_user_idx_1 ON hospital.patient_user ("open_id");
CREATE INDEX patient_user_idx_2 ON hospital.patient_user ("status");


-- 患者端用户基本信息表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.patient_user_info_card;
CREATE TABLE hospital.patient_user_info_card
(
    "id"              INTEGER NOT NULL PRIMARY KEY,
    "user_id"         INTEGER,
    "uuid"            CHAR(32),
    "name"            VARCHAR(20),
    "sex"             VARCHAR(1),
    "pid"             CHAR(18),
    "tel"             CHAR(11),
    "birthday"        DATE,
    -- 疾病史
    "medical_history" VARCHAR,
    -- 保险类型
    "insurance_type"  VARCHAR,
--     是否人脸录入面部信息
    "exist_face_model" BOOLEAN
);

DROP SEQUENCE IF EXISTS hospital.patient_user_info_card_sequence;
CREATE SEQUENCE hospital.patient_user_info_card_sequence START WITH 1 increment BY 1;
CREATE INDEX patient_user_info_card_idx_1 ON hospital.patient_user_info_card ("user_id");
CREATE INDEX patient_user_info_card_idx_2 ON hospital.patient_user_info_card ("uuid");
CREATE INDEX patient_user_info_card_idx_3 ON hospital.patient_user_info_card ("pid");

-- 人脸认证记录表-----------------------------------------------
DROP TABLE IF EXISTS hospital.patient_face_auth;
CREATE TABLE hospital.patient_face_auth(
    "id" INTEGER NOT NULL PRIMARY KEY ,
    "patient_card_id" INTEGER,
    "date" DATE
);

DROP SEQUENCE IF EXISTS hospital.patient_face_auth_sequence;
CREATE SEQUENCE hospital.patient_face_auth_sequence START WITH 1 increment BY 1;
CREATE INDEX patient_face_auth_idx_1 ON hospital.patient_face_auth ("patient_card_id");
CREATE INDEX patient_face_auth_idx_2 ON hospital.patient_face_auth ("date");


-- 科室表 --------------------------------------------------
DROP TABLE IF EXISTS hospital.medical_dept;
CREATE TABLE hospital.medical_dept
(
    "id"          INTEGER NOT NULL PRIMARY KEY,
    "name"        VARCHAR(50),
    "outpatient"  BOOLEAN,
    "description" VARCHAR(500),
--     推荐在首页科室列表中展示的
    "recommended" BOOLEAN
);

DROP SEQUENCE IF EXISTS hospital.medical_dept_sequence;
CREATE SEQUENCE hospital.medical_dept_sequence START WITH 1 increment BY 1;

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'口腔科',true,
    '目前已经成为在国内外具有一定影响力的大型医疗科室，科室现有医护人员近70人，教授6人，副教授3人，博士研究生导师3人，硕士研究生导师4人，每年培养博士、硕士研究生近20余人。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'眼科',true,
    '全科共有医护人员及技师共67人，其中教授6人，副教授9人，讲师10人，助教4人，在职的29名医生全部获得硕士、博士学位，其中博士14人。下设眼底病、青光眼、白内障、眼外伤、飞秒激光治疗近视、眼眶病、斜视等多个专业学组。开展各种外路视网膜脱离复位术，玻璃体视网膜手术治疗视网膜脱离手术、复杂及二次视网膜脱离手术、黄斑裂孔性视网膜脱离手术等。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'耳鼻喉科',true,
    '科室设有耳科、鼻科、咽喉头颈外科3个专业组，配备听力检测、前庭功能检查、喉功能检查、 内窥镜检查及多导睡眠检查5个检查室，配备手术显微镜、鼻内镜系统、耳鼻喉动力系统、CO2激光治疗机等一批先进诊疗设备，编制床位共计100张。年门急诊患者量近10万人次，年住院3500余人次，年手术3000余例，学科综合实力位于国内先进行列。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'内科',true,
    '目前已经成为在国内外具有一定影响力的大型医疗科室，科室现有医护人员近70人，教授6人，副教授3人，博士研究生导师3人，硕士研究生导师4人，每年培养博士、硕士研究生近20余人。呼吸科年门诊量5万余人次，年收治各种呼吸系统疾病住院患者2000余人次，现有床位75张，其中内科重症监护室床位23张，长期承担内科重症患者的诊治工作。长期承担七年制、本科、留学生等医学班的内科学、诊断学、临床实习等授课。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'外科',true,
    '科室现有医生52名，教授8人，副教授9人。年均开展各类手术1000余例。其中腔镜微创手术占手术量的40％以上。近年接连开展胸外科各项领先技术，如3切口食管癌手术，全腔镜食管癌手术，腔镜下胸部单操作孔、腹部单孔食管癌手术等。科室承担国家自然科学基金等国家和部省级各类科研课题10余项，发表国际核心医学期刊SCI收录英文论文10余篇，国家级核心期刊中文论文60余篇。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'皮肤科',true,
    '皮肤科成立于1977年，现共有医生11人(副主任医师3名)，护士2名，其中8人具有硕士学位、2人具有博士学位。目前门诊量120000人次/年左右，是皮肤疾病主要的诊断、治疗场所。诊治的主要疾病包括儿童湿疹、皮炎、荨麻疹、药疹等过敏变态反应性疾病;水痘、手足口病、猩红热、传染性单核细胞增多症、疣、毛囊炎、脓疱疮、念珠菌性皮炎、足癣、体癣、疥疮等感染性皮肤病。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'妇科',true,
    '妇科现有职工89人，其中医生42人（博士16人，硕士26人），包括正高职（教授、主任医师）14人，省管专家2人，享受政府特殊津贴2人，博士生导师3人，硕士生导师11人。在女性复杂生殖道畸形矫治（先天性无阴道阴道成形术、Robert子宫宫腔镜矫形术、阴道斜隔综合征矫治术、特殊类型外阴阴道畸形矫治术、子宫纵隔切除术）有丰富的治疗经验，2019年在女性生殖系统重建的基础研究与临床应用方面的突出成就获得河北省科技进步一等奖。 ',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'儿科',true,
    '目前共有医护人员49 名，包括医师17人，护理人员32人。其中，主任医师2人，副主任医师2人，主治医师9人，具有硕士学位6人，在读硕士4人。儿科分成门诊及病区两个单元。儿科门诊常年24小时接诊，年门急诊量逐年提升，平均年门急诊量可达8万人次。每日输液量在80-100人次、最多时达到200-280人次。每日雾化量在50人次，最多时达到150人次。儿科始终围绕“以病人为中心，以质量为生命”这一主题，构建和谐科室，打造专业儿科团队，病人满意度不断提高，得到家长广泛的认可和尊重。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'神经科',true,
    '科室拥有一支专业的神经科诊断技术团队，常规开展神经影像学(3.0MRI、128排螺旋CT、DSA、TCD、ECT、OCT)、神经电生理（视频脑电图、动态脑电图、睡眠脑电图、肌电图、诱发电位、神经传导速度）、神经免疫、神经生化、神经病理、抗癫痫药物浓度检测、基因检测、脑脊液细胞学检查等工作，为神经系统疾病尤其是疑难杂症的诊断提供了良好的技术平台。每年开展颅内动脉瘤弹簧圈栓塞术200余例；颈内动脉支架植入术30余例；动脉取栓及支架植入20余例；微创颅内血肿清除引流术100余例。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'肿瘤科',true,
    '肿瘤内科建科于1964年，首批国家临床重点专科、卫计委癌痛规范化示范病房、国家临床药理机构肿瘤专业基地、中华医学会肿瘤学分会、中国临床肿瘤学会（CSCO）和中国研究型医院学会精准医学与肿瘤MDT专业委员会常委单位、教育部肿瘤学专业硕士及博士学位授予点。科室开展的治疗包括术前新辅助化疗、转化化疗、术后辅助化疗、姑息性化疗、根治性化疗、靶向治疗及免疫治疗等，并参与开展数十项国际、国内多中心临床试验。成立有肺癌、消化道肿瘤、乳腺癌、淋巴瘤、泌尿生殖系肿瘤、骨及皮肤软组织肉瘤、胃肠胰神经内分泌肿瘤和恶性黑色素瘤8个亚专业及相关MDT团队，重点是在MDT指导下依据组织或血液标本进行多基因检测，获得靶基因、基因多态性及通路基因的相关信息，实施规范化、个体化的综合治疗；治疗和学术水平达国内先进水平。',true);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'产科',true,
    '产科现有专业医护人员50余名，其中高级职称11人，硕士生导师3人，硕士 10余人，开放床位3个普通病区+1个LDR病区，共124张，备有单人间、双人间的母婴同室病房，还有温馨家庭病房16间。本科室在自然分娩、阴道侧切术、新式剖宫产术等方面积累了丰富的临床经验，具备危重产科病人抢救的技术和设备，全面开展了围产期保健、产前筛查、出生缺陷监测、优生遗传咨询、孕妇学校、都哈营养指导、育儿指导、康乐待产、康乐分娩、无痛分娩、新生儿疾病筛查、新生儿抚触、新生儿游泳、产后恢复保健、院后随访等一系列整体化、人性化的治疗及护理措施，擅长各种高危妊娠的筛查、监护和治疗，妊娠合并症及妊娠并发症的诊断及治疗。',false);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'骨科',true,
    '科室设置规范、布局合理，拥有专业医疗团队，本学科对骨科疾病的诊疗,特别是疑难、危重病人的诊治和抢救水平保持区内领先水平。科室拥有进口“C”臂机、全套进口关节镜器械、进口牵引床、进口电钻及磨钻、CPM关节康复器、骨折治疗仪等先进设备。目前开展复杂骨盆及髋臼的前后入路手术、颈胸腰椎的前后路手术、人工全髋全膝关节置换术、膝关节镜下滑膜清理、半月板成形、交叉韧带重建手术，经皮椎体后凸成形术（PKP术），闭合复位PFNA固定术，经皮插入钢板内固定治疗四肢骨折、微创腰椎融合术人工肱骨头、桡骨头置换术、断肢（指）再植术、脊柱侧弯后路矫形复位术、全髋关节翻修术等。',false);

UPSERT
INTO hospital.medical_dept("id","name","outpatient","description","recommended")
VALUES(NEXT VALUE FOR hospital.medical_dept_sequence,'血液科',true,
    '血液科经过几代人三四十年的奋斗，已发展成集医疗、教学、科研为一体的北京市重点专科。血液科聚焦于头颈部淋巴瘤的综合诊疗，尤其是鼻型结外NK/T细胞淋巴瘤、眼附属器淋巴瘤、原发眼内淋巴瘤、及好发于头颈部的其他类型造血系统肿瘤，同时覆盖其他专业患者面临的各种血液学问题，如贫血、血小板减少、出凝血障碍等。据统计，血液科每年初诊NK/T细胞淋巴瘤患者近100例，其他类型淋巴瘤患者200余例。',false);

-- 科室门诊表 --------------------------------------------------
DROP TABLE IF EXISTS hospital.medical_dept_sub;
CREATE TABLE hospital.medical_dept_sub(
  "id" INTEGER NOT NULL PRIMARY KEY ,
  "name" VARCHAR(50),
  "dept_id" INTEGER,
  "location" VARCHAR(50)
);

DROP SEQUENCE IF EXISTS hospital.medical_dept_sub_sequence;
CREATE SEQUENCE hospital.medical_dept_sub_sequence START WITH 1 increment BY 1;
CREATE INDEX medical_dept_sub_idx_1 ON hospital.medical_dept_sub ("dept_id");

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'口腔颌面外科',1,'1号楼2层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'口腔颌面内科',1,'1号楼2层B区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'眼科门诊',2,'1号楼3层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'白内障诊疗中心',2,'1号楼3层B区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'屈光中心门诊',2,'1号楼3层C区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'眼激光门诊',2,'1号楼3层D区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'耳鼻喉门诊',3,'1号楼3层E区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'内分泌门诊',4,'1号楼4层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'呼吸内科门诊',4,'1号楼4层B区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'心血管门诊',4,'1号楼4层C区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'消化内科门诊',4,'1号楼4层D区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'糖尿病门诊',4,'1号楼5层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'肾内科门诊',4,'1号楼5层B区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'风湿免疫门诊',4,'1号楼5层C区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'普通外科门诊',5,'1号楼5层D区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'胸外科门诊',5,'1号楼5层E区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'泌尿外科门诊',5,'1号楼6层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'心脏外科门诊',5,'1号楼5层B区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'整形外科门诊',5,'1号楼5层C区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'皮肤病门诊',6,'1号楼5层D区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'妇科门诊',7,'1号楼6层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'不孕病门诊',7,'1号楼6层B区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'儿科门诊',8,'1号楼6层C区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'神经内科门诊',9,'1号楼7层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'神经外科门诊',9,'1号楼7层B区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'肿瘤科门诊',10,'2号楼2层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'产科门诊',11,'2号楼3层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'骨科门诊',12,'2号楼4层A区');

UPSERT INTO hospital.medical_dept_sub("id","name","dept_id","location")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_sequence,'血液科门诊',13,'2号楼4层B区');

-- 医生表 --------------------------------------------------
DROP TABLE IF EXISTS hospital.doctor;
CREATE TABLE hospital.doctor
(
    "id"          INTEGER NOT NULL PRIMARY KEY,
    "name"        VARCHAR(20),
    "pid"         CHAR(18),
    "uuid"        CHAR(32),
    "sex"         VARCHAR(1),
    "photo"       VARCHAR,
    "birthday"    DATE,
    "school"      VARCHAR(50),
    "degree"      VARCHAR(20),
    "tel"         CHAR(11),
    "address"     VARCHAR(200),
    "email"       VARCHAR(200),
    "job"         VARCHAR(20),
    "remark"      VARCHAR(50),
    "description" VARCHAR,
    "hiredate"    DATE,
    "tag"         VARCHAR,
    "recommended" BOOLEAN,
    -- 1在职，2离职，3退休，4隐藏（逻辑删除）
    "status" TINYINT,
    "create_time" DATE
);

DROP SEQUENCE IF EXISTS hospital.doctor_sequence;
CREATE SEQUENCE hospital.doctor_sequence START WITH 1 increment BY 1;
CREATE INDEX doctor_idx_1 ON hospital.doctor ("pid");
CREATE INDEX doctor_idx_2 ON hospital.doctor ("uuid");

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '程淳美', '360201198609151112','2F0EB81AF9094277A958A41B59139DE1', '女', '/doctor/doctor-1.jpg',
             '1968-05-03', '重庆医科大学', '博士', '13593812535', '北京市西城区北三环中路14-1号','chengchunmei@hospital.com', '主任医师', '首都医科大学博士生导师',
             '擅长诊疗：心脏血管外科，包括风心病瓣膜替换，先心病，大血管疾病外科治疗。特色为冠状动脉外科，1990年以来在院内、外主做冠状动脉搭桥手术近千例，较早引进了世界最新的搭桥技术、非体外循环下冠状动脉搭桥术，获得了良好的效果 。',
             '2004-02-15', '["从业41年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '秦欣源', '460201197611302855','F1FDE764A9BB405596895722F1CCDB06', '男', '/doctor/doctor-2.jpg',
             '1959-05-03', '中国医科大学', '博士', '15179382777', '北京市海淀区龙翔路9号', 'qinxinyuan@hospital.com', '主任医师', '陆军军医大学研究生导师',
             '擅长诊疗：下肢静脉曲张的微创治疗，多种微创方法综合治疗下肢静脉曲张，包括大隐静脉激光治疗、腔内 射频治疗，Trivex透光旋切、泡沫硬化剂注射治疗，对不同曲张静脉特点选择针对性微创方法，创伤小，恢复快。',
             '2004-12-11', '["从业46年","领域专家","快速回复"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '熊佳钰','370101197707304145', '2AE43F717E444031BC0CBB5878932B07', '男', '/doctor/doctor-3.jpg',
             '1976-11-28', '北京协和医学院', '博士', '18658678090', '北京市朝阳区三里屯路北1楼', 'xiongjiayu@hospital.com', '主任医师', '国家远程医疗医学中心主任委员',
             '擅长诊疗：慢性咳嗽、喘息性/呼吸困难性疾病如支气管哮喘、慢性阻塞性肺疾病、弥漫性肺疾病、肺部肿瘤、呼吸疑难危重症及胸膜疾病的诊断与治疗。主要临床研究方向：慢性气道炎症性疾病的基础与临床以及肺部疾病的介入性诊断与治疗。',
             '2005-08-04', '["从业27年","领域专家","快速回复"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '孟明远','370101197707304145', '50595ADEF85C4B35A114A462B0FA0CDA', '男', '/doctor/doctor-4.jpg',
             '1977-06-14', '北京协和医学院', '博士', '14580412494', '北京市海淀区花园东路8号院', 'mengmingyuan@hospital.com', '主任医师', '北京医科大学研究生导师',
             '擅长诊疗：面神经修复与面部整形重建。在国内很早开展咬肌神经吻合、多面神经重建、游离股薄肌移植等面瘫治疗手术，填补面瘫治疗领域多项技术空白。在面部整形重建、显微外科组织瓣移植修复等领域积累了丰富经验。',
             '2005-08-04', '["从业27年","领域专家","快速回复"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '方嘉怡','520201198509071764', 'B762C0BF9F994D23B5695EA78AE3F4F7', '女', '/doctor/doctor-5.jpg',
             '1978-12-31', '北京协和医学院', '博士', '15597529530', '北京市西城区大乘巷1号','fangjiayi@hospital.com',
             '主任医师', '北京医科大学、北京中医药大学研究生导师',
             '擅长诊疗：泌尿系肿瘤，特别是肾肿瘤，肾上腺肿瘤，尿路上皮肿瘤(肾盂、输尿管、膀胱肿瘤)以及前列腺恶性肿瘤的微创治疗，膀胱及前列腺良性疾病，骶神经调控等。',
             '2005-08-04', '["从业24年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '黄涛','500101200212123472', '9718C444BE3646818DD264FB26EC8181', '男', '/doctor/doctor-6.jpg',
             '1974-01-07', '北京协和医学院', '博士', '17723959830', '北京市西城区滨河里34号','huangtao@hospital.com',
             '主任医师', '北京医科大学硕士研究生导师',
             '擅长诊疗：临床常见恶性肿瘤的放射治疗：1.乳腺癌 2.消化系统恶性肿瘤如食管癌、直肠癌 3.头颈部恶性肿瘤 4.肺癌 5.妇科肿瘤。临床研究方向：乳腺癌个体化放射治疗，放射治疗后正常组织损伤的预防。',
             '2005-08-04', '["从业26年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '武萌萌','620101197707093458', '126A2D95DF2E42E4BD093FB9299623FB', '女', '/doctor/doctor-7.jpg',
             '1977-04-04', '解放军第三军医大学', '博士', '18362319314', '北京市海淀区复兴路12号8楼','wumengmeng@hospital.com',
             '主任医师', '中国医师协会微无创专业委员会委员',
             '擅长诊疗：青光眼和白内障的临床诊断及治疗。对各型青光眼的诊断和疑难杂症的处理有独到见解，特别对青光眼的诊断、激光、药物和手术治疗有深入的研究。注重青光眼的早期发现和早期治疗，对青光眼患者的个体化治疗进行了深入的研究',
             '2005-08-04', '["从业26年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '田芳','130201200402256643', 'A1F9664A527F4DCBA48ADF312AFBC421', '女', '/doctor/doctor-8.jpg',
             '1972-07-28', '广州医科大学', '博士', '18576200235', '北京市海淀区太平路22号','tianfang@hospital.com',
             '主任医师', '中国医药教育协会肿瘤专家委员会委员',
             '擅长诊疗：头颈肿瘤的外科及综合治疗（甲状腺癌、喉癌下咽癌、涎腺肿瘤、鼻腔鼻窦良恶性肿瘤、舌癌、咽旁颅底肿瘤）。对鼻窦内窥镜外科、喉内镜手术及声显微手术、耳显微手术、鼾症手术及综合治疗有丰富的临床经验。',
             '2005-08-04', '["从业31年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '马杰','420101199510078280', '3D3F7F2204204E30AD2F23C28A569B9A', '男', '/doctor/doctor-9.jpg',
             '1977-02-14', '哈尔滨医科大学', '博士', '13822560280', '北京市西城区车站西街15号院-5号楼','majie@hospital.com',
             '主任医师', '北京医师协会皮肤病专业专家委员会委员',
             '擅长诊疗：以皮肤病理为专长，擅长常见皮肤病如：湿疹，药疹，蕁麻疹，银屑病，扁平苔癣等;色素性皮肤病如：各种色素斑、痣，白癜风等;感染性皮肤病及性病(梅毒、淋病、尖锐湿疣、非淋菌性尿道炎)等。',
             '2005-08-04', '["从业22年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '杜佳玉','510101198806215034', 'CD2C65C455564181ADFF84BD6A2F35C7', '女', '/doctor/doctor-10.jpg',
             '1978-06-22', '南京医科大学', '研究生', '19738130796', '北京市丰台区望园东路2928号','dujiayu@hospital.com',
             '主治医师', '参加多项国家级、省部级多项科研课题',
             '擅长诊疗：应用中西医优势互补方法治疗糖尿病及其并发症(糖尿病心脑血管病、糖尿病肾病、糖尿病胃轻瘫、糖尿病周围神经病变、血糖难控因素)以及代谢综合征。',
             '2005-08-04', '["从业17年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '邓国栋','530201199301048406', 'FFBA296720C8495785E8A78B379C9B05', '男', '/doctor/doctor-11.jpg',
             '1975-11-11', '天津医科大学', '博士', '13777571218', '北京市石景山区重聚路40号院-3号','dengguodong@hospital.com',
             '副主任医师', '北京医师协会风湿免疫专科分会理事',
             '擅长诊疗：系统性红斑狼疮、多发性肌炎、皮肌炎、类风湿关节炎、痛风、强直性脊柱炎、系统性血管炎等风湿免疫疾病，对疑难复杂危重风湿免疫疾病具备丰富诊疗经验。临床研究方向为多发性肌炎和皮肌炎，系统性红斑狼疮，痛风，风湿免疫疾病心血管系统受累等。',
             '2005-08-04', '["从业19年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '龙泽远','120201198705219290', '0255BFF8CCC1479C898E21D1D3B0A8E7', '男', '/doctor/doctor-12.jpg',
             '1978-12-16', '中国医科大学', '研究生', '13069020752', '北京市海淀区玉泉路16号院','longzeyuan@hospital.com',
             '副主治医师', '参与多项国家自然科学基金课题研究',
             '擅长诊疗：多发性肌炎，皮肌炎，系统性红斑狼疮，类风湿关节炎等多种风湿免疫疾病诊治。对肌炎合并间质性肺疾病有深入研究。',
             '2005-08-04', '["从业15年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '宋秀英','650201198402246623', '0370428B5452441C9F64658F2B7BC7F1', '女', '/doctor/doctor-13.jpg',
             '1970-12-16', '中国医科大学', '博士', '15977965686', '北京市西城区马连道南街1号院','songxiuying@hospital.com',
             '主治医师', '中华医学会风湿病分会会员',
             '擅长诊疗：从事风湿免疫疾病临床诊断、治疗工作20余年，有着丰富的临床经验，对风湿病重症、疑难症及长期不明原因的发热病例的诊治都具有较高的水平。',
             '2005-08-04', '["从业28年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '薛榕润','450201198007308399', '6BD7AB9AE6AD417A90042FF3536ECC6C', '男', '/doctor/doctor-14.jpg',
             '1971-01-07', '中国医科大学', '博士', '15589198858', '北京市石景山区八角南路19号楼','xuerongrun@hospital.com',
             '主治医师', '北京市泌尿外科分会结石感染组委员',
             '擅长诊疗：经皮肾镜、输尿管镜微创治疗肾结石、输尿管结石、膀胱结石;肾上腺、肾、输尿管、膀胱肿瘤及疾病等微创治疗;肾盂输尿管狭窄,输尿管狭窄及尿道狭窄的微创治疗;前列腺增生、前列腺肿瘤的诊断治疗。',
             '2005-08-04', '["从业26年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '谭尚','610201197909271420', '6B4A32C097BA44F1B052B6F85C2D3E7B', '男', '/doctor/doctor-15.jpg',
             '1968-01-07', '南京医科大学', '博士', '13923984769', '北京市丰台区久敬庄路乙1号','tanshang@hospital.com',
             '主治医师', '北京医科大学硕士研究生导师',
             '擅长诊疗：熟练掌握胸外科专业各类疾病的诊断、治疗，特别对肺癌的根治性手术，食管癌的根治性手术，纵隔肿瘤、胸壁恶性肿瘤的根治性手术及综合治疗。纤支镜、纵隔镜的检查及治疗，胸腔镜的各类微创手术治疗及目前开展胸腔镜下肺癌根治术。',
             '2005-08-04', '["从业36年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '任振国','420201198903179411', '43E06B95BD364ACD890C73D91D9881BF', '男', '/doctor/doctor-16.jpg',
             '1972-03-17', '首都医科大学', '博士', '18068672244', '北京市朝阳区东三环北路辛2号','renzhenguo@hospital.com',
             '主治医师', '北京口腔临床技术研究会理事',
             '擅长诊疗：成人正畸、隐形正畸、牙周病正畸、多学科联合治疗、儿童错合畸形的早期正畸治疗、骨性错颌畸形的综合治疗。',
             '2005-08-04', '["从业29年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '许靖琪','220101200306063805', 'DDAF4F5F849B4D2AB6DB8CA442794A5C', '女', '/doctor/doctor-17.jpg',
             '1973-05-08', '首都医科大学', '博士', '17267270501', '北京市东城区和平里北街21号','xujingqi@hospital.com',
             '主治医师', '北京口腔临床技术研究会理事',
             '擅长诊疗：擅长龋齿、牙髓病和根尖周病的诊断与系统治疗；牙周病及牙槽外科手术；牙体的美学修复。',
             '2005-08-04', '["从业26年","领域专家","温暖贴心"]', true,1,NOW());

UPSERT
INTO hospital.doctor( "id", "name","pid" ,"uuid", "sex", "photo", "birthday", "school", "degree", "tel", "address", "email", "job", "remark", "description", "hiredate", "tag","recommended","status", "create_time")
VALUES (NEXT VALUE FOR hospital.doctor_sequence, '吕成龙','630201198312155601', '6B946B8B0C4A42DA8DE05E62A6CDE8E6', '男', '/doctor/doctor-18.jpg',
             '1974-12-24', '北京大学口腔医学院', '博士', '13773287399', '北京市东城区北新桥三条甲58号','lvchenglong@hospital.com',
             '主治医师', '北京口腔临床技术研究会理事',
             '擅长诊疗：口腔科常见病、多发病的诊疗工作，包括牙体牙髓病、牙周病、牙槽外科及口腔修复的相关疾病的全面设计与治疗。',
             '2005-08-04', '["从业25年","领域专家","温暖贴心"]', true,1,NOW());

-- 医生挂号费表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.doctor_price;
CREATE TABLE hospital.doctor_price
(
    "id"        INTEGER NOT NULL PRIMARY KEY,
    "doctor_id" INTEGER,
    "level"     VARCHAR(20),
    "price_1"   DECIMAL(10,2),
    "price_2"   DECIMAL(10,2)
);

DROP SEQUENCE IF EXISTS hospital.doctor_price_sequence;
CREATE SEQUENCE hospital.doctor_price_sequence START WITH 1 increment BY 1;
CREATE INDEX doctor_price_idx_1 ON hospital.doctor_price ("doctor_id");
CREATE INDEX doctor_price_idx_2 ON hospital.doctor_price ("level");

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 1, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 2, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 3, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 4, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 5, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 6, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 7, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 8, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 9, '主任医师',80,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 10, '普通',50,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 11, '副主任医师',60,200);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 12, '副主任医师',60,150);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 13, '普通',50,100);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 14, '普通',50,100);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 15, '普通',50,100);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 16, '普通',50,100);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 17, '普通',50,100);

UPSERT INTO hospital.doctor_price("id","doctor_id","level","price_1","price_2")
VALUES(NEXT VALUE FOR hospital.doctor_price_sequence, 18, '普通',50,100);

-- 门诊与医生交叉表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.medical_dept_sub_and_doctor;
CREATE TABLE hospital.medical_dept_sub_and_doctor
(
    "id"        INTEGER PRIMARY KEY,
    "dept_sub_id"   INTEGER,
    "doctor_id" INTEGER
);

DROP SEQUENCE IF EXISTS hospital.medical_dept_sub_and_doctor_sequence;
CREATE SEQUENCE hospital.medical_dept_sub_and_doctor_sequence START WITH 1 increment BY 1;
CREATE INDEX medical_dept_sub_and_doctor_idx_1 ON hospital.medical_dept_sub_and_doctor ("dept_sub_id");
CREATE INDEX medical_dept_sub_and_doctor_idx_2 ON hospital.medical_dept_sub_and_doctor ("doctor_id");

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,18,1);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,20,2);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,9,3);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,19,4);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,26,5);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,26,6);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,4,7);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,26,8);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,20,9);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,12,10);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,14,11);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,14,12);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,14,13);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,13,14);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,26,15);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,2,16);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,2,17);

UPSERT INTO hospital.medical_dept_sub_and_doctor("id","dept_sub_id","doctor_id")
VALUES(NEXT VALUE FOR hospital.medical_dept_sub_and_doctor_sequence,2,18);

-- 医生出诊表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.doctor_work_plan;
CREATE TABLE hospital.doctor_work_plan(
    "id" INTEGER NOT NULL PRIMARY KEY ,
    "doctor_id" INTEGER,
    "dept_sub_id" INTEGER,
    "date" DATE,
--     当天挂号上限人数
    "maximum" SMALLINT,
--     实际挂号人数
    "num" SMALLINT
);

DROP SEQUENCE IF EXISTS hospital.doctor_work_plan_sequence;
CREATE SEQUENCE hospital.doctor_work_plan_sequence START WITH 1 increment BY 1;
CREATE INDEX doctor_work_plan_idx_1 ON hospital.doctor_work_plan ("doctor_id");
CREATE INDEX doctor_work_plan_idx_2 ON hospital.doctor_work_plan ("dept_sub_id");
CREATE INDEX doctor_work_plan_idx_3 ON hospital.doctor_work_plan ("date");

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,16,2,TO_DATE('2022-09-23'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,17,2,TO_DATE('2022-09-23'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,18,2,TO_DATE('2022-09-23'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,16,2,TO_DATE('2022-09-24'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,16,2,TO_DATE('2022-09-25'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,16,2,TO_DATE('2022-09-26'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,1,18,TO_DATE('2022-09-23'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,2,20,TO_DATE('2022-09-23'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,9,20,TO_DATE('2022-09-23'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,9,20,TO_DATE('2022-09-24'),45,0);

UPSERT INTO hospital.doctor_work_plan("id","doctor_id","dept_sub_id","date","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_sequence,9,20,TO_DATE('2022-09-25'),45,0);

-- 医生出诊时间段表-----------------------------------------------
DROP TABLE IF EXISTS hospital.doctor_work_plan_schedule;
CREATE TABLE hospital.doctor_work_plan_schedule(
    "id" INTEGER NOT NULL PRIMARY KEY ,
    "work_plan_id" INTEGER,
    "slot" TINYINT,
    "maximum" SMALLINT,
    "num" SMALLINT
);

DROP SEQUENCE IF EXISTS hospital.doctor_work_plan_schedule_sequence;
CREATE SEQUENCE hospital.doctor_work_plan_schedule_sequence START WITH 1 increment BY 1;

CREATE INDEX doctor_work_plan_schedule_idx_1 ON hospital.doctor_work_plan_schedule ("work_plan_id");

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,1,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,2,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,3,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,4,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,5,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,6,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,7,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,8,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,9,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,10,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,11,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,12,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,13,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,14,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,1,15,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,2,1,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,2,2,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,2,4,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,2,8,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,5,1,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,6,1,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,3,8,3,0);

UPSERT INTO hospital.doctor_work_plan_schedule("id","work_plan_id","slot","maximum","num")
VALUES(NEXT VALUE FOR hospital.doctor_work_plan_schedule_sequence,3,1,3,0);


-- 挂号表-----------------------------------------------
DROP TABLE IF EXISTS hospital.medical_registration;
CREATE TABLE hospital.medical_registration(
    "id" INTEGER NOT NULL PRIMARY KEY ,
    "patient_card_id" INTEGER,
    "work_plan_id" INTEGER,
    "doctor_schedule_id" INTEGER,
    "doctor_id" INTEGER,
    "dept_sub_id" INTEGER,
    "date" DATE,
    "slot" TINYINT,
    "amount" DECIMAL(10,2),
    "out_trade_no" CHAR(32),
    "prepay_id" CHAR(64),
    "transaction_id" CHAR(32),
    -- 付款状态: 1.未付款，2.已付款，3.已退款，4.已过期
    "payment_status" TINYINT,
    "create_time" DATE
);

DROP SEQUENCE IF EXISTS hospital.medical_registration_sequence;
CREATE SEQUENCE hospital.medical_registration_sequence START WITH 1 increment BY 1;

CREATE INDEX medical_registration_idx_1 ON hospital.medical_registration ("patient_card_id");
CREATE INDEX medical_registration_idx_2 ON hospital.medical_registration ("work_plan_id");
CREATE INDEX medical_registration_idx_3 ON hospital.medical_registration ("doctor_schedule_id");
CREATE INDEX medical_registration_idx_4 ON hospital.medical_registration ("doctor_id");
CREATE INDEX medical_registration_idx_5 ON hospital.medical_registration ("dept_sub_id");
CREATE INDEX medical_registration_idx_6 ON hospital.medical_registration ("date");
CREATE INDEX medical_registration_idx_7 ON hospital.medical_registration ("out_trade_no");
CREATE INDEX medical_registration_idx_8 ON hospital.medical_registration ("prepay_id");
CREATE INDEX medical_registration_idx_9 ON hospital.medical_registration ("transaction_id");
CREATE INDEX medical_registration_idx_10 ON hospital.medical_registration ("payment_status");


-- 处方表-----------------------------------------------
DROP TABLE IF EXISTS hospital.doctor_prescription;
CREATE TABLE hospital.doctor_prescription(
     "id" INTEGER NOT NULL PRIMARY KEY,
     "uuid" VARCHAR(32),
     "patient_card_id" INTEGER,
     -- 诊断结果
     "diagnosis" VARCHAR,
     "sub_dept_id" INTEGER,
     "doctor_id" INTEGER,
     "registration_id" INTEGER,
     --处方
     "rp" VARCHAR
);

DROP SEQUENCE IF EXISTS hospital.doctor_prescription_sequence;
CREATE SEQUENCE hospital.doctor_prescription_sequence START WITH 1 increment BY 1;

CREATE INDEX doctor_prescription_idx_1 ON hospital.doctor_prescription ("uuid");
CREATE INDEX doctor_prescription_idx_2 ON hospital.doctor_prescription ("patient_card_id");
CREATE INDEX doctor_prescription_idx_3 ON hospital.doctor_prescription ("sub_dept_id");
CREATE INDEX doctor_prescription_idx_4 ON hospital.doctor_prescription ("doctor_id");
CREATE INDEX doctor_prescription_idx_5 ON hospital.doctor_prescription ("registration_id");

-- 医生视频咨询表-----------------------------------------------
DROP TABLE IF EXISTS hospital.doctor_consult;
CREATE TABLE hospital.doctor_consult(
    "id" INTEGER NOT NULL PRIMARY KEY,
    "patient_card_id" INTEGER,
    "sub_dept_id" INTEGER,
    "doctor_id" INTEGER,
    "start_time" DATE,
    "end_time" DATE,
    "out_trade_no" CHAR(32),
    "amount" DECIMAL(10,2),
    "prepay_id" CHAR(64),
    "transaction_id" CHAR(32),
--     1.未付款，2.已付款，3.已退款，4.已过期
    "payment_status" TINYINT,
--     1.未开始，2.进行中，3.已结束，4.已关闭
    "status" TINYINT,
    "files" VARCHAR,
    "create_time" DATE
);

DROP SEQUENCE IF EXISTS hospital.doctor_consult_sequence;
CREATE SEQUENCE hospital.doctor_consult_sequence START WITH 1 increment BY 1;

CREATE INDEX doctor_consult_idx_1 ON hospital.doctor_consult ("patient_card_id");
CREATE INDEX doctor_consult_idx_2 ON hospital.doctor_consult ("sub_dept_id");
CREATE INDEX doctor_consult_idx_3 ON hospital.doctor_consult ("doctor_id");
CREATE INDEX doctor_consult_idx_4 ON hospital.doctor_consult ("out_trade_no");
CREATE INDEX doctor_consult_idx_5 ON hospital.doctor_consult ("prepay_id");
CREATE INDEX doctor_consult_idx_6 ON hospital.doctor_consult ("transaction_id");
CREATE INDEX doctor_consult_idx_7 ON hospital.doctor_consult ("status");
CREATE INDEX doctor_consult_idx_8 ON hospital.doctor_consult ("create_time");


-- MIS端行为表 -----------------------------------------------

DROP TABLE IF EXISTS hospital.mis_action;

CREATE TABLE hospital.mis_action
(
    "id"          INTEGER NOT NULL PRIMARY KEY,
    "action_code" VARCHAR,
    "action_name" VARCHAR
);

DROP SEQUENCE IF EXISTS hospital.mis_action_sequence;
CREATE SEQUENCE hospital.mis_action_sequence START WITH 1 increment BY 1;

UPSERT
INTO hospital.mis_action("id","action_code","action_name")
VALUES(NEXT VALUE FOR hospital.mis_action_sequence,'INSERT','添加');

UPSERT
INTO hospital.mis_action("id","action_code","action_name")
VALUES(NEXT VALUE FOR hospital.mis_action_sequence,'DELETE','删除');

UPSERT
INTO hospital.mis_action("id","action_code","action_name")
VALUES(NEXT VALUE FOR hospital.mis_action_sequence,'UPDATE','修改');

UPSERT
INTO hospital.mis_action("id","action_code","action_name")
VALUES(NEXT VALUE FOR hospital.mis_action_sequence,'SELECT','查询');

UPSERT
INTO hospital.mis_action("id","action_code","action_name")
VALUES(NEXT VALUE FOR hospital.mis_action_sequence,'APPROVAL','审批');

UPSERT
INTO hospital.mis_action("id","action_code","action_name")
VALUES(NEXT VALUE FOR hospital.mis_action_sequence,'EXPORT','导出');

UPSERT
INTO hospital.mis_action("id","action_code","action_name")
VALUES(NEXT VALUE FOR hospital.mis_action_sequence,'BACKUP','备份');

UPSERT
INTO hospital.mis_action("id","action_code","action_name")
VALUES(NEXT VALUE FOR hospital.mis_action_sequence,'ARCHIVE','归档');


-- MIS端模块表 -----------------------------------------------

DROP TABLE IF EXISTS hospital.mis_module;

CREATE TABLE hospital.mis_module
(
    "id"          SMALLINT NOT NULL PRIMARY KEY,
    "module_code" VARCHAR,
    "module_name" VARCHAR
);


UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(1,'MIS_USER','MIS端用户管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(2,'PATIENT_USER','患者端用户管理');


UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(3,'WORKER_USER','医护端用户管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(4,'DEPT','部门管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(5,'MEDICAL_DEPT','医疗科室管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(6,'MEDICAL_DEPT_SUB','医疗诊室管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(7,'SCHEDULE','出诊管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(8,'REGISTRATION','挂号管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(9,'VIDEO_DIAGNOSE','视频问诊管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(10,'DOCTOR','医生管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(11,'NURSE','护士管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(12,'NURSING_ASSISTANT','护工管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(13,'DOCTOR_PRICE','诊费管理');

UPSERT INTO hospital.mis_module("id","module_code","module_name")
VALUES(14,'SYSTEM','系统管理');

-- MIS端行为表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.mis_action;

CREATE TABLE hospital.mis_action
(
    "id"          SMALLINT NOT NULL PRIMARY KEY,
    "action_code" VARCHAR,
    "action_name" VARCHAR
);

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(1,'INSERT','添加');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(2,'DELETE','删除');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(3,'UPDATE','修改');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(4,'SELECT','查询');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(5,'APPROVAL','审批');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(6,'IMPORT','导入');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(7,'EXPORT','导出');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(8,'BACKUP','备份');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(9,'ARCHIVE','归档');

UPSERT INTO hospital.mis_action("id","action_code","action_name")
VALUES(10,'DIAGNOSE','诊断');

-- MIS端权限表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.mis_permission;

CREATE TABLE hospital.mis_permission
(
    "id"                SMALLINT NOT NULL PRIMARY KEY,
    "permission_code"   VARCHAR,
    "module_id"         SMALLINT,
    "action_id"         SMALLINT
);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(0,'ROOT',0,0);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(1,'MIS_USER:INSERT',1,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(2,'MIS_USER:DELETE',1,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(3,'MIS_USER:UPDATE',1,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(4,'MIS_USER:SELECT',1,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(5,'PATIENT_USER:INSERT',2,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(6,'PATIENT_USER:DELETE',2,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(7,'PATIENT_USER:UPDATE',2,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(8,'PATIENT_USER:SELECT',2,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(9,'WORKER_USER:INSERT',3,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(10,'WORKER_USER:DELETE',3,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(11,'WORKER_USER:UPDATE',3,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(12,'WORKER_USER:SELECT',3,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(13,'DEPT:INSERT',4,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(14,'DEPT:DELETE',4,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(15,'DEPT:UPDATE',4,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(16,'DEPT:SELECT',4,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(17,'MEDICAL_DEPT:INSERT',5,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(18,'MEDICAL_DEPT:DELETE',5,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(19,'MEDICAL_DEPT:UPDATE',5,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(20,'MEDICAL_DEPT:SELECT',5,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(21,'MEDICAL_DEPT_SUB:INSERT',6,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(22,'MEDICAL_DEPT_SUB:DELETE',6,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(23,'MEDICAL_DEPT_SUB:UPDATE',6,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(24,'MEDICAL_DEPT_SUB:SELECT',6,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(25,'SCHEDULE:INSERT',7,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(26,'SCHEDULE:DELETE',7,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(27,'SCHEDULE:UPDATE',7,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(28,'SCHEDULE:SELECT',7,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(29,'REGISTRATION:INSERT',8,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(30,'REGISTRATION:DELETE',8,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(31,'REGISTRATION:UPDATE',8,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(32,'REGISTRATION:SELECT',8,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(33,'VIDEO_DIAGNOSE:INSERT',9,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(34,'VIDEO_DIAGNOSE:DELETE',9,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(35,'VIDEO_DIAGNOSE:UPDATE',9,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(36,'VIDEO_DIAGNOSE:SELECT',9,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(37,'VIDEO_DIAGNOSE:DIAGNOSE',9,5);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(38,'DOCTOR:INSERT',10,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(39,'DOCTOR:DELETE',10,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(40,'DOCTOR:UPDATE',10,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(41,'DOCTOR:SELECT',10,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(42,'NURSE:INSERT',11,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(43,'NURSE:DELETE',11,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(44,'NURSE:UPDATE',11,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(45,'NURSE:SELECT',11,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(46,'NURSING_ASSISTANT:INSERT',12,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(47,'NURSING_ASSISTANT:DELETE',12,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(48,'NURSING_ASSISTANT:UPDATE',12,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(49,'NURSING_ASSISTANT:SELECT',12,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(46,'DOCTOR_PRICE:INSERT',13,1);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(47,'DOCTOR_PRICE:DELETE',14,2);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(48,'DOCTOR_PRICE:UPDATE',15,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(49,'DOCTOR_PRICE:SELECT',16,4);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(50,'SYSTEM:UPDATE',16,3);

UPSERT INTO hospital.mis_permission("id","permission_code","module_id","action_id")
VALUES(51,'SYSTEM:SELECT',16,4);


-- MIS端角色表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.mis_role;

CREATE TABLE hospital.mis_role
(
    "id"            INTEGER NOT NULL PRIMARY KEY,
    "role_name"     VARCHAR(50),
    "remark"          VARCHAR(50)
);

DROP SEQUENCE IF EXISTS hospital.mis_role_sequence;
CREATE SEQUENCE hospital.mis_role_sequence START WITH 1 increment BY 1;

UPSERT INTO hospital.mis_role("id","role_name","remark")
VALUES(0, '超级管理员','超级管理员');

UPSERT INTO hospital.mis_role("id","role_name","remark")
VALUES(NEXT VALUE FOR hospital.mis_role_sequence, '医生','医生角色');

UPSERT INTO hospital.mis_role("id","role_name","remark")
VALUES(NEXT VALUE FOR hospital.mis_role_sequence, '视频问诊医生','可以视频问诊的医生');

-- MIS端角色权限表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.mis_role_permission;

CREATE TABLE hospital.mis_role_permission
(
    "id"            INTEGER NOT NULL PRIMARY KEY,
    "role_id"       INTEGER,
    "permission_id" SMALLINT
);

DROP SEQUENCE IF EXISTS hospital.mis_role_permission_sequence;
CREATE SEQUENCE hospital.mis_role_permission_sequence START WITH 1 increment BY 1;

-- 超级管理员角色拥有最高权限
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(0, 0,0);

-- 医生拥有查询MIS帐户权限（仅限自己）
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(NEXT VALUE FOR hospital.mis_role_permission_sequence, 1,4);

-- 医生拥有查询部门权限
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(NEXT VALUE FOR hospital.mis_role_permission_sequence, 1,16);

-- 医生拥有查询科室权限
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(NEXT VALUE FOR hospital.mis_role_permission_sequence, 1,20);

-- 医生拥有查询诊室权限
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(NEXT VALUE FOR hospital.mis_role_permission_sequence, 1,24);

-- 医生拥有查询出诊计划权限
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(NEXT VALUE FOR hospital.mis_role_permission_sequence, 1,28);

-- 医生拥有查询挂号记录权限（仅限自己的患者）
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(NEXT VALUE FOR hospital.mis_role_permission_sequence, 1,32);

-- 视频问诊医生拥有查询问诊记录权限（仅限自己的患者）
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(NEXT VALUE FOR hospital.mis_role_permission_sequence, 2,36);

-- 视频问诊医生拥有视频问诊权限
UPSERT INTO hospital.mis_role_permission("id","role_id","permission_id")
VALUES(NEXT VALUE FOR hospital.mis_role_permission_sequence, 2,37);

-- MIS端部门表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.mis_dept;

CREATE TABLE hospital.mis_dept
(
    "id"    INTEGER NOT NULL PRIMARY KEY,
    "name"  VARCHAR
);
DROP SEQUENCE IF EXISTS hospital.mis_dept_sequence;
CREATE SEQUENCE hospital.mis_dept_sequence START WITH 1 increment BY 1;

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '院长办公室');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '人力资源部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '财务部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '保卫部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '后勤部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '工程部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '基建部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '物资部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '运营部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '医务部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '门诊部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '护理部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '科研教育部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '国际合作部');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '信息中心');

UPSERT INTO hospital.mis_dept("id","name")
VALUES(NEXT VALUE FOR hospital.mis_dept_sequence, '公共关系部');


-- MIS端用户表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.mis_user;

CREATE TABLE hospital.mis_user
(
    "id"        INTEGER NOT NULL PRIMARY KEY,
    "username"  VARCHAR(50),
    "password"  VARCHAR,
    "name"      VARCHAR(20),
    "sex"       VARCHAR(1),
    "tel"       VARCHAR(11),
    "email"     VARCHAR(100),
    "dept_id"   INTEGER,
    "job"       VARCHAR(20),
    "ref_id"    INTEGER,
--     1有效，2离职，3禁用
    "status"    TINYINT,
    "create_time" DATE
);

DROP SEQUENCE IF EXISTS hospital.mis_user_sequence;
CREATE SEQUENCE hospital.mis_user_sequence START WITH 1 increment BY 1;

CREATE INDEX mis_user_idx_1 ON hospital.mis_user ("username");
CREATE INDEX mis_user_idx_2 ON hospital.mis_user ("dept_id");
CREATE INDEX mis_user_idx_3 ON hospital.mis_user ("job");
CREATE INDEX mis_user_idx_4 ON hospital.mis_user ("dept_id");
CREATE INDEX mis_user_idx_5 ON hospital.mis_user ("status");

UPSERT INTO hospital.mis_user(
    "id","username","password","name","sex","tel","email","dept_id","job","status","create_time"
)
VALUES(
    0,'admin','061575f43e456772015c0032c0531edf','超级管理员','男',NULL,NULL,NULL,NULL,1,NOW()
);


-- MIS端用户角色表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.mis_user_role;

CREATE TABLE hospital.mis_user_role
(
    "id"       INTEGER NOT NULL PRIMARY KEY,
    "user_id"  INTEGER,
    "role_id"  INTEGER
);

DROP SEQUENCE IF EXISTS hospital.mis_user_role_sequence;
CREATE SEQUENCE hospital.mis_user_role_sequence START WITH 1 increment BY 1;

CREATE INDEX mis_user_role_idx_1 ON hospital.mis_user_role ("user_id");
CREATE INDEX mis_user_role_idx_2 ON hospital.mis_user_role ("role_id");

-- 超级管理员MIS帐户关联超级管理员角色
UPSERT INTO hospital.mis_user_role(
    "id","user_id","role_id"
)
VALUES(0,0,0);


-- 视频问诊订单表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.video_diagnose;
CREATE TABLE hospital.video_diagnose
(
    "id"              INTEGER NOT NULL PRIMARY KEY,
    "patient_card_id" INTEGER,
    "doctor_id"       INTEGER,
    "out_trade_no"    CHAR(32),
    "amount"          DECIMAL(10, 2),
    -- 付款状态：1.未付款，2.已付款，3.已退款，4.已过期
    "payment_status"  TINYINT,
    "prepay_id"       CHAR(64),
    "transaction_id"  CHAR(32),
    "expect_start"     DATE,
    "expect_end"       DATE,
    "real_start"       DATE,
    "real_end"         DATE,
    -- 问诊状态：1.未开始，2.问诊中，3.已结束
    "status"          TINYINT,
    "create_time"     DATE
);

DROP SEQUENCE IF EXISTS hospital.video_diagnose_sequence;
CREATE SEQUENCE hospital.video_diagnose_sequence START WITH 1 increment BY 1;

CREATE INDEX video_diagnose_idx_1 ON hospital.video_diagnose ("patient_card_id");
CREATE INDEX video_diagnose_idx_2 ON hospital.video_diagnose ("doctor_id");
CREATE INDEX video_diagnose_idx_3 ON hospital.video_diagnose ("out_trade_no");
CREATE INDEX video_diagnose_idx_4 ON hospital.video_diagnose ("payment_status");
CREATE INDEX video_diagnose_idx_5 ON hospital.video_diagnose ("prepay_id");
CREATE INDEX video_diagnose_idx_6 ON hospital.video_diagnose ("transaction_id");
CREATE INDEX video_diagnose_idx_7 ON hospital.video_diagnose ("expect_start");
CREATE INDEX video_diagnose_idx_8 ON hospital.video_diagnose ("expect_end");
CREATE INDEX video_diagnose_idx_9 ON hospital.video_diagnose ("status");

-- 视频问诊资料表 -----------------------------------------------
DROP TABLE IF EXISTS hospital.video_diagnose_file;
CREATE TABLE hospital.video_diagnose_file(
    "id" INTEGER NOT NULL PRIMARY KEY,
    "video_diagnose_id" INTEGER,
    "filename" VARCHAR(100),
    "path" VARCHAR(300),
    "create_time" DATE
);

DROP SEQUENCE IF EXISTS hospital.video_diagnose_file_sequence;
CREATE SEQUENCE hospital.video_diagnose_file_sequence START WITH 1 increment BY 1;

CREATE INDEX video_diagnose_file_idx_1 ON hospital.video_diagnose_file ("video_diagnose_id");
CREATE INDEX video_diagnose_file_idx_2 ON hospital.video_diagnose_file ("create_time");

