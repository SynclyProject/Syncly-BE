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

    public record UpdateName(
            @ValidName String newName
    ){}

    @ValidLeaveReason//LeaveReasonType.ETC 일 경우에만 leaveReason null, 공백문자체크
    public record DeleteMember(
            LeaveReasonType leaveReasonType,
            String leaveReason, // 기타 사유일 경우만 필수
            @NotBlank String password
    ){}
}