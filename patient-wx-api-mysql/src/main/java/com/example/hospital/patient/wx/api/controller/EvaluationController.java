package com.example.hospital.patient.wx.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.common.R;
import com.example.hospital.patient.wx.api.controller.form.HasEvaluatedForm;
import com.example.hospital.patient.wx.api.controller.form.InsertEvaluationForm;
import com.example.hospital.patient.wx.api.controller.form.SearchDoctorEvaluationForm;
import com.example.hospital.patient.wx.api.controller.form.SearchEvaluationByPageForm;
import com.example.hospital.patient.wx.api.service.EvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/evaluation")
public class EvaluationController {
    @Resource
    private EvaluationService evaluationService;

    @PostMapping("/insert")
    @SaCheckLogin
    public R insert(@RequestBody @Valid InsertEvaluationForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        evaluationService.insert(param);
        return R.ok();
    }

    @PostMapping("/searchByPage")
    @SaCheckLogin
    public R searchByPage(@RequestBody @Valid SearchEvaluationByPageForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        param.put("start", start);
        PageUtils pageUtils = evaluationService.searchEvaluationByPage(param);
        return R.ok().put("result", pageUtils);
    }

    @PostMapping("/searchDoctorEvaluation")
    public R searchDoctorEvaluation(@RequestBody @Valid SearchDoctorEvaluationForm form) {
        Map param = BeanUtil.beanToMap(form);
        HashMap result = evaluationService.searchDoctorEvaluation(param);
        return R.ok().put("result", result);
    }

    @PostMapping("/hasEvaluated")
    @SaCheckLogin
    public R hasEvaluated(@RequestBody @Valid HasEvaluatedForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        boolean result = evaluationService.hasEvaluated(param);
        return R.ok().put("result", result);
    }
}
