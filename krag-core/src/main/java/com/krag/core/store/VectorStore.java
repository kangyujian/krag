package com.krag.core.store;

import com.krag.core.model.SearchResult;
import com.krag.core.model.VectorRecord;

import java.util.List;

public interface VectorStore {
    void upsert(String tenantId, String kbId, List<VectorRecord> records);
    List<SearchResult> search(String tenantId, String kbId, float[] queryVector, int topK, float minScore);
    void deleteByDoc(String tenantId, String kbId, String docId);
    List<SearchResult> findByDoc(String tenantId, String kbId, String docId);
    /**
     * List distinct document IDs within a tenant+kb namespace.
     */
    List<String> listDocIds(String tenantId, String kbId);
}