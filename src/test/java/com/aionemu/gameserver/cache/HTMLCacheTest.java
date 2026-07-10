package com.aionemu.gameserver.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class HTMLCacheTest {

	@Test
	void selectsHtmlByCountryCode() {
		assertEquals("instances/haramel.xhtml", HTMLCache.localizedPath("instances/haramel.xhtml", 5));
		assertEquals("instances/haramel.en.xhtml", HTMLCache.localizedPath("instances/haramel.xhtml", 1));
		assertEquals("instances/haramel.en.xhtml", HTMLCache.localizedPath("instances/haramel.en.xhtml", 1));
	}

	@Test
	void everyHtmlHasAnEnglishVersion() throws IOException {
		Path htmlRoot = Path.of("src/main/resources/aion/game/data/static_data/HTML");
		try (var files = Files.walk(htmlRoot)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".xhtml") && !path.toString().endsWith(".en.xhtml")).toList()) {
				String name = file.getFileName().toString();
				Path english = file.resolveSibling(name.substring(0, name.length() - ".xhtml".length()) + ".en.xhtml");
				assertTrue(Files.isRegularFile(english), () -> "Missing English HTML: " + english);
			}
		}
	}
}
