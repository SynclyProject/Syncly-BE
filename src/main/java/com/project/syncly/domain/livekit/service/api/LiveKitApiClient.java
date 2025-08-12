package com.project.syncly.domain.livekit.service.api;

import com.project.syncly.domain.livekit.util.LiveKitTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitApiClient {

    private final WebClient liveKitWebClient;
    private final LiveKitTokenUtil liveKitTokenUtil;

    public Mono<Void> deleteRoom(String roomName) {
        String jwt = liveKitTokenUtil.createAdminToken();
        System.out.println("WebClient deleteRoom 시작: {}"+ roomName);
        return liveKitWebClient.post()
                .uri("/twirp/livekit.RoomService/DeleteRoom")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .bodyValue(Map.of("room", roomName))
                .retrieve()
                .bodyToMono(Void.class);
    }
}
