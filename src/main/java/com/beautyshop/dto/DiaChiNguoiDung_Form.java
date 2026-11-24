package com.beautyshop.dto;

import com.beautyshop.entity.DiaChiNguoiDung;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DiaChiNguoiDung_Form {
    @NotBlank(message = "Vui lòng nhập tên người nhận.")
    @Size(max = 160, message = "Tên người nhận tối đa 160 ký tự.")
    private String tenNguoiNhan;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = HoSo_Form.MAU_SDT, message = "Số điện thoại cần 7–15 chữ số, có thể bắt đầu bằng +.")
    private String soDienThoai;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng.")
    @Size(max = 600, message = "Địa chỉ tối đa 600 ký tự.")
    private String diaChiChiTiet;

    private boolean laMacDinh;

    public static DiaChiNguoiDung_Form from(DiaChiNguoiDung diaChi) {
        var form = new DiaChiNguoiDung_Form();
        form.tenNguoiNhan = diaChi.getTenNguoiNhan();
        form.soDienThoai = diaChi.getSoDienThoai();
        form.diaChiChiTiet = diaChi.getDiaChiChiTiet();
        form.laMacDinh = diaChi.isLaMacDinh();
        return form;
    }

    public String getTenNguoiNhan() { return tenNguoiNhan; }
    public void setTenNguoiNhan(String ten) { tenNguoiNhan = ten == null ? null : ten.strip(); }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String sdt) { soDienThoai = sdt == null ? null : sdt.strip(); }
    public String getDiaChiChiTiet() { return diaChiChiTiet; }
    public void setDiaChiChiTiet(String diaChi) { diaChiChiTiet = diaChi == null ? null : diaChi.strip(); }
    public boolean isLaMacDinh() { return laMacDinh; }
    public void setLaMacDinh(boolean laMacDinh) { this.laMacDinh = laMacDinh; }
}
