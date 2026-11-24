package com.beautyshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TuVan_Form {
    @NotBlank(message = "Vui lòng nhập câu hỏi.")
    @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự.")
    private String cauHoi;

    public String getCauHoi() { return cauHoi; }
    public void setCauHoi(String cauHoi) { this.cauHoi = cauHoi; }
}
