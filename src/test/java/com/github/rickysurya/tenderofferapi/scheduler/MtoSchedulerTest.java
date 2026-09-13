package com.github.rickysurya.tenderofferapi.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rickysurya.tenderofferapi.service.JsonStorageService;
import com.github.rickysurya.tenderofferapi.service.ScraperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class MtoSchedulerTest {

    @Mock ScraperService scraperService;
    @Mock GeminiExtractionService geminiExtractionService;
    @Mock JsonStorageService jsonStorageService;

    @InjectMocks MtoScheduler scheduler;

    @Test
    void skipsAnnouncementsWithNoPdfs_extractsOnesWithPdfs() {
        byte[] fakePdf = "fake".getBytes();

        List<ScraperService.ScrapedAnnouncement> announcements = List.of(
                new ScraperService.ScrapedAnnouncement("Ownership Report, nothing attached", List.of()),
                new ScraperService.ScrapedAnnouncement("Announcement of the Plan to Conduct a Mandatory Tender Offer", List.of(fakePdf))
        );
        LocalDate today = LocalDate.now();
        when(scraperService.checkMTO(today, today)).thenReturn(announcements);

        JsonNode fakeExtracted = new ObjectMapper().createObjectNode().put("ticker", "TEST");
        when(geminiExtractionService.extract(List.of(fakePdf))).thenReturn(fakeExtracted);

        scheduler.runDailyCheck();

        verify(geminiExtractionService, times(1)).extract(any());
        verify(jsonStorageService).insertTickerData(fakeExtracted);
    }
}
