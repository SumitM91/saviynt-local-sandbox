package com.saviynt.sandbox.entity;

import jakarta.persistence.*;

/**
 * JPA Entity for Security System Configuration (securitysystems table).
 * Maps existing schema with zero DDL generation.
 */
@Entity
@Table(name = "securitysystems")
public class SecuritySystemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "systemkey")
    private Long systemkey;

    @Column(name = "systemname", unique = true, nullable = false)
    private String systemname;

    @Column(name = "connectionkey")
    private Long connectionkey;

    @Column(name = "accessaddworkflow")
    private String accessaddworkflow;

    @Column(name = "provisioningtries")
    private Integer provisioningtries;

    @Transient
    private String status = "Active";

    public SecuritySystemEntity() {
    }

    public SecuritySystemEntity(Long systemkey, String systemname, Long connectionkey,
                                String accessaddworkflow, Integer provisioningtries) {
        this.systemkey = systemkey;
        this.systemname = systemname;
        this.connectionkey = connectionkey;
        this.accessaddworkflow = accessaddworkflow;
        this.provisioningtries = provisioningtries;
        this.status = "Active";
    }

    public Long getSystemkey() {
        return systemkey;
    }

    public void setSystemkey(Long systemkey) {
        this.systemkey = systemkey;
    }

    public String getSystemname() {
        return systemname;
    }

    public void setSystemname(String systemname) {
        this.systemname = systemname;
    }

    public Long getConnectionkey() {
        return connectionkey;
    }

    public void setConnectionkey(Long connectionkey) {
        this.connectionkey = connectionkey;
    }

    public String getAccessaddworkflow() {
        return accessaddworkflow;
    }

    public void setAccessaddworkflow(String accessaddworkflow) {
        this.accessaddworkflow = accessaddworkflow;
    }

    public Integer getProvisioningtries() {
        return provisioningtries;
    }

    public void setProvisioningtries(Integer provisioningtries) {
        this.provisioningtries = provisioningtries;
    }

    public String getStatus() {
        return status != null ? status : "Active";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long systemkey() {
        return systemkey;
    }

    public String systemname() {
        return systemname;
    }

    public Long connectionkey() {
        return connectionkey;
    }
}
