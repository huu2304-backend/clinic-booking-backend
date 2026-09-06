package com.clinicbookingbackend.dto.doctor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCreateRequest {

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 8, message = "Password phải có ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    private String fullName;

    @NotNull(message = "Khoa không được để trống")
    private Long departmentId;

    // Trim + lowercase email tại setter, đồng bộ với AuthService.registerPatient (email dùng để lookup trùng)
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.trim();
    }
}
