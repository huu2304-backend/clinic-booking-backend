package com.clinicbookingbackend.service.doctorschedule;

import com.clinicbookingbackend.dto.doctorschedule.AvailableSlotResponse;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleCreateRequest;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleResponse;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleUpdateRequest;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

public interface DoctorScheduleService {

    DoctorScheduleResponse create(DoctorScheduleCreateRequest request, Authentication authentication);

    DoctorScheduleResponse update(Long id, DoctorScheduleUpdateRequest request, Authentication authentication);

    void delete(Long id, Authentication authentication);

    // Dùng cho Patient xem slot trống của 1 bác sĩ trong ngày — chỉ trả slot AVAILABLE,
    // không trả slot của ngày/giờ đã qua.
    List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date);
}
