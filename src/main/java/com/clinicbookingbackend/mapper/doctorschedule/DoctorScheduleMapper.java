package com.clinicbookingbackend.mapper.doctorschedule;

import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleResponse;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleUpdateRequest;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DoctorScheduleMapper {

    @Mapping(source = "doctorProfile.id", target = "doctorId")
    @Mapping(source = "doctorProfile.fullName", target = "doctorFullName")
    DoctorScheduleResponse toResponse(DoctorSchedule doctorSchedule);

    // "status"/"doctorProfile" phải giữ nguyên giá trị hiện tại của entity, không cho request ghi đè
    // (không có trong CBS-38 AC) — Service tự set khi cần đổi trạng thái.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "doctorProfile", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromRequest(DoctorScheduleUpdateRequest request, @MappingTarget DoctorSchedule entity);
}
