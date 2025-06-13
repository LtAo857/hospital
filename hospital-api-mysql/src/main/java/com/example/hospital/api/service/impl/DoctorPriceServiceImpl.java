package com.example.hospital.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.db.dao.DoctorPriceDao;
import com.example.hospital.api.db.pojo.DoctorPriceEntity;
import com.example.hospital.api.db.pojo.IllnessEntity;
import com.example.hospital.api.service.DoctorPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class DoctorPriceServiceImpl implements DoctorPriceService {

    @Resource
    private DoctorPriceDao doctorPriceDao;


    @Override
    public HashMap searchByDoctorId(int doctorId) {
        return doctorPriceDao.searchByDoctorId(doctorId);
    }

    @Override
    public void update(Map param) {

        doctorPriceDao.update(param);
    }

    @Override
    public void insert(Map param) {
        final DoctorPriceEntity doctorPriceEntity = BeanUtil.toBean(param, DoctorPriceEntity.class);
        doctorPriceDao.insert(doctorPriceEntity);
    }

    @Override
    public PageUtils searchByPage(Map param) {
        ArrayList list =doctorPriceDao.searchByPage(param);

        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, list.size(), page, length);

        return pageUtils;
    }
}
