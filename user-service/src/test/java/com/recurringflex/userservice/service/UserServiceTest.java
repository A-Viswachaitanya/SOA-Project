package com.recurringflex.userservice.service;

import com.recurringflex.userservice.dto.AuthRequest;
import com.recurringflex.userservice.dto.AuthResponse;
import com.recurringflex.userservice.dto.RegisterRequest;
import com.recurringflex.userservice.dto.UpdateUserRequest;
import com.recurringflex.userservice.dto.UserResponse;
import com.recurringflex.userservice.entity.User;
import com.recurringflex.userservice.exception.InvalidCredentialsException;
import com.recurringflex.userservice.exception.ResourceNotFoundException;
import com.recurringflex.userservice.repository.UserRepository;
import com.recurringflex.userservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPassword("encodedPassword123");
        sampleUser.setRole("USER");
        sampleUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Register: successfully registers a new user")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register: throws exception if username already exists")
    void testRegister_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.register(request));
        assertTrue(ex.getMessage().contains("Username already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register: throws exception if email already exists")
    void testRegister_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.register(request));
        assertTrue(ex.getMessage().contains("Email already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login: successfully authenticates and returns JWT token")
    void testLogin_Success() {
        AuthRequest request = new AuthRequest("testuser", "password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(true);
        when(jwtUtil.generateToken("1", "testuser", "USER")).thenReturn("mock.jwt.token");

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("USER", response.getRole());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    @DisplayName("Login: throws exception for wrong password")
    void testLogin_WrongPassword_ThrowsException() {
        AuthRequest request = new AuthRequest("testuser", "wrongpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword123")).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    @DisplayName("Login: throws exception if user not found")
    void testLogin_UserNotFound_ThrowsException() {
        AuthRequest request = new AuthRequest("unknownuser", "password123");

        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        // Same message as wrong password, so usernames cannot be enumerated
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    @DisplayName("Get User by ID: returns user response when found")
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    @DisplayName("Register: always assigns USER role (no self-assigned ADMIN)")
    void testRegister_AlwaysAssignsUserRole() {
        RegisterRequest request = new RegisterRequest("attacker", "attacker@example.com", "password123");

        when(userRepository.existsByUsername("attacker")).thenReturn(false);
        when(userRepository.existsByEmail("attacker@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("USER", captor.getValue().getRole());
    }

    @Test
    @DisplayName("Get User by ID: throws ResourceNotFoundException when missing")
    void testGetUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    @DisplayName("Get All Users: returns every user")
    void testGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponse> users = userService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).getUsername());
    }

    @Test
    @DisplayName("Update User: changes email, re-hashes password and sets role")
    void testUpdateUser_Success() {
        UpdateUserRequest request = new UpdateUserRequest("new@example.com", "newPassword123", "admin");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNew");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.updateUser(1L, request);

        assertEquals("new@example.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        assertEquals("encodedNew", sampleUser.getPassword());
    }

    @Test
    @DisplayName("Update User: rejects an email that belongs to another user")
    void testUpdateUser_DuplicateEmail_ThrowsException() {
        UpdateUserRequest request = new UpdateUserRequest("taken@example.com", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.updateUser(1L, request));
        assertTrue(ex.getMessage().contains("Email already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete User: removes an existing user")
    void testDeleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        userService.deleteUser(1L);

        verify(userRepository).delete(sampleUser);
    }

    @Test
    @DisplayName("Delete User: throws ResourceNotFoundException when missing")
    void testDeleteUser_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).delete(any(User.class));
    }
}
