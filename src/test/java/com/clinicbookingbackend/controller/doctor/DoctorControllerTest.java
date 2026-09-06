package com.clinicbookingbackend.controller.doctor;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.doctor.DoctorCreateRequest;
import com.clinicbookingbackend.dto.doctor.DoctorResponse;
import com.clinicbookingbackend.dto.doctor.DoctorUpdateRequest;
import com.clinicbookingbackend.entity.account.enums.Status;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.doctor.DoctorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@AutoConfigureMockMvc(addFilters = false)
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DoctorService doctorService;

    // JwtAuthenticationFilter vẫn bị @WebMvcTest tự quét vào context dù addFilters=false
    // (xem giải thích ở DepartmentControllerTest) — mock JwtUtil để thỏa constructor.
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void create_shouldReturn201_whenRequestValid() throws Exception {
        DoctorCreateRequest request = new DoctorCreateRequest();
        request.setEmail("bacsi001@cbs.local");
        request.setPassword("Doctor@123");
        request.setFullName("Nguyễn Văn An");
        request.setDepartmentId(1L);

        DoctorResponse response = new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát");
        when(doctorService.create(any(DoctorCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.email").value("bacsi001@cbs.local"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void create_shouldReturn400_whenPasswordTooShort() throws Exception {
        DoctorCreateRequest request = new DoctorCreateRequest();
        request.setEmail("bacsi001@cbs.local");
        request.setPassword("123");
        request.setFullName("Nguyễn Văn An");
        request.setDepartmentId(1L);

        mockMvc.perform(post("/api/admin/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        DoctorCreateRequest request = new DoctorCreateRequest();
        request.setEmail("bacsi001@cbs.local");
        request.setPassword("Doctor@123");
        request.setFullName("Nguyễn Văn An");
        request.setDepartmentId(1L);

        when(doctorService.create(any(DoctorCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS));

        mockMvc.perform(post("/api/admin/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.EMAIL_ALREADY_EXISTS.name()));
    }

    @Test
    void getAll_shouldReturn200WithPagedContent() throws Exception {
        DoctorResponse response = new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát");
        Pageable pageable = PageRequest.of(0, 10);
        when(doctorService.getAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        mockMvc.perform(get("/api/admin/doctors?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getByDepartment_shouldReturn200_whenDepartmentExists() throws Exception {
        DoctorResponse response = new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát");
        Pageable pageable = PageRequest.of(0, 10);
        when(doctorService.getByDepartment(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response), pageable, 1));

        mockMvc.perform(get("/api/admin/doctors/department/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].departmentId").value(1));
    }

    @Test
    void getByDepartment_shouldReturn404_whenDepartmentMissing() throws Exception {
        when(doctorService.getByDepartment(eq(99L), any(Pageable.class)))
                .thenThrow(new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));

        mockMvc.perform(get("/api/admin/doctors/department/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_shouldReturn200_whenFound() throws Exception {
        when(doctorService.getById(100L)).thenReturn(
                new DoctorResponse(100L, "Nguyễn Văn An", "bacsi001@cbs.local", 1L, Status.ACTIVE, "Nội tổng quát"));

        mockMvc.perform(get("/api/admin/doctors/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"));
    }

    @Test
    void getById_shouldReturn404_whenMissing() throws Exception {
        when(doctorService.getById(999L)).thenThrow(new BusinessException(ErrorCode.DOCTOR_NOT_FOUND));

        mockMvc.perform(get("/api/admin/doctors/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.DOCTOR_NOT_FOUND.name()));
    }

    @Test
    void update_shouldReturn200_whenRequestValid() throws Exception {
        DoctorUpdateRequest request = new DoctorUpdateRequest();
        request.setFullName("Nguyễn Văn B");
        request.setDepartmentId(2L);

        when(doctorService.update(eq(100L), any(DoctorUpdateRequest.class))).thenReturn(
                new DoctorResponse(100L, "Nguyễn Văn B", "bacsi001@cbs.local", 2L, Status.ACTIVE, "Ngoại tổng quát"));

        mockMvc.perform(put("/api/admin/doctors/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn B"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/admin/doctors/100"))
                .andExpect(status().isNoContent());
    }
}
