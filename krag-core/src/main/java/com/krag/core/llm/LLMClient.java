package com.krag.core.llm;

import com.krag.core.model.ContextDoc;

import java.util.List;

public interface LLMClient {
    String modelId();
    String chat(String systemPrompt, String userPrompt, List<ContextDoc> contexts);
}