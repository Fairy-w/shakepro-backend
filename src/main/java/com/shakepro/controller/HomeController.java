package com.shakepro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/index", "/index.html", "/home"})
    public String home() {
        return "redirect:/swagger-ui.html";
    }
}
