package com.beautyshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.http.HttpStatus;

@Controller
public class TuVanDa_Controller {
    @GetMapping("/tu-van-da")
    String hienThi() {
        return "redirect:/chat";
    }

    @PostMapping("/tu-van-da")
    RedirectView chuyenSangChat() {
        var dich = new RedirectView("/chat", true);
        dich.setStatusCode(HttpStatus.SEE_OTHER);
        dich.setExposeModelAttributes(false);
        return dich;
    }
}
