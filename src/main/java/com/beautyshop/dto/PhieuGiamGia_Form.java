package com.beautyshop.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class PhieuGiamGia_Form {
    @NotBlank(message = "Nhập mã giảm giá.")
    @Size(max = 80, message = "Mã giảm giá dài tối đa 80 ký tự.")
    private String maApDung;

    @NotNull(message = "Chọn loại giảm giá.")
    private LoaiGiamGia loaiGiamGia;

    @NotNull(message = "Nhập giá trị giảm.")
    @Positive(message = "Giá trị giảm phải lớn hơn 0.")
    @Digits(integer = 15, fraction = 0, message = "Nhập số nguyên tối đa 15 chữ số.")
    private BigDecimal giaTriGiam;

    @PositiveOrZero(message = "Mức giảm tối đa phải từ 0 trở lên.")
    @Digits(integer = 15, fraction = 0, message = "Nhập số nguyên tối đa 15 chữ số.")
    private BigDecimal mucGiamToiDa;

    @NotNull(message = "Nhập tạm tính tối thiểu.")
    @PositiveOrZero(message = "Tiền hàng tối thiểu để dùng mã phải từ 0 trở lên.")
    @Digits(integer = 15, fraction = 0, message = "Nhập số nguyên tối đa 15 chữ số.")
    private BigDecimal donToiThieu = BigDecimal.ZERO;

    @NotNull(message = "Nhập giới hạn sử dụng.")
    @PositiveOrZero(message = "Giới hạn sử dụng phải từ 0 trở lên.")
    private Integer gioiHanSuDung;

    @NotNull(message = "Nhập thời điểm bắt đầu.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime batDauLuc;

    @NotNull(message = "Nhập thời điểm kết thúc.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime ketThucLuc;

    private boolean dangHoatDong = true;

    public String getMaApDung() { return maApDung; }
    public void setMaApDung(String maApDung) { this.maApDung = maApDung; }
    public LoaiGiamGia getLoaiGiamGia() { return loaiGiamGia; }
    public void setLoaiGiamGia(LoaiGiamGia loaiGiamGia) { this.loaiGiamGia = loaiGiamGia; }
    public BigDecimal getGiaTriGiam() { return giaTriGiam; }
    public void setGiaTriGiam(BigDecimal giaTriGiam) { this.giaTriGiam = giaTriGiam; }
    public BigDecimal getMucGiamToiDa() { return mucGiamToiDa; }
    public void setMucGiamToiDa(BigDecimal mucGiamToiDa) { this.mucGiamToiDa = mucGiamToiDa; }
    public BigDecimal getDonToiThieu() { return donToiThieu; }
    public void setDonToiThieu(BigDecimal donToiThieu) { this.donToiThieu = donToiThieu; }
    public Integer getGioiHanSuDung() { return gioiHanSuDung; }
    public void setGioiHanSuDung(Integer gioiHanSuDung) { this.gioiHanSuDung = gioiHanSuDung; }
    public LocalDateTime getBatDauLuc() { return batDauLuc; }
    public void setBatDauLuc(LocalDateTime batDauLuc) { this.batDauLuc = batDauLuc; }
    public LocalDateTime getKetThucLuc() { return ketThucLuc; }
    public void setKetThucLuc(LocalDateTime ketThucLuc) { this.ketThucLuc = ketThucLuc; }
    public boolean isDangHoatDong() { return dangHoatDong; }
    public void setDangHoatDong(boolean dangHoatDong) { this.dangHoatDong = dangHoatDong; }

    public enum Slot { HANG, VAN_CHUYEN }
    @NotNull private Slot slot = Slot.HANG;
    @PositiveOrZero private Integer gioiHanKhach;
    public Slot getSlot() { return slot; }
    public void setSlot(Slot slot) { this.slot = slot; }
    public Integer getGioiHanKhach() { return gioiHanKhach; }
    public void setGioiHanKhach(Integer gioiHanKhach) { this.gioiHanKhach = gioiHanKhach; }
    @Size(max = 4000) private String sanPham = "";
    public String getSanPham() { return sanPham; }
    public void setSanPham(String ids) { this.sanPham = ids; }

    @Size(max = 4000) private String danhMuc = "";
    public String getDanhMuc() { return danhMuc; }
    public void setDanhMuc(String ids) { this.danhMuc = ids; }

    @Size(max = 4000) private String thuongHieu = "";
    public String getThuongHieu() { return thuongHieu; }
    public void setThuongHieu(String ids) { this.thuongHieu = ids; }

    @Size(max = 4000) private String nguoiDung = "";
    public String getNguoiDung() { return nguoiDung; }
    public void setNguoiDung(String ids) { this.nguoiDung = ids; }

    public enum LoaiGiamGia { FIXED, PERCENT }
}
