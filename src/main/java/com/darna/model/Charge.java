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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Co-ownership charge issued for an apartment, supporting partial payments in TND.
 */
@Entity
@Table(name = "charges")
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String label;

    @NotBlank
    @Column(nullable = false, length = 40)
    private String period;

    @Min(1)
    @Column(nullable = false)
    private double totalAmountTnd;

    @Min(0)
    @Column(nullable = false)
    private double paidAmountTnd = 0.0;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeStatus status = ChargeStatus.NON_PAYE;

    @Column(length = 40)
    private String lastPaymentMode;

    public Charge() {
    }

    public Charge(Apartment apartment, String label, String period, double totalAmountTnd, LocalDate dueDate) {
        this.apartment = apartment;
        this.label = label;
        this.period = period;
        this.totalAmountTnd = totalAmountTnd;
        this.dueDate = dueDate;
        refreshStatus();
    }

    public double getRemainingAmountTnd() {
        return Math.max(0.0, totalAmountTnd - paidAmountTnd);
    }

    public void refreshStatus() {
        if (getRemainingAmountTnd() <= 0.001) {
            this.status = ChargeStatus.PAYE;
        } else if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            this.status = ChargeStatus.EN_RETARD;
        } else {
            this.status = ChargeStatus.NON_PAYE;
        }
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public double getTotalAmountTnd() {
        return totalAmountTnd;
    }

    public void setTotalAmountTnd(double totalAmountTnd) {
        this.totalAmountTnd = totalAmountTnd;
    }

    public double getPaidAmountTnd() {
        return paidAmountTnd;
    }

    public void setPaidAmountTnd(double paidAmountTnd) {
        this.paidAmountTnd = paidAmountTnd;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public void setStatus(ChargeStatus status) {
        this.status = status;
    }

    public String getLastPaymentMode() {
        return lastPaymentMode;
    }

    public void setLastPaymentMode(String lastPaymentMode) {
        this.lastPaymentMode = lastPaymentMode;
    }
}
