package com.clinicbookingbackend.mapper.doctor;

import com.clinicbookingbackend.dto.doctor.DoctorResponse;
import com.clinicbookingbackend.dto.doctor.DoctorSummaryResponse;
import com.clinicbookingbackend.dto.doctor.DoctorUpdateRequest;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DoctorMapper {

    @Mapping(source = "account.email", target = "email")
    @Mapping(source = "account.status", target = "status")
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    DoctorResponse toResponse(DoctorProfile doctorProfile);

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    DoctorSummaryResponse toSummaryResponse(DoctorProfile doctorProfile);

    // Chỉ copy field structural (fullName) — "department" phải set tay ở Service
    // vì cần query DB từ departmentId (Mapper không được phép gọi Repository).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "department", ignore = true)
    void updateEntityFromRequest(DoctorUpdateRequest request, @MappingTarget DoctorProfile entity);
}
