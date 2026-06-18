package com.example.hospital.patient.wx.api.agent.multi.service;

import com.example.hospital.patient.wx.api.service.MedicalDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MultiAgentDepartmentCatalogService {
    private static final String CACHE_KEY = "multi_agent:nlu:departments";
    private static final long CACHE_TTL_MINUTES = 30L;

    @Resource
    private MedicalDeptService medicalDeptService;

    @Autowired(required = false)
    private RedisTemplate<Object, Object> redisTemplate;

    public List<String> getDepartmentNames() {
        List<String> cached = loadCache();
        if (cached != null) {
            return cached;
        }
        List<String> departments = queryDepartmentNames();
        saveCache(departments);
        return departments;
    }

    public void invalidate() {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(CACHE_KEY);
        } catch (Exception ignored) {
        }
    }

    private List<String> loadCache() {
        if (redisTemplate == null) {
            return null;
        }
        try {
            Object value = redisTemplate.opsForValue().get(CACHE_KEY);
            return normalize(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> queryDepartmentNames() {
        ArrayList<HashMap> list = medicalDeptService.searchMedicalDeptList(new HashMap<String, Object>());
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (HashMap one : list) {
            String name = stringValue(one.get("name"));
            if (StringUtils.hasText(name)) {
                names.add(name.trim());
            }
        }
        return new ArrayList<String>(names);
    }

    private void saveCache(List<String> departments) {
        if (redisTemplate == null || departments == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, new ArrayList<String>(departments), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
    }

    private List<String> normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection) {
            ArrayList<String> departments = new ArrayList<String>();
            for (Object item : (Collection<?>) value) {
                String name = stringValue(item);
                if (StringUtils.hasText(name)) {
                    departments.add(name.trim());
                }
            }
            return departments;
        }
        String name = stringValue(value);
        if (!StringUtils.hasText(name)) {
            return Collections.emptyList();
        }
        ArrayList<String> departments = new ArrayList<String>();
        departments.add(name.trim());
        return departments;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
