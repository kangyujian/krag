package com.krag.core.model;

import lombok.Data;

import java.util.Map;

@Data
public class VectorRecord {
    private String chunkId;
    private String docId;
    private String source;
    private float[] vector;
    private Map<String, String> metadata;
}