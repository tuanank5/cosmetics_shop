package com.beautyshop.dto;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public class DangKy_Form {
    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email chưa hợp lệ.")
    @Size(max = 190, message = "Email tối đa 190 ký tự.")
    private String email;

    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 160, message = "Họ tên tối đa 160 ký tự.")
    private String hoTen;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 12, max = 72, message = "Mật khẩu phải có từ 12 đến 72 ký tự.")
    private String matKhau;

    @AssertTrue(message = "Mật khẩu quá dài. Hãy nhập mật khẩu ngắn hơn.")
    public boolean isDoDaiBcryptHopLe() {
        return matKhau == null || matKhau.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.strip(); }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }
}
