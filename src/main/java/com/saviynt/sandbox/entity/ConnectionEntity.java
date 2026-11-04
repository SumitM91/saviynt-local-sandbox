package com.saviynt.sandbox.entity;

import jakarta.persistence.*;

/**
 * JPA Entity for Connection Configuration (connections table).
 * Maps existing schema with zero DDL generation.
 */
@Entity
@Table(name = "connections")
public class ConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connectionkey")
    private Long connectionkey;

    @Column(name = "connectionname", unique = true, nullable = false)
    private String connectionname;

    @Column(name = "connectiontype")
    private String connectiontype;

    @Column(name = "connectionjson", columnDefinition = "TEXT")
    private String connectionjson;

    @Column(name = "createaccountjson", columnDefinition = "TEXT")
    private String createaccountjson;

    @Column(name = "addaccessjson", columnDefinition = "TEXT")
    private String addaccessjson;

    @Column(name = "accountimportjson", columnDefinition = "TEXT")
    private String accountimportjson;

    public ConnectionEntity() {
    }

    public ConnectionEntity(Long connectionkey, String connectionname, String connectiontype,
                            String connectionjson, String createaccountjson, String addaccessjson,
                            String accountimportjson) {
        this.connectionkey = connectionkey;
        this.connectionname = connectionname;
        this.connectiontype = connectiontype;
        this.connectionjson = connectionjson;
        this.createaccountjson = createaccountjson;
        this.addaccessjson = addaccessjson;
        this.accountimportjson = accountimportjson;
    }

    public Long getConnectionkey() {
        return connectionkey;
    }

    public void setConnectionkey(Long connectionkey) {
        this.connectionkey = connectionkey;
    }

    public String getConnectionname() {
        return connectionname;
    }

    public void setConnectionname(String connectionname) {
        this.connectionname = connectionname;
    }

    public String getConnectiontype() {
        return connectiontype;
    }

    public void setConnectiontype(String connectiontype) {
        this.connectiontype = connectiontype;
    }

    public String getConnectionjson() {
        return connectionjson;
    }

    public void setConnectionjson(String connectionjson) {
        this.connectionjson = connectionjson;
    }

    public String getCreateaccountjson() {
        return createaccountjson;
    }

    public void setCreateaccountjson(String createaccountjson) {
        this.createaccountjson = createaccountjson;
    }

    public String getAddaccessjson() {
        return addaccessjson;
    }

    public void setAddaccessjson(String addaccessjson) {
        this.addaccessjson = addaccessjson;
    }

    public String getAccountimportjson() {
        return accountimportjson;
    }

    public void setAccountimportjson(String accountimportjson) {
        this.accountimportjson = accountimportjson;
    }

    public Long connectionkey() {
        return connectionkey;
    }

    public String connectionname() {
        return connectionname;
    }

    public String connectiontype() {
        return connectiontype;
    }
}
