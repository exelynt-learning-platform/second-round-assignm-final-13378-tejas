package com.example.ecommerce_backend;

import com.example.ecommerce_backend.dto.request.LoginRequest;
import com.example.ecommerce_backend.dto.request.RegisterRequest;
import com.example.ecommerce_backend.dto.response.AuthResponse;
import com.example.ecommerce_backend.entity.User;
import com.example.ecommerce_backend.exception.BadRequestException;
import com.example.ecommerce_backend.exception.UnauthorizedException;
import com.example.ecommerce_backend.repository.UserRepository;
import com.example.ecommerce_backend.security.JwtService;
import com.example.ecommerce_backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Tejas");
        registerRequest.setEmail("tejas@example.com");
        registerRequest.setPassword("password123");

        savedUser = User.builder()
                .id(1L)
                .name("Tejas")
                .email("tejas@example.com")
                .password("hashed_password")
                .role(User.Role.CUSTOMER)
                .enabled(true)
                .build();
    }

    @Test
    void register_success_returnsToken() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(registerRequest.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("tejas@example.com");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsBadRequest() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_correctCredentials_returnsToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("tejas@example.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), savedUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(savedUser.getEmail())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("tejas@example.com");
        loginRequest.setPassword("wrong-password");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nobody@example.com");
        loginRequest.setPassword("password");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_disabledAccount_throwsUnauthorized() {
        savedUser.setEnabled(false);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("tejas@example.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("disabled");
    }
}
