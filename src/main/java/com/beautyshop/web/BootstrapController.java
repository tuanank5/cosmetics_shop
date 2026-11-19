package com.beautyshop.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Scaffold navigation only; Catalog/Auth will replace these placeholder pages. */
@Controller
public class BootstrapController {
    @GetMapping("/")
    public String home() { return "home/index"; }

    @GetMapping("/products")
    public String products() { return "product/list"; }

    @GetMapping("/login")
    public String login() { return "auth/login"; }
}
