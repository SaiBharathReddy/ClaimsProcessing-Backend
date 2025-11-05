package com.beagle.claims.model.llm;

import lombok.Data;

@Data
public class LedgerValidation {
    private Boolean firstMonthRentPaid;
    private String firstMonthRentEvidence;
    private Boolean firstMonthSdiPaid;
    private String firstMonthSdiEvidence;
}
