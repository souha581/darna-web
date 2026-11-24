package com.darna.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents an apartment unit inside a residence and building.
 */
@Entity
@Table(name = "apartments")
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String residenceName;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String buildingName;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String unitNumber;

    @Min(0)
    @Column(nullable = false)
    private int floor;

    @Min(10)
    @Column(nullable = false)
    private double surfaceM2;

    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String typology;

    @Min(0)
    @Column(nullable = false)
    private double monthlyRentTnd;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean available = true;

    public Apartment() {
    }

    public Apartment(String residenceName,
                     String buildingName,
                     String unitNumber,
                     int floor,
                     double surfaceM2,
                     String typology,
                     double monthlyRentTnd,
                     String description,
                     boolean available) {
        this.residenceName = residenceName;
        this.buildingName = buildingName;
        this.unitNumber = unitNumber;
        this.floor = floor;
        this.surfaceM2 = surfaceM2;
        this.typology = typology;
        this.monthlyRentTnd = monthlyRentTnd;
        this.description = description;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResidenceName() {
        return residenceName;
    }

    public void setResidenceName(String residenceName) {
        this.residenceName = residenceName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public double getSurfaceM2() {
        return surfaceM2;
    }

    public void setSurfaceM2(double surfaceM2) {
        this.surfaceM2 = surfaceM2;
    }

    public String getTypology() {
        return typology;
    }

    public void setTypology(String typology) {
        this.typology = typology;
    }

    public double getMonthlyRentTnd() {
        return monthlyRentTnd;
    }

    public void setMonthlyRentTnd(double monthlyRentTnd) {
        this.monthlyRentTnd = monthlyRentTnd;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getFullLocation() {
        return residenceName + " · Bât. " + buildingName + " · Étage " + floor + " · Apt " + unitNumber;
    }
}
