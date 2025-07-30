package com.project.syncly.domain.livekit.service.redis;

import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomStateService {

    private final RedisStorage redisStorage;

    public boolean addParticipant(String roomId, String participantId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Long count = redisStorage.addToSet(key, participantId);
        return count == 1; // 첫 입장자면 true
    }

    public Set<Object> getParticipantsByRoom(String roomId){
        return redisStorage.getSetValues(roomId);
    }

    public boolean removeParticipant(String roomId, String participantId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        Long count = redisStorage.removeFromSet(key, participantId);
        return count == 0; // 첫 입장자면 true
    }

    public void removeRoom(String roomId) {
        String key = RedisKeyPrefix.CALL_ROOM.get(roomId);
        redisStorage.delete(key);

    }

    public ParticipantInfoListDTO getParticipantInfoList(Long workspaceId, Long memberId) {
        if (!liveKitTokenService.isMemberIncludeWorkspace(workspaceId, memberId)) {
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