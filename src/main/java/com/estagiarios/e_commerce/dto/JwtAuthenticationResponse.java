package com.estagiarios.e_commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta de autenticação JWT seguindo boas práticas de segurança.
 * Retorna apenas o token de acesso, mantendo dados sensíveis do usuário
 * seguros dentro do token JWT.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";

    public JwtAuthenticationResponse(String accessToken) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
    }
}
