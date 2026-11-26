package com.beautyshop.controller;

import com.beautyshop.dto.PhanTrang;
import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.CuaHang_Service;
import com.beautyshop.service.SauMua_Service;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/products")
class CuaHang_Controller {
    private final CuaHang_Service cuaHangService;
    private final SauMua_Service sauMua;

    CuaHang_Controller(CuaHang_Service cuaHangService, SauMua_Service sauMua) {
        this.cuaHangService = cuaHangService;
        this.sauMua = sauMua;
    }

    @GetMapping
    String danhSach(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String brand,
            @RequestParam(defaultValue = "") String category, @RequestParam(defaultValue = "") String skinType,
            @RequestParam(defaultValue = "newest") String sort, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size, @RequestParam(defaultValue = "") String filterPanel,
            @AuthenticationPrincipal PhienNguoiDung phien, HttpServletRequest request, Model model) {
        String tuKhoa = gioiHan(q, 160);
        String thuongHieu = gioiHan(brand, 190);
        String danhMuc = gioiHan(category, 190);
        String loaiDa = gioiHan(skinType, 100);
        String thuTu = switch (sort) {
            case "priceAsc", "priceDesc" -> sort;
            default -> "newest";
        };
        int kichThuoc = Math.clamp(size, 1, 48);
        var pageable = PageRequest.of(Math.clamp(page, 0, (Integer.MAX_VALUE - 48) / kichThuoc), kichThuoc);
        var boLoc = new LinkedHashMap<String, String>();
        boLoc.put("q", tuKhoa);
        boLoc.put("brand", thuongHieu);
        boLoc.put("category", danhMuc);
        boLoc.put("skinType", loaiDa);
        boLoc.put("sort", thuTu);
        boLoc.put("size", Integer.toString(kichThuoc));
        var ketQua = PhanTrang.doc(pageable,
            trang -> cuaHangService.timSanPham(tuKhoa, thuongHieu, danhMuc, loaiDa, thuTu, trang));
        model.addAttribute("ketQua", ketQua);
        var maSanPham = ketQua.getContent().stream().map(TheSanPham::getMaSanPham).toList();
        model.addAttribute("tomTatDanhGia", sauMua.tomTatDanhGia(maSanPham));
        if (phien != null && request.isUserInRole("CUSTOMER")) {
            model.addAttribute("wishlistSaved", sauMua.daYeuThich(phien.getMaNguoiDung(),
                maSanPham.stream().filter(java.util.Objects::nonNull).distinct().toList()));
        }
        model.addAttribute("thuongHieu", cuaHangService.thuongHieu());
        model.addAttribute("danhMuc", cuaHangService.danhMuc());
        model.addAttribute("q", tuKhoa);
        model.addAttribute("brand", thuongHieu);
        model.addAttribute("category", danhMuc);
        model.addAttribute("skinType", loaiDa);
        model.addAttribute("sort", thuTu);
        String hienBoLoc = switch (filterPanel) {
            case "open", "closed" -> filterPanel;
            default -> null;
        };
        if (hienBoLoc != null) boLoc.put("filterPanel", hienBoLoc);
        model.addAttribute("filterPanel", hienBoLoc);
        model.addAttribute("pager", PhanTrang.tu(ketQua, so -> NguonDieuHuong.trangDanhSach("/products", boLoc, so)));
        var wishlistFields = new LinkedHashMap<>(boLoc);
        wishlistFields.putIfAbsent("filterPanel", "");
        wishlistFields.put("page", Integer.toString(ketQua.getNumber()));
        model.addAttribute("wishlistFields", wishlistFields);
        model.addAttribute("navOrigin", "catalog");
        return "product/list";
    }

    @GetMapping("/{slug}")
    String chiTiet(@PathVariable String slug, @RequestParam(required = false) Long variant,
                   @RequestParam(name = "reviewPage", defaultValue = "0") int reviewPage,
                   @RequestParam Map<String, String> thamSo, @AuthenticationPrincipal PhienNguoiDung phien,
                   HttpServletRequest request, Model model) {
        var chiTiet = cuaHangService.chiTiet(slug, variant);
        model.addAttribute("chiTiet", chiTiet);
        var danhGia = sauMua.danhGiaSanPham(chiTiet.sanPham().getMa(), reviewPage);
        var nguon = NguonDieuHuong.sanPham(thamSo, chiTiet.sanPham().getMa(), request.isUserInRole("CUSTOMER"));
        model.addAttribute("danhGia", danhGia);
        model.addAttribute("navSource", nguon);
        if (phien != null && request.isUserInRole("CUSTOMER")) {
            model.addAttribute("wishlistSaved", sauMua.daYeuThich(phien.getMaNguoiDung(), java.util.List.of(chiTiet.sanPham().getMa())));
        }
        var wishlistFields = new LinkedHashMap<>(nguon.thamSo());
        if (chiTiet.dangChon().getMaBienThe() != null) {
            wishlistFields.put("variant", chiTiet.dangChon().getMaBienThe().toString());
        }
        wishlistFields.put("reviewPage", Integer.toString(danhGia.danhSach().getNumber()));
        model.addAttribute("wishlistFields", wishlistFields);
        if (!danhGia.khongTaiDuoc()) {
            model.addAttribute("pager", PhanTrang.tu(danhGia.danhSach(),
                so -> nguon.trangDanhGia(chiTiet.sanPham().getDuongDan(), chiTiet.dangChon().getMaBienThe(), so)));
        }
        return "product/detail";
    }

    private String gioiHan(String giaTri, int toiDa) {
        String daCat = giaTri.strip();
        return daCat.codePointCount(0, daCat.length()) <= toiDa ? daCat
            : daCat.substring(0, daCat.offsetByCodePoints(0, toiDa));
    }
}
