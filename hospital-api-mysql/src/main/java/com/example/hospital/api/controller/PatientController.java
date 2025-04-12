package com.example.hospital.api.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.common.R;
import com.example.hospital.api.controller.form.SearchIllnessByPageForm;
import com.example.hospital.api.service.PatientService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
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
}
