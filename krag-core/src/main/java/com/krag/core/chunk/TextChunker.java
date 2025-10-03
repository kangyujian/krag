package com.krag.core.chunk;

import java.util.List;

/**
 * Core interface for splitting raw texts into smaller chunks.
 * Implementations live in ingest module (e.g., SimpleTextChunker).
 */
public interface TextChunker {
    List<String> chunk(List<String> texts, int maxTokensOrChars);
}