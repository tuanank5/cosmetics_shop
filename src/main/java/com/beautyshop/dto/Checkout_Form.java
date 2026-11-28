package com.beautyshop.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class Checkout_Form {
    @NotNull(message = "Vui lòng chọn địa chỉ.")
    @Positive(message = "Địa chỉ chưa hợp lệ.")
    private Long addressId;

    @Size(max = 80, message = "Mã giảm giá tối đa 80 ký tự.")
    private String voucher;

    @Size(max = 80, message = "Mã vận chuyển tối đa 80 ký tự.")
    private String ship;
    public String getShip() { return ship; }
    public void setShip(String ship) { this.ship = ship; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public String getVoucher() { return voucher; }
    public void setVoucher(String voucher) { this.voucher = voucher; }
}
