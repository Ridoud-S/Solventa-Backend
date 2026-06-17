package com.solventa.solventa_backend.users.service;

import com.solventa.solventa_backend.shared.context.TenantContext;
import com.solventa.solventa_backend.shared.exception.BusinessException;
import com.solventa.solventa_backend.shared.util.SecurityUtils;
import com.solventa.solventa_backend.tenant.model.Company;
import com.solventa.solventa_backend.tenant.repository.CompanyRepository;
import com.solventa.solventa_backend.users.dto.*;
import com.solventa.solventa_backend.users.model.User;
import com.solventa.solventa_backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository    userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder   passwordEncoder;

    // ── Perfil propio ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        return UserProfileResponse.from(getCurrentUser());
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest req) {
        User user = getCurrentUser();
        user.setName(req.getName());
        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("Contraseña actualizada para usuario: {}", user.getId());
    }

    // ── Empresa ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public com.solventa.solventa_backend.tenant.dto.CompanyResponse getCompany() {
        UUID tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Empresa"));
        return com.solventa.solventa_backend.tenant.dto.CompanyResponse.from(company);
    }

    @Transactional
    public com.solventa.solventa_backend.tenant.dto.CompanyResponse updateCompany(
            com.solventa.solventa_backend.tenant.dto.UpdateCompanyRequest req) {

        UUID tenantId = TenantContext.getTenantId();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Empresa"));

        company.setName(req.getName());
        company.setIndustry(req.getIndustry());
        company.setRfc(req.getRfc());

        return com.solventa.solventa_backend.tenant.dto.CompanyResponse
                .from(companyRepository.save(company));
    }

    // ── Equipo ─────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeam() {
        UUID tenantId = TenantContext.getTenantId();
        return userRepository.findAllByTenantId(tenantId)
                .stream()
                .map(TeamMemberResponse::from)
                .toList();
    }

    @Transactional
    public TeamMemberResponse inviteUser(InviteUserRequest req) {
        UUID tenantId = TenantContext.getTenantId();

        if (userRepository.existsByEmailAndTenantId(req.getEmail(), tenantId)) {
            throw BusinessException.conflict(
                    "Ya existe un usuario con ese correo en tu empresa");
        }

        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> BusinessException.notFound("Empresa"));

        // Contraseña temporal — en producción se enviaría por email
        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        User newUser = User.builder()
                .tenant(company)
                .name(req.getName())
                .email(req.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(req.getRole())
                .isActive(true)
                .invitationAccepted(false)
                .build();

        User saved = userRepository.save(newUser);

        // TODO: enviar email con tempPassword cuando haya servicio de email
        log.info("Usuario invitado: {} | Tenant: {} | TempPass: {}",
                saved.getEmail(), tenantId, tempPassword);

        return TeamMemberResponse.from(saved);
    }

    @Transactional
    public TeamMemberResponse deactivateUser(UUID userId) {
        UUID tenantId = TenantContext.getTenantId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (userId.equals(currentUserId)) {
            throw BusinessException.badRequest(
                    "No puedes desactivar tu propia cuenta");
        }

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Usuario"));

        user.setActive(false);
        return TeamMemberResponse.from(userRepository.save(user));
    }

    @Transactional
    public TeamMemberResponse activateUser(UUID userId) {
        UUID tenantId = TenantContext.getTenantId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> BusinessException.notFound("Usuario"));

        user.setActive(true);
        return TeamMemberResponse.from(userRepository.save(user));
    }

    // ── Helper ─────────────────────────────────────────────────────────────────
    private User getCurrentUser() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("Usuario"));
    }
}