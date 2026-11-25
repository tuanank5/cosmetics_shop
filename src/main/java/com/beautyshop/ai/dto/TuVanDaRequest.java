
package com.beautyshop.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TuVanDaRequest(

        @NotBlank(message = "Vui lòng chọn loại da")
        @Pattern(
                regexp = "Da dầu|Da khô|Da hỗn hợp|Da thường|Da nhạy cảm",
                message = "Vui lòng chọn loại da trong danh sách")
        @Size(max = 50, message = "Loại da không được vượt quá 50 ký tự")
        String loaiDa,

        @NotBlank(message = "Vui lòng mô tả vấn đề da")
        @Size(max = 1000, message = "Mô tả vấn đề da không được vượt quá 1000 ký tự")
        String vanDeDa,

        @NotBlank(message = "Vui lòng cung cấp thông tin về độ nhạy cảm")
        @Pattern(
                regexp = "Không nhạy cảm|Ít nhạy cảm|Nhạy cảm|Rất nhạy cảm",
                message = "Vui lòng chọn độ nhạy cảm trong danh sách")
        @Size(max = 100, message = "Thông tin độ nhạy cảm không được vượt quá 100 ký tự")
        String daNhayCam,

        @NotBlank(message = "Vui lòng chọn nhu cầu chăm sóc da")
        @Pattern(
                regexp = "Dưỡng ẩm|Kiểm soát dầu|Giảm mụn|Làm sáng da|Phục hồi da|Chăm sóc da cơ bản",
                message = "Vui lòng chọn nhu cầu trong danh sách")
        @Size(max = 200, message = "Nhu cầu không được vượt quá 200 ký tự")
        String nhuCau
) {
}
