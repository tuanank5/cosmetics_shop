package com.beautyshop.service;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.repository.GioHang_Repository;
import com.beautyshop.repository.GioHang_Repository.MucGio;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GioHang_Service {
    private final GioHang_Repository repository;

    GioHang_Service(GioHang_Repository repository) {
        this.repository = repository;
    }

    public List<MucGio> docGioKhach(GioKhach gio) {
        return gio.soLuong().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .flatMap(muc -> repository.timBienThe(muc.getKey(), false).stream()
                .map(sku -> sku.voiSoLuong(muc.getValue())))
            .toList();
    }

    public List<MucGio> docGioCustomer(long maNguoiDung) {
        return repository.docGio(maNguoiDung);
    }

    public GioKhach themGioKhach(GioKhach gio, long maBienThe) {
        int khaDung = repository.timBienThe(maBienThe, false).map(MucGio::khaDung).orElse(0);
        if (gio.soLuong().getOrDefault(maBienThe, 0) >= khaDung) {
            throw new IllegalArgumentException("SKU không đủ tồn khả dụng.");
        }
        return gio.them(maBienThe, 1);
    }

    public GioKhach datSoLuongKhach(GioKhach gio, long maBienThe, int soLuong) {
        if (!gio.soLuong().containsKey(maBienThe)) {
            throw new IllegalArgumentException("SKU không có trong giỏ.");
        }
        kiemTraSoLuong(maBienThe, soLuong, false);
        return gio.datSoLuong(maBienThe, soLuong);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void datSoLuongCustomer(long maNguoiDung, long maBienThe, int soLuong) {
        long maGio = repository.khoaGio(maNguoiDung);
        if (repository.soLuong(maGio, maBienThe) == 0) {
            throw new IllegalArgumentException("SKU không có trong giỏ.");
        }
        kiemTraSoLuong(maBienThe, soLuong, true);
        repository.luuSoLuong(maGio, maBienThe, soLuong);
    }

    private void kiemTraSoLuong(long maBienThe, int soLuong, boolean khoa) {
        if (soLuong < 0 || (soLuong > 0
            && soLuong > repository.timBienThe(maBienThe, khoa).map(MucGio::khaDung).orElse(0))) {
            throw new IllegalArgumentException("Số lượng vượt tồn khả dụng hoặc không hợp lệ.");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void themGioCustomer(long maNguoiDung, long maBienThe) {
        long maGio = repository.khoaGio(maNguoiDung);
        int khaDung = repository.timBienThe(maBienThe, true).map(MucGio::khaDung).orElse(0);
        int hienTai = repository.soLuong(maGio, maBienThe);
        if (hienTai >= khaDung) {
            throw new IllegalArgumentException("SKU không đủ tồn khả dụng.");
        }
        repository.luuSoLuong(maGio, maBienThe, hienTai + 1);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean gopSauDangNhap(long maNguoiDung, GioKhach gio) {
        if (gio.soLuong().isEmpty() || !repository.ghiNhanGop(gio.maGop().toString(), maNguoiDung)) {
            return false;
        }
        long maGio = repository.khoaGio(maNguoiDung);
        boolean daGioiHan = false;
        for (var muc : gio.soLuong().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            long maBienThe = muc.getKey();
            int khaDung = repository.timBienThe(maBienThe, true).map(MucGio::khaDung).orElse(0);
            long tong = (long) repository.soLuong(maGio, maBienThe) + muc.getValue();
            int sauGop = (int) Math.min(tong, khaDung);
            repository.luuSoLuong(maGio, maBienThe, sauGop);
            daGioiHan |= sauGop < tong;
        }
        return daGioiHan;
    }
}
