"""
LangGraph 挂号 Agent 探索性 Demo

目标：用最少的节点展示 LangGraph 的核心设计思路——图状态机流转。
不是替代现有多 Agent 链路，而是对比理解"什么时候该用图编排、什么时候 4 Worker 流水线就够"。

架构（2 Agent + 2 工具 + 1 确认）：
  triage ──(不是挂号)──> done
    │(registration)
    ▼
  search_departments (工具)
    │
    ▼
  search_doctors (工具)
    │
    ▼
  confirm ──(确认)──> execute

运行方式：
  python -m inference_demo.langgraph_demo
"""

from __future__ import annotations

import operator
import re
from typing import Annotated, Any, Dict, List, Optional, TypedDict

from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, StateGraph


# ── 模拟工具（不调外部 API，返回固定数据） ──────────────────────

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


def tool_search_departments(symptom: str) -> List[str]:
    """工具：根据症状查可挂号科室。"""
    for key, depts in SYMPTOM_DEPT_MAP.items():
        if key in symptom or symptom in key:
            return depts
    return []


def tool_search_doctors(department: str, date: Optional[str] = None) -> List[Dict[str, Any]]:
    """工具：查某科室某天的出诊医生及可挂号时段。"""
    _ = date  # 演示中不按日期过滤
    for dept_key, doctors in MOCK_DOCTORS.items():
        if dept_key in department or department in dept_key:
            return doctors
    # 兜底：不认识科室也返回值班医生
    return [dict(DEFAULT_DOCTOR)]


# ── LangGraph 状态定义 ─────────────────────────────────────────

class AgentState(TypedDict):
    """图节点间流转的共享状态。LangGraph 的 StateGraph 会自动在节点间传递这个 dict。"""
    messages: Annotated[List[str], operator.add]  # 每一步追加的消息
    user_input: str                                # 本轮用户输入
    intent: str                                    # registration / explain / unknown
    symptom: str                                   # 用户提到的症状
    department: str                                # 匹配到的科室
    doctors: List[Dict[str, Any]]                  # 工具查到的医生列表
    selected_doctor: Dict[str, Any]                # 用户选定的医生
    date: str                                      # 日期
    time_preference: str                           # 时段偏好
    confirmed: bool                                # 用户已确认
    reply: str                                     # 最终回复


# ── 图节点（Agent） ────────────────────────────────────────────

def triage_node(state: AgentState) -> AgentState:
    """
    Agent 1 — 分诊（Triage）：识别意图 + 提取初始槽位。

    规则提取（可替换为 LLM 调用）：
      - 意图：命中挂号关键词 → registration，否则 unknown
      - 症状：从症状词典中匹配
      - 日期：提取今天/明天/x月x日
    """
    text = state.get("user_input", "")
    # 症状提取（先于意图判断，因为症状也是挂号意图的信号）
    symptom = ""
    for key in SYMPTOM_DEPT_MAP:
        if key in text:
            symptom = key
            break

    # 日期提取
    date = ""
    date_map = {"今天": "2026-06-04", "明天": "2026-06-05", "后天": "2026-06-06"}
    for chinese, iso in date_map.items():
        if chinese in text:
            date = iso
            break
    if not date:
        m = re.search(r"(\d{1,2})月(\d{1,2})[日号]?", text)
        if m:
            date = f"2026-{int(m.group(1)):02d}-{int(m.group(2)):02d}"
    if not date:
        date = "2026-06-04"  # 默认今天

    # 意图判断
    if any(kw in text for kw in ["挂", "预约", "挂号", "看", "号源", "就诊"]):
        intent = "registration"
    elif any(kw in text for kw in ["为什么", "为啥", "解释", "推荐理由"]):
        intent = "explain"
    elif symptom or any(dept in text for dept in ["科", "门诊", "内科", "外科"]):
        # 有症状词或科室关键词 → 大概率是挂号意图
        intent = "registration"
    else:
        intent = "unknown"

    # 时段偏好
    time_pref = ""
    if "上午" in text:
        time_pref = "上午"
    elif "下午" in text:
        time_pref = "下午"

    return {
        "messages": [f"[Triage] intent={intent}, symptom={symptom}, date={date}, time_pref={time_pref or '无'}"],
        "intent": intent,
        "symptom": symptom,
        "date": date,
        "time_preference": time_pref,
        "confirmed": False,
        "reply": "",
    }


