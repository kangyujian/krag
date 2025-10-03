package com.krag.ingest.chunk;

import com.krag.core.chunk.TextChunker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleTextChunker implements TextChunker {
    @Override
    public List<String> chunk(List<String> texts, int maxTokensOrChars) {
        StringBuilder all = new StringBuilder();
        for (String t : texts) {
            if (t == null) continue;
            if (all.length() > 0) all.append('\n');
            all.append(t);
        }
        String s = all.toString();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int len = s.length();
        while (start < len) {
            int end = Math.min(start + maxTokensOrChars, len);
            // try to break at whitespace
            int breakPos = end;
            for (int i = end - 1; i > start; i--) {
                char c = s.charAt(i);
                if (Character.isWhitespace(c)) { breakPos = i; break; }
            }
            if (breakPos == start) breakPos = end;
            chunks.add(s.substring(start, breakPos).trim());
            start = breakPos;
        }
        return chunks;
    }
}