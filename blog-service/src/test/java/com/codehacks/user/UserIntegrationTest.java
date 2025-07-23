package com.codehacks.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
class UserIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8-alpine")
            .withDatabaseName("userTestDB")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_andFindById() {
        UserRequest req = new UserRequest("integration", "integration@email.com", "password");
        UserResponse created = userService.createUser(req);
        Optional<UserResponse> found = userService.getUserById(created.id());
        
        assertThat(found).isPresent();
        assertThat(found.get().username()).isEqualTo("integration");
    }

    @Test
    void updateUser_andDeleteUser() {
        UserRequest req = new UserRequest("toUpdate", "toUpdate@email.com", "password");
        UserResponse created = userService.createUser(req);

        UserRequest updateReq = new UserRequest("updated", "updated@email.com", "newpass");
        Optional<UserResponse> updated = userService.updateUser(created.id(), updateReq);
        
        assertThat(updated).isPresent();
        assertThat(updated.get().username()).isEqualTo("updated");
        
        userService.deleteUser(created.id());
        
        assertThat(userService.getUserById(created.id())).isEmpty();
    }

    @Test
    void duplicateUsernameOrEmail_shouldThrow() {
        UserRequest req = new UserRequest("dupe", "dupe@email.com", "password");
        userService.createUser(req);
        
        // Duplicate username
        UserRequest dupeUsername = new UserRequest("dupe", "other@email.com", "password");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> userService.createUser(dupeUsername));
        
        // Duplicate email
        UserRequest dupeEmail = new UserRequest("other", "dupe@email.com", "password");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> userService.createUser(dupeEmail));
    }
} 