package com.krag.core.model;

import java.util.Map;

public class VectorRecord {
    private String chunkId;
    private String docId;
    private String source;
    private float[] vector;
    private Map<String, String> metadata;

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public float[] getVector() { return vector; }
    public void setVector(float[] vector) { this.vector = vector; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}