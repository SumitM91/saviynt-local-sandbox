package com.saviynt.sandbox.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for Target Accounts (accounts table).
 * Maps existing schema with zero DDL generation.
 */
@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accountkey")
    private Long accountkey;

    @Column(name = "name", nullable = false)
    private String accountname;

    @Column(name = "endpointkey")
    private Long endpointkey;

    @Transient
    private String endpoint;

    @Column(name = "status")
    private Integer status;

    @Column(name = "accounttype")
    private String accounttype;

    @Column(name = "customproperty1")
    private String customproperty1;

    @Column(name = "lastlogondate")
    private LocalDateTime lastlogondate;

    public AccountEntity() {
    }

    public AccountEntity(Long accountkey, String accountname, Long endpointkey,
                         Integer status, String customproperty1) {
        this.accountkey = accountkey;
        this.accountname = accountname;
        this.endpointkey = endpointkey;
        this.status = status;
        this.customproperty1 = customproperty1;
    }

    public Long getAccountkey() {
        return accountkey;
    }

    public void setAccountkey(Long accountkey) {
        this.accountkey = accountkey;
    }

    public String getAccountname() {
        return accountname;
    }

    public void setAccountname(String accountname) {
        this.accountname = accountname;
    }

    public String getName() {
        return accountname;
    }

    public void setName(String name) {
        this.accountname = name;
    }

    public Long getEndpointkey() {
        return endpointkey;
    }

    public void setEndpointkey(Long endpointkey) {
        this.endpointkey = endpointkey;
    }

    public String getEndpoint() {
        return endpoint != null ? endpoint : (endpointkey != null ? String.valueOf(endpointkey) : null);
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        if (endpoint != null && endpoint.matches("\\d+")) {
            this.endpointkey = Long.parseLong(endpoint);
        }
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAccounttype() {
        return accounttype;
    }

    public void setAccounttype(String accounttype) {
        this.accounttype = accounttype;
    }

    public String getCustomproperty1() {
        return customproperty1;
    }

    public void setCustomproperty1(String customproperty1) {
        this.customproperty1 = customproperty1;
    }

    public LocalDateTime getLastlogondate() {
        return lastlogondate;
    }

    public void setLastlogondate(LocalDateTime lastlogondate) {
        this.lastlogondate = lastlogondate;
    }

    // Fluent accessors
    public Long accountkey() {
        return accountkey;
    }

    public String accountname() {
        return accountname;
    }

    public Integer status() {
        return status;
    }

    public String customproperty1() {
        return customproperty1;
    }
}
