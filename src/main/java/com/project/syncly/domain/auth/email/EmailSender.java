package com.project.syncly.domain.auth.email;

public interface EmailSender {
    void send(String to, String subject, String body);
}