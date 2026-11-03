package com.saviynt.sandbox.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for Saviynt Identity Repository (users table).
 * Maps existing schema with zero DDL generation.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userkey")
    private Long userkey;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @Column(name = "email")
    private String email;

    @Column(name = "departmentname")
    private String departmentname;

    @Column(name = "employeetype")
    private String employeetype;

    @Column(name = "manager")
    private String manager;

    @Column(name = "orgkey")
    private Long orgkey;

    @Column(name = "statuskey")
    private Integer statuskey;

    @Column(name = "startdate")
    private LocalDate startdate;

    @Column(name = "termdate")
    private LocalDate termdate;

    @Column(name = "customproperty1")
    private String customproperty1;

    @Column(name = "createdate", insertable = false, updatable = false)
    private LocalDateTime createdate;

    public UserEntity() {
    }

    public UserEntity(Long userkey, String username, String firstname, String lastname, String email,
                      String departmentname, String employeetype, String manager, Long orgkey,
                      Integer statuskey, String customproperty1) {
        this.userkey = userkey;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.departmentname = departmentname;
        this.employeetype = employeetype;
        this.manager = manager;
        this.orgkey = orgkey;
        this.statuskey = statuskey;
        this.customproperty1 = customproperty1;
    }

    // Standard Getters and Setters
    public Long getUserkey() {
        return userkey;
    }

    public void setUserkey(Long userkey) {
        this.userkey = userkey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartmentname() {
        return departmentname;
    }

    public void setDepartmentname(String departmentname) {
        this.departmentname = departmentname;
    }

    public String getEmployeetype() {
        return employeetype;
    }

    public void setEmployeetype(String employeetype) {
        this.employeetype = employeetype;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public Long getOrgkey() {
        return orgkey;
    }

    public void setOrgkey(Long orgkey) {
        this.orgkey = orgkey;
    }

    public Integer getStatuskey() {
        return statuskey;
    }

    public void setStatuskey(Integer statuskey) {
        this.statuskey = statuskey;
    }

    public LocalDate getStartdate() {
        return startdate;
    }

    public void setStartdate(LocalDate startdate) {
        this.startdate = startdate;
    }

    public LocalDate getTermdate() {
        return termdate;
    }

    public void setTermdate(LocalDate termdate) {
        this.termdate = termdate;
    }

    public String getCustomproperty1() {
        return customproperty1;
    }

    public void setCustomproperty1(String customproperty1) {
        this.customproperty1 = customproperty1;
    }

    public LocalDateTime getCreatedate() {
        return createdate;
    }

    public void setCreatedate(LocalDateTime createdate) {
        this.createdate = createdate;
    }

    // Fluent accessors for backward-compatibility with record-style calls
    public Long userkey() {
        return userkey;
    }

    public String username() {
        return username;
    }

    public String firstname() {
        return firstname;
    }

    public String lastname() {
        return lastname;
    }

    public String email() {
        return email;
    }

    public String departmentname() {
        return departmentname;
    }

    public String employeetype() {
        return employeetype;
    }

    public String manager() {
        return manager;
    }

    public Long orgkey() {
        return orgkey;
    }

    public Integer statuskey() {
        return statuskey;
    }

    public String customproperty1() {
        return customproperty1;
    }
}
