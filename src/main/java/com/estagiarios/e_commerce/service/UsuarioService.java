package com.estagiarios.e_commerce.service;



import com.estagiarios.e_commerce.dto.RegisterRequest;
import com.estagiarios.e_commerce.entity.Usuario;
import com.estagiarios.e_commerce.repository.UsuarioRepository;
import com.estagiarios.e_commerce.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service responsável pela gestão de usuários
 * Implementa operações de registro, autenticação e recuperação de senha
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );



    /**
     * Registra um novo usuário no sistema
     *
     * @param registerRequest dados do usuário para registro
     * @return usuário registrado
     * @throws IllegalArgumentException se email já existir ou dados inválidos
     */
    @Transactional
    public Usuario registrarUsuario(RegisterRequest registerRequest) {
        log.info("Iniciando registro de usuário com email: {}",
                maskEmail(registerRequest.getEmail()));

        // Validações de negócio
        validateUserRegistration(registerRequest);

        try {
            // Criar novo usuário
            Usuario usuario = buildUsuarioFromRequest(registerRequest);
            Usuario usuarioSalvo = usuarioRepository.save(usuario);

            log.info("Usuário registrado com sucesso. ID: {}, Email: {}",
                    usuarioSalvo.getId(), maskEmail(usuarioSalvo.getEmail()));

            return usuarioSalvo;

        } catch (Exception e) {
            log.error("Erro ao salvar usuário no banco de dados: {}", e.getMessage());
            throw new RuntimeException("Erro interno ao registrar usuário", e);
        }
    }

    /**
     * Valida os dados de registro do usuário
     */
    private void validateUserRegistration(RegisterRequest request) {
        // Validar email
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email inválido");
        }

        // Verificar se email já existe
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Tentativa de registro com email já existente: {}",
                    maskEmail(request.getEmail()));
            throw new IllegalArgumentException("Email já cadastrado no sistema");
        }

        // Data de nascimento não é mais obrigatória no novo sistema
    }

    /**
     * Constrói um objeto Usuario a partir do RegisterRequest
     */
    private Usuario buildUsuarioFromRequest(RegisterRequest request) {
        return new Usuario(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );
    }







    /**
     * Valida se a senha fornecida corresponde à senha criptografada
     *
     * @param senhaFornecida senha em texto plano
     * @param senhaCriptografada senha criptografada do banco
     * @return true se as senhas coincidem
     */
    public boolean validarSenha(String senhaFornecida, String senhaCriptografada) {
        return passwordEncoder.matches(senhaFornecida, senhaCriptografada);
    }







    /**
     * Valida se o email está em formato válido
     */
    private boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }



    /**
     * Mascara o email para logs (ex: user@domain.com -> u***@domain.com)
     */
    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 1) {
            return "***@" + parts[1];
        }
        return localPart.charAt(0) + "***@" + parts[1];
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com email: " + maskEmail(email)));

        return UserPrincipal.create(usuario);
    }

    public UserDetails loadUserById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com ID: " + id));

        return UserPrincipal.create(usuario);
    }


}

