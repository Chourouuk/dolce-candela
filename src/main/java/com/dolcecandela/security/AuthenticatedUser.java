package com.dolcecandela.security;

import com.dolcecandela.user.Role;

/** Utilisateur connecté, extrait du jeton JWT (principal Spring Security). */
public record AuthenticatedUser(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
