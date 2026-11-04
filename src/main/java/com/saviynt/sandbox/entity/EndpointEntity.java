package com.saviynt.sandbox.entity;

import jakarta.persistence.*;

/**
 * JPA Entity for Application Endpoint Configuration (endpoints table).
 * Maps existing schema with zero DDL generation.
 */
@Entity
@Table(name = "endpoints")
public class EndpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "endpointkey")
    private Long endpointkey;

    @Column(name = "endpointname", unique = true, nullable = false)
    private String endpointname;

    @Column(name = "systemkey")
    private Long systemkey;

    @Column(name = "accountnamerule")
    private String accountnamerule;

    @Column(name = "correlationrule")
    private String correlationrule;

    @Column(name = "preprovisiongroovy", columnDefinition = "TEXT")
    private String preprovisiongroovy;

    @Transient
    private String status = "Active";

    public EndpointEntity() {
    }

    public EndpointEntity(Long endpointkey, String endpointname, Long systemkey,
                          String accountnamerule, String correlationrule, String preprovisiongroovy) {
        this.endpointkey = endpointkey;
        this.endpointname = endpointname;
        this.systemkey = systemkey;
        this.accountnamerule = accountnamerule;
        this.correlationrule = correlationrule;
        this.preprovisiongroovy = preprovisiongroovy;
        this.status = "Active";
    }

    public Long getEndpointkey() {
        return endpointkey;
    }

    public void setEndpointkey(Long endpointkey) {
        this.endpointkey = endpointkey;
    }

    public String getEndpointname() {
        return endpointname;
    }

    public void setEndpointname(String endpointname) {
        this.endpointname = endpointname;
    }

    public Long getSystemkey() {
        return systemkey;
    }

    public void setSystemkey(Long systemkey) {
        this.systemkey = systemkey;
    }

    public String getAccountnamerule() {
        return accountnamerule;
    }

    public void setAccountnamerule(String accountnamerule) {
        this.accountnamerule = accountnamerule;
    }

    public String getCorrelationrule() {
        return correlationrule;
    }

    public void setCorrelationrule(String correlationrule) {
        this.correlationrule = correlationrule;
    }

    public String getPreprovisiongroovy() {
        return preprovisiongroovy;
    }

    public void setPreprovisiongroovy(String preprovisiongroovy) {
        this.preprovisiongroovy = preprovisiongroovy;
    }

    public String getStatus() {
        return status != null ? status : "Active";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long endpointkey() {
        return endpointkey;
    }

    public String endpointname() {
        return endpointname;
    }

    public Long systemkey() {
        return systemkey;
    }
}
