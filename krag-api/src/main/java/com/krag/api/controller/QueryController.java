package com.krag.api.controller;

import com.krag.api.vo.QueryRequest;
import com.krag.core.embed.EmbeddingModel;
import com.krag.core.model.SearchResult;
import com.krag.core.store.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/v1")
public class QueryController {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public QueryController(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @PostMapping(path = "/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> query(@RequestBody QueryRequest req) {
        validate(req);

        // If docId is provided, return document chunks or full text
        if (!isBlank(req.getDocId())) {
            List<SearchResult> chunks = vectorStore.findByDoc(req.getTenantId(), req.getKbId(), req.getDocId());
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("tenantId", req.getTenantId());
            resp.put("kbId", req.getKbId());
            resp.put("docId", req.getDocId());
            resp.put("chunks", chunks);
            boolean full = Boolean.TRUE.equals(req.getFull());
            if (full) {
                String fullText = chunks.stream()
                        .sorted(Comparator.comparingInt(QueryController::chunkIndex))
                        .map(SearchResult::getText)
                        .filter(s -> s != null && !s.isEmpty())
                        .collect(Collectors.joining("\n\n"));
                resp.put("documentText", fullText);
            }
            return resp;
        }

        // Otherwise, perform vector search by query text
        int topK = req.getTopK() != null ? req.getTopK() : 5;
        float minScore = req.getMinScore() != null ? req.getMinScore() : 0f;
        float[] qv = embeddingModel.embed(req.getQuery());
        List<SearchResult> results = vectorStore.search(req.getTenantId(), req.getKbId(), qv, topK, minScore);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tenantId", req.getTenantId());
        resp.put("kbId", req.getKbId());
        resp.put("model", embeddingModel.id());
        resp.put("topK", topK);
        resp.put("results", results);
        return resp;
    }

    private void validate(QueryRequest req) {
        if (req == null) throw new IllegalArgumentException("request body is required");
        if (isBlank(req.getTenantId())) throw new IllegalArgumentException("tenantId is required");
        if (isBlank(req.getKbId())) throw new IllegalArgumentException("kbId is required");
        // If docId provided, query may be empty; otherwise query is required
        if (isBlank(req.getDocId()) && isBlank(req.getQuery())) {
            throw new IllegalArgumentException("query is required when docId is not provided");
        }
        if (req.getTopK() != null) {
            if (req.getTopK() <= 0 || req.getTopK() > 50) {
                throw new IllegalArgumentException("topK must be in range [1, 50]");
            }
        }
        if (req.getMinScore() != null) {
            if (req.getMinScore() < 0) {
                throw new IllegalArgumentException("minScore must be >= 0");
            }
        }
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static int chunkIndex(SearchResult sr) {
        String chunkId = sr.getChunkId();
        if (chunkId == null) return Integer.MAX_VALUE;
        int idx = chunkId.lastIndexOf('_');
        if (idx < 0) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(chunkId.substring(idx + 1));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
}