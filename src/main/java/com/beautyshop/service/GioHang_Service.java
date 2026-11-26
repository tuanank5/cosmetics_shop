package com.beautyshop.service;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.repository.GioHang_Repository;
import com.beautyshop.repository.GioHang_Repository.MucGio;
import java.io.Serializable;
import java.util.ArrayList;
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
            throw new IllegalArgumentException("Biến thể này không còn đủ số lượng bạn chọn.");
        }
        return gio.them(maBienThe, 1);
    }

    public GioKhach datSoLuongKhach(GioKhach gio, long maBienThe, int soLuong) {
        if (!gio.soLuong().containsKey(maBienThe)) {
            throw new IllegalArgumentException("Không tìm thấy biến thể này trong giỏ hàng.");
        }
        kiemTraSoLuong(maBienThe, soLuong, false);
        return gio.datSoLuong(maBienThe, soLuong);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void datSoLuongCustomer(long maNguoiDung, long maBienThe, int soLuong) {
        long maGio = repository.khoaGio(maNguoiDung);
        if (repository.soLuong(maGio, maBienThe) == 0) {
            throw new IllegalArgumentException("Không tìm thấy biến thể này trong giỏ hàng.");
        }
        kiemTraSoLuong(maBienThe, soLuong, true);
        repository.luuSoLuong(maGio, maBienThe, soLuong);
    }

    private void kiemTraSoLuong(long maBienThe, int soLuong, boolean khoa) {
        if (soLuong < 0 || (soLuong > 0
            && soLuong > repository.timBienThe(maBienThe, khoa).map(MucGio::khaDung).orElse(0))) {
            throw new IllegalArgumentException("Số lượng bạn nhập không hợp lệ hoặc vượt quá số hàng còn bán được.");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void themGioCustomer(long maNguoiDung, long maBienThe) {
        long maGio = repository.khoaGio(maNguoiDung);
        int khaDung = repository.timBienThe(maBienThe, true).map(MucGio::khaDung).orElse(0);
        int hienTai = repository.soLuong(maGio, maBienThe);
        if (hienTai >= khaDung) {
            throw new IllegalArgumentException("Biến thể này không còn đủ số lượng bạn chọn.");
        }
        repository.luuSoLuong(maGio, maBienThe, hienTai + 1);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQuaGop gopSauDangNhap(long maNguoiDung, GioKhach gio) {
        if (gio.soLuong().isEmpty() || !repository.ghiNhanGop(gio.maGop().toString(), maNguoiDung)) {
            return new KetQuaGop(false, List.of());
        }
        long maGio = repository.khoaGio(maNguoiDung);
        boolean daGioiHan = false;
        List<DongGiam> cacDongGiam = new ArrayList<>();
        for (var muc : gio.soLuong().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            long maBienThe = muc.getKey();
            MucGio sku = repository.timBienThe(maBienThe, true).orElse(null);
            int khaDung = sku == null ? 0 : sku.khaDung();
            long tong = (long) repository.soLuong(maGio, maBienThe) + muc.getValue();
            int sauGop = (int) Math.min(tong, khaDung);
            repository.luuSoLuong(maGio, maBienThe, sauGop);
            daGioiHan |= sauGop < tong;
            if (sauGop > 0 && sauGop < tong) {
                cacDongGiam.add(new DongGiam(sku.tenSanPham(), sku.tenBienThe(), tong, sauGop));
            }
        }
        return new KetQuaGop(daGioiHan, cacDongGiam);
    }

    public record KetQuaGop(boolean daGioiHan, List<DongGiam> cacDongGiam) implements Serializable {
        public KetQuaGop {
            cacDongGiam = List.copyOf(cacDongGiam);
        }
    }

    public record DongGiam(String tenSanPham, String tenBienThe, long duKien, int thucNhan) implements Serializable {
        public long daGiam() { return duKien - thucNhan; }
    }
}
