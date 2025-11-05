package com.beagle.claims.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Date;

@Entity
@Data
public class Policy {
    @Id
    private String policy; // Primary key
    private int trackingNumber;
    private Date leaseStartDate;
    private Date leaseEndDate;
    private Date moveOutDate;
    private double maxBenefit;
}
