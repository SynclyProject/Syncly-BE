package com.project.syncly.domain.url.repository;

import com.project.syncly.domain.url.entity.UrlTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlTabRepository extends JpaRepository<UrlTab, Long> {

    @Query("SELECT DISTINCT t FROM UrlTab t LEFT JOIN FETCH t.urlItems i " +
            "WHERE t.workspace.id = :workspaceId " +
            "ORDER BY t.createdAt ASC, i.createdAt ASC")
    List<UrlTab> findAllWithUrlItemsByWorkspaceId(@Param("workspaceId") Long workspaceId);

}
