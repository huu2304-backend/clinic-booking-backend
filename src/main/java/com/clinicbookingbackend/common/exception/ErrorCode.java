package com.clinicbookingbackend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400 - Validation
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),

    // 401 - Authentication
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc token không hợp lệ"),

    // 403 - Authorization
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này"),

    // 404 - Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),

    // 409 - Conflict (nghiệp vụ CBS)
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    SLOT_UNAVAILABLE(HttpStatus.CONFLICT, "Slot đã được đặt hoặc đang được giữ bởi người khác"),
    CANCELLATION_NOT_ALLOWED(HttpStatus.CONFLICT, "Không thể hủy lịch hẹn trong khung giờ này"),
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