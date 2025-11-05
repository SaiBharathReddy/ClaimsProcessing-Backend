package com.beagle.claims.service;
import com.beagle.claims.model.DocTexts;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
public interface DocumentExtractionService {
    DocTexts extractTexts(Map<String, MultipartFile> files) throws Exception;
}
