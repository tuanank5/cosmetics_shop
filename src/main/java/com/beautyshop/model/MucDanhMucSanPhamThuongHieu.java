package com.beautyshop.model;

import com.beautyshop.dto.DanhMucSanPhamThuongHieu_Form;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class MucDanhMucSanPhamThuongHieu {
    @Column(name = "duong_dan", nullable = false, length = 190)
    private String duongDan;

    @Column(name = "dang_hoat_dong", nullable = false)
    private boolean dangHoatDong;

    public void update(DanhMucSanPhamThuongHieu_Form form) {
        ganTen(form.getName().strip());
        duongDan = form.getSlug();
        datTrangThai(form.isActive());
    }

    public void datTrangThai(boolean hoatDong) {
        dangHoatDong = hoatDong;
    }

    protected abstract void ganTen(String ten);
    public abstract Long getMa();
    public abstract String getTen();
    public String getDuongDan() { return duongDan; }
    public boolean isDangHoatDong() { return dangHoatDong; }
}
