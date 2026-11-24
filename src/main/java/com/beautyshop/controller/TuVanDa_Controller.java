package com.beautyshop.controller;

import com.beautyshop.ai.dto.TuVanDaRequest;
import com.beautyshop.ai.service.TuVanDaService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
@RequestMapping("/tu-van-da")
public class TuVanDa_Controller {

    private static final Logger LOGGER =
            Logger.getLogger(TuVanDa_Controller.class.getName());

    private final TuVanDaService tuVanDaService;

    public TuVanDa_Controller(TuVanDaService tuVanDaService) {
        this.tuVanDaService = tuVanDaService;
    }

    @GetMapping
    public String hienThi(Model model) {
        model.addAttribute("form",
                new TuVanDaRequest("", "", "", ""));

        return "chat/TuVanDa";
    }

    @PostMapping
    public String tuVan(
            @Valid @ModelAttribute("form") TuVanDaRequest form,
            BindingResult errors,
            Model model) {

        if (errors.hasErrors()) {
            return "chat/TuVanDa";
        }

        model.addAttribute("daGui", true);
        model.addAttribute("noiDungDaGui", form.vanDeDa());

        try {
            String ketQua = tuVanDaService.tuVan(form);

            model.addAttribute("traLoi", ketQua);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Lỗi khi thực hiện AI tư vấn da", e);

            model.addAttribute("loi",
                    "AI hiện chưa thể phản hồi. Vui lòng kiểm tra Ollama và thử lại.");
        }

        return "chat/TuVanDa";
    }
}
