package com.beautyshop.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class Checkout_Form {
    @NotNull(message = "Vui lòng chọn địa chỉ.")
    @Positive(message = "Địa chỉ chưa hợp lệ.")
    private Long addressId;

    @Size(max = 80, message = "Mã voucher tối đa 80 ký tự.")
    private String voucher;

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getVoucher() { return voucher; }
    public void setVoucher(String voucher) { this.voucher = voucher; }
}
