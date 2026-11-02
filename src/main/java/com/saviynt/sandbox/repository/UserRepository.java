package com.saviynt.sandbox.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record UserEntity(
            long userkey,
            String username,
            String firstname,
            String lastname,
            String email,
            String departmentname,
            String employeetype,
            String manager,
            Long orgkey,
            int statuskey,
            String customproperty1
    ) {}

    private final RowMapper<UserEntity> userRowMapper = (rs, rowNum) -> new UserEntity(
            rs.getLong("userkey"),
            rs.getString("username"),
            rs.getString("firstname"),
            rs.getString("lastname"),
            rs.getString("email"),
            rs.getString("departmentname"),
            rs.getString("employeetype"),
            rs.getString("manager"),
            rs.getObject("orgkey") != null ? rs.getLong("orgkey") : null,
            rs.getInt("statuskey"),
            rs.getString("customproperty1")
    );

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    public Optional<UserEntity> findByUsername(String username) {
        try {
            UserEntity user = jdbcTemplate.queryForObject(
                    "SELECT userkey, username, firstname, lastname, email, departmentname, employeetype, manager, orgkey, statuskey, customproperty1 FROM users WHERE username = ?",
                    userRowMapper,
                    username
            );
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<UserEntity> findFirstUser() {
        List<UserEntity> list = jdbcTemplate.query(
                "SELECT userkey, username, firstname, lastname, email, departmentname, employeetype, manager, orgkey, statuskey, customproperty1 FROM users LIMIT 1",
                userRowMapper
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    public Optional<UserEntity> findFirstActiveUserWithLinkedAccounts() {
        String sql = """
            SELECT u.userkey, u.username, u.firstname, u.lastname, u.email, u.departmentname, u.employeetype, u.manager, u.orgkey, u.statuskey, u.customproperty1
            FROM users u
            INNER JOIN user_accounts ua ON u.userkey = ua.userkey
            WHERE u.statuskey = 1
            LIMIT 1
            """;
        List<UserEntity> list = jdbcTemplate.query(sql, userRowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    public long createUser(String username, String firstname, String lastname, String email,
                           String departmentname, String employeetype, String manager,
                           Long orgkey, int statuskey, String employeeId) {
        String sql = """
            INSERT INTO users (username, firstname, lastname, email, departmentname, employeetype, manager, orgkey, statuskey, startdate, customproperty1)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE(), ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, firstname);
            ps.setString(3, lastname);
            ps.setString(4, email);
            ps.setString(5, departmentname);
            ps.setString(6, employeetype);
            ps.setString(7, manager);
            ps.setObject(8, orgkey);
            ps.setInt(9, statuskey);
            ps.setString(10, employeeId);
            return ps;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public int updateDepartmentAndManager(long userKey, String departmentname, String manager) {
        if (manager != null) {
            return jdbcTemplate.update(
                    "UPDATE users SET departmentname = ?, manager = ? WHERE userkey = ?",
                    departmentname, manager, userKey
            );
        } else {
            return jdbcTemplate.update(
                    "UPDATE users SET departmentname = ? WHERE userkey = ?",
                    departmentname, userKey
            );
        }
    }

    public int terminateUser(long userKey) {
        return jdbcTemplate.update(
                "UPDATE users SET statuskey = 0, termdate = CURRENT_DATE() WHERE userkey = ?",
                userKey
        );
    }
}
