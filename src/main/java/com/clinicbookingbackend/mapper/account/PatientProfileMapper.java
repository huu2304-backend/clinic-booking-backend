package com.clinicbookingbackend.mapper.account;

import com.clinicbookingbackend.dto.account.PatientProfileResponse;
import com.clinicbookingbackend.dto.account.PatientProfileUpdateRequest;
import com.clinicbookingbackend.entity.account.PatientProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PatientProfileMapper {

    @Mapping(source = "account.email", target = "email")
    PatientProfileResponse toResponse(PatientProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    void updateEntityFromRequest(PatientProfileUpdateRequest request, @MappingTarget PatientProfile entity);
}
