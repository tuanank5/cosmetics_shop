package com.beautyshop.controller.admin;

import com.beautyshop.service.TuVan_Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/ai")
class AdminTuVan_Controller {
    private final TuVan_Service tuVanService;

    AdminTuVan_Controller(TuVan_Service tuVanService) { this.tuVanService = tuVanService; }

    @GetMapping
    String trangThai(Model model) {
        model.addAttribute("trangThai", tuVanService.trangThai());
        return "admin/chat/index";
    }

    @PostMapping("/reindex")
    String napLai(RedirectAttributes flash) {
        try {
            tuVanService.napLai();
            flash.addFlashAttribute("message", "Đã nạp kho tri thức.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/ai";
    }
}
