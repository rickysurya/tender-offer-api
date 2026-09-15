package com.github.rickysurya.tenderofferapi.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.rickysurya.tenderofferapi.service.JsonStorageService;
import com.github.rickysurya.tenderofferapi.service.OllamaExtractionService;
import com.github.rickysurya.tenderofferapi.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MtoScheduler {
    @Autowired
    private ScraperService scraperService;

    @Autowired
    private OllamaExtractionService ollamaExtractionService;

    @Autowired
    private JsonStorageService jsonStorageService;

    private static final Pattern TICKER_PATTERN = Pattern.compile("\\[([A-Z]{4})\\s*]");

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Jakarta")
    public void runDailyCheck() {
        LocalDate today = LocalDate.now();
        processDateRange(today, today);
        jsonStorageService.refreshAllPrices();
    }

    public int processDateRange(LocalDate from, LocalDate to) {
        List<ScraperService.ScrapedAnnouncement> announcements = scraperService.checkMTO(from, to);
        int processed = 0;
        for (var announcement : announcements) {
            if (announcement.pdfBytesList().isEmpty()) continue;
            JsonNode extracted = ollamaExtractionService.extract(announcement.pdfBytesList());

            String tickerFromTitle = extractTickerFromTitle(announcement.title());
            if (tickerFromTitle != null && extracted instanceof ObjectNode obj) {
                obj.put("ticker", tickerFromTitle);
            }
            jsonStorageService.insertTickerData(extracted);
            processed++;
        }
        return processed;
    }
    private String extractTickerFromTitle(String title) {
        Matcher m = TICKER_PATTERN.matcher(title);
        return m.find() ? m.group(1) : null;
    }
}