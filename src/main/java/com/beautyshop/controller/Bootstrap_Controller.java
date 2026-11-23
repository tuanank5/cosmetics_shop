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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang chủ sáu chương và trang đăng nhập. Mọi dữ liệu trang chủ chỉ đọc; chương nào lỗi thì hiện bản tĩnh
 * tối giản, trang chủ không bao giờ sập vì catalog.
 */
@Controller
public class Bootstrap_Controller {
    static final int SO_SAN_PHAM_NOI_BAT = 4;
    private static final Logger log = LoggerFactory.getLogger(Bootstrap_Controller.class);

    private final ObjectProvider<CuaHang_Service> cuaHangService;
    private final ObjectProvider<TrangChu_Service> trangChuService;

    public Bootstrap_Controller(ObjectProvider<CuaHang_Service> cuaHangService,
            ObjectProvider<TrangChu_Service> trangChuService) {
        this.cuaHangService = cuaHangService;
        this.trangChuService = trangChuService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("loaiDa", doc(trangChuService, "loai da", TrangChu_Service::loaiDa, List.of()));
        model.addAttribute("nghiThuc", doc(trangChuService, "nghi thuc", TrangChu_Service::nghiThuc,
            TrangChu_Service.nghiThucTinh()));
        model.addAttribute("hoatChat", doc(trangChuService, "hoat chat", TrangChu_Service::hoatChat, List.of()));
        List<CuaHang_Repository.TheSanPham> noiBat = doc(cuaHangService, "san pham noi bat",
            service -> service.sanPhamNoiBat(SO_SAN_PHAM_NOI_BAT), List.of());
        model.addAttribute("sanPhamNoiBat", noiBat);
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
