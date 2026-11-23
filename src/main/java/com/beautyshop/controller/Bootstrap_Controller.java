package com.beautyshop.controller;

import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.service.CuaHang_Service;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang chủ và trang đăng nhập. Dải sản phẩm nổi bật chỉ đọc và tự ẩn khi không có dữ liệu. */
@Controller
public class Bootstrap_Controller {
    static final int SO_SAN_PHAM_NOI_BAT = 8;

    private final ObjectProvider<CuaHang_Service> cuaHangService;

    public Bootstrap_Controller(ObjectProvider<CuaHang_Service> cuaHangService) {
        this.cuaHangService = cuaHangService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("sanPhamNoiBat", sanPhamNoiBat());
        return "home/index";
    }

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    private List<CuaHang_Repository.TheSanPham> sanPhamNoiBat() {
        var service = cuaHangService.getIfAvailable();
        if (service == null) {
            return List.of();
        }
        try {
            return service.sanPhamNoiBat(SO_SAN_PHAM_NOI_BAT);
        } catch (DataAccessException exception) {
            // Dải nổi bật là phần phụ; trang chủ vẫn phải render khi catalog tạm không đọc được.
            return List.of();
        }
    }
}
