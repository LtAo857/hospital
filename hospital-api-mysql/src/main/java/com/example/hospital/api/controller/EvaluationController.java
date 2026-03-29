package com.example.hospital.api.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.common.R;
import com.example.hospital.api.controller.form.SearchEvaluationByPageForm;
import com.example.hospital.api.service.EvaluationService;
import com.example.hospital.api.service.MisUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/evaluation")
public class EvaluationController {
    @Resource
    private EvaluationService evaluationService;

    @Resource
    private MisUserService misUserService;

    @PostMapping("/searchByPage")
    @SaCheckLogin
    public R searchByPage(@RequestBody @Valid SearchEvaluationByPageForm form) {
        Map param = BeanUtil.beanToMap(form);
        int userId = StpUtil.getLoginIdAsInt();
        Map userInfo = misUserService.searchUserInfoById(userId);
        Object refId = userInfo.get("refId");
        List<String> permissions = StpUtil.getPermissionList();
        boolean isRoot = permissions.contains("ROOT");
        boolean canManageAll = isRoot || (permissions.contains("EVALUATION:SELECT") && refId == null);
        if (refId != null && !isRoot) {
            param.put("doctorId", refId);
        } else if (!canManageAll) {
            return R.error(403, "No permission");
        }
        int start = (form.getPage() - 1) * form.getLength();
        param.put("start", start);
        PageUtils pageUtils = evaluationService.searchByPage(param);
        return R.ok().put("result", pageUtils);
    }
}
