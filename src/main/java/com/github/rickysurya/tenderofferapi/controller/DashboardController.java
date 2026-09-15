package com.github.rickysurya.tenderofferapi.controller;

import com.github.rickysurya.tenderofferapi.scheduler.MtoScheduler;
import com.github.rickysurya.tenderofferapi.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    @Autowired
    private ScraperService scraperService;
    @Autowired
    private MtoScheduler  mtoScheduler;

    //testing purposes
    @GetMapping("/check")
    public String check(@RequestParam("ticker") String ticker) {
        if (ticker == null) {
            return "";
        }
        return scraperService.getTickerLastPrice(ticker);
    }

    @GetMapping("/fetch")
    public Map<String, Integer> testScraper(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to)  {
        LocalDate f = from != null ? LocalDate.parse(from) : LocalDate.now();
        LocalDate t = to != null ? LocalDate.parse(to) : LocalDate.now();
        List<ScraperService.ScrapedAnnouncement> result = scraperService.checkMTO(f,t);
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (var a : result) {
            summary.put(a.title(), a.pdfBytesList().size());
        }
        return summary;
    }

    @GetMapping("/run")
    public String runPipeline(@RequestParam String from, @RequestParam String to) {
        int processed = mtoScheduler.processDateRange(LocalDate.parse(from), LocalDate.parse(to));
        return "Processed " + processed + " announcement(s). Check data/content.json.";
    }

}
