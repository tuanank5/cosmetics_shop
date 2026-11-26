package com.beautyshop.model;

import jakarta.persistence.*;

@Entity
@Table(name = "danh_muc")
public class DanhMuc extends MucDanhMucSanPhamThuongHieu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_danh_muc")
    private Long maDanhMuc;

    @Column(name = "ten_danh_muc", nullable = false, length = 160)
    private String tenDanhMuc;

    public DanhMuc() {}

    public Long getMaDanhMuc() { return maDanhMuc; }
    public String getTenDanhMuc() { return tenDanhMuc; }

    @Override
    public Long getMa() { return getMaDanhMuc(); }

    @Override
    public String getTen() { return getTenDanhMuc(); }

    @Override
    protected void ganTen(String ten) { this.tenDanhMuc = ten; }
}
