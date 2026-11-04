package com.saviynt.sandbox.repository;

import com.saviynt.sandbox.entity.EndpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Application Endpoint entities (endpoints).
 */
@Repository
public interface EndpointRepository extends JpaRepository<EndpointEntity, Long> {

    // Preserved nested record for backward-compatibility with Module4JmlService & Module4JmlServiceTest
    record EndpointEntity(
            long endpointkey,
            String endpointname
    ) {}

    default Optional<EndpointEntity> findFirstEndpoint() {
        return findAll().stream().findFirst()
                .map(e -> new EndpointEntity(e.getEndpointkey(), e.getEndpointname()));
    }

    Optional<com.saviynt.sandbox.entity.EndpointEntity> findByEndpointname(String endpointname);

    List<com.saviynt.sandbox.entity.EndpointEntity> findBySystemkey(Long systemkey);
}
