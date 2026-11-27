package com.beautyshop.controller;

import com.beautyshop.dto.PhanTrang;
import com.beautyshop.cart.ThongBaoGopGio;
import com.beautyshop.dto.DanhGia_Form;
import com.beautyshop.model.DanhGia_Document;
import com.beautyshop.dto.DoiTra_Form;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.SauMua_Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
class SauMua_Controller {
    private final SauMua_Service sauMua;

    SauMua_Controller(SauMua_Service sauMua) { this.sauMua = sauMua; }

    @InitBinder("doiTra")
    void bindDoiTra(WebDataBinder binder) {
        binder.setAllowedFields("lyDo", "soLuong[*]");
    }

    @GetMapping("/orders/items/{id}/review")
    String danhGia(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        var trang = sauMua.xemDanhGia(phien.getMaNguoiDung(), id);
        var form = new DanhGia_Form();
        if (trang.hienTai() != null) {
            form.setSoSao(trang.hienTai().soSao());
            form.setNoiDung(trang.hienTai().noiDung());
        }
        model.addAttribute("form", form);
        return hienDanhGia(id, trang, model);
    }

    @PostMapping({"/orders/items/{id}/review", "/orders/items/{id}/review/{action:edit}"})
    String luuDanhGia(@PathVariable long id, @PathVariable(required = false) String action,
                       @AuthenticationPrincipal PhienNguoiDung phien,
                       @Valid @ModelAttribute("form") DanhGia_Form form, BindingResult errors,
                       Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                long maDon = sauMua.luuDanhGia(phien.getMaNguoiDung(), id, form, action != null);
                flash.addFlashAttribute("message", "Đã gửi đánh giá để duyệt.");
                return "redirect:/orders/" + maDon;
            } catch (IllegalArgumentException exception) {
                errors.rejectValue("noiDung", "review.invalid", exception.getMessage());
            } catch (DuplicateKeyException exception) {
                throw exception;
            } catch (DataAccessException exception) {
                // Kho đánh giá (MongoDB) không phản hồi: giữ nguyên form để khách gửi lại sau.
                model.addAttribute("loiLuu", SauMua_Service.LOI_KHO_DANH_GIA);
            }
        }
        return hienDanhGia(id, sauMua.xemDanhGia(phien.getMaNguoiDung(), id), model);
    }

    private String hienDanhGia(long id, SauMua_Service.TrangDanhGia trang, Model model) {
        model.addAttribute("trang", trang);
        model.addAttribute("action", "/orders/items/" + id + "/review" + (trang.hienTai() == null ? "" : "/edit"));
        model.addAttribute("noiDungToiDa", DanhGia_Document.NOI_DUNG_TOI_DA);
        return "order/review";
    }

    @GetMapping("/orders/{id}/returns/new")
    String taoDoiTra(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        model.addAttribute("doiTra", new DoiTra_Form());
        return hienDoiTra(id, phien, model);
    }

    @PostMapping("/orders/{id}/returns/new")
    String taoDoiTra(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien,
                      @Valid @ModelAttribute("doiTra") DoiTra_Form form, BindingResult errors,
                      Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                long ma = sauMua.yeuCau(phien.getMaNguoiDung(), id, form);
                flash.addFlashAttribute("message", "Đã gửi yêu cầu đổi trả.");
                return "redirect:/account/returns/" + ma;
            } catch (IllegalArgumentException exception) {
                errors.reject("return.quantities", exception.getMessage());
            }
        }
        return hienDoiTra(id, phien, model);
    }

    private String hienDoiTra(long id, PhienNguoiDung phien, Model model) {
        model.addAttribute("trang", sauMua.xemDon(phien.getMaNguoiDung(), id));
        return "return/form";
    }

    @GetMapping("/account/returns")
    String yeuCau(@AuthenticationPrincipal PhienNguoiDung phien, @RequestParam(defaultValue = "0") int page, Model model) {
        var trang = sauMua.danhSachDoiTra(phien.getMaNguoiDung(), PhanTrang.yeuCau(page, 10));
        model.addAttribute("yeuCau", trang.getContent());
        model.addAttribute("pager", PhanTrang.tu(trang, so -> NguonDieuHuong.trangDanhSach("/account/returns", Map.of(), so)));
        model.addAttribute("base", "/account/returns");
        return "return/list";
    }

    @GetMapping("/account/returns/{id}")
    String chiTiet(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien,
                   @RequestParam Map<String, String> thamSo, Model model) {
        model.addAttribute("trang", sauMua.chiTiet(id, phien.getMaNguoiDung()));
        model.addAttribute("base", "/account/returns");
        model.addAttribute("backToCustomerReturns",
            NguonDieuHuong.veDanhSachKhach("/account/returns", thamSo, "customer-return", id));
        return "return/detail";
    }

    @GetMapping("/account/wishlist")
    String yeuThich(@AuthenticationPrincipal PhienNguoiDung phien, @RequestParam(defaultValue = "0") int page,
                    Model model, RedirectAttributes redirect) {
        var trang = sauMua.danhSachYeuThich(phien.getMaNguoiDung(), PhanTrang.yeuCau(page, 12));
        if (page != trang.getNumber()) {
            redirect.addAttribute("page", trang.getNumber());
            return "redirect:/account/wishlist";
        }
        model.addAttribute("yeuThich", trang.getContent());
        model.addAttribute("wishlistFields", page == 0 ? Map.of() : Map.of("wishlistPage", Integer.toString(page)));
        model.addAttribute("pager", PhanTrang.tu(trang, so -> NguonDieuHuong.trangDanhSach("/account/wishlist", Map.of(), so)));
        return "auth/wishlist";
    }

    @GetMapping("/account/wishlist/{id}/source")
    String nguonYeuThich(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien,
                         @RequestParam Map<String, String> nguon, HttpServletRequest request) {
        var hienTai = sauMua.trangThaiYeuThich(phien.getMaNguoiDung(), id);
        String dich = NguonDieuHuong.veYeuThich(nguon, id, hienTai.dangBan() ? hienTai.slug() : null);
        return "redirect:" + ThongBaoGopGio.chuyenDich(request, phien, dich);
    }

    @GetMapping(path = "/account/wishlist/state", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<String>> trangThaiYeuThich(@RequestParam(required = false) List<Long> ids,
                                                    @AuthenticationPrincipal PhienNguoiDung phien) {
        if (ids != null && (ids.size() > 48 || ids.stream().anyMatch(id -> id == null || id <= 0))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
            sauMua.daYeuThich(phien.getMaNguoiDung(), ids).stream().map(String::valueOf).toList());
    }

    @PostMapping(path = {"/account/wishlist/{id}", "/account/wishlist/{id}/{action:delete}"}, headers = "!X-Wishlist-Partial")
    String datYeuThich(@PathVariable long id, @PathVariable(required = false) String action,
                       @AuthenticationPrincipal PhienNguoiDung phien, @RequestParam Map<String, String> nguon,
                       RedirectAttributes flash) {
        var hienTai = sauMua.datYeuThich(phien.getMaNguoiDung(), id, action == null);
        if (action == null && (!hienTai.dangBan() || !hienTai.daLuu())) flash.addFlashAttribute("wishlistError", LOI_YEU_THICH);
        return "redirect:" + NguonDieuHuong.veYeuThich(nguon, id, hienTai.dangBan() ? hienTai.slug() : null);
    }

    @PostMapping(path = {"/account/wishlist/{id}", "/account/wishlist/{id}/{action:delete}"},
        headers = "X-Wishlist-Partial=1", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<KetQuaYeuThich> datYeuThichTaiCho(@PathVariable long id, @PathVariable(required = false) String action,
                                                     @AuthenticationPrincipal PhienNguoiDung phien) {
        var hienTai = sauMua.datYeuThich(phien.getMaNguoiDung(), id, action == null);
        boolean biTuChoi = action == null && (!hienTai.dangBan() || !hienTai.daLuu());
        return ResponseEntity.status(biTuChoi ? HttpStatus.CONFLICT : HttpStatus.OK).cacheControl(CacheControl.noStore())
            .body(new KetQuaYeuThich(hienTai.daLuu(), biTuChoi ? LOI_YEU_THICH : null));
    }

    private static final String LOI_YEU_THICH = "Sản phẩm hiện không còn hiển thị. Không thể lưu yêu thích.";
    record KetQuaYeuThich(boolean saved, String message) {}
}
