package com.project.syncly.domain.member.dto.request;


import com.project.syncly.global.validator.annotation.ValidName;
import com.project.syncly.global.validator.annotation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class MemberRequestDTO {
    public record SignUp(
            @Email @NotBlank String email,
            @ValidPassword String password,
            @ValidName String name
    ) { }
    public record Login(
            @Email @NotBlank String email,
            @ValidPassword String password
    ) { }
}