package com.clinicbookingbackend.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {
    @NotBlank(message = "Tên khoa không được để trống")
    @Size(max = 100, message = "Tên khoa không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    // Trim ngay tại setter (Jackson gọi 1 lần lúc deserialize JSON) để mọi nơi
    // dùng getName() sau này (validate, mapper, log) đều nhận giá trị đã sạch.
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
}
