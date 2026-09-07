package com.clinicbookingbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        // Mặc định: lấy thời gian hệ thống theo múi giờ UTC
        // return Clock.systemUTC();

        // Hoặc lấy theo múi giờ Việt Nam (GMT+7)
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
