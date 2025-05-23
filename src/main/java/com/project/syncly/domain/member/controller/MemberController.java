package com.project.syncly.domain.member.controller;


import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.service.MemberQueryService;
import com.project.syncly.domain.member.service.MemberCommandService;

import com.project.syncly.global.anotations.MemberIdInfo;
import com.project.syncly.global.anotations.MemberInfo;
import com.project.syncly.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Validated
public class MemberController {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;


    // 1. 이메일 전송
    @PostMapping("/email/send")
    public ResponseEntity<CustomResponse<Void>> sendEmailAuthCode(@RequestParam @Email @NotBlank String email) {
        memberQueryService.sendAuthCode(email);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }

    // 2. 인증 코드 검증
    @PostMapping("/email/verify")
    public ResponseEntity<CustomResponse<Boolean>> verifyAuthCode(
            @RequestParam String email,
            @RequestParam String code) {
        boolean isVerified = memberQueryService.verifyCode(email, code);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, isVerified));
    }

    @PostMapping("/register")
    public ResponseEntity<CustomResponse<Void>> register(@RequestBody @Valid MemberRequestDTO.SignUp signUpDTO) {
        memberCommandService.registerMember(signUpDTO);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }


    @PostMapping("/test")
    public CustomResponse<?> test(@MemberInfo Member member) {
        System.out.println(member.toString());
        return CustomResponse.success(HttpStatus.OK);
    }

    @PatchMapping("/name")
    public ResponseEntity<CustomResponse<Void>> updateName(@RequestBody @Valid MemberRequestDTO.UpdateName updateName,
                                        @MemberIdInfo Long memberId) {
        memberCommandService.updateName(updateName, memberId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }

}