package com.shakepro.service.support;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class IngredientDictionaryService {

    private static final Path DICTIONARY_PATH = Path.of("docs", "ingredients.csv");
    private volatile Map<String, DictionaryItem> cache;

    public Map<String, DictionaryItem> getDictionary() {
        Map<String, DictionaryItem> snapshot = cache;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            if (cache == null) {
                cache = loadDictionary();
            }
            return cache;
        }
    }

    public DictionaryItem findByEnglish(String englishName) {
        if (englishName == null) {
            return null;
        }
        return getDictionary().get(normalizeKey(englishName));
    }

    public static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value
                .replace('\uFEFF', ' ')
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        return normalized;
    }

    private Map<String, DictionaryItem> loadDictionary() {
        if (!Files.exists(DICTIONARY_PATH)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "词典文件不存在: " + DICTIONARY_PATH);
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(DICTIONARY_PATH, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "读取词典文件失败: " + e.getMessage());
        }

        Map<String, DictionaryItem> map = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (i == 0 && line.toLowerCase(Locale.ROOT).contains("english")) {
                continue;
            }

            List<String> columns = parseCsvLine(line);
            if (columns.size() < 3) {
                continue;
            }

            String english = clean(columns.get(0));
            String chinese = clean(columns.get(1));
            String category = clean(columns.get(2));
            if (english == null || chinese == null || category == null) {
                continue;
            }

            String key = normalizeKey(english);
            map.putIfAbsent(key, new DictionaryItem(english, chinese, category));
        }

        log.info("Loaded ingredient dictionary from {}, size={}", DICTIONARY_PATH, map.size());
        return Map.copyOf(map);
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.replace('\uFEFF', ' ').trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record DictionaryItem(String english, String chinese, String category) {
    }
}
