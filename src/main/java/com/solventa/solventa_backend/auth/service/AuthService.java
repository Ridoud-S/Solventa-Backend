package com.solventa.solventa_backend.auth.service;

import com.solventa.solventa_backend.auth.dto.*;
import com.solventa.solventa_backend.auth.util.JwtUtil;
import com.solventa.solventa_backend.shared.exception.BusinessException;
import com.solventa.solventa_backend.tenant.model.Company;
import com.solventa.solventa_backend.tenant.repository.CompanyRepository;
import com.solventa.solventa_backend.users.model.Role;
import com.solventa.solventa_backend.users.model.User;
import com.solventa.solventa_backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtil           jwtUtil;

    // ── Registro ───────────────────────────────────────────────────────────────
    @Transactional
    public AuthResponse register(RegisterRequest req) {

        // Verificar si el email ya existe globalmente
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw BusinessException.conflict(
                    "Ya existe una cuenta con ese correo electrónico");
        }

        // Crear empresa (tenant)
        Company company = companyRepository.save(
                Company.builder()
                        .name(req.getCompanyName())
                        .build()
        );

        // Crear usuario administrador
        User admin = userRepository.save(
                User.builder()
                        .tenant(company)
                        .name(req.getName())
                        .email(req.getEmail().toLowerCase())
                        .passwordHash(passwordEncoder.encode(req.getPassword()))
                        .role(Role.ADMIN)
                        .isActive(true)
                        .invitationAccepted(true)
                        .build()
        );

        log.info("Nueva empresa registrada: {} | Admin: {}",
                company.getName(), admin.getEmail());

        return buildAuthResponse(admin, company);
    }

    // ── Login ──────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {

        User user = userRepository
                .findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> BusinessException.badRequest(
                        "Correo o contraseña incorrectos"));

        if (!user.isActive()) {
            throw BusinessException.forbidden("Tu cuenta está desactivada");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("Correo o contraseña incorrectos");
        }

        return buildAuthResponse(user, user.getTenant());
    }

    // ── Refresh Token ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest req) {

        String token = req.getRefreshToken();

        if (!jwtUtil.isValid(token)) {
            throw BusinessException.badRequest("Refresh token inválido o expirado");
        }

        UUID userId = jwtUtil.extractUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Usuario"));

        if (!user.isActive()) {
            throw BusinessException.forbidden("Tu cuenta está desactivada");
        }

        return buildAuthResponse(user, user.getTenant());
    }

    // ── Forgot Password ────────────────────────────────────────────────────────
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {

        // Siempre responde OK para no revelar si el email existe
        userRepository.findByEmail(req.getEmail().toLowerCase())
                .ifPresent(user -> {
                    String token = UUID.randomUUID().toString();
                    user.setResetToken(token);
                    user.setResetTokenExpiresAt(OffsetDateTime.now().plusMinutes(30));
                    userRepository.save(user);

                    // TODO: enviar email con el token
                    // emailService.sendPasswordReset(user.getEmail(), token);
                    log.info("Reset token generado para: {} | Token: {}",
                            user.getEmail(), token);
                });
    }

    // ── Reset Password ─────────────────────────────────────────────────────────
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {

        User user = userRepository
                .findByResetToken(req.getToken())
                .orElseThrow(() -> BusinessException.badRequest(
                        "Token inválido o ya utilizado"));

        if (user.getResetTokenExpiresAt() == null ||
                OffsetDateTime.now().isAfter(user.getResetTokenExpiresAt())) {
            throw BusinessException.badRequest("El token ha expirado");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Contraseña actualizada para: {}", user.getEmail());
    }

    // ── Helper privado ─────────────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user, Company company) {
        String accessToken  = jwtUtil.generateAccessToken(
                user.getId(), company.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(), company.getId(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .tenantId(company.getId())
                        .build())
                .build();
    }
}