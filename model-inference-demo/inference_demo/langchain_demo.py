"""
LangChain 挂号 Agent 探索性 Demo

目标：用 LangChain 的 Tool + Agent 模式实现相同的挂号流程，
      对比理解"LangChain Agent 自动工具调用" vs "LangGraph 显式图编排"的区别。

LangChain 核心理念：给 LLM 一套工具 + 一个 prompt，让 LLM 自主决定
                     调用哪个工具、什么时候停止，不预先画出流程图。

与 LangGraph Demo 的关键差异：
  - LangGraph：你画出图（triage → search_departments → search_doctors → confirm → execute），
               LLM 严格按图走，每步可审计、可插桩、可阻断。
  - LangChain：你给工具 + ReAct prompt，LLM 自己决定 Observation → Thought → Action，
               灵活但不可控，适合探索型任务。

运行方式：
  python -m inference_demo.langchain_demo
"""

from __future__ import annotations

import json
import operator
import os
import time
from typing import Any, Dict, List, Optional

# ── LangChain 核心导入 ──────────────────────────────────────────
# 如果没装 langchain，demo 会降级为本地模拟模式（仍可演示 Agent 循环思想）
try:
    from langchain_core.tools import tool as lc_tool
    LANGCHAIN_AVAILABLE = True
except ImportError:
    LANGCHAIN_AVAILABLE = False

    # 提供一个假装饰器，让 demo 不装 langchain 也能跑
    def lc_tool(func):
        """降级装饰器：无 langchain 时保持函数原型不���。"""
        func._is_tool = True
        return func


# ── 与 LangGraph Demo 共享的 Mock 数据 ───────────────────────────

SYMPTOM_DEPT_MAP: Dict[str, List[str]] = {
    "牙疼": ["口腔科", "牙体牙髓科"],
    "牙痛": ["口腔科"],
    "牙龈肿": ["口腔科"],
    "咳嗽": ["呼吸内科"],
    "发烧": ["呼吸内科", "发热门诊"],
    "胃疼": ["消化内科"],
    "头疼": ["神经内科"],
    "骨折": ["骨科"],
    "皮疹": ["皮肤科"],
}

MOCK_DOCTORS: Dict[str, List[Dict[str, Any]]] = {
    "口腔科": [
        {"name": "张医生", "title": "主任医师", "slots": ["08:00", "09:30", "14:00"]},
        {"name": "李医生", "title": "主治医师", "slots": ["10:00", "15:30"]},
    ],
    "呼吸内科": [
        {"name": "王医生", "title": "副主任医师", "slots": ["08:30", "11:00"]},
    ],
    "消化内科": [
        {"name": "赵医生", "title": "主任医师", "slots": ["09:00", "10:00", "13:30"]},
    ],
    "神经内科": [
        {"name": "刘医生", "title": "主任医师", "slots": ["08:00", "14:30"]},
    ],
    "骨科": [
        {"name": "陈医生", "title": "主治医师", "slots": ["10:30", "16:00"]},
    ],
    "皮肤科": [
        {"name": "周医生", "title": "副主任医师", "slots": ["09:00", "11:30"]},
    ],
}

DEFAULT_DOCTOR = {"name": "值班医生", "title": "主治医师", "slots": ["08:00", "10:00", "14:00", "16:00"]}


# ── 核心函数（保持原生可调用，与 @tool 装饰器分离） ─────────────────

def _search_departments_impl(symptom: str) -> str:
    """根据用户症状查询可挂号的科室。"""
    for key, depts in SYMPTOM_DEPT_MAP.items():
        if key in symptom or symptom in key:
            return json.dumps({"departments": depts, "matched_symptom": key}, ensure_ascii=False)
    return json.dumps({"departments": ["全科门诊"], "matched_symptom": symptom}, ensure_ascii=False)


def _search_doctors_impl(department: str, date: str = "") -> str:
    """查询某科室的出诊医生及可挂号时段。"""
    for dept_key, doctors in MOCK_DOCTORS.items():
        if dept_key in department or department in dept_key:
            return json.dumps({"department": dept_key, "doctors": doctors, "date": date or ""}, ensure_ascii=False)
    return json.dumps({"department": department, "doctors": [dict(DEFAULT_DOCTOR)], "date": date or ""}, ensure_ascii=False)


