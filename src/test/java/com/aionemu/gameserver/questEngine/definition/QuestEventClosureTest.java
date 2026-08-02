package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The legacy engine may only dispatch event types that exist in the typed
 * {@link QuestEvent} sealed set. A new {@code invokeObserved} event without a
 * typed counterpart is a compile-time gate violation (an "unclassified call")
 * and fails this test, so the closure can never silently regress.
 */
class QuestEventClosureTest {

	@Test
	void everyEngineDispatchHasATypedQuestEvent() throws Exception {
		String engineSource = Files.readString(
			Path.of("src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java"));
		String eventSource = Files.readString(
			Path.of("src/main/java/com/aionemu/gameserver/questEngine/definition/QuestEvent.java"));

		Set<String> dispatched = new HashSet<>();
		Pattern dispatchPattern = Pattern.compile("invokeObserved\\(env, [^,]+, \"([A-Z_]+)\"");
		for (var matcher : dispatchPattern.matcher(engineSource).results().toList()) {
			dispatched.add(matcher.group(1));
		}

		Set<String> typed = new HashSet<>();
		Pattern typePattern = Pattern.compile("return \"([A-Z_]+)\";");
		for (var matcher : typePattern.matcher(eventSource).results().toList()) {
			typed.add(matcher.group(1));
		}

		Set<String> missing = new TreeSet<>(dispatched);
		missing.removeAll(typed);
		assertEquals(Set.of(), missing, "engine dispatches an event without a typed QuestEvent");
	}
}
