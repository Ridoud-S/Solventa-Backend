package com.solventa.solventa_backend.auth.controller;

import com.solventa.solventa_backend.auth.dto.*;
import com.solventa.solventa_backend.auth.service.AuthService;
import com.solventa.solventa_backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro, login y gestión de sesión")
@SecurityRequirements   // ← este tag indica que auth NO requiere JWT
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Registrar nueva empresa",
            description = "Crea un tenant nuevo con su usuario administrador. " +
                    "Retorna tokens JWT listos para usar."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Empresa registrada exitosamente",
                        authService.register(req)));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario y retorna accessToken (8h) " +
                    "y refreshToken (30 días)."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req)));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar access token",
            description = "Usa el refreshToken para obtener un nuevo accessToken " +
                    "sin necesidad de volver a hacer login."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(req)));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Solicitar reset de contraseña",
            description = "Envía un email con enlace de recuperación. " +
                    "Siempre responde 200 para no revelar si el email existe."
    )
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(
                ApiResponse.ok("Si el correo existe recibirás un enlace en breve",
                        null));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Restablecer contraseña",
            description = "Recibe el token del email y la nueva contraseña. " +
                    "El token expira en 30 minutos."
    )
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(
                ApiResponse.ok("Contraseña actualizada correctamente", null));
    }
}