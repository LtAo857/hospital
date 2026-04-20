package com.example.hospital.patient.wx.api.agent.multi.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.service.MultiAgentCoordinatorService;
import com.example.hospital.patient.wx.api.agent.multi.service.MultiAgentRequestGuardService;
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
    private final MultiAgentRequestGuardService requestGuardService;

    public MultiAgentChatController(MultiAgentProperties properties,
                                    MultiAgentCoordinatorService coordinatorService,
                                    MultiAgentRequestGuardService requestGuardService) {
        this.properties = properties;
        this.coordinatorService = coordinatorService;
        this.requestGuardService = requestGuardService;
    }

    @PostMapping("/chat")
    public R chat(@RequestBody(required = false) AgentChatRequest request) {
        if (!properties.isEnabled()) {
            throw new HospitalException("多 Agent 功能未启用");
        }
        Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        if (requestGuardService != null) {
            requestGuardService.guard(request, userId);
        }
        AgentChatResponse response = coordinatorService.chat(request, userId);
        return R.ok().put("result", response);
    }
}
