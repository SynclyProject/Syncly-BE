package com.project.syncly.domain.chat.repository;

import com.project.syncly.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByWorkspaceIdAndMsgId(Long workspaceId, String msgId);

    Optional<ChatMessage> findByWorkspaceIdAndSeq(Long workspaceId, Long seq);

    // 최신 N개
    @Query("""
    SELECT m FROM ChatMessage m
    WHERE m.workspace.id = :ws
    ORDER BY m.seq DESC
    """)
    List<ChatMessage> findLatest(@Param("ws") Long wsId, Pageable pageable);

    // 과거 더보기
    @Query("""
    SELECT m FROM ChatMessage m
    WHERE m.workspace.id = :ws AND m.seq < :beforeSeq
    ORDER BY m.seq DESC
    """)
    List<ChatMessage> findBefore(@Param("ws") Long wsId, @Param("beforeSeq") Long beforeSeq, Pageable pageable);

    // 끊김 보정(델타)
    @Query("""
    SELECT m FROM ChatMessage m
    WHERE m.workspace.id = :ws AND m.seq > :afterSeq
    ORDER BY m.seq ASC
    """)
    List<ChatMessage> findAfter(@Param("ws") Long wsId, @Param("afterSeq") Long afterSeq, Pageable pageable);

    @Query("SELECT COALESCE(MAX(m.seq),0) FROM ChatMessage m WHERE m.workspace.id=:ws")
    Long findLatestSeq(@Param("ws") Long wsId);
}


