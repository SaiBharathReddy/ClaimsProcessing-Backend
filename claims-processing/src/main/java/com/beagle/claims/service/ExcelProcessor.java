package com.beagle.claims.service;

import com.beagle.claims.model.Policy;
import com.beagle.claims.repository.PolicyRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;

@Service
public class ExcelProcessor {

    private final PolicyRepository policyRepository;

    public ExcelProcessor(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }
    public void saveExcel(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                Policy policy = new Policy();

                policy.setTrackingNumber((int) row.getCell(0).getNumericCellValue());

                policy.setLeaseStartDate(getDateCellValue(row.getCell(1)));

                policy.setLeaseEndDate(getDateCellValue(row.getCell(2)));

                policy.setMoveOutDate(getDateCellValue(row.getCell(3)));

                policy.setPolicy(getCellValueAsString(row.getCell(4)));

                String benefitStr = getCellValueAsString(row.getCell(5)).replace("$", "").trim();
                if (!benefitStr.isEmpty()) {
                    policy.setMaxBenefit(Double.parseDouble(benefitStr));
                } else {
                    policy.setMaxBenefit(0.0);
                }
                //policy.setMaxBenefit(Double.parseDouble(benefitStr));

                policyRepository.save(policy);
            }
        }
    }

    public String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("MM/dd/yy").format(cell.getDateCellValue());
                } else {
                    double val = cell.getNumericCellValue();
                    if (val == (long) val) return String.valueOf((long) val);
                    else return String.valueOf(val);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
            default:
                return "";
        }
    }

    private Date getDateCellValue(Cell cell) throws Exception {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return new Date(cell.getDateCellValue().getTime());
        } else if (cell.getCellType() == CellType.STRING) {
            String dateStr = cell.getStringCellValue().trim();
            if (dateStr.isEmpty()) return null;
            return new Date(new SimpleDateFormat("MM/dd/yy").parse(dateStr).getTime());
        }
        return null;
    }
}

