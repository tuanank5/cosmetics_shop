package com.beautyshop.service;

import com.beautyshop.repository.DonHangCatalog_Repository;
import com.beautyshop.repository.DonHangCatalog_Repository.PhamVi;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NgungHoatDongCatalog_Service {
    private final DonHangCatalog_Repository donHangCatalog;

    NgungHoatDongCatalog_Service(DonHangCatalog_Repository donHangCatalog) {
        this.donHangCatalog = donHangCatalog;
    }

    public Map<Long, Long> dem(PhamVi phamVi) {
        return donHangCatalog.dem(phamVi, null);
    }

    public long dem(PhamVi phamVi, long ma) {
        return donHangCatalog.dem(phamVi, ma).getOrDefault(ma, 0L);
    }

    public Map<Long, Long> demTheoBienThe(long maSanPham) {
        return donHangCatalog.dem(PhamVi.BIEN_THE, PhamVi.SAN_PHAM, maSanPham);
    }

    // The caller holds the checkout lock on the target catalog row. A fresh
    // committed read sees orders created by the previous holder of that lock.
    public void kiemTra(PhamVi phamVi, long ma) {
        long soDon = dem(phamVi, ma);
        if (soDon > 0) throw new ConDonDangXuLy(soDon);
    }

    public static class ConDonDangXuLy extends IllegalStateException {
        ConDonDangXuLy(long soDon) {
            super("Không thể ngừng hoạt động vì còn " + soDon + " đơn hàng liên quan đang xử lý.");
        }
    }
}
