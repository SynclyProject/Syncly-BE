package com.project.syncly.domain.note.repository;

import com.project.syncly.domain.note.entity.NoteParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteParticipantRepository extends JpaRepository<NoteParticipant, Long> {

    /**
     * 특정 노트의 모든 참여자 조회
     */
    @Query("SELECT np FROM NoteParticipant np WHERE np.note.id = :noteId")
    List<NoteParticipant> findByNoteId(@Param("noteId") Long noteId);

    /**
     * 특정 노트의 온라인 참여자만 조회
     */
    @Query("SELECT np FROM NoteParticipant np WHERE np.note.id = :noteId AND np.isOnline = true")
    List<NoteParticipant> findOnlineParticipantsByNoteId(@Param("noteId") Long noteId);

    /**
     * 노트 ID와 멤버 ID로 참여자 조회
     */
    @Query("SELECT np FROM NoteParticipant np WHERE np.note.id = :noteId AND np.member.id = :memberId")
    Optional<NoteParticipant> findByNoteIdAndMemberId(@Param("noteId") Long noteId, @Param("memberId") Long memberId);

    /**
     * 특정 멤버가 참여 중인 모든 노트의 참여자 정보 조회
     */
    @Query("SELECT np FROM NoteParticipant np WHERE np.member.id = :memberId")
    List<NoteParticipant> findByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 멤버가 온라인 상태인 노트 목록 조회
     */
    @Query("SELECT np FROM NoteParticipant np WHERE np.member.id = :memberId AND np.isOnline = true")
    List<NoteParticipant> findOnlineNotesByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 노트의 온라인 참여자 수 조회
     */
    @Query("SELECT COUNT(np) FROM NoteParticipant np WHERE np.note.id = :noteId AND np.isOnline = true")
    Long countOnlineParticipantsByNoteId(@Param("noteId") Long noteId);

    /**
     * 특정 노트의 전체 참여자 수 조회
     */
    @Query("SELECT COUNT(np) FROM NoteParticipant np WHERE np.note.id = :noteId")
    Long countByNoteId(@Param("noteId") Long noteId);

    /**
     * 노트 참여자의 온라인 상태 업데이트 (온라인으로)
     */
    @Modifying
    @Query("UPDATE NoteParticipant np SET np.isOnline = true, np.joinedAt = CURRENT_TIMESTAMP WHERE np.note.id = :noteId AND np.member.id = :memberId")
    int updateToOnline(@Param("noteId") Long noteId, @Param("memberId") Long memberId);

    /**
     * 노트 참여자의 온라인 상태 업데이트 (오프라인으로)
     */
    @Modifying
    @Query("UPDATE NoteParticipant np SET np.isOnline = false WHERE np.note.id = :noteId AND np.member.id = :memberId")
    int updateToOffline(@Param("noteId") Long noteId, @Param("memberId") Long memberId);

    /**
     * 특정 멤버의 모든 노트를 오프라인 상태로 변경 (연결 끊김 시)
     */
    @Modifying
    @Query("UPDATE NoteParticipant np SET np.isOnline = false WHERE np.member.id = :memberId AND np.isOnline = true")
    int updateAllToOfflineByMemberId(@Param("memberId") Long memberId);

    /**
     * 노트와 멤버 조합이 이미 존재하는지 확인
     */
    @Query("SELECT COUNT(np) > 0 FROM NoteParticipant np WHERE np.note.id = :noteId AND np.member.id = :memberId")
    boolean existsByNoteIdAndMemberId(@Param("noteId") Long noteId, @Param("memberId") Long memberId);
}
