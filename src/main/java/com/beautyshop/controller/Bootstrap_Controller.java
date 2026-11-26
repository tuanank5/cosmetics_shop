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
import com.beautyshop.service.SauMua_Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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
    private final ObjectProvider<SauMua_Service> sauMuaService;
    private final ObjectProvider<ClientRegistrationRepository> oauthClients;

    public Bootstrap_Controller(ObjectProvider<CuaHang_Service> cuaHangService,
            ObjectProvider<TrangChu_Service> trangChuService, ObjectProvider<GoiYSanPham_Service> goiYService,
            ObjectProvider<SauMua_Service> sauMuaService,
            ObjectProvider<ClientRegistrationRepository> oauthClients) {
        this.cuaHangService = cuaHangService;
        this.trangChuService = trangChuService;
        this.goiYService = goiYService;
        this.sauMuaService = sauMuaService;
        this.oauthClients = oauthClients;
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
        // P23: sao trung bình cho mọi thẻ trên trang chủ trong một truy vấn tóm tắt.
        java.util.Set<Long> maThe = new java.util.LinkedHashSet<>();
        for (String khu : List.of("sanPhamNoiBat", "sanPhamPhoBien", "tatCaSanPham")) {
            if (model.getAttribute(khu) instanceof Collection<?> the) {
                the.forEach(t -> { if (t instanceof CuaHang_Repository.TheSanPham sp) { maThe.add(sp.getMaSanPham()); } });
            }
        }
        model.addAttribute("tomTatDanhGia", doc(sauMuaService, "tom tat danh gia", service -> service.tomTatDanhGia(maThe),
            new SauMua_Service.TomTatTheSanPham(java.util.Map.of(), true)));
        maThe.remove(null);
        if (phien != null && phien.getAuthorities().stream().anyMatch(quyen -> quyen.getAuthority().equals("ROLE_CUSTOMER"))) {
            model.addAttribute("wishlistSaved", doc(sauMuaService, "yeu thich",
                service -> service.daYeuThich(phien.getMaNguoiDung(), maThe), java.util.Set.of()));
        }
        model.addAttribute("wishlistFields", java.util.Map.of("sort", sapXep));
        return "home/index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        var clients = oauthClients.getIfAvailable();
        model.addAttribute("googleLogin", clients != null && clients.findByRegistrationId("google") != null);
        model.addAttribute("githubLogin", clients != null && clients.findByRegistrationId("github") != null);
        return "auth/login";
    }

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
