package com.github.rickysurya.tenderofferapi.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;

@Service
public class ScraperService {
    private final String disclosureUrl = "https://idx.co.id/en/listed-companies/disclosure/";
    private final String fetchTickerUrl = "https://www.idx.co.id/en/market-data/trading-summary/stock-summary/";

    public record ScrapedAnnouncement(String title, List<byte[]> pdfBytesList) {
    }


    @Autowired
    private OllamaClassificationService classificationService;

    private void selectDateRange(Page page, LocalDate from, LocalDate to) {
        Locator input = page.locator(".mx-datepicker-range input.mx-input");
        input.click();

        Locator clearIcon = page.locator(".mx-icon-clear");
        if (clearIcon.count() > 0) {
            clearIcon.first().click();
            input.click();
        }

        String formatted = from + " ~ " + to;
        input.pressSequentially(formatted);
        page.keyboard().press("Enter");
    }

    public List<ScrapedAnnouncement> checkMTO(LocalDate from, LocalDate to) {
        Map<String, Object> firefoxPrefs = new HashMap<>();
        firefoxPrefs.put("pdfjs.disabled", true);
        firefoxPrefs.put("browser.download.folderList", 2);
        firefoxPrefs.put("browser.download.dir", System.getProperty("java.io.tmpdir"));
        firefoxPrefs.put("browser.helperApps.neverAsk.saveToDisk", "application/pdf");
        firefoxPrefs.put("browser.download.manager.showWhenStarting", false);
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(
                    new BrowserType.LaunchOptions().setHeadless(false).setFirefoxUserPrefs(firefoxPrefs)
            );
            Page page = browser.newPage();
            page.navigate(disclosureUrl);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Announcement")).click();
            page.waitForLoadState(LoadState.NETWORKIDLE);


            selectDateRange(page, from, to);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            List<ScrapedAnnouncement> results = new ArrayList<>();
            boolean hasNextPage = true;

            while (hasNextPage) {
                Locator cards = page.locator("div.attach-card");
                cards.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
                int count = cards.count();

                List<String> pageTitles = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    pageTitles.add(cards.nth(i).locator("h6.title").innerText()
                            .replaceAll("\\s+", " ").trim());
                }

                List<Integer> relevantOnThisPage = classificationService.classifyRelevant(pageTitles);

                for (int i : relevantOnThisPage) {
                    Locator card = cards.nth(i);
                    Locator pdfLinks = card.locator("ul.list-nostyle li a.d-iblock");
                    List<byte[]> pdfBytesList = new ArrayList<>();

                    for (int j = 0; j < pdfLinks.count(); j++) {
                        Locator pdfLink = pdfLinks.nth(j);
                        Download download = page.waitForDownload(() -> pdfLink.click());
                        pdfBytesList.add(Files.readAllBytes(download.path()));
                    }
                    results.add(new ScrapedAnnouncement(pageTitles.get(i), pdfBytesList));
                }
                //loop through all pages
                Locator nextButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Go to next page"));
                if (nextButton.isDisabled()) {
                    hasNextPage = false;
                } else {
                    nextButton.click();
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                }
            }


            browser.close();
            return results;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            Locator dateInput = page.locator(".mx-datepicker input.mx-input");
            dateInput.click();
            String hardcodedDate = "2026-05-08";
            dateInput.pressSequentially(hardcodedDate);
            page.keyboard().press("Enter");


            page.waitForLoadState(LoadState.NETWORKIDLE);

            Locator targetCell = page.locator("#vgt-table tbody tr").first().locator("td.vgt-right-align").nth(2);

            try {
                targetCell.waitFor(new Locator.WaitForOptions().setTimeout(3000));
                results = targetCell.innerText().trim();
            } catch (com.microsoft.playwright.TimeoutError e) {
                System.out.println("No trading data found for " + ticker + " today (market closed or invalid ticker).");
            }
            browser.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return results;
    }
}

