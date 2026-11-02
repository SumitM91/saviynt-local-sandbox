package com.saviynt.sandbox.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EndpointRepository {

    private final JdbcTemplate jdbcTemplate;

    public EndpointRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record EndpointEntity(
            long endpointkey,
            String endpointname
    ) {}

    public Optional<EndpointEntity> findFirstEndpoint() {
        List<EndpointEntity> list = jdbcTemplate.query(
                "SELECT endpointkey, endpointname FROM endpoints ORDER BY endpointkey LIMIT 1",
                (rs, rowNum) -> new EndpointEntity(
                        rs.getLong("endpointkey"),
                        rs.getString("endpointname")
                )
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }
}
