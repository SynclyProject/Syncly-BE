package com.project.syncly.domain.workspace.service;

import jakarta.mail.internet.MimeMessage;

public interface InvitationMailService {

    String generateUniqueToken();

    MimeMessage createMail(String mail, String number);

    void sendSimpleMessage(String sendEmail, String code);
}