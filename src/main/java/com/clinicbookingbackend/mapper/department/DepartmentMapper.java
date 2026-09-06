package com.clinicbookingbackend.mapper.department;

import com.clinicbookingbackend.dto.department.DepartmentRequest;
import com.clinicbookingbackend.dto.department.DepartmentResponse;
import com.clinicbookingbackend.entity.department.Department;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    DepartmentResponse toResponse(Department department);

    Department toEntity(DepartmentRequest request);

    // IGNORE: nếu client không gửi description (null), giữ nguyên giá trị cũ trong DB
    // thay vì ghi đè thành null — description là field optional, PUT không nên xóa
    // dữ liệu mà admin không chủ đích đụng tới.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(DepartmentRequest request, @MappingTarget Department entity);
}
