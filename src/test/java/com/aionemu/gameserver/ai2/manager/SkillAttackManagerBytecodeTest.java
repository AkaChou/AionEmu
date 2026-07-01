package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class SkillAttackManagerBytecodeTest {

	@Test
	void scheduledSkillAttacksDoNotDependOnSeparateNestedActionClasses() throws IOException {
		String classResource = "com/aionemu/gameserver/ai2/manager/SkillAttackManager.class";
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		byte[] classBytes;
		try (var inputStream = classLoader.getResourceAsStream(classResource)) {
			assertNotNull(inputStream, classResource + " should be compiled before the test runs");
			classBytes = inputStream.readAllBytes();
		}

		String constantPoolText = new String(classBytes, StandardCharsets.ISO_8859_1);

		assertFalse(constantPoolText.contains("com/aionemu/gameserver/ai2/manager/SkillAttackManager$SkillAction"),
			"scheduled NPC skills should not require SkillAttackManager$SkillAction.class at runtime");
		assertFalse(constantPoolText.contains("com/aionemu/gameserver/ai2/manager/SkillAttackManager$1"),
			"NPC skill selection should not require a compiler-generated enum switch-map class at runtime");
	}
}
