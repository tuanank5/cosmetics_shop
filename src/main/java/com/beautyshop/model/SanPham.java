package com.beautyshop.model;

import com.beautyshop.dto.DanhMucSanPhamThuongHieu_Form;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "san_pham")
public class SanPham extends MucDanhMucSanPhamThuongHieu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_san_pham")
    private Long maSanPham;

    @Column(name = "ten_san_pham", nullable = false, length = 255)
    private String tenSanPham;

    public SanPham() {}

    public Long getMaSanPham() { return maSanPham; }
    public String getTenSanPham() { return tenSanPham; }

    @Override
    public Long getMa() { return getMaSanPham(); }

    @Override
    public String getTen() { return getTenSanPham(); }

    @Override
    protected void ganTen(String ten) { this.tenSanPham = ten; }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_danh_muc", nullable = false)
    private DanhMuc danhMuc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_thuong_hieu", nullable = false)
    private ThuongHieu thuongHieu;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "thanh_phan", columnDefinition = "TEXT")
    private String thanhPhan;

    @Column(name = "huong_dan_su_dung", columnDefinition = "TEXT")
    private String huongDanSuDung;

    @Column(name = "loai_da_phu_hop", length = 600)
    private String loaiDaPhuHop;

    @Column(name = "ngay_tao", nullable = false, insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    public void update(DanhMucSanPhamThuongHieu_Form form, DanhMuc danhMuc, ThuongHieu thuongHieu) {
        super.update(form);
        this.danhMuc = danhMuc;
        this.thuongHieu = thuongHieu;
        moTa = form.getDescription();
        thanhPhan = form.getIngredients();
        huongDanSuDung = form.getUsageGuide();
        loaiDaPhuHop = form.getSkinTypeTags();
    }

    public DanhMuc getDanhMuc() { return danhMuc; }
    public ThuongHieu getThuongHieu() { return thuongHieu; }
    public String getMoTa() { return moTa; }
    public String getThanhPhan() { return thanhPhan; }
    public String getHuongDanSuDung() { return huongDanSuDung; }
    public String getLoaiDaPhuHop() { return loaiDaPhuHop; }
}
