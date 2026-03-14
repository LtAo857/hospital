package com.example.hospital.patient.wx.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.common.R;
import com.example.hospital.patient.wx.api.controller.form.ReadMessageForm;
import com.example.hospital.patient.wx.api.controller.form.SearchMessageByPageForm;
import com.example.hospital.patient.wx.api.service.MessageService;
import org.springframework.web.bind.annotation.*;


import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {
    @Resource
    private MessageService messageService;

    @PostMapping("/searchMessageByPage")
    @SaCheckLogin
    public R searchMessageByPage(@RequestBody @Valid SearchMessageByPageForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        param.put("start", start);
        PageUtils pageUtils = messageService.searchMessageByPage(param);
        return R.ok().put("result", pageUtils);
    }

    @GetMapping("/searchUnreadCount")
    @SaCheckLogin
    public R searchUnreadCount() {
        int userId = StpUtil.getLoginIdAsInt();
        long count = messageService.searchUnreadCount(userId);
        return R.ok().put("result", count);
    }

    @PostMapping("/readMessage")
    @SaCheckLogin
    public R readMessage(@RequestBody @Valid ReadMessageForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        Map param = BeanUtil.beanToMap(form);
        param.put("userId", userId);
        messageService.readMessage(param);
        return R.ok();
    }

    @GetMapping("/readAllMessage")
    @SaCheckLogin
    public R readAllMessage() {
        int userId = StpUtil.getLoginIdAsInt();
        messageService.readAllMessage(userId);
        return R.ok();
    }
}
