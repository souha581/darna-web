package com.darna.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * Maintenance / repair request reported by a resident and handled by an assigned worker.
 */
@Entity
@Table(name = "repair_requests")
public class RepairRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "worker_id")
    private AppUser assignedWorker;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String category;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String priority;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepairStatus status = RepairStatus.ENVOYEE;

    private Double interventionCostTnd;

    @Column(length = 400)
    private String closingReport;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RepairRequest() {
    }

    public RepairRequest(Apartment apartment, AppUser resident, String category, String priority, String description) {
        this.apartment = apartment;
        this.resident = resident;
        this.category = category;
        this.priority = priority;
        this.description = description;
        this.status = RepairStatus.ENVOYEE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Apartment getApartment() {
        return apartment;
    }

    public void setApartment(Apartment apartment) {
        this.apartment = apartment;
    }

    public AppUser getResident() {
        return resident;
    }

    public void setResident(AppUser resident) {
        this.resident = resident;
    }

    public AppUser getAssignedWorker() {
        return assignedWorker;
    }

    public void setAssignedWorker(AppUser assignedWorker) {
        this.assignedWorker = assignedWorker;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }

    public Double getInterventionCostTnd() {
        return interventionCostTnd;
    }

    public void setInterventionCostTnd(Double interventionCostTnd) {
        this.interventionCostTnd = interventionCostTnd;
    }

    public String getClosingReport() {
        return closingReport;
    }

    public void setClosingReport(String closingReport) {
        this.closingReport = closingReport;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
