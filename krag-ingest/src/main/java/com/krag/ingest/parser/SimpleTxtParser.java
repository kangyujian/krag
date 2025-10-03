package com.krag.ingest.parser;

import com.krag.core.parser.DocumentParser;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SimpleTxtParser implements DocumentParser {
    @Override
    public boolean supports(String contentTypeOrExt) {
        if (contentTypeOrExt == null) return false;
        String v = contentTypeOrExt.toLowerCase();
        return v.contains("text/plain") || v.endsWith(".txt");
    }

    @Override
    public List<String> parseToTexts(InputStream in, String filename) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> paragraphs = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    if (current.length() > 0) {
                        paragraphs.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    if (current.length() > 0) current.append('\n');
                    current.append(line);
                }
            }
            if (current.length() > 0) paragraphs.add(current.toString());
            return paragraphs.isEmpty() ? List.of("") : paragraphs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}