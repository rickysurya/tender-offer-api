package com.github.rickysurya.tenderofferapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class TenderOfferService {

    private final File file = new File("data/content.json");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> findAll(String statusFilter) {
        Map<String, Map<String, String>> rawData = readJsonFile();
        List<Map<String, Object>> results = new ArrayList<>();

        for (var entry : rawData.entrySet()) {
            Map<String, String> fields = entry.getValue();
            String status = computeStatus(fields.get("periodEnd"));
            if (statusFilter != null && !statusFilter.equalsIgnoreCase(status)) continue;

            Map<String, Object> view = new LinkedHashMap<>();
            view.put("ticker", entry.getKey());
            view.put("periodStart", fields.get("periodStart"));
            view.put("periodEnd", fields.get("periodEnd"));
            view.put("offerPricePerShare", parseNumberOrNull(fields.get("offerPricePerShare")));
            view.put("lastClosePrice", parseNumberOrNull(fields.get("lastClosePrice")));
            view.put("announcementType", fields.get("announcementType"));
            view.put("status", status);
            results.add(view);
        }
        return results;
    }

    private String computeStatus(String periodEndStr) {
        if (periodEndStr == null || periodEndStr.isBlank()) return "UNKNOWN";
        try {
            return LocalDate.now().isAfter(LocalDate.parse(periodEndStr)) ? "CLOSED" : "OPEN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private BigDecimal parseNumberOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Map<String, String>> readJsonFile() {
        if (!file.exists() || file.length() == 0) return new HashMap<>();
        try {
            return objectMapper.readValue(file, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file.getPath(), e);
        }
    }
}
