package com.krag.api.controller;

import com.krag.ingest.service.IngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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
}