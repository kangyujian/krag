package com.krag.core.model;

import java.util.Map;

public class SearchResult {
    public String chunkId;
    public String docId;
    public String text;
    public float score;
    public Map<String, String> metadata;
}