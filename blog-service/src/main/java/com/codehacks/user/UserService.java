package com.codehacks.user;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::fromUser).toList();
    }

    public Optional<UserResponse> getUserById(Long id) {
        return userRepository.findById(id).map(UserResponse::fromUser);
    }

    public Optional<UserResponse> getByUsername(String username) {
        return userRepository.findByUsername(username).map(UserResponse::fromUser);
    }

    public UserResponse createUser(UserRequest userRequest) {
        if (userRepository.findByUsername(userRequest.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(userRequest.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(userRequest.username());
        user.setEmail(userRequest.email());
        user.setPassword(passwordEncoder.encode(userRequest.password()));

        return UserResponse.fromUser(userRepository.save(user));
    }

    public Optional<UserResponse> updateUser(Long id, UserRequest userRequest) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(userRequest.username());
            user.setEmail(userRequest.email());
            user.setPassword(passwordEncoder.encode(userRequest.password()));
            return UserResponse.fromUser(userRepository.save(user));
        });
    }

    public Optional<UserResponse> updateByUsername(String username, UserRequest userRequest) {
        return userRepository.findByUsername(username).map(user -> {
            user.setUsername(userRequest.username());
            user.setEmail(userRequest.email());
            user.setPassword(passwordEncoder.encode(userRequest.password()));
            return UserResponse.fromUser(userRepository.save(user));
        });
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
} 