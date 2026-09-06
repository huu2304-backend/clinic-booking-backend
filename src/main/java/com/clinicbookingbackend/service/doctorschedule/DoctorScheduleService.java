package com.clinicbookingbackend.service.doctorschedule;

import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleCreateRequest;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleResponse;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleUpdateRequest;
import org.springframework.security.core.Authentication;

public interface DoctorScheduleService {

    DoctorScheduleResponse create(DoctorScheduleCreateRequest request, Authentication authentication);

    DoctorScheduleResponse update(Long id, DoctorScheduleUpdateRequest request, Authentication authentication);

    void delete(Long id, Authentication authentication);
}
