package com.beautyshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DanhGia_Form {
    @NotNull(message = "Vui lòng chọn số sao.")
    @Min(value = 1, message = "Số sao phải từ 1 đến 5.")
    @Max(value = 5, message = "Số sao phải từ 1 đến 5.")
    private Integer soSao;

    @Size(max = 1000, message = "Nội dung tối đa 1.000 ký tự.")
    private String noiDung;

    public Integer getSoSao() { return soSao; }
    public void setSoSao(Integer soSao) { this.soSao = soSao; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung == null ? null : noiDung.strip(); }
}
