package com.krag.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/v1")
public class HelloController {

    @GetMapping(path = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> hello() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Hello, KRAG!");
        resp.put("version", "0.1.0-SNAPSHOT");
        return resp;
    }
}