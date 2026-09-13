package com.github.rickysurya.tenderofferapi.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;

@Service
public class PdfExtractionService {
    public String extractRawText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }

    private static final Set<String> COMMON_WORDS = Set.of("yang", "dan", "dari", "the", "and", "of");

    @Autowired
    private OcrFallbackService ocrFallbackService;

    public String extractText(byte[] pdfBytes) {
        String rawText = extractRawText(pdfBytes);
        if (looksGarbled(rawText)) {
            System.out.println("Text extraction looks garbled, falling back to OCR");
            return ocrFallbackService.ocrPdf(pdfBytes);
        }
        return rawText;
    }

    private boolean looksGarbled(String text) {
        if (text == null || text.isBlank()) return true;
        String lower = text.toLowerCase();
        long matches = COMMON_WORDS.stream().filter(lower::contains).count();
        return matches < 2;
    }
}
