package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SimpleAttackManagerTargetSafetyTest {

	@Test
	void geoVisibilityUsesCapturedTargetReference() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/ai2/manager/SimpleAttackManager.java"));

		assertFalse(source.contains("canSee(npc, npc.getTarget())"),
				"Geo visibility checks must not re-read npc.getTarget() after target validation");
	}
}
