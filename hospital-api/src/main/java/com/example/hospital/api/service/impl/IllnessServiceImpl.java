package com.example.hospital.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.db.dao.IllnessDao;
import com.example.hospital.api.db.dao.IllnessDao;
import com.example.hospital.api.db.pojo.DoctorEntity;
import com.example.hospital.api.db.pojo.IllnessEntity;
import com.example.hospital.api.service.IllnessService;
import com.example.hospital.api.service.IllnessService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class IllnessServiceImpl implements IllnessService {
    @Resource
    private IllnessDao illnessDao;



    @Override
    public PageUtils searchIllnessInfo(Map param) {
        ArrayList list =illnessDao.searchIllnessInfo(param);

        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, list.size(), page, length);

        return pageUtils;
    }

    @Override
    public void insert(Map param) {
        final IllnessEntity IllnessEntity = BeanUtil.toBean(param, IllnessEntity.class);
        illnessDao.insert(IllnessEntity);
    }

    @Override
    public void update(Map param) {
        illnessDao.update(param);
    }

    @Override
    public HashMap searchById(int id) {
        return illnessDao.searchById(id);
    }

    @Override
    public void deleteByIds(Integer[] ids) {
        illnessDao.deleteByIds(ids);
    }


}
