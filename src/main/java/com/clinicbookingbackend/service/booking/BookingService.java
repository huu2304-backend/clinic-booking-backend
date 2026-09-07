package com.clinicbookingbackend.service.booking;

import com.clinicbookingbackend.dto.booking.AppointmentResponse;
import com.clinicbookingbackend.dto.booking.ConfirmAppointmentRequest;
import com.clinicbookingbackend.dto.booking.HoldResponse;
import org.springframework.security.core.Authentication;

public interface BookingService {

    // CBS-72: AVAILABLE (hoặc LOCKED đã hết hạn) -> LOCKED, TTL cấu hình được.
    HoldResponse hold(Long doctorScheduleId, Authentication authentication);

    // CBS-72 (optional): Patient chủ động hủy giữ chỗ, trả AVAILABLE ngay. No-op nếu không
    // phải đúng người đang giữ chỗ hoặc slot không còn LOCKED.
    void releaseHold(Long doctorScheduleId, Authentication authentication);

    // CBS-42: LOCKED (bởi đúng Patient đang gọi, chưa hết hạn) -> BOOKED + tạo Appointment CONFIRMED.
    AppointmentResponse confirm(ConfirmAppointmentRequest request, Authentication authentication);
}
