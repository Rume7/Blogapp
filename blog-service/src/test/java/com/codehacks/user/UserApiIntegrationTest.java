package com.codehacks.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestSecurityConfig.class)
class UserApiIntegrationTest {

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
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUpUser() {
        userRepository.deleteAll();
        UserRequest req = new UserRequest("testUser", "testUser@email.com", "testPass");
        userService.createUser(req);
    }

    @Test
    void createUser_andGetUser() {
        UserRequest req = new UserRequest("apiUser", "apiUser@email.com", "password");
        ResponseEntity<UserResponse> createResp = restTemplate.postForEntity(
                "/api/v1/users", req, UserResponse.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        UserResponse created = createResp.getBody();

        assertThat(created).isNotNull();
        assertThat(created.username()).isEqualTo("apiUser");

        ResponseEntity<UserResponse> getResp = restTemplate.getForEntity(
                "/api/v1/users/" + created.id(), UserResponse.class);

        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(getResp.getBody()).username()).isEqualTo("apiUser");
    }

    @Test
    void getAllUsers_shouldReturnAllCreatedUsers() {
        // Create two users
        UserRequest req1 = new UserRequest("userOne", "userOne@email.com", "pass1");
        UserRequest req2 = new UserRequest("userTwo", "userTwo@email.com", "pass2");
        restTemplate.postForEntity("/api/v1/users", req1, UserResponse.class);
        restTemplate.postForEntity("/api/v1/users", req2, UserResponse.class);

        ResponseEntity<UserResponse[]> resp = restTemplate.getForEntity("/api/v1/users", UserResponse[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().length).isGreaterThanOrEqualTo(2);
    }

    @Test
    void updateUser_shouldUpdateDetails() {
        UserRequest createReq = new UserRequest("toUpdate", "toUpdate@email.com", "pass");
        ResponseEntity<UserResponse> createResp = restTemplate.postForEntity("/api/v1/users", createReq, UserResponse.class);
        Long id = createResp.getBody().id();

        UserRequest updateReq = new UserRequest("updatedUser", "updated@email.com", "newpass");
        restTemplate.put("/api/v1/users/" + id, updateReq);

        ResponseEntity<UserResponse> getResp = restTemplate.getForEntity("/api/v1/users/" + id, UserResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(getResp.getBody()).username()).isEqualTo("updatedUser");
        assertThat(getResp.getBody().email()).isEqualTo("updated@email.com");
    }

    @Test
    void deleteUser_shouldRemoveUser() {
        UserRequest createReq = new UserRequest("toDelete", "toDelete@email.com", "pass");
        ResponseEntity<UserResponse> createResp = restTemplate.postForEntity("/api/v1/users", createReq, UserResponse.class);
        Long id = Objects.requireNonNull(createResp.getBody()).id();

        restTemplate.delete("/api/v1/users/" + id);

        ResponseEntity<UserResponse> getResp = restTemplate.getForEntity("/api/v1/users/" + id, UserResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getUserById_shouldReturnNotFoundForMissingUser() {
        ResponseEntity<UserResponse> resp = restTemplate.getForEntity("/api/v1/users/99999", UserResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createUser_shouldFailOnDuplicateUsernameOrEmail() {
        UserRequest req = new UserRequest("dupeUser", "dupe@email.com", "pass");
        restTemplate.postForEntity("/api/v1/users", req, UserResponse.class);

        // Duplicate username
        UserRequest dupeUsername = new UserRequest("dupeUser", "other@email.com", "pass");
        ResponseEntity<String> resp1 = restTemplate.postForEntity("/api/v1/users", dupeUsername, String.class);
        assertThat(resp1.getStatusCode().is4xxClientError()).isTrue();

        // Duplicate email
        UserRequest dupeEmail = new UserRequest("otherUser", "dupe@email.com", "pass");
        ResponseEntity<String> resp2 = restTemplate.postForEntity("/api/v1/users", dupeEmail, String.class);
        assertThat(resp2.getStatusCode().is4xxClientError()).isTrue();
    }
} 