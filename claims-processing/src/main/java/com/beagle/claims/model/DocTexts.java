package com.beagle.claims.model;

import java.util.Map;

public record DocTexts(
        Map<String, String> texts// keys: LEASE_AGREEMENT, LEASE_ADDENDUM_SDI, NOTIFICATION_TO_TENANT, TENANT_LEDGER_OR_MOVE_OUT_CALC
) {}
