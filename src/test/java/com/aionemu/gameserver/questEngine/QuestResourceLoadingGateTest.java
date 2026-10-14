package com.aionemu.gameserver.questEngine;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * quest 栈资源读取门禁：数据资源只许经 QuestDataResources 定位（外部运行目录优先，classpath 回退）。
 * Gate: quest-stack classes must locate quest data resources only through QuestDataResources
 * (external runtime directory first, classpath fallback).
 */
class QuestResourceLoadingGateTest {
	private static final Pattern DIRECT_RESOURCE_READ = Pattern.compile("getResource(AsStream)?\\s*\\(");

	@Test
	void questStackLocatesResourcesThroughQuestDataResourcesOnly() throws IOException {
		try (Stream<Path> paths = Files.walk(Path.of("src/main/java/com/aionemu/gameserver/questEngine"))) {
			paths.filter(path -> path.toString().endsWith(".java"))
				.filter(path -> !"QuestDataResources.java".equals(path.getFileName().toString()))
				.forEach(path -> {
					try {
						String source = Files.readString(path);
						assertFalse(DIRECT_RESOURCE_READ.matcher(source).find(),
							path + " must locate quest data resources through QuestDataResources");
					} catch (IOException e) {
						throw new AssertionError(e);
					}
				});
		}
	}
}
