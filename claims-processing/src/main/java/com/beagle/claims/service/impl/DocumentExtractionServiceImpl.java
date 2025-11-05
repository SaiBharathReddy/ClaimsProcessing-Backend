package com.beagle.claims.service.impl;

import com.beagle.claims.model.DocTexts;
import com.beagle.claims.service.DocumentExtractionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
/*
service class that does the task of extracting the content from all pdf's and merging
them, which we later pass it for LLM analysis.
 */
@Service
public class DocumentExtractionServiceImpl implements DocumentExtractionService {

    @Override
    public DocTexts extractTexts(Map<String, MultipartFile> files) throws Exception {
        Map<String, String> out = new LinkedHashMap<>();
        for (var e : files.entrySet()) {
            String key = e.getKey();
            MultipartFile f = e.getValue();
            if (f == null || f.isEmpty()) {
                System.out.println("********"+key+"**********");
                out.put(key, "");
                continue;
            }
            String text = extractPdfTextPreserveLayout(f.getBytes());
            out.put(key, text == null ? "" : text.trim());
        }
        return new DocTexts(out);
    }

    private String extractPdfTextPreserveLayout(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(true);
            return stripper.getText(doc);
        }
    }
}
