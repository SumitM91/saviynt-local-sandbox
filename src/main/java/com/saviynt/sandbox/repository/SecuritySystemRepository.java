package com.saviynt.sandbox.repository;

import com.saviynt.sandbox.entity.SecuritySystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Security System entities (securitysystems).
 */
@Repository
public interface SecuritySystemRepository extends JpaRepository<SecuritySystemEntity, Long> {
    Optional<SecuritySystemEntity> findBySystemname(String systemname);
}
