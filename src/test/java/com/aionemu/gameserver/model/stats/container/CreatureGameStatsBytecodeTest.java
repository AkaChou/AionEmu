package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CreatureGameStatsBytecodeTest {

	@Test
	void magicalDefenseLookupDoesNotDependOnCompilerGeneratedElementSwitchMap() throws IOException {
		String classResource = "com/aionemu/gameserver/model/stats/container/CreatureGameStats.class";
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		byte[] classBytes;
		try (var inputStream = classLoader.getResourceAsStream(classResource)) {
			assertNotNull(inputStream, classResource + " should be compiled before the test runs");
			classBytes = inputStream.readAllBytes();
		}

		String constantPoolText = new String(classBytes, StandardCharsets.ISO_8859_1);

		assertFalse(constantPoolText.contains("com/aionemu/gameserver/model/stats/container/CreatureGameStats$1"),
				"magical defense lookup should not require a compiler-generated enum switch-map class at runtime");
	}
}
