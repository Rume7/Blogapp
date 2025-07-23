package com.codehacks.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createUser_shouldEncodePasswordAndSaveUser() {
        UserRequest request = new UserRequest("user1", "user1@email.com", "pass");
        User user = new User();
        user.setUsername("user1");
        user.setEmail("user1@email.com");
        user.setPassword("encoded");
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.createUser(request);

        assertThat(response.username()).isEqualTo("user1");
        assertThat(response.email()).isEqualTo("user1@email.com");
        verify(passwordEncoder).encode("pass");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowIfUsernameExists() {
        UserRequest request = new UserRequest("user1", "user1@email.com", "pass");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new User()));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertThat(ex.getMessage()).isEqualTo("Username already exists");
    }

    @Test
    void createUser_shouldThrowIfEmailExists() {
        UserRequest request = new UserRequest("user1", "user1@email.com", "pass");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user1@email.com")).thenReturn(Optional.of(new User()));
        Exception ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertThat(ex.getMessage()).isEqualTo("Email already exists");
    }

    @Test
    void getAllUsers_shouldReturnMappedResponses() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        user.setEmail("user1@email.com");
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> users = userService.getAllUsers();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).username()).isEqualTo("user1");
    }

    @Test
    void getUserById_shouldReturnUserResponseIfFound() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<UserResponse> result = userService.getUserById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("user1");
    }

    @Test
    void getUserById_shouldReturnEmptyIfNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<UserResponse> result = userService.getUserById(1L);
        assertThat(result).isEmpty();
    }

    @Test
    void updateUser_shouldUpdateAndReturnUserResponse() {
        User user = new User();
        user.setId(1L);
        user.setUsername("old");
        user.setEmail("old@email.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserRequest req = new UserRequest("new", "new@email.com", "newpass");
        Optional<UserResponse> result = userService.updateUser(1L, req);

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("new");
        verify(passwordEncoder).encode("newpass");
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_shouldReturnEmptyIfNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        UserRequest req = new UserRequest("new", "new@email.com", "newpass");
        Optional<UserResponse> result = userService.updateUser(1L, req);
        assertThat(result).isEmpty();
    }

    @Test
    void deleteUser_shouldCallRepository() {
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }
} 