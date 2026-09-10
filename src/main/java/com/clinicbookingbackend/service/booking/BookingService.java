package com.clinicbookingbackend.service.booking;

import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.dto.booking.ConfirmAppointmentRequest;
import com.clinicbookingbackend.dto.booking.DoctorAppointmentResponse;
import com.clinicbookingbackend.dto.booking.HoldResponse;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    // CBS-72: AVAILABLE (hoặc LOCKED đã hết hạn) -> LOCKED, TTL cấu hình được.
    HoldResponse hold(Long doctorScheduleId, Authentication authentication);

    // CBS-72 (optional): Patient chủ động hủy giữ chỗ, trả AVAILABLE ngay. No-op nếu không
    // phải đúng người đang giữ chỗ hoặc slot không còn LOCKED.
    void releaseHold(Long doctorScheduleId, Authentication authentication);

    // CBS-42: LOCKED (bởi đúng Patient đang gọi, chưa hết hạn) -> BOOKED + tạo Appointment CONFIRMED.
    AppointmentResponse confirm(ConfirmAppointmentRequest request, Authentication authentication);

    // CBS-70: Patient xem toàn bộ lịch hẹn (mọi status) của chính mình — ownership qua patientId
    // từ JWT, không nhận patientId từ request.
    List<AppointmentResponse> getMyAppointments(Authentication authentication);

    // CBS-53: Doctor xem Appointment CONFIRMED trong ngày của chính mình — ownership qua
    // DoctorProfile gắn với accountId từ JWT (SecurityUtils), không nhận doctorId từ request.
    List<DoctorAppointmentResponse> getMyAppointments(LocalDate date, Authentication authentication);

    // CBS-51: Patient hủy Appointment CONFIRMED của chính mình (ownership qua JWT, không nhận
    // patientId từ request body), chỉ khi còn cách giờ khám tối thiểu N giờ (BR-APT-04, N cấu
    // hình qua application.properties, không hardcode). Appointment.status=CANCELLED và
    // DoctorSchedule.status=AVAILABLE cập nhật cùng 1 @Transactional (BR-APT-05).
    AppointmentResponse cancel(Long appointmentId, Authentication authentication);
}
