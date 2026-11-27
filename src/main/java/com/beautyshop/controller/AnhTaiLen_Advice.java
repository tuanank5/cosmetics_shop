package com.beautyshop.controller;

import com.beautyshop.dto.BienTheSanPham_Form;
import com.beautyshop.service.BienTheSanPham_Service;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/** Multipart parsing can fail before a controller is selected. Rebuild only the existing SKU form. */
@ControllerAdvice
public class AnhTaiLen_Advice {
    private static final Pattern SKU_PATH = Pattern.compile("/admin/catalog/products/([0-9]+)/variants/(new|([0-9]+)/edit)");
    private final ObjectProvider<BienTheSanPham_Service> bienTheService;

    public AnhTaiLen_Advice(ObjectProvider<BienTheSanPham_Service> bienTheService) {
        this.bienTheService = bienTheService;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView quaDungLuong(HttpServletRequest request) {
        var view = new ModelAndView("error/413", HttpStatus.PAYLOAD_TOO_LARGE);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        // Parsing may fail before authorization; never read Admin data without the backend role check.
        if (auth == null || auth.getAuthorities().stream().noneMatch(q -> q.getAuthority().equals("ROLE_ADMIN"))) {
            return view;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        var match = SKU_PATH.matcher(path);
        var service = bienTheService.getIfAvailable();
        if (!match.matches() || service == null) return view;
        try {
            long maSanPham = Long.parseLong(match.group(1));
            var sanPham = service.sanPham(maSanPham);
            var bienThe = match.group(3) == null ? null : service.chiTiet(maSanPham, Long.parseLong(match.group(3)));
            var form = bienThe == null ? new BienTheSanPham_Form() : BienTheSanPham_Form.from(bienThe);
            var errors = new BeanPropertyBindingResult(form, "form");
            errors.rejectValue("hinhAnh", "upload.size", "Ảnh vượt giới hạn tải lên. Chọn JPEG hoặc PNG tối đa 2 MB.");
            view.setViewName("admin/variant/form");
            view.addObject("form", form);
            view.addObject(BindingResult.MODEL_KEY_PREFIX + "form", errors);
            view.addObject("sanPham", sanPham);
            var nguon = NguonDieuHuong.nguonBienTheAdmin(docQuery(request.getQueryString()));
            Long maBienThe = bienThe == null ? null : bienThe.getMaBienThe();
            view.addObject("action", NguonDieuHuong.coThamSo(path, nguon.truongAn()));
            view.addObject("backToProducts", nguon.sanPham().quayLai("/admin/catalog/products", maSanPham));
            view.addObject("backToVariants", nguon.veDanhSach(maSanPham, maBienThe));
            view.addObject("uploadQuaLon", true);
            if (bienThe != null) view.addObject("anhHienTai", bienThe.getUrlHinhAnh());
        } catch (NumberFormatException | org.springframework.web.server.ResponseStatusException exception) {
            // A stale/invalid SKU URL still receives a safe 413 view.
        }
        return view;
    }

    private Map<String, String> docQuery(String query) {
        if (query == null) return Map.of();
        try {
            var thamSo = new LinkedHashMap<String, String>();
            UriComponentsBuilder.newInstance().query(query).build().getQueryParams()
                .toSingleValueMap().forEach((ten, value) -> {
                    if (value != null) thamSo.put(ten, UriUtils.decode(value, StandardCharsets.UTF_8));
                });
            return thamSo;
        } catch (IllegalArgumentException exception) {
            return Map.of();
        }
    }
}
