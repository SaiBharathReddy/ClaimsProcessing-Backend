package com.beagle.claims.service;

import com.beagle.claims.model.DocTexts;
import com.beagle.claims.model.llm.ExtractedPayload;

public interface LlmExtractorService {
    ExtractedPayload extract(DocTexts docs) throws Exception;
}
