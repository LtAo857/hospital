package com.example.hospital.api.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.db.dao.DoctorAccountDao;
import com.example.hospital.api.db.dao.DoctorDao;
import com.example.hospital.api.db.dao.MedicalDeptSubAndDoctorDao;
import com.example.hospital.api.db.pojo.DoctorAccountEntity;
import com.example.hospital.api.exception.HospitalException;
import com.example.hospital.api.service.DoctorAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DoctorAccountServiceImpl implements DoctorAccountService {
    private static final int DOCTOR_ROLE_ID = 1;
    private static final int VIDEO_DOCTOR_ROLE_ID = 2;

    @Resource
    private DoctorDao doctorDao;

    @Resource
    private DoctorAccountDao doctorAccountDao;

    @Value("${storage.local.root-path:D:/hospital-storage}")
    private String storageRootPath;

    @Resource
    private MedicalDeptSubAndDoctorDao medicalDeptSubAndDoctorDao;

    @Override
    public PageUtils searchByPage(Map param) {
        ArrayList<HashMap> list = null;
        long count = doctorDao.searchCount(param);
        if (count > 0) {
            list = doctorDao.searchByPage(param);
        } else {
            list = new ArrayList<>();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        return new PageUtils(list, count, page, length);
    }

    @Override
    public HashMap searchContent(int id) {
        HashMap map = doctorDao.searchContent(id);
        JSONArray tag = JSONUtil.parseArray(MapUtil.getStr(map, "tag"));
        map.replace("tag", tag);
        return map;
    }

    @Override
    @Transactional
    public void updatePhoto(MultipartFile file, Integer doctorId) {
        try {
            String filename = "doctor-" + doctorId + ".jpg";
            String relativePath = "doctor/" + filename;
            Path target = Paths.get(storageRootPath, "doctor", filename);
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            doctorDao.updatePhoto(new HashMap<String, Object>() {{
                put("id", doctorId);
                put("photo", relativePath);
            }});
        } catch (Exception e) {
            log.error("保存医生照片失败", e);
            throw new HospitalException("保存医生照片失败");
        }
    }

    @Override
    @Transactional
    public void insert(Map param) {
        Integer refId = MapUtil.getInt(param, "ref_id");
        if (refId == null) {
            refId = MapUtil.getInt(param, "refId");
        }
        if (refId == null) {
            throw new HospitalException("医生ID不能为空");
        }

        HashMap account = doctorAccountDao.searchAccountByRefId(refId);
        if (account != null && !account.isEmpty()) {
            throw new HospitalException("该医生已经设置登录账号");
        }

        String username = MapUtil.getStr(param, "username");
        String password = MapUtil.getStr(param, "password");
        if (StrUtil.hasBlank(username, password)) {
            throw new HospitalException("账号或密码不能为空");
        }
        ensureUsernameAvailable(username, null);

        DoctorAccountEntity entity = new DoctorAccountEntity();
        entity.setName(MapUtil.getStr(param, "name"));
        entity.setUsername(username);
        entity.setPassword(encodePassword(username, password));
        entity.setDeptId(MapUtil.getInt(param, "dept_id"));
        entity.setRefId(refId);
        entity.setSex(MapUtil.getStr(param, "sex"));
        entity.setTel(MapUtil.getStr(param, "tel"));
        entity.setEmail(MapUtil.getStr(param, "email"));
        entity.setJob(MapUtil.getStr(param, "job", "医生"));
        entity.setStatus(MapUtil.get(param, "status", Byte.class));
        doctorAccountDao.insert(entity);

        doctorAccountDao.insertUserRole(new HashMap<String, Object>() {{
            put("userId", entity.getId());
            put("roleId", DOCTOR_ROLE_ID);
        }});
        doctorAccountDao.insertUserRole(new HashMap<String, Object>() {{
            put("userId", entity.getId());
            put("roleId", VIDEO_DOCTOR_ROLE_ID);
        }});
    }

    @Override
    public HashMap searchAccountByRefId(int refId) {
        return doctorAccountDao.searchAccountByRefId(refId);
    }

    @Override
    @Transactional
    public void updateAccount(Map param) {
        Integer id = MapUtil.getInt(param, "id");
        if (id == null) {
            throw new HospitalException("账号ID不能为空");
        }

        HashMap account = doctorAccountDao.searchAccountById(id);
        if (account == null || account.isEmpty()) {
            throw new HospitalException("账号不存在");
        }

        String username = MapUtil.getStr(param, "username");
        if (StrUtil.isBlank(username)) {
            throw new HospitalException("账号不能为空");
        }
        ensureUsernameAvailable(username, id);

        String password = MapUtil.getStr(param, "password");
        HashMap<String, Object> updateParam = new HashMap<>();
        updateParam.put("id", id);
        updateParam.put("username", username);
        if (StrUtil.isNotBlank(password)) {
            updateParam.put("password", encodePassword(username, password));
        }
        doctorAccountDao.updateAccount(updateParam);
    }

    @Override
    public HashMap searchById(int id) {
        HashMap map = doctorDao.searchById(id);
        String tag = MapUtil.getStr(map, "tag");
        JSONArray array = JSONUtil.parseArray(tag);
        map.replace("tag", array);
        return map;
    }

    @Override
    @Transactional
    public void update(Map param) {
        doctorDao.update(param);
        param = MapUtil.renameKey(param, "id", "doctorId");
        medicalDeptSubAndDoctorDao.updateDoctorSubDept(param);
    }

    @Override
    @Transactional
    public void deleteByIds(Integer[] ids) {
        doctorDao.deleteByIds(ids);
    }

    @Override
    public ArrayList<HashMap> searchByDeptSubId(int deptSubId) {
        return doctorDao.searchByDeptSubId(deptSubId);
    }

    private void ensureUsernameAvailable(String username, Integer id) {
        Integer conflictId = doctorAccountDao.searchAccountIdByUsername(new HashMap<String, Object>() {{
            put("username", username);
            put("id", id);
        }});
        if (conflictId != null) {
            throw new HospitalException("账号名称已存在");
        }
    }

    private String encodePassword(String username, String password) {
        MD5 md5 = MD5.create();
        String temp = md5.digestHex(username);
        String tempStart = StrUtil.subWithLength(temp, 0, 6);
        String tempEnd = StrUtil.subSuf(temp, temp.length() - 3);
        return md5.digestHex(tempStart + password + tempEnd);
    }
}
