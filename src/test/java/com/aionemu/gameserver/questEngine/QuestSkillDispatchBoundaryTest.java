package com.aionemu.gameserver.questEngine;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestSkillDispatchBoundaryTest {
	@Test
	void playerControllerDelegatesQuestDispatchToTheSkillExecutionBoundary() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/controllers/PlayerController.java"));
		int start = source.indexOf("public void useSkill(SkillTemplate template");
		int end = source.indexOf("\n\t/**", start);
		String method = source.substring(start, end);

		assertTrue(method.contains("skill.useSkill();"));
		assertFalse(method.contains(".onUseSkill("));
	}

	@Test
	void skillExecutionOwnsExactlyOneQuestDispatch() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/skillengine/model/Skill.java"));

		assertEquals(1, occurrences(source, ".onUseSkill("));
		assertTrue(source.contains("GameEngineServices.questEngine().onUseSkill(env, skillTemplate.getSkillId());"));
	}

	private static int occurrences(String source, String needle) {
		int count = 0;
		for (int offset = source.indexOf(needle); offset >= 0; offset = source.indexOf(needle, offset + needle.length())) {
			count++;
		}
		return count;
	}
}
