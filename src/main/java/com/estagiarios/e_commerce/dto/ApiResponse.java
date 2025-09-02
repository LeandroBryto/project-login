package com.estagiarios.e_commerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {

    private Boolean sucesso;
    private String mensage;

    @JsonFormat(pattern = "dd/MM/yyyy HH:MM:ss")
    private LocalDateTime timestamp;

    private Object data;

    public ApiResponse(Boolean sucesso, String mensage) {
        this.sucesso = sucesso;
        this.mensage = mensage;
        this.timestamp = LocalDateTime.now();
    }
    public ApiResponse(Boolean sucesso, String mensage, Object data) {
        this.sucesso = sucesso;
        this.mensage = mensage;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static  ApiResponse success(String mensage){
        return new ApiResponse(true, mensage);
    }
    public static  ApiResponse success(String mensage, Object data){
        return new ApiResponse(true, mensage, data);
    }
    public static  ApiResponse error(String mensage){
        return new ApiResponse(false, mensage);
    }
}
