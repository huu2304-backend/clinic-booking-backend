package com.clinicbookingbackend.scheduler;

import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

// CBS-52: slot LOCKED là trạng thái tạm thời (TTL) — job này tự quét và trả về AVAILABLE khi
// Patient giữ chỗ nhưng không xác nhận (confirm) kịp trong TTL (BR-SCH-05).
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotLockExpirySweepJob {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${booking.lock-sweep-interval-ms}")
    @Transactional
    public void expireStaleLocks() {
        int expiredCount = doctorScheduleRepository.expireStaleLocks(LocalDateTime.now(clock));
        if (expiredCount > 0) {
            log.info("Đã tự động trả {} slot LOCKED hết hạn về AVAILABLE", expiredCount);
        }
    }
}