def search_departments_node(state: AgentState) -> AgentState:
    """工具节点：症状 → 科室查询。"""
    symptom = state.get("symptom", "")
    depts = tool_search_departments(symptom)
    department = depts[0] if depts else "全科门诊"

    return {
        "messages": [f"[Tool:search_departments] 症状={symptom} → {depts or ['未匹配，走全科门诊']}"],
        "department": department,
    }


def search_doctors_node(state: AgentState) -> AgentState:
    """工具节点：科室 + 日期 → 医生 + 时段查询。"""
    department = state.get("department", "")
    date = state.get("date", "")
    doctors = tool_search_doctors(department, date)
    top = doctors[0] if doctors else dict(DEFAULT_DOCTOR)

    return {
        "messages": [f"[Tool:search_doctors] {department} {date} → {[d['name'] for d in doctors]}"],
        "doctors": doctors,
        "selected_doctor": top,
    }


def confirm_node(state: AgentState) -> AgentState:
    """确认节点：汇总信息，等待用户确认。"""
    dept = state.get("department", "")
    doc = state.get("selected_doctor", {})
    date = state.get("date", "")
    time_pref = state.get("time_preference", "")

    slots = doc.get("slots", [])
    slot_display = slots[0] if slots else "待定"
    if time_pref == "上午":
        slot_display = next((s for s in slots if s < "12:00"), slots[0])
    elif time_pref == "下午":
        slot_display = next((s for s in slots if s >= "12:00"), slots[-1])

    reply = (
        f"已为您匹配：{dept} {doc.get('name', '')}({doc.get('title', '')}) "
        f" 日期：{date} 时段：{slot_display}。"
        f" 确认挂号请输入「确认」。"
    )
    return {
        "messages": [f"[confirm] {reply}"],
        "reply": reply,
    }


def execute_node(state: AgentState) -> AgentState:
    """执行节点：模拟提交挂号。演示中不做真实写操作。"""
    if not state.get("confirmed"):
        return {
            "messages": ["[execute] 用户未确认，跳过提交。"],
            "reply": "挂号未提交。如需挂号请先确认。",
        }
    doc = state.get("selected_doctor", {})
    dept = state.get("department", "")
    date = state.get("date", "")
    reply = (
        f"挂号成功！{dept} {doc.get('name', '')}({doc.get('title', '')}) "
        f" 日期：{date}。挂号单号：REG{doc.get('name','')[:1]}{date.replace('-','')[4:]}"
    )
    return {
        "messages": [f"[execute] {reply}"],
        "reply": reply,
    }


def fallback_node(state: AgentState) -> AgentState:
    """非挂号意图的兜底节点。"""
    intent = state.get("intent", "unknown")
    if intent == "explain":
        reply = "当前只支持挂号流程演示。解释型问题请参考 RAG 模块。"
    else:
        reply = "您好！请告诉我您的症状或想挂的科室，例如「明天牙疼想挂号」。"
    return {
        "messages": [f"[fallback] {reply}"],
        "reply": reply,
    }


# ── 路由函数 ───────────────────────────────────────────────────

def route_after_triage(state: AgentState) -> str:
    """triage 之后的边路由：挂号走工具链，其他走兜底。"""
    if state.get("intent") == "registration":
        return "search_departments"
    return "fallback"


def route_after_confirm(state: AgentState) -> str:
    """确认后的路由：已确认走执行，否则结束。"""
    return "execute" if state.get("confirmed") else END


