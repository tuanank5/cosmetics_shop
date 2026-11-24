package com.beautyshop.controller;

import com.beautyshop.dto.TuVan_Form;
import com.beautyshop.service.TuVan_Service;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.Serializable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

@Controller
@RequestMapping("/chat")
class TuVan_Controller {
    private static final String SESSION_KEY = TuVan_Controller.class.getName() + ".rate";
    private final TuVan_Service tuVanService;
    private CuaSo toanUngDung = new CuaSo(0, 0);

    TuVan_Controller(TuVan_Service tuVanService) { this.tuVanService = tuVanService; }

    @GetMapping
    String chat(Model model) {
        model.addAttribute("form", new TuVan_Form());
        return "chat/index";
    }

    @PostMapping
    String hoi(@Valid @ModelAttribute("form") TuVan_Form form, BindingResult errors,
               HttpSession session, HttpServletResponse response, Model model) {
        if (!errors.hasErrors()) {
            if (choPhep(session)) {
                model.addAttribute("traLoi", tuVanService.hoi(form.getCauHoi().strip()));
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                errors.reject("chat.rate", "Bạn vừa gửi nhiều câu hỏi. Vui lòng thử lại sau một phút.");
            }
        }
        return "chat/index";
    }

    private boolean choPhep(HttpSession session) {
        long bayGio = System.currentTimeMillis();
        synchronized (WebUtils.getSessionMutex(session)) {
            var cucBo = session.getAttribute(SESSION_KEY) instanceof CuaSo cuaSo && bayGio < cuaSo.hetLuc()
                ? cuaSo : new CuaSo(bayGio + 60_000, 0);
            if (cucBo.soLuong() >= 6 || !choPhepToanUngDung(bayGio)) {
                return false;
            }
            session.setAttribute(SESSION_KEY, new CuaSo(cucBo.hetLuc(), cucBo.soLuong() + 1));
            return true;
        }
    }

    private synchronized boolean choPhepToanUngDung(long bayGio) {
        if (bayGio >= toanUngDung.hetLuc()) {
            toanUngDung = new CuaSo(bayGio + 60_000, 0);
        }
        if (toanUngDung.soLuong() >= 30) {
            return false;
        }
        toanUngDung = new CuaSo(toanUngDung.hetLuc(), toanUngDung.soLuong() + 1);
        return true;
    }

    private record CuaSo(long hetLuc, int soLuong) implements Serializable {}
}
