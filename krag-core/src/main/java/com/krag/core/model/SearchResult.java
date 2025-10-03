package com.krag.core.model;

import java.util.Map;

public class SearchResult {
    private String chunkId;
    private String docId;
    private String text;
    private float score;
    private Map<String, String> metadata;

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}