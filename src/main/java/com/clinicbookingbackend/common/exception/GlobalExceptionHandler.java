package com.clinicbookingbackend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception [{}] tại {}: {}", errorCode.name(), request.getRequestURI(), e.getMessage());

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getHttpStatus().value())
                .errorCode(errorCode.name())
                .message(e.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {

        List<FieldViolation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldViolation(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .message(ErrorCode.VALIDATION_FAILED.getDefaultMessage())
                .path(request.getRequestURI())
                .errors(violations)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        // Body JSON sai kiểu/format (vd LocalDate/LocalTime/enum không parse được) -> lỗi của client,
        // phải là 400 chứ không rơi xuống catch-all 500 ở dưới.
        log.warn("Body JSON không hợp lệ tại {}: {}", request.getRequestURI(), e.getMessage());

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .message("Dữ liệu JSON không hợp lệ hoặc sai định dạng")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        // Thiếu query param bắt buộc (vd ?date=) -> lỗi của client, phải là 400 chứ không
        // rơi xuống catch-all 500 ở dưới (mirror handleHttpMessageNotReadable).
        log.warn("Thiếu tham số bắt buộc tại {}: {}", request.getRequestURI(), e.getMessage());

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .message("Thiếu tham số bắt buộc: " + e.getParameterName())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        // Query param sai định dạng (vd date=abc không parse được thành LocalDate) -> 400.
        log.warn("Tham số sai định dạng tại {}: {}", request.getRequestURI(), e.getMessage());

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .message("Tham số '" + e.getName() + "' sai định dạng")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        // Lưới an toàn cho race condition: 2 request cùng pass check tồn tại rồi cùng insert/update,
        // constraint UNIQUE ở DB chặn request thứ 2 -> convert thành lỗi nghiệp vụ 409 thay vì 500.
        log.warn("Data integrity violation tại {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());

        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(ErrorCode.DATA_CONFLICT.getHttpStatus().value())
                .errorCode(ErrorCode.DATA_CONFLICT.name())
                .message(ErrorCode.DATA_CONFLICT.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(ErrorCode.DATA_CONFLICT.getHttpStatus()).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled Exception at {}: ", request.getRequestURI(), e);
        ApiError apiError = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.name())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}