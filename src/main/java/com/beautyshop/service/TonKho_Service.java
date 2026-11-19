package com.beautyshop.service;

import com.beautyshop.entity.BienTheSanPham;
import com.beautyshop.repository.BienTheSanPham_Repository;
import com.beautyshop.repository.TonKho_Repository;
import com.beautyshop.repository.TonKho_Repository.TrangThaiTamGiu;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TonKho_Service {
    private final BienTheSanPham_Repository bienTheRepository;
    private final TonKho_Repository tonKhoRepository;
    private final EntityManager entityManager;

    public TonKho_Service(BienTheSanPham_Repository bienTheRepository, TonKho_Repository tonKhoRepository,
                          EntityManager entityManager) {
        this.bienTheRepository = bienTheRepository;
        this.tonKhoRepository = tonKhoRepository;
        this.entityManager = entityManager;
    }

    public void reserveAtomically(long maDonHang, long maBienThe, int soLuong) {
        kiemTraSoLuong(soLuong);
        BienTheSanPham bienThe = khoa(maDonHang, maBienThe, "MOMO");
        var tamGiu = tonKhoRepository.timTamGiu(maDonHang, maBienThe);
        if (tamGiu.isPresent()) {
            var hienTai = tamGiu.get();
            if (hienTai.trangThai() == TrangThaiTamGiu.HELD && hienTai.soLuong() == soLuong) {
                return;
            }
            throw new IllegalStateException("Đơn đã có lượt tạm giữ khác cho SKU này.");
        }
        bienThe.giu(soLuong);
        tonKhoRepository.taoTamGiu(maDonHang, maBienThe, soLuong);
    }

    public void consumeReservation(long maDonHang, long maBienThe) {
        chuyenTamGiu(maDonHang, maBienThe, TrangThaiTamGiu.CONSUMED);
    }

    public void releaseReservation(long maDonHang, long maBienThe) {
        chuyenTamGiu(maDonHang, maBienThe, TrangThaiTamGiu.RELEASED);
    }

    public void deductCOD(long maDonHang, long maBienThe, int soLuong) {
        kiemTraSoLuong(soLuong);
        BienTheSanPham bienThe = khoa(maDonHang, maBienThe, "COD");
        var daTru = tonKhoRepository.timSoLuongDaTruCOD(maDonHang, maBienThe);
        if (daTru.isPresent()) {
            if (daTru.get() != soLuong) {
                throw new IllegalStateException("Số lượng COD khác với lần trừ kho đã ghi nhận.");
            }
            return;
        }
        bienThe.truCOD(soLuong);
        tonKhoRepository.ghiNhanTruCOD(maDonHang, maBienThe, soLuong);
    }

    // Chỉ gọi từ transaction chuyển đơn COD sang CANCELLED đang giữ khóa đơn.
    void hoanCOD(long maDonHang, long maBienThe, int soLuong) {
        BienTheSanPham bienThe = khoa(maDonHang, maBienThe, "COD");
        int daTru = tonKhoRepository.timSoLuongDaTruCOD(maDonHang, maBienThe)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy lần trừ tồn của đơn COD."));
        if (daTru != soLuong) {
            throw new IllegalStateException("Số lượng hoàn khác với lần trừ tồn.");
        }
        bienThe.hoanCOD(daTru);
    }

    private void chuyenTamGiu(long maDonHang, long maBienThe, TrangThaiTamGiu dich) {
        BienTheSanPham bienThe = khoa(maDonHang, maBienThe, "MOMO");
        var tamGiu = tonKhoRepository.timTamGiu(maDonHang, maBienThe)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy lượt tạm giữ SKU."));
        if (tamGiu.trangThai() == dich) {
            return;
        }
        if (tamGiu.trangThai() != TrangThaiTamGiu.HELD) {
            throw new IllegalStateException("Lượt tạm giữ đã kết thúc ở trạng thái khác.");
        }
        if (dich == TrangThaiTamGiu.CONSUMED) {
            bienThe.chotTamGiu(tamGiu.soLuong());
        } else {
            bienThe.giaiPhong(tamGiu.soLuong());
        }
        tonKhoRepository.doiTrangThai(maDonHang, maBienThe, dich);
    }

    private BienTheSanPham khoa(long maDonHang, long maBienThe, String phuongThuc) {
        String hienTai = tonKhoRepository.khoaDonHang(maDonHang)
            .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
        if (!phuongThuc.equals(hienTai)) {
            throw new IllegalArgumentException("Phương thức thanh toán của đơn không phù hợp.");
        }
        BienTheSanPham bienThe = bienTheRepository.khoa(maBienThe)
            .orElseThrow(() -> new IllegalArgumentException("SKU không tồn tại."));
        // Checkout có thể đã đọc SKU để tính giá trước khi khóa hàng.
        entityManager.refresh(bienThe, LockModeType.PESSIMISTIC_WRITE);
        return bienThe;
    }

    private void kiemTraSoLuong(int soLuong) {
        if (soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        }
    }
}
