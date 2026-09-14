package com.github.rickysurya.tenderofferapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OllamaExtractionIT {

    @Autowired
    private OllamaExtractionService ollamaExtractionService;

    @Test
    void extractsKnownFieldsFromRealDisclosure() throws IOException {
        Path fixtureDir = Path.of("src/test/resources/fixtures/mto-2026-05-11");

//        List<byte[]> pdfs = List.of(readBytes(fixtureDir.resolve("doc2.pdf")));
        List<byte[]> pdfs;
        try (Stream<Path> files = Files.list(fixtureDir)) {
            pdfs = files.sorted().map(this::readBytes).toList();
        }

        JsonNode result = ollamaExtractionService.extract(pdfs);
        System.out.println(result.toPrettyString());

        assertThat(result.path("ticker").asText()).isEqualTo("MAPI");
        assertThat(result.path("offerPricePerShare").asInt()).isEqualTo(1550);
    }

    private byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
