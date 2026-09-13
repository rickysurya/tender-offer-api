package com.github.rickysurya.tenderofferapi.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.pdfbox.Loader.*;

@Service
public class OcrFallbackService {

    public String ocrPdf(byte[] pdfBytes) {
        try (PDDocument document = loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder allText = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300);

                Path tempImage = Files.createTempFile("ocr-page-" + page, ".png");
                ImageIO.write(image, "png", tempImage.toFile());

                allText.append(runTesseract(tempImage)).append("\n");
                Files.deleteIfExists(tempImage);
            }
            return allText.toString();
        } catch ( IOException e) {
            throw new RuntimeException("OCR failed", e);
        }
    }

    private String runTesseract(Path imagePath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "tesseract", imagePath.toString(), "stdout", "-l", "ind+eng"
        );
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return output;
    }
}
