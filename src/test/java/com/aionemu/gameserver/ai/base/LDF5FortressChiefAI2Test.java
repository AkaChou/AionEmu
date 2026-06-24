package com.aionemu.gameserver.ai.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LDF5FortressChiefAI2Test {

	@Test
	void announcementsDoNotDependOnAnonymousVisitorClasses() throws IOException {
		String classResource = "com/aionemu/gameserver/ai/base/LDF5_Fortress_ChiefAI2.class";
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		byte[] classBytes;
		try (var inputStream = classLoader.getResourceAsStream(classResource)) {
			assertNotNull(inputStream, classResource + " should be compiled before the test runs");
			classBytes = inputStream.readAllBytes();
		}

		String constantPoolText = new String(classBytes, StandardCharsets.ISO_8859_1);

		assertFalse(constantPoolText.contains("com/aionemu/gameserver/ai/base/LDF5_Fortress_ChiefAI2$"),
			"LDF5 fortress announcements should not depend on separately loaded anonymous inner classes");
	}
}
