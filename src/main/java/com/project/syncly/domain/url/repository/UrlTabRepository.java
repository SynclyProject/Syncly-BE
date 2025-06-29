package com.project.syncly.domain.url.repository;

import com.project.syncly.domain.url.entity.UrlTab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlTabRepository extends JpaRepository<UrlTab, Long> {

}
