package com.beautyshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "danh_tinh_oauth")
public class DanhTinhOAuth {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_danh_tinh")
    private Long maDanhTinh;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_nguoi_dung", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "nha_cung_cap", nullable = false)
    private String nhaCungCap;

    @Column(name = "ma_dinh_danh_nha_cung_cap", nullable = false)
    private String maDinhDanhNhaCungCap;

    @Column(name = "ngay_lien_ket")
    private LocalDateTime ngayLienKet;

    protected DanhTinhOAuth() {}

    public DanhTinhOAuth(NguoiDung nguoiDung, String nhaCungCap, String subject, LocalDateTime thoiDiem) {
        this.nguoiDung = nguoiDung;
        this.nhaCungCap = nhaCungCap;
        this.maDinhDanhNhaCungCap = subject;
        this.ngayLienKet = thoiDiem;
    }

    public NguoiDung getNguoiDung() { return nguoiDung; }
    public String getNhaCungCap() { return nhaCungCap; }
}
