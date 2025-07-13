package com.project.syncly.domain.livekit.service.api;

import livekit.LivekitRoom;
import livekit.RoomServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveKitApiClient {

    private final RoomServiceGrpc.RoomServiceBlockingStub roomService;

    public void deleteRoom(String roomName) {
        LivekitRoom.DeleteRoomRequest request = LivekitRoom.DeleteRoomRequest.newBuilder()
                .setRoom(roomName)
                .build();

        roomService.deleteRoom(request);
    }
}
