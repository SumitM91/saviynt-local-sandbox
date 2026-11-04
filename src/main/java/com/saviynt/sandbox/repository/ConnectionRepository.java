package com.saviynt.sandbox.repository;

import com.saviynt.sandbox.entity.ConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Target Connection entities (connections).
 */
@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionEntity, Long> {
    Optional<ConnectionEntity> findByConnectionname(String connectionname);
}
