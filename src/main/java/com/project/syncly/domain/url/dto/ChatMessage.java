package com.project.syncly.domain.url.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatMessage {
    private String sender;
    private String content;
    private MessageType type;

    public enum MessageType {
        ENTER, CHAT, LEAVE
    }
}
