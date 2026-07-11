package com.aionemu.boot.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LocalizedLogCallsTest {

    private static final Pattern DIRECT_VISIBLE_LOG = Pattern.compile(
        "(?s)\\b(?:log|logger|LOG|LOGGER)\\.(?:info|warn|error|fatal)\\s*\\(\\s*(?:\"|String\\.format\\s*\\()"
    );

    @Test
    void visibleLogStringLiteralsUseI18n() throws IOException {
        List<Path> violations = new ArrayList<>();
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path).replaceAll("(?s)/\\*.*?\\*/|//[^\\r\\n]*", "");
                    if (DIRECT_VISIBLE_LOG.matcher(source).find()) {
                        violations.add(path);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertTrue(violations.isEmpty(), "Direct visible log strings must use I18n: " + violations);
    }
}
