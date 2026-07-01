package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class SkillBytecodeTest {

	@Test
	void delayedSkillWorkDoesNotDependOnSeparateAnonymousClasses() throws IOException {
		String classResource = "com/aionemu/gameserver/skillengine/model/Skill.class";
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		byte[] classBytes;
		try (var inputStream = classLoader.getResourceAsStream(classResource)) {
			assertNotNull(inputStream, classResource + " should be compiled before the test runs");
			classBytes = inputStream.readAllBytes();
		}

		String constantPoolText = new String(classBytes, StandardCharsets.ISO_8859_1);

		assertFalse(constantPoolText.contains("com/aionemu/gameserver/skillengine/model/Skill$1"),
			"delayed effect application should not require Skill$1.class at runtime");
		assertFalse(constantPoolText.contains("com/aionemu/gameserver/skillengine/model/Skill$2"),
			"scheduled casts should not require Skill$2.class at runtime");
		assertFalse(constantPoolText.contains("com/aionemu/gameserver/skillengine/model/Skill$3"),
			"skill duration calculation should not require a compiler-generated enum switch-map class at runtime");
	}
}
