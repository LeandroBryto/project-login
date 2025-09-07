package com.estagiarios.e_commerce.service;


import com.estagiarios.e_commerce.dto.ForgotPasswordRequest;
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

import java.time.LocalDate;
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

    private static final String CPF_REGEX = "^\\d{11}$";

    /**
     * Registra um novo usuário no sistema
     *
     * @param registerRequest dados do usuário para registro
     * @return usuário registrado
     * @throws IllegalArgumentException se CPF ou email já existirem ou dados inválidos
     */
    @Transactional
    public Usuario registrarUsuario(RegisterRequest registerRequest) {
        log.info("Iniciando registro de usuário com CPF: {}",
                maskCpf(registerRequest.getCpf()));

        // Validações de negócio
        validateUserRegistration(registerRequest);

        try {
            // Criar novo usuário
            Usuario usuario = buildUsuarioFromRequest(registerRequest);
            Usuario usuarioSalvo = usuarioRepository.save(usuario);

            log.info("Usuário registrado com sucesso. ID: {}, CPF: {}",
                    usuarioSalvo.getId(), maskCpf(usuarioSalvo.getCpf()));

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
        // Validar CPF
        if (!isValidCPF(request.getCpf())) {
            throw new IllegalArgumentException("CPF inválido");
        }

        // Validar email
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email inválido");
        }

        // Verificar se CPF já existe
        if (usuarioRepository.existsByCpf(request.getCpf())) {
            log.warn("Tentativa de registro com CPF já existente: {}",
                    maskCpf(request.getCpf()));
            throw new IllegalArgumentException("CPF já cadastrado no sistema");
        }

        // Verificar se email já existe
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Tentativa de registro com email já existente: {}",
                    maskEmail(request.getEmail()));
            throw new IllegalArgumentException("Email já cadastrado no sistema");
        }

        // Validar data de nascimento
        if (request.getDataNascimento().isAfter(LocalDate.now().minusYears(18))) {
            throw new IllegalArgumentException("Usuário deve ser maior de 18 anos");
        }
    }

    /**
     * Constrói um objeto Usuario a partir do RegisterRequest
     */
    private Usuario buildUsuarioFromRequest(RegisterRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome().trim());
        usuario.setCpf(request.getCpf());
        usuario.setDataNascimento(request.getDataNascimento());
        usuario.setEmail(request.getEmail().toLowerCase().trim());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        return usuario;
    }



    /**
     * Busca usuário por CPF
     *
     * @param cpf CPF do usuário
     * @return Optional contendo o usuário se encontrado
     * @throws IllegalArgumentException se CPF inválido
     */
    public Optional<Usuario> buscarPorCpf(String cpf) {
        if (!isValidCPF(cpf)) {
            throw new IllegalArgumentException("CPF inválido");
        }

        log.debug("Buscando usuário por CPF: {}", maskCpf(cpf));
        return usuarioRepository.findByCpf(cpf);
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
     * Recupera senha do usuário baseado em CPF e data de nascimento
     *
     * @param forgotPasswordRequest dados para recuperação de senha
     * @return usuário com senha atualizada
     * @throws IllegalArgumentException se usuário não for encontrado ou dados inválidos
     */
    @Transactional
    public Usuario recuperarSenha(ForgotPasswordRequest forgotPasswordRequest) {
        log.info("Tentativa de recuperação de senha para CPF: {}",
                maskCpf(forgotPasswordRequest.getCpf()));

        // Validar CPF
        if (!isValidCPF(forgotPasswordRequest.getCpf())) {
            throw new IllegalArgumentException("CPF inválido");
        }

        // Buscar usuário por CPF e data de nascimento
        Usuario usuario = usuarioRepository.findByCpfAndDataNascimento(
                        forgotPasswordRequest.getCpfLimpo(),
                        forgotPasswordRequest.getDataNascimento())
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para recuperação de senha. CPF: {}",
                            maskCpf(forgotPasswordRequest.getCpf()));
                    return new IllegalArgumentException("Usuário não encontrado com os dados fornecidos");
                });

        try {
            // Atualizar senha
            usuario.setSenha(passwordEncoder.encode(forgotPasswordRequest.getNovaSenha()));
            Usuario usuarioAtualizado = usuarioRepository.save(usuario);

            log.info("Senha recuperada com sucesso para usuário: {}", usuario.getNome());
            return usuarioAtualizado;

        } catch (Exception e) {
            log.error("Erro ao atualizar senha do usuário: {}", e.getMessage());
            throw new RuntimeException("Erro interno ao recuperar senha", e);
        }
    }

    /**
     * Valida se o CPF está em formato válido
     */
    private boolean isValidCPF(String cpf) {
        if (!StringUtils.hasText(cpf)) {
            return false;
        }

        // Remove caracteres não numéricos
        String cleanCpf = cpf.replaceAll("\\D", "");

        // Verifica se tem 11 dígitos
        if (!cleanCpf.matches(CPF_REGEX)) {
            return false;
        }

        // Verifica se todos os dígitos são iguais
        if (cleanCpf.chars().distinct().count() == 1) {
            return false;
        }

        // Validação dos dígitos verificadores
        return isValidCPFDigits(cleanCpf);
    }

    /**
     * Valida os dígitos verificadores do CPF
     */
    private boolean isValidCPFDigits(String cpf) {
        try {
            // Primeiro dígito verificador
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int firstDigit = 11 - (sum % 11);
            if (firstDigit >= 10) firstDigit = 0;

            // Segundo dígito verificador
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int secondDigit = 11 - (sum % 11);
            if (secondDigit >= 10) secondDigit = 0;

            return Character.getNumericValue(cpf.charAt(9)) == firstDigit &&
                    Character.getNumericValue(cpf.charAt(10)) == secondDigit;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valida se o email está em formato válido
     */
    private boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Mascara o CPF para logs (ex: 123.456.789-XX)
     */
    private String maskCpf(String cpf) {
        if (!StringUtils.hasText(cpf) || cpf.length() < 11) {
            return "***";
        }
        return cpf.substring(0, 9) + "XX";
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
    public UserDetails loadUserByUsername(String cpf) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com CPF: " + cpf));

        return UserPrincipal.create(usuario);
    }

    public UserDetails loadUserById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com ID: " + id));

        return UserPrincipal.create(usuario);
    }
}

