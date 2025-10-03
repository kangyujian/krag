package com.krag.core.chunk;

import java.util.List;

public interface TextChunker {
    List<String> chunk(List<String> texts, int maxTokensOrChars);
}