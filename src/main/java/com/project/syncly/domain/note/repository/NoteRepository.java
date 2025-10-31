package com.project.syncly.domain.note.repository;

import com.project.syncly.domain.note.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * 워크스페이스의 삭제되지 않은 노트 목록 조회 (페이징)
     * EntityGraph를 사용하여 Creator와 Workspace를 eager loading
     */
    @EntityGraph(attributePaths = {"creator", "workspace"})
    @Query("SELECT n FROM Note n WHERE n.workspace.id = :workspaceId AND n.isDeleted = false")
    Page<Note> findByWorkspaceId(@Param("workspaceId") Long workspaceId, Pageable pageable);

    /**
     * 워크스페이스의 삭제되지 않은 노트 목록 조회 (전체)
     * EntityGraph를 사용하여 Creator와 Workspace를 eager loading
     */
    @EntityGraph(attributePaths = {"creator", "workspace"})
    @Query("SELECT n FROM Note n WHERE n.workspace.id = :workspaceId AND n.isDeleted = false")
    List<Note> findAllByWorkspaceId(@Param("workspaceId") Long workspaceId);

    /**
     * 노트 ID로 조회 (삭제되지 않은 노트만)
     * EntityGraph를 사용하여 Creator와 Workspace를 eager loading
     */
    @EntityGraph(attributePaths = {"creator", "workspace"})
    @Query("SELECT n FROM Note n WHERE n.id = :noteId AND n.isDeleted = false")
    Optional<Note> findByIdAndNotDeleted(@Param("noteId") Long noteId);

    /**
     * 워크스페이스와 노트 ID로 조회 (권한 검증용)
     * EntityGraph를 사용하여 Creator와 Workspace를 eager loading
     */
    @EntityGraph(attributePaths = {"creator", "workspace"})
    @Query("SELECT n FROM Note n WHERE n.id = :noteId AND n.workspace.id = :workspaceId AND n.isDeleted = false")
    Optional<Note> findByIdAndWorkspaceId(@Param("noteId") Long noteId, @Param("workspaceId") Long workspaceId);

    /**
     * 특정 멤버가 생성한 노트 목록 조회
     */
    @Query("SELECT n FROM Note n WHERE n.creator.id = :creatorId AND n.isDeleted = false")
    List<Note> findByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 워크스페이스의 노트 개수 조회 (삭제된 것 제외)
     */
    @Query("SELECT COUNT(n) FROM Note n WHERE n.workspace.id = :workspaceId AND n.isDeleted = false")
    Long countByWorkspaceId(@Param("workspaceId") Long workspaceId);

    /**
     * 제목으로 노트 검색 (워크스페이스 내)
     * EntityGraph를 사용하여 Creator와 Workspace를 eager loading
     */
    @EntityGraph(attributePaths = {"creator", "workspace"})
    @Query("SELECT n FROM Note n WHERE n.workspace.id = :workspaceId AND n.title LIKE %:keyword% AND n.isDeleted = false")
    Page<Note> searchByTitle(@Param("workspaceId") Long workspaceId, @Param("keyword") String keyword, Pageable pageable);
}
