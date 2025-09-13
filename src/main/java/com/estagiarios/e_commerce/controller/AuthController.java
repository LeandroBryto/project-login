package com.estagiarios.e_commerce.controller;

import com.estagiarios.e_commerce.dto.*;
import com.estagiarios.e_commerce.entity.Usuario;
import com.estagiarios.e_commerce.security.JwtTokenProvider;
import com.estagiarios.e_commerce.security.UserPrincipal;
import com.estagiarios.e_commerce.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;




@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:4200}", maxAge = 3600)
@RequiredArgsConstructor
@Validated
@Tag(name = "Autenticação", description = "Endpoints para autenticação e gerenciamento de usuários")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;


    @Operation(summary = "Realizar login", description = "Autentica um usuário e retorna um token JWT",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login realizado com sucesso",
                            content = @Content(schema = @Schema(implementation = JwtAuthenticationResponse.class))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados de login inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciais inválidas")
            })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        log.info("Tentativa de login para email: {}", maskEmail(email));

        try {
            // Autenticar usuário usando Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Gerar JWT token
            String jwt = tokenProvider.generateToken(authentication);

            // Obter dados do usuário autenticado para log
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            log.info("Login realizado com sucesso para usuário: {}", userPrincipal.getNome());

            // Retornar apenas o token seguindo boas práticas de segurança
            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));

        } catch (Exception e) {
            String errorMessage = "Falha na autenticação para email: " + maskEmail(loginRequest.getEmail()) + " - " + e.getMessage();
            log.warn(errorMessage);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Email ou senha inválidos"));
        }
    }


    @Operation(summary = "Registrar usuário", description = "Registra um novo usuário no sistema",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados de registro inválidos"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email já cadastrado")
            })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Tentativa de registro para email: {}",
                 maskEmail(registerRequest.getEmail()));

        try {
            Usuario usuario = usuarioService.registrarUsuario(registerRequest);
            log.info("Usuário registrado com sucesso: {}", usuario.getNome());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Usuário registrado com sucesso"));

        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação durante registro para email: {} - {}",
                     maskEmail(registerRequest.getEmail()), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Erro inesperado durante registro para email: {} - {}",
                     maskEmail(registerRequest.getEmail()), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Erro interno do servidor"));
        }
    }








    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 1) {
            return "***@" + parts[1];
        }
        return localPart.charAt(0) + "***@" + parts[1];
    }


}

