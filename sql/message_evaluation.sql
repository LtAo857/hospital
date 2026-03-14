-- 消息表
CREATE TABLE patient_message (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL COMMENT '患者用户ID',
  type TINYINT NOT NULL COMMENT '1挂号成功 2就诊提醒 3问诊结束 4收到评价回复',
  title VARCHAR(100) NOT NULL,
  content VARCHAR(500) NOT NULL,
  ref_id INT NULL COMMENT '关联业务ID(挂号ID/问诊ID)',
  is_read TINYINT(1) DEFAULT 0 COMMENT '0未读 1已读',
  create_time DATETIME DEFAULT NOW(),
  PRIMARY KEY (id),
  INDEX idx_user_id (user_id),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评价表
CREATE TABLE doctor_evaluation (
  id INT NOT NULL AUTO_INCREMENT,
  doctor_id INT NOT NULL,
  patient_card_id INT NOT NULL,
  registration_id INT NULL COMMENT '挂号ID',
  video_diagnose_id INT NULL COMMENT '视频问诊ID',
  score TINYINT NOT NULL COMMENT '1-5星',
  comment VARCHAR(500) NULL,
  create_time DATETIME DEFAULT NOW(),
  PRIMARY KEY (id),
  INDEX idx_doctor_id (doctor_id),
  INDEX idx_registration_id (registration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