def _submit_registration_impl(department: str, doctor_name: str, date: str, time_slot: str) -> str:
    """提交挂号申请（模拟写操作）。"""
    reg_id = f"REG{doctor_name[:1]}{date.replace('-', '')[4:]}"
    return json.dumps({
        "status": "success",
        "registration_id": reg_id,
        "department": department,
        "doctor": doctor_name,
        "date": date,
        "time_slot": time_slot,
        "message": f"挂号成功！{department} {doctor_name} {date} {time_slot}",
    }, ensure_ascii=False)


# LangChain 工具包装：有 langchain 时用 @tool 生成标准 Tool 对象
if LANGCHAIN_AVAILABLE:
    search_departments = lc_tool(_search_departments_impl)
    search_doctors = lc_tool(_search_doctors_impl)
    submit_registration = lc_tool(_submit_registration_impl)
else:
    search_departments = _search_departments_impl
    search_doctors = _search_doctors_impl
    submit_registration = _submit_registration_impl


def _call_tool(tool, **kwargs):
    """统一工具调用：兼容 LangChain StructuredTool 和原生函数。"""
    if hasattr(tool, 'invoke'):
        return tool.invoke(kwargs)
    else:
        return tool(**kwargs)


# ── LangChain ReAct 风格 Prompt ──────────────────────────────────

REACT_SYSTEM_PROMPT = """你是医院挂号助手，帮用户在以下工具中选择并完成挂号。

工具（仅使用 JSON 格式响应）：

1. search_departments: 输入症状 → 获取科室列表
2. search_doctors: 输入科室 + 日期 → 获取医生和时段
3. submit_registration: 输入科室/医生/日期/时段 → 提交挂号

响应格式：
{
  "thought": "分析用户输入，判断当前需要做什么",
  "action": "工具名 或 final_answer",
  "action_input": {参数},
  "final_answer": "仅 action=final_answer 时需要，是对用户的最终回复"
}

规则：
- 用户说"牙疼"→ 先调 search_departments(symptom="牙疼")
- 拿到科室后 → 调 search_doctors 查医生
- 用户确认或医生说有号 → 调 submit_registration 提交
- 每一步只能选一个工具
- 任何时候用户表达不相关意图（闲聊、解释、问为什么），action 直接设为 final_answer
- 挂号完成后 action 设为 final_answer，给出完整结果
"""


# ── LLM 调用（DashScope，与 parser.py 共用配置） ──────────────────

def _call_dashscope(messages: List[Dict[str, str]]) -> Optional[str]:
    """调 DashScope qwen-plus，失败返回 None 走降级。"""
    import urllib.request
    import urllib.error

    body = json.dumps({
        "model": "qwen-plus",
        "temperature": 0.1,
        "messages": messages,
        "response_format": {"type": "json_object"},
    }).encode("utf-8")

    req = urllib.request.Request(
        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        data=body,
        headers={
            "Authorization": "Bearer sk-03747d36451b4a9ca5a01fb5608575fd",
            "Content-Type": "application/json",
        },
    )
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        raw = json.loads(resp.read().decode("utf-8"))
        return raw.get("choices", [{}])[0].get("message", {}).get("content", "")
    except Exception:
        return None


# ── 本地规则 Agent（无 LLM 时的降级，演示相同 Agent 循环模式）─────

