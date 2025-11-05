package com.beagle.claims.controller;

import com.beagle.claims.model.DocTexts;
import com.beagle.claims.model.EvaluationResponse;
import com.beagle.claims.model.Policy;
import com.beagle.claims.model.llm.ChargeItem;
import com.beagle.claims.model.llm.ExtractedPayload;
import com.beagle.claims.repository.PolicyRepository;
import com.beagle.claims.service.DocumentExtractionService;
import com.beagle.claims.service.EvaluationService;
import com.beagle.claims.service.LlmExtractorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
/* Controller class that provides endpoints to process the uploaded documents
    and evaluate the final payout.
*/
@Slf4j
@RestController
@RequestMapping("/api/claims")
@CrossOrigin(origins = {"*"}, methods = { RequestMethod.DELETE, RequestMethod.GET, RequestMethod.POST,
        RequestMethod.PUT })
public class ClaimController {

    private final DocumentExtractionService extractionService;
    private final LlmExtractorService llmExtractorService;
    private final EvaluationService evaluationService;
    private final PolicyRepository policyRepository;
    public ClaimController(DocumentExtractionService extractionService,
                           LlmExtractorService llmExtractorService,
                           EvaluationService evaluationService, PolicyRepository policyRepository) {
        this.extractionService = extractionService;
        this.llmExtractorService = llmExtractorService;
        this.evaluationService = evaluationService;
        this.policyRepository = policyRepository;
    }
    /*Dummy endpoint*/
    @GetMapping("/health")
    public String healthCheck(){
        return "OK";
    }
    /*
    endpoint that accepts the uploaded files, extracts the content from the files,
    uses LLM to analyze the extracted content and sends out the json with required fields.
     */
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extractAndEvaluate(
            @RequestPart(name = "lease_agreement", required = false) MultipartFile leaseAgreement,
            @RequestPart(name = "lease_addendum", required = false) MultipartFile leaseAddendum,
            @RequestPart(name = "notification_to_tenant", required = false) MultipartFile notificationToTenant,
            @RequestPart(name = "tenant_ledger", required = false) MultipartFile tenantLedger,
            @RequestParam("policyNumber") String policyNumber
    ) throws Exception {
        Map<String, MultipartFile> files = new LinkedHashMap<>();
        files.put("LEASE_AGREEMENT", leaseAgreement);
        files.put("LEASE_ADDENDUM_SDI", leaseAddendum);
        files.put("NOTIFICATION_TO_TENANT", notificationToTenant);
        files.put("TENANT_LEDGER_OR_MOVE_OUT_CALCULATION", tenantLedger);
        System.out.println(files);
        DocTexts docTexts = extractionService.extractTexts(files);

        ExtractedPayload extracted = llmExtractorService.extract(docTexts);
        if(extracted.getMaximumBenefit()==null){
            Optional<Policy> policy=policyRepository.findById(policyNumber);
            policy.ifPresent(value -> extracted.setMaximumBenefit(value.getMaxBenefit()));
        }
        if (extracted.getCharges() != null) {
            for (ChargeItem c : extracted.getCharges()) {
                // Only for prorated rent
                if ("prorated_rent".equalsIgnoreCase(Optional.ofNullable(c.getCategory()).orElse(""))) {
                    c.setOccupancyLink("No");
                    Optional<Policy> policy=policyRepository.findById(policyNumber);
                    if(policy.isPresent()){
                        Policy policyLatest=policy.get();
                        if (c.getDate() != null && policyLatest.getLeaseStartDate() != null && policyLatest.getMoveOutDate() != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            LocalDate chargeDate = LocalDate.parse(c.getDate(), formatter);
                            if (!chargeDate.isBefore(policyLatest.getLeaseStartDate().toLocalDate()) &&
                                    !chargeDate.isAfter(policyLatest.getMoveOutDate().toLocalDate())) {
                                c.setOccupancyLink("Yes");
                            }
                        }
                    }
                }
            }
        }

        System.out.println(extracted);
        return ResponseEntity.ok(extracted);
    }
    /*
    endpoint that processes the final payout
     */
    @PostMapping ("/evaluate")
            public ResponseEntity<?> evaluate(@RequestBody ExtractedPayload extracted){
        System.out.println(extracted);
        EvaluationResponse result = evaluationService.evaluate(extracted); // currently stubbed
        return ResponseEntity.ok(result);
    }
}

