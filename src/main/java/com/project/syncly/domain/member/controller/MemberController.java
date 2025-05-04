package com.project.syncly.domain.member.controller;


import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.service.MemberQueryService;
import com.project.syncly.domain.member.service.MemberCommandService;

import com.project.syncly.global.apiPayload.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    @PostMapping("/register")
    public CustomResponse<?> register(@RequestParam MemberRequestDTO.signUp signUpDTO) {
        Member member = memberCommandService.registerMember(signUpDTO);
        return CustomResponse.success(HttpStatus.OK);
    }

    @GetMapping("/check-email")
    public CustomResponse<?> checkEmail(@RequestParam String email) {
        boolean exists = memberQueryService.isEmailExist(email);
        return CustomResponse.success(HttpStatus.OK);
    }
}