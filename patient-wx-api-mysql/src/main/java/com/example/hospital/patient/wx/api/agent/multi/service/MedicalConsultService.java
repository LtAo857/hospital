package com.example.hospital.patient.wx.api.agent.multi.service;

import com.example.hospital.patient.wx.api.agent.multi.rag.MultiAgentKnowledgeBase;
import com.example.hospital.patient.wx.api.agent.multi.rag.MultiAgentRagService;
import com.example.hospital.patient.wx.api.agent.support.AgentUiAction;
import com.example.hospital.patient.wx.api.agent.tool.MedicalDeptAgentTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MedicalConsultService {
    private static final List<String> URGENT_KEYWORDS = Arrays.asList(
            "气短", "呼吸困难", "喘不上气", "出汗", "大汗", "冷汗",
            "放射", "左臂", "后背", "背痛", "晕", "晕厥", "恶心", "压榨", "濒死感", "突然"
    );

    private static final List<String> HEART_KEYWORDS = Arrays.asList(
            "胸痛", "胸疼", "胸闷", "心慌", "心悸", "心口", "压榨", "放射", "左臂", "后背"
    );

    private static final List<String> BREATH_KEYWORDS = Arrays.asList(
            "咳嗽", "咳痰", "发热", "气短", "呼吸", "吸气", "呼气", "喘", "肺"
    );

    private static final List<String> BREAST_KEYWORDS = Arrays.asList(
            "乳房", "乳腺", "乳晕", "经期", "月经", "胀痛", "哺乳", "哺乳期"
    );

    private static final List<String> EMERGENCY_DEPT_CANDIDATES = Arrays.asList("急诊科", "急诊室");
    private static final List<String> ROUTINE_DEPT_CANDIDATES = Arrays.asList(
            "心内科", "呼吸内科", "胸外科", "乳腺外科", "消化内科", "妇科"
    );

    @Resource
    private MedicalDeptAgentTools medicalDeptAgentTools;

    @Autowired(required = false)
    private MultiAgentRagService ragService;

    public ConsultResult consult(String message, Map<String, Object> memory) {
        Map<String, Object> safeMemory = safeMap(memory);
        String normalized = normalize(firstText(message, safeMemory.get("symptom")));
        String symptom = firstText(safeMemory.get("symptom"), message);
        String patientGender = normalizeGender(firstText(safeMemory.get("patientGender")));
        String doctorGender = normalizeGender(firstText(safeMemory.get("doctorGender")));
        String doctorAgePreference = firstText(safeMemory.get("doctorAgePreference"));
        List<String> departments = loadDepartmentNames();

        boolean urgent = isUrgentChestPain(normalized);
        String recommendedDeptName = chooseDepartment(normalized, departments, urgent);
        if (!StringUtils.hasText(recommendedDeptName) && !departments.isEmpty()) {
            recommendedDeptName = departments.get(0);
        }

        if (!StringUtils.hasText(doctorGender) && "女".equals(patientGender)) {
            doctorGender = "女";
        }

        Map<String, Object> ragMemory = new LinkedHashMap<>();
        if (StringUtils.hasText(symptom)) {
            ragMemory.put("symptom", symptom);
        }
        if (StringUtils.hasText(patientGender)) {
            ragMemory.put("patientGender", patientGender);
        }
        if (StringUtils.hasText(doctorGender)) {
            ragMemory.put("doctorGender", doctorGender);
        }
        if (StringUtils.hasText(doctorAgePreference)) {
            ragMemory.put("doctorAgePreference", doctorAgePreference);
        }
        if (StringUtils.hasText(recommendedDeptName)) {
            ragMemory.put("recommendedDeptName", recommendedDeptName);
        }
        ragMemory.put("medicalConsultRiskLevel", urgent ? "urgent" : "routine");

        String question = urgent
                ? "胸痛有哪些危险信号和就医建议？"
                : "胸痛通常先看什么科，常见原因有哪些？";
        MultiAgentRagService.RagAnswer ragAnswer = ragService == null ? null : ragService.answer(question, ragMemory);
        String explanation = ragAnswer == null ? null : ragAnswer.getAnswer();
        String reply = buildReply(explanation, urgent, recommendedDeptName, patientGender, doctorGender);

        ConsultResult result = new ConsultResult();
        result.setReply(reply);
        result.setRiskLevel(urgent ? "urgent" : "routine");
        result.setRecommendedDeptName(recommendedDeptName);
        result.setPatientGender(patientGender);
        result.setDoctorGenderHint(doctorGender);
        result.setSymptom(symptom);
        result.setRecommendationNotice(urgent
                ? "胸痛伴随红旗症状，建议尽快线下就医"
                : "胸痛可优先按推荐科室挂号");
        result.setMemoryPatch(buildMemoryPatch(result));
        if (ragAnswer != null) {
            result.setRagSources(buildSourceSummary(ragAnswer.getSnippets()));
        }
        return result;
    }

    private String buildReply(String explanation,
                             boolean urgent,
                             String recommendedDeptName,
                             String patientGender,
                             String doctorGenderHint) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(explanation)) {
            builder.append(explanation.trim());
        } else if (urgent) {
            builder.append("你描述的胸痛有些危险信号，建议不要继续在聊天里判断，尽快线下就医或前往急诊。");
        } else {
            builder.append("从你说的胸痛看，先按分诊思路帮你缩小范围。");
        }

        if (urgent) {
            if (StringUtils.hasText(recommendedDeptName)) {
                builder.append(" 当前更建议尽快去 ").append(recommendedDeptName).append("。 ");
            }
            builder.append("如果胸痛是突然加重、伴气短/出汗/放射痛，请直接线下就医，不要只靠线上判断。");
            return builder.toString();
        }

        if (StringUtils.hasText(recommendedDeptName)) {
            builder.append(" 当前更建议先看 ").append(recommendedDeptName).append("。");
        }
        if ("女".equals(patientGender) || "女".equals(doctorGenderHint)) {
            builder.append(" 如果你更在意就诊体验，我会把女性医生作为优先偏好一起考虑。");
        }
        builder.append(" 如果你愿意，我也可以继续帮你走挂号流程。");
        return builder.toString();
    }

    private Map<String, Object> buildMemoryPatch(ConsultResult result) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("patientGender", result.getPatientGender());
        patch.put("doctorGender", result.getDoctorGenderHint());
        patch.put("symptom", result.getSymptom());
        patch.put("medicalConsultRiskLevel", result.getRiskLevel());
        patch.put("medicalConsultAdvice", result.getReply());
        patch.put("medicalConsultRecommendation", result.getRecommendationNotice());
        patch.put("medicalConsultRagSources", result.getRagSources());
        patch.put("medicalConsultDoctorGenderHint", result.getDoctorGenderHint());
        patch.put("doctorGender", result.getDoctorGenderHint());
        patch.put("requestedView", AgentUiAction.MEDICAL_CONSULT);
        patch.put("nluUnavailable", false);
        return patch;
    }

    private String buildSourceSummary(List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets) {
        if (snippets == null || snippets.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (MultiAgentKnowledgeBase.KnowledgeSnippet snippet : snippets) {
            if (snippet == null || !StringUtils.hasText(snippet.getTitle())) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('、');
            }
            builder.append(snippet.getTitle());
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private List<String> loadDepartmentNames() {
        ArrayList<HashMap> departments = medicalDeptAgentTools.searchDepartments(null, null);
        List<String> names = new ArrayList<>();
        if (departments == null) {
            return names;
        }
        for (HashMap department : departments) {
            String name = firstText(department.get("name"));
            if (StringUtils.hasText(name) && !names.contains(name)) {
                names.add(name.trim());
            }
        }
        return names;
    }

    private String chooseDepartment(String text, List<String> availableDepartments, boolean urgent) {
        List<String> candidates;
        if (containsAny(text, BREAST_KEYWORDS)) {
            candidates = Arrays.asList("乳腺外科", "妇科", "心内科");
        } else if (containsAny(text, BREATH_KEYWORDS)) {
            candidates = Arrays.asList("呼吸内科", "心内科", "胸外科");
        } else if (containsAny(text, HEART_KEYWORDS) || containsAny(text, Arrays.asList("胸痛", "胸疼", "胸闷"))) {
            candidates = urgent ? Arrays.asList("急诊科", "心内科", "呼吸内科", "胸外科")
                    : Arrays.asList("心内科", "呼吸内科", "胸外科", "消化内科");
        } else {
            candidates = urgent ? Arrays.asList("急诊科", "心内科", "呼吸内科") : ROUTINE_DEPT_CANDIDATES;
        }
        for (String candidate : candidates) {
            if (availableDepartments.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isUrgentChestPain(String text) {
        if (!containsAny(text, Arrays.asList("胸痛", "胸疼", "胸闷"))) {
            return false;
        }
        return containsAny(text, URGENT_KEYWORDS);
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (!StringUtils.hasText(text) || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeGender(String value) {
        String text = normalize(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (text.contains("女")) {
            return "女";
        }
        if (text.contains("男")) {
            return "男";
        }
        return null;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().replaceAll("\\s+", "");
    }

    private String firstText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            String text = firstText(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? new HashMap<String, Object>() : map;
    }

    public static class ConsultResult {
        private String reply;
        private String riskLevel;
        private String recommendedDeptName;
        private String patientGender;
        private String doctorGenderHint;
        private String symptom;
        private String recommendationNotice;
        private String ragSources;
        private Map<String, Object> memoryPatch;

        public String getReply() {
            return reply;
        }

        public void setReply(String reply) {
            this.reply = reply;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getRecommendedDeptName() {
            return recommendedDeptName;
        }

        public void setRecommendedDeptName(String recommendedDeptName) {
            this.recommendedDeptName = recommendedDeptName;
        }

        public String getPatientGender() {
            return patientGender;
        }

        public void setPatientGender(String patientGender) {
            this.patientGender = patientGender;
        }

        public String getDoctorGenderHint() {
            return doctorGenderHint;
        }

        public void setDoctorGenderHint(String doctorGenderHint) {
            this.doctorGenderHint = doctorGenderHint;
        }

        public String getSymptom() {
            return symptom;
        }

        public void setSymptom(String symptom) {
            this.symptom = symptom;
        }

        public String getRecommendationNotice() {
            return recommendationNotice;
        }

        public void setRecommendationNotice(String recommendationNotice) {
            this.recommendationNotice = recommendationNotice;
        }

        public String getRagSources() {
            return ragSources;
        }

        public void setRagSources(String ragSources) {
            this.ragSources = ragSources;
        }

        public Map<String, Object> getMemoryPatch() {
            return memoryPatch;
        }

        public void setMemoryPatch(Map<String, Object> memoryPatch) {
            this.memoryPatch = memoryPatch;
        }
    }
}
