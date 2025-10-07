package com.krag.api.controller;

import com.krag.core.embed.EmbeddingModel;
import com.krag.core.model.SearchResult;
import com.krag.core.store.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping(path = "/api/v1")
public class DocumentController {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public DocumentController(VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    @GetMapping(path = "/docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> listDocs(
            @RequestParam(name = "tenantId") String tenantId,
            @RequestParam(name = "kbId") String kbId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        int p = (page != null && page > 0) ? page : 1;
        int s = (size != null && size > 0) ? size : 10;
        List<String> ids;
        try {
            ids = vectorStore.listDocIds(tenantId, kbId);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            // Gracefully handle empty index namespaces (Lucene IndexNotFoundException)
            if (msg != null && (msg.contains("IndexNotFoundException") || msg.contains("no segments"))) {
                ids = Collections.emptyList();
            } else {
                throw ex;
            }
        }
        int total = ids.size();
        int from = Math.min((p - 1) * s, total);
        int to = Math.min(from + s, total);

        List<Map<String, Object>> items = new ArrayList<>();
        for (String id : ids.subList(from, to)) {
            List<SearchResult> chunks = vectorStore.findByDoc(tenantId, kbId, id);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("docId", id);
            d.put("chunks", chunks.size());
            items.add(d);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tenantId", tenantId);
        resp.put("kbId", kbId);
        resp.put("page", p);
        resp.put("size", s);
        resp.put("total", total);
        resp.put("items", items);
        return resp;
    }

    @GetMapping(path = "/doc/chunks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> docChunks(
            @RequestParam(name = "tenantId") String tenantId,
            @RequestParam(name = "kbId") String kbId,
            @RequestParam(name = "docId") String docId,
            @RequestParam(name = "includeVectors", required = false) Boolean includeVectors
    ) {
        List<SearchResult> chunks = vectorStore.findByDoc(tenantId, kbId, docId);
        List<Map<String, Object>> items = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (SearchResult sr : chunks) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("chunkId", sr.getChunkId());
            c.put("text", sr.getText());
            items.add(c);
            texts.add(sr.getText() != null ? sr.getText() : "");
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("tenantId", tenantId);
        resp.put("kbId", kbId);
        resp.put("docId", docId);
        resp.put("chunks", items);

        if (Boolean.TRUE.equals(includeVectors)) {
            List<float[]> vecs = embeddingModel.embedBatch(texts);
            resp.put("dimension", embeddingModel.dimension());
            resp.put("vectors", vecs);
        }
        return resp;
    }
}