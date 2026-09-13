package com.github.rickysurya.tenderofferapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class JsonStorageService {

    @Autowired
    private ScraperService scraperService;

    private final File file = new File("data/content.json");
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void insertTickerData(JsonNode extracted) {
        String ticker = extracted.path("ticker").asText(null);
        System.out.println("Ticker: " + ticker);
        if (ticker == null) {
            System.out.println("extracted ticker" + ticker);
            return;

        }

        Map<String, Map<String, String>> rootData = readJsonFile();
        Map<String, String> tickerDetails = rootData.getOrDefault(ticker, new HashMap<>());

        putIfPresent(tickerDetails, "periodStart", extracted);
        putIfPresent(tickerDetails, "periodEnd", extracted);
        putIfPresent(tickerDetails, "periodDurationDays", extracted);
        putIfPresent(tickerDetails, "offerPricePerShare", extracted);
        putIfPresent(tickerDetails, "announcementType", extracted);

        tickerDetails.put("lastClosePrice", scraperService.getTickerLastPrice(ticker));

        rootData.put(ticker, tickerDetails);
        System.out.println("saving ticker to json" + ticker);
        writeJsonFile(rootData);
    }

    private void putIfPresent(Map<String, String> details, String field, JsonNode extracted) {
        JsonNode value = extracted.path(field);
        if (!value.isNull() && !value.isMissingNode()) {
            details.put(field, value.asText());
        }
    }

    public void updateTickerData(String ticker) {
        Map<String, Map<String, String>> rootData = readJsonFile();

        if (rootData.containsKey(ticker)) {
            String lastClosePrice = scraperService.getTickerLastPrice(ticker);
            rootData.get(ticker).put("lastClosePrice", lastClosePrice);
            writeJsonFile(rootData);
        }
    }

    private Map<String, Map<String, String>> readJsonFile() {
        if (!file.exists() || file.length() == 0) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(file, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file.getPath(), e);
        }
    }

    private void writeJsonFile(Map<String, Map<String, String>> data) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            objectMapper.writeValue(file, data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to " + file.getPath(), e);
        }
    }
}
