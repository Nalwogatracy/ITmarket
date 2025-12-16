
package com.IT_market.controller;

// In your controller package
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {
    
    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
      
    @GetMapping("/")
    public String home() {
        return "index"; // This looks for src/main/resources/templates/index.html
    }
}
