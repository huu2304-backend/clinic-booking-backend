package com.clinicbookingbackend.service.booking;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.dto.booking.ConfirmAppointmentRequest;
import com.clinicbookingbackend.dto.booking.HoldResponse;
import com.clinicbookingbackend.entity.appointment.Appointment;
import com.clinicbookingbackend.entity.appointment.enums.AppointmentStatus;
import com.clinicbookingbackend.entity.doctorschedule.DoctorSchedule;
import com.clinicbookingbackend.entity.doctorschedule.enums.ScheduleStatus;
import com.clinicbookingbackend.mapper.booking.AppointmentMapper;
import com.clinicbookingbackend.repository.appointment.AppointmentRepository;
import com.clinicbookingbackend.repository.doctorschedule.DoctorScheduleRepository;
import com.clinicbookingbackend.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final Clock clock;
    private final long slotLockTtlMinutes;
    private final long cancellationMinHours;

    // @Value trên tham số constructor (không phải field) — theo đúng convention của JwtUtil,
    // giúp test dựng service bằng `new BookingServiceImpl(...)` với TTL cố định, không cần
    // Spring context hay reflection để set giá trị.
    public BookingServiceImpl(
            DoctorScheduleRepository doctorScheduleRepository,
            AppointmentRepository appointmentRepository,
            AppointmentMapper appointmentMapper,
            Clock clock,
            @Value("${booking.slot-lock-ttl-minutes}") long slotLockTtlMinutes,
            @Value("${booking.cancellation-min-hours}") long cancellationMinHours) {
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
        this.clock = clock;
        this.slotLockTtlMinutes = slotLockTtlMinutes;
        this.cancellationMinHours = cancellationMinHours;
    }

    @Override
    @Transactional
    public HoldResponse hold(Long doctorScheduleId, Authentication authentication) {
        Long patientAccountId = SecurityUtils.getCurrentAccountId(authentication);
        log.info("Patient accountId={} bắt đầu giữ chỗ slot id={}", patientAccountId, doctorScheduleId);

        DoctorSchedule schedule = doctorScheduleRepository.findById(doctorScheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND,
                        "Không tìm thấy lịch làm việc với id=" + doctorScheduleId));

        // BOOKED/CANCELLED -> từ chối ngay ở tầng validate, không chạm DB write, không cần đọc
        // đồng hồ (đúng AC CBS-40).
        if (schedule.getStatus() == ScheduleStatus.BOOKED || schedule.getStatus() == ScheduleStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.SLOT_UNAVAILABLE,
                    "Slot id=" + doctorScheduleId + " không còn ở trạng thái có thể giữ chỗ");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // Còn LOCKED bởi người khác và CHƯA hết hạn -> từ chối. Nếu đã hết hạn hoặc đang tự giữ
        // (refresh) thì coi như được phép hold — DB constraint version vẫn là chốt chặn cuối.
        if (schedule.getStatus() == ScheduleStatus.LOCKED
                && !isLockExpired(schedule, now)
                && !patientAccountId.equals(schedule.getLockedByAccountId())) {
            throw new BusinessException(ErrorCode.SLOT_UNAVAILABLE,
                    "Slot id=" + doctorScheduleId + " đang được giữ chỗ bởi người khác");
        }

        LocalDateTime expiresAt = now.plusMinutes(slotLockTtlMinutes);
        schedule.setStatus(ScheduleStatus.LOCKED);
        schedule.setLockedByAccountId(patientAccountId);
        schedule.setLockExpiresAt(expiresAt);

        // save() flush khi @Transactional commit với UPDATE...WHERE version=?; nếu 1 request khác
        // đã thắng trước đó, Hibernate ném ObjectOptimisticLockingFailureException, được
        // GlobalExceptionHandler convert thành 409 SLOT_UNAVAILABLE (BR-APT-02) — đây là cơ chế
        // chống 2 Patient cùng giữ được 1 slot khi request gửi lên gần như đồng thời.
        DoctorSchedule saved = doctorScheduleRepository.save(schedule);
        log.info("Giữ chỗ thành công slot id={}, patientAccountId={}, hết hạn lúc {}",
                doctorScheduleId, patientAccountId, expiresAt);

        return new HoldResponse(saved.getId(), expiresAt);
    }

    @Override
    @Transactional
    public void releaseHold(Long doctorScheduleId, Authentication authentication) {
        Long patientAccountId = SecurityUtils.getCurrentAccountId(authentication);

        DoctorSchedule schedule = doctorScheduleRepository.findById(doctorScheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND,
                        "Không tìm thấy lịch làm việc với id=" + doctorScheduleId));

        if (schedule.getStatus() != ScheduleStatus.LOCKED || !patientAccountId.equals(schedule.getLockedByAccountId())) {
            // Không phải đúng người đang giữ (hoặc đã hết hạn/không còn LOCKED) -> coi như release
            // rồi, không phải lỗi của Patient (thao tác idempotent).
            return;
        }

        schedule.setStatus(ScheduleStatus.AVAILABLE);
        schedule.setLockedByAccountId(null);
        schedule.setLockExpiresAt(null);
        doctorScheduleRepository.save(schedule);
        log.info("Patient accountId={} đã hủy giữ chỗ slot id={}", patientAccountId, doctorScheduleId);
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(ConfirmAppointmentRequest request, Authentication authentication) {
        Long patientAccountId = SecurityUtils.getCurrentAccountId(authentication);
        Long doctorScheduleId = request.getDoctorScheduleId();
        log.info("Patient accountId={} xác nhận đặt lịch slot id={}", patientAccountId, doctorScheduleId);

        DoctorSchedule schedule = doctorScheduleRepository.findById(doctorScheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND,
                        "Không tìm thấy lịch làm việc với id=" + doctorScheduleId));

        // Không còn nhận trực tiếp từ AVAILABLE — phải hold trước (CBS-72).
        if (schedule.getStatus() != ScheduleStatus.LOCKED) {
            throw new BusinessException(ErrorCode.HOLD_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (isLockExpired(schedule, now) || !patientAccountId.equals(schedule.getLockedByAccountId())) {
            throw new BusinessException(ErrorCode.SLOT_UNAVAILABLE,
                    "Bạn không giữ chỗ slot này hoặc đã hết hạn giữ chỗ");
        }

        // 1 Patient không được có 2 lịch hẹn CONFIRMED giao nhau về thời gian, kể cả với 2 bác sĩ
        // khác nhau — check nghiệp vụ riêng, UNIQUE(doctor_schedule_id) ở DB chỉ chặn trùng đúng
        // 1 slot chứ không chặn trùng giờ giữa 2 slot khác nhau.
        if (appointmentRepository.existsOverlappingConfirmedAppointment(
                patientAccountId, schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime())) {
            throw new BusinessException(ErrorCode.PATIENT_SCHEDULE_CONFLICT);
        }

        schedule.setStatus(ScheduleStatus.BOOKED);
        schedule.setLockedByAccountId(null);
        schedule.setLockExpiresAt(null);
        doctorScheduleRepository.save(schedule);

        Appointment appointment = new Appointment();
        appointment.setDoctorSchedule(schedule);
        appointment.setPatientAccountId(patientAccountId);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        // UNIQUE(doctor_schedule_id) ở DB (V8) là lưới an toàn cuối cho BR-APT-01 — nếu vi phạm,
        // DataIntegrityViolationException đã được GlobalExceptionHandler convert sẵn thành 409.
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Đặt lịch thành công appointmentId={}, slot id={}, patientAccountId={}",
                saved.getId(), doctorScheduleId, patientAccountId);

        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long appointmentId, Authentication authentication) {
        Long patientAccountId = SecurityUtils.getCurrentAccountId(authentication);
        log.info("Patient accountId={} hủy lịch hẹn id={}", patientAccountId, appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND,
                        "Không tìm thấy lịch hẹn với id=" + appointmentId));

        // Ownership qua patientId lấy từ JWT (SecurityUtils), không nhận từ request body.
        if (!patientAccountId.equals(appointment.getPatientAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền hủy lịch hẹn này");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.CANCELLATION_NOT_ALLOWED,
                    "Lịch hẹn không ở trạng thái có thể hủy");
        }

        DoctorSchedule schedule = appointment.getDoctorSchedule();
        LocalDateTime appointmentStart = schedule.getWorkDate().atTime(schedule.getStartTime());
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cancellationDeadline = appointmentStart.minusHours(cancellationMinHours);

        // BR-APT-04: chỉ hủy được nếu còn cách giờ khám tối thiểu N giờ (N cấu hình qua
        // application.properties, không hardcode) -> quá hạn ném 409 CANCELLATION_NOT_ALLOWED (EX-APT-01).
        if (now.isAfter(cancellationDeadline)) {
            throw new BusinessException(ErrorCode.CANCELLATION_NOT_ALLOWED,
                    "Chỉ có thể hủy lịch hẹn trước giờ khám tối thiểu " + cancellationMinHours + " giờ");
        }

        // BR-APT-05: Appointment.status=CANCELLED + DoctorSchedule.status=AVAILABLE cập nhật cùng
        // 1 @Transactional — DB lỗi giữa chừng thì rollback toàn bộ (EX-APT-04), không để 1 bảng
        // cập nhật còn bảng kia không.
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(now);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        schedule.setStatus(ScheduleStatus.AVAILABLE);
        schedule.setLockedByAccountId(null);
        schedule.setLockExpiresAt(null);
        doctorScheduleRepository.save(schedule);

        log.info("Hủy lịch hẹn thành công appointmentId={}, slot id={} đã trả về AVAILABLE",
                appointmentId, schedule.getId());

        return appointmentMapper.toResponse(savedAppointment);
    }

    private boolean isLockExpired(DoctorSchedule schedule, LocalDateTime now) {
        return schedule.getLockExpiresAt() == null || !schedule.getLockExpiresAt().isAfter(now);
    }
}
