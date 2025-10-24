package com.project.syncly.domain.note.repository;

import com.project.syncly.domain.note.entity.NoteImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoteImageRepository extends JpaRepository<NoteImage, Long> {

    /**
     * 특정 노트의 이미지 목록 조회
     */
    @Query("SELECT ni FROM NoteImage ni WHERE ni.note.id = :noteId ORDER BY ni.createdAt DESC")
    List<NoteImage> findByNoteId(@Param("noteId") Long noteId);

    /**
     * 특정 노트의 이미지 개수 조회
     */
    @Query("SELECT COUNT(ni) FROM NoteImage ni WHERE ni.note.id = :noteId")
    Long countByNoteId(@Param("noteId") Long noteId);

    /**
     * Object Key로 이미지 조회 (중복 업로드 체크용)
     */
    Optional<NoteImage> findByObjectKey(String objectKey);

    /**
     * 특정 사용자가 업로드한 이미지 목록 조회
     */
    @Query("SELECT ni FROM NoteImage ni WHERE ni.uploader.id = :uploaderId ORDER BY ni.createdAt DESC")
    List<NoteImage> findByUploaderId(@Param("uploaderId") Long uploaderId);

    /**
     * 노트와 업로더로 이미지 조회
     */
    @Query("SELECT ni FROM NoteImage ni WHERE ni.note.id = :noteId AND ni.uploader.id = :uploaderId")
    List<NoteImage> findByNoteIdAndUploaderId(@Param("noteId") Long noteId, @Param("uploaderId") Long uploaderId);

    /**
     * 만료된 PENDING 상태의 이미지 조회 (스케줄러용)
     */
    @Query("SELECT ni FROM NoteImage ni WHERE ni.uploadStatus = 'PENDING' AND ni.expiresAt < :now")
    List<NoteImage> findExpiredPendingImages(@Param("now") LocalDateTime now);

    /**
     * 노트 ID와 이미지 ID로 조회
     */
    @Query("SELECT ni FROM NoteImage ni WHERE ni.id = :imageId AND ni.note.id = :noteId")
    Optional<NoteImage> findByIdAndNoteId(@Param("imageId") Long imageId, @Param("noteId") Long noteId);
}
