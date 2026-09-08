package com.github.rickysurya.tenderofferapi.controller;

import com.github.rickysurya.tenderofferapi.scraper.Scraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    @Autowired
    private Scraper scraper;

    @GetMapping("/fetch")
    public List<String> testScraper() {
        return scraper.run();
    }

}
