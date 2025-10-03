package com.krag.api.vo;

public class QueryRequest {
    private String tenantId;
    private String kbId;
    private String query;
    private Integer topK;
    private Float minScore;
    private String docId;      // 可选：指定文档ID进行检索
    private Boolean full;      // 可选：是否返回完整文档文本（拼接所有片段）

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    public Float getMinScore() { return minScore; }
    public void setMinScore(Float minScore) { this.minScore = minScore; }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public Boolean getFull() { return full; }
    public void setFull(Boolean full) { this.full = full; }
}