def _local_agent_step(user_input: str, iteration: int, context: Dict[str, Any]) -> Dict[str, Any]:
    """本地规则模拟 ReAct Agent 的一个思考步。
    不调 LLM，用规则模拟 Thought → Action → Observation 循环。
    """
    context = dict(context)

    # Step 0: 意图判断
    if iteration == 0:
        is_reg = any(kw in user_input for kw in ["挂", "预约", "挂号", "看", "号源", "就诊", "牙", "咳", "疼", "骨折", "疹", "烧", "科"])
        is_explain = any(kw in user_input for kw in ["为什么", "为啥", "解释", "推荐理由"])
        if is_explain or not is_reg:
            return {
                "action": "final_answer",
                "final_answer": "您好！请告诉我您的症状或想挂的科室，例如「明天牙疼想挂号」。",
            }
        context["need_registration"] = True

    # Step 1: 还没有科室 → 提取症状查科室
    if not context.get("department"):
        symptom = ""
        for key in SYMPTOM_DEPT_MAP:
            if key in user_input:
                symptom = key
                break
        if not symptom:
            symptom = "牙疼"  # 演示兜底
        result = json.loads(_call_tool(search_departments, symptom=symptom))
        depts = result.get("departments", ["全科门诊"])
        dept = depts[0]
        context["symptom"] = symptom
        context["department"] = dept
        return {
            "action": "search_departments",
            "action_input": {"symptom": symptom},
            "_observation": f"匹配到科室：{depts}",
            "_context": context,
        }

    # Step 2: 有科室但没有医生 → 查医生
    if not context.get("doctors"):
        dept = context["department"]
        # 提取日期
        date = "2026-06-04"
        date_map = {"今天": "2026-06-04", "明天": "2026-06-05", "后天": "2026-06-06"}
        for cn, iso in date_map.items():
            if cn in user_input:
                date = iso
                break
        result = json.loads(_call_tool(search_doctors, department=dept, date=date))
        doctors = result.get("doctors", [dict(DEFAULT_DOCTOR)])
        context["doctors"] = doctors
        context["date"] = date
        doc = doctors[0]
        return {
            "action": "search_doctors",
            "action_input": {"department": dept},
            "_observation": f"查到{len(doctors)}位医生：{[d['name'] for d in doctors]}",
            "_context": context,
        }

    # Step 3: 有医生有科室 → 提交挂号
    if not context.get("submitted"):
        dept = context["department"]
        doc = context["doctors"][0]
        date = context.get("date", "2026-06-04")
        slot = doc["slots"][0]
        result = json.loads(_call_tool(submit_registration, department=dept, doctor_name=doc["name"], date=date, time_slot=slot))
        context["submitted"] = True
        return {
            "action": "submit_registration",
            "action_input": {"department": dept, "doctor_name": doc["name"], "date": date, "time_slot": slot},
            "_observation": result["message"],
            "_context": context,
        }

    # 完成
    return {
        "action": "final_answer",
        "final_answer": f"挂号已完成！{context.get('department', '')} 挂号单号见上一步。",
    }


# ── 主 Agent 运行器 ──────────────────────────────────────────────

class LangChainRegistrationAgent:
    """LangChain 风格挂号 Agent。

    两种模式：
      - llm 模式：真调 DashScope，LLM 自主 Thought→Action→Observation
      - local 模式（降级）：本地规则模拟相同的 Agent 循环
    """

    def __init__(self, use_llm: bool = True):
        self.use_llm = use_llm and LANGCHAIN_AVAILABLE
        self.tools = {
            "search_departments": search_departments,
            "search_doctors": search_doctors,
            "submit_registration": submit_registration,
        }

    def run(self, user_input: str, max_iterations: int = 5) -> List[Dict[str, Any]]:
        """运行 Agent 循环，返回每一步的 Thought/Action/Observation 日志。"""
        steps: List[Dict[str, Any]] = []
        context: Dict[str, Any] = {}

        messages = [
            {"role": "system", "content": REACT_SYSTEM_PROMPT},
            {"role": "user", "content": user_input},
        ]

        for i in range(max_iterations):
            step_start = time.perf_counter()

            if self.use_llm:
                step = self._llm_step(messages)
            else:
                step = _local_agent_step(user_input, i, context)
                # 把 observation 注入上下文
                if "_context" in step:
                    context = step.pop("_context")

            step["iteration"] = i
            step["latency_ms"] = max(1, int((time.perf_counter() - step_start) * 1000))
            steps.append(step)

            # 把这一步的结果加到 messages 中
            if "action_input" in step:
                observation = step.get("_observation", "")
                if not observation:
                    tool_name = step.get("action", "")
                    tool = self.tools.get(tool_name)
                    if tool:
                        observation = _call_tool(tool, **step["action_input"])
                step["observation"] = observation
                messages.append({
                    "role": "assistant",
                    "content": f"Action: {step['action']}({json.dumps(step['action_input'], ensure_ascii=False)})",
                })
                messages.append({"role": "user", "content": f"Observation: {observation}"})

            if step.get("action") == "final_answer":
                break

        return steps

    def _llm_step(self, messages: List[Dict[str, str]]) -> Dict[str, Any]:
        """真正的 LLM ReAct 一步。"""
        content = _call_dashscope(messages)
        if not content:
            return {"action": "final_answer", "final_answer": "抱歉，服务暂时不可用，请稍后再试。"}
        try:
            parsed = json.loads(content)
            return dict(parsed)
        except json.JSONDecodeError:
            return {"action": "final_answer", "final_answer": content}


