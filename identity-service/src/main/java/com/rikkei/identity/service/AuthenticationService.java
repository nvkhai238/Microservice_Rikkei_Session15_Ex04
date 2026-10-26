package com.rikkei.identity.service;

import com.rikkei.identity.dto.LoginRequest;
import com.rikkei.identity.dto.RegisterRequest;
import com.rikkei.identity.dto.TokenResponseDTO;
import com.rikkei.identity.dto.UserResponse;
import com.rikkei.identity.entity.RefreshToken;
import com.rikkei.identity.entity.User;
import com.rikkei.identity.exception.DuplicateUsernameException;
import com.rikkei.identity.repository.UserRepository;
import com.rikkei.identity.util.JwtUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthenticationService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationService(UserRepository users,
                                  PasswordEncoder passwordEncoder,
                                  JwtUtils jwtUtils,
                                  RefreshTokenService refreshTokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
    }

    @PostConstruct
    public void seedUsers() {
        if (!users.existsByUsername("student")) {
            users.save(new User(
                    UUID.randomUUID().toString(),
                    "student",
                    passwordEncoder.encode("Student@123"),
                    "STUDENT"
            ));
        }
        if (!users.existsByUsername("instructor")) {
            users.save(new User(
                    UUID.randomUUID().toString(),
                    "instructor",
                    passwordEncoder.encode("Instructor@123"),
                    "INSTRUCTOR"
            ));
        }
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new DuplicateUsernameException();
        }
        User user = new User(
                UUID.randomUUID().toString(),
                username,
                passwordEncoder.encode(request.password()),
                "STUDENT"
        );
        return UserResponse.from(users.save(user));
    }

    @Transactional
    public TokenResponseDTO login(LoginRequest request) {
        User user = users.findByUsername(request.username().trim())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        String accessToken = jwtUtils.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return new TokenResponseDTO(accessToken, refreshToken.getToken());
    }
}
