package com.github.rickysurya.tenderofferapi.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.rickysurya.tenderofferapi.service.JsonStorageService;
import com.github.rickysurya.tenderofferapi.service.OllamaExtractionService;
import com.github.rickysurya.tenderofferapi.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class MtoScheduler {
    @Autowired
    private ScraperService scraperService;

    @Autowired
    private OllamaExtractionService ollamaExtractionService;

    @Autowired
    private JsonStorageService jsonStorageService;

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Jakarta")
    public void runDailyCheck() {
        LocalDate today = LocalDate.now();
        processDateRange(today, today);
    }

    public int processDateRange(LocalDate from, LocalDate to) {
        List<ScraperService.ScrapedAnnouncement> announcements = scraperService.checkMTO(from, to);
        int processed = 0;
        for (var announcement : announcements) {
            if (announcement.pdfBytesList().isEmpty()) continue;
            JsonNode extracted = ollamaExtractionService.extract(announcement.pdfBytesList());
            jsonStorageService.insertTickerData(extracted);
            processed++;
        }
        return processed;
    }
}