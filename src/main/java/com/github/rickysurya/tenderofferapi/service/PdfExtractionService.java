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


    @Autowired
    private OcrFallbackService ocrFallbackService;

    public String extractText(byte[] pdfBytes) {
            return ocrFallbackService.ocrPdf(pdfBytes);
    }
}
