package com.saviynt.sandbox.repository;

import com.saviynt.sandbox.dto.OrphanAccountResponse;
import com.saviynt.sandbox.dto.TerminatedViolationResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Enterprise DAO implementing high-volume set-based joins using JdbcTemplate
 * for Saviynt Analytics V2 (Orphan Accounts Detection, Bulk Account Correlation,
 * and Terminated User Violations).
 */
@Repository
public class CorrelationDao {

    private final JdbcTemplate jdbcTemplate;

    public CorrelationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes set-based query to find unlinked / orphan accounts in target endpoints.
     */
    public List<OrphanAccountResponse> findOrphanAccounts() {
        String sql = """
            SELECT 
                e.endpointname AS Endpoint,
                a.accountkey AS AccountKey,
                a.name AS AccountName,
                a.customproperty1 AS Target_EmpID,
                a.accounttype AS AccountType
            FROM accounts a
            INNER JOIN endpoints e ON a.endpointkey = e.endpointkey
            LEFT JOIN user_accounts ua ON a.accountkey = ua.accountkey
            WHERE ua.userkey IS NULL
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OrphanAccountResponse(
                rs.getString("Endpoint"),
                rs.getLong("AccountKey"),
                rs.getString("AccountName"),
                rs.getString("Target_EmpID"),
                rs.getString("AccountType")
        ));
    }

    /**
     * High-performance set-based bulk insert correlating accounts to users via Employee ID.
     */
    public int runAccountCorrelation() {
        String sql = """
            INSERT IGNORE INTO user_accounts (userkey, accountkey)
            SELECT u.userkey, a.accountkey
            FROM accounts a
            INNER JOIN users u ON a.customproperty1 = u.customproperty1
            LEFT JOIN user_accounts ua ON a.accountkey = ua.accountkey
            WHERE ua.userkey IS NULL
            """;
        return jdbcTemplate.update(sql);
    }

    /**
     * Compliance Audit: Detects terminated identities (statuskey=0) that still have active accounts (status=1).
     */
    public List<TerminatedViolationResponse> findTerminatedUsersWithActiveAccounts() {
        String sql = """
            SELECT 
                u.username AS Terminated_User,
                u.statuskey AS User_StatusKey,
                a.name AS Active_Target_Account,
                e.endpointname AS Endpoint,
                a.status AS Account_Status
            FROM users u
            INNER JOIN user_accounts ua ON u.userkey = ua.userkey
            INNER JOIN accounts a ON ua.accountkey = a.accountkey
            INNER JOIN endpoints e ON a.endpointkey = e.endpointkey
            WHERE u.statuskey = 0 AND a.status = 1
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TerminatedViolationResponse(
                rs.getString("Terminated_User"),
                rs.getInt("User_StatusKey"),
                rs.getString("Active_Target_Account"),
                rs.getString("Endpoint"),
                rs.getInt("Account_Status")
        ));
    }
}
