package com.github.rickysurya.tenderofferapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaExtractionService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PdfExtractionService pdfExtractionService;

    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5:3b}")
    private String model;

    private static final String PROMPT_TEMPLATE = """
            Extract these fields from this Indonesian stock exchange announcement text.
            If a field is not present, use null.
            Do not use an acquisition completion date, announcement date, or any
            single event date as periodStart/periodEnd. Only use dates explicitly
            described as the start/end of a tender offer period.
            
            Priority order for announcementType:
              1. TENDER_OFFER_SCHEDULE (if period dates are present)
              2. TENDER_OFFER_PLAN (if offer price is present, even without dates)
              3. CHANGE_OF_CONTROL (if acquisition/takeover is stated but tender offer details deferred)
              4. OTHER
            
            DOCUMENT TEXT:
            %s
            """;

    private static final Map<String, Object> JSON_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "ticker", Map.of("type", List.of("string", "null")),
                    "issuer", Map.of("type", List.of("string", "null")),
                    "offeror", Map.of("type", List.of("string", "null")),
                    "announcementType", Map.of("type", "string", "enum", List.of("CHANGE_OF_CONTROL", "TENDER_OFFER_PLAN", "TENDER_OFFER_SCHEDULE", "OTHER")),
                    "offerPricePerShare", Map.of("type", List.of("number", "null")),
                    "periodStart", Map.of("type", List.of("string", "null")),
                    "periodEnd", Map.of("type", List.of("string", "null")),
                    "periodDurationDays", Map.of("type", List.of("integer", "null"))
            ),
            "required", List.of("ticker", "issuer", "offeror", "announcementType", "offerPricePerShare", "periodStart", "periodEnd", "periodDurationDays")
    );

    public JsonNode extract(List<byte[]> pdfBytesList) {
        StringBuilder combinedText = new StringBuilder();
        for (int i = 0; i < pdfBytesList.size(); i++) {
            String text = pdfExtractionService.extractText(pdfBytesList.get(i));
            System.out.println("--- DOC " + i + " EXTRACTED TEXT ---\n" + text);
            combinedText.append("--- DOCUMENT ").append(i + 1).append(" ---\n").append(text).append("\n\n");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", PROMPT_TEMPLATE.formatted(combinedText.toString()),
                "stream", false,
                "format", JSON_SCHEMA,
                "options", Map.of(
                        "temperature", 0.0,
                        "num_ctx", 32768
                )
        );

        String response = restClient.post()
                .uri(ollamaUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            String jsonText = objectMapper.readTree(response).path("response").asText();
            return objectMapper.readTree(jsonText);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Ollama extraction response: " + response, e);
        }
    }
}