# ── 便捷运行函数 ─────────────────────────────────────────────────

def run(user_input: str, use_llm: bool = False) -> List[Dict[str, Any]]:
    """执行一次 Agent 任务，返回步骤日志。"""
    agent = LangChainRegistrationAgent(use_llm=use_llm)
    return agent.run(user_input)


def pretty_print(steps: List[Dict[str, Any]], user_input: str) -> None:
    """打印 Agent 循环日志。"""
    print("\n" + "═" * 60)
    mode = "LangChain Agent (本地规则模式)"
    print(f"  🔧 {mode}")
    print(f"  📥 用户输入: {user_input}")
    print("═" * 60)
    for step in steps:
        i = step.get("iteration", "?")
        action = step.get("action", "?")
        action_input = step.get("action_input", {})
        thought = step.get("thought", "")
        observation = step.get("observation", "")
        final = step.get("final_answer", "")
        lat = step.get("latency_ms", 0)

        if thought:
            print(f"  💭 Step {i} | Thought: {thought}")
        if action and action != "final_answer":
            print(f"  🔨 Step {i} | Action: {action}({json.dumps(action_input, ensure_ascii=False)})")
        if observation:
            obs_short = observation if len(observation) <= 120 else observation[:120] + "..."
            print(f"  👁  Step {i} | Observation: {obs_short}")
        if final:
            print(f"  ✅ Step {i} | Final Answer: {final}")
        print(f"  ⏱  Step {i} | 耗时: {lat}ms")
        print("─" * 60)
    print(f"\n  📊 共 {len(steps)} 步完成\n")


# ── 与 LangGraph 的对比说明 ─────────────────────────────────────

def print_comparison() -> None:
    """打印 LangChain vs LangGraph 对比。"""
    print("""
╔══════════════════════════════════════════════════════════════════╗
║              LangChain Agent vs LangGraph 对比                  ║
╠══════════════════════════════════════════════════════════════════╣
║                    │  LangChain Agent   │  LangGraph            ║
║  ──────────────────┼────────────────────┼───────────────────────║
║  控制流            │ LLM 自主决定       │ 开发者显式画图        ║
║  可预测性          │ 低（每次可能不同） │ 高（按图走）          ║
║  审计性            │ 弱（只有对话记录） │ 强（每节点可插桩）    ║
║  适合场景          │ 探索型/闲聊       │ 确定性业务流水线      ║
║  新增节点成本      │ 加工具 + 改prompt │ 加节点 + 改边         ║
║  安全性            │ 靠 prompt 约束    │ 每步可阻断/校验       ║
║  当前项目用在哪    │ 未使用            │ 未使用（自研流水线）  ║
╚══════════════════════════════════════════════════════════════════╝

面试关键句：
  "当前项目是 4 Worker 确定性流水线——挂号流程是固定的，
  每一步需要审计和阻断能力，用图编排/流水线更合适。
  LangChain Agent 让 LLM 自主决定走哪步，适合探索型任务。
  我基于 LangGraph 做了探索性 Demo 理解它的设计思路，
  但生产环境根据场景选择了自研编排。"
""")


# ── 入口 ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    print_comparison()

    # ── 用例 1：正常挂号流程 ──
    print("🏥 用例 1 —— 正常挂号：'明天牙疼想挂口腔科'")
    result1 = run(user_input="明天牙疼想挂口腔科")
    pretty_print(result1, "明天牙疼想挂口腔科")

    # ── 用例 2：非挂号意图 ──
    print("🏥 用例 2 —— 非挂号意图：'为什么推荐这位医生'")
    result2 = run(user_input="为什么推荐这位医生")
    pretty_print(result2, "为什么推荐这位医生")

    # ── 用例 3：从症状到挂号 ──
    print("🏥 用例 3 —— 症状查科室：'今天头疼'")
    result3 = run(user_input="今天头疼")
    pretty_print(result3, "今天头疼")

    # ── 用例 4：带时段信息 ──
    print("🏥 用例 4 —— 带时段：'下午骨折想挂骨科'")
    result4 = run(user_input="下午骨折想挂骨科")
    pretty_print(result4, "下午骨折想挂骨科")

    print("✅ LangChain Agent 全部用例运行完毕。")
    print("💡 提示：安装 langchain 后设置 use_llm=True 可体验真正的 LLM 自主工具调用。")
