package com.example.hospital.patient.wx.api.agent.multi.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.service.MultiAgentCoordinatorService;
import com.example.hospital.patient.wx.api.common.R;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/multi")
public class MultiAgentChatController {
    private final MultiAgentProperties properties;
    private final MultiAgentCoordinatorService coordinatorService;

    public MultiAgentChatController(MultiAgentProperties properties,
                                    MultiAgentCoordinatorService coordinatorService) {
        this.properties = properties;
        this.coordinatorService = coordinatorService;
    }

    @PostMapping("/chat")
    public R chat(@RequestBody(required = false) AgentChatRequest request) {
        if (!properties.isEnabled()) {
            throw new HospitalException("Multi-agent is disabled");
        }
        Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        AgentChatResponse response = coordinatorService.chat(request, userId);
        return R.ok().put("result", response);
    }
}
