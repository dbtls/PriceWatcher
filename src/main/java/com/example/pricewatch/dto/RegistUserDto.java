package com.example.pricewatch.dto;

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
