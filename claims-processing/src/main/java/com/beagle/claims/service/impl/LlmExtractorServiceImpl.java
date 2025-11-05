package com.beagle.claims.service.impl;

import com.beagle.claims.model.DocTexts;
import com.beagle.claims.model.llm.ExtractedPayload;
import com.beagle.claims.service.LlmExtractorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
service class that provides methods for analyzing the extracted doc content, by passing that to LLM.
 */
@Service
public class LlmExtractorServiceImpl implements LlmExtractorService {
    private static final Logger log = LoggerFactory.getLogger(LlmExtractorServiceImpl.class);
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${anthropic_api_key}")
    private String apiKey;

    @Value("${anthropic_api_url}")
    private String apiUrl;

    @Value("${anthropic_api_model}")
    private String model;

    public LlmExtractorServiceImpl(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Override
    public ExtractedPayload extract(DocTexts docs) throws Exception {
        String prompt = PromptFactory.buildPrompt(docs.texts());

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("temperature", 0);
        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        log.info("Sending extraction request to Anthropic (model: {})", model);

        String responseJson = webClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Anthropic API error response: {}", errorBody);
                                    return Mono.error(new RuntimeException("Anthropic API error: " + errorBody));
                                })
                )
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    log.error("Anthropic API call failed", e);
                    return Mono.error(new RuntimeException("Anthropic API call failed: " + e.getMessage()));
                })
                .block();

        log.debug("Received response from Anthropic: {}", responseJson);
        Map<String, Object> jsonNode = mapper.readValue(responseJson, Map.class);

        String modelText = null;
        try {
            List<Map<String, Object>> content = (List<Map<String, Object>>) jsonNode.get("content");
            if (content != null && !content.isEmpty()) {
                modelText = (String) content.get(0).get("text");
            }
        } catch (Exception e) {
            log.error("Failed to extract text from model response", e);
            throw new RuntimeException("Invalid response structure from Anthropic API");
        }

        if (modelText == null || modelText.trim().isEmpty()) {
            throw new RuntimeException("No model text content found in Anthropic response");
        }

        log.info("Raw model text: {}", modelText);
        modelText = stripMarkdownCodeBlocks(modelText);

        log.info("Cleaned model text: {}", modelText);
        ExtractedPayload extracted = mapper.readValue(modelText, ExtractedPayload.class);

        log.info("LLM extraction successful: {} charges extracted",
                extracted.getCharges() != null ? extracted.getCharges().size() : 0);

        return extracted;
    }

    private String stripMarkdownCodeBlocks(String text) {
        if (text == null) {
            return null;
        }

        String cleaned = text.trim();

        if (cleaned.startsWith("```")) {

            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            }

            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }

        return cleaned.trim();
    }
}