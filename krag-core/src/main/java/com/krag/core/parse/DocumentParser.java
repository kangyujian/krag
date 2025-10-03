package com.krag.core.parse;

import java.io.InputStream;
import java.util.List;

public interface DocumentParser {
    boolean supports(String contentTypeOrExt);
    List<String> parseToTexts(InputStream in, String filename);
}