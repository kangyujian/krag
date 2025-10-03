package com.krag.core.embed;

import java.util.List;

public interface EmbeddingModel {
    String id();
    int dimension();
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}

