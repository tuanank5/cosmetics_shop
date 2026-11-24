package com.beautyshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class HoSo_Form {
    public static final String MAU_SDT = "(?:\\+?[0-9]{7,15})?";

    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 160, message = "Họ tên tối đa 160 ký tự.")
    private String hoTen;

    @Pattern(regexp = MAU_SDT, message = "Số điện thoại cần 7–15 chữ số, có thể bắt đầu bằng +.")
    private String soDienThoai;

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen == null ? null : hoTen.strip(); }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai == null ? null : soDienThoai.strip(); }
}
