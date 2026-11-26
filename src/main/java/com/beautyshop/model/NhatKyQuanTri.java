package com.beautyshop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "nhat_ky_quan_tri")
public class NhatKyQuanTri {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nhat_ky")
    private Long maNhatKy;
    @Column(name = "ma_nguoi_thuc_hien")
    private Long maNguoiThucHien;
    @Column(name = "vai_tro_nguoi_thuc_hien")
    private String vaiTroNguoiThucHien;
    @Column(name = "hanh_dong")
    private String hanhDong;
    @Column(name = "loai_doi_tuong")
    private String loaiDoiTuong;
    @Column(name = "ma_doi_tuong")
    private long maDoiTuong;
    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;
    @Column(name = "ket_qua")
    private String ketQua;
    @Column(name = "ly_do")
    private String lyDo;
    @Column(name = "correlation_id", columnDefinition = "CHAR(36)")
    private String correlationId;

    protected NhatKyQuanTri() {}

    public NhatKyQuanTri(Long actor, String role, String action, String targetType, long target,
                          String result, String reason, String correlation) {
        maNguoiThucHien = actor;
        vaiTroNguoiThucHien = role;
        hanhDong = action;
        loaiDoiTuong = targetType;
        maDoiTuong = target;
        thoiGian = LocalDateTime.now(ZoneOffset.UTC);
        ketQua = result;
        lyDo = reason;
        correlationId = correlation;
    }
}
