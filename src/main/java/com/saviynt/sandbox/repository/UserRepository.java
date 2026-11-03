package com.saviynt.sandbox.repository;

import com.saviynt.sandbox.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Identity Repository entities (users).
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByUsername(String username);

    Optional<UserEntity> findByUsername(String username);

    default Optional<UserEntity> findFirstUser() {
        return findAll().stream().findFirst();
    }

    @Query(value = """
        SELECT u.* FROM users u
        INNER JOIN user_accounts ua ON u.userkey = ua.userkey
        WHERE u.statuskey = 1
        LIMIT 1
        """, nativeQuery = true)
    Optional<UserEntity> findFirstActiveUserWithLinkedAccounts();

    default long createUser(String username, String firstname, String lastname, String email,
                           String departmentname, String employeetype, String manager,
                           Long orgkey, int statuskey, String employeeId) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setEmail(email);
        user.setDepartmentname(departmentname);
        user.setEmployeetype(employeetype);
        user.setManager(manager);
        user.setOrgkey(orgkey);
        user.setStatuskey(statuskey);
        user.setCustomproperty1(employeeId);
        return save(user).getUserkey();
    }

    @Modifying
    @Query("UPDATE UserEntity u SET u.departmentname = :dept, u.manager = :mgr WHERE u.userkey = :userKey")
    int updateDepartmentAndManager(@Param("userKey") long userKey, @Param("dept") String dept, @Param("mgr") String mgr);

    default int updateDepartmentAndManager(long userKey, String departmentname) {
        return updateDepartmentAndManager(userKey, departmentname, null);
    }

    @Modifying
    @Query("UPDATE UserEntity u SET u.statuskey = 0, u.termdate = CURRENT_DATE WHERE u.userkey = :userKey")
    int terminateUser(@Param("userKey") long userKey);
}
