package com.example.hospital.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.common.R;
import com.example.hospital.api.controller.form.InsertPatientCardInfoForm;
import com.example.hospital.api.controller.form.SearchIllnessByPageForm;
import com.example.hospital.api.controller.form.UpdatePatientCardInfoForm;
import com.example.hospital.api.db.pojo.PatientInfoCardEntity;
import com.example.hospital.api.service.PatientService;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/patient")
public class PatientController {
    @Resource
    private PatientService patientService;



    @GetMapping("/getAllPatient")
    public R getAllPatient() {
        return R.ok().put("result", patientService.searchAllPatient());
    }


    @PostMapping("/insert")
    @SaCheckLogin
    public R insert(@RequestBody @Valid InsertPatientCardInfoForm form) {
        PatientInfoCardEntity entity = BeanUtil.toBean(form, PatientInfoCardEntity.class);
        int userId = StpUtil.getLoginIdAsInt();
        entity.setUserId(userId);
        entity.setUuid(IdUtil.simpleUUID());
        String json = JSONUtil.parseArray(form.getMedicalHistory()).toString();
        entity.setMedicalHistory(json);
        patientService.insert(entity);
        return R.ok();
    }

    @GetMapping("/searchUserInfoCard/{userId}")
    @SaCheckLogin
    public R searchUserInfoCard(@PathVariable Integer userId) {
        HashMap map = patientService.searchUserInfoCard(userId);
        if (MapUtil.isEmpty(map)) {
            return R.ok("没有查询到数据");
        }
        return R.ok(map);
    }

    @PostMapping("/update")
    @SaCheckLogin
    public R update(@RequestBody @Valid UpdatePatientCardInfoForm form) {
        PatientInfoCardEntity entity = BeanUtil.toBean(form, PatientInfoCardEntity.class);
        String json = JSONUtil.parseArray(form.getMedicalHistory()).toString();
        entity.setMedicalHistory(json);
        patientService.update(entity);
        return R.ok();
    }
    @GetMapping("/hasUserInfoCard")
    @SaCheckLogin
    public R hasUserInfoCard() {
        int userId = StpUtil.getLoginIdAsInt();
        boolean bool = patientService.hasUserInfoCard(userId);
        return R.ok().put("result", bool);
    }
}
