package com.beautyshop.dto;

import jakarta.validation.constraints.AssertTrue;
import java.util.Objects;
import com.beautyshop.service.MatKhau_Service;

public class MatKhauMoi_Form {
    private String matKhauMoi;
    private String xacNhan;
    public String getMatKhauMoi() { return matKhauMoi; }
    public void setMatKhauMoi(String value) { matKhauMoi = value; }
    public String getXacNhan() { return xacNhan; }
    public void setXacNhan(String value) { xacNhan = value; }

    @AssertTrue
    public boolean isHopLe() {
        return MatKhau_Service.matKhauHopLe(matKhauMoi) && Objects.equals(matKhauMoi, xacNhan);
    }
}
