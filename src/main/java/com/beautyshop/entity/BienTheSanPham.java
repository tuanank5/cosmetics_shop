package com.beautyshop.entity;

import com.beautyshop.inventory.InventoryRules;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bien_the_san_pham")
public class BienTheSanPham {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_bien_the")
    private Long maBienThe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ma_san_pham", nullable = false)
    private SanPham sanPham;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "ten_bien_the", nullable = false, length = 200)
    private String tenBienThe;

    @Column(name = "gia_ban", nullable = false, precision = 15, scale = 0)
    private BigDecimal giaBan;

    @Column(name = "so_luong_trong_kho", nullable = false)
    private int soLuongTrongKho;

    @Column(name = "so_luong_tam_giu", nullable = false)
    private int soLuongTamGiu;

    @Column(name = "url_hinh_anh", length = 1000)
    private String urlHinhAnh;

    @Column(name = "ma_lo_minh_hoa", length = 100)
    private String maLoMinhHoa;

    @Column(name = "han_su_dung_minh_hoa")
    private LocalDate hanSuDungMinhHoa;

    @Column(name = "dang_hoat_dong", nullable = false)
    private boolean dangHoatDong;

    protected BienTheSanPham() {}

    public BienTheSanPham(SanPham sanPham) {
        this.sanPham = sanPham;
    }

    public void capNhat(String sku, String tenBienThe, BigDecimal giaBan, int soLuongTrongKho,
                        String maLoMinhHoa, LocalDate hanSuDungMinhHoa, boolean dangHoatDong) {
        InventoryRules.available(soLuongTrongKho, soLuongTamGiu);
        this.sku = sku;
        this.tenBienThe = tenBienThe;
        this.giaBan = giaBan;
        this.soLuongTrongKho = soLuongTrongKho;
        this.maLoMinhHoa = maLoMinhHoa;
        this.hanSuDungMinhHoa = hanSuDungMinhHoa;
        this.dangHoatDong = dangHoatDong;
    }

    public void doiAnh(String urlHinhAnh) { this.urlHinhAnh = urlHinhAnh; }
    public void ngungHoatDong() { dangHoatDong = false; }

    public void giu(int soLuong) {
        kiemTraCoTheBan(soLuong);
        soLuongTamGiu += soLuong;
    }

    public void truCOD(int soLuong) {
        kiemTraCoTheBan(soLuong);
        soLuongTrongKho -= soLuong;
    }

    public void hoanCOD(int soLuong) {
        if (soLuong <= 0 || soLuong > Integer.MAX_VALUE - soLuongTrongKho) {
            throw new IllegalStateException("Không thể hoàn tồn vì số lượng trong kho vượt giới hạn.");
        }
        soLuongTrongKho += soLuong;
    }

    public void chotTamGiu(int soLuong) {
        giaiPhong(soLuong);
        soLuongTrongKho -= soLuong;
    }

    public void giaiPhong(int soLuong) {
        if (soLuong <= 0 || soLuong > soLuongTamGiu) {
            throw new IllegalStateException("Số lượng tạm giữ không hợp lệ.");
        }
        soLuongTamGiu -= soLuong;
    }

    private void kiemTraCoTheBan(int soLuong) {
        if (!dangHoatDong || !sanPham.isDangHoatDong() || soLuong <= 0 || soLuong > getSoLuongKhaDung()) {
            throw new IllegalStateException("SKU không còn đủ tồn khả dụng hoặc đã ngừng bán.");
        }
    }

    public Long getMaBienThe() { return maBienThe; }
    public String getSku() { return sku; }
    public String getTenBienThe() { return tenBienThe; }
    public BigDecimal getGiaBan() { return giaBan; }
    public int getSoLuongTrongKho() { return soLuongTrongKho; }
    public int getSoLuongTamGiu() { return soLuongTamGiu; }
    public int getSoLuongKhaDung() { return InventoryRules.available(soLuongTrongKho, soLuongTamGiu); }
    public String getUrlHinhAnh() { return urlHinhAnh; }
    public String getMaLoMinhHoa() { return maLoMinhHoa; }
    public LocalDate getHanSuDungMinhHoa() { return hanSuDungMinhHoa; }
    public boolean isDangHoatDong() { return dangHoatDong; }
}
