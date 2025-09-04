package com.estagiarios.e_commerce.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String nome;
    private String email;
    private String cpf;
    private List<String> roles;

    public JwtAuthenticationResponse(String accessToken,  Long userId, String nome, String email, String cpf, List<String> roles) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.roles = roles;
    }
}
