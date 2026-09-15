package com.exam408.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping({"/", "/login", "/register", "/practice", "/wrong", "/stats", "/pdf-viewer", "/visualization", "/overview", "/smart-exam", "/visualization/*/*"})
    public String index() {
        return "forward:/index.html";
    }
}