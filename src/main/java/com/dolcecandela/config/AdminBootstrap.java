package com.dolcecandela.config;

import com.dolcecandela.user.Role;
import com.dolcecandela.user.User;
import com.dolcecandela.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Crée le compte administrateur au premier démarrage à partir de ADMIN_EMAIL / ADMIN_PASSWORD.
 * Aucun mot de passe n'est écrit dans le code ni dans les migrations SQL.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder, AppProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = properties.admin();
        if (admin.email() == null || admin.email().isBlank()) {
            log.info("ADMIN_EMAIL non défini : aucun compte administrateur créé automatiquement.");
            return;
        }
        String email = admin.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        if (admin.password() == null || admin.password().length() < MIN_ADMIN_PASSWORD_LENGTH) {
            log.warn("ADMIN_PASSWORD absent ou trop court (min. {} caractères) : compte admin NON créé.",
                    MIN_ADMIN_PASSWORD_LENGTH);
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(admin.fullName() == null || admin.fullName().isBlank() ? "Administrateur" : admin.fullName());
        user.setPasswordHash(passwordEncoder.encode(admin.password()));
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        log.info("Compte administrateur créé pour {}", email);
    }
}
