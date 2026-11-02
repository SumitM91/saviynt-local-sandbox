package com.saviynt.sandbox.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
public class ArsTaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public ArsTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createTask(int taskType, String taskDescription, Long userKey, Long accountKey, Long endpointKey, int status, String comments) {
        String sql = """
            INSERT INTO arstasks (tasktype, taskdescription, userkey, accountkey, endpointkey, status, comments)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, taskType);
            ps.setString(2, taskDescription);
            if (userKey != null) {
                ps.setLong(3, userKey);
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            if (accountKey != null) {
                ps.setLong(4, accountKey);
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            if (endpointKey != null) {
                ps.setLong(5, endpointKey);
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setInt(6, status);
            ps.setString(7, comments);
            return ps;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public List<Map<String, Object>> findAllTasksWithDetails() {
        String sql = """
            SELECT 
                t.taskkey,
                t.tasktype,
                CASE t.tasktype
                    WHEN 1 THEN 'Add Access (Birthright)'
                    WHEN 2 THEN 'Remove Access'
                    WHEN 3 THEN 'New Account'
                    WHEN 12 THEN 'Disable Account'
                    ELSE CONCAT('TaskType-', t.tasktype)
                END AS tasktypeName,
                t.taskdescription,
                t.userkey,
                u.username,
                t.accountkey,
                a.name AS accountname,
                t.endpointkey,
                e.endpointname,
                t.status,
                t.comments,
                t.createdate
            FROM arstasks t
            LEFT JOIN users u ON t.userkey = u.userkey
            LEFT JOIN accounts a ON t.accountkey = a.accountkey
            LEFT JOIN endpoints e ON t.endpointkey = e.endpointkey
            ORDER BY t.taskkey DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }
}
