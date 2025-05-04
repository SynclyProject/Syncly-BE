package com.project.syncly.domain.member.dto.request;


public class MemberRequestDTO {
    public record signUp(
            String email,
            String password,
            String name
    ) { }
}