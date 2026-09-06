package com.clinicbookingbackend.mapper.department;

import com.clinicbookingbackend.dto.department.DepartmentRequest;
import com.clinicbookingbackend.entity.department.Department;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Dùng trực tiếp class do MapStruct sinh ra (không mock) để verify đúng hành vi
// null-value-mapping-strategy thật, thứ mà DepartmentServiceImplTest (mock DepartmentMapper)
// không thể verify được.
class DepartmentMapperTest {

    private final DepartmentMapper mapper = new DepartmentMapperImpl();

    @Test
    void updateEntityFromRequest_shouldKeepExistingDescription_whenRequestDescriptionIsNull() {
        Department entity = new Department();
        entity.setId(1L);
        entity.setName("Nội tổng quát cũ");
        entity.setDescription("Mô tả cũ");

        DepartmentRequest request = new DepartmentRequest();
        request.setName("Nội tổng quát mới");
        // description không set -> null, không được ghi đè description cũ trong entity

        mapper.updateEntityFromRequest(request, entity);

        assertThat(entity.getName()).isEqualTo("Nội tổng quát mới");
        assertThat(entity.getDescription()).isEqualTo("Mô tả cũ");
        assertThat(entity.getId()).isEqualTo(1L);
    }

    @Test
    void updateEntityFromRequest_shouldOverwriteDescription_whenRequestProvidesOne() {
        Department entity = new Department();
        entity.setId(1L);
        entity.setName("Nội tổng quát cũ");
        entity.setDescription("Mô tả cũ");

        DepartmentRequest request = new DepartmentRequest();
        request.setName("Nội tổng quát mới");
        request.setDescription("Mô tả mới");

        mapper.updateEntityFromRequest(request, entity);

        assertThat(entity.getDescription()).isEqualTo("Mô tả mới");
    }
}
