package com.beagle.claims.service.impl;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
helper class providing method to construct the prompt, to be used by LLM.
*/
public class PromptFactory {
    private static final Logger log = LoggerFactory.getLogger(PromptFactory.class);
    public static String buildPrompt(Map<String, String> docs) {
        log.info("Building prompt with docs:");
        docs.forEach((key, value) -> {
            log.info("  {}: {} characters", key, value != null ? value.length() : 0);
            if (value == null || value.trim().isEmpty()) {
                log.warn("  WARNING: {} is null or empty!", key);
            }
        });
        return """
You are a precise information extractor for Security Deposit Insurance (SDI) claims.
Your task is to read the four provided documents and extract key fields to determine the final payout.
Return **ONLY valid JSON** following the schema below. Do not include any explanatory text, markdown formatting, or code blocks.

### Schema (use EXACTLY these field names in camelCase):
{
  "tenantName": "string",
  "propertyAddress": "string",
  "docPresence": {
    "leaseAgreement": true,
    "leaseAddendum": true,
    "notificationToTenant": true,
    "tenantLedger": true
  },
  "monthlyRent": number,
  "maximumBenefit": number,
  "ledgerValidation": {
    "firstMonthRentPaid": true|false|null,
    "firstMonthRentEvidence": "string",
    "firstMonthSdiPaid": true|false|null,
    "firstMonthSdiEvidence": "string"
  },
  "charges": [
    {
      "date": "YYYY-MM-DD|null",
      "description": "string",
      "amount": number,
      "status": "unpaid|paid|overdue|null",
      "category": "cleaning|rekey|landscaping|unpaid_utilities|unpaid_rent|lease_break_fee|prorated_rent|painting|carpet|flooring|repair|pet_damage|non_refundable_fee|lawn_service_unoccupied|other|null",
      "wearClassification": "normal_wear_and_tear|beyond_normal_wear_and_tear|null",
      "evidence": "string",
      "occupancyLink":"null"
    }
  ],
  "notification": {
    "present": true|false|null,
    "date": "YYYY-MM-DD|null",
    "evidence": "string"
  }
}
### Category Classification Rules (CRITICAL - READ CAREFULLY):

**prorated_rent**: Use ONLY when:
- The rent amount is for a PARTIAL month (not a full month)
- The charge explicitly mentions "prorated", "partial month", or covers less than a full rental period
- The amount is clearly less than the full monthly rent AND corresponds to specific days (e.g., "10/1-10/15")
- Common patterns: "Rent 10/1-10/15", "Prorated rent", "Partial month rent"

**unpaid_rent**: Use ONLY when:
- The charge is for a FULL MONTH of rent that was not paid
- The amount matches or is close to the full monthly rent amount
- There is NO indication of proration or partial month
- The tenant failed to pay regular monthly rent for complete rental periods

**Examples to guide your classification:**
- "$774.19 for Rent 10/1-10/15" → prorated_rent (partial month)
- "$1,548.00 for October Rent" → unpaid_rent (full month)
- "$774.19 for Rent" on 10/01 when monthly rent is $1,548 → prorated_rent (amount suggests partial month)
- "$1,500 Rent" when monthly rent is $1,500 → unpaid_rent (full month amount)

**Other categories:**
- cleaning: Cleaning fees, carpet cleaning, etc.
- rekey: Lock changes, rekeying fees
- landscaping: Yard work, lawn care during occupancy
- unpaid_utilities: Unpaid water, electric, gas, trash bills
- lease_break_fee: Fee for breaking lease early
- painting: Wall painting, touch-ups
- carpet: Carpet replacement/repair (not cleaning)
- flooring: Floor damage/replacement
- repair: General repairs, damage fixes
- pet_damage: Pet-related damages
- non_refundable_fee: Non-refundable fees from lease
- lawn_service_unoccupied: Lawn care after tenant moved out
- other: Anything that doesn't fit above categories

### Extraction Rules:
- Use the document text exactly as given; do not invent information.
- Use null if you cannot determine a value.
- Status = unpaid or overdue if the balance remains outstanding.
- tenant_name: prefer full legal tenant names listed on lease or move-out letter.
- property_address: full address of the rental property (street, city, state, ZIP) from lease or notice.
- monthlyRent = recurring full rent amount, not prorations.
- maximumBenefit = from the SDI addendum.
- Only include explicit charges or fees; exclude invented items.
- For categories and wearClassification, use only the enums above.
- Provide a short evidence quote for every extracted field.
- **CRITICAL**: Use camelCase for ALL field names exactly as shown in the schema above.

### Documents:
=== LEASE_AGREEMENT ===
%s

=== LEASE_ADDENDUM_SDI ===
%s

=== NOTIFICATION_TO_TENANT ===
%s

=== TENANT_LEDGER_OR_MOVE_OUT_CALCULATION ===
%s
""".formatted(
                docs.getOrDefault("LEASE_AGREEMENT", ""),
                docs.getOrDefault("LEASE_ADDENDUM_SDI", ""),
                docs.getOrDefault("NOTIFICATION_TO_TENANT", ""),
                docs.getOrDefault("TENANT_LEDGER_OR_MOVE_OUT_CALCULATION", "")
        );
    }
}
