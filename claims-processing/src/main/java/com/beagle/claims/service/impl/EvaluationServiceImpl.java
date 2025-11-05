package com.beagle.claims.service.impl;

import com.beagle.claims.model.EvaluationResponse;

import com.beagle.claims.model.llm.ChargeItem;
import com.beagle.claims.model.llm.ExtractedPayload;
import com.beagle.claims.service.EvaluationService;
import org.springframework.stereotype.Service;

import java.util.*;
/*
service class for the evaluation of final payout.
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {

    @Override
    public EvaluationResponse evaluate(ExtractedPayload payload) {
        List<String> missingDocs = new ArrayList<>();
        if (payload.getDocPresence() != null) {
            var docs = payload.getDocPresence();
            if (!docs.isLeaseAgreement()) missingDocs.add("lease_agreement");
            if (!docs.isLeaseAddendum()) missingDocs.add("lease_addendum");
            if (!docs.isNotificationToTenant()) missingDocs.add("notification_to_tenant");
            if (!docs.isTenantLedger()) missingDocs.add("tenant_ledger");
        } else {
            missingDocs.addAll(List.of("lease_agreement","lease_addendum","notification_to_tenant","tenant_ledger"));
        }

        boolean firstMonthRentPaid = Optional.ofNullable(payload.getLedgerValidation().getFirstMonthRentPaid()).orElse(false);
        boolean firstMonthSdiPaid  = Optional.ofNullable(payload.getLedgerValidation().getFirstMonthSdiPaid()).orElse(false);

        String status = "Approved";
        StringBuilder summary = new StringBuilder();

        if (!missingDocs.isEmpty()) {
            status = "Declined";
            summary.append("Missing required docs: ").append(missingDocs).append(". ");
        }
        if (!firstMonthRentPaid) {
            status = "Declined";
            summary.append("First month rent not confirmed. ");
        }
        if (!firstMonthSdiPaid) {
            status = "Declined";
            summary.append("First month SDI premium not confirmed. ");
        }

        double monthlyRent = Optional.ofNullable(payload.getMonthlyRent()).orElse(0.0);
        double maxBenefit  = Optional.ofNullable(payload.getMaximumBenefit()).orElse(0.0);

        List<Map<String,Object>> approvedCharges = new ArrayList<>();
        List<Map<String,Object>> excludedCharges = new ArrayList<>();
        double totalApproved = 0.0;

        if (payload.getCharges() != null) {
            for (ChargeItem c : payload.getCharges()) {
                String statusVal = c.getStatus() == null ? "" : c.getStatus().toLowerCase();
                String cat = c.getCategory() == null ? "" : c.getCategory().toLowerCase();
                String desc = Optional.ofNullable(c.getDescription()).orElse("");
                double amount = Optional.ofNullable(c.getAmount()).orElse(0.0);

                if (statusVal.equals("paid")) {
                    excludedCharges.add(reason(desc, amount, "Charge is already paid"));
                    continue;
                }

                if (amount <= 0) {
                    excludedCharges.add(reason(desc, amount, "Invalid or zero amount"));
                    continue;
                }

                if (Set.of("pet_damage","non_refundable_fee","lawn_service_unoccupied").contains(cat)) {
                    excludedCharges.add(reason(desc, amount, "Excluded category per policy"));
                    continue;
                }

                if (desc.toLowerCase().matches(".*(late fee|returned|admin|legal|maintenance|convenience).*")) {
                    excludedCharges.add(reason(desc, amount, "Excluded fee type per policy"));
                    continue;
                }

                double approvedAmt = 0;
                String why = null;

                switch (cat) {
                    case "unpaid_rent":
                        approvedAmt = Math.min(amount, monthlyRent);
                        why = "Unpaid rent covered up to one month's rent";
                        break;
//                    case "prorated_rent":
//                        if (desc.toLowerCase().contains("prorated") || desc.toLowerCase().contains("partial")) {
//                            approvedAmt = amount;
//                            why = "Prorated rent linked to tenant's occupancy or lease obligation is covered";
//                        } else {
//                            excludedCharges.add(reason(desc, amount, "Prorated rent not clearly linked to tenant occupancy or lease period"));
//                        }
//                        break;
                    case "prorated_rent":
                        String occ = c.getOccupancyLink() == null ? "" : c.getOccupancyLink().toLowerCase();
                        if ("yes".equals(occ)) {
                            approvedAmt = amount;
                            why = "Prorated rent tied to tenant occupancy is covered";
                        } else {
                            excludedCharges.add(reason(desc, amount, "Prorated rent not linked to tenant occupancy or lease obligation"));
                        }
                        break;
                    case "lease_break_fee":
                        approvedAmt = Math.min(amount, monthlyRent);
                        why = "Lease break fee covered up to one month's rent";
                        break;
                    case "landscaping":
                        approvedAmt = Math.min(amount, 500.0);
                        why = "Landscaping covered up to $500";
                        break;
                    case "rekey":
                    case "unpaid_utilities":
                        approvedAmt = amount;
                        why = cat.replace("_", " ") + " covered";
                        break;
                    default:
                        // For cleaning/painting/repair/etc.
                        if ("beyond_normal_wear_and_tear".equalsIgnoreCase(c.getWearClassification())) {
                            approvedAmt = amount;
                            why = "Damage beyond normal wear and tear is covered";
                        } else {
                            excludedCharges.add(reason(desc, amount, "Normal wear and tear or insufficient evidence"));
                        }
                        break;
                }

                if (approvedAmt > 0) {
                    totalApproved += approvedAmt;
                    approvedCharges.add(reason(desc, approvedAmt, why));
                }
            }
        }

        double finalPayout = (maxBenefit > 0)
                ? Math.min(totalApproved, maxBenefit)
                : totalApproved;

        if (status.equals("Declined")) {
            return EvaluationResponse.builder()
                    .firstMonthPaid(firstMonthRentPaid)
                    .firstMonthPaidEvidence(payload.getLedgerValidation().getFirstMonthRentEvidence())
                    .firstMonthSdiPremiumPaid(firstMonthSdiPaid)
                    .firstMonthSdiPremiumPaidEvidence(payload.getLedgerValidation().getFirstMonthSdiEvidence())
                    .missingDocuments(missingDocs)
                    .status(status)
                    .summaryOfDecision(summary.toString().trim())
                    .build();
        }
        return EvaluationResponse.builder()
                .firstMonthPaid(firstMonthRentPaid)
                .firstMonthPaidEvidence(payload.getLedgerValidation().getFirstMonthRentEvidence())
                .firstMonthSdiPremiumPaid(firstMonthSdiPaid)
                .firstMonthSdiPremiumPaidEvidence(payload.getLedgerValidation().getFirstMonthSdiEvidence())
                .missingDocuments(missingDocs)
                .status(status)
                .summaryOfDecision(summary.length() == 0 ? "All conditions satisfied." : summary.toString().trim())
                .approvedCharges(approvedCharges)
                .excludedCharges(excludedCharges)
                .totalApprovedCharges(round(totalApproved))
                .finalPayoutBasedOnCoverage(round(finalPayout))
                .build();
    }

    private Map<String,Object> reason(String desc, double amount, String why) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("description", desc);
        m.put("amount", round(amount));
        m.put("reason", why);
        return m;
    }

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }


}
