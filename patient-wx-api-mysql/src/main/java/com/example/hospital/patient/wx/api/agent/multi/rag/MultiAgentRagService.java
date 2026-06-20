package com.example.hospital.patient.wx.api.agent.multi.rag;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MultiAgentRagService {
    @Resource
    private AgentProperties agentProperties;

    @Resource
    private MultiAgentProperties multiAgentProperties;

    @Resource
    private MultiAgentKnowledgeBase knowledgeBase;

    @Autowired(required = false)
    private RedisTemplate<Object, Object> redisTemplate;

    public MultiAgentRagService() {
    }

    public MultiAgentRagService(AgentProperties agentProperties,
                                MultiAgentKnowledgeBase knowledgeBase) {
        this.agentProperties = agentProperties;
        this.knowledgeBase = knowledgeBase;
        this.multiAgentProperties = new MultiAgentProperties();
    }

    public MultiAgentRagService(AgentProperties agentProperties,
                                MultiAgentProperties multiAgentProperties,
                                MultiAgentKnowledgeBase knowledgeBase) {
        this.agentProperties = agentProperties;
        this.multiAgentProperties = multiAgentProperties;
        this.knowledgeBase = knowledgeBase;
    }

    public RagAnswer answer(String question, Map<String, Object> memory) {
        long startedAt = System.currentTimeMillis();
        Map<String, Object> safeMemory = sanitizeMemory(memory);
        String cacheKey = buildCacheKey(question, safeMemory);
        RagAnswer cached = loadCache(cacheKey);
        if (cached != null) {
            cached.setLatencyMs(System.currentTimeMillis() - startedAt);
            cached.setCacheHit(true);
            return cached;
        }
        MultiAgentKnowledgeBase.SearchResult retrieval = knowledgeBase.retrieve(question, 3);
        List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets = retrieval.getSnippets();
        RagAnswer answer;
        if (snippets.isEmpty()) {
            answer = new RagAnswer(buildFallbackAnswer(safeMemory), snippets, false);
            answer.setMode("fallback");
            answer.setFallbackReason(firstText(retrieval.getFallbackReason(), "no_hit"));
        } else if (!agentProperties.isLlmEnabled() || !StringUtils.hasText(agentProperties.getApiKey())) {
            answer = new RagAnswer(buildSnippetAnswer(snippets, safeMemory), snippets, false);
            answer.setMode(retrieval.getMode());
            answer.setFallbackReason(retrieval.getFallbackReason());
        } else {
            try {
                LlmCallResult llmResult = callLlm(question, safeMemory, snippets);
                if (llmResult == null || !StringUtils.hasText(llmResult.answer)) {
                    answer = new RagAnswer(buildSnippetAnswer(snippets, safeMemory), snippets, false);
                    answer.setMode(retrieval.getMode());
                    answer.setFallbackReason("llm_empty");
                } else {
                    answer = new RagAnswer(cleanResponse(llmResult.answer), snippets, true);
                    answer.setMode(retrieval.getMode());
                    answer.setPromptTokens(llmResult.promptTokens);
                    answer.setCompletionTokens(llmResult.completionTokens);
                }
            } catch (Exception e) {
                answer = new RagAnswer(buildSnippetAnswer(snippets, safeMemory), snippets, false);
                answer.setMode(retrieval.getMode());
                answer.setFallbackReason("llm_failed");
            }
        }
        answer.setHitCount(snippets.size());
        answer.setMaxScore(retrieval.getMaxScore());
        answer.setSafeMemory(safeMemory);
        answer.setLatencyMs(System.currentTimeMillis() - startedAt);
        saveCache(cacheKey, answer);
        return answer;
    }

    /**
     * Neo4j → LLM synthesis: feed graph disease data directly to LLM for natural language output.
     * No RAG retrieval — the graph IS the knowledge source.
     */
    public String synthesizeFromGraph(Map<String, Object> diseaseInfo, String question) {
        if (diseaseInfo == null || diseaseInfo.isEmpty() || !StringUtils.hasText(question)) {
            return null;
        }
        if (!agentProperties.isLlmEnabled() || !StringUtils.hasText(agentProperties.getApiKey())) {
            return null;
        }
        try {
            LlmCallResult result = callSynthesisLlm(diseaseInfo, question);
            if (result != null && StringUtils.hasText(result.answer)) {
                return cleanResponse(result.answer);
            }
        } catch (Exception e) {
            // fallback to structured text
        }
        return null;
    }

    private LlmCallResult callSynthesisLlm(Map<String, Object> diseaseInfo, String question) {
        MultiAgentProperties props = multiAgentProperties == null ? new MultiAgentProperties() : multiAgentProperties;
        int attempts = Math.max(0, props.getRagHttpRetryCount()) + 1;
        for (int i = 0; i < attempts; i++) {
            JSONObject body = new JSONObject();
            body.set("model", agentProperties.getModel());
            body.set("temperature", 0.3D);
            JSONArray messages = new JSONArray();
            messages.add(new JSONObject().set("role", "system").set("content",
                    "你是医疗知识助手。你只能依据提供的<疾病数据>回答用户问题，不得编造任何信息。回答用中文，2到5句，平实自然。"));
            messages.add(new JSONObject().set("role", "user").set("content",
                    buildSynthesisPrompt(diseaseInfo, question)));
            body.set("messages", messages);
            HttpRequest request = HttpRequest.post(agentProperties.getBaseUrl())
                    .header("Content-Type", "application/json")
                    .timeout(agentProperties.getTimeoutMillis())
                    .body(body.toString());
            if (StringUtils.hasText(agentProperties.getApiKey())) {
                request.header("Authorization", "Bearer " + agentProperties.getApiKey());
            }
            HttpResponse httpResponse = request.execute();
            if (httpResponse.getStatus() < 200 || httpResponse.getStatus() >= 300) {
                if (!isRetryableStatus(httpResponse.getStatus()) || i == attempts - 1) {
                    return null;
                }
                continue;
            }
            JSONObject root = JSONUtil.parseObj(httpResponse.body());
            JSONArray choices = root.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String answer = message == null ? null : message.getStr("content");
            JSONObject usage = root.getJSONObject("usage");
            return new LlmCallResult(answer,
                    usage == null ? estimateTokens(question) : usage.getInt("prompt_tokens", estimateTokens(question)),
                    usage == null ? estimateTokens(answer) : usage.getInt("completion_tokens", estimateTokens(answer)));
        }
        return null;
    }

    private String buildSynthesisPrompt(Map<String, Object> d, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("<疾病数据>\n");
        sb.append("疾病名称: ").append(stringValue(d.get("name"))).append("\n");
        String desc = stringValue(d.get("desc"));
        if (StringUtils.hasText(desc)) sb.append("简介: ").append(desc).append("\n");
        String cause = stringValue(d.get("cause"));
        if (StringUtils.hasText(cause)) sb.append("病因: ").append(cause).append("\n");
        String drugs = stringValue(d.get("drugs"));
        if (StringUtils.hasText(drugs)) sb.append("常用药物: ").append(drugs).append("\n");
        String cure = stringValue(d.get("cure_way"));
        if (StringUtils.hasText(cure)) sb.append("治疗方式: ").append(cure).append("\n");
        String prevent = stringValue(d.get("prevent"));
        if (StringUtils.hasText(prevent)) sb.append("预防措施: ").append(prevent).append("\n");
        String doEat = stringValue(d.get("do_eat"));
        if (StringUtils.hasText(doEat)) sb.append("适宜饮食: ").append(doEat).append("\n");
        String notEat = stringValue(d.get("not_eat"));
        if (StringUtils.hasText(notEat)) sb.append("禁忌饮食: ").append(notEat).append("\n");
        String check = stringValue(d.get("check"));
        if (StringUtils.hasText(check)) sb.append("相关检查: ").append(check).append("\n");
        sb.append("</疾病数据>\n\n");
        sb.append("<用户问题>\n").append(question).append("\n</用户问题>\n\n");
        sb.append("<注意>\n严格依据疾病的属性信息回答，不要编造。如果疾病数据中某个字段为空，跳过该方面即可，不要提及。\n</注意>\n");
        return sb.toString();
    }

    private LlmCallResult callLlm(String question,
                                  Map<String, Object> memory,
                                  List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets) {
        MultiAgentProperties props = multiAgentProperties == null ? new MultiAgentProperties() : multiAgentProperties;
        int attempts = Math.max(0, props.getRagHttpRetryCount()) + 1;
        for (int i = 0; i < attempts; i++) {
            JSONObject body = new JSONObject();
            body.set("model", agentProperties.getModel());
            body.set("temperature", 0.2D);
            JSONArray messages = new JSONArray();
            messages.add(new JSONObject().set("role", "system").set("content",
                    "你是医院挂号多Agent的说明助手。你只能依据<知识片段>和<上下文>中的结构化信息回答，禁止编造号源、医生排班、价格、就诊卡状态或挂号结果。回答用中文，2到4句。不要输出内部推理过程、过滤逻辑，不要解释为什么采纳或不采纳某些知识片段，不要引用\"当前memory\"等内部变量。不要输出<知识片段>中未提供的信息。"));
            messages.add(new JSONObject().set("role", "user").set("content", buildPrompt(question, memory, snippets)));
            body.set("messages", messages);
            HttpRequest request = HttpRequest.post(agentProperties.getBaseUrl())
                    .header("Content-Type", "application/json")
                    .timeout(agentProperties.getTimeoutMillis())
                    .body(body.toString());
            if (StringUtils.hasText(agentProperties.getApiKey())) {
                request.header("Authorization", "Bearer " + agentProperties.getApiKey());
            }
            HttpResponse httpResponse = request.execute();
            if (httpResponse.getStatus() < 200 || httpResponse.getStatus() >= 300) {
                if (!isRetryableStatus(httpResponse.getStatus()) || i == attempts - 1) {
                    return null;
                }
                continue;
            }
            JSONObject root = JSONUtil.parseObj(httpResponse.body());
            JSONArray choices = root.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String answer = message == null ? null : message.getStr("content");
            JSONObject usage = root.getJSONObject("usage");
            return new LlmCallResult(answer,
                    usage == null ? estimateTokens(question) : usage.getInt("prompt_tokens", estimateTokens(question)),
                    usage == null ? estimateTokens(answer) : usage.getInt("completion_tokens", estimateTokens(answer)));
        }
        return null;
    }

    private String buildPrompt(String question,
                               Map<String, Object> memory,
                               List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets) {
        StringBuilder builder = new StringBuilder();
        // XML-structured prompt — constrains LLM to only restate provided facts
        builder.append("<指令>\n");
        builder.append("你是医院挂号多Agent的说明助手。结合下文的<上下文>和<知识片段>，用2到4句中文回答<用户问题>。");
        builder.append("只能依据提供的知识片段回答，不得编造号源、医生排班、价格、就诊卡状态或挂号结果。");
        builder.append("不得输出你的内部推理过程、过滤逻辑，不得引用\"当前memory\"等内部变量。\n");
        builder.append("</指令>\n\n");

        builder.append("<上下文>\n");
        if (memory != null && !memory.isEmpty()) {
            String symptom = stringValue(memory.get("symptom"));
            if (StringUtils.hasText(symptom)) {
                builder.append("症状: ").append(symptom).append("\n");
            }
            String dept = firstText(memory.get("deptName"), memory.get("recommendedDeptName"));
            if (StringUtils.hasText(dept)) {
                builder.append("科室: ").append(dept).append("\n");
            }
            String gender = stringValue(memory.get("patientGender"));
            if (StringUtils.hasText(gender)) {
                builder.append("患者性别: ").append(gender).append("\n");
            }
            String doctorGender = stringValue(memory.get("doctorGender"));
            if (StringUtils.hasText(doctorGender)) {
                builder.append("医生性别偏好: ").append(doctorGender).append("\n");
            }
            if (memory.get("pendingOrder") instanceof Map) {
                builder.append("状态: 有待确认号源\n");
            }
        } else {
            builder.append("(无)\n");
        }
        builder.append("</上下文>\n\n");

        builder.append("<知识片段>\n");
        if (snippets != null && !snippets.isEmpty()) {
            for (int i = 0; i < snippets.size(); i++) {
                MultiAgentKnowledgeBase.KnowledgeSnippet snippet = snippets.get(i);
                builder.append(i + 1).append(". 【").append(snippet.getTitle())
                        .append("】").append(snippet.getContent()).append("\n");
            }
        } else {
            builder.append("(无匹配片段)\n");
        }
        builder.append("</知识片段>\n\n");

        builder.append("<用户问题>\n").append(question == null ? "" : question).append("\n</用户问题>\n\n");

        builder.append("<注意>\n");
        builder.append("结合上下文和知识片段，给出压缩解释。如果上下文中已有症状、科室、医生、日期、待确认号源，可以顺带解释当前推荐路径，但不要虚构实时业务事实。\n");
        builder.append("</注意>\n");
        return truncate(builder.toString());
    }

    private String buildSnippetAnswer(List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets, Map<String, Object> memory) {
        StringBuilder builder = new StringBuilder();
        if (memory != null && memory.get("pendingOrder") instanceof Map) {
            builder.append("当前已经命中过真实候选号源，所以系统才会继续保留这个推荐结果。");
        } else if (memory != null && StringUtils.hasText(stringValue(memory.get("deptSubName")))) {
            builder.append("当前会优先沿着你已给出的诊室继续查询，不会直接跨科室乱跳。");
        }
        for (int i = 0; i < snippets.size(); i++) {
            builder.append(snippets.get(i).getContent());
            if (i == 0) {
                break;
            }
        }
        return builder.toString();
    }

    private String buildFallbackAnswer(Map<String, Object> memory) {
        if (memory != null && memory.get("pendingOrder") instanceof Map) {
            return "当前之所以继续推荐这个结果，是因为系统已经查到了真实候选号源，并且后续还要经过条件校验和确认提交。";
        }
        return "当前解释能力主要覆盖挂号规则、就诊卡要求、普通挂号兜底和推荐路径说明；实时号源和挂号结果仍以真实工具查询为准。";
    }

    private Map<String, Object> sanitizeMemory(Map<String, Object> memory) {
        Map<String, Object> safe = new LinkedHashMap<String, Object>();
        if (memory == null || memory.isEmpty()) {
            return safe;
        }
        putIfHasText(safe, "deptName", memory.get("deptName"));
        putIfHasText(safe, "deptSubName", memory.get("deptSubName"));
        putIfHasText(safe, "doctorName", memory.get("doctorName"));
        putIfHasText(safe, "date", memory.get("date"));
        putIfHasText(safe, "requestedView", memory.get("requestedView"));
        putIfHasText(safe, "symptom", memory.get("symptom"));
        putIfHasText(safe, "symptoms", memory.get("symptoms"));
        putIfHasText(safe, "patientGender", memory.get("patientGender"));
        putIfHasText(safe, "doctorGender", memory.get("doctorGender"));
        putIfHasText(safe, "doctorAgePreference", memory.get("doctorAgePreference"));
        putIfHasText(safe, "recommendedDeptName", memory.get("recommendedDeptName"));
        putIfHasText(safe, "medicalConsultRiskLevel", memory.get("medicalConsultRiskLevel"));
        putIfHasText(safe, "medicalConsultAdvice", memory.get("medicalConsultAdvice"));
        if (memory.get("pendingOrder") instanceof Map) {
            Map<?, ?> pendingOrder = (Map<?, ?>) memory.get("pendingOrder");
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            putIfHasText(summary, "deptSubName", pendingOrder.get("deptSubName"));
            putIfHasText(summary, "doctorName", pendingOrder.get("doctorName"));
            putIfHasText(summary, "date", pendingOrder.get("date"));
            if (pendingOrder.get("slot") != null) {
                summary.put("slot", pendingOrder.get("slot"));
            }
            if (!summary.isEmpty()) {
                safe.put("pendingOrder", summary);
            }
        }
        return safe;
    }

    private void putIfHasText(Map<String, Object> map, String key, Object value) {
        if (StringUtils.hasText(stringValue(value))) {
            map.put(key, String.valueOf(value));
        }
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        int limit = multiAgentProperties == null || multiAgentProperties.getRagMaxContextChars() <= 0
                ? 600
                : multiAgentProperties.getRagMaxContextChars();
        return text.length() <= limit ? text : text.substring(0, limit);
    }

    private String buildCacheKey(String question, Map<String, Object> safeMemory) {
        return "multi_agent_rag_cache:" + Integer.toHexString((firstText(question, "") + JSONUtil.toJsonStr(safeMemory)).hashCode());
    }

    @SuppressWarnings("unchecked")
    private RagAnswer loadCache(String cacheKey) {
        if (!StringUtils.hasText(cacheKey) || redisTemplate == null) {
            return null;
        }
        Object value = redisTemplate.opsForValue().get(cacheKey);
        if (!(value instanceof Map)) {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        RagAnswer answer = new RagAnswer(stringValue(map.get("answer")), new ArrayList<MultiAgentKnowledgeBase.KnowledgeSnippet>(), booleanValue(map.get("llmGenerated")));
        answer.setMode(stringValue(map.get("mode")));
        answer.setFallbackReason(stringValue(map.get("fallbackReason")));
        answer.setHitCount(intValue(map.get("hitCount")));
        answer.setMaxScore(doubleValue(map.get("maxScore")));
        answer.setPromptTokens(intValue(map.get("promptTokens")));
        answer.setCompletionTokens(intValue(map.get("completionTokens")));
        return answer;
    }

    private void saveCache(String cacheKey, RagAnswer answer) {
        if (!StringUtils.hasText(cacheKey) || redisTemplate == null || answer == null || !StringUtils.hasText(answer.getAnswer())) {
            return;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("answer", answer.getAnswer());
        map.put("llmGenerated", answer.isLlmGenerated());
        map.put("mode", answer.getMode());
        map.put("fallbackReason", answer.getFallbackReason());
        map.put("hitCount", answer.getHitCount());
        map.put("maxScore", answer.getMaxScore());
        map.put("promptTokens", answer.getPromptTokens());
        map.put("completionTokens", answer.getCompletionTokens());
        long ttl = multiAgentProperties == null || multiAgentProperties.getRagQueryCacheMinutes() <= 0
                ? 15L
                : multiAgentProperties.getRagQueryCacheMinutes();
        redisTemplate.opsForValue().set(cacheKey, map, ttl, TimeUnit.MINUTES);
    }

    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private String cleanResponse(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        // Strip LLM internal reasoning patterns that should never reach users
        text = text.replaceAll("当前\\s*memory\\s*中[^，。；,!！\\n]*[，。；,!！]", "");
        text = text.replaceAll("当前\\s*memory\\s*中[^。！\\n]*[。！]", "");
        text = text.replaceAll("与\\S+无关[，。]*故不采纳该推荐[。]*", "");
        text = text.replaceAll("所有分诊建议均严格依据[^。]*[。]", "");
        text = text.replaceAll("不采纳[^。！\\n]*[。！]", "");
        text = text.replaceAll("未提供[^。！\\n]*[。！]", "");
        // Remove repeated punctuation
        text = text.replaceAll("[。！]{2,}", "。");
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    private String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            String text = stringValue(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    private int intValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return Integer.parseInt((String) value);
        }
        return 0;
    }

    private double doubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return Double.parseDouble((String) value);
        }
        return 0D;
    }

    private static class LlmCallResult {
        private final String answer;
        private final int promptTokens;
        private final int completionTokens;

        private LlmCallResult(String answer, int promptTokens, int completionTokens) {
            this.answer = answer;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }
    }

    public static class RagAnswer {
        private final String answer;
        private final List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets;
        private final boolean llmGenerated;
        private String mode;
        private int hitCount;
        private double maxScore;
        private String fallbackReason;
        private long latencyMs;
        private int promptTokens;
        private int completionTokens;
        private boolean cacheHit;
        private Map<String, Object> safeMemory;

        public RagAnswer(String answer,
                         List<MultiAgentKnowledgeBase.KnowledgeSnippet> snippets,
                         boolean llmGenerated) {
            this.answer = answer;
            this.snippets = snippets;
            this.llmGenerated = llmGenerated;
        }

        public String getAnswer() {
            return answer;
        }

        public List<MultiAgentKnowledgeBase.KnowledgeSnippet> getSnippets() {
            return snippets;
        }

        public boolean isLlmGenerated() {
            return llmGenerated;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getHitCount() {
            return hitCount;
        }

        public void setHitCount(int hitCount) {
            this.hitCount = hitCount;
        }

        public double getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(double maxScore) {
            this.maxScore = maxScore;
        }

        public String getFallbackReason() {
            return fallbackReason;
        }

        public void setFallbackReason(String fallbackReason) {
            this.fallbackReason = fallbackReason;
        }

        public long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
        }

        public int getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }

        public boolean isCacheHit() {
            return cacheHit;
        }

        public void setCacheHit(boolean cacheHit) {
            this.cacheHit = cacheHit;
        }

        public Map<String, Object> getSafeMemory() {
            return safeMemory;
        }

        public void setSafeMemory(Map<String, Object> safeMemory) {
            this.safeMemory = safeMemory;
        }
    }
}
