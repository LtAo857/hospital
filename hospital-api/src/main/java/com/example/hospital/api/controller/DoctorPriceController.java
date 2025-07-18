package com.example.hospital.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.common.R;
import com.example.hospital.api.controller.form.*;
import com.example.hospital.api.service.DoctorPriceService;
import com.example.hospital.api.service.IllnessService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/price")
public class DoctorPriceController {
    @Resource
    private DoctorPriceService doctorPriceService;


    @PostMapping("/searchByPage")
    public R searchByPage(@RequestBody @Valid SearchPriceByPageForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        param.put("start", start);
        PageUtils pageUtils = doctorPriceService.searchByPage(param);
        return R.ok().put("result", pageUtils);
    }


    @PostMapping("/searchDoctorPriceById")
    public R searchDoctorInfo(@RequestBody @Valid SearchDoctorPriceByIdForm form) {
        HashMap map = doctorPriceService.searchByDoctorId(form.getDoctorId());
        return R.ok(map);
    }

    @PostMapping("/insert")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "DOCTOR:INSERT"}, mode = SaMode.OR)
    public R insert(@RequestBody @Valid InsertPriceForm form) {
        Map param = BeanUtil.beanToMap(form);
        doctorPriceService.insert(param);
        return R.ok();
    }



    @PostMapping("/update")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "DOCTOR:UPDATE"}, mode = SaMode.OR)
    public R update(@RequestBody @Valid UpdatePriceForm form) {
        Map param = BeanUtil.beanToMap(form);
        doctorPriceService.update(param);
        return R.ok();
    }


//    @PostMapping("/deleteByIds")
//    @SaCheckLogin
//    @SaCheckPermission(value = {"ROOT", "DOCTOR:DELETE"}, mode = SaMode.OR)
//    public R deleteByIds(@RequestBody @Valid DeleteIllnessByIdsForm form) {
//        illnessService.deleteByIds(form.getIds());
//        return R.ok();
//    }
}
