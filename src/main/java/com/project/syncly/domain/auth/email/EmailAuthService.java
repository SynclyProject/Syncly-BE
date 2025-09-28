package com.project.syncly.domain.auth.email;

public interface EmailAuthService {
    public void sendAuthCode(String email);
    public boolean verifyCodeAndMarkVerified(String email, String inputCode);

    void saveCode(String email, String code);
    String getCode(String email);
    void deleteCode(String email);
    void markVerified(String email);
    boolean isVerified(String email);
    void clearVerified(String email);

    void sendAuthCodeBeforeChangePassword(String email);
    boolean verifyCodeAndMarkVerifiedBeforeChangePassword(String email, String inputCode);
    boolean isVerifiedBeforeChangePassword(String email);
}