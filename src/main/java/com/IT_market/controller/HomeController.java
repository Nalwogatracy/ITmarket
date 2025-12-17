package com.IT_market.controller;  // Must be sub-package of main class

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller  // NOT @RestController for HTML views
public class HomeController {
    
    @GetMapping("/")
    public String home() {
        // This returns the template name (index.html)
        return "index";
    }
}