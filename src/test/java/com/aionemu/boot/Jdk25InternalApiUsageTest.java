package com.aionemu.boot;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class Jdk25InternalApiUsageTest {

    @Test
    void sourceTreeAvoidsJdkInternalAndForRemovalApis() throws Exception {
        assertSourceTreeOmits(Path.of("src/main/java"), List.of(
                "java.security." + "Access" + "Controller",
                "Access" + "Controller." + "do" + "Privileged",
                "System." + "run" + "Finalization(",
                "Class.forName(\"sun.misc." + "Unsafe\")",
                "getMethod(\"invoke" + "Cleaner\"",
                "sun.misc." + "Launcher$"));
    }

    private void assertSourceTreeOmits(Path root, List<String> forbiddenTexts) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path sourcePath : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(sourcePath);
                for (String forbiddenText : forbiddenTexts) {
                    assertFalse(source.contains(forbiddenText),
                            () -> sourcePath + " still contains " + forbiddenText);
                }
            }
        }
    }
}
