package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class QuestDefinitionForbiddenPatternTest {
	@Test
	void typedDefinitionAndRuntimeDoNotAddOpenEndedExecutionHooks() throws Exception {
		List<String> forbidden = List.of("Class.forName(", "java.lang.reflect", ".invoke(", "ScriptEngine",
			"Runtime.getRuntime(", "ProcessBuilder(", "QuestHandlerLoader", "QuestHandler");
		try (Stream<Path> paths = Stream.concat(
				Files.walk(Path.of("src/main/java/com/aionemu/gameserver/questEngine/definition")),
				Files.walk(Path.of("src/main/java/com/aionemu/gameserver/questEngine/runtime")))) {
			paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
				try {
					String source = Files.readString(path);
					for (String pattern : forbidden) {
						assertFalse(source.contains(pattern), path + " contains forbidden pattern " + pattern);
					}
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			});
		}
	}
}
