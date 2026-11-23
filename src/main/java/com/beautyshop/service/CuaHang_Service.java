package com.beautyshop.service;

import com.beautyshop.entity.BienTheSanPham;
import com.beautyshop.entity.SanPham;
import com.beautyshop.repository.BienTheSanPham_Repository;
import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.repository.DanhMuc_Repository;
import com.beautyshop.repository.ThuongHieu_Repository;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CuaHang_Service {
    private final CuaHang_Repository cuaHangRepository;
    private final BienTheSanPham_Repository bienTheRepository;
    private final DanhMuc_Repository danhMucRepository;
    private final ThuongHieu_Repository thuongHieuRepository;

    public CuaHang_Service(CuaHang_Repository cuaHangRepository, BienTheSanPham_Repository bienTheRepository,
            DanhMuc_Repository danhMucRepository, ThuongHieu_Repository thuongHieuRepository) {
        this.cuaHangRepository = cuaHangRepository;
        this.bienTheRepository = bienTheRepository;
        this.danhMucRepository = danhMucRepository;
        this.thuongHieuRepository = thuongHieuRepository;
    }

    public Page<CuaHang_Repository.TheSanPham> timSanPham(
            String q, String brand, String category, String skinType, String sort, Pageable pageable) {
        return cuaHangRepository.timSanPham(mauTimKiem(q), brand, category, mauTimKiem(skinType), sort, pageable);
    }

    public List<CuaHang_Repository.TheSanPham> sanPhamNoiBat(int soLuong) {
        return cuaHangRepository.timNoiBat(PageRequest.of(0, Math.clamp(soLuong, 1, 8)));
    }

    public List<TuyChon> danhMuc() {
        return danhMucRepository.findAllByDangHoatDongTrueOrderByTenDanhMucAsc()
            .stream().map(muc -> new TuyChon(muc.getDuongDan(), muc.getTen())).toList();
    }

    public List<TuyChon> thuongHieu() {
        return thuongHieuRepository.findAllByDangHoatDongTrueOrderByTenThuongHieuAsc()
            .stream().map(muc -> new TuyChon(muc.getDuongDan(), muc.getTen())).toList();
    }

    public ChiTiet chiTiet(String slug, Long variant) {
        SanPham sanPham = cuaHangRepository.timChiTiet(slug).orElseThrow(CuaHang_Service::khongTimThay);
        List<BienTheSanPham> bienThe = bienTheRepository
            .findAllBySanPhamMaSanPhamAndDangHoatDongTrueOrderByGiaBanAscMaBienTheAsc(sanPham.getMa());
        if (bienThe.isEmpty()) {
            throw khongTimThay();
        }
        BienTheSanPham dangChon = variant == null
            ? bienThe.stream().filter(sku -> sku.getSoLuongKhaDung() > 0).findFirst().orElse(bienThe.getFirst())
            : bienThe.stream().filter(sku -> variant.equals(sku.getMaBienThe())).findFirst()
                .orElseThrow(CuaHang_Service::khongTimThay);
        return new ChiTiet(sanPham, bienThe, dangChon);
    }

    private String mauTimKiem(String giaTri) {
        if (giaTri.isBlank()) {
            return "";
        }
        return "%" + giaTri.toLowerCase(Locale.ROOT)
            .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static ResponseStatusException khongTimThay() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public record TuyChon(String duongDan, String ten) {}
    public record ChiTiet(SanPham sanPham, List<BienTheSanPham> bienThe, BienTheSanPham dangChon) {}
}
