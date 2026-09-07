package com.clinicbookingbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling: bật job quét slot LOCKED hết TTL (CBS-52, xem scheduler/SlotLockExpirySweepJob).
@SpringBootApplication
@EnableScheduling
public class ClinicBookingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicBookingBackendApplication.class, args);
    }

}
