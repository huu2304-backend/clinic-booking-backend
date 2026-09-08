package com.clinicbookingbackend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400 - Validation
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "Giờ bắt đầu phải trước giờ kết thúc"),
    PAST_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Không thể xem lịch của ngày đã qua"),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "Tài khoản đang bị khoá hoặc không hoạt động"),
    HOLD_REQUIRED(HttpStatus.BAD_REQUEST, "Phải giữ chỗ (hold) slot trước khi xác nhận đặt lịch"),
    // 401 - Authentication
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc token không hợp lệ"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),

    // 403 - Authorization
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này"),

    // 404 - Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Khoa không tồn tại"),
    DOCTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "Bác sĩ không tồn tại"),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lịch làm việc"),
    APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lịch hẹn"),

    // 409 - Conflict (nghiệp vụ CBS)
    DEPARTMENT_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tên khoa đã tồn tại"),
    DEPARTMENT_HAS_DOCTORS(HttpStatus.CONFLICT, "Không thể xóa khoa vì vẫn còn bác sĩ trực thuộc"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc vi phạm ràng buộc, vui lòng thử lại"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    SLOT_UNAVAILABLE(HttpStatus.CONFLICT, "Slot đã được đặt hoặc đang được giữ bởi người khác"),
    SCHEDULE_SLOT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Slot lịch làm việc đã tồn tại cho bác sĩ này"),
    SCHEDULE_MODIFICATION_NOT_ALLOWED(HttpStatus.CONFLICT, "Không thể sửa/xóa slot đã được đặt lịch (BOOKED)"),
    CANCELLATION_NOT_ALLOWED(HttpStatus.CONFLICT, "Không thể hủy lịch hẹn trong khung giờ này"),
    PATIENT_SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "Bạn đã có lịch hẹn khác trùng khung giờ này, vui lòng chọn giờ khác"),
    AI_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Hệ thống AI hiện không khả dụng"),

    // 500 - Unexpected
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi xảy ra, vui lòng thử lại sau");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}