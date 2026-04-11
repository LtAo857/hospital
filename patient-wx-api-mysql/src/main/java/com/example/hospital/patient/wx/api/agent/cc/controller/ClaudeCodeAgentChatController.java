package com.example.hospital.patient.wx.api.agent.cc.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.hospital.patient.wx.api.agent.cc.config.ClaudeCodeAgentProperties;
import com.example.hospital.patient.wx.api.agent.cc.service.ClaudeCodeAgentExecutorService;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.common.R;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/agent/cc")
public class ClaudeCodeAgentChatController {
    @Resource
    private ClaudeCodeAgentProperties properties;

    @Resource
    private ClaudeCodeAgentExecutorService executorService;

    @PostMapping("/chat")
    public R chat(@RequestBody(required = false) AgentChatRequest request) {
        if (!properties.isEnabled()) {
            throw new HospitalException("Claude Code agent is disabled");
        }
        Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        AgentChatResponse response = executorService.chat(request, userId);
        return R.ok().put("result", response);
    }
}
