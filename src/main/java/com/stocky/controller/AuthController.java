package com.stocky.controller;

import com.stocky.model.User;
import com.stocky.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam String username, @RequestParam String password) {
        
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail("admin@stocky.com");
            admin.setFullName("Administrador");
            admin.setHashedPassword(passwordEncoder.encode("admin"));
            admin.setRole(com.stocky.model.UserRole.ADMIN);
            userRepository.save(admin);
        }

        // Mock de inicio de sesión para que el frontend pueda pasar a la siguiente pantalla
        Map<String, String> response = new HashMap<>();
        response.put("access_token", "fake-jwt-token-for-testing");
        response.put("token_type", "bearer");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password) {
        
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setHashedPassword(passwordEncoder.encode(password));
        user.setRole(com.stocky.model.UserRole.CASHIER);
        userRepository.save(user);

        return login(email, password);
    }
}
