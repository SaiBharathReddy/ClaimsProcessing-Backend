package com.beagle.claims.model.llm;

import lombok.Data;

@Data
public class DocPresence {
    private boolean leaseAgreement;
    private boolean leaseAddendum;
    private boolean notificationToTenant;
    private boolean tenantLedger;
}
