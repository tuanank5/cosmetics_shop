package com.beautyshop.controller;

import com.beautyshop.payment.MomoSignature.Ipn;
import com.beautyshop.service.Momo_Facade;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class Momo_Controller {
    private final Momo_Facade momo;
    private final ObjectReader reader;
    private long giay;
    private int soRequest;

    Momo_Controller(Momo_Facade momo, ObjectMapper mapper) {
        this.momo = momo;
        this.reader = mapper.readerFor(Ipn.class).with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    @PostMapping(path = "/payment/momo/ipn", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> ipn(HttpServletRequest request) {
        if (!momo.enabled()) {
            return ResponseEntity.notFound().build();
        }
        if (!choPhep()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "1").build();
        }
        try {
            byte[] body = request.getInputStream().readNBytes(8193);
            if (body.length > 8192) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }
            Ipn ipn = reader.readValue(body);
            if (ipn == null) {
                return ResponseEntity.badRequest().build();
            }
            momo.nhanIPN(ipn);
            return ResponseEntity.noContent().build();
        } catch (IOException | IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private synchronized boolean choPhep() {
        long hienTai = System.nanoTime() / 1_000_000_000;
        if (hienTai != giay) {
            giay = hienTai;
            soRequest = 0;
        }
        return ++soRequest <= 60;
    }

    @GetMapping("/payment/momo/return")
    String quayLai() {
        return "order/momo-return";
    }

    @GetMapping("/payment/momo/mock/{id}")
    String trangMock(@PathVariable long id, Model model) {
        model.addAttribute("lan", momo.xemMock(id));
        return "order/momo-mock";
    }

    @PostMapping("/payment/momo/mock/{id}")
    String chotMock(@PathVariable long id, @RequestParam String requestId, @RequestParam int result,
                   RedirectAttributes flash) {
        try {
            momo.moPhong(id, requestId, result);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/momo/retry")
    String thuLai(@PathVariable long id, RedirectAttributes flash) {
        try {
            var url = momo.batDau(id);
            if (url.isPresent()) {
                return "redirect:" + url.get();
            }
            flash.addFlashAttribute("error", "Chưa nhận được trang thanh toán. Bạn có thể thử lại hoặc chờ xác nhận kết quả thanh toán.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/admin/orders/{id}/momo/reconcile")
    String doiSoat(@PathVariable long id, @RequestParam java.util.Map<String, String> thamSo,
                   @RequestParam(name = "listStatus", required = false) java.util.List<com.beautyshop.order.OrderStatus> loc,
                   RedirectAttributes flash) {
        boolean coKetQua = momo.doiSoat(id);
        flash.addFlashAttribute("message", coKetQua ? "Đã kiểm tra lại thanh toán. Xem trạng thái đơn bên dưới."
            : "Chưa thể xác nhận kết quả thanh toán.");
        NguonDieuHuong.kyDashboard(thamSo).forEach(flash::addAttribute);
        NguonDieuHuong.donAdmin(thamSo, loc, true).giuQuaRedirect(flash);
        return "redirect:/admin/orders/" + id;
    }
}
