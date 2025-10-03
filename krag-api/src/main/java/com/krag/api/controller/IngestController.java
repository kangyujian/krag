package com.krag.api.controller;

import com.krag.ingest.service.IngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(path = "/api/v1")
public class IngestController {

    private final IngestionService ingestionService;

    public IngestController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(path = "/ingest/txt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> ingestTxt(@RequestParam("tenantId") String tenantId,
                                         @RequestParam("kbId") String kbId,
                                         @RequestPart("file") MultipartFile file) throws Exception {
        return ingestionService.ingestTxt(tenantId, kbId, file.getInputStream(), file.getOriginalFilename());
    }

    @PostMapping(path = "/ingest/text", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> ingestText(@RequestParam("tenantId") String tenantId,
                                          @RequestParam("kbId") String kbId,
                                          @RequestParam("filename") String filename,
                                          @RequestBody String text) {
        String fn = validateTextIngestParams(tenantId, kbId, filename, text);
        ByteArrayInputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        return ingestionService.ingestTxt(tenantId, kbId, in, fn);
    }

    private String validateTextIngestParams(String tenantId, String kbId, String filename, String text) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("kbId is required");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename is required");
        }
        String fn = filename.trim();
        if (fn.length() > 255) {
            throw new IllegalArgumentException("filename length must be <= 255");
        }
        if (!fn.toLowerCase().endsWith(".txt")) {
            throw new IllegalArgumentException("filename must end with .txt");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("text content is empty");
        }
        return fn;
    }
}