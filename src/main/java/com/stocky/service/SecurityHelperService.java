package com.stocky.service;

import com.stocky.model.User;
import com.stocky.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecurityHelperService {

    @Autowired
    private UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof String email) {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        }
        return null;
    }

    public User getRequiredCurrentUser() {
        User user = getCurrentUser();
        if (user == null) {
            // Fallback al primer usuario registrado (ej. administrador) para mantener compatibilidad
            return userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No user found in session or database"));
        }
        return user;
    }
}
