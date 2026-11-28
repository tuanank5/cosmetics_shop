package com.beautyshop.service;

import org.springframework.security.access.prepost.PreAuthorize;
import com.beautyshop.dto.BoLocDonHangAdmin;
import com.beautyshop.dto.PhanTrang;
import com.beautyshop.order.OrderState_Policy;
import com.beautyshop.order.OrderStatus;
import com.beautyshop.payment.Momo_Settings;
import com.beautyshop.repository.DiaChiNguoiDung_Repository;
import com.beautyshop.repository.DonHang_Repository;
import com.beautyshop.repository.DonHang_Repository.ChiTiet;
import com.beautyshop.repository.DonHang_Repository.Don;
import com.beautyshop.repository.DonHang_Repository.DongDonAdmin;
import com.beautyshop.repository.DonHang_Repository.TomTat;
import com.beautyshop.repository.GioHang_Repository;
import com.beautyshop.repository.ThanhToanMomo_Repository;
import com.beautyshop.shipping.ShippingFee_Policy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DonHang_Service {
    private static final BigDecimal GIOI_HAN_TIEN = new BigDecimal("999999999999999");

    private final DonHang_Repository donHangRepository;
    private final GioHang_Repository gioHangRepository;
    private final DiaChiNguoiDung_Repository diaChiRepository;
    private final TonKho_Service tonKho;
    private final PhieuGiamGia_Service voucher;
    private final QuyenDonHang_Service quyenDonHang;
    private final ThanhToanMomo_Repository thanhToanMomo;
    private final Momo_Settings momo;

    DonHang_Service(DonHang_Repository donHangRepository, GioHang_Repository gioHangRepository,
                   DiaChiNguoiDung_Repository diaChiRepository, TonKho_Service tonKho,
                   PhieuGiamGia_Service voucher, QuyenDonHang_Service quyenDonHang,
                   ThanhToanMomo_Repository thanhToanMomo, Momo_Settings momo) {
        this.donHangRepository = donHangRepository;
        this.gioHangRepository = gioHangRepository;
        this.diaChiRepository = diaChiRepository;
        this.tonKho = tonKho;
        this.voucher = voucher;
        this.quyenDonHang = quyenDonHang;
        this.thanhToanMomo = thanhToanMomo;
        this.momo = momo;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long datCOD(long maNguoiDung, long maDiaChi, String maVoucher) {
        return datDon(maNguoiDung, maDiaChi, maVoucher, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long datMomo(long maNguoiDung, long maDiaChi, String maVoucher) {
        String gateway = momo.gateway().orElseThrow(() -> new IllegalStateException("MoMo chưa khả dụng.")).maGateway();
        return datDon(maNguoiDung, maDiaChi, maVoucher, gateway);
    }

    private long datDon(long maNguoiDung, long maDiaChi, String maVoucher, String gateway) {
        boolean online = gateway != null;
        long maGio = gioHangRepository.khoaGio(maNguoiDung);
        var diaChi = diaChiRepository.findByMaDiaChiAndNguoiDungMaNguoiDung(maDiaChi, maNguoiDung)
            .orElseThrow(() -> new IllegalArgumentException("Chọn địa chỉ giao hàng của bạn."));
        var gio = gioHangRepository.docGio(maNguoiDung);
        if (gio.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống.");
        }
        List<ChiTiet> chiTiet = new ArrayList<>(gio.size());
        BigDecimal tamTinh = BigDecimal.ZERO;
        for (var muc : gio) {
            var sku = gioHangRepository.timBienThe(muc.maBienThe(), true)
                .orElseThrow(() -> new IllegalStateException("Có biến thể sản phẩm trong giỏ không còn tồn tại."));
            if (muc.soLuong() > sku.khaDung()) {
                throw new IllegalStateException("Không còn đủ số lượng đã chọn cho " + sku.tenSanPham() + " · " + sku.tenBienThe() + ".");
            }
            var snapshot = new ChiTiet(sku.maBienThe(), sku.sku(), sku.tenSanPham(), sku.tenBienThe(), sku.giaBan(), muc.soLuong(), null);
            chiTiet.add(snapshot);
            tamTinh = tamTinh.add(snapshot.thanhTien());
        }
        var giam = online ? voucher.giuOnline(maVoucher, tamTinh) : voucher.apDungCOD(maVoucher, tamTinh);
        var sauGiam = tamTinh.subtract(giam.soTien());
        var phi = ShippingFee_Policy.calculate(sauGiam);
        var tong = sauGiam.add(phi);
        if (tamTinh.compareTo(GIOI_HAN_TIEN) > 0 || tong.compareTo(GIOI_HAN_TIEN) > 0) {
            throw new IllegalArgumentException("Giá trị đơn vượt giới hạn thanh toán.");
        }
        if (online && (tong.compareTo(BigDecimal.valueOf(1000)) < 0 || tong.compareTo(BigDecimal.valueOf(50_000_000)) > 0)) {
            throw new IllegalArgumentException("MoMo ở chế độ thử nghiệm chỉ hỗ trợ đơn từ 1.000 ₫ đến 50.000.000 ₫.");
        }

        long maDon = donHangRepository.tao("BS-" + UUID.randomUUID(), maNguoiDung, giam.maVoucher(),
            diaChi.getTenNguoiNhan(), diaChi.getSoDienThoai(), diaChi.getDiaChiChiTiet(), tamTinh, giam.soTien(), phi, tong, online);
        for (var muc : chiTiet) {
            if (online) {
                tonKho.reserveAtomically(maDon, muc.maBienThe(), muc.soLuong());
            } else {
                tonKho.deductCOD(maDon, muc.maBienThe(), muc.soLuong());
            }
            donHangRepository.themMuc(maDon, muc);
        }
        if (online) {
            voucher.ghiTamGiu(maDon, giam.maVoucher());
            thanhToanMomo.tao(maDon, UUID.randomUUID().toString(), tong, gateway);
        } else {
            donHangRepository.taoThanhToanCOD(maDon, tong);
        }
        gioHangRepository.xoaCacMuc(maGio);
        return maDon;
    }

    public List<TomTat> danhSachCustomer(long maNguoiDung) { return donHangRepository.danhSach(maNguoiDung); }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<TomTat> danhSachCustomer(long maNguoiDung, Pageable pageable) {
        return PhanTrang.doc(pageable, trang -> donHangRepository.danhSach(maNguoiDung, trang));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<DongDonAdmin> danhSachAdmin(BoLocDonHangAdmin loc, int page) {
        return PhanTrang.doc(PageRequest.of(Math.max(0, page), loc.kichThuoc()),
            pageable -> donHangRepository.danhSachAdmin(loc, pageable));
    }
    @PreAuthorize("hasRole('ADMIN')")
    public DonHang_Repository.TongQuan tongQuanAdmin() { return donHangRepository.tongQuan(); }

    public ChiTietDon chiTietCustomer(long maDon) {
        quyenDonHang.kiemTraSoHuu(maDon);
        return docChiTiet(maDon);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ChiTietDon chiTietAdmin(long maDon) {
        return docChiTiet(maDon);
    }

    private ChiTietDon docChiTiet(long maDon) {
        Don don = timDon(maDon, false);
        boolean online = "MOMO".equals(don.phuongThuc());
        var thanhToan = (online ? thanhToanMomo.trangThaiHienThi(maDon) : donHangRepository.trangThaiThanhToanCOD(maDon)).orElse("");
        boolean mock = online && thanhToanMomo.moiNhat(maDon)
            .map(lan -> com.beautyshop.payment.MomoGateway.MA_MOCK.equals(lan.gateway())).orElse(false);
        boolean doiSoat = online && momo.enabled() && don.trangThai() == OrderStatus.PENDING_PAYMENT;
        return new ChiTietDon(don, donHangRepository.chiTiet(maDon), thanhToan,
            coTheHuy(don, thanhToan), next(don, "PAID".equals(thanhToan)), mock, doiSoat,
            doiSoat && thanhToanMomo.conHan(maDon), online ? thanhToanMomo.soLanCanHoan(maDon) : 0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void huyCustomer(long maDon) {
        quyenDonHang.kiemTraSoHuu(maDon);
        huyCOD(timDon(maDon, true));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void chuyenAdmin(long maDon, OrderStatus trangThaiMoi) {
        Don don = timDon(maDon, true);
        if (trangThaiMoi == OrderStatus.CANCELLED) {
            huyCOD(don);
            return;
        }
        boolean momoDaThu = "MOMO".equals(don.phuongThuc())
            && thanhToanMomo.trangThaiHienThi(maDon).filter("PAID"::equals).isPresent();
        if (next(don, momoDaThu) != trangThaiMoi
            || !OrderState_Policy.canMove(don.trangThai(), trangThaiMoi)) {
            throw new IllegalStateException("Không thể chuyển đơn sang trạng thái đã chọn.");
        }
        if (trangThaiMoi == OrderStatus.SHIPPING) {
            donHangRepository.guiHang(maDon);
        } else if (trangThaiMoi == OrderStatus.DELIVERED
            && (!donHangRepository.giaoXong(maDon) || ("COD".equals(don.phuongThuc()) && !donHangRepository.thuTienCOD(maDon)))) {
            throw new IllegalStateException("Không thể xác nhận hoàn tất giao hàng.");
        }
        donHangRepository.doiTrangThai(maDon, trangThaiMoi);
    }

    private void huyCOD(Don don) {
        if ("COD".equals(don.phuongThuc()) && don.trangThai() == OrderStatus.CANCELLED) {
            return;
        }
        var thanhToan = donHangRepository.trangThaiThanhToanCOD(don.maDonHang()).orElse("");
        if (!coTheHuy(don, thanhToan)) {
            throw new IllegalStateException("Đơn hiện không thể hủy.");
        }
        for (var muc : donHangRepository.chiTiet(don.maDonHang())) {
            tonKho.hoanCOD(don.maDonHang(), muc.maBienThe(), muc.soLuong());
        }
        voucher.hoanCOD(don.maVoucher());
        donHangRepository.doiTrangThai(don.maDonHang(), OrderStatus.CANCELLED);
    }

    private boolean coTheHuy(Don don, String thanhToan) {
        return "COD".equals(don.phuongThuc()) && "UNPAID".equals(thanhToan)
            && (don.trangThai() == OrderStatus.PENDING_CONFIRMATION || don.trangThai() == OrderStatus.CONFIRMED)
            && OrderState_Policy.canMove(don.trangThai(), OrderStatus.CANCELLED);
    }

    private OrderStatus next(Don don, boolean daThu) {
        if (!"COD".equals(don.phuongThuc()) && !("MOMO".equals(don.phuongThuc()) && daThu)) {
            return null;
        }
        return switch (don.trangThai()) {
            case PENDING_CONFIRMATION -> OrderStatus.CONFIRMED;
            case CONFIRMED -> OrderStatus.PACKING;
            case PACKING -> OrderStatus.SHIPPING;
            case SHIPPING -> OrderStatus.DELIVERED;
            default -> null;
        };
    }

    private Don timDon(long maDon, boolean khoa) {
        return donHangRepository.timDon(maDon, khoa)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record ChiTietDon(Don don, List<ChiTiet> muc, String thanhToan, boolean coTheHuy, OrderStatus tiepTheo,
                            boolean mock, boolean doiSoat, boolean coTheThuLai, long canHoanTien) {}
}
