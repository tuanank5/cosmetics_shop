package com.beautyshop.controller.admin;

import com.beautyshop.dto.PhanTrang;
import com.beautyshop.dto.BoLocDanhSachAdmin;
import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.controller.NguonDieuHuong.NguonDanhSachAdmin;
import java.util.Map;
import com.beautyshop.domain.catalog.NhomDanhMucSanPhamThuongHieu;
import com.beautyshop.dto.DanhMucSanPhamThuongHieu_Form;
import com.beautyshop.service.DanhMucSanPhamThuongHieu_Service;
import com.beautyshop.service.NgungHoatDongCatalog_Service.ConDonDangXuLy;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/catalog")
class AdminDanhMucSanPhamThuongHieu_Controller {
    private static final String ROOT = "/admin/catalog/";
    private final DanhMucSanPhamThuongHieu_Service danhMucSanPhamThuongHieuService;

    AdminDanhMucSanPhamThuongHieu_Controller(DanhMucSanPhamThuongHieu_Service danhMucSanPhamThuongHieuService) {
        this.danhMucSanPhamThuongHieuService = danhMucSanPhamThuongHieuService;
    }

    @GetMapping
    String index() {
        return "redirect:" + ROOT + NhomDanhMucSanPhamThuongHieu.DANH_MUC.path;
    }

    @GetMapping("/{section}")
    String list(@PathVariable String section, @RequestParam(defaultValue = "") String q,
                @RequestParam(required = false) Boolean active, @RequestParam(required = false) Long category,
                @RequestParam(required = false) Long brand, @RequestParam(defaultValue = "0") int page, Model model) {
        NhomDanhMucSanPhamThuongHieu nhomDaChon = NhomDanhMucSanPhamThuongHieu.from(section);
        prepare(model, nhomDaChon);
        boolean laSanPham = nhomDaChon == NhomDanhMucSanPhamThuongHieu.SAN_PHAM;
        var loc = new BoLocDanhSachAdmin(q, active, laSanPham ? category : null, laSanPham ? brand : null);
        var trang = danhMucSanPhamThuongHieuService.docTrang(nhomDaChon, loc, PhanTrang.yeuCau(page, 15));
        model.addAttribute("items", trang.getContent());
        model.addAttribute("boLoc", loc);
        model.addAttribute("listSource", new NguonDanhSachAdmin(loc.thamSo(), trang.getNumber()));
        model.addAttribute("pager", PhanTrang.tu(trang, so -> NguonDieuHuong.trangDanhSach(ROOT + section, loc.thamSo(), so)));
        if (laSanPham) {
            model.addAttribute("categories", danhMucSanPhamThuongHieuService.list(NhomDanhMucSanPhamThuongHieu.DANH_MUC));
            model.addAttribute("brands", danhMucSanPhamThuongHieuService.list(NhomDanhMucSanPhamThuongHieu.THUONG_HIEU));
        }
        model.addAttribute("donDangXuLy", danhMucSanPhamThuongHieuService.demDonDangXuLy(nhomDaChon));
        return "admin/catalog/list";
    }

    @GetMapping({"/{section}/new", "/{section}/{id}/edit"})
    String edit(@PathVariable String section, @PathVariable(required = false) Long id,
                @RequestParam Map<String, String> thamSo, Model model) {
        NhomDanhMucSanPhamThuongHieu nhomDaChon = NhomDanhMucSanPhamThuongHieu.from(section);
        var form = id == null ? new DanhMucSanPhamThuongHieu_Form() : danhMucSanPhamThuongHieuService.edit(nhomDaChon, id);
        model.addAttribute("form", form);
        if (id != null) themCanhBao(model, nhomDaChon, id, form.isActive());
        return formView(model, nhomDaChon, id, NguonDieuHuong.nguonDanhSachAdmin(thamSo));
    }

