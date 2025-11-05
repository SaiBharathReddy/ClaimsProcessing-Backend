package com.beagle.claims.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class EvaluationResponse {
    private boolean firstMonthPaid;
    private String firstMonthPaidEvidence;
    private boolean firstMonthSdiPremiumPaid;
    private String firstMonthSdiPremiumPaidEvidence;
    private List<String> missingDocuments;
    private String status;
    private String summaryOfDecision;

    private List<Map<String,Object>> approvedCharges;
    private List<Map<String,Object>> excludedCharges;
    private double totalApprovedCharges;
    private double finalPayoutBasedOnCoverage;
}
