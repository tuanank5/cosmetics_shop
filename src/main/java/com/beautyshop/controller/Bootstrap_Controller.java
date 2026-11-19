package com.beautyshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Scaffold navigation only; Catalog/Auth will replace these placeholder pages. */
@Controller
public class Bootstrap_Controller {
    @GetMapping("/")
    public String home() { return "home/index"; }

    @GetMapping("/products")
    public String products() { return "product/list"; }

    @GetMapping("/login")
    public String login() { return "auth/login"; }
}
