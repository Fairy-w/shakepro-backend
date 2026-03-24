package com.shakepro.service.impl;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.JwtUtil;
import com.shakepro.dto.request.LoginRequest;
import com.shakepro.dto.request.RegisterRequest;
import com.shakepro.dto.response.LoginResponse;
import com.shakepro.dto.response.UserResponse;
import com.shakepro.entity.User;
import com.shakepro.entity.UserRole;
import com.shakepro.repository.UserRepository;
import com.shakepro.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .role(UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered: id={}, username={}", user.getId(), user.getUsername());
        return UserResponse.from(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        log.info("User logged in: id={}, username={}", user.getId(), user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expireSeconds(jwtUtil.getExpirationSeconds())
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        return UserResponse.from(user);
    }
}
