package com.project.syncly.domain.member.controller;


import com.project.syncly.domain.auth.email.EmailAuthService;
import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.dto.response.MemberResponseDTO;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.service.MemberQueryService;
import com.project.syncly.domain.member.service.MemberCommandService;

import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.global.anotations.MemberIdInfo;
import com.project.syncly.global.anotations.MemberInfo;
import com.project.syncly.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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
    private final EmailAuthService emailAuthService;


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
    @GetMapping
    public ResponseEntity<CustomResponse<MemberResponseDTO.MemberInfo>> getMember(@MemberInfo Member member) {
        MemberResponseDTO.MemberInfo memberInfo = memberQueryService.getMyInfo(member);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, memberInfo));
    }

    @PatchMapping("/name")
    public ResponseEntity<CustomResponse<Void>> updateName(@RequestBody @Valid MemberRequestDTO.UpdateName updateName,
                                        @MemberIdInfo Long memberId) {
        memberCommandService.updateName(updateName, memberId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }

    @PatchMapping("/profile-image")
    public CustomResponse<Void> updateProfileImage(
            @RequestBody @Valid S3RequestDTO.UpdateFile request,
            @MemberIdInfo Long memberId) {
        memberCommandService.updateProfileImage(memberId, request);
        return CustomResponse.success(HttpStatus.OK);
    }

    @DeleteMapping("/profile-image")
    public ResponseEntity<CustomResponse<Void>> deleteProfileImage(@MemberIdInfo Long memberId) {
        memberCommandService.deleteProfileImage(memberId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));    }

    // 기존 비밀번호 검증으로 비밀번호 변경
    @Operation(
            summary = "비밀번호 변경 (기존 비밀번호 확인)",
            description = "회원이 기존 비밀번호를 입력하고 새로운 비밀번호로 변경하는 API입니다."
    )
    @PatchMapping("/password")
    public ResponseEntity<CustomResponse<Void>> updatePassword(@RequestBody @Valid MemberRequestDTO.UpdatePassword updatePassword,
                                                           @MemberIdInfo Long memberId) {
        memberCommandService.updatePassword(updatePassword, memberId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }
    //  이메일 인증코드 발송 (비번 변경용)
    @Operation(
            summary = "비밀번호 변경용 이메일 인증코드 발송",
            description = "이메일 인증으로 비밀번호를 변경하기 위해 인증 코드를 발송하는 API입니다.")
    @PostMapping("/password/email/send")
    public ResponseEntity<CustomResponse<Void>> sendPasswordChangeEmailCode(
            @RequestParam @Email @NotBlank String email) {

        emailAuthService.sendAuthCodeBeforeChangePassword(email);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }

    // 인증코드 검증(비번 변경용)
    @Operation(
            summary = "비밀번호 변경용 이메일 인증코드 검증",
            description = "발송된 인증 코드를 검증하는 API입니다."
    )
    @PostMapping("/password/email/verify")
    public ResponseEntity<CustomResponse<Boolean>> verifyPasswordChangeEmailCode(
            @RequestParam @Email @NotBlank String email,
            @RequestParam @NotBlank String code) {

        boolean verified = emailAuthService.verifyCodeAndMarkVerifiedBeforeChangePassword(email, code);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, verified));
    }
    //이메일 인증으로 비밀번호 변경
    @Operation(
            summary = "비밀번호 변경 (이메일 인증 기반)",
            description = "이메일 인증을 통해 비밀번호를 변경하는 API입니다."
    )
    @PatchMapping("/password/email")
    public ResponseEntity<CustomResponse<Void>> updatePasswordWithEmail(@RequestBody @Valid MemberRequestDTO.UpdatePasswordWithEmail updatePassword) {
        memberCommandService.updatePasswordWithEmail(updatePassword);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }

    @DeleteMapping
    public ResponseEntity<CustomResponse<Void>> deleteMember(HttpServletRequest servletRequest,
                                                             HttpServletResponse servletResponse,
                                                             @MemberIdInfo Long memberId,
                                                             @RequestBody @Valid MemberRequestDTO.DeleteMember deleteMember) {
        memberCommandService.deleteMember(servletRequest, servletResponse, memberId, deleteMember);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK));
    }

}