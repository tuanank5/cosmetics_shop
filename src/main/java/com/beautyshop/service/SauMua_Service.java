package com.beautyshop.service;

import com.beautyshop.dto.DanhGia_Form;
import com.beautyshop.dto.DoiTra_Form;
import com.beautyshop.order.ReturnStatus;
import com.beautyshop.repository.DanhGia_Repository;
import com.beautyshop.repository.DanhGia_Repository.DanhGia;
import com.beautyshop.repository.DanhGia_Repository.MucMua;
import com.beautyshop.repository.DoiTra_Repository;
import com.beautyshop.repository.DoiTra_Repository.ChiTiet;
import com.beautyshop.repository.DoiTra_Repository.Don;
import com.beautyshop.repository.DoiTra_Repository.Muc;
import com.beautyshop.repository.DoiTra_Repository.MucTra;
import com.beautyshop.repository.YeuThich_Repository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SauMua_Service {
    private final DanhGia_Repository danhGia;
    private final DoiTra_Repository doiTra;
    private final YeuThich_Repository yeuThich;

    SauMua_Service(DanhGia_Repository danhGia, DoiTra_Repository doiTra, YeuThich_Repository yeuThich) {
        this.danhGia = danhGia;
        this.doiTra = doiTra;
        this.yeuThich = yeuThich;
    }

    public List<DanhGia> danhGiaCongKhai(long maSanPham) { return danhGia.congKhai(maSanPham); }
    public List<DanhGia_Repository.DanhGiaAdmin> danhSachDanhGiaAdmin() { return danhGia.danhSachAdmin(); }
    public List<DoiTra_Repository.YeuCau> danhSachDoiTra(Long maNguoiDung) { return doiTra.danhSach(maNguoiDung); }
    public List<YeuThich_Repository.Muc> danhSachYeuThich(long maNguoiDung) { return yeuThich.danhSach(maNguoiDung); }
    public void themYeuThich(long maNguoiDung, long maSanPham) { yeuThich.them(maNguoiDung, maSanPham); }
    public void xoaYeuThich(long maNguoiDung, long maSanPham) { yeuThich.xoa(maNguoiDung, maSanPham); }

    public TrangDanhGia xemDanhGia(long maNguoiDung, long maMuc) {
        return new TrangDanhGia(mucDaGiao(maNguoiDung, maMuc), danhGia.cuaChuMuc(maMuc, maNguoiDung).orElse(null));
    }

    @Transactional
    public long luuDanhGia(long maNguoiDung, long maMuc, DanhGia_Form form, boolean sua) {
        var muc = mucDaGiao(maNguoiDung, maMuc);
        if (sua) {
            var hienTai = danhGia.cuaChuMuc(maMuc, maNguoiDung).orElseThrow(SauMua_Service::khongTimThay);
            danhGia.sua(hienTai.ma(), maNguoiDung, form.getSoSao(), form.getNoiDung());
        } else {
            try {
                danhGia.tao(maMuc, maNguoiDung, muc.maSanPham(), form.getSoSao(), form.getNoiDung());
            } catch (DuplicateKeyException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mục đã được đánh giá.");
            }
        }
        return muc.maDon();
    }

    public void datHienThi(long maDanhGia, boolean hienThi, String phienBan) {
        if (hienThi && (phienBan == null || !phienBan.matches("[a-fA-F0-9]{64}"))) {
            throw new IllegalStateException("Vui lòng tải lại đánh giá để duyệt.");
        }
        if (!danhGia.datHienThi(maDanhGia, hienThi, phienBan)) {
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
    public void chuyenDoiTra(long maYeuCau, ReturnStatus dich) {
        var hienTai = doiTra.chiTiet(maYeuCau, null).orElseThrow(SauMua_Service::khongTimThay).yeuCau().trangThai();
        if (!hienTai.buocTiepTheo().contains(dich) || !doiTra.chuyen(maYeuCau, hienTai, dich)) {
            throw new IllegalStateException("Không thể chuyển yêu cầu sang trạng thái đã chọn.");
        }
    }

    private static ResponseStatusException khongTimThay() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public record TrangDanhGia(MucMua muc, DanhGia hienTai) {}
    public record TrangDoiTra(Don don, List<Muc> muc) {}
    public record ChiTietDoiTra(ChiTiet chiTiet, List<MucTra> muc) {}
}
