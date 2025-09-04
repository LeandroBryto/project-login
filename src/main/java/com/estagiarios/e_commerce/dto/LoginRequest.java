package com.estagiarios.e_commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "^\\d{11}$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$",
            message = "CPF deve estar no formato 00000000000 ou 000.000.000-00")
    private String cpf;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 12)
    private String senha;

    public String getCpfLimpo(){
        return cpf != null ? cpf.replaceAll("\\D","") : null;
    }
}
