package com.rahul.urlshortener.repository;

import com.rahul.urlshortener.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Transactional
    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + :delta where l.shortCode = :code")
    int incrementClickCount(@Param("code") String code, @Param("delta") long delta);
}
