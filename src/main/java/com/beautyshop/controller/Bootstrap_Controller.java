package com.beautyshop.controller;

import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.service.CuaHang_Service;
import com.beautyshop.service.TrangChu_Service;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.GoiYSanPham_Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang chủ sáu chương và trang đăng nhập. Mọi dữ liệu trang chủ chỉ đọc; chương nào lỗi thì hiện bản tĩnh
 * tối giản, trang chủ không bao giờ sập vì catalog.
 */
@Controller
public class Bootstrap_Controller {
    static final int SO_SAN_PHAM_NOI_BAT = 4;
    /** P21: khu "Được yêu thích nhất" và "Tất cả sản phẩm" dưới chương 6. */
    static final int SO_SAN_PHAM_PHO_BIEN = 5;
    static final int SO_SAN_PHAM_TAT_CA = 12;
    static final List<String> SAP_XEP = List.of("newest", "priceAsc", "priceDesc");
    private static final Logger log = LoggerFactory.getLogger(Bootstrap_Controller.class);

    private final ObjectProvider<CuaHang_Service> cuaHangService;
    private final ObjectProvider<TrangChu_Service> trangChuService;
    private final ObjectProvider<GoiYSanPham_Service> goiYService;

    public Bootstrap_Controller(ObjectProvider<CuaHang_Service> cuaHangService,
            ObjectProvider<TrangChu_Service> trangChuService, ObjectProvider<GoiYSanPham_Service> goiYService) {
        this.cuaHangService = cuaHangService;
        this.trangChuService = trangChuService;
        this.goiYService = goiYService;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "sort", required = false) String sort,
                       @AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        model.addAttribute("loaiDa", doc(trangChuService, "loai da", TrangChu_Service::loaiDa, List.of()));
        model.addAttribute("nghiThuc", doc(trangChuService, "nghi thuc", TrangChu_Service::nghiThuc,
            TrangChu_Service.nghiThucTinh()));
        model.addAttribute("hoatChat", doc(trangChuService, "hoat chat", TrangChu_Service::hoatChat, List.of()));
        List<CuaHang_Repository.TheSanPham> noiBat = doc(cuaHangService, "san pham noi bat",
            service -> service.sanPhamNoiBat(SO_SAN_PHAM_NOI_BAT), List.of());
        model.addAttribute("sanPhamNoiBat", noiBat);
        Long maNguoiDung = phien == null ? null : phien.getMaNguoiDung();
        model.addAttribute("sanPhamPhoBien", doc(goiYService, "duoc yeu thich nhat",
            service -> service.goiY(maNguoiDung, SO_SAN_PHAM_PHO_BIEN), List.<CuaHang_Repository.TheSanPham>of()));
        String sapXep = sort != null && SAP_XEP.contains(sort) ? sort : "newest";
        model.addAttribute("sapXepTrangChu", sapXep);
        model.addAttribute("tatCaSanPham", doc(cuaHangService, "tat ca san pham",
            service -> {
                var trang = service.timSanPham("", "", "", "", sapXep, PageRequest.of(0, SO_SAN_PHAM_TAT_CA));
                return trang == null ? List.<CuaHang_Repository.TheSanPham>of() : trang.getContent();
            },
            List.<CuaHang_Repository.TheSanPham>of()));
        return "home/index";
    }

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    private static <S, T> T doc(ObjectProvider<S> provider, String chuong, Function<S, T> truyVan, T macDinh) {
        S service = provider.getIfAvailable();
        if (service == null) {
            return macDinh;
        }
        try {
            T ketQua = truyVan.apply(service);
            return ketQua == null || ketQua instanceof Collection<?> danhSach && danhSach.isEmpty() ? macDinh : ketQua;
        } catch (DataAccessException exception) {
            log.warn("Trang chu: khong doc duoc du lieu chuong {}; hien ban tinh.", chuong, exception);
            return macDinh;
        }
    }
}
