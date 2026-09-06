package com.clinicbookingbackend.controller.department;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.department.DepartmentRequest;
import com.clinicbookingbackend.dto.department.DepartmentResponse;
import com.clinicbookingbackend.security.JwtUtil;
import com.clinicbookingbackend.service.department.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// addFilters = false: tắt SecurityFilterChain (JWT filter) để test tập trung vào Controller
// logic (validation, mapping request/response, mã trạng thái HTTP) — phân quyền /api/admin/**
// đã được verify riêng thủ công qua Swagger, không phải mục tiêu của test này.
@WebMvcTest(DepartmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DepartmentService departmentService;

    // JwtAuthenticationFilter là @Component implements Filter nên vẫn bị @WebMvcTest tự
    // quét vào context dù addFilters=false (addFilters chỉ tắt lúc chạy request, không tắt
    // lúc khởi tạo bean) — mock JwtUtil để thỏa constructor, không cần stub hành vi gì.
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void create_shouldReturn201_whenRequestValid() throws Exception {
        DepartmentRequest request = new DepartmentRequest();
        request.setName("Nội tổng quát");
        request.setDescription("Khám nội tổng quát");
        DepartmentResponse response = new DepartmentResponse(1L, "Nội tổng quát", "Khám nội tổng quát");

        when(departmentService.create(any(DepartmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nội tổng quát"));
    }

    @Test
    void create_shouldReturn400_whenNameBlank() throws Exception {
        DepartmentRequest request = new DepartmentRequest();
        request.setName("");
        request.setDescription("Khám nội tổng quát");

        mockMvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_FAILED.name()));
    }

    @Test
    void create_shouldReturn409_whenNameAlreadyExists() throws Exception {
        DepartmentRequest request = new DepartmentRequest();
        request.setName("Nội tổng quát");

        when(departmentService.create(any(DepartmentRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.DEPARTMENT_NAME_ALREADY_EXISTS));

        mockMvc.perform(post("/api/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.DEPARTMENT_NAME_ALREADY_EXISTS.name()));
    }

    @Test
    void getAll_shouldReturn200WithList() throws Exception {
        when(departmentService.getAll()).thenReturn(List.of(
                new DepartmentResponse(1L, "Nội tổng quát", null),
                new DepartmentResponse(2L, "Ngoại tổng quát", null)
        ));

        mockMvc.perform(get("/api/admin/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Nội tổng quát"));
    }

    @Test
    void getById_shouldReturn200_whenFound() throws Exception {
        when(departmentService.getById(1L)).thenReturn(new DepartmentResponse(1L, "Nội tổng quát", null));

        mockMvc.perform(get("/api/admin/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_shouldReturn404_whenMissing() throws Exception {
        when(departmentService.getById(99L))
                .thenThrow(new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));

        mockMvc.perform(get("/api/admin/departments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.DEPARTMENT_NOT_FOUND.name()));
    }

    @Test
    void update_shouldReturn200_whenRequestValid() throws Exception {
        DepartmentRequest request = new DepartmentRequest();
        request.setName("Nội tổng quát 2");

        when(departmentService.update(eq(1L), any(DepartmentRequest.class)))
                .thenReturn(new DepartmentResponse(1L, "Nội tổng quát 2", null));

        mockMvc.perform(put("/api/admin/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nội tổng quát 2"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/admin/departments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn409_whenHasActiveDoctors() throws Exception {
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.DEPARTMENT_HAS_DOCTORS))
                .when(departmentService).delete(1L);

        mockMvc.perform(delete("/api/admin/departments/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.DEPARTMENT_HAS_DOCTORS.name()));
    }
}
