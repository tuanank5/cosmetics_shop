package com.beautyshop.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dia_chi_nguoi_dung")
public class DiaChiNguoiDung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dia_chi")
    private Long maDiaChi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_nguoi_dung", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "ten_nguoi_nhan", nullable = false, length = 160)
    private String tenNguoiNhan;

    @Column(name = "so_dien_thoai", nullable = false, length = 30)
    private String soDienThoai;

    @Column(name = "dia_chi_chi_tiet", nullable = false, length = 600)
    private String diaChiChiTiet;

    @Column(name = "la_mac_dinh", nullable = false)
    private boolean laMacDinh;

    protected DiaChiNguoiDung() {}

    public DiaChiNguoiDung(NguoiDung nguoiDung) { this.nguoiDung = nguoiDung; }

    public void capNhat(String ten, String sdt, String diaChi) {
        tenNguoiNhan = ten;
        soDienThoai = sdt;
        diaChiChiTiet = diaChi;
    }

    public void datMacDinh(boolean macDinh) { laMacDinh = macDinh; }
    public Long getMaDiaChi() { return maDiaChi; }
    public String getTenNguoiNhan() { return tenNguoiNhan; }
    public String getSoDienThoai() { return soDienThoai; }
    public String getDiaChiChiTiet() { return diaChiChiTiet; }
    public boolean isLaMacDinh() { return laMacDinh; }
}
