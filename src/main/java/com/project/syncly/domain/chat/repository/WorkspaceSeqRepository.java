package com.project.syncly.domain.chat.repository;

import com.project.syncly.domain.chat.entity.WorkspaceSeq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface WorkspaceSeqRepository extends JpaRepository<WorkspaceSeq, Long> {
    // MySQL: 증가 + 증가값을 LAST_INSERT_ID()로 설정
    @Modifying
    @Query(value = """
      UPDATE workspace_seq
      SET current_seq = LAST_INSERT_ID(current_seq + 1)
      WHERE workspace_id = :wsId
      """, nativeQuery = true)
    int bumpAndSetLastInsertId(@Param("wsId") long wsId);

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    long fetchLastInsertId();
}


