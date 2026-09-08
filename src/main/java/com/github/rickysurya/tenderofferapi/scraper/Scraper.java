package com.github.rickysurya.tenderofferapi.scraper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class Scraper {
    private final String url = "https://idx.co.id/en/listed-companies/disclosure/";
    private final String filterSearchId = "/refloat";

    public List<String> run() {
        List<String> results = new ArrayList<>();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();
            page.navigate(url);

            // keyword search
            page.locator("#FilterSearch").fill(filterSearchId);
            page.locator("#FilterSearch").press("Enter");

            //open datepicker
            page.locator(".mx-datepicker-range input.mx-input").click();

            Locator todayCell = page.locator(".mx-datepicker-popup td.cell.today");
            // fetch today only since it will be running everyday
            todayCell.first().click();
            todayCell.first().click();

            // scrape the results
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.locator("h6.title");

            Locator titles = page.locator("h6.title");
            int count = titles.count();

            if (count == 0) {
                browser.close();
                results.add("nada");
                return results;
            }

            for (int i = 0; i < count; i++) {
                String cleanTitle = titles.nth(i).innerText().replaceAll("\\s+", " ").trim();
                results.add(cleanTitle);
            }

            browser.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return results;
    }
}

