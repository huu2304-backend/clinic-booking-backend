package com.clinicbookingbackend.service.department;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.department.DepartmentRequest;
import com.clinicbookingbackend.dto.department.DepartmentResponse;
import com.clinicbookingbackend.mapper.department.DepartmentMapper;
import com.clinicbookingbackend.entity.department.Department;
import com.clinicbookingbackend.repository.department.DepartmentRepository;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final DoctorProfileRepository doctorProfileRepository;

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.getName();
        log.info("Bắt đầu tạo khoa mới: {}", name);

        // Bước 1: guard clause — chặn sớm nếu vi phạm rule nghiệp vụ (trùng tên)
        if (departmentRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NAME_ALREADY_EXISTS,
                    "Tên khoa '" + name + "' đã tồn tại");
        }

        // Bước 2: DTO -> Entity qua Mapper
        Department department = departmentMapper.toEntity(request);

        // Bước 3: persist (nếu race condition lọt qua guard clause ở trên, DataIntegrityViolationException
        // ném ra do constraint UNIQUE ở DB sẽ được GlobalExceptionHandler bắt và convert sang 409)
        Department saved = departmentRepository.save(department);
        log.info("Tạo khoa thành công, id={}, name={}", saved.getId(), saved.getName());

        // Bước 4+5: Entity -> Response, trả về
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        String name = request.getName();
        log.info("Bắt đầu sửa thông tin khoa id={}: {}", id, name);

        // Bước 1a: fetch entity theo id — không thấy thì báo 404 ngay
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND,
                        "Không tìm thấy khoa với id=" + id));

        // Bước 1b: guard clause — trùng tên với khoa KHÁC (loại trừ chính nó)
        if (departmentRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NAME_ALREADY_EXISTS,
                    "Tên khoa '" + name + "' đã tồn tại");
        }

        // Bước 2: để MapStruct copy field từ request vào entity đã fetch, thay vì set tay từng field
        departmentMapper.updateEntityFromRequest(request, department);

        // Bước 3: persist
        Department saved = departmentRepository.save(department);
        log.info("Sửa khoa thành công, id={}, name={}", saved.getId(), saved.getName());

        // Bước 4+5: Entity -> Response
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND,
                        "Không tìm thấy khoa với id=" + id));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND,
                        "Không tìm thấy khoa với id=" + id));

        // Guard clause: chặn xóa nếu còn bất kỳ doctor nào (kể cả đã soft-delete/INACTIVE)
        // thuộc khoa này — doctor_profile.department_id là FK NOT NULL không có ON DELETE,
        // nên DB sẽ chặn xóa bất kể status; guard phải khớp đúng điều đó để trả lỗi rõ ràng
        // thay vì để lọt xuống DataIntegrityViolationException không thể tự khắc phục.
        if (doctorProfileRepository.existsByDepartmentId(id)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HAS_DOCTORS,
                    "Khoa id=" + id + " vẫn còn bác sĩ trực thuộc");
        }

        departmentRepository.delete(department);
        log.info("Xóa khoa thành công, id={}", id);
    }
}
