package com.onde.api.application.auth.dto;



import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Getter;



@Getter

@Builder

@AllArgsConstructor

public class LoginResponse {

    private String tokenType;

    private Long expiresIn;

    private String role;

    private boolean passwordChangeRequired;

}

