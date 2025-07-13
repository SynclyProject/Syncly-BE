package com.project.syncly.domain.livekit.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


public record NotificationDto(
        String type, String identity
) {}
