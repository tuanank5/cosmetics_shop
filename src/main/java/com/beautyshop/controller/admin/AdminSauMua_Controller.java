package com.beautyshop.controller.admin;

import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.controller.NguonDieuHuong.NguonDanhSachAdmin;
import com.beautyshop.dto.PhanTrang;
import java.util.LinkedHashMap;
import java.util.Map;
import com.beautyshop.order.ReturnStatus;
import com.beautyshop.dto.DoiTra_Form;
import com.beautyshop.service.KiemDinhDoiTra_Service;
import com.beautyshop.service.KiemDinhDoiTra_Service.Lenh;
import com.beautyshop.service.KiemDinhDoiTra_Service.Kiem;
import java.util.UUID;
import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.service.SauMua_Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class AdminSauMua_Controller {
    private static final String DANH_GIA = "/admin/reviews";
    private static final String DANH_GIA_VUA_XU_LY = "danhGiaVuaXuLy";

    private final SauMua_Service sauMua;

    private final KiemDinhDoiTra_Service kiemDinh;

    AdminSauMua_Controller(SauMua_Service sauMua, KiemDinhDoiTra_Service kiemDinh) {
        this.sauMua = sauMua;
        this.kiemDinh = kiemDinh;
    }

    @GetMapping("/admin/reviews")
    String danhGia(@RequestParam(required = false) String status, @RequestParam(required = false) Integer stars,
                   @RequestParam(required = false) Long product, @RequestParam(defaultValue = "0") int page,
                   @RequestParam(required = false) String focus, Model model, RedirectAttributes redirect) {
        String maFocus = focus != null && focus.matches("[0-9a-fA-F]{24}") ? focus : null;
        var trangThai = DanhGia_Store.TrangThaiLoc.tu(status);
        Integer soSao = stars != null && stars >= 1 && stars <= 5 ? stars : null;
        Long sanPham = product != null && product > 0 ? product : null;
        var trang = sauMua.danhSachDanhGiaAdmin(new DanhGia_Store.BoLocDanhGia(trangThai, soSao, sanPham), page);
        if (!trang.khongTaiDuoc()) {
            var boLoc = new LinkedHashMap<String, String>();
            boLoc.put("status", trangThai.name());
            if (soSao != null) boLoc.put("stars", soSao.toString());
            if (sanPham != null) boLoc.put("product", sanPham.toString());
            if (page != trang.danhSach().getNumber()) {
                boLoc.forEach(redirect::addAttribute);
                redirect.addAttribute("page", trang.danhSach().getNumber());
                if (maFocus != null) redirect.addAttribute("focus", maFocus);
                for (String ten : java.util.List.of("message", "error", DANH_GIA_VUA_XU_LY)) {
                    if (model.containsAttribute(ten)) redirect.addFlashAttribute(ten, model.getAttribute(ten));
                }
                return "redirect:" + DANH_GIA;
            }
            model.addAttribute("pager", PhanTrang.tu(trang.danhSach(),
                so -> NguonDieuHuong.trangDanhSach(DANH_GIA, boLoc, so)));
        }
        Object vuaXuLy = model.getAttribute(DANH_GIA_VUA_XU_LY);
        if (vuaXuLy != null) {
            boolean conTrongTrang = !trang.khongTaiDuoc()
                && trang.danhSach().stream().anyMatch(danhGia -> danhGia.ma().equals(vuaXuLy));
            model.addAttribute("focusReviewId", conTrongTrang ? vuaXuLy : null);
            model.addAttribute("focusReviewResults", !conTrongTrang);
        }
        if (maFocus != null) {
            boolean conTrongTrang = !trang.khongTaiDuoc()
                && trang.danhSach().stream().anyMatch(danhGia -> danhGia.ma().equals(maFocus));
            model.addAttribute("focusReviewOrder", conTrongTrang ? maFocus : null);
            model.addAttribute("focusReviewResults", !conTrongTrang);
        }
        model.addAttribute("trang", trang);
        model.addAttribute("locTrangThai", trangThai.name());
        model.addAttribute("locSao", soSao);
        model.addAttribute("locSanPham", sanPham);
        return "admin/user/reviews";
    }

    @PostMapping("/admin/reviews/{id}/visibility")
    String hienThi(@PathVariable String id, @RequestParam boolean visible,
                   @RequestParam(required = false) String revision, @RequestParam(required = false) String status,
                   @RequestParam(required = false) String stars, @RequestParam(required = false) String product,
                   @RequestParam(required = false) String page, RedirectAttributes flash) {
        try {
            sauMua.datHienThi(id, visible, revision);
            flash.addFlashAttribute("message", visible ? "Đã duyệt đánh giá." : "Đã ẩn đánh giá.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        flash.addFlashAttribute(DANH_GIA_VUA_XU_LY, id);
        var trangThai = DanhGia_Store.TrangThaiLoc.tu(status);
        if (trangThai != DanhGia_Store.TrangThaiLoc.TAT_CA || "TAT_CA".equalsIgnoreCase(status)) {
            flash.addAttribute("status", trangThai.name());
        }
        giuThamSoSo(flash, "stars", stars, 1, 5);
        giuThamSoSo(flash, "product", product, 1, Long.MAX_VALUE);
        giuThamSoSo(flash, "page", page, 0, 10_000);
        return "redirect:" + DANH_GIA;
    }

    private static void giuThamSoSo(RedirectAttributes redirect, String ten, String giaTri, long min, long max) {
        if (giaTri == null || !giaTri.matches("[0-9]{1,19}")) return;
        try {
            long so = Long.parseLong(giaTri);
            if (so >= min && so <= max) redirect.addAttribute(ten, so);
        } catch (NumberFormatException ignored) {
            // Bỏ tham số phân trang/lọc vượt miền biểu diễn.
        }
    }

    @GetMapping("/admin/returns")
    String yeuCau(@RequestParam(defaultValue = "0") int page, Model model) {
        var trang = sauMua.danhSachDoiTra(null, PhanTrang.yeuCau(page, 15));
        model.addAttribute("yeuCau", trang.getContent());
        model.addAttribute("listSource", new NguonDanhSachAdmin(Map.of(), trang.getNumber()));
        model.addAttribute("pager", PhanTrang.tu(trang, so -> NguonDieuHuong.trangDanhSach("/admin/returns", Map.of(), so)));
        model.addAttribute("base", "/admin/returns");
        return "return/list";
    }

    @GetMapping("/admin/returns/{id}")
    String chiTiet(@PathVariable long id, @RequestParam Map<String, String> thamSo, Model model) {
        model.addAttribute("trang", sauMua.chiTiet(id, null));
        model.addAttribute("kienLech", kiemDinh.kienLech(id));
        model.addAttribute("dongNhan", kiemDinh.dongNhan(id));
        model.addAttribute("maThaoTac", UUID.randomUUID());
        model.addAttribute("base", "/admin/returns");
        var ky = NguonDieuHuong.kyDashboard(thamSo);
        model.addAttribute("navDashboard", ky);
        model.addAttribute("returnBack", ky.isEmpty() ? null : NguonDieuHuong.veDashboard(ky));
        var nguon = nguonDoiTra(thamSo);
        model.addAttribute("listSourceFields", nguon.truongAn());
        model.addAttribute("backToReturns", nguon.quayLai("/admin/returns", id));
        return "return/detail";
    }

    @PostMapping("/admin/returns/{id}/status")
    String trangThai(@PathVariable long id, @RequestParam ReturnStatus status, @RequestParam UUID thaoTac,
                     @RequestParam long phienBan, @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        return xuLy(id, thamSo, flash, () -> kiemDinh.duyet(new Lenh(id, thaoTac, phienBan), status));
    }

    @InitBinder("dongTra")
    void bindDongTra(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("soLuong[*]");
    }

    @PostMapping("/admin/returns/{id}/receive")
    String nhan(@PathVariable long id, @RequestParam UUID thaoTac, @RequestParam long phienBan,
                @RequestParam String thamChieu, @RequestParam(defaultValue="0") int lech,
                @RequestParam(required=false) KiemDinhDoiTra_Service.LyDoLech lyDoLech, @ModelAttribute("dongTra") DoiTra_Form form,
                @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        return xuLy(id, thamSo, flash, () -> kiemDinh.nhan(new Lenh(id, thaoTac, phienBan), thamChieu, form.getSoLuong(), lech, lyDoLech));
    }

    @PostMapping("/admin/returns/{id}/reconcile-parcel")
    String doiChieu(@PathVariable long id, @RequestParam UUID thaoTac, @RequestParam long phienBan,
                    @RequestParam long kien, @RequestParam String chungTu, @ModelAttribute("dongTra") DoiTra_Form form,
                    @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        return xuLy(id, thamSo, flash, () -> kiemDinh.doiChieu(new Lenh(id,thaoTac,phienBan),kien,chungTu,form.getSoLuong()));
    }

    @PostMapping("/admin/returns/{id}/inspect")
    String kiem(@PathVariable long id, @RequestParam UUID thaoTac, @RequestParam long phienBan,
                @RequestParam long dong, @RequestParam int duBan, @RequestParam int daMo,
                @RequestParam int hong, @RequestParam int khac, @RequestParam(defaultValue="") String lyDo,
                @RequestParam(defaultValue="false") boolean datChecklist,
                @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        return xuLy(id, thamSo, flash, () -> kiemDinh.kiem(new Lenh(id, thaoTac, phienBan), dong,
            new Kiem(duBan, daMo, hong, khac, lyDo.strip(), datChecklist)));
    }

    @PostMapping("/admin/returns/{id}/confirm")
    String chot(@PathVariable long id, @RequestParam UUID thaoTac, @RequestParam long phienBan,
                @RequestParam long kien, @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        return xuLy(id, thamSo, flash, () -> kiemDinh.xacNhan(new Lenh(id, thaoTac, phienBan), kien));
    }

    @PostMapping("/admin/returns/{id}/close")
    String dong(@PathVariable long id, @RequestParam UUID thaoTac, @RequestParam long phienBan,
                @RequestParam String nghiaVu, @RequestParam(defaultValue="") String thieu,
                @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        return xuLy(id, thamSo, flash, () -> kiemDinh.dong(new Lenh(id, thaoTac, phienBan), nghiaVu, thieu));
    }

    private String xuLy(long id, Map<String, String> thamSo, RedirectAttributes flash, Runnable thaoTac) {
        try {
            thaoTac.run();
            flash.addFlashAttribute("message", "Đã ghi nhận thao tác đổi trả.");
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            flash.addFlashAttribute("error", "Mã kiện hoặc thao tác đã được ghi. Hãy đối chiếu hồ sơ.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        NguonDieuHuong.kyDashboard(thamSo).forEach(flash::addAttribute);
        nguonDoiTra(thamSo).truongAn().forEach(flash::addAttribute);
        flash.addFlashAttribute("focusReturnUpdate", true);
        return "redirect:/admin/returns/" + id;
    }

    private static NguonDanhSachAdmin nguonDoiTra(Map<String, String> request) {
        return new NguonDanhSachAdmin(Map.of(), NguonDieuHuong.trangNguon(request, "listPage"));
    }
}