# ── 构建 LangGraph 图 ──────────────────────────────────────────

def build_graph() -> StateGraph:
    """组装 LangGraph StateGraph：
       triage → [非挂号] → fallback → END
       triage → [挂号] → search_departments → search_doctors → confirm → execute → END
    """
    builder = StateGraph(AgentState)

    # 添加节点
    builder.add_node("triage", triage_node)
    builder.add_node("search_departments", search_departments_node)
    builder.add_node("search_doctors", search_doctors_node)
    builder.add_node("confirm", confirm_node)
    builder.add_node("execute", execute_node)
    builder.add_node("fallback", fallback_node)

    # 入口
    builder.set_entry_point("triage")

    # 条件边：triage → registration ? 工具链 : fallback
    builder.add_conditional_edges("triage", route_after_triage, {
        "search_departments": "search_departments",
        "fallback": "fallback",
    })

    # 固定边：工具链依次流转
    builder.add_edge("search_departments", "search_doctors")
    builder.add_edge("search_doctors", "confirm")

    # 条件边：confirm → confirmed ? execute : END
    builder.add_conditional_edges("confirm", route_after_confirm, {
        "execute": "execute",
        END: END,
    })

    # 终止节点
    builder.add_edge("execute", END)
    builder.add_edge("fallback", END)

    return builder


# ── Demo 运行 ──────────────────────────────────────────────────

def run(user_input: str, confirmed: bool = False, existing_state: Optional[AgentState] = None) -> AgentState:
    """执行一次图遍历，返回最终状态。"""
    graph_builder = build_graph()
    memory = MemorySaver()
    graph = graph_builder.compile(checkpointer=memory)

    initial: AgentState = {
        "messages": [],
        "user_input": user_input,
        "intent": "",
        "symptom": "",
        "department": "",
        "doctors": [],
        "selected_doctor": {},
        "date": "",
        "time_preference": "",
        "confirmed": confirmed,
        "reply": "",
    }
    # 合并已有状态用于多轮演示
    if existing_state:
        for key in ("messages", "intent", "symptom", "department",
                     "doctors", "selected_doctor", "date", "time_preference"):
            if key in existing_state:
                initial[key] = existing_state[key]

    config = {"configurable": {"thread_id": "demo-session-1"}}
    result = graph.invoke(initial, config)
    return result


def pretty_print(state: AgentState) -> None:
    """打印图遍历日志。"""
    print("\n" + "═" * 60)
    print("  🔄 LangGraph 节点遍历日志")
    print("═" * 60)
    for msg in state.get("messages", []):
        print(f"  {msg}")
    print("─" * 60)
    print(f"  📩 最终回复: {state.get('reply', '')}")
    print("═" * 60 + "\n")


if __name__ == "__main__":
    # ── 用例 1：正常挂号流程 ──
    print("\n\n🏥 用例 1 —— 正常挂号：'明天牙疼想挂口腔科'")
    result1 = run(user_input="明天牙疼想挂口腔科")
    pretty_print(result1)

    # ── 用例 2：非挂号意图 ──
    print("\n\n🏥 用例 2 —— 非挂号意图：'为什么推荐这位医生'")
    result2 = run(user_input="为什么推荐这位医生")
    pretty_print(result2)

    # ── 用例 3：多轮确认（第二轮带 confirmed=True） ──
    print("\n\n🏥 用例 3 —— 多轮确认：第一轮查号源，第二轮确认提交")
    result3a = run(user_input="今天头疼")
    pretty_print(result3a)
    # 第二轮：用户确认
    result3b = run(
        user_input="确认",
        confirmed=True,
        existing_state=result3a,
    )
    pretty_print(result3b)

    # ── 用例 4：带时段偏好的挂号 ──
    print("\n\n🏥 用例 4 —— 带时段偏好：'下午想看骨科'")
    result4 = run(user_input="下午骨折想看骨科")
    pretty_print(result4)

    print("\n✅ 全部用例运行完毕。")
