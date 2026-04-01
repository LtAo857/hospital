package com.example.hospital.patient.wx.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.common.R;
import com.example.hospital.patient.wx.api.controller.form.DoctorFavoriteForm;
import com.example.hospital.patient.wx.api.controller.form.SearchDoctorByPageForm;
import com.example.hospital.patient.wx.api.controller.form.SearchDoctorInfoByIdForm;
import com.example.hospital.patient.wx.api.service.DoctorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/doctor")
public class DoctorController {
    @Resource
    private DoctorService doctorService;

    @PostMapping("/searchDoctorInfoById")
    public R searchDoctorInfoById(@RequestBody @Valid SearchDoctorInfoByIdForm form) {
        Integer userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
        HashMap map = doctorService.searchDoctorInfoById(form.getId(), userId);
        return R.ok(map);
    }

    @PostMapping("/favorite/insert")
    @SaCheckLogin
    public R insertFavorite(@RequestBody @Valid DoctorFavoriteForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        boolean result = doctorService.favoriteDoctor(userId, form.getDoctorId());
        return R.ok().put("result", result);
    }

    @PostMapping("/favorite/delete")
    @SaCheckLogin
    public R deleteFavorite(@RequestBody @Valid DoctorFavoriteForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        boolean result = doctorService.unfavoriteDoctor(userId, form.getDoctorId());
        return R.ok().put("result", result);
    }

    @PostMapping("/searchDoctorInfo")
    public R searchDoctorInfo(@RequestBody @Valid SearchDoctorByPageForm form) {
        Map param = BeanUtil.beanToMap(form);
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        param.put("start", start);
        PageUtils pageUtils = doctorService.searchDoctorInfo(param);
        return R.ok().put("result", pageUtils);
    }

    @PostMapping("/favorite/searchByPage")
    @SaCheckLogin
    public R searchFavoriteDoctorByPage(@RequestBody @Valid SearchDoctorByPageForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        Map param = BeanUtil.beanToMap(form);
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        param.put("start", start);
        PageUtils pageUtils = doctorService.searchFavoriteDoctorByPage(userId, param);
        return R.ok().put("result", pageUtils);
    }
}
