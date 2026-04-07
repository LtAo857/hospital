package com.example.hospital.patient.wx.api.agent.tool;

import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.service.MessageService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class MessageAgentTools {
    @Resource
    private MessageService messageService;

    public long getUnreadCount(int userId) {
        return messageService.searchUnreadCount(userId);
    }

    public PageUtils listMessages(int userId, int page, int length) {
        Map<String, Object> param = new HashMap<>();
        param.put("userId", userId);
        param.put("page", page);
        param.put("length", length);
        param.put("start", (page - 1) * length);
        return messageService.searchMessageByPage(param);
    }
}
