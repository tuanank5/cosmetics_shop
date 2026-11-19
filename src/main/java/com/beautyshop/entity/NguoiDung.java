package com.beautyshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "nguoi_dung")
public class NguoiDung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nguoi_dung")
    private Long maNguoiDung;

    @Column(name = "email", nullable = false, length = 190)
    private String email;

    @Column(name = "mat_khau_bam", nullable = false, length = 255)
    private String matKhauBam;

    @Column(name = "ho_ten", nullable = false, length = 160)
    private String hoTen;

    @Column(name = "so_dien_thoai", length = 30)
    private String soDienThoai;

    @Column(name = "vai_tro", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private VaiTro vaiTro;

    @Column(name = "kich_hoat", nullable = false)
    private boolean kichHoat;

    @Column(name = "phien_ban_xac_thuc", nullable = false)
    private long phienBanXacThuc;

    protected NguoiDung() {}

    public NguoiDung(String email, String matKhauBam, String hoTen) {
        this.email = email;
        this.matKhauBam = matKhauBam;
        this.hoTen = hoTen;
        this.vaiTro = VaiTro.CUSTOMER;
        this.kichHoat = true;
    }

    public Long getMaNguoiDung() { return maNguoiDung; }
    public String getEmail() { return email; }
    public String getMatKhauBam() { return matKhauBam; }
    public String getHoTen() { return hoTen; }
    public String getSoDienThoai() { return soDienThoai; }
    public VaiTro getVaiTro() { return vaiTro; }
    public boolean isKichHoat() { return kichHoat; }
    public long getPhienBanXacThuc() { return phienBanXacThuc; }

    public void capNhatHoSo(String hoTen, String soDienThoai) {
        this.hoTen = hoTen;
        this.soDienThoai = soDienThoai;
    }

    public void datKichHoat(boolean kichHoat) {
        if (this.kichHoat && !kichHoat) {
            phienBanXacThuc++;
        }
        this.kichHoat = kichHoat;
    }

    public enum VaiTro { CUSTOMER, ADMIN }
}
