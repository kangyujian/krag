package com.krag.core.model;

import java.util.Map;

public class VectorRecord {
    public String chunkId;
    public String docId;
    public String source;
    public float[] vector;
    public Map<String, String> metadata;
}