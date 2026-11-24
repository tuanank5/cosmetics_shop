
package com.beautyshop.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TuVanDaRequest(

        @NotBlank(message = "Vui lòng chọn loại da")
        @Size(max = 50, message = "Loại da không được vượt quá 50 ký tự")
        String loaiDa,

        @NotBlank(message = "Vui lòng mô tả vấn đề da")
        @Size(max = 1000, message = "Mô tả vấn đề da không được vượt quá 1000 ký tự")
        String vanDeDa,

        @NotBlank(message = "Vui lòng cung cấp thông tin về độ nhạy cảm")
        @Size(max = 100, message = "Thông tin độ nhạy cảm không được vượt quá 100 ký tự")
        String daNhayCam,

        @NotBlank(message = "Vui lòng chọn nhu cầu chăm sóc da")
        @Size(max = 200, message = "Nhu cầu không được vượt quá 200 ký tự")
        String nhuCau
) {
}
