package com.dolcecandela.auth.dto;

import com.dolcecandela.user.Role;
import com.dolcecandela.user.User;

public record UserResponse(Long id, String fullName, String email, String phone, Role role, String preferredLanguage) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getPreferredLanguage());
    }
}
