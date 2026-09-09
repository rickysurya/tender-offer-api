package com.github.rickysurya.tenderofferapi.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScraperService {
    private final String disclosureUrl = "https://idx.co.id/en/listed-companies/disclosure/";
    private final String fetchTickerUrl = "https://www.idx.co.id/en/market-data/trading-summary/stock-summary/";
    private final String filterSearchMto = "/refloat";

    public List<String> checkMTO() {
        List<String> results = new ArrayList<>();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch();
            Page page = browser.newPage();
            page.navigate(disclosureUrl);

            // keyword search
            page.locator("#FilterSearch").fill(filterSearchMto);
            page.locator("#FilterSearch").press("Enter");

            //open datepicker
            page.locator(".mx-datepicker-range input.mx-input").click();

            Locator todayCell = page.locator(".mx-datepicker-popup td.cell.today");
            // fetch today only since it will be running everyday
            todayCell.first().click();
            todayCell.first().click();

            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.locator("h6.title");
            Locator titles = page.locator("h6.title");
            int count = titles.count();
            if (count == 0) {
                browser.close();
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

    public String getTickerLastPrice(String ticker) {
        String results = "";
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch();
            Page page = browser.newPage();

            page.navigate(fetchTickerUrl);

            // keyword search
            page.locator("#FilterSearch").fill(ticker);
            page.locator("#FilterSearch").press("Enter");

            //open datepicker
            page.locator(".mx-datepicker input.mx-input").click();
            Locator todayCell = page.locator(".mx-datepicker-popup td.cell.today");
            todayCell.first().click();


            page.waitForLoadState(LoadState.NETWORKIDLE);

            Locator rows = page.locator("#vgt-table tbody tr");
            if (rows.count() == 0) {
                browser.close();
                return results;
            }
            results = rows.first().locator("td.vgt-right-align").nth(2).innerText().trim();
            browser.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return results;
    }
}

