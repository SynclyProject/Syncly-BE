package com.project.syncly.global.config;

import io.livekit.server.RoomService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.livekit.server.WebhookReceiver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import livekit.RoomServiceGrpc;

@Configuration
public class LiveKitBeanConfig {

    @Bean
    public WebhookReceiver webhookReceiver(LiveKitProperties props) {
        return new WebhookReceiver(props.getApiKey(), props.getApiSecret());
    }

    @Bean
    public ManagedChannel liveKitChannel(LiveKitProperties props) {
        return ManagedChannelBuilder
                .forTarget(props.getGrpcUrl()) // 예: "localhost:7881"
                .usePlaintext() // TLS가 아닌 경우
                .build();
    }

    // Stub : .proto 파일로부터 자동으로 생성되는 클라이언트 코드
    @Bean
    public RoomServiceGrpc.RoomServiceBlockingStub roomServiceStub(ManagedChannel channel) {
        return RoomServiceGrpc.newBlockingStub(channel);
    }
}
