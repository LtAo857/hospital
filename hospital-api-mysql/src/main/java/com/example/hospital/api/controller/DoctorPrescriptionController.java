package com.example.hospital.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.common.R;
import com.example.hospital.api.controller.form.SaveDoctorPrescriptionForm;
import com.example.hospital.api.controller.form.SearchDoctorPrescriptionByPageForm;
import com.example.hospital.api.controller.form.SearchDoctorPrescriptionByRegistrationIdForm;
import com.example.hospital.api.service.DoctorPrescriptionService;
import com.example.hospital.api.service.MisUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/doctor_prescription")
public class DoctorPrescriptionController {
    @Resource
    private DoctorPrescriptionService doctorPrescriptionService;

    @Resource
    private MisUserService misUserService;

    @PostMapping("/searchRegistrationByPage")
    @SaCheckLogin
    public R searchRegistrationByPage(@RequestBody @Valid SearchDoctorPrescriptionByPageForm form) {
        Map param = BeanUtil.beanToMap(form);
        int userId = StpUtil.getLoginIdAsInt();
        Map userInfo = misUserService.searchUserInfoById(userId);
        Object refId = userInfo.get("refId");
        List<String> permissions = StpUtil.getPermissionList();
        boolean isRoot = permissions.contains("ROOT");
        boolean canManageAll = isRoot || permissions.contains("PRESCRIPTION:SELECT");
        if (refId != null && !isRoot) {
            param.put("doctorId", refId);
        }
        else if (!canManageAll) {
            return R.error(403, "No permission");
        }
        int start = (form.getPage() - 1) * form.getLength();
        param.put("start", start);
        PageUtils pageUtils = doctorPrescriptionService.searchRegistrationByPage(param);
        return R.ok().put("result", pageUtils);
    }

    @PostMapping("/searchByRegistrationId")
    @SaCheckLogin
    public R searchByRegistrationId(@RequestBody @Valid SearchDoctorPrescriptionByRegistrationIdForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        List<String> permissions = StpUtil.getPermissionList();
        HashMap result = doctorPrescriptionService.searchByRegistrationId(form.getRegistrationId(), userId, permissions);
        return R.ok().put("result", result);
    }

    @PostMapping("/save")
    @SaCheckLogin
    public R save(@RequestBody @Valid SaveDoctorPrescriptionForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        List<String> permissions = StpUtil.getPermissionList();
        Map param = BeanUtil.beanToMap(form);
        doctorPrescriptionService.save(param, userId, permissions);
        return R.ok();
    }
}
