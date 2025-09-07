package com.estagiarios.e_commerce.repository;

import com.estagiarios.e_commerce.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCpf(String cpf);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCpfAndDataNascimento(String cpf, LocalDate dataNascimento);

    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
}
