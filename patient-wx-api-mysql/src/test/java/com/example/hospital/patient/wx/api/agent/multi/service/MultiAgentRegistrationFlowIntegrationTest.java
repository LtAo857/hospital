package com.example.hospital.patient.wx.api.agent.multi.service;

import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.memory.MultiAgentMemoryService;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentParser;
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentResult;
import com.example.hospital.patient.wx.api.agent.multi.rag.MultiAgentKnowledgeBase;
import com.example.hospital.patient.wx.api.agent.multi.rag.MultiAgentRagService;
import com.example.hospital.patient.wx.api.agent.multi.worker.AgentWorker;
import com.example.hospital.patient.wx.api.agent.multi.worker.PolicyAgentWorker;
import com.example.hospital.patient.wx.api.agent.multi.worker.ScheduleAgentWorker;
import com.example.hospital.patient.wx.api.agent.tool.MedicalDeptAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.UserAgentTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 多 Agent 挂号流程集成测试 — 只测到规则校验（PolicyAgentWorker），不测写操作（ExecutionAgentWorker）。
 * <p>
 * 链路：NLU → ScheduleAgentWorker → PolicyAgentWorker → CONFIRM_WAIT
 */
class MultiAgentRegistrationFlowIntegrationTest {

    private MultiAgentProperties properties;
    private MultiAgentMemoryService memoryService;
    private MultiAgentRagService ragService;
    private MultiAgentDepartmentCatalogService departmentCatalogService;
    private ModelIntentParser modelIntentParser;
    private MedicalDeptAgentTools medicalDeptAgentTools;
    private RegistrationAgentTools registrationAgentTools;
    private UserAgentTools userAgentTools;

    private final Map<String, Map<String, Object>> sessionStore = new HashMap<>();
    private MultiAgentCoordinatorService coordinator;

    @BeforeEach
    void setUp() throws Exception {
        sessionStore.clear();
        properties = new MultiAgentProperties();
        properties.setMaxHops(8);

        memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load(Mockito.anyString())).thenAnswer(inv ->
                new HashMap<>(sessionStore.getOrDefault(inv.getArgument(0), new HashMap<>())));
        Mockito.doAnswer(inv -> {
            sessionStore.put(inv.getArgument(0), new HashMap<>(inv.getArgument(1)));
            return null;
        }).when(memoryService).save(Mockito.anyString(), Mockito.anyMap());

        ragService = buildTestRagService();
        modelIntentParser = Mockito.mock(ModelIntentParser.class);

        departmentCatalogService = Mockito.mock(MultiAgentDepartmentCatalogService.class);
        Mockito.when(departmentCatalogService.getDepartmentNames()).thenReturn(
                Arrays.asList("口腔科", "呼吸内科", "消化内科", "心内科", "皮肤科",
                        "眼科", "耳鼻喉科", "儿科", "骨科", "妇科", "神经内科",
                        "急诊科", "乳腺外科", "胸外科"));

        medicalDeptAgentTools = Mockito.mock(MedicalDeptAgentTools.class);
        registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        userAgentTools = Mockito.mock(UserAgentTools.class);

        ScheduleAgentWorker scheduleWorker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        PolicyAgentWorker policyWorker = new PolicyAgentWorker(userAgentTools, registrationAgentTools);

