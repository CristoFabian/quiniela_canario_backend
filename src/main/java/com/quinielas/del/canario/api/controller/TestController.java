package com.quinielas.del.canario.api.controller;

import com.quinielas.del.canario.api.service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/privado")
    public ResponseEntity<Map<String, String>> privado(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(testService.getPrivado(user));
    }

    @GetMapping("/publico")
    public ResponseEntity<Map<String, String>> publico() {
        return ResponseEntity.ok(testService.getPublico());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(testService.getHealth());
    }
}
