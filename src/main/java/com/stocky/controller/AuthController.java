package com.stocky.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam String username, @RequestParam String password) {
        // Mock de inicio de sesión para que el frontend pueda pasar a la siguiente pantalla
        Map<String, String> response = new HashMap<>();
        response.put("access_token", "fake-jwt-token-for-testing");
        response.put("token_type", "bearer");
        return ResponseEntity.ok(response);
    }
}
