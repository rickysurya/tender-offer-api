package com.github.rickysurya.tenderofferapi.controller;

import com.github.rickysurya.tenderofferapi.service.Scraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    @Autowired
    private Scraper scraper;

    @GetMapping("/check")
    public Map<String, Object> check(@RequestParam("ticker") String ticker) {
        if (ticker == null) {
            return Map.of("error", "ticker is null");
        }
        return scraper.getTickers(ticker);
    }

    @GetMapping("/fetch")
    public List<String> testScraper() {
        return scraper.checkMTO();
    }

}
