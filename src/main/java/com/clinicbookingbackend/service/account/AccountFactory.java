package com.clinicbookingbackend.service.account;

import com.clinicbookingbackend.entity.account.Account;
import com.clinicbookingbackend.entity.account.enums.Role;
import com.clinicbookingbackend.entity.account.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Dùng chung giữa AuthService (đăng ký Patient) và DoctorServiceImpl (Admin tạo Doctor) —
// tránh lặp lại chuỗi setEmail/setPasswordHash/setRole/setStatus ở 2 nơi rồi lệch nhau
// khi 1 trong 2 chỗ đổi mà quên sửa chỗ còn lại.
@Component
@RequiredArgsConstructor
public class AccountFactory {

    private final PasswordEncoder passwordEncoder;

    public Account create(String email, String rawPassword, Role role) {
        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setStatus(Status.ACTIVE);
        return account;
    }
}
