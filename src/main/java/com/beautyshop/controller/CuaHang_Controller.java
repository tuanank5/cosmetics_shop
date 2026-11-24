package com.beautyshop.controller;

import com.beautyshop.service.CuaHang_Service;
import com.beautyshop.service.SauMua_Service;
import org.springframework.data.domain.PageRequest;
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
            @RequestParam(defaultValue = "12") int size, Model model) {
        q = gioiHan(q, 160);
        brand = gioiHan(brand, 190);
        category = gioiHan(category, 190);
        skinType = gioiHan(skinType, 100);
        String thuTu = switch (sort) {
            case "priceAsc", "priceDesc" -> sort;
            default -> "newest";
        };
        int kichThuoc = Math.clamp(size, 1, 48);
        var pageable = PageRequest.of(Math.clamp(page, 0, (Integer.MAX_VALUE - 48) / kichThuoc), kichThuoc);
        var ketQua = cuaHangService.timSanPham(q, brand, category, skinType, thuTu, pageable);
        if (pageable.getPageNumber() > 0 && pageable.getPageNumber() >= ketQua.getTotalPages()) {
            ketQua = cuaHangService.timSanPham(q, brand, category, skinType, thuTu,
                pageable.withPage(Math.max(0, ketQua.getTotalPages() - 1)));
        }
        model.addAttribute("ketQua", ketQua);
        model.addAttribute("thuongHieu", cuaHangService.thuongHieu());
        model.addAttribute("danhMuc", cuaHangService.danhMuc());
        model.addAttribute("q", q);
        model.addAttribute("brand", brand);
        model.addAttribute("category", category);
        model.addAttribute("skinType", skinType);
        model.addAttribute("sort", thuTu);
        return "product/list";
    }

    @GetMapping("/{slug}")
    String chiTiet(@PathVariable String slug, @RequestParam(required = false) Long variant, Model model) {
        var chiTiet = cuaHangService.chiTiet(slug, variant);
        model.addAttribute("chiTiet", chiTiet);
        model.addAttribute("danhGia", sauMua.danhGiaCongKhai(chiTiet.sanPham().getMa()));
        return "product/detail";
    }

    private String gioiHan(String giaTri, int toiDa) {
        String daCat = giaTri.strip();
        return daCat.codePointCount(0, daCat.length()) <= toiDa ? daCat
            : daCat.substring(0, daCat.offsetByCodePoints(0, toiDa));
    }
}
