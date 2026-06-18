package com.example.hospital.api.service.impl;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.db.dao.MedicalDeptDao;
import com.example.hospital.api.db.pojo.MedicalDeptEntity;
import com.example.hospital.api.exception.HospitalException;
import com.example.hospital.api.service.MedicalDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MedicalDeptServiceImpl implements MedicalDeptService {
    private static final String DEPARTMENT_CACHE_KEY = "multi_agent:nlu:departments";

    @Resource
    private MedicalDeptDao medicalDeptDao;

    @Autowired(required = false)
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public ArrayList<HashMap> searchAll() {
        ArrayList<HashMap> list = medicalDeptDao.searchAll();
        return list;
    }

    @Override
    public HashMap searchDeptAndSub() {
        ArrayList<HashMap> list = medicalDeptDao.searchDeptAndSub();
        LinkedHashMap map = new LinkedHashMap();
        for (HashMap one : list) {
            Integer deptId = MapUtil.getInt(one, "deptId");
            Integer subId = MapUtil.getInt(one, "subId");
            String deptName = MapUtil.getStr(one, "deptName");
            String subName = MapUtil.getStr(one, "subName");
            if (map.containsKey(deptName)) {
                ArrayList<HashMap> subList = (ArrayList<HashMap>) map.get(deptName);
                subList.add(new HashMap() {{
                    put("subId", subId);
                    put("subName", subName);
                }});
            } else {
                map.put(deptName, new ArrayList() {{
                    add(new HashMap() {{
                        put("subId", subId);
                        put("subName", subName);
                    }});
                }});
            }
        }
        return map;
    }

    @Override
    public PageUtils searchByPage(Map param) {
        ArrayList<HashMap> list = null;
        long count = medicalDeptDao.searchCount(param);
        if (count > 0) {
            list = medicalDeptDao.searchByPage(param);
        } else {
            list = new ArrayList<>();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, count, page, length);
        return pageUtils;
    }

    @Override
    @Transactional
    public void insert(MedicalDeptEntity entity) {
        medicalDeptDao.insert(entity);
        invalidateDepartmentCache();
    }

    @Override
    public HashMap searchById(int id) {
        HashMap map = medicalDeptDao.searchById(id);
        return map;
    }

    @Override
    @Transactional
    public void update(MedicalDeptEntity entity) {
        medicalDeptDao.update(entity);
        invalidateDepartmentCache();
    }

    @Override
    @Transactional
    public void deleteByIds(Integer[] ids) {
        long count = medicalDeptDao.searchSubCount(ids);
        if (count == 0) {
            medicalDeptDao.deleteByIds(ids);
            invalidateDepartmentCache();
        } else {
            throw new HospitalException("科室存在关联诊室，无法删除记录");
        }
    }

    private void invalidateDepartmentCache() {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(DEPARTMENT_CACHE_KEY);
        } catch (Exception ignored) {
        }
    }
}
