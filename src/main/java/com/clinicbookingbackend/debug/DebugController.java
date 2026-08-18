package com.clinicbookingbackend.debug;

import com.clinicbookingbackend.common.exception.BusinessException;
import com.clinicbookingbackend.common.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/business-exception")
    public void triggerBusinessException() {
        throw new BusinessException(ErrorCode.SLOT_UNAVAILABLE);
    }
    @GetMapping("/business-exception-custom")
    public void triggerBusinessExceptionCustom() {
        throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email test@gmail.com đã được đăng ký");
    }

    @PostMapping("/validate")
    public String triggerValidationException(@Valid @RequestBody TestRequest request) {
        return "Không bao giờ tới được đây nếu validate fail: " + request.getEmail();
    }

    @GetMapping("/unexpected-exception")
    public void triggerUnexpectedException() {
        String s = null;
        s.length(); // cố tình gây NullPointerException
    }

    @Getter
    @Setter
    public static class TestRequest {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        private String email;

        @Min(value = 1, message = "Tuổi phải lớn hơn 0")
        private int age;
    }
}