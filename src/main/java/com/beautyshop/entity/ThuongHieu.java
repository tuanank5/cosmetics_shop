package com.beautyshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "thuong_hieu")
public class ThuongHieu extends MucDanhMucSanPhamThuongHieu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_thuong_hieu")
    private Long maThuongHieu;

    @Column(name = "ten_thuong_hieu", nullable = false, length = 160)
    private String tenThuongHieu;

    public ThuongHieu() {}

    public Long getMaThuongHieu() { return maThuongHieu; }
    public String getTenThuongHieu() { return tenThuongHieu; }

    @Override
    public Long getMa() { return getMaThuongHieu(); }

    @Override
    public String getTen() { return getTenThuongHieu(); }

    @Override
    protected void ganTen(String ten) { this.tenThuongHieu = ten; }
}
