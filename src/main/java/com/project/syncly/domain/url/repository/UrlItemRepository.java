package com.project.syncly.domain.url.repository;

import com.project.syncly.domain.url.entity.UrlItem;
import com.project.syncly.domain.url.entity.UrlTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlItemRepository extends JpaRepository<UrlItem, Long> {
}
