package com.clinicbookingbackend.service.doctorschedule;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleCreateRequest;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleResponse;
import com.clinicbookingbackend.dto.doctorschedule.DoctorScheduleUpdateRequest;
import com.clinicbookingbackend.entity.doctor.DoctorProfile;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.mapper.doctorschedule.DoctorScheduleMapper;
import com.clinicbookingbackend.repository.doctor.DoctorProfileRepository;
import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import com.clinicbookingbackend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorScheduleMapper doctorScheduleMapper;

    @Override
    @Transactional
    public DoctorScheduleResponse create(DoctorScheduleCreateRequest request, Authentication authentication) {
        log.info("Bắt đầu tạo slot lịch làm việc cho doctorId={}", request.getDoctorId());

        DoctorProfile doctorProfile = doctorProfileRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCTOR_NOT_FOUND,
                        "Không tìm thấy bác sĩ với id=" + request.getDoctorId()));

        requireOwnerOrAdmin(doctorProfile, authentication);
        requireValidTimeRange(request.getStartTime(), request.getEndTime());

        // Pre-check trùng slot — UNIQUE thật vẫn nằm ở DB (uq_doctor_schedule_slot),
        // check này chỉ để trả lỗi nghiệp vụ rõ nghĩa cho case thường gặp.
        if (doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTime(
                doctorProfile.getId(), request.getWorkDate(), request.getStartTime())) {
            throw new BusinessException(ErrorCode.SCHEDULE_SLOT_ALREADY_EXISTS,
                    "Bác sĩ đã có slot vào " + request.getWorkDate() + " lúc " + request.getStartTime());
        }

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctorProfile(doctorProfile);
        schedule.setWorkDate(request.getWorkDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setStatus(ScheduleStatus.AVAILABLE);

        DoctorSchedule saved = doctorScheduleRepository.save(schedule);
        log.info("Tạo slot lịch làm việc thành công, id={}, doctorProfileId={}", saved.getId(), doctorProfile.getId());

        return doctorScheduleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DoctorScheduleResponse update(Long id, DoctorScheduleUpdateRequest request, Authentication authentication) {
        log.info("Bắt đầu sửa slot lịch làm việc id={}", id);

        DoctorSchedule schedule = doctorScheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND,
                        "Không tìm thấy lịch làm việc với id=" + id));

        requireOwnerOrAdmin(schedule.getDoctorProfile(), authentication);
        requireNotBooked(schedule, "sửa");
        requireValidTimeRange(request.getStartTime(), request.getEndTime());

        if (doctorScheduleRepository.existsByDoctorProfileIdAndWorkDateAndStartTimeAndIdNot(
                schedule.getDoctorProfile().getId(), request.getWorkDate(), request.getStartTime(), id)) {
            throw new BusinessException(ErrorCode.SCHEDULE_SLOT_ALREADY_EXISTS,
                    "Bác sĩ đã có slot vào " + request.getWorkDate() + " lúc " + request.getStartTime());
        }

        doctorScheduleMapper.updateEntityFromRequest(request, schedule);

        DoctorSchedule saved = doctorScheduleRepository.save(schedule);
        log.info("Sửa slot lịch làm việc thành công, id={}", saved.getId());

        return doctorScheduleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, Authentication authentication) {
        log.info("Bắt đầu xóa slot lịch làm việc id={}", id);

        DoctorSchedule schedule = doctorScheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND,
                        "Không tìm thấy lịch làm việc với id=" + id));

        requireOwnerOrAdmin(schedule.getDoctorProfile(), authentication);
        requireNotBooked(schedule, "xóa");

        // Xóa cứng (khác với soft-delete của Doctor ở CBS-37) — đúng theo AC CBS-38,
        // slot không mang lịch sử cần giữ lại như Account, chỉ cấm khi đang BOOKED.
        doctorScheduleRepository.delete(schedule);
        log.info("Xóa slot lịch làm việc thành công, id={}", id);
    }

    // Doctor chỉ được đụng slot của chính mình; Admin được đụng slot của bất kỳ doctor nào.
    private void requireOwnerOrAdmin(DoctorProfile doctorProfile, Authentication authentication) {
        if (SecurityUtils.hasRole(authentication, "ADMIN")) {
            return;
        }
        Long currentAccountId = SecurityUtils.getCurrentAccountId(authentication);
        if (!doctorProfile.getAccount().getId().equals(currentAccountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn chỉ được thao tác trên lịch làm việc của chính mình");
        }
    }

    private void requireNotBooked(DoctorSchedule schedule, String action) {
        if (schedule.getStatus() == ScheduleStatus.BOOKED) {
            throw new BusinessException(ErrorCode.SCHEDULE_MODIFICATION_NOT_ALLOWED,
                    "Không thể " + action + " slot đang ở trạng thái BOOKED");
        }
    }

    private void requireValidTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }
    }
}
