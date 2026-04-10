package com.example.hospital.patient.wx.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.patient.wx.api.common.R;
import com.example.hospital.patient.wx.api.controller.form.SearchPrescriptionByRegistrationIdForm;
import com.example.hospital.patient.wx.api.service.PrescriptionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/prescription")
public class PrescriptionController {
    @Resource
    private PrescriptionService prescriptionService;

    @PostMapping("/searchPrescriptionByRegistrationId")
    @SaCheckLogin
    public R searchPrescriptionByRegistrationId(@RequestBody @Valid SearchPrescriptionByRegistrationIdForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        HashMap result = prescriptionService.searchPrescriptionByRegistrationId(param);
        return R.ok().put("result", result);
    }
}
