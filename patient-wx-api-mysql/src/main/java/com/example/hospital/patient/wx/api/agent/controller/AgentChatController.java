package com.example.hospital.patient.wx.api.agent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.service.AgentOrchestratorService;
import com.example.hospital.patient.wx.api.common.R;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/agent")
public class AgentChatController {
    @Resource
    private AgentProperties agentProperties;

    @Resource
    private AgentOrchestratorService agentOrchestratorService;

    @PostMapping("/chat")
    public R chat(@RequestBody(required = false) AgentChatRequest request) {
        if (!agentProperties.isEnabled()) {
            throw new HospitalException("Agent 功能未启用");
        }
        Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        AgentChatResponse response = agentOrchestratorService.chat(request, userId);
        return R.ok().put("result", response);
    }
}
