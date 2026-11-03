package com.saviynt.sandbox.repository;

import com.saviynt.sandbox.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Target Account entities (accounts).
 */
@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    record LinkedAccountEntity(
            long accountkey,
            String accountname,
            Long endpointkey,
            String endpointname,
            int status
    ) {}

    Optional<AccountEntity> findByAccountname(String accountname);

    @Query(value = """
        SELECT 
            a.accountkey, 
            a.name AS accountname, 
            a.endpointkey, 
            a.status, 
            e.endpointname
        FROM user_accounts ua
        INNER JOIN accounts a ON ua.accountkey = a.accountkey
        LEFT JOIN endpoints e ON a.endpointkey = e.endpointkey
        WHERE ua.userkey = :userKey
        """, nativeQuery = true)
    List<Map<String, Object>> findAccountsByUserKeyRaw(@Param("userKey") long userKey);

    default List<LinkedAccountEntity> findAccountsByUserKey(long userKey) {
        return findAccountsByUserKeyRaw(userKey).stream()
                .map(row -> new LinkedAccountEntity(
                        ((Number) row.get("accountkey")).longValue(),
                        (String) row.get("accountname"),
                        row.get("endpointkey") != null ? ((Number) row.get("endpointkey")).longValue() : null,
                        (String) row.get("endpointname"),
                        ((Number) row.get("status")).intValue()
                ))
                .toList();
    }

    @Modifying
    @Query("UPDATE AccountEntity a SET a.status = :status WHERE a.accountkey = :accountKey")
    int updateAccountStatus(@Param("accountKey") long accountKey, @Param("status") int status);
}
