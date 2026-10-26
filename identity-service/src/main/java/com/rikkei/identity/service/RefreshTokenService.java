package com.rikkei.identity.service;

import com.rikkei.identity.config.AppProperties;
import com.rikkei.identity.dto.TokenResponseDTO;
import com.rikkei.identity.entity.RefreshToken;
import com.rikkei.identity.entity.User;
import com.rikkei.identity.exception.RefreshTokenException;
import com.rikkei.identity.repository.RefreshTokenRepository;
import com.rikkei.identity.repository.UserRepository;
import com.rikkei.identity.util.JwtUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final JwtUtils jwtUtils;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               AppProperties appProperties,
                               JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public RefreshToken createRefreshToken(String userId) {
        refreshTokenRepository.deleteByUser_Id(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                Instant.now().plusMillis(appProperties.getRefreshTokenExpiration()),
                user
        );
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public TokenResponseDTO refreshToken(String requestToken) {
        // 1. Tim RefreshToken trong DB
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new RefreshTokenException("Refresh token not found in database"));

        // 2. Kiem tra han su dung
        verifyExpiration(refreshToken);

        // 3. (Token Rotation) Xoa token cu khoi DB
        refreshTokenRepository.delete(refreshToken);

        // 4. Sinh Access Token moi
        User user = refreshToken.getUser();
        String newAccessToken = jwtUtils.generateAccessToken(user);

        // 5. Sinh Refresh Token moi va luu vao DB
        RefreshToken newRefreshToken = createRefreshToken(user.getId());

        // 6. Tra ve cap token moi
        return new TokenResponseDTO(newAccessToken, newRefreshToken.getToken());
    }
}
