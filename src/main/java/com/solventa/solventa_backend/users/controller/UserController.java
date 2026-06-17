package com.solventa.solventa_backend.users.controller;

import com.solventa.solventa_backend.shared.dto.ApiResponse;
import com.solventa.solventa_backend.tenant.dto.CompanyResponse;
import com.solventa.solventa_backend.tenant.dto.UpdateCompanyRequest;
import com.solventa.solventa_backend.users.dto.*;
import com.solventa.solventa_backend.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Perfil, empresa y gestión de equipo")
public class UserController {

    private final UserService userService;

    // ── Perfil ─────────────────────────────────────────────────────────────────
    @GetMapping("/users/me")
    @Operation(summary = "Obtener perfil del usuario en sesión")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyProfile()));
    }

    @PutMapping("/users/me")
    @Operation(summary = "Actualizar nombre del perfil")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(
                ApiResponse.ok("Perfil actualizado", userService.updateMyProfile(req)));
    }

    @PutMapping("/users/me/password")
    @Operation(summary = "Cambiar contraseña")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(req);
        return ResponseEntity.ok(
                ApiResponse.ok("Contraseña actualizada correctamente", null));
    }

    // ── Empresa ────────────────────────────────────────────────────────────────
    @GetMapping("/company")
    @Operation(summary = "Obtener datos de la empresa")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompany() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getCompany()));
    }

    @PutMapping("/company")
    @Operation(summary = "Actualizar datos de la empresa")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @Valid @RequestBody UpdateCompanyRequest req) {
        return ResponseEntity.ok(
                ApiResponse.ok("Empresa actualizada", userService.updateCompany(req)));
    }

    // ── Equipo ─────────────────────────────────────────────────────────────────
    @GetMapping("/users")
    @Operation(summary = "Listar equipo del tenant")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeam() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getTeam()));
    }

    @PostMapping("/users/invite")
    @Operation(
            summary = "Invitar usuario al equipo",
            description = "Crea el usuario con contraseña temporal. " +
                    "En producción se enviará por email — por ahora se imprime en logs."
    )
    public ResponseEntity<ApiResponse<TeamMemberResponse>> invite(
            @Valid @RequestBody InviteUserRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario invitado", userService.inviteUser(req)));
    }

    @PatchMapping("/users/{id}/deactivate")
    @Operation(summary = "Desactivar usuario del equipo")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> deactivate(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Usuario desactivado", userService.deactivateUser(id)));
    }

    @PatchMapping("/users/{id}/activate")
    @Operation(summary = "Reactivar usuario del equipo")
    public ResponseEntity<ApiResponse<TeamMemberResponse>> activate(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Usuario activado", userService.activateUser(id)));
    }
}