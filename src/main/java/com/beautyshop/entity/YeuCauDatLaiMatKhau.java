package com.beautyshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "yeu_cau_dat_lai_mat_khau")
public class YeuCauDatLaiMatKhau {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_yeu_cau")
    private Long maYeuCau;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_nguoi_dung")
    private NguoiDung nguoiDung;
    @Column(name = "token_bam", columnDefinition = "BINARY(32)")
    private byte[] tokenBam;
    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;
    @Column(name = "het_han_luc")
    private LocalDateTime hetHanLuc;
    @Column(name = "da_su_dung_luc")
    private LocalDateTime daSuDungLuc;
    @Column(name = "nguon_yeu_cau")
    private String nguonYeuCau;
    @Column(name = "phien_ban_xac_thuc")
    private long phienBanXacThuc;
    @Column(name = "correlation_id", columnDefinition = "CHAR(36)")
    private String correlationId;

    protected YeuCauDatLaiMatKhau() {}

    public YeuCauDatLaiMatKhau(NguoiDung user, byte[] hash, LocalDateTime now, String source, String correlation) {
        nguoiDung = user;
        tokenBam = hash;
        ngayTao = now;
        hetHanLuc = now.plusMinutes(15);
        nguonYeuCau = source;
        phienBanXacThuc = user.getPhienBanXacThuc();
        correlationId = correlation;
    }

    public long getMaYeuCau() { return maYeuCau; }
    public String getCorrelationId() { return correlationId; }
    public boolean hopLe(LocalDateTime now, long version) {
        return daSuDungLuc == null && now.isBefore(hetHanLuc) && phienBanXacThuc == version;
    }
    public void tieuThu(LocalDateTime now) { daSuDungLuc = now; }
}
