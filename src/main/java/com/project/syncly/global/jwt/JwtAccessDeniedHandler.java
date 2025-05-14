package com.project.syncly.global.jwt;


import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
//role 없어서 필요없음
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    // 인가 관련 예외처리

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("403 Forbidden - Access Denied: {}", accessDeniedException.getMessage());

        //jwt필터에서 발생한 예외는 response로 직접 예외 만들기에 CorsConfig에서 설정한 허용이 반영안됨. 따라서 프론트에서 못받음
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        // ContentType header 설정
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        BaseErrorCode errorCode = JwtErrorCode.ACCESS_DENIED;
        // 반환할 응답 만들기
        CustomResponse<Void> errorResponse = errorCode.getErrorResponse();
        // 응답을 response에 작성
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}