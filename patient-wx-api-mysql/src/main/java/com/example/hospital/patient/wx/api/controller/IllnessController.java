package com.example.hospital.patient.wx.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.common.R;
import com.example.hospital.patient.wx.api.controller.form.SearchDoctorByPageForm;
import com.example.hospital.patient.wx.api.controller.form.SearchDoctorInfoByIdForm;
import com.example.hospital.patient.wx.api.service.DoctorService;
import com.example.hospital.patient.wx.api.service.IllnessService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/illness")
public class IllnessController {
    @Resource
    private IllnessService illnessService;



    @PostMapping("/searchIllnessInfoList")
    public R searchDoctorInfo(@RequestBody @Valid SearchDoctorByPageForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        param.put("start", start);
        PageUtils pageUtils = illnessService.searchIllnessInfo(param);
        return R.ok().put("result", pageUtils);
    }
}
