package com.github.rickysurya.tenderofferapi.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

                allText.append(runTesseractFiltered(tempImage)).append("\n");
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
    private static final Set<String> ANNOUNCEMENT_KEYWORDS =
            Set.of("pengumuman", "tender", "pengambilalihan", "pojk", "penawaran");

    private String runTesseractFiltered(Path imagePath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "tesseract", imagePath.toString(), "stdout", "-l", "ind+eng", "tsv"
        );
        Process process = pb.start();
        String tsv = new String(process.getInputStream().readAllBytes());
        try { process.waitFor(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Map<Integer, StringBuilder> blocks = new LinkedHashMap<>();
        String[] lines = tsv.split("\n");
        for (int i = 1; i < lines.length; i++) { // row 0 is the header
            String[] cols = lines[i].split("\t");
            if (cols.length < 12) continue;
            int blockNum = Integer.parseInt(cols[2]);
            blocks.computeIfAbsent(blockNum, k -> new StringBuilder()).append(cols[11]).append(" ");
        }

        StringBuilder relevant = new StringBuilder();
        for (StringBuilder block : blocks.values()) {
            String lower = block.toString().toLowerCase();
            if (ANNOUNCEMENT_KEYWORDS.stream().anyMatch(lower::contains)) {
                relevant.append(block).append("\n\n");
            }
        }
        return relevant.toString();
    }
}
