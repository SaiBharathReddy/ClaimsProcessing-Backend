package com.beagle.claims.model.llm;
import lombok.Data;
import java.util.List;
@Data
public class ExtractedPayload {
    private String tenantName;
    private String propertyAddress;
    private DocPresence docPresence;
    private Double monthlyRent;
    private Double maximumBenefit;
    private LedgerValidation ledgerValidation;
    private List<ChargeItem> charges;
    private Notification notification;
}
