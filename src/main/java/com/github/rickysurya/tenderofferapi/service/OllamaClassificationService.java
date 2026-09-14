package com.github.rickysurya.tenderofferapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaClassificationService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5:3b}")
    private String model;

    private static final String PROMPT_TEMPLATE = """
            Below is a numbered list of IDX announcement titles from today.
            Return ONLY a JSON array of the indices (integers) that relate to
            any of: a mandatory or voluntary tender offer, a change of control,
            takeover, or acquisition of a public company, or a tender offer
            schedule. Titles may be English or Indonesian, e.g.
            "Pengambilalihan", "Penawaran Tender Wajib", "Jadwal Penawaran
            Tender", "Change of control". Ignore routine items.
            
            Return format: [0, 3, 7] (empty array [] if none match)
            
            Titles:
            %s
            """;
    private static final Map<String, Object> JSON_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "indices", Map.of(
                            "type", "array",
                            "items", Map.of("type", "integer")
                    )
            ),
            "required", List.of("indices")
    );

    public List<Integer> classifyRelevant(List<String> titles) {
        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < titles.size(); i++) {
            numbered.append(i).append(". ").append(titles.get(i)).append("\n");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", PROMPT_TEMPLATE.formatted(numbered),
                "stream", false,
                "format", JSON_SCHEMA,
                "options", Map.of(
                        "temperature", 0.0)
        );

        String response = restClient.post()
                .uri(ollamaUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            String jsonText = objectMapper.readTree(response).path("response").asText();

            JsonNode rootNode = objectMapper.readTree(jsonText);

            JsonNode indicesNode = rootNode.path("indices");
            return objectMapper.convertValue(indicesNode, new TypeReference<List<Integer>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse classification response: " + response, e);
        }
    }
}
