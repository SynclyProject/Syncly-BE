package com.project.syncly.domain.livekit.util;

import io.livekit.server.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.project.syncly.global.config.LiveKitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LiveKitTokenUtil {

    private final LiveKitProperties liveKitProperties;
    public String createToken(String identity,String memberName ,String roomName) {
        AccessToken token = new AccessToken(
                liveKitProperties.getIngress().getApiKey(),
                liveKitProperties.getIngress().getApiSecret());
        token.setName(memberName);
        token.setIdentity(identity);
        token.addGrants(new RoomJoin(true), new Room(roomName));

        return token.toJwt();
    }

    //관리자 API 호출용 JWT 생성 (roomCreate 권한 포함)
    public String createAdminToken() {
        AccessToken token = new AccessToken(
                liveKitProperties.getAdmin().getApiKey(),
                liveKitProperties.getAdmin().getApiSecret()
        );
        token.addGrants(
                new RoomCreate(true),
                new RoomAdmin(true)
        );
        return token.toJwt();
    }
}