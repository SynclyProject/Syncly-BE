package com.project.syncly.domain.livekit.service.redis;

import com.project.syncly.domain.livekit.converter.LiveKitConverter;
import com.project.syncly.domain.livekit.dto.ParticipantInfoDTO;
import com.project.syncly.domain.livekit.dto.ParticipantInfoListDTO;
import com.project.syncly.domain.livekit.exception.LiveKitErrorCode;
import com.project.syncly.domain.livekit.exception.LiveKitException;
import com.project.syncly.domain.livekit.service.LiveKitTokenService;
import com.project.syncly.domain.livekit.service.api.LiveKitApiService;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomStateService {

    private final RedisStorage redisStorage;
    private final ParticipantStateService participantStateService;
    private final RoomExpirationProducer roomExpirationProducer;
    private final LiveKitTokenService liveKitTokenService;
    private final LiveKitApiService liveKitApiService;

    public boolean addParticipant(String roomId, String participantId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Long count = redisStorage.addToSet(key, participantId);
        return count == 1; // 첫 입장자면 true
    }

    public boolean removeParticipant(String roomId, String participantId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Long count = redisStorage.removeFromSet(key, participantId);
        return count == 0; // 마지막 참여자면 true
    }

    public void removeRoom(String roomId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        redisStorage.delete(key);
    }

    public void removeRoomAllData(String roomId) {
        participantStateService.removeAllParticipants(roomId); // Redis hash 제거
        removeRoom(roomId);  // Redis set 제거
        roomExpirationProducer.deleteRoomExpiration(roomId);
    }

    public ParticipantInfoListDTO getParticipantInfoList(Long workspaceId, Long memberId) {
        if (!liveKitTokenService.isMemberIncludeWorkspace(memberId, workspaceId)) {
            throw new LiveKitException(LiveKitErrorCode.MEMBER_NOT_INCLUDE_WORKSPACE);
        }
        String roomId = LiveKitConverter.getRoomId(workspaceId);
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Set<String> participantIds = redisStorage.getSetValues(key);

        if (participantIds == null || participantIds.isEmpty()) {
            return LiveKitConverter.toParticipantInfoListDTO(List.of());
        }

        List<ParticipantInfoDTO> list = participantIds.stream()
                .map(id -> participantStateService.getParticipantInfo(roomId, id))
                .filter(Objects::nonNull)
                .toList();

        return LiveKitConverter.toParticipantInfoListDTO(list);
    }
//방삭제 테스트
    public void deleteRoomTester(String roomId){
        try {
            liveKitApiService.deleteRoom(roomId); // LiveKit에서 방 삭제
        } catch (Exception e) {
            System.out.println("방 삭제 중 오류 발생: {}"+ roomId+ e.getMessage());

            return;
        }
        removeRoomAllData(roomId); // ZSet에서 삭제
    }

}