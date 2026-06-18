// ============================================================
// 医院挂号知识图谱 — Neo4j 初始化脚本
// 执行方式：cypher-shell -u neo4j -p neo4j123456 -f neo4j_init.cypher
// 或者在 Neo4j Browser 中逐段执行
// ============================================================


// ── 创建约束 ──
CREATE CONSTRAINT symptom_name IF NOT EXISTS FOR (s:Symptom) REQUIRE s.name IS UNIQUE;
CREATE CONSTRAINT dept_name IF NOT EXISTS FOR (d:Department) REQUIRE d.name IS UNIQUE;
CREATE CONSTRAINT pop_name IF NOT EXISTS FOR (p:Population) REQUIRE p.name IS UNIQUE;

// ── 科室节点 ──
CREATE (:Department {name: "口腔科", outpatient: true});
CREATE (:Department {name: "呼吸内科", outpatient: true});
CREATE (:Department {name: "消化内科", outpatient: true});
CREATE (:Department {name: "心内科", outpatient: true});
CREATE (:Department {name: "皮肤科", outpatient: true});
CREATE (:Department {name: "眼科", outpatient: true});
CREATE (:Department {name: "耳鼻喉科", outpatient: true});
CREATE (:Department {name: "儿科", outpatient: true});
CREATE (:Department {name: "骨科", outpatient: true});
CREATE (:Department {name: "妇科", outpatient: true});
CREATE (:Department {name: "神经内科", outpatient: true});
CREATE (:Department {name: "急诊科", outpatient: true});
CREATE (:Department {name: "乳腺外科", outpatient: true});
CREATE (:Department {name: "胸外科", outpatient: true});

// ── 人群节点 ──
CREATE (:Population {name: "儿童"});
CREATE (:Population {name: "成人"});
CREATE (:Population {name: "孕妇"});
CREATE (:Population {name: "老人"});

// ── 症状节点 ──
CREATE (:Symptom {name: "牙疼", aliases: ["牙痛", "牙酸", "牙龈肿", "牙龈出血", "口腔疼"]});
CREATE (:Symptom {name: "咳嗽", aliases: ["干咳", "咳痰", "嗓子痒", "喉咙痛", "有痰"]});
CREATE (:Symptom {name: "发烧", aliases: ["发热", "高烧", "低烧", "体温高", "发烫"]});
CREATE (:Symptom {name: "胸闷", aliases: ["心慌", "气短", "呼吸困难", "心悸", "憋气"]});
CREATE (:Symptom {name: "胸痛", aliases: ["胸疼", "心口疼", "心口痛", "压榨感"]});
CREATE (:Symptom {name: "胃疼", aliases: ["胃痛", "胃酸", "胃胀", "反酸", "烧心"]});
CREATE (:Symptom {name: "腹痛", aliases: ["肚子疼", "肚子痛", "拉肚子", "腹泻", "肚子胀"]});
CREATE (:Symptom {name: "皮疹", aliases: ["过敏", "起疹", "起包", "瘙痒", "红疹", "荨麻疹"]});
CREATE (:Symptom {name: "眼睛不适", aliases: ["视力模糊", "看不清", "眼花", "眼睛疼", "干眼", "流泪"]});
CREATE (:Symptom {name: "耳朵不适", aliases: ["耳鸣", "听力下降", "耳朵疼", "耳闷", "中耳炎"]});
CREATE (:Symptom {name: "头疼", aliases: ["头痛", "头晕", "脑壳疼", "偏头痛", "头昏"]});
CREATE (:Symptom {name: "骨折", aliases: ["摔伤", "骨裂", "扭伤", "脱臼", "伤到骨头"]});
CREATE (:Symptom {name: "乳腺胀痛", aliases: ["乳房疼", "乳腺疼", "乳晕疼", "乳房胀"]});
CREATE (:Symptom {name: "胳膊疼", aliases: ["手臂疼", "手臂痛", "胳膊酸", "手疼", "手痛"]});
CREATE (:Symptom {name: "嘴疼", aliases: ["嘴痛", "嘴巴疼", "口腔溃疡", "嘴唇疼"]});

// ── 症状→科室 关系 (BELONGS_TO) ──
// 格式: MATCH (s),(d) WHERE s.name=? AND d.name=? CREATE (s)-[:BELONGS_TO {weight:?}]->(d)

MATCH (s:Symptom {name: "牙疼"}), (d:Department {name: "口腔科"})
CREATE (s)-[:BELONGS_TO {weight: 0.95}]->(d);

MATCH (s:Symptom {name: "嘴疼"}), (d:Department {name: "口腔科"})
CREATE (s)-[:BELONGS_TO {weight: 0.90}]->(d);

