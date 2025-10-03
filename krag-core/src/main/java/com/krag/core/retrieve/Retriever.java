package com.krag.core.retrieve;

import com.krag.core.model.SearchResult;

import java.util.List;

public interface Retriever {
    List<SearchResult> retrieve(String tenantId, String kbId, String query, int topK);
}