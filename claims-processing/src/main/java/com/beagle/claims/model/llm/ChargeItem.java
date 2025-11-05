package com.beagle.claims.model.llm;

import lombok.Data;

@Data
public class ChargeItem {
    private String date;
    private String description;
    private Double amount;
    private String status; // unpaid | paid | overdue | null
    private String category;
    private String wearClassification; // normal_wear_and_tear | beyond_normal_wear_and_tear | null
    private String evidence;
    private String occupancyLink;
}