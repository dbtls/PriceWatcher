package com.example.pricewatch.user.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegistUserDto {
    public String email;
    public String password;
}





