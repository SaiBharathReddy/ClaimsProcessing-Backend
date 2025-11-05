package com.beagle.claims.service;

import com.beagle.claims.model.EvaluationResponse;
import com.beagle.claims.model.llm.ExtractedPayload;


public interface EvaluationService {
    EvaluationResponse evaluate(ExtractedPayload payload);
}
