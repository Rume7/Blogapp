package com.codehacks.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Mock
    private UserService userService;
    
    @InjectMocks
    private UserController userController;

    private UserResponse sampleResponse;
    private UserRequest sampleRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampleResponse = new UserResponse(1L, "user1", "user1@email.com", LocalDateTime.now());
        sampleRequest = new UserRequest("user1", "user1@email.com", "pass");
    }

    @Test
    void getAllUsers_shouldReturnList() {
        when(userService.getAllUsers()).thenReturn(List.of(sampleResponse));

        List<UserResponse> result = userController.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("user1");
    }

    @Test
    void getUserById_shouldReturnUserIfFound() {
        when(userService.getUserById(1L)).thenReturn(Optional.of(sampleResponse));

        ResponseEntity<UserResponse> result = userController.getUserById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(result.getBody()).username()).isEqualTo("user1");
    }

    @Test
    void getUserById_shouldReturnNotFoundIfMissing() {
        when(userService.getUserById(1L)).thenReturn(Optional.empty());

        ResponseEntity<UserResponse> result = userController.getUserById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createUser_shouldReturnCreatedUser() {
        when(userService.createUser(any(UserRequest.class))).thenReturn(sampleResponse);

        UserResponse result = userController.createUser(sampleRequest);

        assertThat(result.username()).isEqualTo("user1");
    }

    @Test
    void updateUser_shouldReturnUpdatedUserIfFound() {
        when(userService.updateUser(eq(1L), any(UserRequest.class))).thenReturn(Optional.of(sampleResponse));

        ResponseEntity<UserResponse> result = userController.updateUser(1L, sampleRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(result.getBody()).username()).isEqualTo("user1");
    }

    @Test
    void updateUser_shouldReturnNotFoundIfMissing() {
        when(userService.updateUser(eq(1L), any(UserRequest.class))).thenReturn(Optional.empty());

        ResponseEntity<UserResponse> result = userController.updateUser(1L, sampleRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteUser_shouldReturnNoContent() {
        ResponseEntity<Void> result = userController.deleteUser(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteUser(1L);
    }

    @Test
    void getProfile_shouldReturnProfileIfAuthenticated() {
        UserDetails userDetails = mock(UserDetails.class);

        when(userDetails.getUsername()).thenReturn("user1");
        when(userService.getByUsername("user1")).thenReturn(Optional.of(sampleResponse));

        ResponseEntity<UserResponse> result = userController.getProfile(userDetails);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(result.getBody()).username()).isEqualTo("user1");
    }

    @Test
    void getProfile_shouldReturnUnauthorizedIfNoPrincipal() {
        ResponseEntity<UserResponse> result = userController.getProfile(null);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateProfile_shouldReturnProfileIfAuthenticated() {
        UserDetails userDetails = mock(UserDetails.class);

        when(userDetails.getUsername()).thenReturn("user1");
        when(userService.updateByUsername(eq("user1"), any(UserRequest.class))).thenReturn(Optional.of(sampleResponse));

        ResponseEntity<UserResponse> result = userController.updateProfile(userDetails, sampleRequest);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(result.getBody()).username()).isEqualTo("user1");
    }

    @Test
    void updateProfile_shouldReturnUnauthorizedIfNoPrincipal() {
        ResponseEntity<UserResponse> result = userController.updateProfile(null, sampleRequest);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
} 