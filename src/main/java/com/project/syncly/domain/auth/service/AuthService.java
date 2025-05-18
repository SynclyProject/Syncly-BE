package com.project.syncly.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public interface AuthService {

    public String login(String email, String password, HttpServletResponse response);
    public String reissueAccessToken(HttpServletRequest request, HttpServletResponse response);
    public void logout(HttpServletRequest request, HttpServletResponse response);
}
