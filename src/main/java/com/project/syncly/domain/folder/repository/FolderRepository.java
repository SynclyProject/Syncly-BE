package com.project.syncly.domain.folder.repository;

import com.project.syncly.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    // 동일 워크스페이스 + 동일 parentId 내 존재 여부 확인
    boolean existsByWorkspaceIdAndParentIdAndName(Long workspaceId, Long parentId, String name);

    // 루트 폴더 존재 확인
    boolean existsByWorkspaceIdAndParentIdIsNull(Long workspaceId);

    // 루트 폴더 조회
    Optional<Folder> findByWorkspaceIdAndParentIdIsNull(Long workspaceId);

    // 폴더가 특정 워크스페이스에 속하는지 확인
    boolean existsByWorkspaceIdAndId(Long workspaceId, Long folderId);

    // 특정 워크스페이스의 폴더 조회
    Optional<Folder> findByIdAndWorkspaceId(Long folderId, Long workspaceId);

    // 여러 폴더 ID들을 일괄 soft delete 처리
    @Query("UPDATE Folder f SET f.deletedAt = :deletedAt WHERE f.id IN :folderIds AND f.deletedAt IS NULL")
    @Modifying
    void updateDeletedAtByIdIn(@Param("folderIds") List<Long> folderIds, @Param("deletedAt") java.time.LocalDateTime deletedAt);

    // 삭제된 폴더 조회 (워크스페이스 확인 포함)
    @Query("SELECT f FROM Folder f WHERE f.id = :folderId AND f.workspaceId = :workspaceId AND f.deletedAt IS NOT NULL")
    Optional<Folder> findDeletedByIdAndWorkspaceId(@Param("folderId") Long folderId, @Param("workspaceId") Long workspaceId);

    // 폴더 복원 (deletedAt을 null로 설정)
    @Query("UPDATE Folder f SET f.deletedAt = NULL WHERE f.id IN :folderIds")
    @Modifying
    void restoreByIdIn(@Param("folderIds") List<Long> folderIds);

    // 특정 부모 폴더 하위에서 이름이 존재하는지 확인 (삭제되지 않은 폴더만)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Folder f " +
           "WHERE f.workspaceId = :workspaceId AND f.parentId = :parentId " +
           "AND f.name = :name AND f.deletedAt IS NULL")
    boolean existsByWorkspaceIdAndParentIdAndNameAndDeletedAtIsNull(@Param("workspaceId") Long workspaceId,
                                                                   @Param("parentId") Long parentId,
                                                                   @Param("name") String name);

    // 폴더의 부모와 이름 업데이트
    @Query("UPDATE Folder f SET f.parentId = :parentId, f.name = :name WHERE f.id = :folderId")
    @Modifying
    void updateParentIdAndName(@Param("folderId") Long folderId, @Param("parentId") Long parentId, @Param("name") String name);

    // 삭제된 하위 폴더들 조회
    @Query("SELECT f FROM Folder f WHERE f.parentId = :parentId AND f.deletedAt IS NOT NULL")
    List<Folder> findByParentIdAndDeletedAtIsNotNull(@Param("parentId") Long parentId);

    // 여러 폴더 ID들을 완전 삭제 (hard delete)
    @Query("DELETE FROM Folder f WHERE f.id IN :folderIds")
    @Modifying
    void deleteByIdIn(@Param("folderIds") List<Long> folderIds);

    // 폴더 내 아이템 통합 조회 (폴더 + 파일)를 위한 커스텀 메서드들

    // 특정 폴더의 하위 폴더 목록 조회 (워크스페이스 멤버 정보 포함)
    @Query("""
        SELECT f.id, f.name, f.createdAt, f.updatedAt,
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM Folder f
        JOIN WorkspaceMember wm ON f.workspaceMemberId = wm.id
        WHERE f.parentId = :folderId
        AND f.deletedAt IS NULL
        AND (:search IS NULL OR f.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        ORDER BY
            CASE WHEN :sort = 'latest' THEN f.updatedAt END DESC,
            CASE WHEN :sort = 'alphabet' THEN f.name END ASC
        """)
    List<Object[]> findSubFoldersByParentIdWithMember(@Param("folderId") Long folderId,
                                                      @Param("search") String search,
                                                      @Param("sort") String sort,
                                                      @Param("uploaderId") Long uploaderId);

    // 특정 폴더의 파일 목록 조회 (워크스페이스 멤버 정보 포함)
    @Query("""
        SELECT fi.id, fi.name, fi.createdAt, fi.updatedAt, fi.size, fi.objectKey, CAST(fi.type AS string),
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM File fi
        JOIN WorkspaceMember wm ON fi.workspaceMemberId = wm.id
        WHERE fi.folderId = :folderId
        AND fi.deletedAt IS NULL
        AND (:search IS NULL OR fi.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        ORDER BY
            CASE WHEN :sort = 'latest' THEN fi.updatedAt END DESC,
            CASE WHEN :sort = 'alphabet' THEN fi.name END ASC
        """)
    List<Object[]> findFilesByFolderIdWithMember(@Param("folderId") Long folderId,
                                                 @Param("search") String search,
                                                 @Param("sort") String sort,
                                                 @Param("uploaderId") Long uploaderId);

    // 커서 기반 페이징을 위한 폴더 조회 (latest 정렬)
    @Query("""
        SELECT f.id, f.name, f.createdAt, f.updatedAt,
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM Folder f
        JOIN WorkspaceMember wm ON f.workspaceMemberId = wm.id
        WHERE f.parentId = :folderId
        AND f.deletedAt IS NULL
        AND (:search IS NULL OR f.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        AND (:cursor IS NULL OR f.updatedAt < :cursor)
        ORDER BY f.updatedAt DESC
        """)
    List<Object[]> findSubFoldersByParentIdWithCursorLatest(@Param("folderId") Long folderId,
                                                           @Param("search") String search,
                                                           @Param("uploaderId") Long uploaderId,
                                                           @Param("cursor") java.time.LocalDateTime cursor,
                                                           org.springframework.data.domain.Pageable pageable);

    // 커서 기반 페이징을 위한 폴더 조회 (alphabet 정렬)
    @Query("""
        SELECT f.id, f.name, f.createdAt, f.updatedAt,
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM Folder f
        JOIN WorkspaceMember wm ON f.workspaceMemberId = wm.id
        WHERE f.parentId = :folderId
        AND f.deletedAt IS NULL
        AND (:search IS NULL OR f.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        AND (:cursor IS NULL OR f.name > :cursor)
        ORDER BY f.name ASC
        """)
    List<Object[]> findSubFoldersByParentIdWithCursorAlphabet(@Param("folderId") Long folderId,
                                                             @Param("search") String search,
                                                             @Param("uploaderId") Long uploaderId,
                                                             @Param("cursor") String cursor,
                                                             org.springframework.data.domain.Pageable pageable);

    // 커서 기반 페이징을 위한 파일 조회 (latest 정렬)
    @Query("""
        SELECT fi.id, fi.name, fi.createdAt, fi.updatedAt, fi.size, fi.objectKey, CAST(fi.type AS string),
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM File fi
        JOIN WorkspaceMember wm ON fi.workspaceMemberId = wm.id
        WHERE fi.folderId = :folderId
        AND fi.deletedAt IS NULL
        AND (:search IS NULL OR fi.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        AND (:cursor IS NULL OR fi.updatedAt < :cursor)
        ORDER BY fi.updatedAt DESC
        """)
    List<Object[]> findFilesByFolderIdWithCursorLatest(@Param("folderId") Long folderId,
                                                      @Param("search") String search,
                                                      @Param("uploaderId") Long uploaderId,
                                                      @Param("cursor") java.time.LocalDateTime cursor,
                                                      org.springframework.data.domain.Pageable pageable);

    // 커서 기반 페이징을 위한 파일 조회 (alphabet 정렬)
    @Query("""
        SELECT fi.id, fi.name, fi.createdAt, fi.updatedAt, fi.size, fi.objectKey, CAST(fi.type AS string),
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM File fi
        JOIN WorkspaceMember wm ON fi.workspaceMemberId = wm.id
        WHERE fi.folderId = :folderId
        AND fi.deletedAt IS NULL
        AND (:search IS NULL OR fi.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        AND (:cursor IS NULL OR fi.name > :cursor)
        ORDER BY fi.name ASC
        """)
    List<Object[]> findFilesByFolderIdWithCursorAlphabet(@Param("folderId") Long folderId,
                                                        @Param("search") String search,
                                                        @Param("uploaderId") Long uploaderId,
                                                        @Param("cursor") String cursor,
                                                        org.springframework.data.domain.Pageable pageable);

    // 워크스페이스 휴지통 - 삭제된 폴더 목록 조회 (워크스페이스 멤버 정보 포함)
    @Query("""
        SELECT f.id, f.name, f.createdAt, f.updatedAt,
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM Folder f
        JOIN WorkspaceMember wm ON f.workspaceMemberId = wm.id
        WHERE f.workspaceId = :workspaceId
        AND f.deletedAt IS NOT NULL
        AND (:search IS NULL OR f.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        ORDER BY
            CASE WHEN :sort = 'latest' THEN f.deletedAt END DESC,
            CASE WHEN :sort = 'alphabet' THEN f.name END ASC
        """)
    List<Object[]> findTrashFoldersByWorkspaceIdWithMember(@Param("workspaceId") Long workspaceId,
                                                           @Param("search") String search,
                                                           @Param("sort") String sort,
                                                           @Param("uploaderId") Long uploaderId);

    // 워크스페이스 휴지통 - 삭제된 파일 목록 조회 (워크스페이스 멤버 정보 포함)
    @Query("""
        SELECT fi.id, fi.name, fi.createdAt, fi.updatedAt, fi.size, fi.objectKey, CAST(fi.type AS string),
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM File fi
        JOIN WorkspaceMember wm ON fi.workspaceMemberId = wm.id
        JOIN Folder f ON fi.folderId = f.id
        WHERE f.workspaceId = :workspaceId
        AND fi.deletedAt IS NOT NULL
        AND (:search IS NULL OR fi.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        ORDER BY
            CASE WHEN :sort = 'latest' THEN fi.deletedAt END DESC,
            CASE WHEN :sort = 'alphabet' THEN fi.name END ASC
        """)
    List<Object[]> findTrashFilesByWorkspaceIdWithMember(@Param("workspaceId") Long workspaceId,
                                                         @Param("search") String search,
                                                         @Param("sort") String sort,
                                                         @Param("uploaderId") Long uploaderId);

    // 커서 기반 페이징을 위한 휴지통 폴더 조회 (latest 정렬)
    @Query("""
        SELECT f.id, f.name, f.createdAt, f.updatedAt,
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM Folder f
        JOIN WorkspaceMember wm ON f.workspaceMemberId = wm.id
        WHERE f.workspaceId = :workspaceId
        AND f.deletedAt IS NOT NULL
        AND (:search IS NULL OR f.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        AND (:cursor IS NULL OR f.deletedAt < :cursor)
        ORDER BY f.deletedAt DESC
        """)
    List<Object[]> findTrashFoldersByWorkspaceIdWithCursorLatest(@Param("workspaceId") Long workspaceId,
                                                                @Param("search") String search,
                                                                @Param("uploaderId") Long uploaderId,
                                                                @Param("cursor") java.time.LocalDateTime cursor,
                                                                org.springframework.data.domain.Pageable pageable);

    // 커서 기반 페이징을 위한 휴지통 폴더 조회 (alphabet 정렬)
    @Query("""
        SELECT f.id, f.name, f.createdAt, f.updatedAt,
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM Folder f
        JOIN WorkspaceMember wm ON f.workspaceMemberId = wm.id
        WHERE f.workspaceId = :workspaceId
        AND f.deletedAt IS NOT NULL
        AND (:search IS NULL OR f.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        AND (:cursor IS NULL OR f.name > :cursor)
        ORDER BY f.name ASC
        """)
    List<Object[]> findTrashFoldersByWorkspaceIdWithCursorAlphabet(@Param("workspaceId") Long workspaceId,
                                                                  @Param("search") String search,
                                                                  @Param("uploaderId") Long uploaderId,
                                                                  @Param("cursor") String cursor,
                                                                  org.springframework.data.domain.Pageable pageable);

    // 커서 기반 페이징을 위한 휴지통 파일 조회 (latest 정렬)
    @Query("""
        SELECT fi.id, fi.name, fi.createdAt, fi.updatedAt, fi.size, fi.objectKey, CAST(fi.type AS string),
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM File fi
        JOIN WorkspaceMember wm ON fi.workspaceMemberId = wm.id
        JOIN Folder f ON fi.folderId = f.id
        WHERE f.workspaceId = :workspaceId
        AND fi.deletedAt IS NOT NULL
        AND (:search IS NULL OR fi.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        AND (:cursor IS NULL OR fi.deletedAt < :cursor)
        ORDER BY fi.deletedAt DESC
        """)
    List<Object[]> findTrashFilesByWorkspaceIdWithCursorLatest(@Param("workspaceId") Long workspaceId,
                                                              @Param("search") String search,
                                                              @Param("uploaderId") Long uploaderId,
                                                              @Param("cursor") java.time.LocalDateTime cursor,
                                                              org.springframework.data.domain.Pageable pageable);

    // 커서 기반 페이징을 위한 휴지통 파일 조회 (alphabet 정렬)
    @Query("""
        SELECT fi.id, fi.name, fi.createdAt, fi.updatedAt, fi.size, fi.objectKey, CAST(fi.type AS string),
               wm.id as wmId, wm.name as wmName, wm.profileImage as wmProfileImage
        FROM File fi
        JOIN WorkspaceMember wm ON fi.workspaceMemberId = wm.id
        JOIN Folder f ON fi.folderId = f.id
        WHERE f.workspaceId = :workspaceId
        AND fi.deletedAt IS NOT NULL
        AND (:search IS NULL OR fi.name LIKE %:search%)
        AND (:uploaderId IS NULL OR wm.id = :uploaderId)
        AND (:cursor IS NULL OR fi.name > :cursor)
        ORDER BY fi.name ASC
        """)
    List<Object[]> findTrashFilesByWorkspaceIdWithCursorAlphabet(@Param("workspaceId") Long workspaceId,
                                                                @Param("search") String search,
                                                                @Param("uploaderId") Long uploaderId,
                                                                @Param("cursor") String cursor,
                                                                org.springframework.data.domain.Pageable pageable);
}
