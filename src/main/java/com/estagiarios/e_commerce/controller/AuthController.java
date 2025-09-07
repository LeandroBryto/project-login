package com.estagiarios.e_commerce.controller;

import com.estagiarios.e_commerce.dto.*;
import com.estagiarios.e_commerce.entity.Usuario;
import com.estagiarios.e_commerce.security.JwtTokenProvider;
import com.estagiarios.e_commerce.security.UserPrincipal;
import com.estagiarios.e_commerce.service.UsuarioService;
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

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:4200}", maxAge = 3600)
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;


    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Tentativa de login para CPF: {}", maskCpf(loginRequest.getCpf()));

        try {
            // Autenticar usuário usando Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getCpf(),
                            loginRequest.getSenha()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Gerar JWT token
            String jwt = tokenProvider.generateToken(authentication);

            // Obter dados do usuário autenticado
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            List<String> roles = userPrincipal.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .toList();

            log.info("Login realizado com sucesso para usuário: {}", userPrincipal.getNome());

            return ResponseEntity.ok(new JwtAuthenticationResponse(
                    jwt,
                    userPrincipal.getId(),
                    userPrincipal.getNome(),
                    userPrincipal.getEmail(),
                    userPrincipal.getCpf(),
                    roles
            ));

        } catch (Exception e) {
            log.warn("Falha na autenticação para CPF: {} - {}",
                    maskCpf(loginRequest.getCpf()), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "CPF ou senha inválidos"));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Tentativa de registro para CPF: {} e email: {}",
                maskCpf(registerRequest.getCpf()),
                maskEmail(registerRequest.getEmail()));

        try {
            Usuario usuario = usuarioService.registrarUsuario(registerRequest);
            log.info("Usuário registrado com sucesso: {}", usuario.getNome());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Usuário registrado com sucesso"));

        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação durante registro para CPF: {} - {}",
                    maskCpf(registerRequest.getCpf()), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Erro inesperado durante registro para CPF: {} - {}",
                    maskCpf(registerRequest.getCpf()), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Erro interno do servidor"));
        }
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        log.info("Tentativa de recuperação de senha para CPF: {}",
                maskCpf(forgotPasswordRequest.getCpf()));

        try {
            Usuario usuario = usuarioService.recuperarSenha(forgotPasswordRequest);
            log.info("Senha recuperada com sucesso para usuário: {}", usuario.getNome());
            return ResponseEntity.ok(new ApiResponse(true, "Senha redefinida com sucesso"));

        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação durante recuperação de senha para CPF: {} - {}",
                    maskCpf(forgotPasswordRequest.getCpf()), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Erro inesperado durante recuperação de senha para CPF: {} - {}",
                    maskCpf(forgotPasswordRequest.getCpf()), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Erro interno do servidor"));
        }
    }


    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return "***";
        }
        String cleanCpf = cpf.replaceAll("\\D", "");
        if (cleanCpf.length() != 11) {
            return "***";
        }
        return cleanCpf.substring(0, 9) + "XX";
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

