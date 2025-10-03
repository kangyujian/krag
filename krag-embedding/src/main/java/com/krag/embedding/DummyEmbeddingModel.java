package com.krag.embedding;

import com.krag.core.embed.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class DummyEmbeddingModel implements EmbeddingModel {
    private static final int DIM = 128;

    @Override
    public String id() { return "dummy-embedding-128"; }

    @Override
    public int dimension() { return DIM; }

    @Override
    public float[] embed(String text) {
        if (text == null) text = "";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        float[] v = new float[DIM];
        for (int i = 0; i < bytes.length; i++) {
            int idx = (bytes[i] & 0xFF) % DIM;
            v[idx] += 1.0f;
        }
        // L2 normalize
        float sumSq = 0f;
        for (float f : v) sumSq += f * f;
        float norm = (float) Math.sqrt(sumSq);
        if (norm > 0f) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
        return v;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> out = new ArrayList<>();
        if (texts == null) return out;
        for (String t : texts) out.add(embed(t));
        return out;
    }
}