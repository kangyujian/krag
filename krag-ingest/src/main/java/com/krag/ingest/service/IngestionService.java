package com.krag.ingest.service;

import com.krag.core.chunk.TextChunker;
import com.krag.core.embed.EmbeddingModel;
import com.krag.core.model.VectorRecord;
import com.krag.core.parser.DocumentParser;
import com.krag.core.store.VectorStore;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class IngestionService {

    private final DocumentParser parser;
    private final TextChunker chunker;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public IngestionService(DocumentParser parser, TextChunker chunker, EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.parser = parser;
        this.chunker = chunker;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    public Map<String, Object> ingestTxt(String tenantId, String kbId, InputStream in, String filename) {
        String ext = filename != null ? filename.toLowerCase() : "";
        if (!parser.supports(ext)) {
            throw new IllegalArgumentException("Unsupported file type: " + filename);
        }
        List<String> paragraphs = parser.parseToTexts(in, filename);
        List<String> chunks = chunker.chunk(paragraphs, 800);
        List<float[]> vectors = embeddingModel.embedBatch(chunks);

        String docId = UUID.randomUUID().toString();
        List<VectorRecord> records = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            VectorRecord r = new VectorRecord();
            r.setDocId(docId);
            r.setChunkId(docId + "_" + i);
            r.setSource(filename);
            r.setVector(vectors.get(i));
            Map<String, String> md = new HashMap<>();
            md.put("text", chunks.get(i));
            r.setMetadata(md);
            records.add(r);
        }
        vectorStore.upsert(tenantId, kbId, records);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("docId", docId);
        resp.put("chunks", chunks.size());
        resp.put("dimension", embeddingModel.dimension());
        return resp;
    }
}