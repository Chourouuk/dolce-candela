package com.dolcecandela.auth;

import com.dolcecandela.auth.dto.AuthResponse;
import com.dolcecandela.auth.dto.LoginRequest;
import com.dolcecandela.auth.dto.RegisterRequest;
import com.dolcecandela.auth.dto.UserResponse;
import com.dolcecandela.common.ApiException;
import com.dolcecandela.security.JwtService;
import com.dolcecandela.security.LoginAttemptService;
import com.dolcecandela.user.Role;
import com.dolcecandela.user.User;
import com.dolcecandela.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    /** Hash factice : évite de révéler par le temps de réponse qu'un email n'existe pas. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email.");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        user.setPreferredLanguage(request.preferredLanguage() == null ? "fr" : request.preferredLanguage());
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        if (loginAttemptService.isBlocked(email)) {
            throw ApiException.tooManyRequests("Trop de tentatives. Réessayez dans quelques minutes.");
        }
        Optional<User> found = userRepository.findByEmailIgnoreCase(email);
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !passwordOk || !found.get().isEnabled()) {
            loginAttemptService.loginFailed(email);
            throw ApiException.unauthorized("Email ou mot de passe incorrect.");
        }
        loginAttemptService.loginSucceeded(email);
        return toAuthResponse(found.get());
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable."));
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpirationSeconds(), UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
