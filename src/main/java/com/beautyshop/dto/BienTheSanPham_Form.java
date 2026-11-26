package com.beautyshop.dto;

import com.beautyshop.model.BienTheSanPham;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

public class BienTheSanPham_Form {
    @NotBlank(message = "Vui lòng nhập mã biến thể sản phẩm.")
    @Size(max = 100, message = "Mã biến thể sản phẩm tối đa 100 ký tự.")
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*", message = "Mã biến thể sản phẩm chỉ dùng chữ, số, dấu chấm, gạch ngang hoặc gạch dưới.")
    private String sku;

    @NotBlank(message = "Vui lòng nhập tên biến thể.")
    @Size(max = 200, message = "Tên tối đa 200 ký tự.")
    private String tenBienThe;

    @NotNull(message = "Vui lòng nhập giá bán.")
    @DecimalMin(value = "0", message = "Giá bán phải từ 0 ₫.")
    @Digits(integer = 15, fraction = 0, message = "Giá bán phải là số nguyên VNĐ tối đa 15 chữ số.")
    private BigDecimal giaBan;

    @NotNull(message = "Vui lòng nhập số lượng trong kho.")
    @PositiveOrZero(message = "Số lượng trong kho không được âm.")
    private Integer soLuongTrongKho;

    @Size(max = 100, message = "Mã lô tối đa 100 ký tự.")
    private String maLoMinhHoa;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hanSuDungMinhHoa;

    private boolean dangHoatDong = true;
    private MultipartFile hinhAnh;
    private boolean xoaHinhAnh;

    public static BienTheSanPham_Form from(BienTheSanPham bienThe) {
        var form = new BienTheSanPham_Form();
        form.sku = bienThe.getSku();
        form.tenBienThe = bienThe.getTenBienThe();
        form.giaBan = bienThe.getGiaBan();
        form.soLuongTrongKho = bienThe.getSoLuongTrongKho();
        form.maLoMinhHoa = bienThe.getMaLoMinhHoa();
        form.hanSuDungMinhHoa = bienThe.getHanSuDungMinhHoa();
        form.dangHoatDong = bienThe.isDangHoatDong();
        return form;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getTenBienThe() { return tenBienThe; }
    public void setTenBienThe(String tenBienThe) { this.tenBienThe = tenBienThe; }
    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
    public Integer getSoLuongTrongKho() { return soLuongTrongKho; }
    public void setSoLuongTrongKho(Integer soLuongTrongKho) { this.soLuongTrongKho = soLuongTrongKho; }
    public String getMaLoMinhHoa() { return maLoMinhHoa; }
    public void setMaLoMinhHoa(String maLoMinhHoa) { this.maLoMinhHoa = maLoMinhHoa; }
    public LocalDate getHanSuDungMinhHoa() { return hanSuDungMinhHoa; }
    public void setHanSuDungMinhHoa(LocalDate hanSuDungMinhHoa) { this.hanSuDungMinhHoa = hanSuDungMinhHoa; }
    public boolean isDangHoatDong() { return dangHoatDong; }
    public void setDangHoatDong(boolean dangHoatDong) { this.dangHoatDong = dangHoatDong; }
    public MultipartFile getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(MultipartFile hinhAnh) { this.hinhAnh = hinhAnh; }
    public boolean isXoaHinhAnh() { return xoaHinhAnh; }
    public void setXoaHinhAnh(boolean xoaHinhAnh) { this.xoaHinhAnh = xoaHinhAnh; }
}
