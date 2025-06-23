package com.project.syncly.domain.url.controller;

import com.project.syncly.domain.url.dto.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class ChatController { //웹소켓 테스트용 채팅 컨트롤러 -> 추후 확장하여 화면공유 채팅에 활용할 예정
    @MessageMapping("/sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage chatMessage, Principal principal) {
        String username = principal.getName(); // 인증된 사용자 정보
        chatMessage.setSender(username);       // 보낸 사람 세팅

        System.out.println("메시지 수신: " + chatMessage.getContent() + " / from: " + username);
        return chatMessage; // 반드시 return 해야 broadcast 됨
    }


    @MessageMapping("/newUser")
    @SendTo("/topic/public")
    public ChatMessage newUser(ChatMessage chatMessage, Principal principal) {
        String username = principal.getName();

        chatMessage.setType(ChatMessage.MessageType.ENTER);
        chatMessage.setSender(username);
        chatMessage.setContent(username + "님이 입장했습니다.");
        return chatMessage;
    }
}
