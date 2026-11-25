package com.beautyshop.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * P23: một đánh giá sao + bình luận trong MongoDB (collection {@code danh_gia}).
 * Các mã {@code ma*} là snapshot từ MariaDB lúc tạo; MariaDB vẫn là nguồn kiểm điều kiện BR16.
 * Index được khai báo bằng annotation và tạo khi khởi động bằng IndexOperations (xem {@code MongoDanhGia_Store}).
 */
@Document(collection = DanhGia_Document.COLLECTION)
@CompoundIndexes({
    @CompoundIndex(name = "idx_san_pham_hien_thi_ngay", def = "{'maSanPham': 1, 'hienThi': 1, 'ngayTao': -1}"),
    @CompoundIndex(name = "idx_hien_thi_ngay", def = "{'hienThi': 1, 'ngayTao': -1}")
})
public class DanhGia_Document {
    public static final String COLLECTION = "danh_gia";
    public static final int NOI_DUNG_TOI_DA = 1000;

    @Id
    private String id;

    @Indexed(name = "uq_ma_chi_tiet_don_hang", unique = true)
    private Long maChiTietDonHang;
    private Long maDonHang;
    @Indexed(name = "idx_nguoi_dung")
    private Long maNguoiDung;
    private Long maSanPham;
    private Long maBienThe;
    /** Tên rút gọn công khai ("Thịnh P."); không lưu email/SĐT. */
    private String tenHienThi;
    private Integer soSao;
    /** ≤ 1000 ký tự, đã strip; có thể rỗng. */
    private String noiDung;
    /** false = chờ duyệt (ngayDuyet null) hoặc đã ẩn sau khi duyệt (ngayDuyet khác null). Sửa -> chờ duyệt lại. */
    private Boolean hienThi;
    /** Lần duyệt gần nhất của nội dung hiện tại; xóa khi khách sửa. */
    private Instant ngayDuyet;
    /**
     * Khóa lạc quan: tăng ở MỌI thay đổi document (khách sửa, Admin duyệt/ẩn). Admin chỉ duyệt được khi document vẫn đúng
     * phiên bản đã xem (thay SHA2 nội dung của bản MariaDB). Store ghi {@code $inc} tường minh; tạo mới đặt 0.
     */
    @Version
    private Long phienBan;
    private Instant ngayTao;
    private Instant ngayCapNhat;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getMaChiTietDonHang() { return maChiTietDonHang; }
    public void setMaChiTietDonHang(Long maChiTietDonHang) { this.maChiTietDonHang = maChiTietDonHang; }
    public Long getMaDonHang() { return maDonHang; }
    public void setMaDonHang(Long maDonHang) { this.maDonHang = maDonHang; }
    public Long getMaNguoiDung() { return maNguoiDung; }
    public void setMaNguoiDung(Long maNguoiDung) { this.maNguoiDung = maNguoiDung; }
    public Long getMaSanPham() { return maSanPham; }
    public void setMaSanPham(Long maSanPham) { this.maSanPham = maSanPham; }
    public Long getMaBienThe() { return maBienThe; }
    public void setMaBienThe(Long maBienThe) { this.maBienThe = maBienThe; }
    public String getTenHienThi() { return tenHienThi; }
    public void setTenHienThi(String tenHienThi) { this.tenHienThi = tenHienThi; }
    public Integer getSoSao() { return soSao; }
    public void setSoSao(Integer soSao) { this.soSao = soSao; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
    public Boolean getHienThi() { return hienThi; }
    public void setHienThi(Boolean hienThi) { this.hienThi = hienThi; }
    public Instant getNgayDuyet() { return ngayDuyet; }
    public void setNgayDuyet(Instant ngayDuyet) { this.ngayDuyet = ngayDuyet; }
    public Long getPhienBan() { return phienBan; }
    public void setPhienBan(Long phienBan) { this.phienBan = phienBan; }
    public Instant getNgayTao() { return ngayTao; }
    public void setNgayTao(Instant ngayTao) { this.ngayTao = ngayTao; }
    public Instant getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(Instant ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
}
