package com.beagle.claims.controller;

import com.beagle.claims.service.ExcelProcessor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/excel")
/*
controller class that provides endpoints to process the SDI claims sheet
*/
public class ExcelController {
    private final ExcelProcessor excelProcessor;

    public ExcelController(ExcelProcessor excelProcessor) {
        this.excelProcessor = excelProcessor;
    }
    /*
    endpoint that allows to upload the Excel sheet and store it in database.
    We use this data to fetch values that our LLM processor fails to fetch. This is like a fallback.
     */
    @PostMapping("/upload")
    public String uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            excelProcessor.saveExcel(file);
            return "File uploaded and data saved!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}