MATCH (s:Symptom {name: "咳嗽"}), (d:Department {name: "呼吸内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.70}]->(d);
MATCH (s:Symptom {name: "咳嗽"}), (d:Department {name: "儿科"})
CREATE (s)-[:BELONGS_TO {weight: 0.50}]->(d);

MATCH (s:Symptom {name: "发烧"}), (d:Department {name: "呼吸内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.60}]->(d);
MATCH (s:Symptom {name: "发烧"}), (d:Department {name: "儿科"})
CREATE (s)-[:BELONGS_TO {weight: 0.70}]->(d);

MATCH (s:Symptom {name: "胸闷"}), (d:Department {name: "心内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.80}]->(d);
MATCH (s:Symptom {name: "胸闷"}), (d:Department {name: "呼吸内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.50}]->(d);
MATCH (s:Symptom {name: "胸闷"}), (d:Department {name: "急诊科"})
CREATE (s)-[:BELONGS_TO {weight: 0.60}]->(d);

MATCH (s:Symptom {name: "胸痛"}), (d:Department {name: "心内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.90}]->(d);
MATCH (s:Symptom {name: "胸痛"}), (d:Department {name: "胸外科"})
CREATE (s)-[:BELONGS_TO {weight: 0.60}]->(d);
MATCH (s:Symptom {name: "胸痛"}), (d:Department {name: "急诊科"})
CREATE (s)-[:BELONGS_TO {weight: 0.85}]->(d);

MATCH (s:Symptom {name: "胃疼"}), (d:Department {name: "消化内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.90}]->(d);

MATCH (s:Symptom {name: "腹痛"}), (d:Department {name: "消化内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.85}]->(d);
MATCH (s:Symptom {name: "腹痛"}), (d:Department {name: "妇科"})
CREATE (s)-[:BELONGS_TO {weight: 0.40}]->(d);

MATCH (s:Symptom {name: "皮疹"}), (d:Department {name: "皮肤科"})
CREATE (s)-[:BELONGS_TO {weight: 0.95}]->(d);

MATCH (s:Symptom {name: "眼睛不适"}), (d:Department {name: "眼科"})
CREATE (s)-[:BELONGS_TO {weight: 0.95}]->(d);

MATCH (s:Symptom {name: "耳朵不适"}), (d:Department {name: "耳鼻喉科"})
CREATE (s)-[:BELONGS_TO {weight: 0.95}]->(d);

MATCH (s:Symptom {name: "头疼"}), (d:Department {name: "神经内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.80}]->(d);
MATCH (s:Symptom {name: "头疼"}), (d:Department {name: "急诊科"})
CREATE (s)-[:BELONGS_TO {weight: 0.30}]->(d);

MATCH (s:Symptom {name: "骨折"}), (d:Department {name: "骨科"})
CREATE (s)-[:BELONGS_TO {weight: 0.95}]->(d);

MATCH (s:Symptom {name: "胳膊疼"}), (d:Department {name: "骨科"})
CREATE (s)-[:BELONGS_TO {weight: 0.70}]->(d);
MATCH (s:Symptom {name: "胳膊疼"}), (d:Department {name: "神经内科"})
CREATE (s)-[:BELONGS_TO {weight: 0.30}]->(d);

MATCH (s:Symptom {name: "乳腺胀痛"}), (d:Department {name: "乳腺外科"})
CREATE (s)-[:BELONGS_TO {weight: 0.90}]->(d);
MATCH (s:Symptom {name: "乳腺胀痛"}), (d:Department {name: "妇科"})
CREATE (s)-[:BELONGS_TO {weight: 0.60}]->(d);

// ── 症状→人群 关系 (AFFECTS) ──
// preferredDepartment: 只有匹配到对应科室时才加分，不会给所有科室加
// 咳嗽+儿童 → 儿科加分
MATCH (s:Symptom {name: "咳嗽"}), (p:Population {name: "儿童"})
CREATE (s)-[:AFFECTS {relevance: 0.6, preferredDepartment: "儿科"}]->(p);
// 发烧+儿童 → 儿科加分
MATCH (s:Symptom {name: "发烧"}), (p:Population {name: "儿童"})
CREATE (s)-[:AFFECTS {relevance: 0.7, preferredDepartment: "儿科"}]->(p);
// 骨折+老人 → 骨科加分
MATCH (s:Symptom {name: "骨折"}), (p:Population {name: "老人"})
CREATE (s)-[:AFFECTS {relevance: 0.8, preferredDepartment: "骨科"}]->(p);
// 乳腺胀痛+孕妇 → 乳腺外科加分
MATCH (s:Symptom {name: "乳腺胀痛"}), (p:Population {name: "孕妇"})
CREATE (s)-[:AFFECTS {relevance: 0.5, preferredDepartment: "乳腺外科"}]->(p);
