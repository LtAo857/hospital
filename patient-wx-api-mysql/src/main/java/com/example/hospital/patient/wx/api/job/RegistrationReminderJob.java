package com.example.hospital.patient.wx.api.job;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.patient.wx.api.db.dao.MedicalRegistrationDao;
import com.example.hospital.patient.wx.api.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
public class RegistrationReminderJob extends QuartzJobBean {
    @Resource
    private MedicalRegistrationDao medicalRegistrationDao;

    @Resource
    private MessageService messageService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("执行就诊提醒定时任务");
        ArrayList<HashMap> list = medicalRegistrationDao.searchTodayRegistrationUsers();
        for (HashMap map : list) {
            int userId = MapUtil.getInt(map, "userId");
            String doctorName = MapUtil.getStr(map, "doctorName");
            String subDeptName = MapUtil.getStr(map, "subDeptName");
            int registrationId = MapUtil.getInt(map, "registrationId");
            String content = "您今天在" + subDeptName + "有预约（" + doctorName + "医生），请按时就诊";
            messageService.sendMessage(userId, (byte) 2, "就诊提醒", content, registrationId);
        }
        log.info("就诊提醒发送完成，共{}条", list.size());
    }
}
