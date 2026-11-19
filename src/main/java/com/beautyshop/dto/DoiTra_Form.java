package com.beautyshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public class DoiTra_Form {
    @NotBlank(message = "Vui lòng nhập lý do đổi trả.")
    @Size(max = 2000, message = "Lý do tối đa 2.000 ký tự.")
    private String lyDo;

    private Map<Long, Integer> soLuong = new LinkedHashMap<>();

    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo == null ? null : lyDo.strip(); }
    public Map<Long, Integer> getSoLuong() { return soLuong; }
    public void setSoLuong(Map<Long, Integer> soLuong) { this.soLuong = soLuong; }
}
