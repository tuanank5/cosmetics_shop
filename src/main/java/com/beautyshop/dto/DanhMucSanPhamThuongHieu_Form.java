package com.beautyshop.dto;

import com.beautyshop.entity.MucDanhMucSanPhamThuongHieu;
import com.beautyshop.entity.SanPham;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DanhMucSanPhamThuongHieu_Form {
    private String ten;

    private String duongDan;

    private boolean dangHoatDong = true;

    private Long maDanhMuc;

    private Long maThuongHieu;

    private String moTa;

    private String thanhPhan;

    private String huongDanSuDung;

    private String loaiDaPhuHop;

    public static DanhMucSanPhamThuongHieu_Form from(MucDanhMucSanPhamThuongHieu muc) {
        DanhMucSanPhamThuongHieu_Form form = new DanhMucSanPhamThuongHieu_Form();
        form.ten = muc.getTen();
        form.duongDan = muc.getDuongDan();
        form.dangHoatDong = muc.isDangHoatDong();
        if (muc instanceof SanPham sanPham) {
            form.maDanhMuc = sanPham.getDanhMuc().getMaDanhMuc();
            form.maThuongHieu = sanPham.getThuongHieu().getMaThuongHieu();
            form.moTa = sanPham.getMoTa();
            form.thanhPhan = sanPham.getThanhPhan();
            form.huongDanSuDung = sanPham.getHuongDanSuDung();
            form.loaiDaPhuHop = sanPham.getLoaiDaPhuHop();
        }
        return form;
    }

    @NotBlank(message = "Vui lòng nhập tên.")
    @Size(max = 255, message = "Tên tối đa 255 ký tự.")
    public String getName() { return ten; }
    public void setName(String ten) { this.ten = ten; }

    @NotBlank(message = "Vui lòng nhập đường dẫn.")
    @Size(max = 190, message = "Đường dẫn tối đa 190 ký tự.")
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Dùng chữ thường không dấu, số và dấu gạch ngang.")
    public String getSlug() { return duongDan; }
    public void setSlug(String duongDan) { this.duongDan = duongDan; }

    public boolean isActive() { return dangHoatDong; }
    public void setActive(boolean dangHoatDong) { this.dangHoatDong = dangHoatDong; }

    public Long getCategoryId() { return maDanhMuc; }
    public void setCategoryId(Long maDanhMuc) { this.maDanhMuc = maDanhMuc; }

    public Long getBrandId() { return maThuongHieu; }
    public void setBrandId(Long maThuongHieu) { this.maThuongHieu = maThuongHieu; }

    @Size(max = 16000, message = "Tối đa 16000 ký tự.")
    public String getDescription() { return moTa; }
    public void setDescription(String moTa) { this.moTa = moTa; }

    @Size(max = 16000, message = "Tối đa 16000 ký tự.")
    public String getIngredients() { return thanhPhan; }
    public void setIngredients(String thanhPhan) { this.thanhPhan = thanhPhan; }

    @Size(max = 16000, message = "Tối đa 16000 ký tự.")
    public String getUsageGuide() { return huongDanSuDung; }
    public void setUsageGuide(String huongDanSuDung) { this.huongDanSuDung = huongDanSuDung; }

    @Size(max = 600, message = "Tối đa 600 ký tự.")
    public String getSkinTypeTags() { return loaiDaPhuHop; }
    public void setSkinTypeTags(String loaiDaPhuHop) { this.loaiDaPhuHop = loaiDaPhuHop; }
}
