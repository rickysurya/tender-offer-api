package com.github.rickysurya.tenderofferapi.service;

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

    public void insertTickerData(String ticker) {
        Map<String, Map<String, String>> rootData = readJsonFile();
        String lastClosePrice = scraperService.getTickerLastPrice(ticker);

        Map<String, String> tickerDetails = rootData.getOrDefault(ticker, new HashMap<>());
        tickerDetails.putIfAbsent("periodStart", "");
        tickerDetails.putIfAbsent("periodEnd", "");
        tickerDetails.putIfAbsent("tenderOffer", "");
        tickerDetails.put("lastClosePrice", lastClosePrice);
        tickerDetails.putIfAbsent("cagr", "");

        rootData.put(ticker, tickerDetails);
        writeJsonFile(rootData);
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
            return objectMapper.readValue(file, new TypeReference<>() {});
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