    @PostMapping({"/{section}/new", "/{section}/{id}/edit"})
    String save(@PathVariable String section, @PathVariable(required = false) Long id,
                @Valid @ModelAttribute("form") DanhMucSanPhamThuongHieu_Form form, BindingResult errors,
                @RequestParam Map<String, String> thamSo, Model model, RedirectAttributes flash) {
        NhomDanhMucSanPhamThuongHieu nhomDaChon = NhomDanhMucSanPhamThuongHieu.from(section);
        if (!errors.hasErrors()) {
            try {
                danhMucSanPhamThuongHieuService.save(nhomDaChon, id, form, errors);
            } catch (ConDonDangXuLy exception) {
                errors.reject("catalog.active.orders", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                errors.reject("catalog.integrity", "Không thể lưu. Hãy kiểm tra tên dùng trong đường dẫn có bị trùng, hoặc danh mục và thương hiệu có vừa thay đổi hay không.");
            }
        }
        if (errors.hasErrors()) {
            if (id != null) themCanhBao(model, nhomDaChon, id, danhMucSanPhamThuongHieuService.edit(nhomDaChon, id).isActive());
            return formView(model, nhomDaChon, id, NguonDieuHuong.nguonDanhSachAdmin(thamSo));
        }
        flash.addFlashAttribute("message", "Đã lưu.");
        return "redirect:" + NguonDieuHuong.nguonDanhSachAdmin(thamSo).quayLai(ROOT + nhomDaChon.path, id);
    }

    @PostMapping("/{section}/{id}/status")
    String trangThai(@PathVariable String section, @PathVariable long id, @RequestParam boolean active,
                     @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        NhomDanhMucSanPhamThuongHieu nhomDaChon = NhomDanhMucSanPhamThuongHieu.from(section);
        try {
            danhMucSanPhamThuongHieuService.datTrangThai(nhomDaChon, id, active);
            flash.addFlashAttribute("message", active ? "Đã kích hoạt lại." : "Đã ngừng hoạt động.");
        } catch (ConDonDangXuLy exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:" + NguonDieuHuong.nguonDanhSachAdmin(thamSo).quayLai(ROOT + nhomDaChon.path, id);
    }

    @PostMapping("/products/{id}/delete")
    String ngungHienThiSanPham(@PathVariable long id, RedirectAttributes flash) {
        return trangThai(NhomDanhMucSanPhamThuongHieu.SAN_PHAM.path, id, false, Map.of(), flash);
    }

    private String formView(Model model, NhomDanhMucSanPhamThuongHieu nhomDaChon, Long id, NguonDanhSachAdmin nguon) {
        model.addAttribute("backToCatalog", nguon.quayLai(ROOT + nhomDaChon.path, id));
        model.addAttribute("listSourceFields", nguon.truongAn());
        prepare(model, nhomDaChon);
        model.addAttribute("action", ROOT + nhomDaChon.path + (id == null ? "/new" : "/" + id + "/edit"));
        model.addAttribute("heading", id == null ? "Thêm mới" : "Chỉnh sửa");
        if (nhomDaChon == NhomDanhMucSanPhamThuongHieu.SAN_PHAM) {
            model.addAttribute("categories", danhMucSanPhamThuongHieuService.list(NhomDanhMucSanPhamThuongHieu.DANH_MUC));
            model.addAttribute("brands", danhMucSanPhamThuongHieuService.list(NhomDanhMucSanPhamThuongHieu.THUONG_HIEU));
        }
        return "admin/catalog/form";
    }

    private void themCanhBao(Model model, NhomDanhMucSanPhamThuongHieu nhom, long id, boolean hoatDong) {
        if (hoatDong) model.addAttribute("soDonDangXuLy", danhMucSanPhamThuongHieuService.demDonDangXuLy(nhom, id));
    }

    private void prepare(Model model, NhomDanhMucSanPhamThuongHieu nhomDaChon) {
        model.addAttribute("section", nhomDaChon.path);
        model.addAttribute("label", nhomDaChon.label);
        model.addAttribute("product", nhomDaChon == NhomDanhMucSanPhamThuongHieu.SAN_PHAM);
    }
}
