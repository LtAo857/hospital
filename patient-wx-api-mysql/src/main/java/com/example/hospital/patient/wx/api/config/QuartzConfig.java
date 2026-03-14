package com.example.hospital.patient.wx.api.config;

import com.example.hospital.patient.wx.api.job.RegistrationReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail registrationReminderJobDetail() {
        return JobBuilder.newJob(RegistrationReminderJob.class)
                .withIdentity("registrationReminderJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger registrationReminderTrigger() {
        // 每天早上8:00执行
        CronScheduleBuilder schedule = CronScheduleBuilder.cronSchedule("0 0 8 * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(registrationReminderJobDetail())
                .withIdentity("registrationReminderTrigger")
                .withSchedule(schedule)
                .build();
    }
}
