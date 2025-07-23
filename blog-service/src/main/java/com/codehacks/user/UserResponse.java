package com.codehacks.user;

import java.time.LocalDateTime;

record UserResponse(Long id, String username, String email, LocalDateTime createdAt) {
    public static UserResponse fromUser(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
} 