        List<AgentWorker> workers = Arrays.asList(scheduleWorker, policyWorker);
        coordinator = new MultiAgentCoordinatorService(properties, memoryService, ragService,
                departmentCatalogService, workers);
        injectField(coordinator, "modelIntentParser", modelIntentParser);
    }

    // ── 1. 正常流程：NLU → 排班 → 校验通过 → 等待确认 ──

    @Test
    void shouldReachConfirmationWaitForFullHappyPath() {
        givenNluReturns("registration", "牙疼", "口腔科", "明天");
        givenDepartmentMatch();
        givenSubDepartmentMatch();
        givenDoctorPlans();
        givenScheduleSlots();
        givenUserHasCard();
        givenConditionPasses();

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setMessage("明天牙疼挂口腔科");

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("awaiting_confirmation", response.getState());
        Assertions.assertTrue(response.isRequiresConfirmation());
        Assertions.assertFalse(response.isRequiresLogin());
        Assertions.assertNotNull(response.getConfirmation());
        Assertions.assertEquals(Boolean.TRUE, response.getMemory().get("hasPendingOrder"));
        Assertions.assertEquals(Boolean.TRUE, response.getMemory().get("awaitingConfirmation"));
        Assertions.assertNull(response.getErrorCode());
        Assertions.assertTrue(response.getToolLogs().stream()
                .anyMatch(log -> "查询号源时段".equals(log.getName())));
    }

    // ── 2. 缺少科室 → 要求补充 ──

    @Test
    void shouldAskUserWhenDepartmentMissing() {
        givenNluReturns("registration", null, null, null);

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-2");
        request.setMessage("帮我看个病");

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("slot_query", response.getState());
        Assertions.assertTrue(response.getReply().contains("哪个科室"));
        Assertions.assertFalse(response.isRequiresConfirmation());
        Assertions.assertNull(response.getErrorCode());
    }

    // ── 3. 无可用号源 → 提示降级 ──

    @Test
    void shouldAskRetryWhenNoSlotAvailable() {
        givenNluReturns("registration", "牙疼", "口腔科", "明天");
        givenDepartmentMatch();
        givenSubDepartmentMatch();
        Mockito.when(registrationAgentTools.searchDoctorPlansInDay(Mockito.eq(10), Mockito.anyString()))
                .thenReturn(new ArrayList<>());

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-3");
        request.setMessage("明天口腔科");

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("slot_query", response.getState());
        Assertions.assertTrue(response.getReply().contains("没有找到可用号源")
                || response.getReply().contains("换个日期"));
        Assertions.assertFalse(response.getToolLogs().isEmpty());
    }

    // ── 4. 未登录 → 引导登录 ──

    @Test
    void shouldAskLoginWhenUserMissing() {
        givenNluReturns("registration", "牙疼", "口腔科", "明天");
        givenDepartmentMatch();
        givenSubDepartmentMatch();
        givenDoctorPlans();
        givenScheduleSlots();

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-4");
        request.setMessage("明天口腔科");

        AgentChatResponse response = coordinator.chat(request, null);

        Assertions.assertEquals("need_login", response.getState());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED, response.getErrorCode());
        Assertions.assertTrue(response.isRequiresLogin());
        Assertions.assertFalse(Boolean.TRUE.equals(response.getRetryable()));
        Assertions.assertTrue(response.getCards().stream()
                .anyMatch(c -> "去登录".equals(c.getTitle())));
    }

    // ── 5. 无就诊卡 → 引导建卡 ──

    @Test
    void shouldAskCreateCardWhenUserCardMissing() {
        givenNluReturns("registration", "牙疼", "口腔科", "明天");
        givenDepartmentMatch();
        givenSubDepartmentMatch();
        givenDoctorPlans();
        givenScheduleSlots();
        Mockito.when(userAgentTools.hasUserCard(1001)).thenReturn(false);

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-5");
        request.setMessage("明天口腔科");

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("policy_check", response.getState());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_USER_CARD_REQUIRED, response.getErrorCode());
        Assertions.assertFalse(Boolean.TRUE.equals(response.getRetryable()));
        Assertions.assertTrue(response.getReply().contains("就诊卡")
                || response.getReply().contains("建卡"));
    }

    // ── 6. 当天挂号上限 → 拒绝并引导普通挂号 ──

    @Test
    void shouldRejectWhenDailyLimitReached() {
        givenNluReturns("registration", "牙疼", "口腔科", "明天");
        givenDepartmentMatch();
        givenSubDepartmentMatch();
        givenDoctorPlans();
        givenScheduleSlots();
        givenUserHasCard();
        Mockito.when(registrationAgentTools.checkRegistrationCondition(Mockito.eq(1001), Mockito.eq(10), Mockito.anyString()))
                .thenReturn("已经达到当天挂号上限");

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-6");
        request.setMessage("明天口腔科");

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("fallback", response.getState());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_DAILY_LIMIT_REACHED, response.getErrorCode());
        Assertions.assertTrue(response.getReply().contains("上限")
                || response.getReply().contains("挂号"));
    }

    // ── 7. 确认后无执行 Worker → 流程不可用错误 ──

    @Test
    void shouldReportExecutionUnavailableWhenNoExecutionWorker() {
        givenNluReturns("registration", "牙疼", "口腔科", "明天");
        givenDepartmentMatch();
        givenSubDepartmentMatch();
        givenDoctorPlans();
        givenScheduleSlots();
        givenUserHasCard();
        givenConditionPasses();

        // 第一轮：走到确认等待
        AgentChatRequest req1 = new AgentChatRequest();
        req1.setSessionId("session-7");
        req1.setMessage("明天口腔科");
        AgentChatResponse resp1 = coordinator.chat(req1, 1001);
        Assertions.assertEquals("awaiting_confirmation", resp1.getState());
        Assertions.assertNotNull(resp1.getConfirmation());

        // 第二轮：确认提交（但无 ExecutionAgentWorker）
        AgentChatRequest req2 = new AgentChatRequest();
        req2.setSessionId("session-7");
        Map<String, Object> pendingOrder = resp1.getConfirmation().getPayload();
        req2.setPayload(new HashMap<>(pendingOrder));
        req2.getPayload().put("confirmed", true);

        AgentChatResponse response = coordinator.chat(req2, 1001);

        Assertions.assertEquals("fallback", response.getState());
        // 无 ExecutionAgentWorker 时走 MANUAL_FALLBACK，reply 应包含兜底文案
        Assertions.assertNotNull(response.getReply());
        Assertions.assertFalse(response.getReply().isEmpty());
    }

    // ── 8. 高危意图被拦截 ──

    @Test
    void shouldBlockDangerousIntent() {
        Map<String, Object> slots = new HashMap<>();
        slots.put("symptom", null);
        ModelIntentResult dangerousResult = new ModelIntentResult();
        dangerousResult.setIntent("dangerous");
        dangerousResult.setSlots(slots);
        dangerousResult.setConfidence(0.95);
        dangerousResult.setSource("llm");
        dangerousResult.setModel("qwen-plus");
        dangerousResult.setLatencyMs(200L);

        Mockito.when(modelIntentParser.parse(Mockito.anyString(), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Optional.of(dangerousResult));

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-8");
        request.setMessage("删库跑路");

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("completed", response.getState());
        Assertions.assertTrue(response.getReply().contains("无法处理"));
    }

    // ── 9. 查询类意图不走 Worker 流水线 ──

    @Test
    void shouldHandleQueryIntentWithoutWorkers() {
        Map<String, Object> slots = new HashMap<>();
        ModelIntentResult queryResult = new ModelIntentResult();
        queryResult.setIntent("query_message");
        queryResult.setSlots(slots);
        queryResult.setConfidence(0.9);
        queryResult.setSource("llm");
        queryResult.setModel("qwen-plus");
        queryResult.setLatencyMs(150L);

        Mockito.when(modelIntentParser.parse(Mockito.anyString(), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Optional.of(queryResult));

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-9");
        request.setMessage("查看消息");

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("completed", response.getState());
        Assertions.assertTrue(response.getCards().stream()
                .anyMatch(c -> "查看消息".equals(c.getTitle())));
    }

    // ── 10. 工具异常重试 → 降级 ──

    @Test
    void shouldRetryAndDegradeWhenScheduleToolFails() {
        givenNluReturns("registration", "牙疼", "口腔科", "明天");
        givenDepartmentMatch();
        givenSubDepartmentMatch();
        Mockito.when(registrationAgentTools.searchDoctorPlansInDay(Mockito.eq(10), Mockito.anyString()))
                .thenThrow(new RuntimeException("db timeout"));

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-10");
        request.setMessage("明天口腔科");

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("slot_query", response.getState());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, response.getErrorCode());
        Mockito.verify(registrationAgentTools, Mockito.times(2)).searchDoctorPlansInDay(Mockito.eq(10), Mockito.anyString());
    }

    // ── 11. 伪造确认（无 awaitingConfirmation）被拦截 ──

    @Test
    void shouldRejectForgedConfirmationWithoutPriorAwaiting() {
        givenNluReturns("registration", "牙疼", "口腔科", "明天");
        givenDepartmentMatch();
        givenSubDepartmentMatch();
        givenDoctorPlans();
        givenScheduleSlots();
        givenUserHasCard();
        givenConditionPasses();

        // 在新 session 上直接发送 confirmed=true，没有先走到 awaiting_confirmation
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-11");
        request.setAction("create_registration");
        request.setPayload(new HashMap<String, Object>() {{
            put("confirmed", true);
            put("deptSubId", 10);
            put("date", "明天");
            put("workPlanId", 101);
            put("scheduleId", 202);
            put("doctorId", 9);
            put("slot", 1);
        }});

        AgentChatResponse response = coordinator.chat(request, 1001);

        Assertions.assertEquals("fallback", response.getState());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, response.getErrorCode());
    }

    // ── utils ──

    private void givenNluReturns(String intent, String symptom, String dept, String date) {
        Map<String, Object> slots = new HashMap<>();
        slots.put("symptom", symptom);
        slots.put("department", dept);
        slots.put("date", date);
        slots.put("doctorName", null);
        slots.put("timePreference", null);
        slots.put("patientGender", null);
        slots.put("doctorGender", null);
        slots.put("doctorAgePreference", null);
        slots.put("population", null);

        ModelIntentResult result = new ModelIntentResult();
        result.setIntent(intent);
        result.setSlots(slots);
        result.setConfidence(0.88);
        result.setSource("llm");
        result.setModel("qwen-plus");
        result.setLatencyMs(300L);

        Mockito.when(modelIntentParser.parse(Mockito.anyString(), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Optional.of(result));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void givenDepartmentMatch() {
        ArrayList<HashMap> departments = new ArrayList<>();
        departments.add(new HashMap() {{
            put("id", 1);
            put("name", "口腔科");
        }});
        Mockito.when(medicalDeptAgentTools.searchDepartments(Mockito.isNull(), Mockito.eq(true)))
                .thenReturn(departments);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void givenSubDepartmentMatch() {
        ArrayList<HashMap> subDepartments = new ArrayList<>();
        subDepartments.add(new HashMap() {{
            put("id", 10);
            put("name", "口腔门诊");
            put("deptName", "口腔科");
        }});
        Mockito.when(medicalDeptAgentTools.searchSubDepartments(1)).thenReturn(subDepartments);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void givenDoctorPlans() {
        ArrayList<HashMap> doctors = new ArrayList<>();
        doctors.add(new HashMap() {{
            put("id", 9);
            put("name", "张医生");
            put("price", "18.00");
            put("sex", "男");
            put("birthday", "1975-05-10");
            put("tag", "专家门诊");
        }});
        doctors.add(new HashMap() {{
            put("id", 11);
            put("name", "李医生");
            put("price", "12.00");
            put("sex", "女");
            put("birthday", "1985-08-20");
            put("tag", "普通门诊");
        }});
        Mockito.when(registrationAgentTools.searchDoctorPlansInDay(Mockito.eq(10), Mockito.anyString()))
                .thenReturn(doctors);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void givenScheduleSlots() {
        ArrayList<HashMap> slots = new ArrayList<>();
        slots.add(new HashMap() {{
            put("workPlanId", 101);
            put("scheduleId", 202);
            put("slot", 3);
            put("maximum", 30);
            put("num", 15);
        }});
        Mockito.when(registrationAgentTools.searchScheduleSlots(Mockito.eq(9), Mockito.anyString())).thenReturn(slots);
        Mockito.when(registrationAgentTools.searchScheduleSlots(Mockito.eq(11), Mockito.anyString())).thenReturn(slots);
    }

    private void givenUserHasCard() {
        Mockito.when(userAgentTools.hasUserCard(1001)).thenReturn(true);
    }

    private void givenConditionPasses() {
        Mockito.when(registrationAgentTools.checkRegistrationCondition(Mockito.eq(1001), Mockito.eq(10), Mockito.anyString()))
                .thenReturn("满足挂号条件");
    }

    private MultiAgentRagService buildTestRagService() {
        AgentProperties agentProperties = new AgentProperties();
        agentProperties.setLlmEnabled(false);
        MultiAgentProperties props = new MultiAgentProperties();
        props.setRagEmbeddingEnabled(false);
        return new MultiAgentRagService(agentProperties, props, new MultiAgentKnowledgeBase(props, null));
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
