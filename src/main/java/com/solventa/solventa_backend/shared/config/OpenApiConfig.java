package com.solventa.solventa_backend.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI solventaOpenAPI() {

        // ── Esquema de seguridad JWT ────────────────────────────────────────────
        SecurityScheme jwtScheme = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Ingresa el token JWT obtenido en /api/auth/login");

        // ── Servidores por ambiente ────────────────────────────────────────────
        Server devServer = new Server()
                .url("http://localhost:8080")
                .description("Servidor de desarrollo local");

        Server prodServer = new Server()
                .url("https://api.solventaio.com")
                .description("Servidor de producción");

        List<Server> servers = activeProfile.equals("prod")
                ? List.of(prodServer, devServer)
                : List.of(devServer, prodServer);

        return new OpenAPI()
                .info(new Info()
                        .title("Solventa CRM — API REST")
                        .description("""
                                ## API REST de Solventa CRM
                                
                                Plataforma SaaS B2B para gestión de leads, clientes,
                                cotizaciones y seguimientos comerciales.
                                
                                ### Autenticación
                                Todos los endpoints (excepto `/api/auth/**`) requieren
                                un token JWT en el header: Authorization: Bearer {accessToken}
                                ### Multi-tenancy
                                El `tenant_id` se extrae automáticamente del JWT.
                                Nunca se envía manualmente en las peticiones.
                                
                                ### Formato de respuesta
                                Todas las respuestas siguen el wrapper `ApiResponse<T>`:
```json
                                {
                                  "success": true,
                                  "message": "Operación exitosa",
                                  "data": { }
                                }
```
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Solventa")
                                .email("dev@solventaio.com")
                                .url("https://solventaio.com"))
                        .license(new License()
                                .name("Privado — Uso interno")
                                .url("https://solventaio.com")))
                .servers(servers)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", jwtScheme));
    }
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("solventa-api")
                .pathsToMatch("/api/**")
                .build();
    }
}