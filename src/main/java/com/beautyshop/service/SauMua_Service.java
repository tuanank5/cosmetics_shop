package com.beautyshop.service;

import com.beautyshop.dto.PhanTrang;

import org.springframework.security.access.prepost.PreAuthorize;
import com.beautyshop.dto.DanhGia_Form;
import com.beautyshop.dto.DoiTra_Form;
import com.beautyshop.order.ReturnStatus;
import com.beautyshop.model.DanhGia_Document;
import com.beautyshop.repository.DanhGia_Repository;
import com.beautyshop.repository.DanhGia_Repository.MucMua;
import com.beautyshop.repository.DanhGia_Repository.SanPhamLoc;
import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.repository.DanhGia_Store.BoLocDanhGia;
import com.beautyshop.repository.DanhGia_Store.DanhGiaAdmin;
import com.beautyshop.repository.DanhGia_Store.DanhGiaCongKhai;
import com.beautyshop.repository.DanhGia_Store.DanhGiaCuaKhach;
import com.beautyshop.repository.DanhGia_Store.DanhGiaMoi;
import com.beautyshop.repository.DanhGia_Store.TomTatDanhGia;
import com.beautyshop.repository.DoiTra_Repository;
import com.beautyshop.repository.DoiTra_Repository.ChiTiet;
import com.beautyshop.repository.DoiTra_Repository.Don;
import com.beautyshop.repository.DoiTra_Repository.Muc;
import com.beautyshop.repository.DoiTra_Repository.MucTra;
import com.beautyshop.repository.YeuThich_Repository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SauMua_Service {
    private static final Logger log = LoggerFactory.getLogger(SauMua_Service.class);
    public static final int DANH_GIA_MOI_TRANG = 10;
    public static final int DANH_GIA_ADMIN_MOI_TRANG = 20;
    public static final String LOI_KHO_DANH_GIA = "Tạm thời chưa lưu được đánh giá. Vui lòng thử lại sau ít phút.";

    /** Điều kiện BR16 luôn đọc từ MariaDB; nội dung đánh giá đọc/ghi qua {@link DanhGia_Store} (P23). */
    private final DanhGia_Repository danhGia;
    private final DanhGia_Store kho;
    private final DoiTra_Repository doiTra;
    private final YeuThich_Repository yeuThich;

    SauMua_Service(DanhGia_Repository danhGia, DanhGia_Store kho, DoiTra_Repository doiTra, YeuThich_Repository yeuThich) {
        this.danhGia = danhGia;
        this.kho = kho;
        this.doiTra = doiTra;
        this.yeuThich = yeuThich;
    }

    /** Trang chi tiết sản phẩm: tóm tắt + danh sách công khai; kho đánh giá lỗi thì trả cờ để trang hiện thông báo. */
    public TrangDanhGiaSanPham danhGiaSanPham(long maSanPham, int trang) {
        var pageable = PageRequest.of(Math.clamp(trang, 0, 10_000), DANH_GIA_MOI_TRANG);
        try {
            return new TrangDanhGiaSanPham(kho.tomTat(maSanPham),
                PhanTrang.doc(pageable, trangDoc -> kho.congKhai(maSanPham, trangDoc)), false);
        } catch (DataAccessException exception) {
            log.warn("Khong tai duoc danh gia san pham {}: {}", maSanPham, exception.getMessage());
            return new TrangDanhGiaSanPham(TomTatDanhGia.RONG, Page.empty(pageable), true);
        }
    }

    /** Sao trung bình + số đánh giá cho thẻ sản phẩm, một truy vấn cho cả trang; lỗi thì thẻ không hiện sao. */
    public TomTatTheSanPham tomTatDanhGia(Collection<Long> maSanPham) {
        List<Long> ma = maSanPham.stream().filter(Objects::nonNull).distinct().toList();
        if (ma.isEmpty()) {
            return new TomTatTheSanPham(Map.of(), false);
        }
        try {
            return new TomTatTheSanPham(kho.tomTatNhieu(ma, null), false);
        } catch (DataAccessException exception) {
            log.warn("Khong tai duoc tom tat danh gia cho the san pham: {}", exception.getMessage());
            return new TomTatTheSanPham(Map.of(), true);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public TrangDanhGiaAdmin danhSachDanhGiaAdmin(BoLocDanhGia boLoc, int trang) {
        var pageable = PageRequest.of(Math.clamp(trang, 0, 10_000), DANH_GIA_ADMIN_MOI_TRANG);
        List<SanPhamLoc> sanPham = danhGia.sanPhamLoc();
        try {
            Page<DanhGiaAdmin> ketQua = PhanTrang.doc(pageable, trangDoc -> kho.danhSachAdmin(boLoc, trangDoc));
            Map<Long, String> ten = danhGia.tenSanPham(ketQua.getContent().stream().map(DanhGiaAdmin::maSanPham).distinct().toList());
            return new TrangDanhGiaAdmin(ketQua.map(d -> d.voiTenSanPham(ten.getOrDefault(d.maSanPham(), "Sản phẩm #" + d.maSanPham()))),
                sanPham, false);
        } catch (DataAccessException exception) {
            log.warn("Admin: khong tai duoc danh sach danh gia: {}", exception.getMessage());
            return new TrangDanhGiaAdmin(Page.empty(pageable), sanPham, true);
        }
    }
    public List<DoiTra_Repository.YeuCau> danhSachDoiTra(Long maNguoiDung) { return doiTra.danhSach(maNguoiDung); }
    public List<YeuThich_Repository.Muc> danhSachYeuThich(long maNguoiDung) { return yeuThich.danhSach(maNguoiDung); }
    public void themYeuThich(long maNguoiDung, long maSanPham) { yeuThich.them(maNguoiDung, maSanPham); }
    public void xoaYeuThich(long maNguoiDung, long maSanPham) { yeuThich.xoa(maNguoiDung, maSanPham); }

    /** BR16 kiểm ở MariaDB trước; kho đánh giá lỗi thì vẫn hiện form nhưng báo không lưu được. */
    public TrangDanhGia xemDanhGia(long maNguoiDung, long maMuc) {
        var muc = mucDaGiao(maNguoiDung, maMuc);
        try {
            return new TrangDanhGia(muc, kho.cuaChuMuc(maMuc, maNguoiDung).orElse(null), false);
        } catch (DataAccessException exception) {
            log.warn("Khong tai duoc danh gia cua dong don {}: {}", maMuc, exception.getMessage());
            return new TrangDanhGia(muc, null, true);
        }
    }

    /**
     * Thứ tự: kiểm điều kiện ở MariaDB (chủ đơn, DELIVERED) -> ghi kho đánh giá (upsert theo dòng đơn).
     * Kho lỗi kết nối -> {@link DataAccessException} cho controller báo lỗi thân thiện; chỉ một lệnh ghi nên không có ghi nửa vời.
     */
    public long luuDanhGia(long maNguoiDung, long maMuc, DanhGia_Form form, boolean sua) {
        var muc = mucDaGiao(maNguoiDung, maMuc);
        int soSao = form.getSoSao() == null ? 0 : form.getSoSao();
        if (soSao < 1 || soSao > 5) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5.");
        }
        String noiDung = form.getNoiDung() == null ? "" : form.getNoiDung().strip();
        if (noiDung.length() > DanhGia_Document.NOI_DUNG_TOI_DA) {
            throw new IllegalArgumentException("Nội dung tối đa 1.000 ký tự.");
        }
        if (sua) {
            if (!kho.sua(maMuc, maNguoiDung, soSao, noiDung)) {
                throw khongTimThay();
            }
        } else {
            try {
                kho.tao(new DanhGiaMoi(maMuc, muc.maDon(), maNguoiDung, muc.maSanPham(), muc.maBienThe(), muc.tenHienThi(),
                    soSao, noiDung));
            } catch (DuplicateKeyException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mục đã được đánh giá.");
            }
        }
        return muc.maDon();
    }

    /** Duyệt cần đúng phiên bản Admin đã xem; ẩn thì không. */
    @PreAuthorize("hasRole('ADMIN')")
    public void datHienThi(String maDanhGia, boolean hienThi, String phienBan) {
        if (hienThi && (phienBan == null || !phienBan.matches("[A-Za-z0-9]{1,64}"))) {
            throw new IllegalStateException("Vui lòng tải lại đánh giá để duyệt.");
        }
        boolean thanhCong;
        try {
            thanhCong = kho.datHienThi(maDanhGia, hienThi, phienBan);
        } catch (DataAccessException exception) {
            log.warn("Admin: khong cap nhat duoc hien thi danh gia {}: {}", maDanhGia, exception.getMessage());
            throw new IllegalStateException("Tạm thời không kết nối được kho đánh giá. Vui lòng thử lại sau.");
        }
        if (!thanhCong) {
            if (hienThi) {
                throw new IllegalStateException("Đánh giá đã thay đổi. Vui lòng kiểm tra lại.");
            }
            throw khongTimThay();
        }
    }

    private MucMua mucDaGiao(long maNguoiDung, long maMuc) {
        var muc = danhGia.mucMua(maMuc, maNguoiDung).orElseThrow(SauMua_Service::khongTimThay);
        if (!"DELIVERED".equals(muc.trangThai())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ đánh giá đơn đã giao.");
        }
        return muc;
    }

    public TrangDoiTra xemDon(long maNguoiDung, long maDon) {
        var don = doiTra.timDon(maDon, maNguoiDung, false).orElseThrow(SauMua_Service::khongTimThay);
        return new TrangDoiTra(don, don.conHan() ? doiTra.cacMuc(maDon) : List.of());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long yeuCau(long maNguoiDung, long maDon, DoiTra_Form form) {
        var don = doiTra.timDon(maDon, maNguoiDung, true).orElseThrow(SauMua_Service::khongTimThay);
        if (!don.conHan()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn chỉ được yêu cầu đổi trả trong 7 ngày sau giao.");
        }
        Map<Long, Integer> conLai = new LinkedHashMap<>();
        doiTra.cacMuc(maDon).forEach(muc -> conLai.put(muc.maMuc(), muc.conLai()));
        Map<Long, Integer> daChon = new LinkedHashMap<>();
        for (var muc : form.getSoLuong().entrySet()) {
            Integer khaDung = conLai.get(muc.getKey());
            int soLuong = muc.getValue() == null ? 0 : muc.getValue();
            if (khaDung == null || soLuong < 0 || soLuong > khaDung) {
                throw new IllegalArgumentException("Số lượng hoặc mặt hàng đổi trả không hợp lệ.");
            }
            if (soLuong > 0) {
                daChon.put(muc.getKey(), soLuong);
            }
        }
        if (daChon.isEmpty()) {
            throw new IllegalArgumentException("Chọn ít nhất một mặt hàng để đổi trả.");
        }
        long maYeuCau = doiTra.tao(maDon, maNguoiDung, form.getLyDo());
        daChon.forEach((maMuc, soLuong) -> doiTra.themMuc(maYeuCau, maMuc, soLuong));
        return maYeuCau;
    }

    public ChiTietDoiTra chiTiet(long maYeuCau, Long maNguoiDung) {
        var chiTiet = doiTra.chiTiet(maYeuCau, maNguoiDung).orElseThrow(SauMua_Service::khongTimThay);
        return new ChiTietDoiTra(chiTiet, doiTra.mucDaYeuCau(maYeuCau));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void chuyenDoiTra(long maYeuCau, ReturnStatus dich) {
        var hienTai = doiTra.chiTiet(maYeuCau, null).orElseThrow(SauMua_Service::khongTimThay).yeuCau().trangThai();
        if (!hienTai.buocTiepTheo().contains(dich) || !doiTra.chuyen(maYeuCau, hienTai, dich)) {
            throw new IllegalStateException("Không thể chuyển yêu cầu sang trạng thái đã chọn.");
        }
    }

    private static ResponseStatusException khongTimThay() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public record TrangDanhGia(MucMua muc, DanhGiaCuaKhach hienTai, boolean khongTaiDuoc) {}
    public record TrangDanhGiaSanPham(TomTatDanhGia tomTat, Page<DanhGiaCongKhai> danhSach, boolean khongTaiDuoc) {}
    public record TomTatTheSanPham(Map<Long, TomTatDanhGia> theoSanPham, boolean khongTaiDuoc) {}
    public record TrangDanhGiaAdmin(Page<DanhGiaAdmin> danhSach, List<SanPhamLoc> sanPham, boolean khongTaiDuoc) {}
    public record TrangDoiTra(Don don, List<Muc> muc) {}
    public record ChiTietDoiTra(ChiTiet chiTiet, List<MucTra> muc) {}
}
