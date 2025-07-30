package com.project.syncly.domain.livekit.service.api;

import com.project.syncly.domain.livekit.exception.LiveKitErrorCode;
import com.project.syncly.domain.livekit.exception.LiveKitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitApiService {
    private final LiveKitApiClient liveKitApiClient;

    public void deleteRoom(String roomId) {
        log.debug("deleteRoom() 호출됨: {}", roomId);
        liveKitApiClient.deleteRoom(roomId)
                .subscribe(
                        unused -> log.info("LiveKit 방 '{}' 삭제 성공", roomId),
                        error -> {
                            log.warn("LiveKit 방 '{}' 삭제 실패", roomId, error);
                            liveKitApiClient.deleteRoom(roomId);
                        }
                );
    }
}
